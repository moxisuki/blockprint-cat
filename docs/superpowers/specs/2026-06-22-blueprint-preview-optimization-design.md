# Blueprint Preview Optimization — Design

**Date:** 2026-06-22
**Status:** DRAFT — pending user approval
**Scope:** Single PR, single commit

## Context

Blueprint preview flow (`PreviewScreen` + `BlueprintDetailScreen` + `GlbGenerator` + `RenderResourceManager`) carries four classes of debt:

1. **Memory leak**: complete GLB byte array held in Compose state for the entire preview lifetime, even after Filament has uploaded to GPU.
2. **Manual memory-mapping**: code manually `RandomAccessFile` + `FileChannel.map` to feed SceneView, when SceneView 4.18.0's `ModelLoader.createModelInstance(File)` already does this internally.
3. **Loading UX race**: internal loading overlay is gated by `!fromCache`, so cache hits show no progress feedback and the SceneView appears empty/black for a few frames after the top-level progress bar disappears.
4. **API duplication**: `GlbGenerator.generate()` and `GlbGenerator.getOrGenerateFile()` do the same work; `RenderResourceManager.cachedGlb` / `takeGlb` / `cachedKeys` are vestigial; `GlbCache.get/put` (ByteArray variant) is dead code.

This spec unifies the flow, removes dead code, fixes the UX race, and avoids redundant NBT parsing on disk-cache hits.

## Goals

| # | Goal | Measurement |
|---|---|---|
| G1 | Java heap never holds a complete GLB byte array during preview | No `byteArrayOf` in compose state after refactor |
| G2 | SceneView consumes File directly, no manual `FileChannel.map` | grep `FileChannel.map` in PreviewScreen returns 0 |
| G3 | Loading overlay shown for all cache states, no black-screen flicker after it disappears | Manual T2/T3/T4 pass |
| G4 | Restart-and-open-preview on a disk-cached blueprint does not trigger top-level progress | Manual T3 pass |
| G5 | No duplicate methods, no zero-caller fields/methods | grep results below |
| G6 | `GlbCacheEntity.sizeBytes` accurate for new writes | Manual T6 + Room inspection |

### G5 grep targets (post-refactor must return 0)

- `\.generate\(` outside `LitematicToGlb.convert` call sites
- `\.takeGlb\b`
- `cachedGlb\b` (the field)
- `GlbCache\.get\(` / `GlbCache\.put\(`
- `GlbCacheEntry\b` (the old data class)
- `cachedGlbKey\b` / `cachedGlbMinY\b` / `cachedGlbCenterX\b` / `cachedGlbCenterZ\b` / `cachedGlbFile\b` (the 5 separate fields)

## Non-Goals

- ❌ Room schema migration (no version bump)
- ❌ New unit tests (no test infrastructure in repo)
- ❌ Visual redesign of loading overlays
- ❌ Changes to `SettingsScreen` / `CacheManagementDialog` (auto-benefits from sizeBytes fix)
- ❌ Changes to SceneView interaction code (camera, lights, walk joystick UI, tool icons styling)
- ❌ Cross-module changes to `litematic-lib` / `LitematicToGlb.convert`
- ❌ SceneView engine pre-warm on app start (separate concern)

## Architecture

Three-layer responsibility split, with UI explicitly orchestrating the three-segment cache fallback:

```
┌─────────────────────────────────────────────────────────┐
│  PreviewScreen / BlueprintDetailScreen  (UI / caller)  │
│  Orchestrates: in-memory → disk → generate             │
│  Owns: SceneView, loading overlay, progress state       │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  GlbResourceManager  (in-memory cache coordinator)      │
│  CachedGlb data class, cachedKeys StateFlow (UUID set), │
│  peek / hasGlb / putGlb / clearGlb,                    │
│  transferLitematic / receiveLitematic                   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  GlbGenerator  (streaming GLB writer / disk query)      │
│  Key data class, peekCacheFile, getOrGenerateFile       │
│  (with onProgress), hasCache, clearCache                │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  GlbCache  (disk directory)                            │
│  getFile(Key), clear(Key), clear(), size()             │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  GlbCacheEntity / GlbCacheDao  (Room persistence)      │
│  Schema unchanged; sizeBytes written from cacheFile    │
└─────────────────────────────────────────────────────────┘
```

