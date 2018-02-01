package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.Project;

public class Factory {

    public static StrategyBase buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new JavaLibraryStrategy(config);
            case "android_library":
                return new AndroidLibraryStrategy(config);
            default:
                System.out.println("Unsupported target kind " + kind + ". Currently, supporting java_library and android_library. Fix " + config.targetPath + ":" + config.targetName);
                return new PlainBazelBuildStrategy(config);
        }
    }

    private static class PlainBazelBuildStrategy extends StrategyBase {
        public PlainBazelBuildStrategy(BazelLeafConfig.Decorated config) {
            super(config);
        }

        @Override
        protected String generateBazelBuildTaskName(Project project) {
            return "bazelBuild_" + mConfig.targetName;
        }
    }
}
