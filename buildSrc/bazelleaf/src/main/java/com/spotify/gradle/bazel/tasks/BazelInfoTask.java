package com.spotify.gradle.bazel.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;
import com.spotify.gradle.bazel.utils.LoggerWithFlush;

import org.gradle.wrapper.Logger;

import javax.inject.Inject;

/**
 * A variant of {@link BazelExecTaskBase} that performs a `bazel info` action.
 */
public class BazelInfoTask extends BazelExecTaskBase {
    private static final Logger LOGGER = new LoggerWithFlush();

    @Inject
    public BazelInfoTask() {
    }

    @VisibleForTesting
    BazelInfoTask(BazelExecHelper bazelExecHelper) {
        super(bazelExecHelper);
    }

    @Override
    protected BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config) {
        return mBazelExecHelper.createBazelRun(false, config, "", "info");
    }

    @Override
    protected void onSuccessfulRun(BazelExecHelper.RunResult runResult) {
        super.onSuccessfulRun(runResult);
        runResult.getExecutionOutput().forEach(LOGGER::log);
    }

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        super.setBazelConfig(config);
        setDescription("Prints out information about Bazel workspace");
        setGroup("others");
    }
}
