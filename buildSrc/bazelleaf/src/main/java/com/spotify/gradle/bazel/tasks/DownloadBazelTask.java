package com.spotify.gradle.bazel.tasks;

import static org.gradle.util.GUtil.map;

import com.spotify.gradle.bazel.BazelLeafConfig;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class DownloadBazelTask extends DefaultTask {

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
        System.out.println("Downloading Bazel binary from " + mDownloadUrl + " to " + mTargetFile + "...");

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

    public static void injectDownloadTasks(Project project, BazelLeafConfig.Decorated config) {
        Project rootProject = project.getRootProject();

        injectTask(rootProject, "downloadBazelLinux", rootProject.getProperties().get("bazel.bin.url.linux"), config.bazelBin);
        injectTask(rootProject, "downloadBazelMacOs", rootProject.getProperties().get("bazel.bin.url.macos"), config.bazelBin);
        injectTask(rootProject, "downloadBazelWindows", rootProject.getProperties().get("bazel.bin.url.windows"), config.bazelBin);
    }

    private static void injectTask(Project rootProject, String taskName, Object urlValue, String bazelBinPath) {
        if (urlValue instanceof String && ((String) urlValue).length() > 0) {
            String url = (String) urlValue;

            if (rootProject.getTasksByName(taskName, false).isEmpty()) {
                final DownloadBazelTask downloadBazelTask = rootProject.getTasks().create(taskName, DownloadBazelTask.class);
                downloadBazelTask.setDownloadUrl(url);
                downloadBazelTask.setTargetFile(new File(bazelBinPath));
            }
        }
    }
}
