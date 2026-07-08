package io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterOutputStream

/**
 * 解码后的导出载荷。
 *
 * 早期版本只有 bitmap / width / height / totalBlocks / materials 五个字段，
 * 后来为了支持 BP 预览页原样重建 result grid 而扩展。下游代码应当使用带默认值的
 * 拷贝（[copy]）来处理旧 payload，确保缺失字段拿到合理 fallback。
 *
 * 抽到 blueprintcommon 后 ITB 和 TTB 共用同一个编码：
 *   - 「sourceWidth/sourceHeight」在 ITB 填图原始尺寸，在 TTB 填文字画布尺寸。
 *   - 「selectedGroups/ditherMethod/...」重建 result grid 时回传给 engine。
 */
data class DecodedExportPayload(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val totalBlocks: Int,
    val materials: Map<String, Int>,
    val ditherMethod: Int = 0,
    val brightness: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
    val contrast: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
    val saturation: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
    val transparencyEnabled: Boolean = false,
    val transparencyTolerance: Int = BlueprintUiDefaults.DEFAULT_TOLERANCE,
    val selectedGroups: List<String> = emptyList(),
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    /** v3: 逐格方块 ID 列表（行优先，"" 表示空气），BlockPaint 专用直出。 */
    val blockIds: List<String> = emptyList(),
)

/**
 * 把 result bitmap + 元数据编码为 base64 字符串，用于 NavController 跨页面传参。
 *
 * v1（旧）二进制布局（无 magic 头）：
 *   [4-byte width][4-byte height][4-byte totalBlocks][4-byte materialsCount]
 *   每个 material: [4-byte keyLen][keyLen bytes][4-byte value]
 *   [4-byte keyLens 数组总长][4-byte * materialsCount 个 keyLen]
 *   [png bytes]
 *
 * v2（新）二进制布局：
 *   [4-byte magic 0x42504650 = "BPFP"]
 *   [4-byte version = 2]
 *   [4-byte width][4-byte height][4-byte totalBlocks][4-byte materialsCount]
 *   每个 material: [4-byte keyLen][keyLen bytes][4-byte value]
 *   [4-byte keyLens 数组总长][4-byte * materialsCount 个 keyLen]
 *   [4-byte ditherMethod][4-byte brightness][4-byte contrast][4-byte saturation]
 *   [1-byte transparencyEnabled flag]
 *   [4-byte transparencyTolerance]
 *   [4-byte selectedGroupsCount]
 *   每个 group: [4-byte len][len bytes UTF-8]
 *   [4-byte sourceWidth][4-byte sourceHeight]
 *   [png bytes]
 *
 * 整体再用 zlib 压缩 + base64。
 *
 * 解码时按 magic 字节区分版本，v1 走旧路径，v2 走新路径。magic 不匹配
 * 但首字段宽度合法时仍按 v1 兜底，保证热路径不挂。
 */
object ExportPayloadCodec {

    /** 4-byte ASCII "BPFP" — v2/v3 magic。 */
    private const val MAGIC_V2: Int = 0x42504650
    private const val VERSION_V3: Int = 3

    fun encode(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        totalBlocks: Int,
        materials: Map<String, Int>,
        ditherMethod: Int = DitherMethod.DEFAULT.id,
        brightness: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
        contrast: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
        saturation: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
        transparencyEnabled: Boolean = false,
        transparencyTolerance: Int = BlueprintUiDefaults.DEFAULT_TOLERANCE,
        selectedGroups: List<String> = emptyList(),
        sourceWidth: Int = 0,
        sourceHeight: Int = 0,
        blockIds: List<String> = emptyList(),
    ): String {
        val baos = ByteArrayOutputStream()
        val d = DataOutputStream(baos)
        d.writeInt(MAGIC_V2)
        d.writeInt(VERSION_V3)
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

        // v2 元数据
        d.writeInt(ditherMethod)
        d.writeInt(brightness)
        d.writeInt(contrast)
        d.writeInt(saturation)
        d.writeByte(if (transparencyEnabled) 1 else 0)
        d.writeInt(transparencyTolerance)
        d.writeInt(selectedGroups.size)
        selectedGroups.forEach { g ->
            val bytes = g.toByteArray(Charsets.UTF_8)
            d.writeInt(bytes.size)
            d.write(bytes)
        }
        d.writeInt(sourceWidth)
        d.writeInt(sourceHeight)

        // v3: blockIds (逐格方块 ID)
        d.writeInt(blockIds.size)
        blockIds.forEach { id ->
            val bytes = id.toByteArray(Charsets.UTF_8)
            d.writeShort(bytes.size)
            d.write(bytes)
        }

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

        var cursor = 0
        val magic = readInt(full, cursor); cursor += 4
        return if (magic == MAGIC_V2) {
            decodeV2(full, cursor)
        } else {
            // v1：magic 位置实际就是 width，回退用 cursor=0
            decodeV1(full, 0)
        }
    }

