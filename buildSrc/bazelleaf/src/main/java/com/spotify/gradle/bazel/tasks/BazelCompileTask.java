package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.io.IOException;


public class BazelCompileTask extends AbstractCompile {

    private BazelLeafConfig.Decorated mConfig;

    @TaskAction
    @Override
    protected void compile() {
        BazelExecHelper.BazelExec builder = BazelExecHelper.createBazelRun(mConfig, mConfig.targetName, "build");

        try {
            builder.start();
            setDidWork(true);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to build Bazel target " + mConfig + ":" + mConfig.targetName);
        }
    }

    public void setBazelConfig(BazelLeafConfig.Decorated bazelConfig) {
        mConfig = bazelConfig;
        setDescription("Compiles Bazel target " + mConfig.targetPath + ":" + mConfig.targetName);
        setGroup(BasePlugin.BUILD_GROUP);
    }
}
