package io.github.moxisuki.blockprint.cat.data.community

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disclaimer_status")
data class DisclaimerStatusEntity(
    @PrimaryKey val id: Int = 1,
    val accepted: Boolean = false,
    val acceptedAt: Long = 0L,
)
