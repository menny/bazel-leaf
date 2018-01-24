package com.spotify.gradle.bazel;

public class BazelLeafConfig {

    private String target;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    BazelLeafConfig verifyConfigured() {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("Specify the target to build using Bazel, by adding \"bazel {target = 'target-name'}\" to the module's build.gradle file.");
        }
        return this;
    }
}
