package io.github.moxisuki.blockprint.cat.data.community

data class CmsSearchPage(val page: Int, val items: List<CmsListItem>)

data class CmsListItem(
    val detailId: Int,
    val title: String,
    val datetime: String,
    val displayDate: String,
    val author: String,
    val size: String,
    val description: String,
    val stress: String,
    val downloads: Int,
)

data class CmsDetail(
    val detailId: Int,
    val title: String,
    val downloadId: Int?,
    val description: String,
    val coverUrl: String?,
    val materials: List<UnifiedMaterial>,
    val production: List<UnifiedMaterial>,
    val dependencies: List<String>,
    val stress: String?,
    val sizeXYZ: Triple<Int, Int, Int>?,
    val tags: List<String>,
    val author: String,
)

data class CmsDownloadInfo(
    val downloadId: Int,
    val fileUrl: String?,
    val filename: String?,
)

open class CmsException(message: String) : RuntimeException(message)

class CmsCloudflareException(message: String = "需要浏览器验证") : CmsException(message)
