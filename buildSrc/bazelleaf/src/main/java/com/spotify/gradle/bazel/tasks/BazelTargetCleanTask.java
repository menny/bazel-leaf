package com.spotify.gradle.bazel.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link Task} that deletes all the outputs of the given task.
 */
public class BazelTargetCleanTask extends DefaultTask {

    private final Set<Task> mTargetTasks = new HashSet<>();

    @TaskAction
    public void deleteOutputs() throws IOException {
        if (mTargetTasks.isEmpty()) {
            throw new IllegalArgumentException("addTargetTask was not called for this clean task");
        }

        for (Task targetTask : mTargetTasks) {
            for (File outputFile : targetTask.getOutputs().getFiles()) {
                if (outputFile.exists() && !outputFile.delete()) {
                    throw new IOException("Failed to delete Bazel output " + outputFile.getAbsolutePath());
                }
            }
        }
    }

    public void addTargetTask(Task targetTask) {
        mTargetTasks.add(targetTask);
    }
}
