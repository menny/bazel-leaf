package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.TestUtils;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.Project;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

import java.io.File;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "PMD.UseAssertThatThrowingInstead"})
public class BazelTestTaskTest {
    public static class TestableBazelTestTask extends BazelTestTask {
        public final BazelExecHelper.BazelExec mockBazelExec = mock(BazelExecHelper.BazelExec.class);

        @Inject
        public TestableBazelTestTask() {
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

        TestableBazelTestTask bazelTest = project.getTasks().create("bazelTest", TestableBazelTestTask.class);

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        bazelTest.setBazelConfig(config);
        bazelTest.bazelExec();

        verify(bazelTest.getMockBazelExecHelper()).createBazelRun(true, config, "testTargetName", "test");
        verify(bazelTest.mockBazelExec).start();

        assertThat(bazelTest.getDescription(), IsEqual
                .equalTo("Tests Bazel target targetPath:testTargetName"));
    }
}
