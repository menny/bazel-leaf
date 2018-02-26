package com.spotify.gradle.bazel;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BazelLeafConfig {

    private String mTarget;

    private String mTestTarget;

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

    public Decorated decorate(Project project) {
        verifyConfigured();

        final String projectGradlePath = project.getPath();
        final String outputPath = projectGradlePath.replace(":", "/");

        return new Decorated(
                getBazelBinPath(project),
                "/" + outputPath.replace(":", "/"),
                mTarget,
                mTestTarget,
                project.getRootProject().getProjectDir(),
                project.getRootProject().getBuildDir().getAbsolutePath() + "/bazel-build/");
    }

    public static class Decorated {
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

        public final String bazelBin;
        public final String targetPath;
        public final String targetName;
        public final File workspaceRootFolder;
        public final String testTargetName;
        public final String buildOutputDir;
    }

    private static final String DEFAULT_BIN_PATH = "/usr/local/bin/bazel";

    private static String getBazelBinPath(Project project) {
        final Properties properties = new Properties();
        final File propertiesFile = project.getRootProject().file("local.properties");
        if (propertiesFile.exists()) {
            try (FileInputStream input = new FileInputStream(propertiesFile)) {
                properties.load(input);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return properties.getProperty("bazel.bin.path", DEFAULT_BIN_PATH);
    }
}
