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
public class BazelExpungeTaskTest {
    public static class TestableBazelExpungeTask extends BazelExpungeTask {
        public final BazelExecHelper.BazelExec mockBazelExec = mock(BazelExecHelper.BazelExec.class);

        @Inject
        public TestableBazelExpungeTask() {
            super(mock(BazelExecHelper.class));
            doReturn(mockBazelExec).when(mBazelExecHelper).createBazelRun(anyBoolean(), any(), anyString(), anyString(), any());
        }

        public BazelExecHelper getMockBazelExecHelper() {
            return mBazelExecHelper;
        }
    }

    @Test
    public void testTask() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        TestableBazelExpungeTask bazelExpunge = project.getTasks().create("bazelExpunge", TestableBazelExpungeTask.class);

        BazelLeafConfig.Decorated config = new BazelLeafConfig.Decorated(
                "bazelBin",
                "targetPath",
                "targetName",
                "testTargetName",
                new File("workspaceDir"),
                "outputDir"
        );
        bazelExpunge.setBazelConfig(config);
        bazelExpunge.bazelExec();

        verify(bazelExpunge.getMockBazelExecHelper()).createBazelRun(true, config, "", "clean", "--expunge");
        verify(bazelExpunge.mockBazelExec).start();

        assertThat(bazelExpunge.getDescription(), IsEqual
                .equalTo("Cleans and expunges Bazel workspace"));
        assertThat(bazelExpunge.getGroup(), IsEqual.equalTo(BasePlugin.BUILD_GROUP));
    }
}
