"""
bazel-out/darwin_x86_64-fastbuild/bin/andlib/aarbin_resources-src.jar
"""

def _aspect_impl(target, ctx):
    for f in target.files:
        print(f.path)
    return []

print_aspect = aspect(
    implementation = _aspect_impl,
    attr_aspects = [],
)