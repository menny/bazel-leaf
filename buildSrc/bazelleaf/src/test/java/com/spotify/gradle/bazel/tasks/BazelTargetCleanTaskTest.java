package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.TestUtils;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskOutputs;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static junit.framework.TestCase.fail;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "PMD.UseAssertThatThrowingInstead", "PMD.JUnitUseExpected"})
public class BazelTargetCleanTaskTest {

    @Test
    public void testTaskWithNoFiles() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        FileCollection outputFiles = project.files();
        BazelTargetCleanTask bazelTargetClean =
                project.getTasks().create("bazelTargetClean", BazelTargetCleanTask.class);

        Task mockTask = mock(Task.class);
        TaskOutputs outputs = mock(TaskOutputs.class);
        when(outputs.getFiles()).thenReturn(outputFiles);
        when(mockTask.getOutputs()).thenReturn(outputs);

        bazelTargetClean.addTargetTask(mockTask);
        bazelTargetClean.deleteOutputs();

        verify(mockTask).getOutputs();
    }

    @Test
    public void testTaskWithNonExistentFiles() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        FileCollection outputFiles = project.files(new File("1"), new File("2"));
        BazelTargetCleanTask bazelTargetClean =
                project.getTasks().create("bazelTargetClean", BazelTargetCleanTask.class);

        Task mockTask = mock(Task.class);
        TaskOutputs outputs = mock(TaskOutputs.class);
        when(outputs.getFiles()).thenReturn(outputFiles);
        when(mockTask.getOutputs()).thenReturn(outputs);

        bazelTargetClean.addTargetTask(mockTask);
        bazelTargetClean.deleteOutputs();

        verify(mockTask).getOutputs();
    }

    @Test
    public void testTaskWitExistentFiles() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        File temp1 = File.createTempFile("file", ".tmp");
        File temp2 = File.createTempFile("file", ".tmp");
        // precaution to attempt to clean up if there is a failure with the task
        temp1.deleteOnExit();
        temp2.deleteOnExit();

        assertThat(temp1.exists(), is(true));
        assertThat(temp2.exists(), is(true));
        FileCollection outputFiles = project.files(temp1, temp2);
        BazelTargetCleanTask bazelTargetClean =
                project.getTasks().create("bazelTargetClean", BazelTargetCleanTask.class);

        Task mockTask = mock(Task.class);
        TaskOutputs outputs = mock(TaskOutputs.class);
        when(outputs.getFiles()).thenReturn(outputFiles);
        when(mockTask.getOutputs()).thenReturn(outputs);

        bazelTargetClean.addTargetTask(mockTask);
        bazelTargetClean.deleteOutputs();

        verify(mockTask).getOutputs();

        assertThat(temp1.exists(), is(false));
        assertThat(temp2.exists(), is(false));
    }

    @Test
    public void testTaskWithDeleteFailure() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        File undeleteableFile = new File("nonexistentfile.tmp");
        undeleteableFile.delete(); // just in case
        assertThat(undeleteableFile.exists(), is(false));
        undeleteableFile = spy(undeleteableFile);

        // fake the exists call to return true
        doReturn(true).when(undeleteableFile).exists();
        // force it to return false when delete is called
        doReturn(false).when(undeleteableFile).delete();

        FileCollection outputFiles = project.getLayout().files(undeleteableFile);
        outputFiles = spy(outputFiles);
        doReturn(Collections.singleton(undeleteableFile)).when(outputFiles).getFiles();

        BazelTargetCleanTask bazelTargetClean =
                project.getTasks().create("bazelTargetClean", BazelTargetCleanTask.class);

        Task mockTask = mock(Task.class);
        TaskOutputs outputs = mock(TaskOutputs.class);
        when(outputs.getFiles()).thenReturn(outputFiles);
        when(mockTask.getOutputs()).thenReturn(outputs);

        bazelTargetClean.addTargetTask(mockTask);
        try {
            bazelTargetClean.deleteOutputs();
            fail("Expected failure because of file deletion failure");
        } catch (IOException e) {
            // no-op expected
        }
        verify(mockTask).getOutputs();
        verify(undeleteableFile).exists();
        verify(undeleteableFile).delete();
    }
}
