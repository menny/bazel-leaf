"""
____Loading complete.  Analyzing...
DEBUG: /Users/menny/dev/spotify/bazel-leaf/build/bazel_aspects/__lib3_jar/get_deps.bzl:7:13: <target //lib4:jar>
____Found 1 target...
____Elapsed time: 0.134s, Critical Path: 0.00s
"""

def _aspect_impl(target, ctx):
    # Make sure the rule has a srcs attribute.
    if hasattr(ctx.rule.attr, 'deps'):
        # Iterate through the files that make up the sources and
        # print their paths.
        for dep in ctx.rule.attr.deps:
            print(dep)
    return []

print_aspect = aspect(
    implementation = _aspect_impl,
    attr_aspects = [],
)