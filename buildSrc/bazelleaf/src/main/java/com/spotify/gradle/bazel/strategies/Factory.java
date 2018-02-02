package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.Project;

public class Factory {

    public static Strategy buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new PlainBuildStrategy(config);
            case "android_library":
                return new AndroidLibraryStrategy(config);
            case "java_test":
                return new JavaTestStrategy(config);
            default:
                System.out.println("Unsupported target kind " + kind + ". Currently, supporting java_library, java_test and android_library. Fix " + config.targetPath + ":" + config.targetName);
                return new PlainBuildStrategy(config);
        }
    }
}
