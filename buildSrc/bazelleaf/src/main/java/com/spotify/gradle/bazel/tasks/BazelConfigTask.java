package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;

/**
 * A common interface that ensures that Bazel related tasks are using the project's {@link BazelLeafConfig.Decorated}
 * configuration object.
 */
public interface BazelConfigTask {
    void setBazelConfig(BazelLeafConfig.Decorated config);
}
