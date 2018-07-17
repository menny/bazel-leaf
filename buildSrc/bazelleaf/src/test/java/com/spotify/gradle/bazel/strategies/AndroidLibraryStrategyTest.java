package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.TestUtils;
import com.spotify.gradle.bazel.tasks.BazelBuildTask;

import org.gradle.api.Project;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("PMD.AlwaysSpecifyTestRunner")
public class AndroidLibraryStrategyTest {

    @Test
    public void testConfiguration() {
        Project project = TestUtils.createConfiguredAppliedProject();

        BazelLeafConfig.Decorated decoratedConfig = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        AndroidLibraryStrategy androidLibraryStrategy = new AndroidLibraryStrategy(decoratedConfig);
        BazelBuildTask createdTask =
                (BazelBuildTask) androidLibraryStrategy.createBazelExecTask(project);
        assertThat(createdTask, IsNull.notNullValue());

        assertThat(createdTask.getDescription(), IsEqual
                .equalTo(String.format("Compiles Bazel target %s:%s", "targetPath", "targetName.aar")));
    }
}
