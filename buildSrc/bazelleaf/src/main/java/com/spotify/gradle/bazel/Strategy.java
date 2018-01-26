package com.spotify.gradle.bazel;

import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;

import java.io.File;
import java.util.Collections;

public abstract class Strategy {

    final BazelLeafConfig.Decorated mConfig;

    protected Strategy(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    public static Strategy buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new JavaLibraryStrategy(config);
            case "android_library":
                return new AndroidLibraryStrategy(config);
            default:
                throw new IllegalArgumentException("Unsupported target kind " + kind +". Currently, supporting java_library and android_library. Fix " + config.targetPath + ":" + config.targetName);
        }
    }

    public abstract Exec createBazelBuildTask(Project project);

    Exec createBazelBuildTaskInternal(Project project, String bazelTargetName, String taskName) {
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), taskName);
        bazelBuildTask.setWorkingDir(project.getRootDir());
        bazelBuildTask.setCommandLine(mConfig.bazelBin, "build", "--symlink_prefix=" + mConfig.buildOutputDir + "/", mConfig.targetPath + ":" + bazelTargetName);
        bazelBuildTask.setDescription("Assembles this project using Bazel.");
        bazelBuildTask.setGroup(BasePlugin.BUILD_GROUP);
        return bazelBuildTask;
    }

    File generateFileForOutput(String filename) {
        return new File(mConfig.buildOutputDir + "/bin/" + mConfig.targetPath.replace("/", "_") + "/" + filename);
    }

    public abstract BazelPublishArtifact getBazelArtifact(AspectRunner aspectRunner, Exec bazelBuildTask);

    private static class AndroidLibraryStrategy extends Strategy {

        private static final String ANDROID_TARGET_NAME_PREFIX = "actual_android_";

        public AndroidLibraryStrategy(BazelLeafConfig.Decorated config) {
            super(config);
        }

        @Override
        public Exec createBazelBuildTask(Project project) {
            return createBazelBuildTaskInternal(project, ANDROID_TARGET_NAME_PREFIX + mConfig.targetName, "bazelAarBuild_" + mConfig.targetName);
        }

        @Override
        public BazelPublishArtifact getBazelArtifact(AspectRunner aspectRunner, Exec bazelBuildTask) {
            final File file = generateFileForOutput(ANDROID_TARGET_NAME_PREFIX + mConfig.targetName + ".aar");
            return new BazelPublishArtifact(bazelBuildTask, file);
        }
    }

    private static class JavaLibraryStrategy extends Strategy {
        public JavaLibraryStrategy(BazelLeafConfig.Decorated config) {
            super(config);
        }

        @Override
        public Exec createBazelBuildTask(Project project) {
            return createBazelBuildTaskInternal(project, mConfig.targetName, "bazelJavaLibBuild_" + mConfig.targetName);
        }

        @Override
        public BazelPublishArtifact getBazelArtifact(AspectRunner aspectRunner, Exec bazelBuildTask) {
            final File file = generateFileForOutput("lib" + mConfig.targetName + ".jar");
            return new BazelPublishArtifact(bazelBuildTask, file);
        }
    }
}
