package io.github.moxisuki.blockprint.cat.data.render

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 已生成 GLB 模型的蓝图记录。uuid 为主键，size/createdAt 供缓存管理页展示。
 */
@Entity(tableName = "glb_cache")
data class GlbCacheEntity(
    @PrimaryKey val uuid: String,
    val sizeBytes: Long = 0L,
    val createdAt: Long = 0L,
)
