package com.spotify.gradle.bazel.utils;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "PMD.UsingExpectedTestAnnotationParameter"})
public class BazelExecHelperTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testEmptyOutput() throws Exception {
        BazelExecHelper spyBazelExecHelper = spy(new BazelExecHelper());

        BazelExecHelper.BazelExec bazelExec = mock(BazelExecHelper.BazelExec.class);
        doReturn(bazelExec).when(spyBazelExecHelper).createBazelRun(anyBoolean(),
                any(BazelLeafConfig.Decorated.class),
                anyString(),
                anyString());

        BazelExecHelper.RunResult runResult =
                new BazelExecHelper.RunResult(0, Collections.emptyList());
        when(bazelExec.start()).thenReturn(runResult);

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        Properties info = spyBazelExecHelper.getInfo(config);
        assertThat(info.isEmpty(), Is.is(true));
    }

    @Test
    public void testPropertyCapturing() throws Exception {
        BazelExecHelper spyBazelExecHelper = spy(new BazelExecHelper());

        BazelExecHelper.BazelExec bazelExec = mock(BazelExecHelper.BazelExec.class);
        doReturn(bazelExec).when(spyBazelExecHelper).createBazelRun(anyBoolean(),
                any(BazelLeafConfig.Decorated.class),
                anyString(),
                anyString());

        BazelExecHelper.RunResult runResult = new BazelExecHelper.RunResult(
                0,
                Arrays.asList("line1=value1", "line2=value2"));
        when(bazelExec.start()).thenReturn(runResult);

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        Properties info = spyBazelExecHelper.getInfo(config);

        assertThat(info.size(), Is.is(2));
        assertThat(info, IsMapContaining.hasEntry("line1", "value1"));
    }

    @Test(expected = IOException.class)
    public void testBazelExecFailure() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        File parentDir = temporaryFolder.newFolder();
        // have the dir creation done in the BazelExecHelper
        parentDir.delete();
        File output = new File(parentDir, "outputfile");
        File error = new File(parentDir, "errorfile");

        processBuilder.redirectOutput(output);
        processBuilder.redirectError(error);

        processBuilder.command("NONEXISTENTEXE");

        BazelExecHelper.BazelExec bazelExec = new BazelExecHelper.BazelExec(false, processBuilder);
        bazelExec.start();
    }
}
