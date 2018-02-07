android_sdk_repository(
    name = "android_sdk_27",
    api_level = 27,
)

# see https://github.com/bazelbuild/migration-tooling#transitive_maven_jar
http_archive(
	name = "trans_maven_jar",
	url = "https://github.com/bazelbuild/migration-tooling/archive/a5e687403ec59db52383a2c0996003e5ad18b6cf.zip",
	type = "zip",
	strip_prefix = "migration-tooling-a5e687403ec59db52383a2c0996003e5ad18b6cf",
)

load("@trans_maven_jar//transitive_maven_jar:transitive_maven_jar.bzl", "transitive_maven_jar")

transitive_maven_jar(
	name = "third_party",
	artifacts = [
	    "junit:junit:4.12",
	    "com.google.guava:guava:20.0"
	    ],
	repositories = [
	    "http://repo1.maven.org/maven2/",
	    #"https://artifactory.spotify.net/artifactory/android-repo/",
	    ]
)

#load("@third_party//:generate_workspace.bzl", "generated_maven_jars")
#generated_maven_jars()

# Robolectric support
http_archive(
 name = "robolectric",
 url = "https://github.com/robolectric/robolectric/archive/bf4f7f9e3aa16f514f626bcff739749a3a9350c3.tar.gz",
 strip_prefix = "robolectric-bf4f7f9e3aa16f514f626bcff739749a3a9350c3"
)
load("@robolectric//bazel:setup_robolectric.bzl", "setup_robolectric")
setup_robolectric()