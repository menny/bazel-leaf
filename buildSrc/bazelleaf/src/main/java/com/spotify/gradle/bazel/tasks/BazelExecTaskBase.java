package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

/**
 * A base class that execute the Bazel binary.
 */
public abstract class BazelExecTaskBase extends DefaultTask implements BazelConfigTask {

    protected BazelLeafConfig.Decorated mConfig;

    protected final BazelExecHelper mBazelExecHelper;

    protected BazelExecTaskBase() {
        this(new BazelExecHelper());
    }

    protected BazelExecTaskBase(BazelExecHelper bazelExecHelper) {
        mBazelExecHelper = bazelExecHelper;
    }

    @TaskAction
    public void bazelExec() {
        BazelExecHelper.BazelExec bazelExec = createBazelExec(mConfig);
        try {
            onSuccessfulRun(bazelExec.start());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute task using Bazel", e);
        }
    }

    protected void onSuccessfulRun(BazelExecHelper.RunResult runResult) {
    }

    protected abstract BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config);

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }
}
