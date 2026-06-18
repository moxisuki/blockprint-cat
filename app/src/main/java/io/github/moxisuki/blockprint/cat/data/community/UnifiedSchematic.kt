package io.github.moxisuki.blockprint.cat.data.community

data class UnifiedSchematic(
    val source: CommunitySource,
    val id: String,
    val name: String,
    val author: String,
    val heat: Int? = null,
    val downloads: Int? = null,
    val size: Triple<Int, Int, Int>? = null,
    val sizeText: String? = null,
    val tags: List<String> = emptyList(),
    val description: String = "",
    val updateTime: String = "",
    val typeForSuffix: Int = 0,
    val stress: String? = null,
    val coverUrl: String? = null,
    val cmsDownloadId: Int? = null,
)
