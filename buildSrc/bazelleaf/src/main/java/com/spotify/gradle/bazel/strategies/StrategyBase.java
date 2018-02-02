package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.BazelPublishArtifact;

import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

abstract class StrategyBase implements Strategy {

    final BazelLeafConfig.Decorated mConfig;

    protected StrategyBase(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    protected abstract String generateBazelExecTaskName(Project project);

    @Override
    public Exec createBazelExecTask(Project project) {
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), generateBazelExecTaskName(project));
        bazelBuildTask.setWorkingDir(mConfig.workspaceRootFolder);
        final String bazelBuildTarget = mConfig.targetPath + ":" + mConfig.targetName;
        bazelBuildTask.setCommandLine(mConfig.bazelBin, "build", "--symlink_prefix=" + mConfig.buildOutputDir, bazelBuildTarget);
        bazelBuildTask.setDescription("Assembles this project using Bazel target " + bazelBuildTarget);
        bazelBuildTask.setGroup(BasePlugin.BUILD_GROUP);
        return bazelBuildTask;
    }

    private File generateFileForOutput(String filename) {
        return new File(mConfig.workspaceRootFolder, filename);
    }

    @Override
    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Exec bazelExecTask) {
        return aspectRunner.getAspectResult("get_rule_outs.bzl", mConfig.targetName).stream()
                .map(this::generateFileForOutput)
                .map(artifactFile -> new BazelPublishArtifact(bazelExecTask, artifactFile))
                .collect(Collectors.toList());
    }

}
