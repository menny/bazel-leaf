package com.spotify.gradle.bazel.utils;

import com.spotify.gradle.bazel.BazelLeafConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BazelExecHelper {

    public static class BazelExec {
        private final ProcessBuilder mProcessBuilder;

        BazelExec(ProcessBuilder processBuilder) {
            mProcessBuilder = processBuilder;
        }

        public ProcessBuilder getProcessBuilder() {
            return mProcessBuilder;
        }

        public RunResult start() throws IOException, InterruptedException {
            ensurePathExists(mProcessBuilder.redirectOutput().file());
            ensurePathExists(mProcessBuilder.redirectError().file());

            final int exitCode = mProcessBuilder.start().waitFor();
            final RunResult result = new RunResult(exitCode,
                    Files.readAllLines(mProcessBuilder.redirectOutput().file().toPath()),
                    Files.readAllLines(mProcessBuilder.redirectError().file().toPath()));
            if (exitCode != 0) {
                result.getErrorOutput().forEach(log -> System.err.println("Bazel error: " + log));
                result.getStandardOutput().forEach(log -> System.out.println("Bazel std: " + log));
                throw new IOException("Got process exit code " + exitCode + " when running bazel " + toString());
            }

            return result;
        }

        @Override
        public String toString() {
            return "BazelExec: " + Arrays.toString(mProcessBuilder.command().toArray(new String[0])) + ". Output to " + mProcessBuilder.redirectError().file().getAbsolutePath();
        }
    }

    public static class RunResult {
        private final int mExitCode;
        private final List<String> mStandardOutput;
        private final List<String> mErrorOutput;

        RunResult(int exitCode, List<String> standardOutput, List<String> errorOutput) {
            mExitCode = exitCode;
            mStandardOutput = standardOutput;
            mErrorOutput = errorOutput;
        }

        public int getExitCode() {
            return mExitCode;
        }

        public List<String> getStandardOutput() {
            return mStandardOutput;
        }

        public List<String> getErrorOutput() {
            return mErrorOutput;
        }
    }

    public static BazelExec createBazelRun(BazelLeafConfig.Decorated config, String target, String bazelCommand, String... args) {
        ProcessBuilder builder = new ProcessBuilder();
        ArrayList<String> execArgs = new ArrayList<>(Arrays.asList(config.bazelBin, bazelCommand, "--symlink_prefix=" + config.buildOutputDir));
        if (target != null && target.length() > 0) {
            execArgs.add(config.targetPath + ":" + target);
        }
        execArgs.addAll(Arrays.asList(args));
        builder.command(execArgs);
        builder.directory(config.workspaceRootFolder);

        builder.redirectOutput(new File(config.buildOutputDir, config.targetPath + "/runner_" + bazelCommand + "/" + target + ".txt"));
        builder.redirectError(new File(config.buildOutputDir, config.targetPath + "/runner_" + bazelCommand + "/" + target + ".err"));

        return new BazelExec(builder);
    }

    private static void ensurePathExists(File outputFile) throws IOException {
        final File parentFile = outputFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Failed to create output folder for " + outputFile.getAbsolutePath());
        }
    }
}
