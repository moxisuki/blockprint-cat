package io.github.moxisuki.blockprint.cat.app.feature.about.data

import androidx.compose.runtime.Immutable

@Immutable
data class HitokotoQuote(
    val text: String,
    val from: String,
    val fromWho: String,
)
