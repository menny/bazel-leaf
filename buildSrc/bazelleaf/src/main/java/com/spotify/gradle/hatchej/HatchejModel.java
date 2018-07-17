package com.spotify.gradle.hatchej;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class HatchejModel {

    private final Set<File> mExcludeFolders = new HashSet<>();
    private final Set<File> mSourceFolders = new HashSet<>();
    private final Set<File> mTestSourceFolders = new HashSet<>();
    private final Set<File> mProjectOutputs = new HashSet<>();
    private final Set<String> mProjectDependencies = new HashSet<>();
    private final Set<String> mLibraryDependencies = new HashSet<>();
    private final Set<String> mProjectTestDependencies = new HashSet<>();
    private final Set<String> mLibraryTestDependencies = new HashSet<>();

    public Set<File> getTestSourceFolders() {
        return mTestSourceFolders;
    }

    public Set<File> getSourceFolders() {
        return mSourceFolders;
    }

    public Set<File> getExcludeFolders() {
        return mExcludeFolders;
    }

    public Set<File> getProjectOutputs() {
        return mProjectOutputs;
    }

    public Set<String> getLibraryDependencies() {
        return mLibraryDependencies;
    }

    public Set<String> getProjectDependencies() {
        return mProjectDependencies;
    }

    public Set<String> getLibraryTestDependencies() {
        return mLibraryTestDependencies;
    }

    public Set<String> getProjectTestDependencies() {
        return mProjectTestDependencies;
    }
}
