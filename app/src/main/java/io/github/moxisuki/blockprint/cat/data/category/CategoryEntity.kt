package io.github.moxisuki.blockprint.cat.data.category

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorIdx: Int,
    val patternIdx: Int,
    val sortOrder: Int = 0,
    val createdAt: Long,
)