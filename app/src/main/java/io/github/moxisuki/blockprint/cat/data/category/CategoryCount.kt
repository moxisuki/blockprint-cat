package io.github.moxisuki.blockprint.cat.data.category

/** Aggregate row for `SELECT categoryId, COUNT(*) FROM blueprints GROUP BY categoryId`. */
data class CategoryCount(
    val categoryId: String?,
    val cnt: Int,
)