android_sdk_repository(
    name = "android_sdk_27",
    api_level = 27,
    build_tools_version = "27.0.3",
    path = "/Users/menny/dev/sdk/")

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
	repositories = ["https://artifactory.spotify.net/artifactory/android-repo/"]
)

load("@third_party//:generate_workspace.bzl", "generated_maven_jars")
generated_maven_jars()
