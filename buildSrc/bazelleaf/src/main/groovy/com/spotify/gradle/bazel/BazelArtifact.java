package com.spotify.gradle.bazel

import org.gradle.api.Task
import org.gradle.api.internal.artifacts.publish.AbstractPublishArtifact

class BazelArtifact extends AbstractPublishArtifact {
    File file

    BazelArtifact(Task buildTask, File file) {
      super(buildTask)
        this.file = file
    }

    @Override
    String getName() {
        'lib2'
    }

    @Override
    String getExtension() {
        'jar'
    }

    @Override
    String getType() {
        'jar'
    }

    @Override
    String getClassifier() {
        null
    }

    @Override
    File getFile() {
        file
    }

    @Override
    Date getDate() {
        return new Date(file.lastModified())
    }
}