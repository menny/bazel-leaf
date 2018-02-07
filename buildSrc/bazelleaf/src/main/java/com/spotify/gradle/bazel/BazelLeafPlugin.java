package com.spotify.gradle.bazel;

import com.spotify.gradle.bazel.strategies.Factory;
import com.spotify.gradle.bazel.strategies.Strategy;
import com.spotify.gradle.bazel.tasks.BazelCleanTask;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        /*
         * Adding build artifacts
         */
        strategy.getBazelArtifacts(aspectRunner, project, bazelBuildTask)
                .forEach(bazelPublishArtifact -> {
                            defaultConfiguration.getOutgoing().getArtifacts().add(bazelPublishArtifact);
                        }
                );
        /*
         * Setting up project dependencies
         */
        final Deps targetDeps = getAllDepsFromBazel(aspectRunner, config.targetName);
        targetDeps.moduleDeps.stream().map(BazelLeafPlugin::convertLocalBazelDepToGradle).forEach(gradlePathToDep -> {
            //this will add the dependency to the default configuration.
            final ProjectDependency path = (ProjectDependency) project.getDependencies().project(Collections.singletonMap("path", gradlePathToDep));
            System.out.println("Adding dependency " + path + " to " + project.getPath());
            defaultConfiguration.getDependencies().add(path);
            /*defaultConfiguration.getIncoming().beforeResolve(new Action<ResolvableDependencies>() {
                @Override
                public void execute(ResolvableDependencies resolvableDependencies) {
                    resolvableDependencies.getArtifacts().getArtifacts().add(path.getArtifacts().iterator().next().)
                }
            });*/
        });

        //targetDeps.remoteWorkspaceDeps.forEach(d -> System.out.println("remote dep: " + d));

        /*
         * Exclude bazel build directories for IntelliJ's indexing
         * Adds <exclude .../> to the root project iml file
         */
        if (!rootProject.getPlugins().hasPlugin(IdeaPlugin.class)) {
            IdeaPlugin rootIdeaPlugin = (IdeaPlugin) rootProject.getPlugins().apply("idea");

            Set<File> set = new HashSet<>();
            set.add(new File(config.workspaceRootFolder, "bazel-out"));
            set.add(new File(config.workspaceRootFolder, "build/bazel_aspects"));
            set.add(new File(config.buildOutputDir));
            set.addAll(rootIdeaPlugin.getModel().getModule().getExcludeDirs());
            // set is required to override the existing values
            rootIdeaPlugin.getModel().getModule().setExcludeDirs(set);
        }

        /*
         * Applying IDEA plugin, so IntelliJ will index the source files
         */
        IdeaPlugin ideaPlugin = (IdeaPlugin) project.getPlugins().apply("idea");
        final IdeaModule ideaModule = ideaPlugin.getModel().getModule();
        final Set<File> sourceFolders = getSourceFoldersFromBazelAspect(rootProject, aspectRunner, config.targetName);
        ideaModule.setSourceDirs(sourceFolders);

        /*
         * Creating a CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelClean", false/*only search the root project*/).isEmpty()) {
            final BazelCleanTask bazelCleanTask = (BazelCleanTask) rootProject.task(Collections.singletonMap("type", BazelCleanTask.class), "bazelClean");
            bazelCleanTask.setBazelConfig(config);
            rootProject.getTasks().findByPath(":clean").dependsOn(bazelCleanTask);
        }

        /*
         * Adding tests
         */
        if (config.testTargetName != null && config.testTargetName.length() > 0) {
            final Strategy testStrategy = Factory.buildStrategy(aspectRunner.getAspectResult("get_rule_kind.bzl", config.testTargetName).stream().findFirst().orElse("java_test"), config);
            testStrategy.createBazelExecTask(project);
            ideaModule.setTestSourceDirs(getSourceFoldersFromBazelAspect(rootProject, aspectRunner, config.testTargetName));
            /*
             * Setting up test project dependencies
             */
            /*System.out.println(config.testTargetName + " deps:");
            final Deps testTargetDeps = getAllDepsFromBazel(aspectRunner, config.testTargetName);
            testTargetDeps.moduleDeps.forEach(d -> System.out.println("local dep: " + d));
            testTargetDeps.remoteWorkspaceDeps.forEach(d -> System.out.println("remote dep: " + d));*/
        }
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
        final Pattern pattern = Pattern.compile("^<.*target\\s*(.+)\\s*>$");

        // like "//andlib/innerandlib:inneraar"
        // like "//lib4:jar"
        final Pattern localModulesPattern = Pattern.compile("^\\s*(//.+)\\s*$");
        // like "@junit_junit//jar:jar"
        final Pattern remoteWorkspaceModulesPattern = Pattern.compile("^\\s*(@.+)\\s*$");

        return aspectRunner.getAspectResult("get_deps.bzl", targetName).stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .collect(Deps::new, (deps, bazelDepAnnotation) -> {
                    final Matcher localModuleMatcher = localModulesPattern.matcher(bazelDepAnnotation);
                    final Matcher remoteWorkspaceMatcher = remoteWorkspaceModulesPattern.matcher(bazelDepAnnotation);
                    if (localModuleMatcher.matches()) {
                        deps.moduleDeps.add(localModuleMatcher.group(1));
                    } else if (remoteWorkspaceMatcher.matches()) {
                        deps.remoteWorkspaceDeps.add(remoteWorkspaceMatcher.group(1));
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
