package com.spotify.gradle.bazel;

import com.google.common.annotations.VisibleForTesting;
import com.spotify.gradle.bazel.strategies.Factory;
import com.spotify.gradle.bazel.strategies.Strategy;
import com.spotify.gradle.bazel.tasks.BazelCleanTask;
import com.spotify.gradle.bazel.tasks.BazelConfigTask;
import com.spotify.gradle.bazel.tasks.BazelExpungeTask;
import com.spotify.gradle.bazel.tasks.BazelInfoTask;
import com.spotify.gradle.bazel.tasks.BazelTargetCleanTask;
import com.spotify.gradle.bazel.tasks.DownloadBazelTask;
import com.spotify.gradle.bazel.utils.BazelExecHelper;
import com.spotify.gradle.hatchej.HatchejImlAction;
import com.spotify.gradle.hatchej.HatchejImlActionFactory;
import com.spotify.gradle.hatchej.HatchejModel;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

/**
 * A Gradle plugin that uses Bazel to build a module. The module then can be used as a dependency to any
 * other Gradle module.
 * <p>
 * Each Bazel module will have a `compile` task (expressed by {@link com.spotify.gradle.bazel.tasks.BazelBuildTask}),
 * a `test` task (expressed by {@link com.spotify.gradle.bazel.tasks.BazelTestTask}).
 * <p>
 * Dependencies declared in Bazel will be mirrored to Gradle dependency graph.
 * <p>
 * The root project will get a `bazelClean` (expressed by {@link BazelCleanTask}) - which will be added to the
 * global `clean` task dependency - and a `bazelExpungeClean` task (expressed by {@link BazelExpungeTask}).
 */
