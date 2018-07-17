package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.TestUtils;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.Project;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "PMD.UseAssertThatThrowingInstead"})
public class BazelBuildTaskTest {
    public static class TestableBazelBuildTask extends BazelBuildTask {
        public final BazelExecHelper.BazelExec mockBazelExec = mock(BazelExecHelper.BazelExec.class);

        @Inject
        public TestableBazelBuildTask() {
            super(mock(BazelExecHelper.class));
            doReturn(mockBazelExec).when(mBazelExecHelper).createBazelRun(anyBoolean(), any(), anyString(), any(), any());
        }

        public BazelExecHelper getMockBazelExecHelper() {
            return mBazelExecHelper;
        }
    }

    @Test
    public void testTask() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        TestableBazelBuildTask bazelBuild = project.getTasks().create("bazelBuild", TestableBazelBuildTask.class);

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        bazelBuild.setBazelConfig(config);
        bazelBuild.bazelExec();

        verify(bazelBuild.getMockBazelExecHelper()).createBazelRun(true, config, "targetName", "build");
        verify(bazelBuild.mockBazelExec).start();

        assertThat(bazelBuild.getDescription(), IsEqual
                .equalTo("Compiles Bazel target targetPath:targetName"));
    }

    @Test
    public void testTaskStartThrowsIOException() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        TestableBazelBuildTask bazelBuild = project.getTasks().create("bazelBuild", TestableBazelBuildTask.class);
        final IOException ioException = new IOException("real tasks walk away from explosions!");
        doThrow(ioException).when(bazelBuild.mockBazelExec).start();

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        try {
            bazelBuild.setBazelConfig(config);
            bazelBuild.bazelExec();
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), IsEqual.equalTo("Failed to execute task using Bazel"));
            assertThat(e.getCause(), IsEqual.equalTo(ioException));
        }

        verify(bazelBuild.getMockBazelExecHelper()).createBazelRun(true, config, "targetName", "build");
        verify(bazelBuild.mockBazelExec).start();
    }

    @Test
    public void testTaskStartThrowsInterruptedException() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        TestableBazelBuildTask bazelBuild = project.getTasks().create("bazelBuild", TestableBazelBuildTask.class);

        final InterruptedException interruptedException = new InterruptedException("real tasks walk away from explosions!");
        doThrow(interruptedException).when(bazelBuild.mockBazelExec).start();

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        try {
            bazelBuild.setBazelConfig(config);
            bazelBuild.bazelExec();
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), IsEqual.equalTo("Failed to execute task using Bazel"));
            assertThat(e.getCause(), IsEqual.equalTo(interruptedException));
        }

        verify(bazelBuild.getMockBazelExecHelper()).createBazelRun(true, config, "targetName", "build");
        verify(bazelBuild.mockBazelExec).start();
    }

    @Test
    public void testBazelBuildTaskStartThrowsUncaughtException() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        TestableBazelBuildTask bazelBuild = project.getTasks().create("bazelBuild", TestableBazelBuildTask.class);
        RuntimeException runtimeException = new RuntimeException("real tasks walk away from explosions!");
        doThrow(runtimeException).when(bazelBuild.mockBazelExec).start();

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        try {
            bazelBuild.setBazelConfig(config);
            bazelBuild.bazelExec();
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e, IsEqual.equalTo(runtimeException));
        }

        verify(bazelBuild.getMockBazelExecHelper()).createBazelRun(true, config, "targetName", "build");
        verify(bazelBuild.mockBazelExec).start();
    }
}
