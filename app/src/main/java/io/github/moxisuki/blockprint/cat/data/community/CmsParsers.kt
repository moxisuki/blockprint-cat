package io.github.moxisuki.blockprint.cat.data.community

object CmsParsers {

    /** "4.9k" → 4900;"1.2万" → 12000;"7" → 7;空/非法 → 0。 */
    fun parseCountText(text: String): Int {
        val s = text.trim()
        if (s.isEmpty()) return 0
        return when {
            s.endsWith("k", ignoreCase = true) ->
                (s.dropLast(1).toDoubleOrNull()?.times(1000))?.toInt() ?: 0
            s.endsWith("万") ->
                (s.dropLast(1).toDoubleOrNull()?.times(10000))?.toInt() ?: 0
            else -> s.toIntOrNull() ?: 0
        }
    }

    fun mapCmsToMinecraftBlockId(cmsId: String): String {
        val stripped = cmsId.removePrefix("block.")
        val dotIdx = stripped.indexOf('.')
        return if (dotIdx > 0) {
            stripped.substring(0, dotIdx) + ":" + stripped.substring(dotIdx + 1)
        } else {
            "minecraft:$stripped"
        }
    }

    // —— search 页解析 ——

    private val LIST_RESULT_RE = Regex(
        """<a href="[^"]*?/detail/(\d+)/?" class="list_result">(.*?)</a>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val TITLE_RE = Regex("""<h2 class="b title oh">([^<]+)</h2>""")
    private val TIME_RE = Regex("""<time[^>]*datetime="([^"]+)"[^>]*>([^<]+)</time>""")
    private val AUTHOR_RE = Regex("""作者：</div><div class="op5 b nw oh author">([^<]*)</div>""")
    private val SIZE_RE = Regex("""尺寸：([^<]+)</div>""")
    private val DESC_RE = Regex("""<div class="desc oh ow">([^<]+)</div>""")
    private val STRESS_RE = Regex("""应力：([^<]+)</div>""")
    private val DOWNLOADS_RE = Regex("""下载量：(\d+)""")

    fun parseSearchHtml(html: String, page: Int): CmsSearchPage {
        val items = LIST_RESULT_RE.findAll(html).map { m ->
            val detailId = m.groupValues[1].toInt()
            val body = m.groupValues[2]
            val titleM = TITLE_RE.find(body)
            val timeM = TIME_RE.find(body)
            val authorM = AUTHOR_RE.find(body)
            val sizeM = SIZE_RE.find(body)
            val descM = DESC_RE.find(body)
            val stressM = STRESS_RE.find(body)
            val dlM = DOWNLOADS_RE.find(body)
            CmsListItem(
                detailId = detailId,
                title = titleM?.groupValues?.get(1)?.trim().orEmpty(),
                datetime = timeM?.groupValues?.get(1).orEmpty(),
                displayDate = timeM?.groupValues?.get(2)?.trim().orEmpty(),
                author = authorM?.groupValues?.get(1)?.trim().orEmpty(),
                size = sizeM?.groupValues?.get(1)?.trim().orEmpty(),
                description = descM?.groupValues?.get(1)?.trim().orEmpty(),
                stress = stressM?.groupValues?.get(1)?.trim().orEmpty(),
                downloads = dlM?.groupValues?.get(1)?.toIntOrNull() ?: 0,
            )
        }.toList()
        return CmsSearchPage(page = page, items = items)
    }

    // —— detail 页解析 ——

    private val DETAIL_TITLE_RE = Regex("""<title>([^<]+)-CMS""")
    private val DETAIL_DOWNLOAD_RE = Regex(
        """<a href="[^"]*?/download/(\d+)/"[^>]*>[^<]*</a>""",
    )
    private val DETAIL_DESC_RE = Regex(
        """<div class="content_box oh ow">(.+?)</div>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val COVER_RE = Regex(
        """<div class="cover">\s*<img src="([^"]+)""""
    )
    private val STRESS_DETAIL_RE = Regex(
        """<b>应力</b>\s*<span class="text_theme_r">\s*([^<]+?)\s*</span>"""
    )
    private val SIZE_X_RE = Regex("""<b>x尺寸</b>\s*<span class="text_theme_r">\s*(\d+)\s*</span>""")
    private val SIZE_Y_RE = Regex("""<b>y尺寸</b>\s*<span class="text_theme_r">\s*(\d+)\s*</span>""")
    private val SIZE_Z_RE = Regex("""<b>z尺寸</b>\s*<span class="text_theme_r">\s*(\d+)\s*</span>""")
    private val DEPEND_BLOCK_RE = Regex(
        """<span class="gap_r">依赖</span>(.*?)<div class="f gap_cx""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val DEPEND_ITEM_RE = Regex("""alt="([^"]+)"[^>]*>\s*<div>([^<]+)</div>""")
    private val AUTHOR_DETAIL_RE = Regex("""<b class="op7 gap_t f">作者：([^<]*)</b>""")

    /** 一组 tip_box 节点的通用结构。 */
    private val TIP_BOX_RE = Regex(
        """<div class="tip_box">.*?""" +
        """<img src="(/upload/Tag/[^"]+\.png)"\s+alt="([^"]*)".*?""" +
        """<span>\s*([^<]+?)\s*</span>.*?""" +
        """<div class="fss op5">([^<]+)</div>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parseDetailHtml(html: String, detailId: Int): CmsDetail {
        val title = DETAIL_TITLE_RE.find(html)?.groupValues?.get(1)?.trim().orEmpty()
        val downloadId = DETAIL_DOWNLOAD_RE.find(html)?.groupValues?.get(1)?.toIntOrNull()
        val description = DETAIL_DESC_RE.find(html)?.groupValues?.get(1)
            ?.let { it.replace(Regex("""<[^>]+>"""), "").trim() }
            .orEmpty()
        val coverUrl = COVER_RE.find(html)?.groupValues?.get(1)
        val stress = STRESS_DETAIL_RE.find(html)?.groupValues?.get(1)
            ?.replace(Regex("""\s+"""), " ")?.trim()
            ?.takeIf { it.isNotEmpty() }
        val sx = SIZE_X_RE.find(html)?.groupValues?.get(1)?.toIntOrNull()
        val sy = SIZE_Y_RE.find(html)?.groupValues?.get(1)?.toIntOrNull()
        val sz = SIZE_Z_RE.find(html)?.groupValues?.get(1)?.toIntOrNull()
        val sizeXYZ = if (sx != null && sy != null && sz != null) Triple(sx, sy, sz) else null
        val author = AUTHOR_DETAIL_RE.find(html)?.groupValues?.get(1)?.trim().orEmpty()
        // 生产区:从 <span>生产</span> 之后,直到下一个 h4(注释) 之前
        val production = parseTipBoxesInSection(html, "<span>生产</span>", "<h4 class=\"gap_t\">注释")
        // 材料区:从 <h4 class="gap_t">材料统计 之后,直到 canvas_container 之前
        val materials = parseTipBoxesInSection(
            html,
            "材料统计",
            "canvas_container",
        )
        val dependencies = parseDependencies(html)
        return CmsDetail(
            detailId = detailId,
            title = title,
            downloadId = downloadId,
            description = description,
            coverUrl = coverUrl,
            materials = materials,
            production = production,
            dependencies = dependencies,
            stress = stress,
            sizeXYZ = sizeXYZ,
            tags = emptyList(),
            author = author,
        )
    }

    private fun parseTipBoxesInSection(
        html: String,
        startMarker: String,
        endMarker: String,
    ): List<UnifiedMaterial> {
        val start = html.indexOf(startMarker)
        if (start < 0) return emptyList()
        val endIdx = html.indexOf(endMarker, start)
        val localEnd = if (endIdx > start) endIdx else (start + 20_000).coerceAtMost(html.length)
        val local = html.substring(start, localEnd)
        return TIP_BOX_RE.findAll(local).map { m ->
            val iconUrl = m.groupValues[1]
            val displayName = m.groupValues[2]
            val countText = m.groupValues[3]
            val blockId = m.groupValues[4]
            UnifiedMaterial(
                blockId = blockId,
                displayName = displayName,
                iconUrl = iconUrl,
                count = parseCountText(countText),
                countText = countText,
            )
        }.toList()
    }

    private fun parseDependencies(html: String): List<String> {
        val m = DEPEND_BLOCK_RE.find(html) ?: return emptyList()
        val local = m.groupValues[1]
        return DEPEND_ITEM_RE.findAll(local)
            .map { it.groupValues[2].trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    // —— download 页解析 ——

    private val DOWNLOAD_HREF_RE = Regex(
        """<a href="(/upload/blueprint/[^"]+)"\s+download="([^"]+)""""
    )

    fun parseDownloadHtml(html: String, downloadId: Int): CmsDownloadInfo {
        val m = DOWNLOAD_HREF_RE.find(html)
        return CmsDownloadInfo(
            downloadId = downloadId,
            fileUrl = m?.groupValues?.get(1),
            filename = m?.groupValues?.get(2),
        )
    }
}
