package io.github.moxisuki.blockprint.cat.data.blueprint

import io.github.moxisuki.blockprint.core.Litematic

data class FullBlueprint(
    val meta: BlueprintMeta,
    val materials: List<Pair<String, Int>>,
    val raw: Litematic,
)
