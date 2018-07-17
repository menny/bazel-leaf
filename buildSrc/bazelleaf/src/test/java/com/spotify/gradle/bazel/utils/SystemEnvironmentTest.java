package com.spotify.gradle.bazel.utils;

import org.hamcrest.core.IsEqual;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("PMD.AlwaysSpecifyTestRunner")
public class SystemEnvironmentTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void testDarwin() {
        System.setProperty("os.name", "darwin");
        SystemEnvironment.OsType osType = SystemEnvironment.getOsType();
        assertThat(osType, IsEqual.equalTo(SystemEnvironment.OsType.macOs));
    }

    @Test
    public void testMac() {
        System.setProperty("os.name", "mac");
        SystemEnvironment.OsType osType = SystemEnvironment.getOsType();
        assertThat(osType, IsEqual.equalTo(SystemEnvironment.OsType.macOs));
    }

    @Test
    public void testWindows() {
        // case-insensitive
        System.setProperty("os.name", "WINDOWS");
        SystemEnvironment.OsType osType = SystemEnvironment.getOsType();
        assertThat(osType, IsEqual.equalTo(SystemEnvironment.OsType.Windows));
    }

    @Test
    public void testLinux() {
        System.setProperty("os.name", "NOTSOMEEXPECTEDVALUE");
        SystemEnvironment.OsType osType = SystemEnvironment.getOsType();
        assertThat(osType, IsEqual.equalTo(SystemEnvironment.OsType.Linux));
    }
}
