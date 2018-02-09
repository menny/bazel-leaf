package com.spotify.gradle.hatchej;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;

public class HatchejImlAction {

    public void modifyImlFile(final Project project, final HatchejModel hatchejModel) throws Exception {
        final File imlFile = new File(project.getProjectDir().getAbsoluteFile(), project.getName() + ".iml");
        if (!imlFile.exists()) return;//not running within IntelliJ workspace

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

    private Node projectModuleNode(String path) {
        //<orderEntry type="module" module-name="lib2" exported="" />
        HashMap<String, String> attributes = new HashMap<>(2);
        attributes.put("type", "module");
        attributes.put("module-name", path.substring(1 + path.lastIndexOf(":")));
        attributes.put("exported", "");
        return new Node(null, "orderEntry", attributes);
    }

    private Node excludeContentFolderNode(Project project, File excludeFolder) {
        ////<excludeFolder url="file://$MODULE_DIR$/.gradle" />
        return new Node(null, "excludeFolder", Collections.singletonMap("url", "file://$MODULE_DIR$" + excludeFolder.getAbsolutePath().substring(project.getProjectDir().getAbsolutePath().length())));
    }

    private Node sourceContentFolderNode(Project project, File sourceFolder, boolean isTestFolder) {
        //<sourceFolder url="file://$MODULE_DIR$/src" isTestSource="false" />
        HashMap<String, String> attributes = new HashMap<>(2);
        attributes.put("url", "file://$MODULE_DIR$" + sourceFolder.getAbsolutePath().substring(project.getProjectDir().getAbsolutePath().length()));
        attributes.put("isTestSource", Boolean.toString(isTestFolder));
        return new Node(null, "sourceFolder", attributes);
    }

    private Node xmlPath(Node xmlNode, Predicate<Node>... finders) {
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