public class BazelLeafPlugin implements Plugin<Project> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BazelLeafPlugin.class);
    @VisibleForTesting final BazelAspectServiceFactory mBazelAspectServiceFactory;
    @VisibleForTesting final HatchejImlActionFactory mHatchejImlActionFactory;
    @VisibleForTesting final BazelExecHelper mBazelExecHelper;
    private String mBazelBinPath;

    @Inject
    public BazelLeafPlugin() {
        this(new HatchejImlActionFactory(),
                new BazelAspectServiceFactory(),
                new BazelExecHelper());
    }

    public BazelLeafPlugin(
            HatchejImlActionFactory hatchejImlActionFactory,
            BazelAspectServiceFactory bazelAspectServiceFactory,
            BazelExecHelper bazelExecHelper) {
        mHatchejImlActionFactory = hatchejImlActionFactory;
        mBazelAspectServiceFactory = bazelAspectServiceFactory;
        mBazelExecHelper = bazelExecHelper;
    }

    @Override
    public void apply(Project project) {
        project.getExtensions().create("bazel", BazelLeafConfig.class);

        //Adding some basic, static, elements to the module
        final Configuration defaultConfiguration = project.getConfigurations().create(Dependency.DEFAULT_CONFIGURATION);
        defaultConfiguration.setCanBeConsumed(true);
        defaultConfiguration.setCanBeResolved(true);
        defaultConfiguration.setTransitive(true);
        defaultConfiguration.setVisible(true);

        mBazelBinPath = BazelLeafConfig.getBazelBinPath(project);

        final DefaultTask assemble = project.getTasks().create("assemble", DefaultTask.class);
        final DefaultTask check = project.getTasks().create("check", DefaultTask.class);
        check.dependsOn(assemble);

        /*
         * Adding download tasks to root project
         */
        final DownloadBazelTask downloadBazelBinTask = DownloadBazelTask.injectDownloadTask(project, mBazelBinPath);
        //this task will not be NULL if injection took place (should happen only once).
        if (downloadBazelBinTask != null) {
            //verifying that bazel is valid
            if (!checkIsBazelBinaryValid(mBazelBinPath, project.getRootDir(), downloadBazelBinTask)) {
                throw new IllegalStateException(String.format("Bazel binary at %s is invalid, and was unable to download a valid binary.", mBazelBinPath));
            }
        }

        project.afterEvaluate(this::configurePlugin);
    }

    private void configurePlugin(Project project) {
        final Configuration defaultConfiguration = project.getConfigurations().findByName(Dependency.DEFAULT_CONFIGURATION);

        final Project rootProject = project.getRootProject();

        final BazelLeafConfig.Decorated config = project.getExtensions().getByType(BazelLeafConfig.class).decorate(project, mBazelBinPath);

        final Properties bazelInfo = mBazelExecHelper.getInfo(config);

        project.setBuildDir(String.format(Locale.US, "%s/%s", bazelInfo.getProperty("bazel-bin"), config.targetPath.substring(2)));

        final HatchejModel hatchejModel = new HatchejModel();

        final AspectRunner aspectRunner = new AspectRunner(config, mBazelExecHelper);
        final BazelAspectService bazelAspectService = mBazelAspectServiceFactory.create(rootProject, aspectRunner);

        final Strategy strategy = Factory.buildStrategy(
                aspectRunner.getAspectResult("get_rule_kind.bzl", config.targetName).stream().findFirst()
                        .orElse("java_library"), config);
        /*
         * creating a Bazel-Build task
         */
        final Task bazelBuildTask = strategy.createBazelExecTask(project);
        project.getTasks().findByName("assemble").dependsOn(bazelBuildTask);

        strategy.getBazelArtifacts(aspectRunner, project, bazelBuildTask).stream()
                .peek(defaultConfiguration.getOutgoing().getArtifacts()::add)
                .map(BazelPublishArtifact::getFile)
                .peek(hatchejModel.getProjectOutputs()::add)
                .forEach(bazelBuildTask.getOutputs()::file);

        final Set<File> sourceFolders = bazelAspectService.getSourceFolders(config.targetName);
        hatchejModel.getSourceFolders().addAll(sourceFolders);
        sourceFolders.forEach(bazelBuildTask.getInputs()::dir);

        final BazelAspectService.Deps targetDeps = bazelAspectService.getAllDeps(config.targetName);
        targetDeps.moduleDeps.stream()
                .map(BazelLeafPlugin::convertLocalBazelDepToGradle)
                .peek(hatchejModel.getProjectDependencies()::add)
                .map(gradlePath -> convertGradlePathToProject(project, gradlePath))
                .forEach(defaultConfiguration.getDependencies()::add);

        targetDeps.remoteWorkspaceDeps.stream()
                .map(bazelDep -> convertExternalJarBazelLocalPath(bazelInfo, bazelDep.jarPath))
                .forEach(hatchejModel.getLibraryDependencies()::add);
        targetDeps.remoteWorkspaceDeps.stream()
                .map(BazelLeafPlugin::createMavenDependency)
                .forEach(defaultConfiguration.getDependencies()::add);

        /*
         * Creating a regular CLEAN task
         */
        final BazelTargetCleanTask bazelTargetCleanTask = (BazelTargetCleanTask) project.task(Collections.singletonMap("type", BazelTargetCleanTask.class), "clean");
        bazelTargetCleanTask.addTargetTask(bazelBuildTask);

        /*
         * Adding tests
         */
        if (config.testTargetName != null && !config.testTargetName.isEmpty()) {
            final Strategy testStrategy = Factory.buildStrategy(aspectRunner.getAspectResult("get_rule_kind.bzl", config.testTargetName).stream().findFirst().orElse("java_test"), config);
            final Task testTask = testStrategy.createBazelExecTask(project);
            bazelTargetCleanTask.addTargetTask(testTask);
            final Set<File> testSources = bazelAspectService.getSourceFolders(config.testTargetName);
            testSources.forEach(testTask.getInputs()::dir);
            hatchejModel.getTestSourceFolders().addAll(testSources);
            testTask.dependsOn(bazelBuildTask);

            final BazelAspectService.Deps testTargetDeps = bazelAspectService.getAllDeps(config.testTargetName);
            testTargetDeps.moduleDeps.stream()
                    .filter(bazelDep -> !bazelDep.equals(config.targetPath + ':' + config.targetName))//no need to depend on self for tests
                    .map(BazelLeafPlugin::convertLocalBazelDepToGradle)
                    .peek(hatchejModel.getProjectTestDependencies()::add)
                    .map(gradlePath -> convertGradlePathToProject(project, gradlePath))
                    .forEach(defaultConfiguration.getDependencies()::add);

            testTargetDeps.remoteWorkspaceDeps.stream()
                    .map(bazelDep -> convertExternalJarBazelLocalPath(bazelInfo, bazelDep.jarPath))
                    .forEach(hatchejModel.getLibraryTestDependencies()::add);
            testTargetDeps.remoteWorkspaceDeps.stream()
                    .map(BazelLeafPlugin::createMavenDependency)
                    .forEach(defaultConfiguration.getDependencies()::add);
        }

        try {
            HatchejImlAction hatchejImlAction = mHatchejImlActionFactory.create();
            hatchejImlAction.modifyImlFile(project, hatchejModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        addBazelTasksToRootProject(rootProject, config);
    }

    private static void addBazelTasksToRootProject(
            Project rootProject,
            BazelLeafConfig.Decorated config) {
        /*
         * Creating a CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelClean", false/*only search the root project*/).isEmpty()) {
            final BazelCleanTask bazelCleanTask = (BazelCleanTask) rootProject.task(Collections.singletonMap("type", BazelCleanTask.class), "bazelClean");
            bazelCleanTask.setBazelConfig(config);
        }

        /*
         * Creating a SUPER-CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelExpungeClean", false/*only search the root project*/).isEmpty()) {
            final BazelConfigTask task = (BazelConfigTask) rootProject.task(Collections.singletonMap("type", BazelExpungeTask.class), "bazelExpungeClean");
            task.setBazelConfig(config);
        }

        /*
         * Creating a info task in the root project
         */
        if (rootProject.getTasksByName("bazelInfo", false/*only search the root project*/).isEmpty()) {
            final BazelConfigTask task = (BazelConfigTask) rootProject.task(Collections.singletonMap("type", BazelInfoTask.class), "bazelInfo");
            task.setBazelConfig(config);
        }
    }

    private boolean checkIsBazelBinaryValid(
            String bazelBinPath,
            File workspceRootFolder,
            DownloadBazelTask downloadBazelBinTask) {
        if (!isBazelExecutable(bazelBinPath, workspceRootFolder)) {
            LOGGER.debug("Bazel binary at {} is invalid. Trying to download...", bazelBinPath);
            try {
                downloadBazelBinTask.download();
            } catch (IOException e) {
                LOGGER.error(String.format(Locale.US, "Failed to download Bazel binary to %s!", bazelBinPath), e);
                return false;
            }
            return isBazelExecutable(bazelBinPath, workspceRootFolder);
        } else {
            return true;
        }
    }

    private boolean isBazelExecutable(
            String bazelBinPath,
            File workspceRootFolder) {
        try {
            final BazelExecHelper.BazelExec bazelExecVersion = mBazelExecHelper.createBazelRun(true, bazelBinPath, workspceRootFolder, "version", Collections.emptyList());

            return bazelExecVersion.start().getExitCode() == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.warn("Bazel binary is invalid! Error: {}", e.getMessage());
            return false;
        }
    }

    private static String convertExternalJarBazelLocalPath(
            Properties bazelInfo,
            String pathToExternalFile) {
        return String.format(Locale.US, "%s/%s", bazelInfo.getProperty("output_base"), pathToExternalFile);
    }

    private static String convertLocalBazelDepToGradle(BazelAspectService.LocalBazelTarget bazelDep) {
        final Matcher localModulesPattern = Pattern.compile("^/(/.+):.+$").matcher(bazelDep.path);
        if (localModulesPattern.matches()) {
            return localModulesPattern.group(1).replace("/", ":");
        } else {
            throw new IllegalArgumentException("The Bazel dep '" + bazelDep + "' can not be converted into a local Gradle dependency");
        }
    }

    private static Dependency convertGradlePathToProject(Project project, String gradlePath) {
        return project.getDependencies().project(Collections.singletonMap("path", gradlePath));
    }

    private static Dependency createMavenDependency(BazelAspectService.ExternalTarget remoteTarget) {
        //commons-io__commons-io__2_5 -> org.hamcrest:hamcrest-core:1.3
        //org_hamcrest__hamcrest-library__1_3 -> org.hamcrest:hamcrest-library:1.3
        final Matcher moduleMatcher = Pattern.compile("^(.+)__(.+)__(.+)$").matcher(remoteTarget.externalDepName);
        if (moduleMatcher.matches()) {
            final String groupId = moduleMatcher.group(1).replace('_', '.');
            final String artifactId = moduleMatcher.group(2);
            final String artifactVersion = moduleMatcher.group(3).replace('_', '.');
            return new DefaultClientModule(groupId, artifactId, artifactVersion);
        } else {
            throw new IllegalArgumentException("The path " + remoteTarget.externalDepName + " can not be parsed to retrieve artifact Maven details");
        }
    }
}
