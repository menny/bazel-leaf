package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.plugins.BasePlugin;

/**
 * A variant of {@link BazelExecTaskBase} that performs a `bazel clean` action.
 */
public class BazelCleanTask extends BazelExecTaskBase {

    @Override
    protected BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config) {
        return BazelExecHelper.createBazelRun(config, "", "clean");
    }

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        super.setBazelConfig(config);
        setDescription("Cleans Bazel workspace");
        setGroup(BasePlugin.BUILD_GROUP);
    }
}
