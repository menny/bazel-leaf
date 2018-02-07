package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.plugins.BasePlugin;


public class BazelBuildTask extends BazelExecTaskBase {

    @Override
    protected BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config) {
        return BazelExecHelper.createBazelRun(config, config.targetName, "build");
    }

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated bazelConfig) {
        super.setBazelConfig(bazelConfig);
        setDescription("Compiles Bazel target " + bazelConfig.targetPath + ":" + bazelConfig.targetName);
        setGroup(BasePlugin.BUILD_GROUP);
    }
}
