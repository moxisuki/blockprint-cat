package io.github.moxisuki.blockprint.cat.data.vanilla

import io.github.moxisuki.blockprint.core.Litematic

/** Detects which asset namespaces a blueprint requires for rendering. */
object AssetNamespaceResolver {

    /** Extract all namespaces used by a blueprint's block palette. */
    fun resolve(litematic: Litematic): Set<String> {
        val namespaces = mutableSetOf<String>()
        for (region in litematic.regions) {
            for (block in region.palette.entries) {
                namespaces.add(block.name.substringBefore(':', block.name))
            }
        }
        return namespaces
    }
}
