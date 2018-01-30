package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.Project;
import org.gradle.api.tasks.Exec;

class JavaLibraryStrategy extends StrategyBase {
    JavaLibraryStrategy(BazelLeafConfig.Decorated config) {
        super(config);
    }

    @Override
    public Exec createBazelBuildTask(Project project) {
        return createBazelBuildTaskInternal(project, mConfig.targetName, "bazelJavaLibBuild_" + mConfig.targetName);
    }
}
