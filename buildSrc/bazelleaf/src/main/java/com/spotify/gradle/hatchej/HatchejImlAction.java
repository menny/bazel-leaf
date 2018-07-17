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

/**
 * Perform not-so-subtle changes to IntelliJ's IML and XML files according to the provided {@link HatchejModel}.
 */
public class HatchejImlAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(HatchejImlAction.class);

    HatchejImlAction() {
    }

    /**
     * Returns true if a library file for any of the dependencies was created otherwise false.  This
     * will also return false if the IDEA libraries directory doesn't exist and we cannot add a file.
     */
    public boolean addLibraryFiles(final Project project, final HatchejModel hatchejModel) {
        boolean added = false;
        File ideaLibrariesDir = new File(project.getRootDir(), ".idea/libraries");
        if (!ideaLibrariesDir.isDirectory() || !ideaLibrariesDir.exists()) {
            return added; // not running within IntelliJ workspace
        }

        for (String dependency : hatchejModel.getLibraryDependencies()) {
            added |= createLibraryFileIfNotExists(ideaLibrariesDir, dependency);
        }
        return added;
    }

    static boolean createLibraryFileIfNotExists(File ideaLibrariesDir, String dependency) {
        final boolean created;
        final String path = getJarUrl(dependency);
        final String name = getFilenameWithoutExtension(path);
        final String normalizedName = normalizePath(name);

        final File ideaLibraryFile = new File(ideaLibrariesDir, normalizedName + ".xml");
        if (!ideaLibraryFile.exists()) {
            final Node componentRootNode = createNode("component", "name", "libraryTable");

            final Node libraryNode = createLibraryNode(name);
            componentRootNode.append(libraryNode);

            Node classes = libraryNode.appendNode("CLASSES");
            classes.append(createRootNode(path));

            libraryNode.appendNode("JAVADOC");
            libraryNode.appendNode("SOURCES");

            try (final FileWriter fileWriter = new FileWriter(ideaLibraryFile)) {
                new XmlNodePrinter(new PrintWriter(fileWriter)).print(componentRootNode);
                created = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.debug("IDEA library file {} already exists", ideaLibraryFile.getAbsolutePath());
            created = false;
        }
        return created;
    }

    /**
     * take the name of the file and normalize without the extension.
     */
    private static String getFilenameWithoutExtension(String path) {
        return StringUtils.substringBeforeLast(new File(path).getName(), ".");
    }

    static String getJarUrl(String path) {
        final String prefix = "jar://";
        final String suffix = "!/";

        String jarUrl = path;
        if (!StringUtils.startsWith(path, prefix) && !StringUtils.endsWith(path, suffix)) {
            jarUrl = StringUtils.join(prefix, path, suffix);
        } else {
            LOGGER.warn("Unable to create the URL for the library {}", path);
        }
        return jarUrl;
    }

    private static Node createNode(String nodeName, String attributeName, String attributeValue) {
        Map<String, String> attributes = new HashMap<>(1);
        attributes.put(attributeName, attributeValue);
        return new Node(null, nodeName, attributes);
    }

    private static Node createLibraryNode(String name) {
        return createNode("library", "name", name);
    }

    private static Node createRootNode(String path) {
        return createNode("root", "url", path);
    }

    @SuppressWarnings("unchecked")
    public void modifyImlFile(final Project project, final HatchejModel hatchejModel) throws Exception {
        final File projectDir = project.getProjectDir().getAbsoluteFile();

        // backward-compatible with existing IML files using old name format
        final File imlPathFile = new File(projectDir,
                project.getPath().substring(1).replace(':', '.') + ".iml");
        // new format for IML file names
        final File imlNameFile = new File(projectDir, project.getName() + ".iml");

        if (!imlPathFile.exists() && !imlNameFile.exists()) {
            LOGGER.warn(
                    "Could not find an IML file {} or {} in project dir {}. Assuming this is not an IntelliJ project.",
                    imlNameFile.getName(),
                    imlPathFile.getName(),
                    imlPathFile.getParentFile().getAbsolutePath());
            return; //not running within IntelliJ workspace
        }

        final File imlFile = imlPathFile.exists() ? imlPathFile : imlNameFile;
        final Node imlRootNode = new XmlParser().parse(imlFile);

        final Node componentNode = xmlPath(imlRootNode, node -> node.name().equals("component") && "NewModuleRootManager".equals(node.attribute("name")));
        final Node contentFoldersNode = xmlPath(componentNode, node -> node.name().equals("content") && "file://$MODULE_DIR$".equals(node.attribute("url")));

        ((NodeList) contentFoldersNode.get("sourceFolder")).forEach(sourceFolder -> contentFoldersNode.remove((Node) sourceFolder));

        hatchejModel.getSourceFolders().forEach(sourceFolder -> contentFoldersNode.append(sourceContentFolderNode(project, sourceFolder, false)));
        hatchejModel.getTestSourceFolders().forEach(sourceFolder -> contentFoldersNode.append(sourceContentFolderNode(project, sourceFolder, true)));

        ((NodeList) contentFoldersNode.get("excludeFolder")).forEach(excludeFolder -> contentFoldersNode.remove((Node) excludeFolder));
        hatchejModel.getExcludeFolders().forEach(excludeFolder -> contentFoldersNode.append(excludeContentFolderNode(project, excludeFolder)));

        ((NodeList) componentNode.get("orderEntry")).stream()
                .filter(n -> "module".equals(((Node) n).attribute("type")))
                .forEach(orderEntry -> componentNode.remove((Node) orderEntry));
        hatchejModel.getProjectDependencies().forEach(path -> componentNode.append(projectModuleNode(path, false)));
        hatchejModel.getProjectTestDependencies().forEach(path -> componentNode.append(projectModuleNode(path, true)));

        ((NodeList) componentNode.get("orderEntry")).stream()
                .filter(n -> "library".equals(((Node) n).attribute("type")))
                .forEach(orderEntry -> componentNode.remove((Node) orderEntry));
        hatchejModel.getLibraryDependencies().forEach(path -> componentNode.append(libraryModuleNode(path, false)));
        hatchejModel.getLibraryTestDependencies().forEach(path -> componentNode.append(libraryModuleNode(path, true)));

        //outputs
        ((NodeList) componentNode.get("output"))
                .forEach(orderEntry -> componentNode.remove((Node) orderEntry));
        hatchejModel.getProjectOutputs().forEach(path -> componentNode.append(outputNode(path, false)));

        // rewrite project.iml file
        try (FileWriter fileWriter = new FileWriter(imlFile)) {
            new XmlNodePrinter(new PrintWriter(fileWriter)).print(imlRootNode);
        }

        //external libraries
        final File ideaLibrariesDir = new File(project.getRootDir(), ".idea/libraries");
        if (!ideaLibrariesDir.exists() && !ideaLibrariesDir.mkdirs()) {
            throw new IOException("Failed to create idea-libraries folder at " + ideaLibrariesDir.getAbsolutePath());
        }
        hatchejModel.getLibraryDependencies().forEach(dependency -> createLibraryFileIfNotExists(ideaLibrariesDir, dependency));
        hatchejModel.getLibraryTestDependencies().forEach(dependency -> createLibraryFileIfNotExists(ideaLibrariesDir, dependency));
    }

    /**
     * Converting the path to the &lt;library&gt;.xml naming convention.  The '-', '.', and '/' are converted to '_'.
     *
     * @param path the path to convert
     * @return a converted path
     */
    private static String normalizePath(String path) {
        return StringUtils.replaceChars(path, "/-.", "___");
    }

    private static Node projectModuleNode(String path, boolean forTest) {
        Map<String, String> attributes = new HashMap<>(3);
        attributes.put("type", "module");
        attributes.put("module-name", StringUtils.defaultIfBlank(StringUtils.substringAfter(path, ":"), path).replace(':', '.'));
        attributes.put("exported", "");
        if (forTest) {
            attributes.put("scope", "TEST");
        }
        return new Node(null, "orderEntry", attributes);
    }

    private static Node libraryModuleNode(String path, boolean forTest) {
        Map<String, String> attributes = new HashMap<>(4);
        attributes.put("type", "library");
        attributes.put("name", getFilenameWithoutExtension(path));
        attributes.put("exported", "");
        if (forTest) {
            attributes.put("scope", "TEST");
        }
        attributes.put("level", "project");
        return new Node(null, "orderEntry", attributes);
    }

    private static Node outputNode(File path, boolean forTest) {
        final Map<String, String> attributes = Collections.singletonMap("url", "file://" + path.getAbsolutePath());
        return new Node(null, forTest ? "output-test" : "output", attributes);
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

    @SuppressWarnings("unchecked")
    static Node xmlPath(Node xmlNode, Predicate<Node>... finders) {
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