`PreviewScreen.kt` further splits into 5 files (Section "File Layout" below).

## Component Changes

### `GlbGenerator.kt`

- Add `Key` data class: `Key(blueprintUuid: String, regionIndex: Int = 0, floorHeight: Int = LAYER_FLOOR_HEIGHT)`
- Add `peekCacheFile(key: Key): File?` — returns `null` if file missing or empty
- Change `getOrGenerateFile(litematic, key: Key, onProgress: ((Float) -> Unit)? = null): File`
- Add companion `const val MIN_VALID_GLB_BYTES = 200L`
- **Delete** `generate(...)` (zero callers; equivalent to `getOrGenerateFile` minus bytes return)

### `GlbCache.kt`

- Change `getFile(key: GlbGenerator.Key): File`
- Change `clear(key: GlbGenerator.Key)`
- **Delete** `get(key, regionIndex, floorHeight): ByteArray?` (zero callers; internally `file.readBytes()` is the anti-pattern we're removing)
- **Delete** `put(key, regionIndex, bytes, floorHeight)` (zero callers)

### `GlbResourceManager.kt` (renamed from `RenderResourceManager.kt`)

**Two-tier state design** (rationale: Room doesn't store `minY/centerX/centerZ`, so the in-memory Map can't be populated from Room alone; splitting UI subscription from session hot-path keeps `init()` fast and avoids per-entry I/O on startup).

```kotlin
data class CachedGlb(
    val blueprintUuid: String,
    val cacheFile: File,
    val minY: Float,
    val centerX: Float,
    val centerZ: Float,
)

// Tier 1: UUID set mirrored from Room — used by DetailScreen "View Cached" button.
private val _cachedKeys = MutableStateFlow<Set<String>>(emptySet())
val cachedKeys: StateFlow<Set<String>> = _cachedKeys.asStateFlow()

// Tier 2: per-session metadata — used by PreviewScreen Segment 1 (in-memory hot path).
private val sessionCache = mutableMapOf<String, CachedGlb>()
```

**Public API:**
- `peek(uuid): CachedGlb? = sessionCache[uuid]` — PreviewScreen Segment 1
- `hasGlb(uuid): Boolean = uuid in _cachedKeys.value` — DetailScreen check
- `putGlb(uuid, cacheFile, minY, centerX, centerZ)` — writes to both tiers + Room
- `clearGlb(uuid)` / `clearAllGlb()` — clears both tiers + Room
- `transferLitematic(uuid, lit)` — renamed from `putLitematic`
- `receiveLitematic(uuid): Litematic?` — renamed from `takeLitematic`

**`init()` populates Tier 1 from Room, leaves Tier 2 empty:**
```kotlin
fun init(context: Context, dao: GlbCacheDao) {
    ...
    runBlocking {
        _cachedKeys.value = dao.getAll().map { it.uuid }.toSet()
    }
    // sessionCache intentionally empty — populated by putGlb within current session.
    // PreviewScreen Segment 1 only fires within same session; cold-start always
    // falls through to Segment 2 (stat + small litematic region header parse).
}
```

**Deleted:**
- `takeGlb(uuid): GlbCacheEntry?` (zero callers)
- `cachedGlb: ByteArray?` field (only self-referenced by `peekGlb`/`takeGlb`/`clearGlb`)
- 5 separate fields: `cachedGlbKey` / `cachedGlbMinY` / `cachedGlbCenterX` / `cachedGlbCenterZ` / `cachedGlbFile` (folded into `CachedGlb`)
- Old `GlbCacheEntry` data class

**Room write change:** `GlbCacheEntity.sizeBytes = cacheFile.length()` instead of `bytes.size` (was always 0).

### `PreviewScreen.kt`

**`GlbEntry` data class simplification:**
```kotlin
private data class GlbEntry(
    val minY: Float,
    val centerX: Float,
    val centerZ: Float,
    val cacheFile: File,
    val fromCache: Boolean = false,
)
```

**`LaunchedEffect(uuid)` three-segment fallback:**
```kotlin
LaunchedEffect(uuid) {
    // Segment 1: in-memory hit
    GlbResourceManager.peek(uuid)?.let { hit ->
        if (hit.cacheFile.isFile && hit.cacheFile.length() > GlbGenerator.MIN_VALID_GLB_BYTES) {
            glbEntry = GlbEntry(hit.minY, hit.centerX, hit.centerZ, hit.cacheFile, fromCache = true)
            return@LaunchedEffect
        }
    }
    // Segment 2: disk hit (no NBT re-parse)
    val onDisk = try { generator?.peekCacheFile(GlbGenerator.Key(blueprintUuid = uuid)) } catch (_: Exception) { null }
    if (onDisk != null) {
        val reg = try { blueprintManager.loadDetail(uuid)?.raw?.regions?.getOrNull(0) } catch (_: Exception) { null }
        glbEntry = GlbEntry(
            minY = reg?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f,
            centerX = reg?.position?.x?.toFloat() ?: 0f,
            centerZ = reg?.position?.z?.toFloat() ?: 0f,
            cacheFile = onDisk,
            fromCache = true,
        )
        return@LaunchedEffect
    }
    // Segment 3: miss → generate
    glbProgress = 0f
    glbStageText = context.getString(R.string.preview_stage_region)
    try {
        val cacheFile = withContext(Dispatchers.IO) {
            val lit = GlbResourceManager.receiveLitematic(uuid)
                ?: blueprintManager.loadDetail(uuid)?.raw
                ?: throw IllegalStateException("蓝图不存在或已被删除")
            if (lit.blockCount() == 0) throw IllegalStateException("该蓝图不包含任何方块")
            generator?.getOrGenerateFile(lit, GlbGenerator.Key(blueprintUuid = uuid)) { f ->
                glbProgress = f
                glbStageText = stageFor(f)
            } ?: throw IllegalStateException("渲染引擎未初始化")
        }
        val reg = blueprintManager.loadDetail(uuid)?.raw?.regions?.getOrNull(0)
        glbEntry = GlbEntry(
            minY = reg?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f,
            centerX = reg?.position?.x?.toFloat() ?: 0f,
            centerZ = reg?.position?.z?.toFloat() ?: 0f,
            cacheFile = cacheFile,
        )
    } catch (e: Exception) {
        Log.e(TAG, "预览加载失败", e)
        error = "${e.javaClass.simpleName}: ${e.message ?: context.getString(R.string.preview_error_unknown)}"
    }
}
```

**`PreviewSceneContent` loading state changes:**
```kotlin
var modelInst by remember { mutableStateOf<ModelInstance?>(null) }
var modelOnScreen by remember { mutableStateOf(false) }     // NEW — replaces centered for overlay control
var loadingVisible by remember { mutableStateOf(true) }

LaunchedEffect(modelOnScreen, glbEntry) {
    loadingVisible = !modelOnScreen
    if (modelOnScreen) kotlinx.coroutines.delay(250)        // 250ms buffer post-first-frame
}

LaunchedEffect(glbEntry?.cacheFile) {
    modelOnScreen = false                                   // reset on new entry
    modelError = false
    val file = glbEntry?.cacheFile ?: return@LaunchedEffect
    try {
        modelInst = modelLoader.createModelInstance(file)   // SceneView handles mmap internally
    } catch (e: Exception) {
        Log.e(TAG, "模型加载失败: ${e.message}", e)
        modelErrorMessage = if (e.message?.contains("Empty vertex") == true)
            context.getString(R.string.preview_resource_missing)
        else
            context.getString(R.string.preview_render_failed_with_msg, e.message ?: "")
        modelError = true
    }
}

SceneView(
    onFrame = { frameTimeNanos ->
        if (modelInst != null && !modelOnScreen) modelOnScreen = true   // NEW
        val delta = if (lastFrameNanos > 0) ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceIn(0f, 0.1f) else 0f
        lastFrameNanos = frameTimeNanos
        if (cam.isWalk) cam.applyWalkMove(delta)
        cam.applyToCamera(cameraNode)
    },
    // ... other params unchanged
) { /* SceneView children unchanged */ }

// Overlay gate — !fromCache REMOVED
if (loadingVisible && !modelError) {
    Box(...) { /* spinner + "加载中" */ }
}
```

**`key(glbBytes)` → `key(glbEntry)`:**
```kotlin
key(glbEntry) {                  // was key(glbBytes)
    SceneView(...) { ... }
}
```

**`centered` flag kept** but only used for layer plane rendering (line ~454 in original), not for loading state.

### `BlueprintDetailScreen.kt`

**Generation path consolidation (around line 378-399):**
```kotlin
LaunchedEffect(Unit) {
    val t0 = System.currentTimeMillis()
    try {
        val region = bp.raw.regions.getOrNull(0)
        val modelMinY = region?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f
        val modelCX = region?.position?.x?.toFloat() ?: 0f
        val modelCZ = region?.position?.z?.toFloat() ?: 0f
        val key = GlbGenerator.Key(blueprintUuid = bp.meta.uuid)
        val cacheFile = withContext(Dispatchers.IO) {
            generator?.getOrGenerateFile(bp.raw, key) { p ->
                genProgress = p
                genElapsed = System.currentTimeMillis() - t0
                genStage = stageName(p)
            }
        } ?: throw IllegalStateException("渲染引擎未初始化")
        GlbResourceManager.putGlb(bp.meta.uuid, cacheFile, modelMinY, modelCX, modelCZ)
        showDialog = false
        navController.navigate(NavRoutes.previewRoute(bp.meta.uuid))
    } catch (_: Exception) {
        showDialog = false
    }
}
```

**`cachedKeys` subscription update (line ~247):**
```kotlin
// Before
val cachedKeys by RenderResourceManager.cachedKeys.collectAsState()
val hasCache = bp.meta.uuid in cachedKeys

// After
val cachedKeys by GlbResourceManager.cachedKeys.collectAsState()
val hasCache = bp.meta.uuid in cachedKeys
```

**Other DetailScreen code unchanged:** import statements adjusted (renamed types), dialog UI unchanged, `startGenerate` / `viewExisting` lambdas unchanged in shape (only type names swap).

### File Layout (new file split)

| File | Contents |
|---|---|
| `PreviewScreen.kt` (modified) | Entry `PreviewScreen` + `PreviewSceneContent` skeleton + `GlbEntry` + `PreviewEntryPoint` (moved out — see below) |
| `PreviewCamera.kt` (new) | `CameraController` class + all camera math (orbit/drag/zoom/walk) + related constants |
| `PreviewLighting.kt` (new) | `LightPreset` + `LIGHT_PRESETS` + `applyPreviewLightPreset` + `rememberNoDiscSunLight` + `updateMCNoDiscSunLight` |
| `PreviewJoystick.kt` (new) | `WalkJoystick` composable + `JOYSTICK_RADIUS_DP` / `THUMB_RADIUS_DP` |
| `PreviewToolbar.kt` (new) | `ToolIcon` + `LayerIconBtn` composables + toolbar + layer panel UI |
| `PreviewEntryPoint.kt` (new) | `PreviewEntryPoint` Hilt interface + `resolve()` companion — moved out of `PreviewScreen.kt` for consistency with `SettingsEntryPoint` / `CacheManagementEntryPoint` |

### Unchanged files

- `GlbCacheEntity.kt` — schema unchanged
- `GlbCacheDao.kt` — unchanged
- `SettingsScreen.kt` / `CacheManagementDialog.kt` — read DAO only, auto-benefits
- `LitematicToGlb.convert` — in `litematic-lib`, out of scope
- All SceneView interaction code (orbit math, lighting math, grid lines, axis arrows) — only relocated, not rewritten

## Data Flow

### 3.1 Three-segment fallback (PreviewScreen)

```
user clicks "View" / navigates to preview/<uuid>
   │
   ▼
PreviewScreen(uuid) composes, glbEntry = null
   │
   ▼ LaunchedEffect(uuid)
   │
   ├─ ① GlbResourceManager.peek(uuid) ─→ in-memory hit?
   │     ├─ yes ─→ glbEntry = GlbEntry(fromCache=true)         [0ms I/O]
   │     └─ no
   │
   ├─ ② generator.peekCacheFile(Key(uuid)) ─→ disk hit?
   │     ├─ yes ─→ loadDetail just for region metadata (no block parse)
   │     │         glbEntry = GlbEntry(fromCache=true)
   │     └─ no
   │
   └─ ③ cache miss → generation path
         ├─ glbProgress = 0f, stageText = "区域划分..."
         ├─ receiveLitematic ?: loadDetail → litematic
         ├─ getOrGenerateFile(lit, Key(uuid), onProgress = { ... }) → cacheFile
         └─ glbEntry = GlbEntry(cacheFile = cacheFile)
   │
   ▼
glbEntry != null ─→ PreviewSceneContent composes
```

### 3.2 SceneView loading sequence (PreviewSceneContent)

```
PreviewSceneContent mounts (glbEntry ready, cacheFile known)
   │
   ├─ modelInst = null
   ├─ modelOnScreen = false
   ├─ loadingVisible = true
   │
   ├─ LaunchedEffect(cacheFile):
   │     modelOnScreen = false
   │     modelError = false
   │     modelInst = modelLoader.createModelInstance(file)
   │     ↑ SceneView internally mmap + Filament GPU upload, ~100-300ms
   │
   ├─ LaunchedEffect(modelOnScreen):
   │     loadingVisible = !modelOnScreen
   │     if (modelOnScreen) delay(250ms)
   │
   └─ SceneView starts rendering:
         frame N:   model uploading to GPU
         frame N+1: model drawn
         onFrame fires → if (modelInst != null && !modelOnScreen) modelOnScreen = true
                          │
                          ▼
                    delay(250ms) ends
                          │
                          ▼
                    loadingVisible = false
                          │
                          ▼
                    Overlay hidden, model fully visible
```

**Critical invariant**: Overlay visibility is no longer gated by `fromCache`. The overlay shows from PreviewSceneContent mount until first frame with model + 250ms buffer, regardless of how `glbEntry` was populated.

### 3.3 DetailScreen generation

```
DetailScreen, user clicks "Generate" or "Regenerate"
   │
   ├─ startGenerate:
   │     GlbResourceManager.clearGlb(uuid)         ← clear in-memory StateFlow
   │     generator.clearCache(Key(uuid))           ← clear disk
   │     showDialog = true
   │
   └─ LaunchedEffect(Unit):
         t0 = now
         compute region minY/centerX/centerZ
         cacheFile = withContext(Dispatchers.IO) {
             generator.getOrGenerateFile(bp.raw, Key(uuid)) { p → genProgress, genStage }
               ↑ single call:
                 - disk hit → return existing file
                 - disk miss → LitematicToGlb.convert stream → .tmp → rename
         }
         GlbResourceManager.putGlb(uuid, cacheFile, minY, centerX, centerZ)
           ↑ StateFlow update + Room upsert(sizeBytes = cacheFile.length())
         showDialog = false
         navController.navigate(previewRoute(uuid))
```

## Error Handling

### Failure matrix

| Scenario | Detection | User-visible behavior |
|---|---|---|
| Blueprint file deleted | `loadDetail` returns null | DetailScreen: dialog closes, no nav. PreviewScreen: error card "蓝图不存在或已被删除" |
| Blueprint empty blocks | `lit.blockCount() == 0` | Same as above with "不包含任何方块" |
| Renderer not initialized | `generator == null` | Same as above with "渲染引擎未初始化" |
| SceneView load failure | `createModelInstance` throws | Snackbar with error message; overlay shows `preview_render_failed_with_msg` |
| Cache file too small / corrupt | `length() < MIN_VALID_GLB_BYTES` | Auto-fall-through to generation (Segment 3) |
| Disk full / IO error during gen | `getOrGenerateFile` throws | DetailScreen dialog closes silently (existing behavior) |
| Room write failure | `dao.upsert` throws inside `glbScope.launch` | Fire-and-forget; UI not blocked (existing behavior) |
| Memory cache / disk mismatch | `peek` returns entry but file deleted externally | Segment 2 `peekCacheFile` returns null; fall through to Segment 3 |

### Strategy

- **Segments 1 & 2** (read-only): wrap in `try { ... } catch (_: Exception) { null }`; any IO failure silently degrades to next segment.
- **Segment 3** (generation): catch in `try { ... } catch (e: Exception) { error = ... }`; render error card.
- **SceneView load**: catch in `LaunchedEffect(cacheFile)`, set `modelError = true`, show snackbar.
- **DetailScreen dialog**: existing `catch (_: Exception) { showDialog = false }` unchanged (silent close).

### Constants

- `GlbGenerator.MIN_VALID_GLB_BYTES = 200L` — far below any real GLB but catches zero-byte / corrupt files.

## Migration & Legacy Data

### Room `sizeBytes` for historical rows

Existing rows have `sizeBytes = 0` (pre-refactor bug). Post-refactor writes are correct. **Strategy: passive fix** — re-generating a blueprint causes its Room row to be re-written with the correct size. Settings page may show incorrect total until legacy entries are regenerated.

This is documented in PR description and accepted by user as out-of-scope.

### API compatibility

All changes are internal. No public API surface changes (no module boundaries crossed). `litematic-lib` unchanged.

## Testing Strategy

No automated tests in repo (`ExampleUnitTest.kt` / `ExampleInstrumentedTest.kt` are placeholders). Manual verification:

| # | Path | Steps | Expected |
|---|---|---|---|
| T1 | New blueprint, DetailScreen generate | Click "Generate" | Dialog shows 0→100%, generation completes, navigate to preview |
| T2 | Just-generated, PreviewScreen | Auto-nav from T1 | **Overlay visible immediately on enter**, model appears after |
| T3 | Restart App, PreviewScreen | Kill app, reopen, enter preview | Brief overlay, no top-level progress |
| T4 | DetailScreen "View Cached" | Click cached-entry button | Same as T2 |
| T5 | DetailScreen "Regenerate" | Click regenerate icon | Confirm dialog if >70k blocks, else direct regen dialog |
| T6 | Settings → Cache Management | Check total size | New entries sum correctly; legacy entries still 0 (known) |
| T7 | Blueprint deleted then Preview | Delete litematic externally, open cached preview | Error card "蓝图不存在或已被删除" |
| T8 | Uninstall + reinstall, same blueprint | Redownload, generate | Same as T1 |
| T9 | Large blueprint (>70k blocks) | DetailScreen confirm flow | Confirm dialog appears, then generation |
| T10 | Light preset switch | Preview light button | 4 presets cycle smoothly |
| T11 | Walk mode + joystick | Mode button + joystick | Camera follows, no lag |

## Rollback Strategy

Single PR, single commit → `git revert` reverses everything in one step.

| Failure mode | Detection | Rollback |
|---|---|---|
| Build error | `gradle build` | `git revert HEAD` |
| App crash on launch | Manual launch | `git revert HEAD` |
| Generation path broken | T1/T5 | `git revert HEAD` |
| Preview load broken | T2/T3/T4 | `git revert HEAD` |
| Single component broken (e.g., camera) | T11 | `git revert HEAD -- PreviewCamera.kt` |
| Camera math wrong | Orbit/drag/zoom misbehave | Revert single file |

## Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| `PreviewEntryPoint` move breaks Hilt injection | Low | High | Compile-time catch |
| `Key` default `floorHeight` differs from current behavior | Very low | Medium | grep confirms all callers pass `LAYER_FLOOR_HEIGHT`; default matches |
| `takeLitematic`/`putLitematic` rename has hidden caller | Very low | Medium | grep confirmed 0 hidden callers |
| `cachedGlb` field deletion breaks reflection | Very low | Medium | No reflection usage; pure internal state |
| `GlbCache.get/put` deletion has hidden caller | Very low | Medium | grep confirmed 0 callers |
| SceneView `File` overload behaves differently across devices | Low | Low | Officially supported in SceneView 4.18.0+; min SDK 24 |
| Legacy `sizeBytes=0` confuses users in Settings | Medium | Low | Documented, passive fix accepted |

## PR Description (draft)

```
Title: refactor(preview): consolidate GLB loading, drop dead code, fix loading UX

GlbGenerator:
- New Key data class groups (uuid, regionIndex, floorHeight)
- New peekCacheFile(Key): File? — avoids NBT re-parse on disk hit
- getOrGenerateFile(lit, Key, onProgress?): File — onProgress now propagated
- generate() removed (zero callers)
- Companion MIN_VALID_GLB_BYTES = 200L added

GlbCache:
- getFile / clear now take GlbGenerator.Key
- get / put (ByteArray) removed (zero callers)

GlbResourceManager (renamed from RenderResourceManager):
- New CachedGlb data class replaces 5 separate fields
- cachedKeys StateFlow retained (Room-mirrored UUID set); private sessionCache map added for per-session metadata
- takeGlb / cachedGlb field removed (zero callers)
- takeLitematic/putLitematic renamed to receiveLitematic/transferLitematic
- Room sizeBytes now writes cacheFile.length() instead of bytes.size (was 0)

PreviewScreen:
- GlbEntry data class no longer carries bytes
- Three-segment fallback explicitly orchestrated (in-memory → disk → generate)
- SceneView.createModelInstance(File) used directly, no manual FileChannel.map
- onFrame-based modelOnScreen replaces centered for overlay timing
- Overlay no longer gated by !fromCache — shown until first model frame + 250ms
- key(glbEntry) replaces key(glbBytes)
- Split: PreviewCamera.kt / PreviewLighting.kt / PreviewJoystick.kt / PreviewToolbar.kt / PreviewEntryPoint.kt

BlueprintDetailScreen:
- Generation path now single getOrGenerateFile call (was generate + getOrGenerateFile)
- cachedKeys subscription unchanged in shape (still `bp.meta.uuid in cachedKeys`)

Bugfixes:
- Loading overlay now shows for cache hits too (no more "nothing then black")
- "Just generated, open preview" no longer races with first-frame composition
- "Restart, open preview" no longer shows spurious top-level progress on disk hit

Legacy:
- Existing Room rows have sizeBytes=0; new writes are correct. Settings page
  total for old entries remains 0 until re-generated (passive fix).
```

## Open Items

None — all clarifying questions resolved:
- Single PR / single commit ✓
- Add `peekCacheFile` to avoid NBT re-parse ✓
- Passive fix for legacy `sizeBytes` ✓
- Option 1 file split (5 new files) ✓
- Rename `RenderResourceManager` → `GlbResourceManager` ✓
- Rename `take/putLitematic` → `receive/transferLitematic` ✓
- Move `PreviewEntryPoint` to its own file ✓
