package com.spotify.gradle.bazel;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationVariant;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
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
    private static final String DEFAULT_BIN_PATH = "/usr/local/bin/bazel";

    public void apply(final Project project) {
        project.getExtensions().create("bazel", BazelLeafConfig.class);

        project.afterEvaluate(BazelLeafPlugin::configurePlugin);
    }

    private static String getBazelBinPath(Project project) {
        Properties properties = new Properties();
        File propertiesFile = project.getRootProject().file("local.properties");
        if (propertiesFile.exists()) {
            FileInputStream input = null;
            try {
                input = new FileInputStream(propertiesFile);
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return properties.getProperty("bazel.bin.path", DEFAULT_BIN_PATH);
    }

    private static void configurePlugin(Project project) {
        final BazelLeafConfig config = project.getExtensions().getByType(BazelLeafConfig.class).verifyConfigured();

        final String projectGradlePath = project.getPath();
        final String outputPath = projectGradlePath.replace(":", "/");
        final String bazelPath = "/" + outputPath.replace(":", "/");
        final Project rootProject = project.getRootProject();

        /*
         * creating a Bazel-Build task
         */
        final String pathToBazelBin = getBazelBinPath(project);
        //note: Bazel must use the same folder for all outputs, so we use the build-folder of the root-project
        final String bazelBuildDir = rootProject.getBuildDir().getAbsolutePath() + "/bazel-build";
        final String bazelTargetName = bazelPath + ":" + config.getTarget();
        final Exec bazelBuildTask = createBazelBuildTask(project, pathToBazelBin, bazelBuildDir, bazelTargetName);

        final AspectRunner aspectRunner = new AspectRunner(pathToBazelBin, project.getProjectDir().getAbsoluteFile(), bazelBuildDir, bazelTargetName);
        /*
         * Adding build configurations
         */
        final Configuration defaultConfiguration = project.getConfigurations().create(Dependency.DEFAULT_CONFIGURATION);
        defaultConfiguration.setCanBeConsumed(true);
        defaultConfiguration.setCanBeResolved(true);
        //defaultConfiguration.getAttributes().attribute(USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        //final Configuration implementationConfiguration = project.getConfigurations().create("implementation");
        //implementationConfiguration.getIncoming().getAttributes().attribute(USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_));
        //final Configuration runtimeConfiguration = project.getConfigurations().create("runtime");
        //runtimeConfiguration.getAttributes().attribute(USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));
        /*
         * Adding build artifacts
         */
        final BazelJavaLibraryArtifact bazelArtifact = new BazelJavaLibraryArtifact(bazelBuildTask, new File(bazelBuildDir + "/bin/" + outputPath + "/lib" + config.getTarget() + ".jar"));

        //defaultConfiguration.getOutgoing().artifact(bazelArtifact);
        ConfigurationVariant variant = defaultConfiguration.getOutgoing().getVariants().create("classes");
        variant.getAttributes().attribute(Usage.USAGE_ATTRIBUTE, rootProject.getObjects().named(Usage.class, Usage.JAVA_API_CLASSES));
        variant.artifact(bazelArtifact);

        /*
         * Applying IDEA plugin, so InteliJ will index the source files
         */
        IdeaPlugin ideaPlugin = (IdeaPlugin) project.getPlugins().apply("idea");
        final IdeaModule ideaModule = ideaPlugin.getModel().getModule();
        ideaModule.setSourceDirs(getSourceFoldersFromBazelAspect(rootProject, aspectRunner));
        
        /*
         * Creating a CLEAN task in the root project
         */
        if (rootProject.getTasksByName("bazelClean", false/*only search the root project*/).isEmpty()) {
            final Exec bazelCleanTask = (Exec) rootProject.task(Collections.singletonMap("type", Exec.class), "bazelClean");
            bazelCleanTask.setWorkingDir(rootProject.getRootDir());
            bazelCleanTask.setCommandLine(pathToBazelBin, "clean", "--symlink_prefix=" + bazelBuildDir + "/");

            rootProject.getTasks().findByPath(":clean").dependsOn(bazelCleanTask);
        }
    }

    private static Exec createBazelBuildTask(Project project, String pathToBazelBin, String bazelBuildDir, String bazelTargetName) {
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), "bazelBuild");
        bazelBuildTask.setWorkingDir(project.getRootDir());
        bazelBuildTask.setCommandLine(pathToBazelBin, "build", "--symlink_prefix=" + bazelBuildDir + "/", bazelTargetName);
        bazelBuildTask.setDescription("Assembles this project using Bazel.");
        bazelBuildTask.setGroup(BasePlugin.BUILD_GROUP);
        return bazelBuildTask;
    }

    private static List<String> getModuleDepsFromBazel(Project rootProject, AspectRunner aspectRunner) {
        final Pattern pattern = Pattern.compile("^<target.*//(.+):.*>$");
        return aspectRunner.getAspectResult("get_deps.bzl").stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .map(bazelDep -> ":" + bazelDep.replace("/", ":"))
                .collect(Collectors.toList());
    }

    private static Set<File> getSourceFoldersFromBazelAspect(Project rootProject, AspectRunner runner) {
        final Map<File, String> packageByFolder = new HashMap<>();

        return runner.getAspectResult("get_source_files.bzl").stream()
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
