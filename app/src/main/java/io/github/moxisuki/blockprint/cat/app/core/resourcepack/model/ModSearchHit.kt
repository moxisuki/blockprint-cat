package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

data class ModSearchHit(
    val slug: String,
    val title: String,
    val description: String,
    val projectId: String,
    val downloads: Int = 0,
    val iconUrl: String? = null,
)