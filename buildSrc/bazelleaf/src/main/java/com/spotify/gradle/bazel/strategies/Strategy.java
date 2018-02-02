package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelPublishArtifact;

import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;

import java.util.List;

public interface Strategy {
    Exec createBazelExecTask(Project project);

    List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Exec bazelExecTask);
}
