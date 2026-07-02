package io.github.moxisuki.blockprint.cat.data.vanilla

import io.github.moxisuki.blockprint.core.model.BlockPrintDocument

/** Detects which asset namespaces a blueprint requires for rendering. */
object AssetNamespaceResolver {

    /** Extract all namespaces used by a blueprint's block palette. */
    fun resolve(document: BlockPrintDocument): Set<String> {
        val namespaces = mutableSetOf<String>()
        for (region in document.regions) {
            for (block in region.palette.entries) {
                namespaces.add(block.name.substringBefore(':', block.name))
            }
        }
        return namespaces
    }
}
