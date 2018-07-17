package com.spotify.gradle.bazel.utils;

import com.spotify.gradle.bazel.BazelLeafConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * General utilities for executing actions with Bazel binary.
 */
public class BazelExecHelper {

    private static final LoggerWithFlush LOGGER = new LoggerWithFlush();
    private static final Collection<String> BASIC_EXEC_ARGS = Arrays.asList("--curses=no", "--color=no", "--experimental_ui=no", "--progress_in_terminal_title=no");

    public static class BazelExec {
        private final boolean mOutputToConsole;
        private final ProcessBuilder mProcessBuilder;

        BazelExec(boolean outputToConsole, ProcessBuilder processBuilder) {
            mOutputToConsole = outputToConsole;
            mProcessBuilder = processBuilder;
        }

        public RunResult start() throws IOException, InterruptedException {
            mProcessBuilder.redirectErrorStream(true);
            final Process process = mProcessBuilder.start();
            final InputStream processInputStream = process.getInputStream();

            final StringBuilder stringBuilder = new StringBuilder();
            int c;
            while ((c = processInputStream.read()) != -1) {
                if (mOutputToConsole) {
                    LOGGER.append((char) c);
                }
                stringBuilder.append((char) c);
            }
            process.waitFor();

            final RunResult result = new RunResult(process.exitValue(), Arrays.asList(stringBuilder.toString().split("\\r?\\n")));
            if (result.getExitCode() != 0) {
                if (!mOutputToConsole) {
                    LOGGER.log(stringBuilder.toString());
                }
                throw new IOException("Got process exit code " + result.getExitCode() + " when running bazel " + toString());
            }

            return result;
        }

        @Override
        public String toString() {
            return "BazelExec: " + Arrays.toString(mProcessBuilder.command().toArray(new String[0]));
        }
    }

    public static class RunResult {
        private final int mExitCode;
        private final List<String> mOutput;

        public RunResult(int exitCode, List<String> executionOutput) {
            mExitCode = exitCode;
            mOutput = executionOutput;
        }

        public int getExitCode() {
            return mExitCode;
        }

        public List<String> getExecutionOutput() {
            return mOutput;
        }
    }

    public BazelExec createBazelRun(
            boolean outputToConsole,
            BazelLeafConfig.Decorated config,
            String target,
            String bazelCommand,
            String... args) {
        List<String> execArgs = new ArrayList<>();
        execArgs.add("--symlink_prefix=" + config.buildOutputDir);
        if (target != null && !target.isEmpty()) {
            execArgs.add(config.targetPath + ':' + target);
        }
        execArgs.addAll(Arrays.asList(args));

        return createBazelRun(outputToConsole, config.bazelBin, config.workspaceRootFolder, bazelCommand, execArgs);
    }

    public BazelExec createBazelRun(
            boolean outputToConsole,
            String bazelBinPath,
            File workspaceRootFolder,
            String bazelCommand,
            List<String> args) {
        final ProcessBuilder builder = new ProcessBuilder();
        final List<String> execArgs = new ArrayList<>(1 + BASIC_EXEC_ARGS.size() + 1 + args.size());
        execArgs.add(bazelBinPath);
        execArgs.add(bazelCommand);
        execArgs.addAll(BASIC_EXEC_ARGS);
        execArgs.addAll(args);

        builder.command(execArgs);
        builder.directory(workspaceRootFolder);

        return new BazelExec(outputToConsole, builder);
    }

    public Properties getInfo(BazelLeafConfig.Decorated config) {
        BazelExec infoExec = createBazelRun(false, config, "", "info");
        try {
            RunResult runResult = infoExec.start();

            StringWriter w = new StringWriter();
            PrintWriter b = new PrintWriter(w);

            runResult.getExecutionOutput().forEach(b::println);

            b.close();

            Properties properties = new Properties();
            properties.load(new StringReader(w.toString()));
            return properties;
        } catch (InterruptedException | IOException e) {
            LOGGER.log(String.format(Locale.US, "Failed to get information from Bazel. Error: %s", e.getMessage()));
            return null;
        }
    }
}
