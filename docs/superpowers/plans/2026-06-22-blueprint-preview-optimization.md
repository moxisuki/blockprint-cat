# Blueprint Preview Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate GLB byte memory leak, fix loading overlay UX race, remove dead code, and split oversized `PreviewScreen.kt` — all in one PR with multiple commits.

**Architecture:** Bottom-up additive API changes first (compile-safe), then caller migrations, then deletions of now-zero-caller code, then file split. Two-tier in-memory state (`cachedKeys: Set<String>` for UI, `sessionCache: Map<String, CachedGlb>` for hot path) avoids schema migration while keeping startup fast.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Filament + SceneView 4.18.0, Hilt, kotlinx-coroutines.

**Spec:** `docs/superpowers/specs/2026-06-22-blueprint-preview-optimization-design.md`

**Test Infrastructure:** None. Project has only placeholder `ExampleUnitTest.kt`. All verification is via `gradle assembleDebug` + manual T1–T11.

---

## File Structure

**New files:**
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewCamera.kt` — `CameraController` class + math
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewLighting.kt` — `LightPreset`, presets, sun light
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewJoystick.kt` — `WalkJoystick` composable
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewToolbar.kt` — toolbar + layer panel UI
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewEntryPoint.kt` — Hilt entry point

**Renamed file:**
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/render/RenderResourceManager.kt` → `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/render/GlbResourceManager.kt`

**Modified files:**
- `app/src/main/java/io/github/moxisuki/blockprint/cat/glb/GlbGenerator.kt`
- `app/src/main/java/io/github/moxisuki/blockprint/cat/glb/GlbCache.kt`
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt`
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/detail/BlueprintDetailScreen.kt`

**Unchanged (read-only consumers):**
- `app/src/main/java/io/github/moxisuki/blockprint/cat/data/render/GlbCacheEntity.kt`
- `app/src/main/java/io/github/moxisuki/blockprint/cat/data/render/GlbCacheDao.kt`
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/settings/SettingsScreen.kt`
- `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/settings/CacheManagementDialog.kt`

---

## Task 1: GlbGenerator — add `Key` data class, `peekCacheFile`, `MIN_VALID_GLB_BYTES`, `onProgress` to `getOrGenerateFile`

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/glb/GlbGenerator.kt`

This task is purely additive. The existing `generate()` method is kept for now (Task 5 deletes it). The new `peekCacheFile` and updated `getOrGenerateFile` signatures don't break any existing caller because:
- `peekCacheFile` is new (no callers yet — PreviewScreen will adopt it in Task 4)
- `getOrGenerateFile` keeps all old params; `onProgress` is added with default `null`

- [ ] **Step 1: Edit `GlbGenerator.kt` to add new declarations**

Replace the entire file content with:

```kotlin
package io.github.moxisuki.blockprint.cat.glb

import io.github.moxisuki.blockprint.core.Litematic
import io.github.moxisuki.blockprint.core.glb.GlbExportOptions
import io.github.moxisuki.blockprint.core.glb.ImageBackend
import io.github.moxisuki.blockprint.core.glb.LitematicToGlb
import java.io.File
import java.nio.file.Path

class GlbGenerator(
    private val assetsDirs: List<Path>,
    private val cache: GlbCache,
    private val imageBackend: ImageBackend? = null,
) {

    /**
     * 一份 GLB 缓存的唯一标识。regionIndex 与 floorHeight 是历史参数,
     * 当前永远 (0, LAYER_FLOOR_HEIGHT),保留为数据类字段是为了日后扩展不破坏 API。
     */
    data class Key(
        val blueprintUuid: String,
        val regionIndex: Int = 0,
        val floorHeight: Int = LAYER_FLOOR_HEIGHT,
    )

    companion object {
        const val LAYER_FLOOR_HEIGHT = 1
        /** Files smaller than this are treated as missing/corrupt. */
        const val MIN_VALID_GLB_BYTES = 200L
        private const val TAG = "GlbGenerator"
        private fun log(msg: String) = println("[$TAG] $msg")
    }

    /**
     * 用流式 [LitematicToGlb.convert] 生成 GLB 并写入缓存文件。
     * 不再缓冲完整字节数组，直接流式写入磁盘。
     */
    fun generate(
        litematic: Litematic,
        cacheKey: String,
        regionIndex: Int = 0,
        floorHeight: Int = 0,
        onProgress: ((Float) -> Unit)? = null,
    ): ByteArray {
        val cacheFile = cache.getFile(Key(cacheKey, regionIndex, floorHeight))
        if (cacheFile.isFile) {
            log("缓存命中: $cacheKey r$regionIndex fh$floorHeight, ${cacheFile.length()} bytes")
            return byteArrayOf()
        }
        log("缓存未命中, 开始生成: $cacheKey r$regionIndex fh$floorHeight")
        val t0 = System.currentTimeMillis()
        val options = if (floorHeight > 0) GlbExportOptions(floorHeight = floorHeight)
                      else GlbExportOptions()
        val tmp = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
        tmp.parentFile?.mkdirs()
        tmp.outputStream().use { out ->
            LitematicToGlb.convert(litematic, assetsDirs, out, regionIndex, options, onProgress)
        }
        tmp.renameTo(cacheFile)
        val elapsed = System.currentTimeMillis() - t0
        log("GLB 生成完成: ${cacheFile.length()} bytes, 耗时 ${elapsed}ms")
        return byteArrayOf()
    }

    /** Check if a cached GLB file exists and looks valid (non-empty, ≥ MIN_VALID_GLB_BYTES). */
    fun peekCacheFile(key: Key): File? {
        val file = cache.getFile(key)
        return file.takeIf { it.isFile && it.length() >= MIN_VALID_GLB_BYTES }
    }

    /**
     * Return the cached GLB file, generating it on disk if absent.
     * Writes the GLB to a `.tmp` file first, then atomic-rename.
     */
    fun getOrGenerateFile(
        litematic: Litematic,
        key: Key,
        onProgress: ((Float) -> Unit)? = null,
    ): File {
        val file = cache.getFile(key)
        if (file.isFile) {
            log("缓存文件命中: ${file.absolutePath}, ${file.length()} bytes")
            return file
        }
        log("缓存文件未命中, 生成中: ${key.blueprintUuid} r${key.regionIndex} fh${key.floorHeight}")
        val t0 = System.currentTimeMillis()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.parentFile?.mkdirs()
        val opts = if (key.floorHeight > 0) GlbExportOptions(floorHeight = key.floorHeight) else GlbExportOptions()
        tmp.outputStream().use { out ->
            LitematicToGlb.convert(litematic, assetsDirs, out, key.regionIndex, opts, onProgress)
        }
        tmp.renameTo(file)
        val elapsed = System.currentTimeMillis() - t0
        log("GLB 生成完成: ${file.length()} bytes, 耗时 ${elapsed}ms")
        return file
    }

    fun hasCache(key: Key): Boolean = cache.getFile(key).isFile

    fun clearCache(key: Key) = cache.clear(key)

    fun clearAllCache() = cache.clear()
}
```

Note: `cache.getFile(key)` is called with the new `Key` signature — Task 2 will update `GlbCache.getFile` to accept `Key`. Until Task 2 lands, the build will fail. **Stop here and run Task 2 first if you're committing immediately**, otherwise bundle them in the same commit by deferring the commit until after Task 2.

- [ ] **Step 2: Verify the file compiles in isolation (skipped — full build requires Task 2)**

Move to Task 2 immediately.

---

