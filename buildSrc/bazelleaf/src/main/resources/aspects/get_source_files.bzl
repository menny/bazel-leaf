def _aspect_impl(target, ctx):
    # Make sure the rule has a srcs attribute.
    if hasattr(ctx.rule.attr, 'srcs'):
        # Iterate through the files that make up the sources and
        # print their paths.
        for src in ctx.rule.attr.srcs:
            for f in src.files:
                print(f.path)
    return []

print_aspect = aspect(
    implementation = _aspect_impl,
    attr_aspects = [],
)