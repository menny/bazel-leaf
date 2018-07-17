
def _aspect_impl(target, ctx):
    #print("target {}".format(target))
    # Make sure the rule has a srcs attribute.
    # print("attr {}".format(ctx.rule.attr))
    if hasattr(ctx.rule.attr, 'deps'):
        # Iterate through the files that make up the sources and
        # print their paths.
        for dep in ctx.rule.attr.deps:
            print("{}<FILES:>{}".format(dep.label, dep.files.to_list()))
    if hasattr(ctx.rule.attr, 'runtime_deps'):
        # Iterate through the files that make up the sources and
        # print their paths.
        for dep in ctx.rule.attr.runtime_deps:
            print("{}<FILES:>{}".format(dep.label, dep.files.to_list()))
    return []

print_aspect = aspect(
    implementation = _aspect_impl,
    attr_aspects = ['exports', 'runtime_deps'],
)