## Task 2: GlbCache — change `getFile`/`clear` to take `GlbGenerator.Key`, delete `get`/`put` ByteArray variants

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/glb/GlbCache.kt`

The `get`/`put` ByteArray variants have zero callers (verified by grep). `getFile`/`clear` signature changes to accept `Key`. `cacheFile()` private helper also updated.

- [ ] **Step 1: Replace entire `GlbCache.kt` content**

```kotlin
package io.github.moxisuki.blockprint.cat.glb

import java.io.File

class GlbCache(private val cacheDir: File) {

    companion object {
        private const val MAX_SIZE_BYTES = 200L * 1024 * 1024  // 200 MB
    }

    init {
        cacheDir.mkdirs()
    }

    fun getFile(key: GlbGenerator.Key): File = cacheFile(key)

    fun clear(key: GlbGenerator.Key) {
        val prefix = key.blueprintUuid.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        cacheDir.listFiles()?.filter { it.name.startsWith("${prefix}_r") }?.forEach { it.delete() }
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun size(): Long = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun cacheFile(key: GlbGenerator.Key): File {
        val safe = key.blueprintUuid.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val suffix = if (key.floorHeight > 0) "_fh${key.floorHeight}" else ""
        return File(cacheDir, "${safe}_r${key.regionIndex}${suffix}.glb")
    }
}
```

- [ ] **Step 2: Build to confirm no callers broke**

Run:
```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS. (The only external caller of `GlbCache` is `GlbGenerator` itself, which was updated in Task 1. There are no other call sites — verified by `grep "GlbCache" app/src` returning only `GlbCache.kt`, `GlbGenerator.kt`, and `RenderResourceManager.kt:init`. The `RenderResourceManager` use is `_generator = GlbGenerator(listOf(renderAssetsDir.toPath()), GlbCache(cacheDir))` which is constructor-only, no method calls.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/io/github/moxisuki/blockprint/cat/glb/
git commit -m "feat(glb): introduce Key data class, peekCacheFile, onProgress

- GlbGenerator.Key groups (uuid, regionIndex, floorHeight)
- peekCacheFile(Key): File? — non-mutating disk check
- getOrGenerateFile accepts Key + onProgress; onProgress now propagated
- MIN_VALID_GLB_BYTES = 200L added
- GlbCache.getFile/clear take GlbGenerator.Key
- GlbCache.get/put (ByteArray) removed — zero callers

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: BlueprintDetailScreen — drop the `generate()` call, use `getOrGenerateFile` with `onProgress`

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/detail/BlueprintDetailScreen.kt:385-393`

This collapses the two-call pattern (`generate()` + `getOrGenerateFile()`) into a single `getOrGenerateFile()` call, with progress now flowing through the new `onProgress` parameter.

- [ ] **Step 1: Replace the `LaunchedEffect` body in `BlueprintDetailScreen.kt`**

Find the block at lines 378-399 (the `LaunchedEffect(Unit)` inside `showDialog = true`). Replace the contents of that `LaunchedEffect` with:

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
                RenderResourceManager.putGlb(bp.meta.uuid, cacheFile, modelMinY, modelCX, modelCZ)
                showDialog = false
                navController.navigate(NavRoutes.previewRoute(bp.meta.uuid))
            } catch (_: Exception) {
                showDialog = false
            }
        }
```

Note: `RenderResourceManager.putGlb` still takes `bytes` here — that signature change happens in Task 7. The current `putGlb(key, bytes, minY, centerX, centerZ, cacheFile)` accepts `bytes` as a no-op position — we'll still pass an empty `ByteArray(0)` until Task 7 changes the signature. To avoid the temporary noise, **keep this Task paired with Task 7's signature change in the same commit**. Add a temporary local:

```kotlin
                RenderResourceManager.putGlb(bp.meta.uuid, ByteArray(0), modelMinY, modelCX, modelCZ, cacheFile = cacheFile)
```

in this task, then in Task 7 you'll fix the call to drop `ByteArray(0)` and `cacheFile =`.

- [ ] **Step 2: Build**

Run:
```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/io/github/moxisuki/blockprint/cat/ui/detail/BlueprintDetailScreen.kt
git commit -m "refactor(detail): drop generate() double-call, use getOrGenerateFile onProgress

Generation path was previously calling GlbGenerator.generate() (for its
onProgress callback) then GlbGenerator.getOrGenerateFile() (for the File
return) — two calls doing the same work. Consolidated into one.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: PreviewScreen — three-segment fallback rewrite

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt:170-211`

This rewrites the `LaunchedEffect(uuid)` body to:
- Segment 1: `GlbResourceManager.peek(uuid)` → in-memory hit (unchanged API but new name)
- Segment 2: NEW `generator.peekCacheFile(Key(uuid))` → disk hit, no NBT re-parse
- Segment 3: cache miss → load litematic + generate

The `GlbEntry` data class is also simplified (no `bytes`).

- [ ] **Step 1: Replace the `LaunchedEffect(uuid)` body in `PreviewScreen.kt`**

Find the block at lines 170-211. Replace it with:

```kotlin
    LaunchedEffect(uuid) {
        // Segment 1: in-memory hit
        val cached = io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.peek(uuid)
        if (cached != null && cached.cacheFile.isFile && cached.cacheFile.length() > GlbGenerator.MIN_VALID_GLB_BYTES) {
            glbEntry = GlbEntry(cached.minY, cached.centerX, cached.centerZ, cached.cacheFile, fromCache = true)
            return@LaunchedEffect
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
                val lit = io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.takeLitematic(uuid)
                    ?: blueprintManager.loadDetail(uuid)?.raw
                    ?: throw IllegalStateException("蓝图不存在或已被删除")
                if (lit.blockCount() == 0) throw IllegalStateException("该蓝图不包含任何方块")
                generator?.getOrGenerateFile(lit, GlbGenerator.Key(blueprintUuid = uuid)) { f ->
                    glbProgress = f
                    glbStageText = stageFor(f)
                } ?: throw IllegalStateException("渲染引擎未初始化")
            }
            val reg = blueprintManager.loadDetail(uuid)?.raw?.regions.getOrNull(0)
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

Note: This task still uses `RenderResourceManager.peek()` (old API). Task 6 renames the class; Task 7 refactors the data shape. The fully-qualified name `io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager` keeps references working through both renames — we'll only collapse to short names in Task 8.

- [ ] **Step 2: Replace `GlbEntry` data class (line 244)**

Find:
```kotlin
private data class GlbEntry(val bytes: ByteArray, val minY: Float, val centerX: Float, val centerZ: Float, val cacheFile: java.io.File? = null, val fromCache: Boolean = false)
```

Replace with:
```kotlin
private data class GlbEntry(val minY: Float, val centerX: Float, val centerZ: Float, val cacheFile: java.io.File, val fromCache: Boolean = false)
```

- [ ] **Step 3: Build**

```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS. The old `cached.cacheFile` access (now non-nullable `File` via the new CachedGlb in Task 7) — wait, the current `peek()` returns `GlbCacheEntry` with `cacheFile: File?` (nullable). After Task 7, it returns `CachedGlb` with `cacheFile: File` (non-null). This task references `cached.cacheFile.isFile` which works for both nullable and non-null (smart-cast on local val), so the build should still succeed.

If the build fails because Task 7 hasn't run yet, defer this commit until Task 7 is done.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt
git commit -m "refactor(preview): three-segment cache fallback

- Segment 1 (in-memory): GlbResourceManager.peek → 0 I/O
- Segment 2 (disk): peekCacheFile + minimal metadata parse — no full NBT
- Segment 3 (miss): litematic load + getOrGenerateFile with onProgress

Previously, disk-cache hit triggered blueprintManager.loadDetail()
just to obtain region metadata, costing NBT parse time and surfacing
the 'fake progress bar on restart' bug.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Delete `GlbGenerator.generate()` (now zero callers)

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/glb/GlbGenerator.kt`

- [ ] **Step 1: Verify zero callers**

```bash
cd app/src/main && grep -rn "\.generate(" . | grep -v "LitematicToGlb.convert" | grep -v "modelLoader.createModelInstance"
```

Expected output: empty (or only the `getOrGenerateFile` definition itself). The remaining `.generate(` references are all internal — `LitematicToGlb.convert(...)` inside `generate()`, and `modelLoader.createModelInstance` which is unrelated.

- [ ] **Step 2: Remove the `generate` method**

In `GlbGenerator.kt`, delete the `generate` method (the entire block from `/**` doc comment through `return byteArrayOf()`). The file should now only contain `Key`, `peekCacheFile`, `getOrGenerateFile`, `hasCache`, `clearCache`, `clearAllCache`, and the companion.

- [ ] **Step 3: Build**

```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/io/github/moxisuki/blockprint/cat/glb/GlbGenerator.kt
git commit -m "refactor(glb): remove GlbGenerator.generate() — zero callers

generate() returned an empty ByteArray (the GLB had been streamed to
disk) and was only used for its onProgress callback. That role is now
filled by getOrGenerateFile(lit, key, onProgress).

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: Rename `RenderResourceManager` → `GlbResourceManager` (file + class)

**Files:**
- Rename: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/render/RenderResourceManager.kt` → `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/render/GlbResourceManager.kt`

Use git to track the rename (preserves history).

- [ ] **Step 1: Stage the rename**

```bash
cd app/src/main/java/io/github/moxisuki/blockprint/cat/ui/render
git mv RenderResourceManager.kt GlbResourceManager.kt
```

- [ ] **Step 2: Edit the file to rename the class**

In `GlbResourceManager.kt`, change `object RenderResourceManager` → `object GlbResourceManager`. Update any internal `TAG` if it referenced the old name (it currently doesn't — `TAG = "RenderResourceMgr"` is a string used only in `init` log; leave as-is or update to `"GlbResourceMgr"`).

Update the `TAG` constant:
```kotlin
    private const val TAG = "GlbResourceMgr"
```

- [ ] **Step 3: Update all references**

Find and replace across the codebase:

```bash
cd app/src/main && grep -rln "RenderResourceManager" .
```

For each file (expected: `PreviewScreen.kt`, `BlueprintDetailScreen.kt`, plus the renamed file itself), replace `RenderResourceManager` with `GlbResourceManager`. Both the import line and call sites.

In `PreviewScreen.kt`: there are ~3 call sites (`peek`, `takeLitematic`).
In `BlueprintDetailScreen.kt`: there are ~3 call sites (`cachedKeys`, `clearGlb`, `putGlb`).

- [ ] **Step 4: Build**

```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/java/io/github/moxisuki/blockprint/cat/
git commit -m "refactor(render): rename RenderResourceManager → GlbResourceManager

The class only manages GLB cache state (peek/putGlb/clearGlb/
takeLitematic + Room mirror). The old name 'RenderResourceManager'
suggests it manages render assets, but those live in the Render screen
and aren't touched by this class.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: GlbResourceManager — two-tier state refactor + drop vestigial fields + rename litematic methods

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/render/GlbResourceManager.kt`

This is the largest single change. Two-tier state (cachedKeys + sessionCache), new CachedGlb data class, deletes takeGlb + cachedGlb field + 5 separate fields + old GlbCacheEntry, fixes `sizeBytes` write to use `cacheFile.length()`, renames `takeLitematic`/`putLitematic` → `receiveLitematic`/`transferLitematic`, updates `putGlb` signature.

- [ ] **Step 1: Replace the file content**

Replace the entire `GlbResourceManager.kt` (keep package + imports updated) with:

```kotlin
package io.github.moxisuki.blockprint.cat.ui.render

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

import io.github.moxisuki.blockprint.core.Litematic
import io.github.moxisuki.blockprint.cat.glb.FileSystemFileAccessor
import io.github.moxisuki.blockprint.cat.glb.GlbCache
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator

object GlbResourceManager {

    /**
     * Per-session metadata for a generated GLB. Held in memory only;
     * for UI subscription across cold start, see [cachedKeys].
     */
    data class CachedGlb(
        val blueprintUuid: String,
        val cacheFile: File,
        val minY: Float,
        val centerX: Float,
        val centerZ: Float,
    )

    data class ResourceState(
        val vanillaInstalled: Boolean = false,
        val vanillaVersion: String = "",
        val vanillaDate: String = "",
        val resolverReady: Boolean = false,
        val resourcesDir: File? = null,
        val i18nVersion: Int = 0,
        val resolverVersion: Int = 0,
        val builtinReady: Boolean = false,
    )

    private val _state = MutableStateFlow(ResourceState())
    val state: StateFlow<ResourceState> = _state.asStateFlow()

    private var fileAccessor: AndroidFileAccessor? = null
    val accessor: AndroidFileAccessor? get() = fileAccessor

    private var _generator: GlbGenerator? = null
    val generator: GlbGenerator?
        get() = _generator

    val activeMods: Set<String> get() = emptySet()
    val modVersions: Map<String, String> get() = emptyMap()

    fun assetsForRead(): Any? = null

    fun modVersion(namespace: String): String? = null

    fun getDisplayName(blockName: String, langCode: String = "zh_cn"): String =
        blockName.removePrefix("minecraft:")

    private var applicationContext: Context? = null

    /** Litematic handed from DetailScreen to PreviewScreen so we don't re-parse NBT. */
    private var pendingLitematic: Litematic? = null
    private var pendingLitematicKey: String = ""

    fun transferLitematic(key: String, lit: Litematic) {
        pendingLitematic = lit
        pendingLitematicKey = key
    }

    fun receiveLitematic(key: String): Litematic? {
        if (pendingLitematicKey == key) {
            val lit = pendingLitematic
            pendingLitematic = null
            pendingLitematicKey = ""
            return lit
        }
        return null
    }

    /**
     * Tier 1: UUID set mirrored from Room. Used by DetailScreen's
     * "View Cached" button to know which blueprints have a cached GLB.
     */
    private val _cachedKeys = MutableStateFlow<Set<String>>(emptySet())
    val cachedKeys: StateFlow<Set<String>> = _cachedKeys.asStateFlow()

    /**
     * Tier 2: per-session metadata (current process only). Used by
     * PreviewScreen Segment 1 to skip disk I/O when we already loaded
     * this blueprint in the same session.
     */
    private val sessionCache = mutableMapOf<String, CachedGlb>()

    fun peek(uuid: String): CachedGlb? = sessionCache[uuid]

    fun hasGlb(uuid: String): Boolean = uuid in _cachedKeys.value

    fun putGlb(uuid: String, cacheFile: File, minY: Float, centerX: Float, centerZ: Float) {
        sessionCache[uuid] = CachedGlb(uuid, cacheFile, minY, centerX, centerZ)
        _cachedKeys.update { it + uuid }
        val dao = glbCacheDao ?: return
        glbScope.launch {
            dao.upsert(
                io.github.moxisuki.blockprint.cat.data.render.GlbCacheEntity(
                    uuid = uuid,
                    sizeBytes = cacheFile.length(),
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun clearGlb(uuid: String) {
        sessionCache.remove(uuid)
        _cachedKeys.update { it - uuid }
        val dao = glbCacheDao ?: return
        glbScope.launch { dao.delete(uuid) }
    }

    fun clearAllGlb() {
        sessionCache.clear()
        _cachedKeys.value = emptySet()
        val dao = glbCacheDao ?: return
        glbScope.launch { dao.clearAll() }
    }

    private var glbCacheDao: io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao? = null
    private val glbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context, dao: io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao) {
        Log.d(TAG, "初始化 GLB 资源管线...")
        applicationContext = context.applicationContext
        glbCacheDao = dao

        runBlocking {
            val all = dao.getAll()
            _cachedKeys.value = all.map { it.uuid }.toSet()
            Log.d(TAG, "已恢复 GLB 缓存键: ${all.size} 个")
        }

        val renderAssetsDir = File(context.filesDir, "blockprintcat/render_assets")
        val accessor = FileSystemFileAccessor(renderAssetsDir)
        Log.d(TAG, "使用渲染资源目录: $renderAssetsDir (已下载=${renderAssetsDir.isDirectory})")
        fileAccessor = AndroidFileAccessor(context, baseDir = renderAssetsDir)

        val cacheDir = File(context.filesDir, "glb_cache")
        _generator = GlbGenerator(listOf(renderAssetsDir.toPath()), GlbCache(cacheDir))

        val testPath = "minecraft/textures/block/stone.png"
        val testBytes = accessor.readBytes(testPath)
        Log.d(TAG, "资源测试: $testPath -> ${if (testBytes != null) "${testBytes.size} bytes" else "未找到"}")

        _state.value = ResourceState(
            resourcesDir = renderAssetsDir,
            builtinReady = true,
        )
        Log.d(TAG, "GLB 资源管线初始化完成, generator=${_generator != null}, accessor=${accessor.javaClass.simpleName}")
    }

    private const val TAG = "GlbResourceMgr"

    suspend fun installVanilla(versionId: String = "latest"): String = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException("TODO: render pipeline not yet rebuilt")
    }

    suspend fun uninstallVanilla(): Boolean = false

    fun installModAssets(namespace: String, version: String = "") {}

    suspend fun resetVanilla(versionId: String) {
        _state.value = _state.value.copy(vanillaInstalled = false, resolverReady = false)
    }

    fun listInstalled(): List<Map<String, String>> = emptyMap()

    fun iconFor(blockName: String, targetPx: Int): Bitmap? = null

    fun isInstalled(): Boolean = false
    fun available(): Boolean = false
    fun resourcesDir(): File? = _state.value.resourcesDir
}
```

- [ ] **Step 2: Update callers**

In `PreviewScreen.kt`, update call sites:
- `RenderResourceManager.peek` → `GlbResourceManager.peek` (the fully-qualified reference from Task 4 is now `GlbResourceManager.peek`)
- `RenderResourceManager.takeLitematic` → `GlbResourceManager.receiveLitematic`

In `BlueprintDetailScreen.kt`, update call sites:
- `RenderResourceManager.cachedKeys` → `GlbResourceManager.cachedKeys`
- `RenderResourceManager.putGlb(...)` — drop the `ByteArray(0)` arg and the `cacheFile =` named arg, since signature is now `(uuid, cacheFile, minY, centerX, centerZ)`
- `RenderResourceManager.clearGlb` → `GlbResourceManager.clearGlb`

The final `putGlb` call should be:
```kotlin
                GlbResourceManager.putGlb(bp.meta.uuid, cacheFile, modelMinY, modelCX, modelCZ)
```

- [ ] **Step 3: Build**

```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS.

- [ ] **Step 4: Verify grep targets return zero for dead code**

Run:
```bash
cd app/src/main && \
  echo "--- generate() outside LitematicToGlb ---" && grep -rn "\.generate(" . | grep -v "LitematicToGlb.convert" | grep -v "createModelInstance" | grep -v "GlbGenerator.kt" || echo "PASS" && \
  echo "--- takeGlb ---" && grep -rn "takeGlb" . || echo "PASS" && \
  echo "--- cachedGlb field ---" && grep -rn "cachedGlb\b" . || echo "PASS" && \
  echo "--- cachedGlbKey/MinY/CenterX/CenterZ/File fields ---" && grep -rn "cachedGlbKey\|cachedGlbMinY\|cachedGlbCenterX\|cachedGlbCenterZ\|cachedGlbFile" . || echo "PASS" && \
  echo "--- GlbCacheEntry ---" && grep -rn "GlbCacheEntry" . || echo "PASS" && \
  echo "--- GlbCache.get/put ---" && grep -rn "GlbCache\.get(\|GlbCache\.put(" . || echo "PASS" && \
  echo "--- takeLitematic/putLitematic ---" && grep -rn "takeLitematic\|putLitematic" . || echo "PASS"
```

Expected: All targets print "PASS" (zero hits).

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/java/io/github/moxisuki/blockprint/cat/
git commit -m "refactor(manager): two-tier state, drop vestigial, fix sizeBytes

Two-tier state design:
- cachedKeys: StateFlow<Set<String>> mirrored from Room (UI subscription)
- sessionCache: MutableMap<String, CachedGlb> per-process (PreviewScreen
  hot path)

Room schema unchanged; sizeBytes now writes cacheFile.length() instead
of the (always-0) bytes.size from the dropped ByteArray parameter.

Removed:
- takeGlb() — zero callers
- cachedGlb ByteArray field — only self-referenced
- cachedGlbKey/MinY/CenterX/CenterZ/File fields — folded into CachedGlb
- GlbCacheEntry data class
- GlbCache.get/put ByteArray variants — zero callers
- GlbGenerator.generate() — zero callers

Renamed:
- putLitematic → transferLitematic (matches semantics, avoids 'take'
  confusion with the deleted takeGlb)
- takeLitematic → receiveLitematic

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 8: PreviewScreen — `modelOnScreen` state + `onFrame` detection + drop `!fromCache` gate + drop manual `FileChannel.map`

**Files:**
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt`

This is the UX bugfix. Replaces the `centered`-based loading logic with `modelOnScreen`-based logic driven by `onFrame`. Also drops manual `RandomAccessFile`/`FileChannel.map` in favor of `modelLoader.createModelInstance(file)`.

- [ ] **Step 1: Add `modelOnScreen` state and replace the `LaunchedEffect(centered, glbBytes)` block**

Find the lines around 303-307:
```kotlin
    var loadingVisible by remember { mutableStateOf(true) }
    LaunchedEffect(centered, glbBytes) {
        if (!centered) loadingVisible = true
        else { kotlinx.coroutines.delay(400); loadingVisible = false }
    }
```

Replace with:
```kotlin
    var modelOnScreen by remember { mutableStateOf(false) }
    var loadingVisible by remember { mutableStateOf(true) }
    LaunchedEffect(modelOnScreen, glbEntry) {
        loadingVisible = !modelOnScreen
        if (modelOnScreen) kotlinx.coroutines.delay(250)
    }
```

- [ ] **Step 2: Replace the model loading `LaunchedEffect`**

Find lines around 387-408 (the `LaunchedEffect(glbFile ?: glbBytes)` block). Replace it with:

```kotlin
        LaunchedEffect(glbEntry?.cacheFile) {
            modelOnScreen = false
            modelError = false
            val file = glbEntry?.cacheFile ?: return@LaunchedEffect
            try {
                modelInst = modelLoader.createModelInstance(file)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "模型加载失败: ${e.message}", e)
                modelErrorMessage = if (e.message?.contains("Empty vertex") == true) context.getString(R.string.preview_resource_missing)
                else context.getString(R.string.preview_render_failed_with_msg, e.message ?: "")
                modelError = true; null
            }
        }
```

Note: the existing `glbFile`/`glbBytes` local vals at the top of `PreviewSceneContent` can be removed since `glbEntry.cacheFile` is now the single source. Replace lines around 289-290:

```kotlin
    val glbFile = entry.cacheFile
    val glbBytes = entry.bytes
```

with:

```kotlin
    // entry.cacheFile is the single source for the model file
```

- [ ] **Step 3: Update `SceneView`'s `onFrame` callback**

Find the `onFrame = { ... }` lambda inside `SceneView(...)`. Add the `modelOnScreen` detection at the top:

```kotlin
            onFrame = { frameTimeNanos ->
                if (modelInst != null && !modelOnScreen) modelOnScreen = true
                val delta = if (lastFrameNanos > 0) ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceIn(0f, 0.1f) else 0f
                lastFrameNanos = frameTimeNanos
                if (cam.isWalk) cam.applyWalkMove(delta)
                cam.applyToCamera(cameraNode)
            },
```

- [ ] **Step 4: Remove the `!fromCache` gate on the loading overlay**

Find the overlay check around line 482:
```kotlin
        if (!fromCache && loadingVisible && !modelError) {
```

Replace with:
```kotlin
        if (loadingVisible && !modelError) {
```

- [ ] **Step 5: Update `key(glbBytes)` to `key(glbEntry)`**

Find the `key(glbBytes)` wrapper:
```kotlin
        key(glbBytes) {
        SceneView(
```

Replace with:
```kotlin
        key(glbEntry) {
        SceneView(
```

- [ ] **Step 6: Build**

```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS.

- [ ] **Step 7: Verify grep for old anti-patterns**

```bash
cd app/src/main && \
  echo "--- FileChannel.map in PreviewScreen ---" && grep -n "FileChannel.map\|RandomAccessFile" java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt || echo "PASS" && \
  echo "--- !fromCache gate ---" && grep -n "!fromCache" java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt || echo "PASS"
```

Expected: Both print "PASS".

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt
git commit -m "fix(preview): onFrame-based loading overlay, drop manual mmap

- New modelOnScreen state set true by SceneView.onFrame when
  modelInst != null — replaces centered (which only fired when
  ModelNode was added to the tree, not when actually rendered)
- Overlay now hides after first model frame + 250ms buffer, not
  after 'centered + 400ms' (which often left a black-frame gap)
- Overlay no longer gated by !fromCache — cache hits now show the
  loading indicator instead of jumping straight to black SceneView
- modelLoader.createModelInstance(File) used directly; SceneView
  handles mmap internally, removing manual RandomAccessFile + map
- key(glbEntry) replaces key(glbBytes) — glbBytes was always empty
  post-refactor, keying on the whole entry is more stable

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 9: Split `PreviewScreen.kt` into 5 files

**Files:**
- Create: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewCamera.kt`
- Create: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewLighting.kt`
- Create: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewJoystick.kt`
- Create: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewToolbar.kt`
- Create: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewEntryPoint.kt`
- Modify: `app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt`

The original `PreviewScreen.kt` is ~946 lines. After this task it drops to ~400 lines (entry + scene content skeleton + GlbEntry).

- [ ] **Step 1: Create `PreviewCamera.kt`**

Extract the `CameraController` class (lines ~733-835 of original) and any related constants. The file should look like:

```kotlin
package io.github.moxisuki.blockprint.cat.ui.preview

import io.github.sceneview.math.Position
import io.github.sceneview.node.CameraNode
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val DRAG_SPEED = 0.15f
private const val ZOOM_STRENGTH = 5f
private const val WALK_ROTATE_SPEED = 0.004f
private const val WALK_MOVE_SPEED = 12f

internal class CameraController(
    var eyeX: Float, var eyeY: Float, var eyeZ: Float,
    var targetX: Float, var targetY: Float, var targetZ: Float,
) {
    var gridY = 0f; var gridOffX = 0f; var gridOffZ = 0f
    var anchorX = 0f; var anchorY = 0f; var anchorZ = 0f
    private var initEyeX: Float; private var initEyeY: Float; private var initEyeZ: Float
    private var initTargetX: Float; private var initTargetY: Float; private var initTargetZ: Float
    private var walkYaw: Float; private var walkPitch: Float
    private var initWalkYaw: Float; private var initWalkPitch: Float

    @Volatile var walkForward = 0f; @Volatile var walkRight = 0f
    @Volatile var isWalk = false
    init {
        initEyeX = eyeX; initEyeY = eyeY; initEyeZ = eyeZ
        initTargetX = targetX; initTargetY = targetY; initTargetZ = targetZ
        val dx = targetX - eyeX; val dy = targetY - eyeY; val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz); walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
        initWalkYaw = walkYaw; initWalkPitch = walkPitch
    }

    fun setWalkInput(f: Float, r: Float) { walkForward = f.coerceIn(-1f, 1f); walkRight = r.coerceIn(-1f, 1f) }
    fun syncWalkOrientation() {
        val dx = targetX - eyeX; val dy = targetY - eyeY; val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz)
        walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
    }
    fun reset() {
        eyeX = initEyeX; eyeY = initEyeY; eyeZ = initEyeZ
        targetX = initTargetX; targetY = initTargetY; targetZ = initTargetZ
        walkYaw = initWalkYaw; walkPitch = initWalkPitch; walkForward = 0f; walkRight = 0f
    }
    fun setTargetFromNode(node: io.github.sceneview.node.ModelNode) {
        val b = node.boundingBox; val c = b.center
        eyeX += c[0] - targetX; eyeY += c[1] - targetY; eyeZ += c[2] - targetZ
        targetX = c[0]; targetY = c[1]; targetZ = c[2]
        anchorX = c[0]; anchorY = c[1]; anchorZ = c[2]
        gridY = c[1] - b.halfExtent[1]
        gridOffX = if ((b.halfExtent[0] * 2).toInt() % 2 == 1) 0.5f else 0f
        gridOffZ = if ((b.halfExtent[2] * 2).toInt() % 2 == 1) 0.5f else 0f
        initEyeX = eyeX; initEyeY = eyeY; initEyeZ = eyeZ
        initTargetX = targetX; initTargetY = targetY; initTargetZ = targetZ
        val dx = targetX - eyeX; val dy = targetY - eyeY; val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz); walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
        initWalkYaw = walkYaw; initWalkPitch = walkPitch
    }

    fun orbitRaw(dh: Float, dv: Float) = orbit(dh, dv)
    fun dragRaw(dx: Float, dy: Float) = drag(dx, dy)
    fun walkRotateRaw(dyaw: Float, dpitch: Float) = walkRotate(dyaw, dpitch)
    fun zoomRaw(factor: Float) = zoom(factor)

    private fun walkRotate(dYaw: Float, dPitch: Float) { walkYaw += dYaw; walkPitch = (walkPitch + dPitch).coerceIn(-1.5f, 1.5f) }

    fun applyWalkMove(dt: Float) {
        if (walkForward == 0f && walkRight == 0f) return
        val spd = WALK_MOVE_SPEED * dt
        val fx = cos(walkPitch) * sin(walkYaw); val fy = sin(walkPitch); val fz = cos(walkPitch) * cos(walkYaw)
        val rx = -fz; val rz = fx; val rl = sqrt(rx * rx + rz * rz)
        val nrx = if (rl > 0.001f) rx / rl else 0f; val nrz = if (rl > 0.001f) rz / rl else 0f
        eyeX += (fx * walkForward + nrx * walkRight) * spd
        eyeY += fy * walkForward * spd
        eyeZ += (fz * walkForward + nrz * walkRight) * spd
    }

    fun applyToCamera(cameraNode: CameraNode) {
        val (cx, cy, cz) = if (isWalk) {
            val ld = 10f
            Triple(eyeX + cos(walkPitch) * sin(walkYaw) * ld,
                   eyeY + sin(walkPitch) * ld,
                   eyeZ + cos(walkPitch) * cos(walkYaw) * ld)
        } else {
            Triple(targetX, targetY, targetZ)
        }
        cameraNode.lookAt(eye = Position(eyeX, eyeY, eyeZ), center = Position(cx, cy, cz), up = Position(0f, 1f, 0f))
    }

    private fun orbit(dH: Float, dV: Float) {
        val ox = eyeX - targetX; val oy = eyeY - targetY; val oz = eyeZ - targetZ
        val d = sqrt(ox * ox + oy * oy + oz * oz); if (d < 0.01f) return
        val t = atan2(ox, oz) + dH; val p = (asin((oy / d).coerceIn(-1f, 1f)) + dV).coerceIn(-1.5f, 1.5f)
        eyeX = targetX + d * cos(p) * sin(t); eyeY = targetY + d * sin(p); eyeZ = targetZ + d * cos(p) * cos(t)
    }
    private fun drag(dx: Float, dy: Float) {
        val ox = eyeX - targetX; val oz = eyeZ - targetZ; val d = sqrt(ox * ox + oz * oz).coerceAtLeast(0.1f)
        val rx = -oz / d; val rz = ox / d; val s = d * DRAG_SPEED
        val mx = rx * dx * s; val mz = rz * dx * s; val my = dy * s
        targetX -= mx; targetY -= my; targetZ -= mz; eyeX -= mx; eyeY -= my; eyeZ -= mz
    }
    private fun zoom(f: Float) {
        val s = 1f + (f - 1f) * ZOOM_STRENGTH
        val dx = eyeX - targetX; val dy = eyeY - targetY; val dz = eyeZ - targetZ
        val d = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.5f); val nd = (d / s).coerceIn(1.5f, 500f); val r = nd / d
        eyeX = targetX + dx * r; eyeY = targetY + dy * r; eyeZ = targetZ + dz * r
    }
}
```

- [ ] **Step 2: Create `PreviewLighting.kt`**

Extract `LightPreset`, `LIGHT_PRESETS`, `applyPreviewLightPreset`, `rememberNoDiscSunLight`, `updateMCNoDiscSunLight`. The file should look like:

```kotlin
package io.github.moxisuki.blockprint.cat.ui.preview

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.colorOf
import io.github.sceneview.node.LightNode

private const val LIGHTING_TAG = "PreviewLighting"

private data class LightPreset(
    val label: String,
    val timeOfDay: Float,
    val sunIntensity: Float,
    val fillIntensity: Float,
    val envName: String = "neutral",
    val celestialAzimuthDeg: Float,
    val celestialElevationDeg: Float,
)

internal val LIGHT_PRESETS = arrayOf(
    LightPreset("白天", timeOfDay = 12f, sunIntensity = 140_000f, fillIntensity = 10_000f, envName = "noon",   celestialAzimuthDeg = 135f, celestialElevationDeg = 55f),
    LightPreset("黄昏", timeOfDay = 18f, sunIntensity = 90_000f,  fillIntensity = 8_000f,  envName = "sunset", celestialAzimuthDeg = 270f, celestialElevationDeg = 2f),
    LightPreset("夜晚", timeOfDay = 0f,  sunIntensity = 5_000f,   fillIntensity = 1_200f,  envName = "night",  celestialAzimuthDeg = 110f, celestialElevationDeg = 45f),
    LightPreset("影棚", timeOfDay = 12f, sunIntensity = 30_000f,  fillIntensity = 60_000f, envName = "studio", celestialAzimuthDeg = 135f, celestialElevationDeg = 55f),
)

internal fun applyPreviewLightPreset(
    mainLight: LightNode,
    fillLight: LightNode,
    preset: LightPreset,
) {
    fillLight.intensity = preset.fillIntensity
    updateMCNoDiscSunLight(
        lightNode = mainLight,
        timeOfDay = preset.timeOfDay,
        sunIntensity = preset.sunIntensity,
        celestialAzimuthDeg = preset.celestialAzimuthDeg,
        celestialElevationDeg = preset.celestialElevationDeg,
    )
}

@Composable
internal fun rememberNoDiscSunLight(engine: com.google.android.filament.Engine): LightNode {
    val lightNode = remember(engine) {
        LightNode(
            engine = engine,
            entity = EntityManager.get().create(),
            builder = LightManager.Builder(LightManager.Type.DIRECTIONAL).apply {
                intensity(110_000f)
                color(1f, 1f, 1f)
                castShadows(true)
            }
        )
    }
    DisposableEffect(lightNode) {
        onDispose { lightNode.destroy() }
    }
    return lightNode
}

private fun updateMCNoDiscSunLight(
    lightNode: LightNode,
    timeOfDay: Float,
    sunIntensity: Float,
    celestialAzimuthDeg: Float,
    celestialElevationDeg: Float,
    turbidity: Float = 2f,
) {
    val az = Math.toRadians(celestialAzimuthDeg.toDouble()).toFloat()
    val el = Math.toRadians(celestialElevationDeg.toDouble()).toFloat()
    val dirX = kotlin.math.sin(az) * kotlin.math.cos(el) * 0.6f
    val dirY = kotlin.math.sin(el)
    val dirZ = -kotlin.math.cos(az) * kotlin.math.cos(el) * 0.5f
    val len = kotlin.math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ).coerceAtLeast(1e-6f)
    val elevation = dirY / len

    lightNode.lightDirection = Direction(x = dirX / len, y = dirY / len, z = dirZ / len)

    lightNode.color = if (timeOfDay < 6f || timeOfDay >= 18f) {
        colorOf(r = 0.78f, g = 0.84f, b = 0.96f)
    } else {
        val horizonFactor = (1f - elevation.coerceIn(0f, 1f)) * (1f - elevation.coerceIn(0f, 1f))
        val turbidityBoost = ((turbidity - 1f) / 9f).coerceIn(0f, 1f)
        val warmR = 1.0f
        val warmG = 0.45f + 0.05f * turbidityBoost
        val warmB = 0.20f - 0.10f * turbidityBoost
        colorOf(
            r = warmR * horizonFactor + 1.00f * (1f - horizonFactor),
            g = warmG * horizonFactor + 0.98f * (1f - horizonFactor),
            b = warmB * horizonFactor + 0.95f * (1f - horizonFactor),
        )
    }

    lightNode.intensity = if (timeOfDay < 6f || timeOfDay >= 18f) sunIntensity else sunIntensity * elevation.coerceAtLeast(0f)
}

internal fun loadCachedEnvironments(environmentLoader: EnvironmentLoader): Map<String, Environment> {
    fun loadEnv(name: String): Environment {
        return try {
            environmentLoader.createKTX1Environment(
                iblAssetFile = "environments/$name/${name}_ibl.ktx",
                skyboxAssetFile = "environments/$name/${name}_skybox.ktx",
            )
        } catch (e: Exception) {
            Log.w(LIGHTING_TAG, "env=$name KTX 缺失,使用 fallback neutral: ${e.message}")
            environmentLoader.createKTX1Environment(
                iblAssetFile = "environments/neutral/neutral_ibl.ktx",
                skyboxAssetFile = "environments/neutral/neutral_skybox.ktx",
            )
        }.also { env ->
            Log.i(LIGHTING_TAG, "env=$name loaded: skybox=${env.skybox != null}, ibl=${env.indirectLight != null}")
        }
    }

    return mapOf(
        "noon" to loadEnv("noon"),
        "sunset" to loadEnv("sunset"),
        "night" to loadEnv("night"),
        "studio" to loadEnv("studio"),
    )
}
```

Note: `loadCachedEnvironments` is added because the original 4 lines of inline environment loading in `PreviewSceneContent` (lines ~313-337) is also extracted here.

- [ ] **Step 3: Create `PreviewJoystick.kt`**

Extract `WalkJoystick` composable and its constants:

```kotlin
package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val JOYSTICK_RADIUS_DP = 72
private const val THUMB_RADIUS_DP = 32

@Composable
internal fun WalkJoystick(
    onMove: (forward: Float, right: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseR = with(LocalDensity.current) { JOYSTICK_RADIUS_DP.dp.toPx() }
    val thumbR = with(LocalDensity.current) { THUMB_RADIUS_DP.dp.toPx() }
    var tx by remember { mutableFloatStateOf(0f) }; var ty by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sf = MaterialTheme.colorScheme.surface; val onSf = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size((JOYSTICK_RADIUS_DP * 2).dp)
            .clip(CircleShape).background(sf.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; tx = 0f; ty = 0f; onMove(0f, 0f) },
                    onDragCancel = { dragging = false; tx = 0f; ty = 0f; onMove(0f, 0f) },
                    onDrag = { change, _ ->
                        val rx = change.position.x - baseR; val ry = change.position.y - baseR
                        val d = sqrt(rx * rx + ry * ry); val maxD = baseR - thumbR
                        val s = if (d > 0.01f) d.coerceAtMost(maxD) / d else 0f
                        tx = rx * s; ty = ry * s
                        onMove(-(ty / maxD), tx / maxD)
                    },
                )
            },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Box(Modifier.size(thumbR.dp).offset { IntOffset(tx.roundToInt(), ty.roundToInt()) }
            .clip(CircleShape).background(onSf.copy(alpha = if (dragging) 0.55f else 0.35f)))
    }
}
```

- [ ] **Step 4: Create `PreviewToolbar.kt`**

Extract `ToolIcon`, `LayerIconBtn`, toolbar Row (lines 578-605), and the layer panel Column (lines 607-659). The new file should look like:

```kotlin
package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

@Composable
internal fun ToolIcon(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).padding(4.dp)) { Icon(icon, desc, Modifier.size(22.dp), tint = tint) }
}

