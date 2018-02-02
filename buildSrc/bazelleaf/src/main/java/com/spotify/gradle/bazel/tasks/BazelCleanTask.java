package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;


public class BazelCleanTask extends DefaultTask {

    private BazelLeafConfig.Decorated mConfig;

    @TaskAction
    public void cleanUsingBazel() {
        BazelExecHelper.BazelExec bazelExec = BazelExecHelper.createBazelRun(mConfig, "", "clean");
        try {
            bazelExec.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to clean using Bazel", e);
        }
    }

    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        mConfig = config;
        setDescription("Cleans Bazel workspace");
        setGroup(BasePlugin.BUILD_GROUP);
    }
}
