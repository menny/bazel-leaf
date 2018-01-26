def _aspect_impl(target, ctx):
    print(ctx.rule.kind)
    return []

print_aspect = aspect(
    implementation = _aspect_impl,
    attr_aspects = [],
)