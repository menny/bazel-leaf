package com.spotify.gradle.bazel

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Exec

import static org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE

class BazelLeafPlugin implements Plugin<Project> {
    final static DEFAULT_BIN_PATH = '/usr/local/bin/bazel'

    void apply(Project project) {
        project.with {
            String pathToBazelBin = getBazelBinPath(project)
            BazelLeafConfig config = extensions.create('bazel', BazelLeafConfig)
            String outputPath = path.replace(':', '/')
            String bazelPath = "/${path.replace(':', '/')}"
            //note: Bazel must use the same folder for all outputs, so we use the build-folder of the root-project
            final String bazelBuildDir = "${rootProject.buildDir.absolutePath}/bazel-build"

            Task bazelBuildTask = task('bazelBuild', type: Exec) {
                workingDir rootDir
                commandLine pathToBazelBin, 'build', "--symlink_prefix=${bazelBuildDir}/", "${bazelPath}:${config.target}"
            }

            configurations {
                'default' {}
                implementation {
                    attributes.attribute(USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_API))
                }
                runtime {
                    attributes.attribute(USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                }
            }

            BazelArtifact bazelArtifact = new BazelArtifact(bazelBuildTask, new File("${bazelBuildDir}/bin/${outputPath}/lib${config.target}.jar"))

            artifacts {
                runtime bazelArtifact
                implementation bazelArtifact
            }

            apply plugin: 'idea'
            idea {
                module {
                    sourceDirs += file('src/main/java')
                }
            }
        }
    }

    static String getBazelBinPath(Project project) {
        Properties properties = new Properties()
        File propertiesFile = project.rootProject.file('local.properties')
        if (propertiesFile.exists()) {
            DataInputStream input = propertiesFile.newDataInputStream()
            properties.load(input)
            input.close()
        }

        properties.get('bazel.bin.path', DEFAULT_BIN_PATH)
    }
}


