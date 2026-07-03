# BlockPrint Cat — Development Conventions

Android app (Kotlin + Compose + Hilt + Room) for browsing, previewing, and
converting Minecraft blueprint files (.litematic, .schem, .nbt, .json).

## Build & verify

```bash
./gradlew compileDebugKotlin        # required after every code change
./gradlew testDebugUnitTest         # required before commit
```

`app/build.gradle.kts` enables Compose Compiler Reports + metrics; if a
regression appears in `compose_reports/` (unstable classes, restartable
but not skippable), fix it before shipping.

## i18n

All user-facing strings live in `app/src/main/res/values/strings.xml`.
Localized copies:

- `app/src/main/res/values-en/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`

Rules:

- **Never** hardcode Chinese or English in composables, ViewModels, or
  error messages. Use `stringResource(R.string.xxx)` (Composable) or
  `context.getString(R.string.xxx)` (other).
- Add the key to all three files in the same commit. `values-en/` is
  the English fallback; `values-zh-rCN/` is the source-of-truth Chinese.
  The bare `values/` matches the source-of-truth language (zh) and
  doubles as the default for unspecified locales.
- For purely decorative / debug strings (log tags, content descriptions
  that always match an icon's semantic), `stringResource` is still
  preferred — keeps translators in the loop.

## UI / composable structure

**Avoid large single files.** When a screen file passes ~600 lines, split
it into focused sibling files in the same `ui/<feature>/` package — see
`ui/detail/` for the canonical example:

```
ui/detail/
  BlueprintDetailScreen.kt   # phone entry: state, dispatch, branch
  BlueprintDetailContent.kt  # pad entry: same logic, different chrome
  DetailRows.kt              # DetailRow / FormatRow / SectionCard
  MaterialSection.kt         # MaterialRow / MaterialIcon / resolver
  ConvertDialog.kt           # pure rendering, no business logic
  NamespaceCard.kt           # one card, one file
  PreviewButton.kt           # preview-generate flow
  DetailViewModel.kt
```

Conventions for split files:

- One composable per file, named after the most prominent widget.
- `internal` visibility for cross-file helpers; `public` only for the
  entry point that other packages import.
- Pure rendering components (no ViewModel, no state ownership) take
  everything as parameters. State-owning screens pass state down.
- Pre-compute lists / derived data in the parent (`remember(key) { ... }`)
  and pass them in — children should not call `FormatCatalog.foo()`
  per-recomposition.

## Compose stability rules

- Wrap event lambdas in `remember(...)` if the captured inputs are
  stable. Without this, downstream children can't skip recomposition.
  ```kotlin
  val openConvertDialog = remember(convertTargets) { { ... } }
  ```
- Hoist expensive lookups (Hilt `EntryPointAccessors.fromApplication`,
  `iconIndexResolver`, palette resolutions) **once per screen**, not
  per row. Expose them via a `rememberXxx()` helper in the shared
  sibling file (see `rememberIconIndexResolver()` in `MaterialSection.kt`).
- Do **not** wrap a direct state mutation in `LaunchedEffect` inside a
  Coil `error` slot. The state write already triggers recomposition;
  the wrapper just adds a frame of latency.
- Flatten nested `scope.launch { scope.launch { ... } }` — the inner
  scope serves no purpose.

## Architecture

- `@HiltViewModel` for screen state. `@Singleton` for cross-screen
  services (managers, storage, network).
- `@EntryPoint` + `EntryPointAccessors.fromApplication(...)` is the
  escape hatch for non-injected composables (preview-render code, the
  MaterialRow subtree). Prefer a real `@HiltViewModel` first.
- Hilt requires `internal` interface declarations to live in modules
  with `@InstallIn(SingletonComponent::class)` — not in a feature
  package — when multiple modules depend on them. The detail-screen
  `DetailScreenEntryPoint` is feature-local because only the detail
  screen reads it.

## blockprint-core dependency

`gradle/libs.versions.toml` pins `blockprint = "1.0.0"`, resolved from
`mavenLocal()` (configured in `settings.gradle.kts` under the
`io.github.moxisuki` group). The core jar lives at
`~/.m2/repository/io/github/moxisuki/blockprint-core/<version>/`.

When bumping the core version, check `blockprint-core/CHANGELOG.md` for
breaking changes — the 1.0.0 release renamed `Litematic` →
`BlockPrintDocument`, `LitematicReader` → `BlockPrintReader`, etc., and
moved `glb.FileAccessor` → `glb.platform.FileAccessor`. The companion
docs in `blockprint-core/docs/` (`BLUEPRINT_API.md`,
`GLB_PIPELINE.md`) are the canonical API reference.

## Git

- Identity is already set: `Moix <59305689+moxisuki@users.noreply.github.com>`.
- Commit messages: conventional commits (`feat:`, `fix:`, `refactor:`,
  `chore:`, `perf:`, `docs:`, `test:`).
- **Do not** add `Co-Authored-By: Claude` (or any other AI) footers.
  This is enforced globally; commits with AI footers will be rejected.
- One commit per logical change. If a refactor is large enough that
  it spans unrelated files, split it.

## Project-specific

- `MaterialList.from(...)` produces a `List<Pair<String, Int>>`; the
  sort order is chosen by the caller (`toSortedByCount()` for the
  detail screen, `toSortedList()` for pickers).
- `SchematicFormat` lives at `io.github.moxisuki.blockprint.core`
  (top-level, not nested under `model`). Same for `MaterialList`,
  `MinecraftVersions`, `BLOCKPRINT_CORE_VERSION`.
- `FormatCatalog` is the single source of truth for the mapping
  between `SchematicFormat` and user-facing labels. Don't hand-roll
  `when (format)` branches in UI code — call `FormatCatalog.from(format)`.
- `BLOCKPRINT_CORE_VERSION` is auto-generated at compile time; do not
  edit it manually.
