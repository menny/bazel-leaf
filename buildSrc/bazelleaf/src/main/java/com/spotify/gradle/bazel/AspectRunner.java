package com.spotify.gradle.bazel;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AspectRunner {
    private final BazelLeafConfig.Decorated mConfig;
    private final String mBazelFullPathTarget;
    private final File mAspectsFolder;

    public AspectRunner(BazelLeafConfig.Decorated config) {
        mConfig = config;
        mBazelFullPathTarget = mConfig.targetPath + ":" + mConfig.targetName;
        mAspectsFolder = new File("build/bazel_aspects/", mBazelFullPathTarget.replace("/", "_").replace(":", "_"));
        if (!mAspectsFolder.exists() && !mAspectsFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create output folder for aspects at " + mAspectsFolder.getAbsolutePath());
        }

        try (FileOutputStream bazelBuildFile = new FileOutputStream(new File(mAspectsFolder, "BUILD.bazel"))) {
            IOUtils.write("", bazelBuildFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create an empty BUILD file for aspects rules!", e);
        }
    }

    public List<String> getAspectResult(String aspectRuleFileName) {
        try {

            try (InputStream resourceAsStream = BazelLeafPlugin.class.getClassLoader().getResourceAsStream("aspects/" + aspectRuleFileName)) {
                final File aspectRuleFile = new File(mAspectsFolder, aspectRuleFileName);
                try (OutputStream outputStream = new FileOutputStream(aspectRuleFile, false)) {
                    IOUtils.copy(resourceAsStream, outputStream);
                    //bazel build //MyExample:example --aspects print.bzl%print_aspect
                    ProcessBuilder builder = new ProcessBuilder();
                    builder.command(mConfig.bazelBin, "build", mBazelFullPathTarget, "--symlink_prefix=" + mConfig.buildOutputDir + "/", "--aspects", aspectRuleFile + "%print_aspect");
                    builder.directory(mConfig.projectRootDir);
                    final File aspectOutputFile = new File(mAspectsFolder, aspectRuleFileName + ".txt");
                    builder.redirectOutput(aspectOutputFile);
                    final File aspectErrOutputFile = new File(mAspectsFolder, aspectRuleFileName + ".err");
                    builder.redirectError(aspectErrOutputFile);
                    final int exitCode = builder.start().waitFor();
                    if (exitCode != 0) {
                        for (String errorLine : Files.readAllLines(aspectErrOutputFile.toPath())) {
                            System.out.println("ASPECT ERROR: " + errorLine);
                        }
                        throw new IllegalStateException("Failed to run " + aspectRuleFileName + " aspect on " + mBazelFullPathTarget,
                                new IOException("Got process exit code " + exitCode));
                    }

                    //yes.... We need to read the ERROR output stream. Need to figure this out.
                    return cleanUp(Files.readAllLines(aspectErrOutputFile.toPath()), aspectRuleFileName);
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static List<String> cleanUp(List<String> rawBazelAspectOutput, String aspectRuleFileName) {
        final Pattern pattern = Pattern.compile("^DEBUG.*/" + aspectRuleFileName + ":\\d+:\\d+:\\s*(.+)$");
        /*
____Loading complete.  Analyzing...
DEBUG: /Users/menny/dev/spotify/bazel-leaf/build/bazel_aspects/__lib2_jar/get_source_files.bzl:8:17: lib2/src/main/java/com/spotify/music/lib2/Lib2.java
____Found 1 target...
____Elapsed time: 0.126s, Critical Path: 0.00s
         */
        return rawBazelAspectOutput.stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .collect(Collectors.toList());
    }
}
