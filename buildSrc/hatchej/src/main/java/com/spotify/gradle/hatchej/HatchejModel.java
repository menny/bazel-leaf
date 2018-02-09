package com.spotify.gradle.hatchej;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class HatchejModel {
    private Set<File> mExcludeFolders = new HashSet<>();
    private Set<File> mSourceFolders = new HashSet<>();
    private Set<File> mTestSourceFolders = new HashSet<>();
    private Set<String> mProjectDependencies = new HashSet<>();

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
}
