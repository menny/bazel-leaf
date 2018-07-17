package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.TestUtils;
import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
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
public class BazelCleanTaskTest {
    public static class TestableBazelCleanTask extends BazelCleanTask {
        public final BazelExecHelper.BazelExec mockBazelExec = mock(BazelExecHelper.BazelExec.class);

        @Inject
        public TestableBazelCleanTask() {
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
        TestableBazelCleanTask bazelClean = project.getTasks().create("bazelClean", TestableBazelCleanTask.class);

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        bazelClean.setBazelConfig(config);
        bazelClean.bazelExec();

        verify(bazelClean.getMockBazelExecHelper()).createBazelRun(true, config, "", "clean");
        verify(bazelClean.mockBazelExec).start();

        assertThat(bazelClean.getDescription(), IsEqual
                .equalTo("Cleans Bazel workspace"));
        assertThat(bazelClean.getGroup(), IsEqual.equalTo(BasePlugin.BUILD_GROUP));
    }
}
