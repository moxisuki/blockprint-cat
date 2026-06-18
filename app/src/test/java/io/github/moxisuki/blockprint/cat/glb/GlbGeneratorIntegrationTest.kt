package io.github.moxisuki.blockprint.cat.glb

import io.github.moxisuki.blockprint.core.*
import io.github.moxisuki.blockprint.core.glb.ImageBackend
import io.github.moxisuki.blockprint.core.glb.ImageData
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.zip.CRC32
import java.util.zip.Deflater

class GlbGeneratorIntegrationTest {

    /** Pure-Java ImageBackend for testing without Android/AWT dependencies. */
    private val testImageBackend = object : ImageBackend {
        override fun loadPng(path: Path): ImageData? = null
        override fun encodePng(data: ImageData): ByteArray = encodePngPure(data)
    }

    @Test
    fun `generate GLB for single stone block`() {
        val palette = BlockPalette(
            listOf(BlockState("minecraft:air"), BlockState("minecraft:stone"))
        )
        val region = LitematicRegion("test", 1, 1, 1, Position.ZERO, palette, intArrayOf(1))
        val lit = Litematic(null, null, "test_stone", "", "", listOf(region))

        val cacheDir = File(System.getProperty("java.io.tmpdir"), "glb_int_test_${System.nanoTime()}")
        cacheDir.deleteOnExit()

        // Create temp asset directory with basic blockstate/model files
        val assetsDir = File(cacheDir, "assets")
        assetsDir.mkdirs()
        File(assetsDir, "minecraft/blockstates").mkdirs()
        File(assetsDir, "minecraft/models/block").mkdirs()
        File(assetsDir, "minecraft/textures/block").mkdirs()
        File(assetsDir, "minecraft/blockstates/stone.json").writeText(
            """{"variants":{"":{"model":"minecraft:block/stone"}}}"""
        )
        File(assetsDir, "minecraft/models/block/stone.json").writeText("""{
            "parent":"block/cube_all",
            "textures":{"all":"minecraft:block/stone"}
        }""")
        File(assetsDir, "minecraft/models/block/cube_all.json").writeText("""{
            "elements":[{
                "from":[0,0,0],"to":[16,16,16],
                "faces":{
                    "down":{"texture":"#all","cullface":"down"},
                    "up":{"texture":"#all","cullface":"up"},
                    "north":{"texture":"#all","cullface":"north"},
                    "south":{"texture":"#all","cullface":"south"},
                    "west":{"texture":"#all","cullface":"west"},
                    "east":{"texture":"#all","cullface":"east"}
                }
            }]
        }""")

        // Create a 16x16 transparent PNG for the stone texture
        val texPixels = IntArray(16 * 16) { 0x00000000 }
        val texData = ImageData(16, 16, texPixels)
        val pngBytes = testImageBackend.encodePng(texData)
        File(assetsDir, "minecraft/textures/block/stone.png").writeBytes(pngBytes)

        val generator = GlbGenerator(listOf(assetsDir.toPath()), GlbCache(cacheDir), imageBackend = testImageBackend)
        val cacheKey = "test_stone_1x1x1"

        val bytes = generator.generate(lit, cacheKey)
        assertTrue("GLB should have content", bytes.isNotEmpty())

        // Verify GLB magic bytes (glTF)
        val magic = bytes.take(4).toByteArray()
        assertArrayEquals("glTF magic", byteArrayOf(0x67, 0x6C, 0x54, 0x46), magic)
    }

    companion object {
        private fun encodePngPure(data: ImageData): ByteArray {
            val w = data.width; val h = data.height; val argb = data.argb
            val rawRows = ByteArray(h * (1 + w * 4))
            for (y in 0 until h) {
                val rowStart = y * (1 + w * 4); rawRows[rowStart] = 0
                for (x in 0 until w) {
                    val pixel = argb[y * w + x]; val off = rowStart + 1 + x * 4
                    rawRows[off] = ((pixel shr 16) and 0xFF).toByte()
                    rawRows[off + 1] = ((pixel shr 8) and 0xFF).toByte()
                    rawRows[off + 2] = (pixel and 0xFF).toByte()
                    rawRows[off + 3] = ((pixel shr 24) and 0xFF).toByte()
                }
            }
            val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
            deflater.setInput(rawRows); deflater.finish()
            val compressed = ByteArrayOutputStream(); val buf = ByteArray(4096)
            while (!deflater.finished()) { val n = deflater.deflate(buf); if (n > 0) compressed.write(buf, 0, n) }
            deflater.end()
            val png = ByteArrayOutputStream()
            png.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            val ihdr = ByteBuffer.allocate(13).apply { putInt(w); putInt(h); put(8); put(6); put(0); put(0); put(0) }.array()
            writePngChunk(png, "IHDR", ihdr)
            writePngChunk(png, "IDAT", compressed.toByteArray())
            writePngChunk(png, "IEND", byteArrayOf())
            return png.toByteArray()
        }
        private fun writePngChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
            out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data.size).array())
            val tb = type.toByteArray(Charsets.US_ASCII); out.write(tb)
            out.write(data)
            val crc = CRC32(); crc.update(tb); crc.update(data)
            out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(crc.value.toInt()).array())
        }
    }
}
