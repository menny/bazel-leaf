package com.spotify.gradle.bazel.tasks;

import com.spotify.gradle.bazel.utils.LoggerWithFlush;
import com.spotify.gradle.bazel.utils.SystemEnvironment;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.wrapper.Download;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import static org.gradle.util.GUtil.map;

/**
 * A Gradle task that download Bazel from the provided URL.
 * See {@link #injectDownloadTask(Project, String)} for default usage.
 */
public class DownloadBazelTask extends DefaultTask {

    private static final String DOWNLOAD_TASK_NAME = "downloadBazel";
    private File mTargetFile;
    private String mDownloadUrl;

    /**
     * Creates (if required) download tasks for the configured platforms.
     * Essentially, will look at `gradle.properties` file, and will create a {@link DownloadBazelTask} for each configured
     * platform (donated by `bazel.bin.url.linux`, `bazel.bin.url.macos` and `bazel.bin.url.windows`).
     *
     * @return null if injection did not take place (which means that the root-project already has a task named 'downloadBazel').
     */
    public static DownloadBazelTask injectDownloadTask(Project project, String pathToBazelBin) {
        Project rootProject = project.getRootProject();

        if (rootProject.getTasks().findByName(DOWNLOAD_TASK_NAME) == null) {
            final Object urlPropertyName;
            switch (SystemEnvironment.getOsType()) {
                case macOs:
                    urlPropertyName = rootProject.getProperties().get("bazel.bin.url.macos");
                    break;
                case Windows:
                    urlPropertyName = rootProject.getProperties().get("bazel.bin.url.windows");
                    break;
                default:
                    urlPropertyName = rootProject.getProperties().get("bazel.bin.url.linux");
            }
            return injectTask(rootProject, urlPropertyName, pathToBazelBin);
        }

        return null;
    }

    private static DownloadBazelTask injectTask(
            Project rootProject,
            Object urlValue,
            String bazelBinPath) {
        if (urlValue instanceof String && !((String) urlValue).isEmpty()) {
            final String url = (String) urlValue;
            final DownloadBazelTask downloadBazelTask = rootProject.getTasks().create(DOWNLOAD_TASK_NAME, DownloadBazelTask.class);
            downloadBazelTask.setDownloadUrl(url);
            downloadBazelTask.setTargetFile(new File(bazelBinPath));

            return downloadBazelTask;
        } else {
            throw new IllegalArgumentException("The URL property to download Bazel is invalid. Value is " + urlValue);
        }
    }

    @Input
    public File getTargetFile() {
        return mTargetFile;
    }

    public void setTargetFile(File targetFile) {
        mTargetFile = targetFile;
    }

    @Input
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        mDownloadUrl = downloadUrl;
    }

    @TaskAction
    public void download() throws IOException {
        if (mTargetFile == null) {
            throw new NullPointerException("targetFile needs to be set!");
        }

        if (mDownloadUrl == null) {
            throw new NullPointerException("downloadUrl needs to be set!");
        }

        File canonicalFileTargetFile = mTargetFile.getCanonicalFile();
        if (canonicalFileTargetFile.getParentFile() == null || (
                !canonicalFileTargetFile.getParentFile().exists() && !canonicalFileTargetFile
                        .getParentFile().mkdirs())) {
            throw new IOException(
                    "Failed to create parent folder for " + canonicalFileTargetFile.getAbsolutePath());
        }

        try {
            final Download downloader = new Download(
                    new DownloadProgressLogger(mDownloadUrl, canonicalFileTargetFile.getAbsolutePath()),
                    "bazel-leaf", "0.0.1");
            downloader.download(URI.create(mDownloadUrl), canonicalFileTargetFile);

            getAnt().invokeMethod("chmod", map(
                    "file", canonicalFileTargetFile,
                    "perm", "+x"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to download Bazel to " + canonicalFileTargetFile
                    + "! This could be a target path issue.", e);
        }
    }

    private static class DownloadProgressLogger extends LoggerWithFlush {
        private DownloadProgressLogger(String downloadUrl, String targetFile) {
            append(String.format(Locale.US, "Downloading Bazel binary from %s to %s...", downloadUrl, targetFile));
        }
    }
}
