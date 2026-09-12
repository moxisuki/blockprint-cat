package io.github.moxisuki.blockprint.cat.app.feature.community.data

import android.text.Html
import androidx.compose.runtime.Immutable

internal const val CmsBaseUrl = "https://www.creativemechanicserver.com"

@Immutable
internal data class CmsSearchPage(
    val page: Int,
    val items: List<CmsListItem>,
)

@Immutable
internal data class CmsListItem(
    val detailId: Int,
    val title: String,
    val datetime: String,
    val displayDate: String,
    val author: String,
    val size: String,
    val description: String,
    val stress: String,
    val downloads: Int,
    val coverUrl: String?,
)

@Immutable
internal data class CmsDetail(
    val detailId: Int,
    val title: String,
    val downloadId: Int?,
    val description: String,
    val coverUrl: String?,
    val materials: List<CmsMaterial>,
    val production: List<CmsMaterial>,
    val dependencies: List<String>,
    val stress: String?,
    val size: Triple<Int, Int, Int>?,
    val author: String,
)

@Immutable
internal data class CmsMaterial(
    val blockId: String,
    val displayName: String,
    val iconUrl: String?,
    val count: Int,
    val countText: String,
)

@Immutable
internal data class CmsDownloadInfo(
    val downloadId: Int,
    val fileUrl: String?,
    val fileName: String?,
)

