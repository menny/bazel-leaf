package com.spotify.gradle.bazel;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testing.base.plugins.TestingBasePlugin;

public final class TestUtils {

    private TestUtils() {
    }

    public static Project createConfiguredProject() {
        return setupGenericExtensions(ProjectBuilder.builder().build());
    }

    public static Project setupGenericExtensions(Project project) {
        Project rootProject = project.getRootProject();
        rootProject.getExtensions().add("bazel.bin.url.linux", "http://example.com");
        rootProject.getExtensions().add("bazel.bin.url.macos", "http://example.com");
        rootProject.getExtensions().add("bazel.bin.url.windows", "http://example.com");

        return project;
    }

    public static Project createConfiguredAppliedProject() {
        final Project project = createConfiguredProject();
        project.getPluginManager().apply(TestingBasePlugin.class);
        return project;
    }
}
