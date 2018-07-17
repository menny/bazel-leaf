package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "checkstyle:regexpsinglelinejava"})
public class FactoryTest {

    @Test
    public void testJavaLibraryKind() {
        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        Strategy strategy = Factory.buildStrategy("java_library", config);
        assertThat(strategy, Matchers.instanceOf(PlainBuildStrategy.class));
    }

    @Test
    public void testAndroidLibraryKind() {
        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        Strategy strategy = Factory.buildStrategy("android_library", config);
        assertThat(strategy, Matchers.instanceOf(AndroidLibraryStrategy.class));
    }

    @Test
    public void testTestKind() {
        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        Strategy strategy = Factory.buildStrategy("android_local_test", config);
        assertThat(strategy, Matchers.instanceOf(JavaTestStrategy.class));

        strategy = Factory.buildStrategy("java_test", config);
        assertThat(strategy, Matchers.instanceOf(JavaTestStrategy.class));
    }

    @Test
    public void testInvalidKind() {
        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        Strategy strategy = Factory.buildStrategy("UnknownKind", config);
        assertThat(strategy, Matchers.instanceOf(PlainBuildStrategy.class));
    }
}
