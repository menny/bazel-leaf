package com.spotify.gradle.hatchej;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;

public class HatchejImlAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(HatchejImlAction.class);

    public void addLibraryFiles(final Project project, final HatchejModel hatchejModel)
            throws IOException {

        /**
         * Create file(s) for jar references in the /.idea/libraries directory
         * <component name="libraryTable">
         * <library name="guava-20.0">
         * <CLASSES>
         * <root url="jar://$USER_HOME$/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/20.0/89507701249388e1ed5ddcf8c41f4ce1be7831ef/guava-20.0.jar!/" />
         * </CLASSES>
         * <JAVADOC />
         * <SOURCES>
         * <root url="jar://$USER_HOME$/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/20.0/9c8493c7991464839b612d7547d6c263adf08f75/guava-20.0-sources.jar!/" />
         * </SOURCES>
         * </library>
         * </component>
         */
        File ideaLibrariesDir = new File(project.getRootDir(), ".idea/libraries");
        if (!ideaLibrariesDir.isDirectory() || !ideaLibrariesDir.exists()) {
            return; // not running within IntelliJ workspace
        }

        hatchejModel.getLibraryDependencies().stream().forEach(dependency -> {
            try {
                createLibraryFileIfNotExists(ideaLibrariesDir, dependency);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createLibraryFileIfNotExists(File ideaLibrariesDir, String dependency)
            throws IOException {
        String path = getJarUrl(dependency);
        String name = getJarName(path);
        String normalizedName = normalizePath(name);

        File ideaLibraryFile = new File(ideaLibrariesDir, normalizedName + ".xml");
        if (!ideaLibraryFile.exists()) {
            final Node componentRootNode = createNode("component", "name", "libraryTable");

            final Node libraryNode = createLibraryNode(name);
            componentRootNode.append(libraryNode);

            Node classes = libraryNode.appendNode("CLASSES");
            classes.append(createRootNode(path));

            libraryNode.appendNode("JAVADOC");
            libraryNode.appendNode("SOURCES");

            FileWriter fileWriter = new FileWriter(ideaLibraryFile);
            new XmlNodePrinter(new PrintWriter(fileWriter)).print(componentRootNode);
        } else {
            LOGGER.debug("IDEA library file {} already exists", ideaLibraryFile.getAbsolutePath());
        }
    }

    private String getJarName(String path) {
        // take the name of the file and normalize without the .jar
        String name = new File(path).getName();
        return StringUtils.substringBeforeLast(name, ".");
    }

    private String getJarUrl(String path) {
        String prefix = "jar://";
        String suffix = "!/";

        String jarUrl = path;
        if (!StringUtils.startsWith(path, prefix) && !StringUtils.endsWith(path, suffix)) {
            jarUrl = StringUtils.join(prefix, path, suffix);
        } else {
            LOGGER.warn("Unable to create the URL for the library {}", path);
        }
        return jarUrl;
    }

    private Node createNode(String nodeName, String attributeName, String attributeValue) {
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put(attributeName, attributeValue);
        return new Node(null, nodeName, attributes);
    }

    private Node createLibraryNode(String name) {
        return createNode("library", "name", name);
    }

    private Node createRootNode(String path) {
        return createNode("root", "url", path);
    }

    public void modifyImlFile(final Project project, final HatchejModel hatchejModel)
            throws Exception {
        final File imlFile = new File(project.getProjectDir().getAbsoluteFile(), project.getName() + ".iml");
        if (!imlFile.exists()) {
            return; //not running within IntelliJ workspace
        }

        final Node imlRootNode = new XmlParser().parse(imlFile);

        if (hatchejModel != null) {
            final Node componentNode = xmlPath(imlRootNode, node -> node.name().equals("component") && "NewModuleRootManager".equals(node.attribute("name")));
            final Node contentFoldersNode = xmlPath(componentNode, node -> node.name().equals("content") && "file://$MODULE_DIR$".equals(node.attribute("url")));

            ((NodeList) contentFoldersNode.get("sourceFolder")).forEach(sourceFolder -> contentFoldersNode.remove((Node) sourceFolder));

            hatchejModel.getSourceFolders().forEach(sourceFolder -> contentFoldersNode.append(sourceContentFolderNode(project, sourceFolder, false)));
            hatchejModel.getTestSourceFolders().forEach(sourceFolder -> contentFoldersNode.append(sourceContentFolderNode(project, sourceFolder, true)));

            ((NodeList) contentFoldersNode.get("excludeFolder")).forEach(excludeFolder -> contentFoldersNode.remove((Node) excludeFolder));
            hatchejModel.getExcludeFolders().forEach(excludeFolder -> contentFoldersNode.append(excludeContentFolderNode(project, excludeFolder)));

            ((NodeList) componentNode.get("orderEntry")).stream()
                    .filter(n -> "module".equals(((Node) n).attribute("type")))
                    .forEach(excludeFolder -> componentNode.remove((Node) excludeFolder));
            hatchejModel.getProjectDependencies().forEach(path -> componentNode.append(projectModuleNode(path)));

            // rewrite project.iml file
            FileWriter fileWriter = new FileWriter(imlFile);
            new XmlNodePrinter(new PrintWriter(fileWriter)).print(imlRootNode);
        } else {
            throw new IllegalStateException("HatchejImlAction was called without any HatchejModel set on project " + project.getPath());
        }
    }

    /**
     * Converting the path to the &lt;library&gt;.xml naming convention.  The '-', '.', and '/' are converted to '_'.
     *
     * @param path the path to convert
     * @return a converted path
     */
    private static String normalizePath(String path) {
        return path != null ? path.replace('/', '_').replace('-', '_').replace('.', '_') : null;
    }

    private static Node projectModuleNode(String path) {
        //<orderEntry type="module" module-name="lib2" exported="" />
        HashMap<String, String> attributes = new HashMap<>(3);
        attributes.put("type", "module");
        attributes.put("module-name", StringUtils.substringBefore(path, ":"));
        attributes.put("exported", "");
        return new Node(null, "orderEntry", attributes);
    }

    private static Node excludeContentFolderNode(Project project, File excludeFolder) {
        ////<excludeFolder url="file://$MODULE_DIR$/.gradle" />
        return new Node(null, "excludeFolder", Collections.singletonMap("url", "file://$MODULE_DIR$" + excludeFolder.getAbsolutePath().substring(project.getProjectDir().getAbsolutePath().length())));
    }

    private static Node sourceContentFolderNode(
            Project project,
            File sourceFolder,
            boolean isTestFolder) {
        //<sourceFolder url="file://$MODULE_DIR$/src" isTestSource="false" />
        HashMap<String, String> attributes = new HashMap<>(2);
        attributes.put("url", "file://$MODULE_DIR$" + sourceFolder.getAbsolutePath().substring(project.getProjectDir().getAbsolutePath().length()));
        attributes.put("isTestSource", Boolean.toString(isTestFolder));
        return new Node(null, "sourceFolder", attributes);
    }

    private static Node xmlPath(Node xmlNode, Predicate<Node>... finders) {
        Node value = xmlNode;
        for (Predicate<Node> finder : finders) {
            final Optional<Node> first = value.children().stream()
                    .filter(finder)
                    .findFirst();
            if (first.isPresent()) {
                value = first.get();
            } else {
                throw new IllegalArgumentException("Could not find requested node at " + value.name() + ". There are " + Arrays.toString(value.children().toArray()));
            }
        }
        return value;
    }
}
