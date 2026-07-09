# Blueprint Categories — Design Spec

| Field | Value |
|---|---|
| Date | 2026-07-10 |
| Target version | v1.3.0 (versionCode 5) |
| Database version | 9 → 10 (fallbackToDestructiveMigration) |
| Status | Draft → pending user review |
| Companion prototype | http://127.0.0.1:8765/ (local-only) |

## 1. Problem & Goals

### Problem

The current Home screen lists **all local blueprints in a single flat `LazyColumn`**. With v1.2.0's growing feature surface (SAF import, PC bridge, three blueprint-generation tools, BlockPaint), users accumulate dozens to hundreds of files in one undifferentiated pile. There is no way to organize them.

### Goals

1. Add a **new layer** between the Home screen and the blueprint list: a horizontal carousel of user-defined **categories**.
2. Allow users to **create, rename, delete, and restyle** categories; categories show a count of blueprints they contain.
3. Let users **assign blueprints to a category** (and remove them) via a multi-select flow.
4. Keep the existing PC tab, capsule tabs, filter chips, blueprint cards, and SAF/bridge mechanics **untouched**.
5. Keep the experience **comfortable**: no extra page jumps, no AppBar clutter, no friction when the user has not yet created any category.

### Non-Goals (YAGNI)

- **Many-to-many** blueprint↔category. One blueprint = at most one category. (User mental model.)
- **Nested / sub-categories**. Flat list only.
- **Custom uploaded cover images**. Covers are generated from a fixed palette + pattern catalog.
- **Drag-to-reorder categories**. `sortOrder` column is reserved for future use but the UI does not expose drag handles in v1.3.0.
- **Category-scoped PC transfer**. PC bridge sees the global blueprint list, not per-category.
- **Community blueprint categories**. Out of scope; only `BlueprintMetaEntity` (local files) participates.

## 2. UX Summary

The Home → Local tab becomes a **two-section layout**:

```
┌─────────────────────────────────────┐
│ AppBar: BlockPrint Cat      [🔄]    │ ← unchanged
├─────────────────────────────────────┤
│ ╭──Local──╮ ╭──PC──╮                 │ ← capsule tabs, unchanged
├─────────────────────────────────────┤
│ 我的分类                       [+]   │ ← NEW: category rail
│ [全部 35][城堡 12][红石 8]…         │     (LazyRow, 96×120 cards)
├─────────────────────────────────────┤
│ [筛选 chips]                         │ ← unchanged
├─────────────────────────────────────┤
│ [📄] blueprint card                  │ ← unchanged shape
│ [📄] blueprint card                  │
└─────────────────────────────────────┘
```

### Interactions

| Gesture / trigger | Result |
|---|---|
| Tap a category card | Filter the lower list to that category. AppBar title updates to `分类：<name>`. A `✕ 清除筛选` chip appears next to the title. |
| Tap `全部` card | Show all blueprints (including uncategorized). |
| Tap `+` next to section title | Open **新建分类** dialog (name + color + pattern in one screen). |
| Long-press a blueprint card | Enter multi-select mode; that card is selected; bottom action bar appears with `移动到分类` and `删除`. |
| Tap a card while in multi-select | Toggle its selection. |
| Back gesture / `✕` in AppBar while in multi-select | Exit multi-select, return to normal. |
| Long-press a category card | Open **编辑分类** dialog (rename + recolor + repatter + delete). |
| Multi-select → `移动到分类` | Open **移动到分类** dialog with a 4×2 grid including a dashed `移出分类` cell. |

### "All" and "Uncategorized" semantics

| Virtual ID | Meaning | Source |
|---|---|---|
| `ALL` (null) | All blueprints, regardless of `categoryId` | UI-only constant; not stored |
| `UNCATEGORIZED` | Blueprints whose `categoryId IS NULL` | UI-only constant; not stored |
| `<user id>` | Real category, queries `WHERE categoryId = <id>` | DB |

The `全部` card and `未分类` card are **virtual**: they do not occupy rows in `categories`. They are always shown, in fixed order at the start of the rail (全部 → 未分类 → user categories). `未分类` only appears if there is at least one uncategorized blueprint AND the user has created at least one real category (otherwise it is redundant with `全部`).

## 3. Data Model

### 3.1 Room entities

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,        // UUID
    val name: String,
    val colorIdx: Int,                 // 0..7 → ui/category/CategoryCover.palette
    val patternIdx: Int,               // 0..7 → ui/category/CategoryCover.patterns
    val sortOrder: Int = 0,            // reserved for future drag-reorder
    val createdAt: Long,
)

