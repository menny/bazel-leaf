package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.Project;

class JavaLibraryStrategy extends StrategyBase {
    JavaLibraryStrategy(BazelLeafConfig.Decorated config) {
        super(config);
    }

    @Override
    protected String generateBazelBuildTaskName(Project project) {
        return "bazelJavaLibBuild_" + mConfig.targetName;
    }
}
