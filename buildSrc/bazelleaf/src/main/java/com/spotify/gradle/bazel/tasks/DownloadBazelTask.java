package com.spotify.gradle.bazel.tasks;

import static org.gradle.util.GUtil.map;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;

/**
 * A Gradle task that download Bazel from the provided URL.
 * See {@link #injectDownloadTask(Project, BazelLeafConfig.Decorated)} for default usage.
 */
public class DownloadBazelTask extends DefaultTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadBazelTask.class);

    private File mTargetFile;
    private String mDownloadUrl;

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

        if (!mTargetFile.getParentFile().exists() && !mTargetFile.getParentFile().mkdirs()) {
            throw new IOException("Failed to create parent folder for " + mTargetFile.getAbsolutePath());
        }
        LOGGER.info("Downloading Bazel binary from %s to %s...", mDownloadUrl, mTargetFile);

        try {
            getAnt().invokeMethod("get", map(
                    "src", new URL(mDownloadUrl),
                    "dest", mTargetFile,
                    "retries", 2));

            getAnt().invokeMethod("chmod", map(
                    "file", mTargetFile,
                    "perm", "+x"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to download Bazel to " + mTargetFile + "! This could be a target path issue.", e);
        }
    }

    /**
     * Creates (if required) download tasks for the configured platforms.
     * Essentially, will look at `gradle.properties` file, and will create a {@link DownloadBazelTask} for each configured
     * platform (donated by `bazel.bin.url.linux`, `bazel.bin.url.macos` and `bazel.bin.url.windows`).
     */
    public static DownloadBazelTask injectDownloadTask(Project project, BazelLeafConfig.Decorated config) {
        Project rootProject = project.getRootProject();

        final String osName = System.getProperty("os.name", "none").toLowerCase(Locale.US);
        final Object urlPropertyName;

        if (osName.contains("mac") || osName.contains("darwin")) {
            LOGGER.info("Detected macOS.");
            urlPropertyName = rootProject.getProperties().get("bazel.bin.url.macos");
        } else if (osName.contains("win")) {
            LOGGER.info("Detected Windows.");
            urlPropertyName = rootProject.getProperties().get("bazel.bin.url.windows");
        } else {
            LOGGER.info("Detected Linux.");
            urlPropertyName = rootProject.getProperties().get("bazel.bin.url.linux");
        }

        return injectTask(rootProject, urlPropertyName, config.bazelBin);
    }

    private static DownloadBazelTask injectTask(Project rootProject, Object urlValue, String bazelBinPath) {
        if (urlValue instanceof String && !((String) urlValue).isEmpty()) {
            final String url = (String) urlValue;
            final String downloadBazelTaskName = "downloadBazel";
            final Set<Task> tasksByName = rootProject.getTasksByName(downloadBazelTaskName, false);
            if (tasksByName.isEmpty()) {
                final DownloadBazelTask downloadBazelTask = rootProject.getTasks().create(downloadBazelTaskName, DownloadBazelTask.class);
                downloadBazelTask.setDownloadUrl(url);
                downloadBazelTask.setTargetFile(new File(bazelBinPath));

                return downloadBazelTask;
            } else {
                return (DownloadBazelTask) tasksByName.stream().findFirst().get();
            }
        }

        return null;
    }
}
