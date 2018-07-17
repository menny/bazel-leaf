package com.spotify.gradle.bazel.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.plugins.BasePlugin;

import javax.inject.Inject;

/**
 * A variant of {@link BazelExecTaskBase} that performs a `bazel build` action.
 */
public class BazelBuildTask extends BazelExecTaskBase {

    @Inject
    public BazelBuildTask() {
    }

    @VisibleForTesting
    BazelBuildTask(BazelExecHelper bazelExecHelper) {
        super(bazelExecHelper);
    }

    @Override
    protected BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config) {
        return mBazelExecHelper.createBazelRun(true, config, config.targetName, "build");
    }

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated bazelConfig) {
        super.setBazelConfig(bazelConfig);
        setGroup(BasePlugin.BUILD_GROUP);
        setDescription("Compiles Bazel target " + bazelConfig.targetPath + ':' + bazelConfig.targetName);
    }
}
