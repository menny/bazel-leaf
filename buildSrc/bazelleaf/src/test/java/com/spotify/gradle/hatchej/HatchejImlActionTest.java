package com.spotify.gradle.hatchej;

import com.spotify.gradle.bazel.TestUtils;
import com.spotify.gradle.bazel.TestableBazelLeafPlugin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsArrayContaining;
import org.hamcrest.collection.IsArrayContainingInAnyOrder;
import org.hamcrest.collection.IsArrayWithSize;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsCollectionContaining;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.IsEqualIgnoringWhiteSpace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import groovy.util.Node;
import groovy.util.XmlParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@SuppressWarnings({"PMD.AlwaysSpecifyTestRunner", "checkstyle:regexpsinglelinejava"})
public class HatchejImlActionTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testLibrariesDirDoesntExistNoUpdates() throws Exception {
        Project project =
                ProjectBuilder.builder().withGradleUserHomeDir(temporaryFolder.newFolder("gradleUserHome"))
                        .withProjectDir(temporaryFolder.getRoot()).build();
        TestUtils.setupGenericExtensions(project).getPluginManager().apply(TestableBazelLeafPlugin.class);

        HatchejModel hatchejModel = new HatchejModel();
        hatchejModel.getLibraryDependencies().add("library1");
        HatchejImlAction hatchejImlAction = new HatchejImlAction();

        File librariesFile = new File(project.getRootDir().getCanonicalFile(), ".idea/libraries");
        assertThat(librariesFile.exists(), Is.is(false));
        boolean added = hatchejImlAction.addLibraryFiles(project, hatchejModel);
        assertThat(added, Is.is(false));

