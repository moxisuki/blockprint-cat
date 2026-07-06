package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterOutputStream

data class DecodedExportPayload(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val totalBlocks: Int,
    val materials: Map<String, Int>,
)

/**
 * 把 result bitmap + 元数据编码为 base64 字符串，用于 NavController 跨页面传参。
 * 二进制布局：
 *   [4-byte width][4-byte height][4-byte totalBlocks][4-byte materialsCount]
 *   每个 material: [4-byte keyLen][keyLen bytes][4-byte value]
 *   [4-byte keyLens 数组总长][4-byte * materialsCount 个 keyLen]
 *   [png bytes]
 * 整体再用 zlib 压缩 + base64。
 */
object ExportPayloadCodec {

    fun encode(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        totalBlocks: Int,
        materials: Map<String, Int>,
    ): String {
        val baos = ByteArrayOutputStream()
        val d = DataOutputStream(baos)
        d.writeInt(width)
        d.writeInt(height)
        d.writeInt(totalBlocks)
        d.writeInt(materials.size)

        val keyLengths = IntArray(materials.size)
        var i = 0
        materials.forEach { (k, v) ->
            val keyBytes = k.toByteArray(Charsets.UTF_8)
            d.writeInt(keyBytes.size)
            d.write(keyBytes)
            d.writeInt(v)
            keyLengths[i++] = keyBytes.size
        }
        keyLengths.forEach { d.writeInt(it) }

        val png = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, png)
        baos.write(png.toByteArray())
        d.close()

        val deflated = ByteArrayOutputStream()
        DeflaterInputStream(ByteArrayInputStream(baos.toByteArray())).use { it.copyTo(deflated) }
        return Base64.encodeToString(deflated.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(encoded: String): DecodedExportPayload {
        val compressed = Base64.decode(encoded, Base64.NO_WRAP)
        val inflated = ByteArrayOutputStream()
        InflaterOutputStream(inflated).use { it.write(compressed) }
        val full = inflated.toByteArray()

        val headerIn = DataInputStream(ByteArrayInputStream(full, 0, 16))
        val width = headerIn.readInt()
        val height = headerIn.readInt()
        val totalBlocks = headerIn.readInt()
        val materialsCount = headerIn.readInt()
        headerIn.close()

        val materials = linkedMapOf<String, Int>()
        var cursor = 16
        for (i in 0 until materialsCount) {
            val keyLen = readInt(full, cursor); cursor += 4
            val keyBytes = full.copyOfRange(cursor, cursor + keyLen); cursor += keyLen
            val value = readInt(full, cursor); cursor += 4
            materials[String(keyBytes, Charsets.UTF_8)] = value
        }
        cursor += 4 * materialsCount

        val pngBytes = full.copyOfRange(cursor, full.size)
        val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
            ?: throw IllegalStateException("Failed to decode bitmap from payload")

        return DecodedExportPayload(bitmap, width, height, totalBlocks, materials)
    }

    private fun readInt(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0xFF) shl 24) or
            ((buf[offset + 1].toInt() and 0xFF) shl 16) or
            ((buf[offset + 2].toInt() and 0xFF) shl 8) or
            (buf[offset + 3].toInt() and 0xFF)
    }
}