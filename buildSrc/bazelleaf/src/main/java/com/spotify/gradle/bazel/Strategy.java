package com.spotify.gradle.bazel;

import org.gradle.api.Project;
import org.gradle.api.internal.file.archive.ZipFileTree;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.ZipEntryCompression;
import org.gradle.internal.hash.DefaultContentHasherFactory;
import org.gradle.internal.hash.DefaultFileHasher;
import org.gradle.internal.hash.DefaultStreamHasher;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Strategy {

    final BazelLeafConfig.Decorated mConfig;

    protected Strategy(BazelLeafConfig.Decorated config) {
        mConfig = config;
    }

    public static Strategy buildStrategy(String kind, BazelLeafConfig.Decorated config) {
        switch (kind) {
            case "java_library":
                return new JavaLibraryStrategy(config);
            case "android_library":
                return new AndroidLibraryStrategy(config);
            default:
                throw new IllegalArgumentException("Unsupported target kind " + kind + ". Currently, supporting java_library and android_library. Fix " + config.targetPath + ":" + config.targetName);
        }
    }

    public abstract Exec createBazelBuildTask(Project project);

    Exec createBazelBuildTaskInternal(Project project, String bazelTargetName, String taskName) {
        final Exec bazelBuildTask = (Exec) project.task(Collections.singletonMap("type", Exec.class), taskName);
        bazelBuildTask.setWorkingDir(mConfig.workspaceRootFolder);
        bazelBuildTask.setCommandLine(mConfig.bazelBin, "build", "--symlink_prefix=" + mConfig.buildOutputDir, mConfig.targetPath + ":" + bazelTargetName);
        bazelBuildTask.setDescription("Assembles this project using Bazel.");
        bazelBuildTask.setGroup(BasePlugin.BUILD_GROUP);
        return bazelBuildTask;
    }

    File generateFileForOutput(String filename) {
        return new File(mConfig.workspaceRootFolder, filename);
    }

    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Exec bazelBuildTask) {
        return aspectRunner.getAspectResult("get_rule_outs.bzl").stream()
                .map(this::generateFileForOutput)
                .map(artifactFile -> new BazelPublishArtifact(bazelBuildTask, artifactFile))
                .collect(Collectors.toList());
    }

    private static class AndroidLibraryStrategy extends Strategy {

        public AndroidLibraryStrategy(BazelLeafConfig.Decorated config) {
            super(config);
        }

        @Override
        public Exec createBazelBuildTask(Project project) {
            return createBazelBuildTaskInternal(project, mConfig.targetName, "bazelAarBuild_" + mConfig.targetName);
        }

        @Override
        public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Exec bazelBuildTask) {
            //we're going to manipulate the outs of this task:
            //using the Bazel outs, we'll construct a valid AAR file and give that to Gradle
            final File outputArtifactFolder = super.getBazelArtifacts(aspectRunner, bazelBuildTask).get(0).getFile().getParentFile();
            final File aarOutputFile = new File(outputArtifactFolder, mConfig.targetName + ".aar");
            Jar aarCreationTask = (Jar) bazelBuildTask.getProject().task(Collections.singletonMap("type", Jar.class), bazelBuildTask.getName() + "_AarPackage");
            aarCreationTask.dependsOn(bazelBuildTask);
            aarCreationTask.doFirst(task -> new File(outputArtifactFolder, "lib" + mConfig.targetName + ".jar").renameTo(new File(outputArtifactFolder, "classes.jar")));
            aarCreationTask.setEntryCompression(ZipEntryCompression.STORED);
            aarCreationTask.setMetadataCharset("UTF-8");
            aarCreationTask.setBaseName(mConfig.targetName);
            aarCreationTask.setExtension("aar");
            aarCreationTask.setDestinationDir(outputArtifactFolder);
            aarCreationTask.from(
                    new ZipFileTree(new File(outputArtifactFolder, mConfig.targetName + "_files/resource_files.zip"),
                            new File(outputArtifactFolder, "unzip/"), null, null, new DefaultFileHasher(new DefaultStreamHasher(new DefaultContentHasherFactory()))),
                    new File(outputArtifactFolder, "classes.jar"),
                    new File(outputArtifactFolder, mConfig.targetName + "_processed_manifest/AndroidManifest.xml"),
                    new File(outputArtifactFolder, mConfig.targetName + "_symbols/R.txt"));

            return Collections.singletonList(new BazelPublishArtifact(aarCreationTask, aarOutputFile));
        }
    }

    private static class JavaLibraryStrategy extends Strategy {
        public JavaLibraryStrategy(BazelLeafConfig.Decorated config) {
            super(config);
        }

        @Override
        public Exec createBazelBuildTask(Project project) {
            return createBazelBuildTaskInternal(project, mConfig.targetName, "bazelJavaLibBuild_" + mConfig.targetName);
        }
    }
}