@Composable
internal fun LayerIconBtn(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint)
    }
}

@Composable
internal fun PreviewToolbar(
    onReset: () -> Unit,
    lightPreset: Int,
    onCycleLight: () -> Unit,
    showGrid: Boolean,
    onToggleGrid: () -> Unit,
    layerPanelOpen: Boolean,
    onToggleLayerPanel: () -> Unit,
    cameraMode: CameraMode,
    onCycleCameraMode: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    toolbarIconOn: Color,
    toolbarIconOff: Color,
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.End) {
            ToolIcon(Icons.Default.Refresh, stringResource(R.string.cd_reset), toolbarIconOn) { onReset() }
            Box(Modifier.clickable { onCycleLight() }.padding(4.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LightMode, LIGHT_PRESETS[lightPreset].label, Modifier.size(22.dp), tint = if (lightPreset != 2) toolbarIconOn else toolbarIconOff)
                    Spacer(Modifier.width(4.dp))
                    Text(LIGHT_PRESETS[lightPreset].label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = toolbarIconOn, maxLines = 1)
                }
            }
            ToolIcon(Icons.Default.GridOn, stringResource(R.string.cd_grid), if (showGrid) toolbarIconOn else toolbarIconOff) { onToggleGrid() }
            ToolIcon(Icons.Default.Layers, stringResource(R.string.cd_layer), if (layerPanelOpen) toolbarIconOn else toolbarIconOff) { onToggleLayerPanel() }
            Box(Modifier.clickable { onCycleCameraMode() }.padding(4.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (cameraMode) {
                        CameraMode.ORBIT -> Icons.Default.ViewInAr; CameraMode.DRAG -> Icons.Default.OpenWith; CameraMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                    }, cameraMode.label, Modifier.size(22.dp), tint = toolbarIconOn)
                    Spacer(Modifier.width(4.dp))
                    Text(cameraMode.label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = toolbarIconOn, maxLines = 1)
                }
            }
            ToolIcon(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, stringResource(R.string.cd_fullscreen), toolbarIconOn) {
                onToggleFullscreen()
            }
        }
    }
}

