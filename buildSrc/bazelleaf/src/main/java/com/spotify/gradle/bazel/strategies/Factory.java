package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

public class Factory {
    public static StrategyBase buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new JavaLibraryStrategy(config);
            case "android_library":
                return new AndroidLibraryStrategy(config);
            default:
                throw new IllegalArgumentException("Unsupported target kind " + kind + ". Currently, supporting java_library and android_library. Fix " + config.targetPath + ":" + config.targetName);
        }
    }
}
