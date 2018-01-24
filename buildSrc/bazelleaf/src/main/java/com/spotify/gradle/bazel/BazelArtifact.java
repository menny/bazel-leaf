package com.spotify.gradle.bazel;

import org.gradle.api.Task;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;

import java.io.File;
import java.util.Date;

import javax.annotation.Nullable;

class BazelArtifact extends AbstractPublishArtifact {

    private final File mFile;

    BazelArtifact(Task task, File file) {
        super(task);
        mFile = file;
    }

    @Override
    public String getName() {
        return "lib2";
    }

    @Override
    public String getExtension() {
        return "jar";
    }

    @Override
    public String getType() {
        return "jar";
    }

    @Nullable
    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public File getFile() {
        return mFile;
    }

    @Nullable
    @Override
    public Date getDate() {
        return new Date(mFile.lastModified());
    }
}
