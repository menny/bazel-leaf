package com.spotify.gradle.bazel;

import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Strategy {

    final BazelLeafConfig.Decorated mConfig;

    protected Strategy(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    public static Strategy buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new JavaLibraryStrategy(config);
            case "genrule"://a huge ugly assumption that a genrule is our macro
                return new AndroidLibraryStrategy(config);
            default:
                throw new IllegalArgumentException("Unsupported target kind " + kind + ". Currently, supporting java_library and android_library. Fix " + config.targetPath + ":" + config.targetName);
        }
    }

    public abstract Exec createBazelBuildTask(Project project);

    Exec createBazelBuildTaskInternal(Project project, String bazelTargetName, String taskName) {
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), taskName);
        bazelBuildTask.setWorkingDir(mConfig.workspaceRootFolder);
        bazelBuildTask.setCommandLine(mConfig.bazelBin, "build", "--symlink_prefix=" + mConfig.buildOutputDir, mConfig.targetPath + ":" + bazelTargetName);
        bazelBuildTask.setDescription("Assembles this project using Bazel.");
        bazelBuildTask.setGroup(BasePlugin.BUILD_GROUP);
        return bazelBuildTask;
    }

    File generateFileForOutput(String filename) {
        return new File(mConfig.workspaceRootFolder, filename);
    }

    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Exec bazelBuildTask) {
        return aspectRunner.getAspectResult("get_rule_outs.bzl").stream()
                .map(this::generateFileForOutput)
                .map(artifactFile -> new BazelPublishArtifact(bazelBuildTask, artifactFile))
                .collect(Collectors.toList());
    }

    private static class AndroidLibraryStrategy extends Strategy {

        public AndroidLibraryStrategy(BazelLeafConfig.Decorated config) {
            super(config);
        }

        @Override
        public Exec createBazelBuildTask(Project project) {
            return createBazelBuildTaskInternal(project, mConfig.targetName, "bazelAarBuild_" + mConfig.targetName);
        }

        @Override
        public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Exec bazelBuildTask) {
            final List<BazelPublishArtifact> bazelArtifacts = super.getBazelArtifacts(aspectRunner, bazelBuildTask);
            bazelArtifacts.forEach(bazelPublishArtifact -> System.out.println(bazelPublishArtifact.getFile().getAbsolutePath()));

            return bazelArtifacts;
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
    }
}
