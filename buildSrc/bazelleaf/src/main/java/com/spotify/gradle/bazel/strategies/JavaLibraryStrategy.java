package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.Project;

class JavaLibraryStrategy extends StrategyBase {
    JavaLibraryStrategy(BazelLeafConfig.Decorated config) {
        super(config);
    }

    @Override
    protected String generateBazelExecTaskName(Project project) {
        return "compileBazelJavaLib_" + mConfig.targetName;
    }
}
