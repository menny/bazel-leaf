package com.spotify.gradle.bazel;

import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Usage;
import org.gradle.api.tasks.Exec;
import org.gradle.plugins.ide.idea.IdeaPlugin;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE;

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
            DataInputStream input = null;
            try {
                input = ResourceGroovyMethods.newDataInputStream(propertiesFile);
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
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), "bazelBuild");
        bazelBuildTask.setWorkingDir(project.getRootDir());
        bazelBuildTask.setCommandLine(pathToBazelBin, "build", "--symlink_prefix=" + bazelBuildDir + "/", bazelPath + ":" + config.getTarget());

        /*
         * Adding build configurations
         */
        project.getConfigurations().create("default");
        project.getConfigurations().create("implementation").getAttributes().attribute(USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_API));
        project.getConfigurations().create("runtime").getAttributes().attribute(USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME));

        /*
         * Adding build artifacts
         */
        final BazelArtifact bazelArtifact = new BazelArtifact(bazelBuildTask, new File(bazelBuildDir + "/bin/" + outputPath + "/lib" + config.getTarget() + ".jar"));

        project.getArtifacts().add("runtime", bazelArtifact);
        project.getArtifacts().add("implementation", bazelArtifact);

        /*
         * Applying IDEA plugin, so InteliJ will index the source files
         */
        IdeaPlugin ideaPlugin = (IdeaPlugin) project.getPlugins().apply("idea");
        ideaPlugin.getModel().getModule().setSourceDirs(Collections.singleton(project.file("src/main/java")));

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
}
