package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;


public abstract class BazelExecTaskBase extends DefaultTask implements BazelConfigTask {

    private BazelLeafConfig.Decorated mConfig;

    @TaskAction
    public void bazelExec() {
        BazelExecHelper.BazelExec bazelExec = createBazelExec(mConfig);
        try {
            bazelExec.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to clean using Bazel", e);
        }
    }

    protected abstract BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config);

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }
}
