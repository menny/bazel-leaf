package com.spotify.gradle.bazel;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Task;
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact;

import java.io.File;
import java.util.Date;

public class BazelPublishArtifact extends AbstractPublishArtifact {

    private final File mFile;

    public BazelPublishArtifact(Task task, File file) {
        super(task);
        mFile = file;
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public String getExtension() {
        return StringUtils.substringAfterLast(mFile.getName(), ".");
    }

    @Override
    public String getType() {
        return getExtension();
    }

    @javax.annotation.Nullable
    @Override
    public String getClassifier() {
        return null;
    }

    @Override
    public File getFile() {
        return mFile;
    }

    @javax.annotation.Nullable
    @Override
    public Date getDate() {
        return new Date(mFile.lastModified());
    }
}
