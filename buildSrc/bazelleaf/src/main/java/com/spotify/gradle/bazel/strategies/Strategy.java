package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelPublishArtifact;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.List;

public interface Strategy {
    Task createBazelExecTask(Project project);

    List<BazelPublishArtifact> getBazelArtifacts(
            AspectRunner aspectRunner,
            Project project,
            Task bazelExecTask);
}
