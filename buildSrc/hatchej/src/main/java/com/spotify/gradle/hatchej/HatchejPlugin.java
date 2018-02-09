package com.spotify.gradle.hatchej;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class HatchejPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.getExtensions().create("hatchej", HatchejModel.class);

        project.afterEvaluate(HatchejPlugin::configurePlugin);
    }

    private static void configurePlugin(Project project) {
        try {
            new HatchejImlAction().modifyImlFile(project, project.getExtensions().getByType(HatchejModel.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
