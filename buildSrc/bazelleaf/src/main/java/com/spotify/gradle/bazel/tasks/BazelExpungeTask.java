package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.plugins.BasePlugin;


public class BazelExpungeTask extends BazelExecTaskBase {

    @Override
    protected BazelExecHelper.BazelExec createBazelExec(BazelLeafConfig.Decorated config) {
        return BazelExecHelper.createBazelRun(config, "", "clean", "--expunge");
    }

    @Override
    public void setBazelConfig(BazelLeafConfig.Decorated config) {
        super.setBazelConfig(config);
        setDescription("Cleans and expunges Bazel workspace");
        setGroup(BasePlugin.BUILD_GROUP);
    }
}
