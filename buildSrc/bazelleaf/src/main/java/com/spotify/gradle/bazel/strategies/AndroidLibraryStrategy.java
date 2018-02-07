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

class AndroidLibraryStrategy extends PlainBuildStrategy {

    AndroidLibraryStrategy(BazelLeafConfig.Decorated config) {
        super(config);
    }

    @Override
    public Task createBazelExecTask(Project project) {
        final BazelBuildTask bazelBuildTask = (BazelBuildTask) super.createBazelExecTask(project);
        //using the implicit output `aar` in this build task.
        bazelBuildTask.setBazelConfig(new BazelLeafConfig.Decorated(
                mConfig.bazelBin,
                mConfig.targetPath, mConfig.targetName + ".aar", mConfig.testTargetName,
                mConfig.workspaceRootFolder, mConfig.buildOutputDir));
        return bazelBuildTask;
    }

    @Override
    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Task bazelExecTask) {
        //since we are using the implicit output `aar` we know exactly what output to expect.
        //also, Bazel will not report the AAR output anyway.
        final File outputArtifactFolder = super.getBazelArtifacts(aspectRunner, project, bazelExecTask).get(0).getFile().getParentFile();
        final File aarOutputFile = new File(outputArtifactFolder, mConfig.targetName + ".aar");
        return Collections.singletonList(new BazelPublishArtifact(bazelExecTask, aarOutputFile));
    }
}
