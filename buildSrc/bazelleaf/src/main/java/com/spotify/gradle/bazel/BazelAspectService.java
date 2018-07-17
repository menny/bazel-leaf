package com.spotify.gradle.bazel;

import org.gradle.api.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service to encapsulate the Aspect calls to Bazel.
 */
public class BazelAspectService {

    public static BazelAspectService create(Project rootProject, AspectRunner aspectRunner) {
        return new BazelAspectService(rootProject, aspectRunner);
    }

    //taken from https://github.com/bazelbuild/intellij/blob/master/aspect/tools/src/com/google/idea/blaze/aspect/PackageParser.java#L163
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w\\.]+);$");
    private final Project mRootProject;
    private final AspectRunner mRunner;

    BazelAspectService(Project rootProject, AspectRunner aspectRunner) {
        mRootProject = rootProject;
        mRunner = aspectRunner;
    }

    @javax.annotation.Nullable
    private static String parseDeclaredPackage(File sourceFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher packageMatch = PACKAGE_PATTERN.matcher(line);
                if (packageMatch.find()) {
                    return packageMatch.group(1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse java package for " + sourceFile, e);
        }
        return null;
    }

    public Deps getAllDeps(String targetName) {
        //"//andlib/innerandlib:inneraar<FILES:>[<generated file andlib/innerandlib/inneraar.srcjar>"
        final Pattern localModulesPattern = Pattern.compile("^(//.+)<FILES:>.*$");
        //"//third_party:com_google_code_findbugs__findbugs-annotations__3_0_1<FILES:>[<source file external/com_google_code_findbugs__findbugs_annotations__3_0_1/file/findbugs-annotations-3.0.1.jar>]"
        final Pattern remoteWorkspaceModulesPattern = Pattern.compile("^//third_party:(.*)<FILES:>\\[(.*)]$");
        //"[<source file external/com_google_code_findbugs__findbugs_annotations__3_0_1/file/findbugs-annotations-3.0.1.jar>, <generated file andlib/innerandlib/inneraar_resources-src.jar>]"
        final Pattern generatedFilesPattern = Pattern.compile("<.*\\s+file\\s+(.+)>");
        return mRunner.getAspectResult("get_deps.bzl", targetName).stream()
                .collect(Deps::new, (deps, bazelDepAnnotation) -> {
                    final Matcher localModuleMatcher = localModulesPattern.matcher(bazelDepAnnotation);
                    final Matcher remoteWorkspaceMatcher = remoteWorkspaceModulesPattern.matcher(bazelDepAnnotation);
                    if (remoteWorkspaceMatcher.matches()) {
                        final String remotedepRuleName = remoteWorkspaceMatcher.group(1);
                        final Matcher generateFilesMatcher = generatedFilesPattern.matcher(remoteWorkspaceMatcher.group(2));
                        if (generateFilesMatcher.matches()) {
                            deps.remoteWorkspaceDeps.add(new ExternalTarget(remotedepRuleName, generateFilesMatcher.group(1)));
                        } else {
                            throw new IllegalStateException("Was not able to match remote-dependency " + bazelDepAnnotation);
                        }
                    } else if (localModuleMatcher.matches()) {
                        final String localPath = localModuleMatcher.group(1);
                        deps.moduleDeps.add(new LocalBazelTarget(localPath));
                    } else {
                        throw new IllegalStateException(
                                "the Bazel dep '" + bazelDepAnnotation + "' does not match any known annotations.");
                    }
                }, (deps, deps2) -> {
                    deps.moduleDeps.addAll(deps2.moduleDeps);
                    deps.remoteWorkspaceDeps.addAll(deps2.remoteWorkspaceDeps);
                });
    }

    public Set<File> getSourceFolders(String targetName) {
        final Map<File, String> packageByFolder = new HashMap<>();

        return mRunner.getAspectResult("get_source_files.bzl", targetName).stream()
                .map(File::new)
                //we need the root-project since the WORKSPACE file is there.
                .map(mRootProject::file)
                .map(sourceFile -> {
                    File parent = sourceFile.getParentFile();
                    String packageInFolder = packageByFolder
                            .computeIfAbsent(parent, fileNotUsedHere -> parseDeclaredPackage(sourceFile));
                    final String parentFullPath = parent.getPath();
                    //removing the package folders, we only want the root folder
                    return new File(
                            parentFullPath.substring(0, parentFullPath.length() - packageInFolder.length()));
                })
                .distinct()
                .collect(Collectors.toSet());
    }

    public static class Deps {
        public final List<LocalBazelTarget> moduleDeps = new ArrayList<>();
        public final List<ExternalTarget> remoteWorkspaceDeps = new ArrayList<>();
    }

    public static class LocalBazelTarget {
        public final String path;

        public LocalBazelTarget(String path) {
            this.path = path;
        }
    }

    public static class ExternalTarget {
        public final String externalDepName;
        public final String jarPath;

        public ExternalTarget(String externalDepName, String jarPath) {
            this.externalDepName = externalDepName;
            this.jarPath = jarPath;
        }
    }
}
