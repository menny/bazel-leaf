package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;

public interface BazelConfigTask {
    void setBazelConfig(BazelLeafConfig.Decorated config);
}
