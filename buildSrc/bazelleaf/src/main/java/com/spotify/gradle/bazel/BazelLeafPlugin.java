package com.spotify.gradle.bazel;

import com.spotify.gradle.bazel.strategies.Factory;
import com.spotify.gradle.bazel.strategies.Strategy;
import com.spotify.gradle.bazel.tasks.BazelCleanTask;
import com.spotify.gradle.bazel.tasks.BazelConfigTask;
import com.spotify.gradle.bazel.tasks.BazelExpungeTask;
import com.spotify.gradle.bazel.utils.BazelExecHelper;
import com.spotify.gradle.hatchej.HatchejImlAction;
import com.spotify.gradle.hatchej.HatchejModel;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class BazelLeafPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.getExtensions().create("bazel", BazelLeafConfig.class);

        project.afterEvaluate(BazelLeafPlugin::configurePlugin);
    }

    private static void configurePlugin(Project project) {
        final BazelLeafConfig.Decorated config = project.getExtensions().getByType(BazelLeafConfig.class).decorate(project);
        final Properties bazelInfo = BazelExecHelper.getInfo(config);

        final HatchejModel hatchejModel = new HatchejModel();
        final Project rootProject = project.getRootProject();

        final AspectRunner aspectRunner = new AspectRunner(config);
        final Strategy strategy = Factory.buildStrategy(aspectRunner.getAspectResult("get_rule_kind.bzl", config.targetName).stream().findFirst().orElse("java_library"), config);
        /*
         * creating a Bazel-Build task
         */
        final Task bazelBuildTask = strategy.createBazelExecTask(project);

        /*
         * Adding build configurations
         */
        final Configuration defaultConfiguration = project.getConfigurations().create(Dependency.DEFAULT_CONFIGURATION);
        defaultConfiguration.setCanBeConsumed(true);
        defaultConfiguration.setCanBeResolved(true);

        strategy.getBazelArtifacts(aspectRunner, project, bazelBuildTask).stream()
                .peek(defaultConfiguration.getOutgoing().getArtifacts()::add)
                .forEach(artifact -> bazelBuildTask.getOutputs().file(artifact.getFile()));

        hatchejModel.getSourceFolders().addAll(getSourceFoldersFromBazelAspect(rootProject, aspectRunner, config.targetName));

        final Deps targetDeps = getAllDepsFromBazel(aspectRunner, config.targetName);
        targetDeps.moduleDeps.stream().map(BazelLeafPlugin::convertLocalBazelDepToGradle).forEach(hatchejModel.getProjectDependencies()::add);
        targetDeps.remoteWorkspaceDeps.stream().map(bazelDep -> {
            return convertExternalJarBazelLocalPath(config, bazelInfo, bazelDep);
        }).forEach(hatchejModel.getLibraryDependencies()::add);
        /*
         * Creating a CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelClean", false/*only search the root project*/).isEmpty()) {
            final BazelCleanTask bazelCleanTask = (BazelCleanTask) rootProject.task(Collections.singletonMap("type", BazelCleanTask.class), "bazelClean");
            bazelCleanTask.setBazelConfig(config);
            rootProject.getTasks().findByPath(":clean").dependsOn(bazelCleanTask);
        }

        /*
         * Creating a SUPER-CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelExpungeClean", false/*only search the root project*/).isEmpty()) {
            final BazelConfigTask task = (BazelConfigTask) rootProject.task(Collections.singletonMap("type", BazelExpungeTask.class), "bazelExpungeClean");
            task.setBazelConfig(config);
        }

        /*
         * Adding tests
         */
        if (config.testTargetName != null && config.testTargetName.length() > 0) {
            final Strategy testStrategy = Factory.buildStrategy(aspectRunner.getAspectResult("get_rule_kind.bzl", config.testTargetName).stream().findFirst().orElse("java_test"), config);
            testStrategy.createBazelExecTask(project);
            hatchejModel.getTestSourceFolders().addAll(getSourceFoldersFromBazelAspect(rootProject, aspectRunner, config.testTargetName));
        }

        try {
            HatchejImlAction hatchejImlAction = new HatchejImlAction();
            hatchejImlAction.modifyImlFile(project, hatchejModel);
            hatchejImlAction.addLibraryFiles(project, hatchejModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String convertExternalJarBazelLocalPath(BazelLeafConfig.Decorated config,
                                                           Properties bazelInfo,
                                                           String pathToExternalFile) {
        //converts
        //external/com_google_guava_guava/jar/guava-20.0.jar
        //to
        //FULL_PATH_TO_REPO/build/bazel-build/bazel-leaf/external/com_google_guava_guava/jar/guava-20.0.jar
        return String.format("%s/%s", bazelInfo.getProperty("output_base"), pathToExternalFile);
    }

    private static String convertLocalBazelDepToGradle(String bazelDep) {
        // "//andlib/innerandlib:inneraar" -> ":andlib:innerandlib"
        // "//lib4:jar" -> ":lib4"
        final Matcher localModulesPattern = Pattern.compile("^/(/.+):.+$").matcher(bazelDep);
        if (localModulesPattern.matches()) {
            return localModulesPattern.group(1).replace("/", ":");
        } else {
            throw new IllegalArgumentException("The Bazel dep '" + bazelDep + "' can not be converted into a local Gradle dependency");
        }
    }

    private static Deps getAllDepsFromBazel(AspectRunner aspectRunner, String targetName) {
        //"//andlib/innerandlib:inneraar<FILES:>[<generated file andlib/innerandlib/inneraar.srcjar>"
        final Pattern localModulesPattern = Pattern.compile("^(//.+)<FILES:>.*$");
        //"@com_google_guava_guava//jar:jar<FILES:>[<source file external/com_google_guava_guava/jar/guava-20.0.jar>]"
        final Pattern remoteWorkspaceModulesPattern = Pattern.compile("^@.*<FILES:>\\[(.*)\\]$");
        //[<generated file andlib/innerandlib/inneraar.srcjar>, <generated file andlib/innerandlib/inneraar_resources-src.jar>]
        final Pattern generatedFilesPattern = Pattern.compile("<.*file\\s*(.+)>");
        return aspectRunner.getAspectResult("get_deps.bzl", targetName).stream()
                .collect(Deps::new, (deps, bazelDepAnnotation) -> {
                    final Matcher localModuleMatcher = localModulesPattern.matcher(bazelDepAnnotation);
                    final Matcher remoteWorkspaceMatcher = remoteWorkspaceModulesPattern.matcher(bazelDepAnnotation);
                    if (localModuleMatcher.matches()) {
                        deps.moduleDeps.add(localModuleMatcher.group(1));
                    } else if (remoteWorkspaceMatcher.matches()) {
                        final Matcher generateFilesMatcher = generatedFilesPattern.matcher(remoteWorkspaceMatcher.group(1));
                        if (generateFilesMatcher.matches()) {
                            for (int matchIndex = 1; matchIndex < generateFilesMatcher.groupCount() + 1; matchIndex++) {
                                deps.remoteWorkspaceDeps.add(generateFilesMatcher.group(matchIndex));
                            }
                        }
                    } else {
                        throw new IllegalStateException("the bazel dep '" + bazelDepAnnotation + "' does not match any known annotations.");
                    }
                }, (deps, deps2) -> {
                    deps.moduleDeps.addAll(deps2.moduleDeps);
                    deps.remoteWorkspaceDeps.addAll(deps2.remoteWorkspaceDeps);
                });
    }

    private static class Deps {
        public final List<String> moduleDeps = new ArrayList<>();
        public final List<String> remoteWorkspaceDeps = new ArrayList<>();
    }

    private static Set<File> getSourceFoldersFromBazelAspect(Project rootProject, AspectRunner runner, String targetName) {
        final Map<File, String> packageByFolder = new HashMap<>();

        return runner.getAspectResult("get_source_files.bzl", targetName).stream()
                .map(File::new)
                //we need the root-project since the WORKSPACE file is there.
                .map(rootProject::file)
                .map(sourceFile -> {
                    File parent = sourceFile.getParentFile();
                    String packageInFolder = packageByFolder.computeIfAbsent(parent, fileNotUsedHere -> parseDeclaredPackage(sourceFile));
                    final String parentFullPath = parent.getPath();
                    //removing the package folders, we only want the root folder
                    return new File(parentFullPath.substring(0, parentFullPath.length() - packageInFolder.length()));
                })
                .distinct()
                .collect(Collectors.toSet());
    }

    //taken from https://github.com/bazelbuild/intellij/blob/master/aspect/tools/src/com/google/idea/blaze/aspect/PackageParser.java#L163
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w\\.]+);$");

    @Nullable
    private static String parseDeclaredPackage(File sourceFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher packageMatch = PACKAGE_PATTERN.matcher(line);
                if (packageMatch.find()) {
                    return packageMatch.group(1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse java package for " + sourceFile, e);
        }
        return null;
    }
}
