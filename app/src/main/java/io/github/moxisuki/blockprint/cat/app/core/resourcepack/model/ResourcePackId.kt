package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

@JvmInline
value class ResourcePackId(val value: String) {
    val isVanilla: Boolean get() = value == VANILLA_RAW
    val modSlug: String get() = value.removePrefix(MOD_PREFIX)

    companion object {
        const val VANILLA_RAW = "vanilla"
        const val MOD_PREFIX = "mod:"

        val Vanilla: ResourcePackId = ResourcePackId(VANILLA_RAW)
        fun mod(slug: String): ResourcePackId = ResourcePackId("$MOD_PREFIX$slug")
    }
}