package io.github.moxisuki.blockprint.cat.data.community

import org.junit.Assert.assertEquals
import org.junit.Test

class CmsParsersTest {

    @Test fun parseCountText_plainInteger() {
        assertEquals(7, CmsParsers.parseCountText("7"))
        assertEquals(0, CmsParsers.parseCountText(""))
        assertEquals(0, CmsParsers.parseCountText("not a number"))
    }

    @Test fun parseCountText_kSuffix() {
        assertEquals(4900, CmsParsers.parseCountText("4.9k"))
        assertEquals(44300, CmsParsers.parseCountText("44.3k"))
        assertEquals(1000, CmsParsers.parseCountText("1k"))
    }

    @Test fun parseCountText_wanSuffix() {
        assertEquals(12000, CmsParsers.parseCountText("1.2万"))
        assertEquals(50000, CmsParsers.parseCountText("5万"))
    }

    @Test fun parseCountText_stripsWhitespace() {
        assertEquals(42, CmsParsers.parseCountText("  42  \n"))
    }

    @Test fun mapCmsToMinecraftBlockId_modNamespace() {
        assertEquals("create:brass_funnel",
            CmsParsers.mapCmsToMinecraftBlockId("block.create.brass_funnel"))
    }

    @Test fun mapCmsToMinecraftBlockId_minecraftNamespace() {
        assertEquals("minecraft:bone_block",
            CmsParsers.mapCmsToMinecraftBlockId("block.minecraft.bone_block"))
    }

    @Test fun mapCmsToMinecraftBlockId_noPrefix() {
        assertEquals("minecraft:bone_meal",
            CmsParsers.mapCmsToMinecraftBlockId("bone_meal"))
    }

    // 读测试 resources 下的 HTML 文件。
    private fun readResource(path: String): String =
        javaClass.classLoader!!.getResourceAsStream(path)!!.use {
            it.readBytes().toString(Charsets.UTF_8)
        }

    @Test fun parseSearchHtml_realPage() {
        val html = readResource("cms/cms_search_page1.html")
        val page = CmsParsers.parseSearchHtml(html, page = 1)
        assertEquals(1, page.page)
        assertEquals(20, page.items.size)
        val first = page.items[0]
        assert(first.detailId > 0) { "detailId 必须为正整数" }
        assert(first.title.isNotBlank()) { "title 不应为空" }
    }

    @Test fun parseSearchHtml_emptyHtml() {
        val page = CmsParsers.parseSearchHtml("<html><body></body></html>", page = 1)
        assertEquals(0, page.items.size)
    }

    @Test fun parseDetailHtml_real1264_basics() {
        val html = readResource("cms/cms_detail_1264.html")
        val d = CmsParsers.parseDetailHtml(html, detailId = 1264)
        assertEquals(1264, d.detailId)
        assert(d.title.contains("骨粉机")) { "title 应含 '骨粉机',实际=${d.title}" }
        assert(d.downloadId != null) { "downloadId 应非空" }
    }

    @Test fun parseDetailHtml_real1264_materials() {
        val html = readResource("cms/cms_detail_1264.html")
        val d = CmsParsers.parseDetailHtml(html, detailId = 1264)
        assertEquals("材料统计应解析出 20 项", 20, d.materials.size)
        val stone = d.materials.first { it.blockId == "block.minecraft.stone_bricks" }
        assertEquals("石砖", stone.displayName)
        assertEquals(24, stone.count)
        assertEquals("24", stone.countText)
        assertEquals("/upload/Tag/block.minecraft.stone_bricks.png", stone.iconUrl)
    }

    @Test fun parseDetailHtml_real1264_production() {
        val html = readResource("cms/cms_detail_1264.html")
        val d = CmsParsers.parseDetailHtml(html, detailId = 1264)
        assertEquals("生产应解析出 2 项(骨块 + 骨粉)", 2, d.production.size)
        val boneBlock = d.production.first { it.blockId == "block.minecraft.bone_block" }
        assertEquals("骨块", boneBlock.displayName)
        assertEquals(4900, boneBlock.count)
        assertEquals("4.9k", boneBlock.countText)
    }

    @Test fun parseDetailHtml_real1264_cover() {
        val html = readResource("cms/cms_detail_1264.html")
        val d = CmsParsers.parseDetailHtml(html, detailId = 1264)
        assert(d.coverUrl != null) { "1264 有 cover" }
        assert(d.coverUrl!!.startsWith("/upload/post/")) { "cover 路径=${d.coverUrl}" }
        assert(d.coverUrl!!.endsWith(".webp")) { "cover 后缀=${d.coverUrl}" }
    }

    @Test fun parseDetailHtml_real1264_stressAndSize() {
        val html = readResource("cms/cms_detail_1264.html")
        val d = CmsParsers.parseDetailHtml(html, detailId = 1264)
        assertEquals(Triple(5, 5, 5), d.sizeXYZ)
        assert(d.stress != null) { "应力非空" }
    }

    @Test fun parseDetailHtml_real1264_dependencies() {
        val html = readResource("cms/cms_detail_1264.html")
        val d = CmsParsers.parseDetailHtml(html, detailId = 1264)
        assert(d.dependencies.any { it.contains("航空学") }) {
            "dependencies 应含航空学,实际=${d.dependencies}"
        }
    }

    @Test fun parseDownloadHtml_findsFileUrl() {
        val html = """
            <html><body>
            <a href="/upload/blueprint/abc-uuid.nbt" download="my_machine.nbt">下载</a>
            </body></html>
        """.trimIndent()
        val info = CmsParsers.parseDownloadHtml(html, downloadId = 42)
        assertEquals(42, info.downloadId)
        assertEquals("/upload/blueprint/abc-uuid.nbt", info.fileUrl)
        assertEquals("my_machine.nbt", info.filename)
    }

    @Test fun parseDownloadHtml_missingLink() {
        val info = CmsParsers.parseDownloadHtml("<html></html>", downloadId = 7)
        assertEquals(7, info.downloadId)
        assertEquals(null, info.fileUrl)
        assertEquals(null, info.filename)
    }
}
