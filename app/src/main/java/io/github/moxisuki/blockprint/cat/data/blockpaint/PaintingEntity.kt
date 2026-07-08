package io.github.moxisuki.blockprint.cat.data.blockpaint

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一张方块绘画的 Room 持久化记录。
 * grid 用逗号分隔的字符串序列化（"" 表示空气格），避免 TypeConverter + JSON 依赖。
 * 32×32 = 1024 单元格 × ~10 字符 ≈ 10KB 一张图，可以接受。
 */
@Entity(tableName = "block_paintings")
data class PaintingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val gridCsv: String,
    val createdAt: Long,
    val updatedAt: Long,
)