    private fun decodeV2(full: ByteArray, start: Int): DecodedExportPayload {
        var cursor = start
        val version = readInt(full, cursor); cursor += 4
        require(version == 2 || version == 3) { "Unsupported payload version: $version" }
        val isV3 = version == 3
        val width = readInt(full, cursor); cursor += 4
        val height = readInt(full, cursor); cursor += 4
        val totalBlocks = readInt(full, cursor); cursor += 4
        val materialsCount = readInt(full, cursor); cursor += 4

        val materials = linkedMapOf<String, Int>()
        for (i in 0 until materialsCount) {
            val keyLen = readInt(full, cursor); cursor += 4
            val keyBytes = full.copyOfRange(cursor, cursor + keyLen); cursor += keyLen
            val value = readInt(full, cursor); cursor += 4
            materials[String(keyBytes, Charsets.UTF_8)] = value
        }
        cursor += 4 * materialsCount // keyLens

        val ditherMethod = readInt(full, cursor); cursor += 4
        val brightness = readInt(full, cursor); cursor += 4
        val contrast = readInt(full, cursor); cursor += 4
        val saturation = readInt(full, cursor); cursor += 4
        val transparencyByte = full[cursor].toInt() and 0xFF; cursor += 1
        val transparencyEnabled = transparencyByte != 0
        val transparencyTolerance = readInt(full, cursor); cursor += 4
        val groupsCount = readInt(full, cursor); cursor += 4
        val groups = ArrayList<String>(groupsCount)
        for (i in 0 until groupsCount) {
            val len = readInt(full, cursor); cursor += 4
            val bytes = full.copyOfRange(cursor, cursor + len); cursor += len
            groups.add(String(bytes, Charsets.UTF_8))
        }
        val sourceWidth = readInt(full, cursor); cursor += 4
        val sourceHeight = readInt(full, cursor); cursor += 4

        // v3 blockIds
        val blockIds = if (isV3 && cursor + 4 <= full.size) {
            val count = readInt(full, cursor); cursor += 4
            val list = ArrayList<String>(count)
            for (i in 0 until count) {
                if (cursor + 2 > full.size) break
                val len = (full[cursor].toInt() and 0xFF shl 8) or (full[cursor + 1].toInt() and 0xFF); cursor += 2
                val bytes = full.copyOfRange(cursor, cursor + len); cursor += len
                list.add(String(bytes, Charsets.UTF_8))
            }
            list
        } else emptyList<String>()

        val pngBytes = full.copyOfRange(cursor, full.size)
        val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
            ?: throw IllegalStateException("Failed to decode bitmap from payload")

        return DecodedExportPayload(
            bitmap = bitmap,
            width = width,
            height = height,
            totalBlocks = totalBlocks,
            materials = materials,
            ditherMethod = ditherMethod,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            transparencyEnabled = transparencyEnabled,
            transparencyTolerance = transparencyTolerance,
            selectedGroups = groups,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            blockIds = blockIds,
        )
    }

    private fun decodeV1(full: ByteArray, start: Int): DecodedExportPayload {
        var cursor = start
        val width = readInt(full, cursor); cursor += 4
        val height = readInt(full, cursor); cursor += 4
        val totalBlocks = readInt(full, cursor); cursor += 4
        val materialsCount = readInt(full, cursor); cursor += 4

        val materials = linkedMapOf<String, Int>()
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

        return DecodedExportPayload(
            bitmap = bitmap,
            width = width,
            height = height,
            totalBlocks = totalBlocks,
            materials = materials,
        )
    }

    private fun readInt(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0xFF) shl 24) or
            ((buf[offset + 1].toInt() and 0xFF) shl 16) or
            ((buf[offset + 2].toInt() and 0xFF) shl 8) or
            (buf[offset + 3].toInt() and 0xFF)
    }
}
