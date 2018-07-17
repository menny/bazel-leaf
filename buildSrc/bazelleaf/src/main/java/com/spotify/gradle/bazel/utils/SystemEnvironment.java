package com.spotify.gradle.bazel.utils;

import java.util.Locale;

public final class SystemEnvironment {
    public enum OsType {
        Linux,
        macOs,
        Windows
    }

    private SystemEnvironment() {
    }

    public static OsType getOsType() {
        final String osName = System.getProperty("os.name", "none").toLowerCase(Locale.US);

        if (osName.contains("mac") || osName.contains("darwin")) {
            return OsType.macOs;
        } else if (osName.contains("win")) {
            return OsType.Windows;
        } else {
            return OsType.Linux;
        }
    }
}
