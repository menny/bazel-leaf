package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.BazelPublishArtifact;
import com.spotify.gradle.bazel.tasks.BazelCompileTask;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;

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
        final BazelCompileTask bazelBuildTask = (BazelCompileTask) project.task(Collections.singletonMap("type", BazelCompileTask.class), "compile");
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