@Entity(
    tableName = "blueprints",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,    // delete category → blueprints go to "未分类"
    )],
    indices = [Index("categoryId")],
)
data class BlueprintMetaEntity(
    @PrimaryKey val uuid: String,
    val fileDocId: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val regionCount: Int,
    val blockCount: Int,
    val format: String,
    val lastScannedAt: Long,
    val categoryId: String? = null,     // NEW: null = uncategorized
)
```

### 3.2 DAO

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT categoryId, COUNT(*) AS cnt FROM blueprints GROUP BY categoryId")
    fun observeCountsByCategory(): Flow<List<CategoryCount>>    // projection

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Update suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories") suspend fun deleteAll()
}

data class CategoryCount(val categoryId: String?, val cnt: Int)
```

`BlueprintMetaDao` gains:

```kotlin
@Query("UPDATE blueprints SET categoryId = :targetId WHERE uuid IN (:uuids)")
suspend fun reassignCategory(uuids: List<String>, targetId: String?)
```

### 3.3 Migration

`fallbackToDestructiveMigration()` is already enabled in `DatabaseModule.kt`. Bump:

```kotlin
@Database(
    entities = [
        BlueprintMetaEntity::class,
        CategoryEntity::class,           // NEW
        // …existing entities…
    ],
    version = 10,                       // was 9
    exportSchema = false,
)
```

Users upgrading from v1.2.0 lose local blueprint metadata cache on first launch after upgrade — **acceptable** because the manager rescan rebuilds metadata from SAF on next refresh. Document this in CHANGELOG.

## 4. State Management

Continues the project's existing **Manager + VM-Adapter** pattern (`CLAUDE.md` and commit `1d35b5b`).

### 4.1 New `CategoryManager`

```kotlin
@Singleton
class CategoryManager @Inject constructor(
    private val categoryDao: CategoryDao,
    private val blueprintDao: BlueprintMetaDao,
) {
    /** Always includes virtual ALL + (UNCATEGORIZED if applicable) first. */
    val categories: StateFlow<List<CategoryRow>>
    val counts: StateFlow<Map<String?, Int>>

    suspend fun create(name: String, colorIdx: Int, patternIdx: Int): String
    suspend fun rename(id: String, newName: String)
    suspend fun changeCover(id: String, colorIdx: Int, patternIdx: Int)
    suspend fun delete(id: String)             // SET_NULL cascades automatically
    suspend fun moveBlueprintsToCategory(uuids: List<String>, targetId: String?)
}
```

`CategoryRow` is a UI-facing wrapper that pairs `CategoryEntity?` (null for virtual rows) with display fields.

### 4.2 `HomeViewModel` extension

