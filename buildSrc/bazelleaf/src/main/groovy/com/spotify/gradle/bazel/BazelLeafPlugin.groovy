package com.spotify.gradle.bazel

import org.gradle.api.Plugin
import org.gradle.api.Project

class BazelLeafPlugin implements Plugin<Project>  {

    private Project project

    void apply(Project project) {
        this.project = project

        println "Welcome to Bazel land!"
    }
}
