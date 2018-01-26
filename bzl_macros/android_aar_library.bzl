def android_aar_library(name, android_library):
  print(name)
  native.genrule(
    visibility = ["//visibility:public"],
    name = name,
    outs = [name + ".aar"],
    srcs = [android_library],
    tools = ["@bazel_tools//tools/android:busybox"],
    cmd = """
# fetching a few of the outputs from the android_library dep
RESOURCES_JAR=`echo "$(locations {})" | awk -F  " " '/ / {}'`
CLASSES_JAR=`echo "$(locations {})" | awk -F  " " '/ / {}'`
BASE_OUT_PUT_FOLDER=$$(dirname $$CLASSES_JAR)

# creating required, yet unused, files for the conversion
touch $$BASE_OUT_PUT_FOLDER/R.txt
cat > $$BASE_OUT_PUT_FOLDER/AndroidManifest.xml <<EOF
<manifest
  xmlns:android="http://schemas.android.com/apk/res/android"
  package="does.not.matter">
  <uses-sdk android:minSdkVersion="999"/>
</manifest>
EOF

mkdir -p $$(dirname $(location {}.aar))
sh $(location @bazel_tools//tools/android:busybox) --tool GENERATE_AAR \
    -- --manifest $$BASE_OUT_PUT_FOLDER/AndroidManifest.xml \
    --rtxt $$BASE_OUT_PUT_FOLDER/R.txt \
    --classes $$CLASSES_JAR \
    --mainData $$RESOURCES_JAR::$$BASE_OUT_PUT_FOLDER/AndroidManifest.xml \
    --aarOutput $(location {}.aar)

echo $(location {}.aar)
""".format(android_library, "{print $$3}", android_library, "{print $$4}", name, name, name),
  )