@Composable
internal fun PreviewLayerPanel(
    layerPanelOpen: Boolean,
    layerY: Int,
    floorCount: Int,
    onLayerUp: () -> Unit,
    onLayerDown: () -> Unit,
    onShowAllLayers: () -> Unit,
) {
    if (!layerPanelOpen) return
    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.layer_panel_title), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            if (layerY == Int.MAX_VALUE) stringResource(R.string.layer_all)
            else "${layerY + 1} / $floorCount",
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LayerIconBtn(Icons.Default.KeyboardArrowUp, "+1", enabled = layerY == Int.MAX_VALUE || layerY < floorCount - 1, onClick = onLayerUp)
            Spacer(Modifier.width(4.dp))
            LayerIconBtn(Icons.Default.KeyboardArrowDown, "-1", enabled = layerY != Int.MAX_VALUE, onClick = onLayerDown)
        }
        Spacer(Modifier.height(6.dp))
        LayerIconBtn(Icons.Default.Layers, stringResource(R.string.layer_show_all), enabled = layerY != Int.MAX_VALUE, onClick = onShowAllLayers)
    }
}
```

- [ ] **Step 5: Create `PreviewEntryPoint.kt`**

```kotlin
package io.github.moxisuki.blockprint.cat.ui.preview

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PreviewEntryPoint {
    fun blueprintManager(): BlueprintManager

    companion object {
        fun resolve(context: Context): BlueprintManager =
            dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, PreviewEntryPoint::class.java)
                .blueprintManager()
    }
}
```

- [ ] **Step 6: Strip the extracted code from `PreviewScreen.kt`**

Remove from `PreviewScreen.kt`:
- The `JOYSTICK_RADIUS_DP`, `THUMB_RADIUS_DP` constants
- The `CameraMode` enum (moved to top of `PreviewScreen.kt`; actually keep here for now, it's small and used by state in `PreviewSceneContent`)
- The `LightPreset` data class, `LIGHT_PRESETS`, `applyPreviewLightPreset`, `rememberNoDiscSunLight`, `updateMCNoDiscSunLight`
- The `WalkJoystick` composable
- The `ToolIcon`, `LayerIconBtn` composables
- The toolbar `Row` block (lines 578-605) — replace with `PreviewToolbar(...)` call
- The layer panel `Column` block (lines 607-659) — replace with `PreviewLayerPanel(...)` call
- The `PreviewEntryPoint` interface (the bottom of the file)

Add at the top of `PreviewScreen.kt`:
```kotlin
import io.github.moxisuki.blockprint.cat.ui.preview.WalkJoystick as _WalkJoystickImport // not needed
```

Actually imports are auto-managed by the IDE. Just ensure the toolbar/joystick/lighting/camera are NOT in this file's imports anymore — they're imported FROM here.

Replace the inline toolbar (lines 570-605) with:

```kotlin
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            PreviewToolbar(
                onReset = { cam.reset(); cam.applyToCamera(cameraNode) },
                lightPreset = lightPreset,
                onCycleLight = { lightPreset = (lightPreset + 1) % LIGHT_PRESETS.size },
                showGrid = showGrid,
                onToggleGrid = { showGrid = !showGrid },
                layerPanelOpen = layerPanelOpen,
                onToggleLayerPanel = { layerPanelOpen = !layerPanelOpen },
                cameraMode = cameraMode,
                onCycleCameraMode = { cameraMode = CameraMode.entries[(cameraMode.ordinal + 1) % CameraMode.entries.size] },
                fullscreen = fullscreen,
                onToggleFullscreen = {
                    fullscreen = !fullscreen
                    onFullscreenChange?.invoke(fullscreen)
                },
                toolbarIconOn = toolbarIconOn,
                toolbarIconOff = toolbarIconOff,
            )
        }
