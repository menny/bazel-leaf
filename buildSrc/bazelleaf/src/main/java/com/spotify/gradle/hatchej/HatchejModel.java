package com.spotify.gradle.hatchej;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class HatchejModel {
    private final Set<File> mExcludeFolders = new HashSet<>();
    private final Set<File> mSourceFolders = new HashSet<>();
    private final Set<File> mTestSourceFolders = new HashSet<>();
    private final Set<String> mProjectDependencies = new HashSet<>();
    private final Set<String> mLibraryDependencies = new HashSet<>();

    public Set<String> getProjectDependencies() {
        return mProjectDependencies;
    }

    public Set<File> getTestSourceFolders() {
        return mTestSourceFolders;
    }

    public Set<File> getSourceFolders() {
        return mSourceFolders;
    }

    public Set<File> getExcludeFolders() {
        return mExcludeFolders;
    }

    public Set<String> getLibraryDependencies() {
        return mLibraryDependencies;
    }
}
