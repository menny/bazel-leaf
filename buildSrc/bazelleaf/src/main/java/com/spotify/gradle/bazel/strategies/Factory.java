package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a {@link Strategy} to build Gradle bridges to the to provided Bazel rule.
 */
public final class Factory {
    private static final Logger LOGGER = LoggerFactory.getLogger(Factory.class);

    private Factory() {
        /*this is a static factory*/
    }

    public static Strategy buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new PlainBuildStrategy(config);
            case "android_library":
                return new AndroidLibraryStrategy(config);
            case "java_test":
            case "android_local_test":
                return new JavaTestStrategy(config);
            default:
                LOGGER.warn("Unsupported target kind '%s'. Currently, supporting java_library, java_test and android_library. Fix '%s:%s'",
                        kind, config.targetPath, config.targetName);
                return new PlainBuildStrategy(config);
        }
    }
}
