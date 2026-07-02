package io.github.moxisuki.blockprint.cat.data.blueprint

import io.github.moxisuki.blockprint.core.model.BlockPrintDocument

data class FullBlueprint(
    val meta: BlueprintMeta,
    val materials: List<Pair<String, Int>>,
    /** The parsed litematic. Null after GLB generation to free memory;
     *  reloaded from disk on Regenerate. */
    val raw: BlockPrintDocument? = null,
)
