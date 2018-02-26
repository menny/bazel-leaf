package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

/**
 * A variant of {@link BazelExecTaskBase} that performs a `bazel test` action.
 */
public class BazelTestTask extends BazelExecTaskBase {

    @Override
    protected BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config) {
        return BazelExecHelper.createBazelRun(config, config.testTargetName, "test");
    }

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        super.setBazelConfig(config);
        setDescription("Tests Bazel target " + config.targetPath + ":" + config.testTargetName);
        setGroup("verification");
    }
}
