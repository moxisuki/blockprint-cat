package io.github.moxisuki.blockprint.cat.data.category

import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaDao
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI-facing row for the category rail. Includes virtual ALL row.
 *
 * Note: `displayName` is intentionally NOT here. UI must apply localized text:
 * - `row is CategoryRow.All` -> stringResource(R.string.home_category_all)
 * - `row is CategoryRow.Real` -> row.entity.name
 * (Data layer cannot access Context for stringResource.)
 */
sealed interface CategoryRow {
    val id: String? // null for All
    val count: Int
    val colorIdx: Int
    val patternIdx: Int

    data class All(override val count: Int) : CategoryRow {
        override val id: String? = null
        override val colorIdx: Int = 0
        override val patternIdx: Int = 0

        override fun equals(other: Any?): Boolean = other is All
        override fun hashCode(): Int = "All".hashCode()
        override fun toString(): String = "All(count=$count)"
    }

    data class Real(
        val entity: CategoryEntity,
        override val count: Int = 0,
    ) : CategoryRow {
        override val id: String? get() = entity.id
        override val colorIdx: Int get() = entity.colorIdx
        override val patternIdx: Int get() = entity.patternIdx
    }
}

/**
 * Singleton owner of category state. Exposes hot [StateFlow]s for the UI and
 * thin CRUD wrappers over [CategoryDao] and [BlueprintMetaDao].
 *
 * Follows the same pattern as [io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager].
 */
@Singleton
class CategoryManager @Inject internal constructor(
    private val categoryDao: CategoryDao,
    private val blueprintDao: BlueprintMetaDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Always starts with [CategoryRow.All] (synthetic), followed by real
     * categories ordered as [CategoryDao.observeAll] returns them, each
     * annotated with its blueprint count.
     */
    val categories: StateFlow<List<CategoryRow>> = combine(
        categoryDao.observeAll(),
        categoryDao.observeCountsByCategory(),
    ) { rows, counts ->
        val countsMap = counts.associate { it.categoryId to it.cnt }
        val totalAll = countsMap.values.sum()
        val real = rows.map { entity ->
            CategoryRow.Real(entity, countsMap[entity.id] ?: 0)
        }
        listOf<CategoryRow>(CategoryRow.All(totalAll)) + real
    }.stateIn(scope, SharingStarted.Eagerly, listOf(CategoryRow.All(0)))

    /** Map from categoryId (or null = uncategorized) to blueprint count. */
    val counts: StateFlow<Map<String?, Int>> = categoryDao.observeCountsByCategory()
        .map { list -> list.associate { row -> row.categoryId to row.cnt } }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Create a new category and return its generated UUID. */
    suspend fun create(name: String, colorIdx: Int, patternIdx: Int): String {
        val id = UUID.randomUUID().toString()
        categoryDao.upsert(
            CategoryEntity(
                id = id,
                name = name,
                colorIdx = colorIdx,
                patternIdx = patternIdx,
                sortOrder = 0,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    /** Rename a category. No-op if the id is unknown. */
    suspend fun rename(id: String, newName: String) {
        val current = currentEntity(id) ?: return
        categoryDao.update(current.copy(name = newName))
    }

    /** Update a category's cover style. No-op if the id is unknown. */
    suspend fun changeCover(id: String, colorIdx: Int, patternIdx: Int) {
        val current = currentEntity(id) ?: return
        categoryDao.update(current.copy(colorIdx = colorIdx, patternIdx = patternIdx))
    }

    /** Delete a category by id. FK on blueprints sets their categoryId to null. */
    suspend fun delete(id: String) {
        categoryDao.deleteById(id)
    }

    /**
     * Move a batch of blueprints to [targetId] (or clear their category with
     * `null`). No-op for an empty uuid list.
     */
    suspend fun moveBlueprintsToCategory(uuids: List<String>, targetId: String?) {
        if (uuids.isEmpty()) return
        blueprintDao.reassignCategory(uuids, targetId)
    }

    /** Fetch the first emission of observeAll() and find the row with [id]. */
    private suspend fun currentEntity(id: String): CategoryEntity? =
        categoryDao.observeAll().first().firstOrNull { it.id == id }
}
