package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.TestUtils;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "PMD.UseAssertThatThrowingInstead"})
public class DownloadBazelTaskTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void testMissingTargetFileArgument() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();
        DownloadBazelTask downloadBazel =
                project.getTasks().create("downloadBazel", DownloadBazelTask.class);

        try {
            downloadBazel.download();
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), IsEqual.equalTo("targetFile needs to be set!"));
        }
    }

    @Test
    public void testMissingDownloadUrlArgument() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();

        DownloadBazelTask downloadBazel =
                project.getTasks().create("downloadBazel", DownloadBazelTask.class);

        downloadBazel.setTargetFile(new File("targetFile"));
        try {
            downloadBazel.download();
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), IsEqual.equalTo("downloadUrl needs to be set!"));
        }
    }

    @Test
    public void testDownloadUrlDoesntExistsForOs() throws Exception {
        Project project = TestUtils.createConfiguredAppliedProject();

        System.setProperty("os.name", "mac");
        // set download value to null

        project.getExtensions().getExtraProperties().set("bazel.bin.url.macos", null);

        Exception caughtException = null;
        try {
            DownloadBazelTask.injectDownloadTask(project, "bazelBin");
        } catch (Exception e) {
            caughtException = e;
        }

        assertNotNull(caughtException);
        assertTrue(caughtException instanceof IllegalArgumentException);
        assertTrue(caughtException.getMessage().contains("The URL property to download Bazel is invalid"));
    }

    @Test
    public void testDownloadUrlExistsForOs() throws Exception {
        Project project = ProjectBuilder.builder().build();

        System.setProperty("os.name", "mac");
        // set download value to null

        project.getExtensions().getExtraProperties().set("bazel.bin.url.macos", "http://someurl.com");
        BazelLeafConfig bazelLeafConfig =
                project.getExtensions().create("somevalue", BazelLeafConfig.class);
        // normally set by the build.gradle file configuration using the plugin
        bazelLeafConfig.setTarget("targetName");
        bazelLeafConfig.setTestTarget("testTargetName");

        DownloadBazelTask injectedTask = DownloadBazelTask.injectDownloadTask(project, "bazelBin");
        assertThat(injectedTask, IsNull.notNullValue());
        assertThat(project.getTasksByName("downloadBazel", false), IsNull.notNullValue());
        assertThat(injectedTask.getDownloadUrl(), IsEqual.equalTo("http://someurl.com"));
        assertThat(injectedTask.getTargetFile(), IsEqual.equalTo(new File("bazelBin")));
    }
}