internal object CmsCommunityParser {
    private val listResultRegex = Regex(
        """<a href="[^"]*?/detail/(\d+)/?" class="list_result">(.*?)</a>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val titleRegex = Regex("""<h2 class="b title oh">([^<]+)</h2>""")
    private val timeRegex = Regex("""<time[^>]*datetime="([^"]+)"[^>]*>([^<]+)</time>""")
    private val authorRegex = Regex("""作者：</div><div class="op5 b nw oh author">([^<]*)</div>""")
    private val sizeRegex = Regex("""尺寸：([^<]+)</div>""")
    private val descriptionRegex = Regex("""<div class="desc oh ow">([^<]+)</div>""")
    private val stressRegex = Regex("""应力：([^<]+)</div>""")
    private val downloadsRegex = Regex("""下载量：(\d+)""")
    private val listCoverRegex = Regex("""<div class="cover">\s*<img src="([^"]+)"""")

    private val detailTitleRegex = Regex("""<title>([^<]+)-CMS""")
    private val detailDownloadRegex = Regex("""<a href="[^"]*?/download/(\d+)/"[^>]*>[^<]*</a>""")
    private val detailDescriptionRegex = Regex(
        """<div class="content_box oh ow">(.+?)</div>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val coverRegex = Regex("""<div class="cover">\s*<img src="([^"]+)"""")
    private val stressDetailRegex = Regex("""<b>应力</b>\s*<span class="text_theme_r">\s*([^<]+?)\s*</span>""")
    private val sizeXRegex = Regex("""<b>x尺寸</b>\s*<span class="text_theme_r">\s*(\d+)\s*</span>""")
    private val sizeYRegex = Regex("""<b>y尺寸</b>\s*<span class="text_theme_r">\s*(\d+)\s*</span>""")
    private val sizeZRegex = Regex("""<b>z尺寸</b>\s*<span class="text_theme_r">\s*(\d+)\s*</span>""")
    private val authorDetailRegex = Regex("""<b class="op7 gap_t f">作者：([^<]*)</b>""")
    private val dependencyBlockRegex = Regex(
        """<span class="gap_r">依赖</span>(.*?)<div class="f gap_cx""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val dependencyItemRegex = Regex("""alt="([^"]+)"[^>]*>\s*<div>([^<]+)</div>""")
    private val tipBoxRegex = Regex(
        """<div class="tip_box">.*?""" +
            """<img src="(/upload/Tag/[^"]+\.png)"\s+alt="([^"]*)".*?""" +
            """<span>\s*([^<]+?)\s*</span>.*?""" +
            """<div class="fss op5">([^<]+)</div>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val downloadHrefRegex = Regex("""<a href="(/upload/blueprint/[^"]+)"\s+download="([^"]+)"""")

    fun parseSearchHtml(html: String, page: Int): CmsSearchPage {
        val items = listResultRegex.findAll(html).mapNotNull { match ->
            val detailId = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val body = match.groupValues[2]
            val timeMatch = timeRegex.find(body)
            CmsListItem(
                detailId = detailId,
                title = titleRegex.find(body)?.groupValues?.get(1).toText(),
                datetime = timeMatch?.groupValues?.get(1).orEmpty(),
                displayDate = timeMatch?.groupValues?.get(2).toText(),
                author = authorRegex.find(body)?.groupValues?.get(1).toText(),
                size = sizeRegex.find(body)?.groupValues?.get(1).toText(),
                description = descriptionRegex.find(body)?.groupValues?.get(1).toText(),
                stress = stressRegex.find(body)?.groupValues?.get(1).toText(),
                downloads = downloadsRegex.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                coverUrl = listCoverRegex.find(body)?.groupValues?.get(1)?.trim(),
            )
        }.toList()
        return CmsSearchPage(page = page, items = items)
    }

    fun parseDetailHtml(html: String, detailId: Int): CmsDetail {
        val title = detailTitleRegex.find(html)?.groupValues?.get(1).toText()
        val downloadId = detailDownloadRegex.find(html)?.groupValues?.get(1)?.toIntOrNull()
        val description = detailDescriptionRegex.find(html)?.groupValues?.get(1)
            ?.replace(Regex("""<[^>]+>"""), "")
            .toText()
        val stress = stressDetailRegex.find(html)?.groupValues?.get(1)
            ?.replace(Regex("""\s+"""), " ")
            .toText()
            .takeIf { it.isNotBlank() }
        val size = listOf(sizeXRegex, sizeYRegex, sizeZRegex)
            .map { it.find(html)?.groupValues?.get(1)?.toIntOrNull() }
            .takeIf { values -> values.all { it != null } }
            ?.let { values -> Triple(values[0] ?: 0, values[1] ?: 0, values[2] ?: 0) }
        return CmsDetail(
            detailId = detailId,
            title = title,
            downloadId = downloadId,
            description = description,
            coverUrl = coverRegex.find(html)?.groupValues?.get(1)?.trim(),
            materials = parseTipBoxesInSection(html, "材料统计", "canvas_container"),
            production = parseTipBoxesInSection(html, "<span>生产</span>", "<h4 class=\"gap_t\">注释"),
            dependencies = parseDependencies(html),
            stress = stress,
            size = size,
            author = authorDetailRegex.find(html)?.groupValues?.get(1).toText(),
        )
    }

    fun parseDownloadHtml(html: String, downloadId: Int): CmsDownloadInfo {
        val match = downloadHrefRegex.find(html)
        return CmsDownloadInfo(
            downloadId = downloadId,
            fileUrl = match?.groupValues?.get(1),
            fileName = match?.groupValues?.get(2).toText().ifBlank { null },
        )
    }

    private fun parseTipBoxesInSection(
        html: String,
        startMarker: String,
        endMarker: String,
    ): List<CmsMaterial> {
        val start = html.indexOf(startMarker)
        if (start < 0) return emptyList()
        val endIndex = html.indexOf(endMarker, start)
        val localEnd = if (endIndex > start) endIndex else (start + 20_000).coerceAtMost(html.length)
        return tipBoxRegex.findAll(html.substring(start, localEnd)).map { match ->
            CmsMaterial(
                blockId = match.groupValues[4].toText().toMinecraftBlockId(),
                displayName = match.groupValues[2].toText(),
                iconUrl = match.groupValues[1].trim(),
                count = parseCountText(match.groupValues[3]),
                countText = match.groupValues[3].toText(),
            )
        }.toList()
    }

    private fun parseDependencies(html: String): List<String> {
        val block = dependencyBlockRegex.find(html)?.groupValues?.get(1) ?: return emptyList()
        return dependencyItemRegex.findAll(block)
            .map { it.groupValues[2].toText() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun parseCountText(text: String): Int {
        val value = text.toText()
        return when {
            value.endsWith("k", ignoreCase = true) ->
                value.dropLast(1).toDoubleOrNull()?.times(1000)?.toInt() ?: 0
            value.endsWith("万") ->
                value.dropLast(1).toDoubleOrNull()?.times(10000)?.toInt() ?: 0
            else -> value.toIntOrNull() ?: 0
        }
    }

    private fun String?.toText(): String =
        this
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString() }
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            .orEmpty()

    private fun String.toMinecraftBlockId(): String {
        val stripped = removePrefix("block.")
        val dotIndex = stripped.indexOf('.')
        return if (dotIndex > 0) {
            stripped.substring(0, dotIndex) + ":" + stripped.substring(dotIndex + 1)
        } else {
            "minecraft:$stripped"
        }
    }
}

internal open class CmsCommunityException(message: String) : RuntimeException(message)

internal class CmsCloudflareException(
    message: String = "CMS 需要先在浏览器完成验证",
) : CmsCommunityException(message)
