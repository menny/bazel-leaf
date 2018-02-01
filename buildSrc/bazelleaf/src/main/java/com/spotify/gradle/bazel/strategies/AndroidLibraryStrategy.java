package com.spotify.gradle.bazel.strategies;

import com.spotify.gradle.bazel.AspectRunner;
import com.spotify.gradle.bazel.BazelLeafConfig;
import com.spotify.gradle.bazel.BazelPublishArtifact;

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

class AndroidLibraryStrategy extends StrategyBase {

    AndroidLibraryStrategy(BazelLeafConfig.Decorated config) {
        super(config);
    }

    @Override
    protected String generateBazelBuildTaskName(Project project) {
        return "bazelAarBuild_" + mConfig.targetName;
    }

    @Override
    public List<BazelPublishArtifact> getBazelArtifacts(AspectRunner aspectRunner, Project project, Exec bazelBuildTask) {
        //we're going to manipulate the outs of this task:
        //using the Bazel outs, we'll construct a valid AAR file and give that to Gradle
        final File outputArtifactFolder = super.getBazelArtifacts(aspectRunner, project, bazelBuildTask).get(0).getFile().getParentFile();
        final File aarOutputFile = new File(outputArtifactFolder, mConfig.targetName + ".aar");
        Jar aarCreationTask = (Jar) bazelBuildTask.getProject().task(Collections.singletonMap("type", Jar.class), bazelBuildTask.getName() + "_AarPackage");
        aarCreationTask.dependsOn(bazelBuildTask);
        aarCreationTask.doFirst(task -> new File(outputArtifactFolder, "lib" + mConfig.targetName + ".jar").renameTo(new File(outputArtifactFolder, "classes.jar")));
        aarCreationTask.setEntryCompression(ZipEntryCompression.STORED);
        aarCreationTask.setMetadataCharset("UTF-8");
        aarCreationTask.setBaseName(mConfig.targetName);
        aarCreationTask.setDescription("Package an AAR from Bazel outputs.");
        aarCreationTask.setGroup(BasePlugin.BUILD_GROUP);
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
