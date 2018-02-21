package com.spotify.gradle.bazel.utils;

import com.spotify.gradle.bazel.BazelLeafConfig;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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
                    mProcessBuilder.redirectOutput().file().exists() ? Files.readAllLines(mProcessBuilder.redirectOutput().file().toPath()) : Collections.emptyList(),
                    mProcessBuilder.redirectError().file().exists() ? Files.readAllLines(mProcessBuilder.redirectError().file().toPath())  : Collections.emptyList());
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

    public static Properties getInfo(BazelLeafConfig.Decorated config) {
        try {
            BazelExec infoExec = createBazelRun(config, "", "info");
            RunResult start = infoExec.start();

            StringWriter w = new StringWriter();
            PrintWriter b = new PrintWriter(w);

            start.getStandardOutput().forEach(b::println);

            b.close();

            Properties properties = new Properties();
            properties.load(new StringReader(w.toString()));
            return properties;
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void ensurePathExists(File outputFile) throws IOException {
        final File parentFile = outputFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Failed to create output folder for " + outputFile.getAbsolutePath());
        }
    }
}