```

Replace the inline layer panel (lines 607-659) with:

```kotlin
        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            PreviewLayerPanel(
                layerPanelOpen = layerPanelOpen,
                layerY = layerY,
                floorCount = floorCount,
                onLayerUp = {
                    if (layerY == Int.MAX_VALUE) layerY = 0
                    else if (layerY < floorCount - 1) layerY++
                },
                onLayerDown = { if (layerY > 0) layerY-- else layerY = Int.MAX_VALUE },
                onShowAllLayers = { layerY = Int.MAX_VALUE },
            )
        }
```

Replace the inline `cachedEnvironments = remember(environmentLoader) { ... }` block (lines 313-337) with:

```kotlin
    val cachedEnvironments = remember(environmentLoader) { loadCachedEnvironments(environmentLoader) }
```

(After this the local `DisposableEffect(environmentLoader, cachedEnvironments) { ... }` can remain in `PreviewScreen.kt` since it disposes environment resources.)

- [ ] **Step 7: Build**

```bash
cd app && ./gradlew compileDebugKotlin
```

Expected: SUCCESS. If build fails due to missing imports, the IDE auto-suggests; in CLI, add imports for `PreviewToolbar`, `PreviewLayerPanel`, `loadCachedEnvironments`, `applyPreviewLightPreset`, `rememberNoDiscSunLight`, `CameraController`, `WalkJoystick` from the same package — they should resolve without explicit imports since they're in the same package.

- [ ] **Step 8: Verify file sizes**

```bash
cd app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview && wc -l *.kt
```

Expected: `PreviewScreen.kt` is now roughly 350-450 lines (down from 946). New files total around 600 lines combined.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/io/github/moxisuki/blockprint/cat/ui/preview/
git commit -m "refactor(preview): split monolithic PreviewScreen.kt into 5 files

PreviewScreen.kt was 946 lines doing too many things:
- Camera math (orbit/drag/zoom/walk) → PreviewCamera.kt
- Light presets + sun light → PreviewLighting.kt
- Walk joystick composable → PreviewJoystick.kt
- Toolbar + layer panel UI → PreviewToolbar.kt
- Hilt entry point → PreviewEntryPoint.kt

No behavior change. PreviewScreen.kt is now ~400 lines containing only
entry orchestration, PreviewSceneContent skeleton, and GlbEntry.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 10: Manual verification T1–T11

This task produces no commits. It validates the refactor end-to-end against the spec's verification checklist.

- [ ] **Step 1: Clean build**

```bash
cd app && ./gradlew clean assembleDebug
```

Expected: BUILD SUCCESSFUL with no warnings about unused imports or dead code.

- [ ] **Step 2: Install and run on a test device**

```bash
cd app && ./gradlew installDebug
```

- [ ] **Step 3: Run manual verification checklist**

Execute each test case from the spec:

- T1 (new blueprint, DetailScreen generate) → Dialog shows 0→100%, navigate to preview
- T2 (just-generated, PreviewScreen) → **Overlay visible immediately**, model appears after
- T3 (restart App, PreviewScreen) → Brief overlay, **no top-level progress**
- T4 (DetailScreen "View Cached" button) → Same as T2
- T5 (DetailScreen "Regenerate") → Confirm dialog (if >70k blocks), else direct regen dialog
- T6 (Settings → Cache Management) → New entries sum correctly; legacy still 0
- T7 (deleted blueprint, open cached preview) → Error card "蓝图不存在或已被删除"
- T8 (uninstall + reinstall, redownload, generate) → Same as T1
- T9 (>70k blocks) → Confirm flow works
- T10 (light preset switch) → 4 presets cycle smoothly
- T11 (Walk mode + joystick) → Camera follows, no lag

For each, mark pass/fail in the PR description.

- [ ] **Step 4: Final grep verification**

Run all G5 grep targets + spec grep targets:

```bash
cd app/src/main && \
  echo "G5 grep targets (must be 0):" && \
  echo "  generate() outside LitematicToGlb: $(grep -rn '\.generate(' . | grep -v 'LitematicToGlb.convert' | grep -v 'createModelInstance' | grep -v 'GlbGenerator.kt' | wc -l)" && \
  echo "  takeGlb: $(grep -rn 'takeGlb' . | wc -l)" && \
  echo "  cachedGlb field: $(grep -rn 'cachedGlb\b' . | wc -l)" && \
  echo "  5 separate fields: $(grep -rn 'cachedGlbKey\|cachedGlbMinY\|cachedGlbCenterX\|cachedGlbCenterZ\|cachedGlbFile' . | wc -l)" && \
  echo "  GlbCacheEntry: $(grep -rn 'GlbCacheEntry' . | wc -l)" && \
  echo "  GlbCache.get/put: $(grep -rn 'GlbCache\.get(\|GlbCache\.put(' . | wc -l)" && \
  echo "  takeLitematic/putLitematic: $(grep -rn 'takeLitematic\|putLitematic' . | wc -l)" && \
  echo "" && \
  echo "UX bugfix grep targets (must be 0):" && \
  echo "  FileChannel.map in PreviewScreen: $(grep -n 'FileChannel.map\|RandomAccessFile' java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt | wc -l)" && \
  echo "  !fromCache gate: $(grep -n '!fromCache' java/io/github/moxisuki/blockprint/cat/ui/preview/PreviewScreen.kt | wc -l)"