Currently 22 lines (pure state forwarder). Grows to handle category-aware state and transient multi-select. Stays small because each piece is a one-liner.

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    blueprintManager: BlueprintManager,
    private val categoryManager: CategoryManager,
) : ViewModel() {

    val blueprints = blueprintManager.blueprints
    val blueprintCount = blueprintManager.blueprintCount
    val scanning = blueprintManager.scanning
    val categories = categoryManager.categories
    val counts = categoryManager.counts

    // Transient UI state — never persisted
    private val _selectedCategoryId = MutableStateFlow<String?>(null)  // null = ALL
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId

    private val _multi = MutableStateFlow<MultiSelectState>(MultiSelectState.Off)
    val multi: StateFlow<MultiSelectState> = _multi

    /** Derived: blueprints shown in the lower list, given current selection. */
    val displayedBlueprints: StateFlow<List<BlueprintMeta>> = combine(
        blueprintManager.blueprints,
        _selectedCategoryId,
    ) { list, id ->
        when (id) {
            null, ALL_ID            -> list
            UNCATEGORIZED_ID        -> list.filter { it.categoryId == null }
            else                    -> list.filter { it.categoryId == id }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectCategory(id: String?) { _selectedCategoryId.value = id; exitMulti() }
    fun clearFilter()              { _selectedCategoryId.value = null; exitMulti() }

    fun enterMultiSelect(uuid: String) { _multi.value = MultiSelectState.On(setOf(uuid)) }
    fun toggleSelected(uuid: String)   { _multi.update { it.toggle(uuid) } }
    fun selectAll()                   { _multi.update { it.selectAll(displayedBlueprints.value) } }
    fun exitMulti()                   { _multi.value = MultiSelectState.Off }

    suspend fun createCategory(name: String, color: Int, pattern: Int) =
        categoryManager.create(name, color, pattern)
    suspend fun renameCategory(id: String, name: String) = categoryManager.rename(id, name)
    suspend fun changeCover(id: String, color: Int, pattern: Int) =
        categoryManager.changeCover(id, color, pattern)
    suspend fun deleteCategory(id: String) = categoryManager.delete(id)
    suspend fun moveSelectedTo(targetId: String?) {
        val uuids = (_multi.value as? MultiSelectState.On)?.selected.orEmpty()
        if (uuids.isNotEmpty()) categoryManager.moveBlueprintsToCategory(uuids, targetId)
        exitMulti()
    }
}
```

`MultiSelectState`:

```kotlin
sealed interface MultiSelectState {
    data object Off : MultiSelectState
    data class On(val selected: Set<String>) : MultiSelectState
}
```

### 4.3 Why `HomeViewModel` and not a separate `CategoryViewModel`

- The category state is **only meaningful in the context of Home**. Detail page, settings, and PC tab never need it.
- `BlueprintViewModel` (in `ui/management/`) is a legacy screen we keep for compatibility; it does not own category logic.
- Keeping a single `HomeViewModel` avoids the cross-VM plumbing cost for a feature used only on one screen.

## 5. UI Structure

### 5.1 New files (all under `ui/category/`)

| File | Lines (est.) | Responsibility |
|---|---|---|
| `CategoryCover.kt` | ~90 | Palette (8 colors), Patterns (8 pixel patterns), `CategoryCover` composable that renders a card cover using `Canvas` |
| `CategoryCard.kt` | ~80 | Single 96×120 category card with `selected` state and `onClick`/`onLongClick` |
| `CategoryRail.kt` | ~110 | `LazyRow` rail, empty-state composable, the `+` add button |
| `CategoryHomeSection.kt` | ~50 | Thin wrapper that connects `HomeViewModel.categories` and `counts` to `CategoryRail` |
| `CategoryDialogs.kt` | ~180 | `NewCategoryDialog`, `EditCategoryDialog` — same body layout (name + color grid + pattern grid + meta footer for edit) |
| `CategoryMoveDialog.kt` | ~90 | 4×2 grid dialog with `移出分类` (dashed) cell |
| `CategoryMultiSelect.kt` | ~70 | AppBar variant + bottom action bar composables that read from `multi` |

### 5.2 Modified files

| File | Change |
|---|---|
| `ui/home/HomeScreen.kt` | Insert `<CategoryHomeSection>` between capsule tabs and filter chips; pass `onLongPress` down to list; pass `multiSelect` to swap AppBar variant and show bottom action bar |
| `ui/home/HomeViewModel.kt` | See §4.2 |
| `ui/home/HomeLocalList.kt` | Use `displayedBlueprints` instead of `blueprints`; raise `onLongPress(uuid)` to `HomeScreen` |
| `ui/home/components/HomeBlueprintCard.kt` | Accept optional `onLongClick: (() -> Unit)?`; accept optional `selected: Boolean` for visual highlight in multi-select mode |
| `data/AppDatabase.kt` | Bump version + add entity |
| `data/blueprint/BlueprintMetaEntity.kt` | Add `categoryId` column + FK + index |
| `data/blueprint/BlueprintMetaDao.kt` | Add `reassignCategory` |
| `data/blueprint/BlueprintManager.kt` | When scanning, leave `categoryId` as `null` (rescan does not touch category) |
| `di/DatabaseModule.kt` | Provide `CategoryDao` |
| `res/values/strings.xml`, `values-en/`, `values-zh-rCN/` | Add 19 new keys (see §6) |

### 5.3 Cover rendering

Covers are rendered with `Canvas` from two indices into fixed catalogs. No bitmap allocation at runtime. Example pattern `bricks` is a 4×4 matrix where `1` cells paint in the palette's `light` color on the `main` background. Eight patterns × eight colors = 64 unique covers — more than enough variety without bloating APK.

### 5.4 Compose stability

- `CategoryCover` takes `colorIdx: Int`, `patternIdx: Int`, `modifier` — all primitive/stable. Renders skippable.
- `CategoryCard` takes the entity (data class, stable) and stable lambdas. Renders skippable.
- `CategoryRail` keys `items(index, key = "cat-${it.id ?: it.kind}")`.
- `HomeViewModel` exposes only `StateFlow`, all reads via `collectAsStateWithLifecycle()` — no unstable reads.

## 6. i18n

All 19 keys go into the **three** `strings.xml` files in the same commit (CLAUDE.md hard requirement).

| Key | `values/` (zh) | `values-en/` | `values-zh-rCN/` |
|---|---|---|---|
| `home_category_title` | 我的分类 | My categories | 我的分类 |
| `home_category_all` | 全部 | All | 全部 |
| `home_category_uncategorized` | 未分类 | Uncategorized | 未分类 |
| `home_category_empty_title` | 还没有分类 | No categories yet | 还没有分类 |
| `home_category_empty_sub` | 点击右上角 + 新建分类，把蓝图整理得井井有条 | Tap + to create a category | 点击右上角 + 新建分类 |
| `cat_dialog_new_title` | 新建分类 | New category | 新建分类 |
| `cat_dialog_edit_title` | 编辑"%1$s" | Edit "%1$s" | 编辑"%1$s" |
| `cat_dialog_label_name` | 名称 | Name | 名称 |
| `cat_dialog_label_color` | 颜色 | Color | 颜色 |
| `cat_dialog_label_pattern` | 图案 | Pattern | 图案 |
| `cat_dialog_btn_create` | 创建 | Create | 创建 |
| `cat_dialog_btn_save` | 保存 | Save | 保存 |
| `cat_dialog_btn_delete_cat` | 删除分类 | Delete category | 删除分类 |
| `cat_dialog_delete_title` | 删除分类"%1$s"? | Delete "%1$s"? | 删除分类"%1$s"? |
| `cat_dialog_delete_body` | 分类下 %1$d 个蓝图会移至未分类，分类本身不可恢复。 | %1$d blueprints will be moved to Uncategorized. This cannot be undone. | 分类下 %1$d 个蓝图会移至未分类 |
| `cat_move_dialog_title` | 移动 %1$d 项到 | Move %1$d items to | 移动 %1$d 项到 |
| `cat_move_remove` | 移出分类 | Remove from category | 移出分类 |
| `cat_multi_count` | 已选 %1$d 项 | %1$d selected | 已选 %1$d 项 |
| `cd_category_add` | 新建分类 | Create category | 新建分类 |

## 7. Testing

| Test | Type | Coverage |
|---|---|---|
| `CategoryDaoTest` | Room in-memory (`:memory:`) | upsert / update / delete / observeAll ordering / count projection |
| `CategoryManagerTest` | JUnit + fake DAO | create returns id; rename / changeCover update fields; delete triggers SET_NULL on child blueprints; move reassigns uuid list |
| `HomeViewModelCategoryTest` | JUnit + Turbine + fake managers | selectedCategoryId filtering; multi-select state machine; combine flow emits correctly on category change |
| `CategoryCoverRendererTest` | JUnit | 64 cover combinations render without throwing; pattern indices map to distinct visual outputs |

Target: ≥ 80% coverage on new files (project standard from `common/testing.md`).

## 8. Risk & Rollout

| Risk | Mitigation |
|---|---|
| DB destructive migration loses blueprint cache | Manager rescan rebuilds metadata on next launch. Document in CHANGELOG. v1.3.0 upgrade message shown in About. |
| Long-press conflicts with future drag-reorder gestures on the same card | Only one gesture family lives on `HomeBlueprintCard`; reserve drag for v1.4. |
| `combine(blueprints, selectedCategoryId)` recomputes often | `stateIn(Eagerly)` + immutable list copy in VM; list is small (~hundreds max). |
| Multi-select state lost on configuration change (rotation) | `MultiSelectState` is held in `HomeViewModel`, which survives rotation. Do not move into `remember`. |
| Color/pattern selection drift if palette order changes | Palette/pattern tables are part of the UI module, not user-data. They are constants in code; if reordered in code, no migration is needed because user data only stores `colorIdx` / `patternIdx` (ints). |

## 9. Out of Scope (deferred)

- Drag-to-reorder categories (column reserved).
- Custom uploaded cover images.
- Per-category PC bridge filter.
- Category-aware BlockPaint import (auto-suggest).
- Many-to-many blueprint↔category.

---

## Appendix A — File change summary

```
+ app/src/main/java/io/github/moxisuki/blockprint/cat/data/category/CategoryEntity.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/data/category/CategoryDao.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/data/category/CategoryCount.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/data/category/CategoryManager.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryCover.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryCard.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryRail.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryHomeSection.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryDialogs.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryMoveDialog.kt
+ app/src/main/java/io/github/moxisuki/blockprint/cat/ui/category/CategoryMultiSelect.kt
+ app/src/test/java/.../CategoryDaoTest.kt
+ app/src/test/java/.../CategoryManagerTest.kt
+ app/src/test/java/.../HomeViewModelCategoryTest.kt
+ app/src/test/java/.../CategoryCoverRendererTest.kt
~ app/src/main/java/.../data/AppDatabase.kt
~ app/src/main/java/.../data/blueprint/BlueprintMetaEntity.kt
~ app/src/main/java/.../data/blueprint/BlueprintMetaDao.kt
~ app/src/main/java/.../data/blueprint/BlueprintManager.kt
~ app/src/main/java/.../di/DatabaseModule.kt
~ app/src/main/java/.../ui/home/HomeScreen.kt
~ app/src/main/java/.../ui/home/HomeViewModel.kt
~ app/src/main/java/.../ui/home/HomeLocalList.kt
~ app/src/main/java/.../ui/home/components/HomeBlueprintCard.kt
~ app/src/main/res/values/strings.xml
~ app/src/main/res/values-en/strings.xml
~ app/src/main/res/values-zh-rCN/strings.xml
~ CHANGELOG.md
~ README.md
```