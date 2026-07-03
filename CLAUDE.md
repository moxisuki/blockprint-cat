# BlockPrint Cat — Conventions

Android (Kotlin + Compose + Hilt + Room) for browsing, previewing, and
converting Minecraft blueprint files.

## Build & verify

```bash
./gradlew compileDebugKotlin   # after every code change
./gradlew testDebugUnitTest    # before commit
```

Compose Compiler Reports are on — unstable classes or restartable-but-
not-skippable composables must be fixed before shipping.

## i18n

User-facing strings live in `res/values*/strings.xml`
(default zh, plus `values-en/` and `values-zh-rCN/`). Never hardcode
Chinese or English in composables, ViewModels, or error messages — use
`stringResource(R.string.x)` or `context.getString`. Add the key to
all three locales in the same commit.

## UI structure

Avoid large single files. When a screen passes ~600 lines, split it
into focused siblings in the same `ui/<feature>/` package — one
composable per file, `internal` for cross-file helpers, `public` only
for package entry points. Pure rendering components take everything as
parameters; pre-compute derived data in the parent and pass it down.

## Compose stability

- Wrap event lambdas in `remember(...)` so children can skip recompose.
- Hoist expensive lookups (Hilt `EntryPointAccessors`, palette resolvers)
  once per screen, not per row.
- Don't `LaunchedEffect { state++ }` inside Coil `error` slots — the
  state write already triggers recompose.
- Flatten nested `scope.launch { scope.launch { ... } }`.

## Architecture

`@HiltViewModel` for screen state, `@Singleton` for cross-screen services.
`@EntryPoint` + `EntryPointAccessors.fromApplication(...)` is the escape
hatch for non-injected composables — prefer a real ViewModel first.

## blockprint-core

API reference lives in `blockprint-core/docs/` (not in this repo).
Check `blockprint-core/CHANGELOG.md` for renames when bumping.
