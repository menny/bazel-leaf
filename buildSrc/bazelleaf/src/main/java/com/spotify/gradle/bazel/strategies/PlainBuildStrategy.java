package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.BazelPublishArtifact;
import com.spotify.gradle.bazel.tasks.BazelBuildTask;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class PlainBuildStrategy implements Strategy {
    final BazelLeafConfig.Decorated mConfig;

    PlainBuildStrategy(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    @Override
    public Task createBazelExecTask(Project project) {
        final BazelBuildTask bazelBuildTask = (BazelBuildTask) project.task(Collections.singletonMap("type", BazelBuildTask.class), "compile");
        bazelBuildTask.setBazelConfig(mConfig);
        return bazelBuildTask;
    }

    private File generateFileForOutput(String filename) {
        return new File(mConfig.workspaceRootFolder, filename);
    }

    @Override
    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Task bazelExecTask) {
        return aspectRunner.getAspectResult("get_rule_outs.bzl", mConfig.targetName).stream()
                .map(this::generateFileForOutput)
                .map(artifactFile -> new BazelPublishArtifact(bazelExecTask, artifactFile))
                .collect(Collectors.toList());
    }
}
