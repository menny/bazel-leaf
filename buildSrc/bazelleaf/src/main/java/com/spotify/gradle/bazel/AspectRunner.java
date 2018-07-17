package com.spotify.gradle.bazel;

import com.spotify.gradle.bazel.utils.BazelExecHelper;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AspectRunner {

    // the 'noop' group shouldn't exist and isn't expected to exist.  This should avoid the default output_group execution.
    private static final String NOOP_OUTPUT_GROUP = "noop";

    private final BazelLeafConfig.Decorated mConfig;
    private final File mAspectsFolder;
    private final BazelExecHelper mBazelExecHelper;

    public AspectRunner(BazelLeafConfig.Decorated config, BazelExecHelper bazelExecHelper) {
        mConfig = config;
        mBazelExecHelper = bazelExecHelper;
        mAspectsFolder = new File("build/bazel_aspects/", mConfig.targetPath.replace("/", "_").replace(":", "_"));
        if (!mAspectsFolder.exists() && !mAspectsFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create output folder for aspects at " + mAspectsFolder.getAbsolutePath());
        }

        try (FileOutputStream bazelBuildFile = new FileOutputStream(new File(mAspectsFolder, "BUILD.bazel"))) {
            IOUtils.write("", bazelBuildFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create an empty BUILD file for aspects rules!", e);
        }
    }

    public List<String> getAspectResult(String aspectRuleFileName, String target) {
        try (InputStream resourceAsStream = BazelLeafPlugin.class.getClassLoader()
                .getResourceAsStream("aspects/" + aspectRuleFileName)) {
            final File aspectRuleFile = new File(mAspectsFolder, aspectRuleFileName);
            try (OutputStream outputStream = new FileOutputStream(aspectRuleFile, false)) {
                IOUtils.copy(resourceAsStream, outputStream);
            }

            String outputGroupArg = "--output_groups=" + NOOP_OUTPUT_GROUP;
            BazelExecHelper.BazelExec builder = mBazelExecHelper
                    .createBazelRun(false, mConfig, target, "build", outputGroupArg, "--aspects",
                            aspectRuleFile + "%print_aspect");
            //yes... Aspect output is on the error channel.
            return cleanUp(builder.start().getExecutionOutput(), aspectRuleFileName);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> cleanUp(List<String> outputLines, String aspectRuleFileName) {
        final Pattern pattern = Pattern.compile("^.*" + aspectRuleFileName + ":\\d+:\\d+:\\s+(.+)\\s*$");
        return outputLines.stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .collect(Collectors.toList());
    }
}
