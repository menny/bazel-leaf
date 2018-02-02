package com.spotify.gradle.bazel.utils;

import com.spotify.gradle.bazel.BazelLeafConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BazelExecHelper {

    public static class BazelBuilder {
        private final ProcessBuilder mProcessBuilder;
        private final File mOutputFile;

        BazelBuilder(ProcessBuilder processBuilder, File outputFile) {
            mProcessBuilder = processBuilder;
            mOutputFile = outputFile;
        }

        public ProcessBuilder getProcessBuilder() {
            return mProcessBuilder;
        }

        public List<String> start() throws IOException, InterruptedException {
            ensurePathExists(mProcessBuilder.redirectOutput().file());
            ensurePathExists(mProcessBuilder.redirectError().file());

            final int exitCode = mProcessBuilder.start().waitFor();
            final List<String> bazelOutput = Files.readAllLines(mOutputFile.toPath());
            if (exitCode != 0) {
                bazelOutput.forEach(log -> System.out.println("Bazel error: " + log));
                throw new IOException("Got process exit code " + exitCode + " when running bazel " + Arrays.toString(mProcessBuilder.command().toArray(new String[0])));
            }

            //yes.... We need to read the ERROR output stream. Need to figure this out.
            return bazelOutput;
        }
    }

    public static BazelBuilder createBazelRun(BazelLeafConfig.Decorated config, String targetPath, String target, String bazelCommand, String... args) {
        ProcessBuilder builder = new ProcessBuilder();
        ArrayList<String> execArgs = new ArrayList<>(Arrays.asList(config.bazelBin, bazelCommand, "--symlink_prefix=" + config.buildOutputDir, targetPath + ":" + target));
        execArgs.addAll(Arrays.asList(args));
        builder.command(execArgs);
        builder.directory(config.workspaceRootFolder);


        final File standardOutputFile = new File(config.buildOutputDir, targetPath + "/runner_" + bazelCommand + "/" + target + ".txt");
        builder.redirectOutput(standardOutputFile);
        final File errorOutputFile = new File(config.buildOutputDir, targetPath + "/runner_" + bazelCommand + "/" + target + ".err");
        builder.redirectError(errorOutputFile);

        return new BazelBuilder(builder, errorOutputFile);
    }

    private static void ensurePathExists(File outputFile) throws IOException {
        final File parentFile = outputFile.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Failed to create output folder for " + outputFile.getAbsolutePath());
        }
    }
}
