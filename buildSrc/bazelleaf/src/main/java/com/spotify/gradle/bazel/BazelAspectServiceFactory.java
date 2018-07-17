package com.spotify.gradle.bazel;

import org.gradle.api.Project;

public class BazelAspectServiceFactory {

    public BazelAspectService create(Project rootProject, AspectRunner aspectRunner) {
        return new BazelAspectService(rootProject, aspectRunner);
    }
}
