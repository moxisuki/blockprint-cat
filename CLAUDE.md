# BlockPrint Cat — Conventions

Android (Kotlin + Compose + Hilt + Room) for browsing, previewing, and
converting Minecraft blueprint files (.litematic, .schem, .nbt, .json).

## Build & verify

```bash
./gradlew compileDebugKotlin   # after every code change
./gradlew testDebugUnitTest    # before commit
```

Compose Compiler Reports are on — regressions in `compose_reports/`
(unstable classes, restartable-but-not-skippable) must be fixed.

## i18n

User-facing strings → `app/src/main/res/values*/strings.xml`
(`values/` default zh, `values-en/`, `values-zh-rCN/`). No hardcoded
Chinese or English in composables, ViewModels, or error messages.
`stringResource(R.string.x)` in Composables, `context.getString` elsewhere.
Add the key to all three files in the same commit.

## UI structure

Avoid large single files. When a screen passes ~600 lines, split into
focused siblings in the same `ui/<feature>/` package — see `ui/detail/`:

```
ui/detail/
  BlueprintDetailScreen.kt   # phone entry — state, dispatch, branch
  BlueprintDetailContent.kt  # pad entry — same logic, different chrome
  DetailRows.kt              # DetailRow / FormatRow / SectionCard
  MaterialSection.kt         # MaterialRow / MaterialIcon + resolver
  ConvertDialog.kt           # pure rendering
  NamespaceCard.kt           # one card, one file
  PreviewButton.kt           # preview-generate flow
  DetailViewModel.kt
```

Rules: one composable per file; `internal` for cross-file helpers;
`public` only for package entry points; pure components take everything
as parameters; pre-compute derived data in the parent and pass down.

## Compose stability

- Wrap event lambdas in `remember(...)` so children can skip recompose.
- Hoist expensive lookups (Hilt `EntryPointAccessors`, palette resolvers)
  once per screen — expose via `rememberXxx()` helpers in the shared
  sibling file. See `rememberIconIndexResolver()` in `MaterialSection.kt`.
- Don't `LaunchedEffect { state++ }` inside a Coil `error` slot — the
  state write already triggers recompose.
- Flatten nested `scope.launch { scope.launch { ... } }`.

## Architecture

- `@HiltViewModel` for screen state. `@Singleton` for cross-screen
  services (managers, storage, network).
- `@EntryPoint` + `EntryPointAccessors.fromApplication(...)` is the
  escape hatch for non-injected composables. Prefer a real
  `@HiltViewModel` first.

## blockprint-core

`gradle/libs.versions.toml` pins `blockprint = "1.0.0"` from `mavenLocal()`
(under the `io.github.moxisuki` group, configured in `settings.gradle.kts`).
Bumping versions: check `blockprint-core/CHANGELOG.md` for renames; the
companion docs `blockprint-core/docs/BLUEPRINT_API.md` and
`GLB_PIPELINE.md` are the canonical reference.

## Project-specific

- `SchematicFormat`, `MaterialList`, `MinecraftVersions`,
  `BLOCKPRINT_CORE_VERSION` all live at top-level
  `io.github.moxisuki.blockprint.core.*` (not under `model.*`).
- `FormatCatalog.from(format)` is the single source of truth for
  schematic-format display labels. Don't hand-roll `when (format)` in UI.
- `MaterialList.from(doc).toSortedByCount()` / `toSortedList()` — caller
  picks the sort order.
