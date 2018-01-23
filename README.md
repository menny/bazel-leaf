# 🍂🐘 Bazel Leaf

A Gradle plugin that builds modules using Bazel.

# Setup

## Development setup
* ensure you have Bazel installed. Follow the instructions [here](https://docs.bazel.build/versions/master/install.html)
* (optionally) if `bazel` binary in not in PATH set the path to it in `local.properties`:
  * `bazel.bin.path=/usr/local/bin/bazel`
* Open this project using Android Studio.

# Repo structure:

```
app - an Android app which is built using Gradle
  |
  |_ lib1 - a Java lib that is built using Gradle
  |    |
  |    |_ Lib2 - a Java lib that is build using Bazel
  |
  |_ lib3 - a Java lib that is built using Bazel
       |
       |_ lib4 - a Java lib that is built using Bazel

