package com.spotify.gradle.bazel;

import org.gradle.api.Task;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;

import java.io.File;
import java.util.Date;

import javax.annotation.Nullable;

class BazelPublishArtifact extends AbstractPublishArtifact {

    private final File mFile;

    BazelPublishArtifact(Task task, File file) {
        super(task);
        mFile = file;
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public String getExtension() {
        return mFile.getName().substring(mFile.getName().lastIndexOf(".") + 1);
    }

    @Override
    public String getType() {
        return getExtension();
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
