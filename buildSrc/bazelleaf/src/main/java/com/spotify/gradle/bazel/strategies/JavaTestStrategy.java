package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.BazelPublishArtifact;

import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;

import java.util.Collections;
import java.util.List;

class JavaTestStrategy implements Strategy {

    private final BazelLeafConfig.Decorated mConfig;

    JavaTestStrategy(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    @Override
    public Exec createBazelExecTask(Project project) {
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), "testBazelJavaLibTest_" + mConfig.testTargetName);
        bazelBuildTask.setWorkingDir(mConfig.workspaceRootFolder);
        final String bazelTestTarget = mConfig.targetPath + ":" + mConfig.testTargetName;
        bazelBuildTask.setCommandLine(mConfig.bazelBin, "test", "--symlink_prefix=" + mConfig.buildOutputDir, bazelTestTarget);
        bazelBuildTask.setDescription("Test this project using Bazel target " + bazelTestTarget);
        bazelBuildTask.setGroup("test");
        return bazelBuildTask;
    }

    @Override
    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Exec bazelExecTask) {
        //no outputs here.
        return Collections.emptyList();
    }
}
