package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.BazelPublishArtifact;
import com.spotify.gradle.bazel.tasks.BazelTestTask;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.Collections;
import java.util.List;

class JavaTestStrategy implements Strategy {

    private final BazelLeafConfig.Decorated mConfig;

    JavaTestStrategy(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    @Override
    public Task createBazelExecTask(Project project) {
        final BazelTestTask bazelBuildTask = (BazelTestTask) project.task(Collections.singletonMap("type", BazelTestTask.class), "test");
        bazelBuildTask.setBazelConfig(mConfig);
        return bazelBuildTask;
    }

    @Override
    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Task bazelExecTask) {
        //no outputs here.
        return Collections.emptyList();
    }
}