```

Expected output: All counts are 0.

- [ ] **Step 5: Update PR description with verification results**

Append the pass/fail results from Step 3 to the PR description. If any test failed, **do not merge** — investigate and add a fix-up commit.

---

## Self-Review Notes

(Recorded for traceability.)

**Spec coverage:**
- G1 (no GLB bytes in compose state) → Task 8 (drops `GlbEntry.bytes`, drops `cachedGlb` field via Task 7)
- G2 (no manual mmap) → Task 8 (uses `modelLoader.createModelInstance(file)`)
- G3 (overlay for all cache states) → Task 8 (removes `!fromCache` gate, adds `modelOnScreen`)
- G4 (no top-level progress on disk hit) → Task 4 (Segment 2 doesn't trigger top-level progress)
- G5 (no duplicate / zero-caller APIs) → Task 5, Task 7
- G6 (sizeBytes accurate) → Task 7 (`cacheFile.length()`)

**Placeholder scan:** No "TBD", "TODO", "fill in details" present. All file paths and code shown.

**Type consistency:**
- `GlbGenerator.Key` introduced in Task 1, used in Task 2, 3, 4, 7, 8 — consistent.
- `GlbResourceManager.putGlb(uuid, cacheFile, minY, centerX, centerZ)` final signature introduced in Task 7 — all callers updated in Tasks 3 (via interim), 7, 8.
- `GlbResourceManager.peek(uuid): CachedGlb?` introduced in Task 7 — used in Task 4 (which was authored before Task 7 but uses fully-qualified name to avoid breakage).
- `PreviewEntryPoint` extracted in Task 9 Step 5 — `PreviewScreen.kt` Step 6 removes the old declaration.

**Known issue handled:** Task 3 makes a temporary call `putGlb(uuid, ByteArray(0), ..., cacheFile = cacheFile)` that doesn't match the new signature introduced in Task 7. The plan acknowledges this by having Task 3 keep the old signature with `cacheFile =` named arg, then Task 7 Step 2 cleans up the call site. Commits are sequential; the intermediate state has a working build because `putGlb` still accepts those args at that point.