        assertThat(librariesFile.exists(), Is.is(false));
        // should only have 1 directory `gradleUserHome`
        assertThat(temporaryFolder.getRoot().list(), IsArrayWithSize.arrayWithSize(1));
        assertThat(temporaryFolder.getRoot().list(),
                IsArrayContaining.hasItemInArray("gradleUserHome"));
    }

    @Test
    public void testAddLibraryFilesLibraryDirExists() throws Exception {
        Project project =
                ProjectBuilder.builder().withGradleUserHomeDir(temporaryFolder.newFolder("gradleUserHome"))
                        .withProjectDir(temporaryFolder.getRoot()).build();
        File ideaFolder = temporaryFolder.newFolder(".idea");
        File librariesFolder = new File(ideaFolder, "libraries");
        Files.createDirectories(librariesFolder.toPath());

        TestUtils.setupGenericExtensions(project).getPluginManager().apply(TestableBazelLeafPlugin.class);

        HatchejModel hatchejModel = new HatchejModel();
        hatchejModel.getLibraryDependencies().add("librarydep1");
        hatchejModel.getLibraryDependencies().add("librarydep2");
        HatchejImlAction hatchejImlAction = new HatchejImlAction();

        boolean added = hatchejImlAction.addLibraryFiles(project, hatchejModel);
        assertThat(added, Is.is(true));

        // should only have 2 directories `gradleUserHome, .idea`
        assertThat(temporaryFolder.getRoot().list(), IsArrayWithSize.arrayWithSize(2));
        assertThat(librariesFolder.list(), IsArrayContainingInAnyOrder
                .arrayContainingInAnyOrder("librarydep1!.xml", "librarydep2!.xml"));

        String libraryDep1Xml =
                FileUtils.readFileToString(new File(librariesFolder, "librarydep1!.xml"), "UTF-8");
        assertThat(StringUtils.deleteWhitespace(libraryDep1Xml),
                IsEqual.equalTo(StringUtils.deleteWhitespace(getLibraryXmlContent("librarydep1"))));

        String libraryDep2Xml =
                FileUtils.readFileToString(new File(librariesFolder, "librarydep2!.xml"), "UTF-8");
        assertThat(StringUtils.deleteWhitespace(libraryDep2Xml),
                IsEqual.equalTo(StringUtils.deleteWhitespace(getLibraryXmlContent("librarydep2"))));
    }

    private static String getLibraryXmlContent(String libraryName) {
        return String.format("<component name=\"libraryTable\">\n"
                + "  <library name=\"%s!\">\n"
                + "    <CLASSES>\n"
                + "      <root url=\"jar://%s!/\"/>\n"
                + "    </CLASSES>\n"
                + "    <JAVADOC/>\n"
                + "    <SOURCES/>\n"
                + "  </library>\n"
                + "</component>\n", libraryName, libraryName);
    }

    @Test
    public void testModifyImlFileWithNoExistingImlFileExpectNothing() throws Exception {
        Project project =
                ProjectBuilder.builder().withGradleUserHomeDir(temporaryFolder.newFolder("gradleUserHome"))
                        .withProjectDir(temporaryFolder.getRoot()).build();
        TestUtils.setupGenericExtensions(project).getPluginManager().apply(TestableBazelLeafPlugin.class);

        HatchejModel hatchejModel = new HatchejModel();

        HatchejImlAction hatchejImlAction = new HatchejImlAction();
        hatchejImlAction.modifyImlFile(project, hatchejModel);
    }

    @Test
    public void testModifyImlFileWithExistingImlFile() throws Exception {
        File gradleUserHomeDir = temporaryFolder.newFolder("gradleUserHome");
        File projectDir = temporaryFolder.getRoot();

        File subProjectDir = temporaryFolder.newFolder("subProject");
        Project project =
                ProjectBuilder.builder().withName("genericProject")
                        .withGradleUserHomeDir(gradleUserHomeDir)
                        .withProjectDir(projectDir).build();
        Project subProject =
                ProjectBuilder.builder().withName("subProject")
                        .withProjectDir(subProjectDir)
                        .withParent(project).build();
        TestUtils.setupGenericExtensions(subProject).getPluginManager().apply(TestableBazelLeafPlugin.class);

        File projectImlFile = new File(subProjectDir, "subProject.iml");

        FileUtils.write(projectImlFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<module>\n"
                + "  <component name=\"NewModuleRootManager\">\n"
                + "    <content url=\"file://$MODULE_DIR$\" />\n"
                + "  </component>\n"
                + "</module>", "UTF-8");
        projectImlFile.setLastModified(0L);

        HatchejModel hatchejModel = new HatchejModel();

        HatchejImlAction hatchejImlAction = new HatchejImlAction();
        hatchejImlAction.modifyImlFile(subProject, hatchejModel);

        assertThat(projectImlFile.lastModified(), Matchers.greaterThan(0L));
    }

    @Test
    public void testModifyImlFileWithLibraryDependencies() throws Exception {
        File gradleUserHomeDir = temporaryFolder.newFolder("gradleUserHome");
        File projectDir = temporaryFolder.getRoot();

        File subProjectDir = temporaryFolder.newFolder("subProject");
        Project project =
                ProjectBuilder.builder().withName("genericProject")
                        .withGradleUserHomeDir(gradleUserHomeDir)
                        .withProjectDir(projectDir).build();
        Project subProject =
                ProjectBuilder.builder().withName("subProject")
                        .withProjectDir(subProjectDir)
                        .withParent(project).build();
        TestUtils.setupGenericExtensions(subProject).getPluginManager().apply(TestableBazelLeafPlugin.class);

        File subProjectImlFile = new File(subProjectDir, "subProject.iml");

        FileUtils.write(subProjectImlFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<module>\n"
                + "  <component name=\"NewModuleRootManager\">\n"
                + "    <content url=\"file://$MODULE_DIR$\" prevValue=\"somevalue\">\n"
                // will get removed
                + "      <excludeFolder url=\"file://$MODULE_DIR$/.gradle\"/>"
                + "      <sourceFolder url=\"file://$MODULE_DIR$/.gradle\"/>"
                + "      <orderEntry type=\"module\"/>"
                + "      <orderEntry type=\"library\"/>"
                + "    </content>\n"
                + "  </component>\n"
                + "</module>", "UTF-8");
        subProjectImlFile.setLastModified(0L);

        HatchejModel hatchejModel = new HatchejModel();
        hatchejModel.getLibraryDependencies().add("dep1");
        hatchejModel.getLibraryDependencies().add("dep2");

        hatchejModel.getProjectDependencies().add("projectDep1");
        hatchejModel.getProjectDependencies().add("projectDep2");

        hatchejModel.getSourceFolders().add(new File(subProjectDir, "source1"));
        hatchejModel.getSourceFolders().add(new File(subProjectDir, "source2"));

        hatchejModel.getExcludeFolders().add(new File(subProjectDir, "exclude1"));
        hatchejModel.getExcludeFolders().add(new File(subProjectDir, "exclude2"));

        HatchejImlAction hatchejImlAction = new HatchejImlAction();
        hatchejImlAction.modifyImlFile(subProject, hatchejModel);

        assertThat(subProjectImlFile.lastModified(), Matchers.greaterThan(0L));

        final Node imlRootNode = new XmlParser().parse(subProjectImlFile);
        // traverse and find the library nodes
        List<String> dependencyList =
                (List<String>) imlRootNode.breadthFirst().stream()
                        .filter(n -> {
                            String name = (String) ((Node) n).name();
                            String type = (String) ((Node) n).attribute("type");
                            return StringUtils.equals(name, "orderEntry") && StringUtils
                                    .equals(type, "library");
                        })
                        .filter(
                                n -> {
                                    String name = (String) ((Node) n).attribute("name");
                                    return StringUtils.equalsAny(name, "dep1", "dep2");
                                })
                        .map(n -> ((Node) n).attribute("name")).collect(Collectors.toList());
        assertThat(dependencyList, IsCollectionWithSize.hasSize(2));
        assertThat(dependencyList, IsCollectionContaining.hasItems("dep1", "dep2"));

        String subProjectImlXml = FileUtils.readFileToString(subProjectImlFile, "UTF-8");

        List<String> projectDependencyList = (List<String>)
                imlRootNode.breadthFirst().stream()
                        .filter(n -> {
                            String name = (String) ((Node) n).name();
                            String type = (String) ((Node) n).attribute("type");
                            return StringUtils.equals(name, "orderEntry")
                                    && StringUtils.equals(type, "module");
                        })
                        .filter(
                                n -> {
                                    String name = (String) ((Node) n).attribute("module-name");
                                    return StringUtils.equalsAny(name, "projectDep1", "projectDep2");
                                })
                        .map(n -> ((Node) n).attribute("module-name")).collect(Collectors.toList());
        assertThat(subProjectImlXml, projectDependencyList, IsCollectionWithSize.hasSize(2));
        assertThat(subProjectImlXml, projectDependencyList,
                IsCollectionContaining.hasItems("projectDep1", "projectDep2"));

        List<String> sourceList = (List<String>)
                imlRootNode.breadthFirst().stream()
                        .filter(n -> {
                            String name = (String) ((Node) n).name();
                            return StringUtils.equals(name, "sourceFolder");
                        })
                        .map(n -> ((Node) n).attribute("url")).collect(Collectors.toList());
        assertThat(subProjectImlXml, sourceList, IsCollectionWithSize.hasSize(2));
        assertThat(subProjectImlXml, sourceList,
                IsCollectionContaining
                        .hasItems("file://$MODULE_DIR$/source1", "file://$MODULE_DIR$/source2"));

        List<String> excludeList = (List<String>)
                imlRootNode.breadthFirst().stream()
                        .filter(n -> {
                            String name = (String) ((Node) n).name();
                            return StringUtils.equals(name, "excludeFolder");
                        })
                        .map(n -> ((Node) n).attribute("url")).collect(Collectors.toList());
        assertThat(subProjectImlXml, excludeList, IsCollectionWithSize.hasSize(2));
        assertThat(subProjectImlXml, excludeList,
                IsCollectionContaining
                        .hasItems("file://$MODULE_DIR$/exclude1", "file://$MODULE_DIR$/exclude2"));

        assertThat(subProjectImlXml, imlRootNode.breadthFirst().stream().filter(n -> {
            String name = (String) ((Node) n).name();
            return StringUtils.equals(name, "content");
        }).count(), IsEqual.equalTo(1L));

        assertThat(subProjectImlXml, imlRootNode.breadthFirst().stream().filter(n -> {
            String name = (String) ((Node) n).name();
            return StringUtils.equals(name, "content");
        }).filter(n -> {
            // check to see if it didn't get removed
            Object o = ((Node) n).attribute("prevValue");
            return o != null;
        }).count(), IsEqual.equalTo(1L));
    }

    @Test
    public void testModifyImlFileWithProjectOutput() throws Exception {
        File gradleUserHomeDir = temporaryFolder.newFolder("gradleUserHome");
        File projectDir = temporaryFolder.getRoot();

        File subProjectDir = temporaryFolder.newFolder("subProject");
        Project project =
                ProjectBuilder.builder().withName("genericProject")
                        .withGradleUserHomeDir(gradleUserHomeDir)
                        .withProjectDir(projectDir).build();
        Project subProject =
                ProjectBuilder.builder().withName("subProject")
                        .withProjectDir(subProjectDir)
                        .withParent(project).build();
        TestUtils.setupGenericExtensions(subProject).getPluginManager().apply(TestableBazelLeafPlugin.class);

        File subProjectImlFile = new File(subProjectDir, "subProject.iml");

        FileUtils.write(subProjectImlFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<module>\n"
                + "  <component name=\"NewModuleRootManager\">\n"
                // will get removed
                + "    <output url=\"file:///something_old\"/>\n"
                + "    <content url=\"file://$MODULE_DIR$\" prevValue=\"somevalue\">\n"
                // will get removed
                + "      <excludeFolder url=\"file://$MODULE_DIR$/.gradle\"/>"
                + "      <sourceFolder url=\"file://$MODULE_DIR$/.gradle\"/>"
                + "      <orderEntry type=\"module\"/>"
                + "      <orderEntry type=\"library\"/>"
                + "    </content>\n"
                + "  </component>\n"
                + "</module>", "UTF-8");
        subProjectImlFile.setLastModified(0L);

        HatchejModel hatchejModel = new HatchejModel();
        final File artifactPath = new File(subProjectDir, "some_new_output.jar");
        hatchejModel.getProjectOutputs().add(artifactPath);

        HatchejImlAction hatchejImlAction = new HatchejImlAction();
        hatchejImlAction.modifyImlFile(subProject, hatchejModel);

        assertThat(subProjectImlFile.lastModified(), Matchers.greaterThan(0L));

        final Node imlRootNode = new XmlParser().parse(subProjectImlFile);
        final Node componentNode = HatchejImlAction.xmlPath(imlRootNode, node -> node.name().equals("component") && "NewModuleRootManager".equals(node.attribute("name")));
        final List<Node> outputs = (List<Node>) componentNode.children().stream().filter(n -> "output".equals(((Node) n).name())).collect(Collectors.toList());
        assertEquals(1, outputs.size());
        assertEquals("file://" + artifactPath.getAbsolutePath(), outputs.get(0).attribute("url"));
    }

    @Test
    public void testCreateLibraryFileIfNotExists() throws IOException {
        File ideaLibrariesDir = new File(temporaryFolder.getRoot(), ".idea/libraries");
        ideaLibrariesDir.mkdirs();

        boolean created = HatchejImlAction.createLibraryFileIfNotExists(
                ideaLibrariesDir, "dep1");
        File dep1LibraryXml = new File(ideaLibrariesDir, "dep1!.xml");
        assertThat(dep1LibraryXml.exists(), IsEqual.equalTo(true));
        String s = FileUtils.readFileToString(dep1LibraryXml, "UTF-8");
        assertThat(s,
                IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace("<component name=\"libraryTable\">\n"
                        + "  <library name=\"dep1!\">\n"
                        + "    <CLASSES>\n"
                        + "      <root url=\"jar://dep1!/\"/>\n"
                        + "    </CLASSES>\n"
                        + "    <JAVADOC/>\n"
                        + "    <SOURCES/>\n"
                        + "  </library>\n"
                        + "</component>\n"));
        assertThat(created, Matchers.is(true));
    }

    @Test
    public void testCreateLibraryFileExists() throws IOException {
        File ideaLibrariesDir = new File(temporaryFolder.getRoot(), ".idea/libraries");
        ideaLibrariesDir.mkdirs();
        File dep1LibraryXml = new File(ideaLibrariesDir, "dep1!.xml");
        FileUtils.touch(dep1LibraryXml);
        dep1LibraryXml.setLastModified(0L);

        boolean created = HatchejImlAction.createLibraryFileIfNotExists(
                ideaLibrariesDir, "dep1");
        assertThat(dep1LibraryXml.exists(), IsEqual.equalTo(true));

        assertThat(dep1LibraryXml.length(), IsEqual.equalTo(0L));
        assertThat(dep1LibraryXml.lastModified(), IsEqual.equalTo(0L));
        assertThat(created, Matchers.is(false));
    }
}
