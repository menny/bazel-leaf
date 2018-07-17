package com.spotify.gradle.bazel;

import com.spotify.gradle.bazel.utils.SystemEnvironment;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

public class BazelLeafConfig {

    /**
     * first: user's home folder.
     * second: project name.
     * third: extension for OS.
     */
    private static final String DEFAULT_BIN_PATH_TEMPLATE = "%s/.bazel-leaf/%s/bazel%s";
    private String mTarget;
    private String mTestTarget;

    public static String getBazelBinPath(Project project) {
        final Properties properties = new Properties();
        final File propertiesFile = project.getRootProject().file("local.properties");
        if (propertiesFile.exists()) {
            try (FileInputStream input = new FileInputStream(propertiesFile)) {
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return properties.getProperty("bazel.bin.path", getDefaultBazelBinPath(project));
    }

    private static String getDefaultBazelBinPath(Project project) {
        return String.format(Locale.US, DEFAULT_BIN_PATH_TEMPLATE,
                System.getProperty("user.home", "/usr/bin"),
                project.getRootProject().getName(),
                SystemEnvironment.OsType.Windows.equals(SystemEnvironment.getOsType())
                        ? ".exe"
                        : "");
    }

    public String getTarget() {
        return mTarget;
    }

    public void setTarget(String target) {
        mTarget = target;
    }

    public String getTestTarget() {
        return mTestTarget;
    }

    public void setTestTarget(String testTarget) {
        mTestTarget = testTarget;
    }

    private BazelLeafConfig verifyConfigured() {
        if (mTarget == null || mTarget.isEmpty()) {
            throw new IllegalArgumentException("Specify the target to build using Bazel, by adding \"bazel {target = 'target-name'}\" to the module's build.gradle file.");
        }
        return this;
    }

    public Decorated decorate(Project project, String bazelBinPath) {
        verifyConfigured();

        final String projectGradlePath = project.getPath();
        final String outputPath = projectGradlePath.replace(":", "/");

        return new Decorated(
                bazelBinPath,
                "/" + outputPath.replace(":", "/"),
                getTarget(),
                getTestTarget(),
                project.getRootProject().getProjectDir(),
                project.getRootProject().getBuildDir().getAbsolutePath() + "/bazel-build/");
    }

    public static class Decorated {

        public final String bazelBin;
        public final String targetPath;
        public final String targetName;
        public final File workspaceRootFolder;
        public final String testTargetName;
        public final String buildOutputDir;

        public Decorated(
                String bazelBin,
                String targetPath,
                String targetName,
                String testTargetName,
                File workspaceRootFolder,
                String buildOutputDir) {
            this.bazelBin = bazelBin;
            this.targetPath = targetPath;
            this.targetName = targetName;
            this.testTargetName = testTargetName;
            this.buildOutputDir = buildOutputDir;
            this.workspaceRootFolder = workspaceRootFolder;
        }
    }
}
