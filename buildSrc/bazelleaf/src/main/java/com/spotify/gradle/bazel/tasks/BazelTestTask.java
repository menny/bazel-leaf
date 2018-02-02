package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;


public class BazelTestTask extends DefaultTask {

    private BazelLeafConfig.Decorated mConfig;

    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        mConfig = config;
        setDescription("Tests Bazel target " + mConfig.targetPath + ":" + mConfig.testTargetName);
        setGroup("verification");
    }

    @TaskAction
    public void testUsingBazel() {
        BazelExecHelper.BazelExec bazelExec = BazelExecHelper.createBazelRun(mConfig, mConfig.testTargetName, "test");
        try {
            bazelExec.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to test using Bazel", e);
        }
    }
}
