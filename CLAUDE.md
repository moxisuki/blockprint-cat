# BlockPrint Cat — Conventions

Android (Kotlin + Compose + Hilt) for browsing, previewing, and converting
Minecraft blueprint files.

## Rebuild direction

The app is being rebuilt from a clean architecture. Treat the old code as
reference material, not active app structure.

- New runtime root package: `io.github.moxisuki.blockprint.cat.app`.
- `MainActivity.kt` is the only Android entry file kept outside `app/*`.
- Legacy source lives under `reference/legacy-source/` and must not be
  imported from new runtime code.
- Do not re-enable old `ui.navigation.AppNavGraph`; port behavior into the
  new package structure when needed.
- Keep app startup thin: `MainActivity -> app.BlockPrintApp -> shell.AppShell`.
- Hilt is retained for the rebuilt app's dependency injection layer.
- `app.BlockPrintCatApplication` is the Hilt application entry. Activities
  that host Compose routes should use `@AndroidEntryPoint`, and route
  ViewModels should use `@HiltViewModel` with `hiltViewModel()`.

## Build & verify

```bash
./gradlew compileDebugKotlin   # after every code change
./gradlew testDebugUnitTest    # before commit
```

Compose Compiler Reports are on — unstable classes or restartable-but-
not-skippable composables must be fixed before shipping.

## i18n

User-facing strings live in `res/values*/strings.xml`.
- `res/values/strings.xml` — **source language: Simplified Chinese (zh)**
- `res/values-en/strings.xml` — English translation

Never hardcode Chinese or English in composables, ViewModels, or error
messages — use `stringResource(R.string.x)` or `context.getString`.
Add the key to **both `values/` and `values-en/` in the same commit**.
Other locales (ru, ja, ko, …) are managed via Crowdin — do not edit them locally.

## UI structure

New app code is organized under `app/*`:

```text
app/
├─ core/        # Miuix design system, navigation contracts, shared models/platform APIs
├─ shell/       # top-level Scaffold, global app chrome, global hosts
└─ feature/     # user-facing feature modules
```

The new design system is based on Miuix Compose. App-level theming lives in
`app/core/design/AppTheme.kt` and wraps runtime content in `MiuixTheme`.
Use Miuix components for app chrome and common surfaces (`TopAppBar`,
`Scaffold`, `FloatingNavigationBar`, `Card`, `Text`, etc.). Use
`miuix-icons`/`MiuixIcons` for product icons instead of Material icons when a
matching icon exists.
App theme selection lives in `app/core/design/AppThemeState.kt` and is exposed
through `LocalAppThemeState`. Feature Routes may read this local and translate
feature actions into app-level theme changes; Screens receive the selected
mode/color source as plain state and stay independent from `ThemeController`.
Theme selection is split into two axes: display mode (`System`, `Light`,
`Dark`) and color source (`Default`, `Monet`, `Custom`). Monet is a color
source, not another set of display modes. Custom palette selection stores a
seed color and passes it as `ThemeController.keyColor`. Map app-level choices
through `AppThemeMode.toColorSchemeMode(colorSource)` instead of referencing
Miuix `ColorSchemeMode` directly from feature packages. The custom palette
picker should be collapsed by default; keep its expansion state in the owning
feature state, currently `SettingsState`.
Keep the shell color stack consistent: top app bar and page background use
`MiuixTheme.colorScheme.surface`; floating bottom navigation and grouped
content surfaces use `surfaceContainer`; text on grouped surfaces uses
`onSurfaceContainer`.

Motion and interaction are part of the design system:
- App-level animation constants live in `app/core/design/AppMotion.kt`.
  Shell and feature routes should reuse these specs instead of inlining
  `tween(...)` durations.
- Top-level tab switching keeps each tab mounted and animates visibility with
  `AppMotion.topLevelVisibilitySpec()`: a perceptible Miuix-aligned fade/scale
  with no push/dim transition.
- Child navigation uses `AppMotion.contentForwardTransition()` for pushes and
  `AppMotion.contentBackTransition()` for pops. App bar back buttons and system
  back must both call the same navigator pop path so the animation stays
  identical.
- Scaffold top bar shape changes are animated with `AnimatedContent` and
  `AppMotion.appBarTransition(...)`; do not hand-roll per-page appbar
  transitions.
- `MiuixTheme` already provides the default Miuix indication and overscroll
  factory. Standard scroll containers should rely on that first.
- Use `Modifier.appScrollEndHaptic()` only on meaningful vertical scroll
  containers. Do not attach haptics to static layouts or every small surface;
  blank scaffold pages should stay simple until they have real scrolling
  content.
- Prefer Miuix components' built-in feedback. For clickable cards, use Miuix
  `Card(..., pressFeedbackType = AppInteraction.DefaultPressFeedback, onClick = ...)`.
  Reserve `ProminentPressFeedback` for large primary touch targets where tilt
  helps communicate depth.
- Avoid custom per-screen motion unless the interaction has a product reason.
  Add reusable rules to `AppMotion`/`AppInteraction` first.

Feature packages follow the Home template:

```text
feature/<name>/
├─ <Name>Route.kt       # stateful boundary: ViewModel, effects, navigation callbacks
├─ <Name>Screen.kt      # stateless rendering only
├─ <Name>State.kt       # immutable UI state
├─ <Name>Action.kt      # user/lifecycle actions
└─ <Name>ViewModel.kt   # action -> state, later use cases/repositories
```

Avoid large single files. When a screen passes ~600 lines, split it into
focused siblings in the same feature package. Pure rendering components
take everything as parameters; pre-compute derived data in the Route or
ViewModel and pass it down.

About page dependency versions are generated from the Gradle version catalog
into `BuildConfig` fields in `app/build.gradle.kts`, then exposed through
`AboutState`. When adding a dependency that should appear in About, add the
version field in Gradle and append one `AboutLibrary` entry; do not hardcode
version strings in composables.

Shared network plumbing lives in `app/core/network`. Keep this layer small:
`AppHttpClient` wraps OkHttp and returns `AppNetworkResult`, while endpoint
URLs, JSON parsing, and business models stay in feature-owned data sources
or repositories. Screens must never issue network calls directly. For example,
About's Hitokoto quote flows through
`AboutViewModel -> AboutRepository -> HitokotoRemoteDataSource -> AppHttpClient`.

## Compose stability

- Wrap event lambdas in `remember(...)` so children can skip recompose.
- Hoist expensive lookups once per Route/ViewModel, not per row.
- Don't `LaunchedEffect { state++ }` inside Coil `error` slots — the
  state write already triggers recompose.
- Flatten nested `scope.launch { scope.launch { ... } }`.

## Architecture

Use unidirectional data flow:

```text
User action -> ViewModel.onAction(action) -> StateFlow<FeatureState>
Route collects state -> Screen renders state
```

- `Route` is stateful and may own ViewModels, effects, navigation, and
  platform callbacks.
- `Screen` is stateless and must not import ViewModel, NavController, Context,
  repositories, or platform services.
- `State` classes should be `@Immutable` when used by Compose.
- `Action` is the single input channel back to the ViewModel.
- One-shot events will use a separate event flow when a feature needs
  navigation, snackbars, file pickers, or permissions.
- Navigation is centralized in `app/core/navigation/AppNavigator.kt` and
  rendered by Navigation3 in `shell/AppShell.kt`.
- Navigation keys under `AppRoute` must be `@Serializable` so Navigation3 can
  save and restore back stacks across configuration changes and process death.
- Top-level destinations implement `AppTopLevelRoute`. Current top-level tabs
  are Home and Settings; About is a child route in the Settings back stack.
  Each top-level tab owns
  an independent Navigation3 `rememberNavBackStack(...)`; switching tabs must
  not clear another tab's child stack.
- Top-level tab content is persistent. `AppShell` keeps every top-level
  `NavDisplay` composed and only animates visibility/z-order, so switching
  bottom tabs does not dispose and recreate routes, ViewModels, or screen
  content. Hidden tab displays must use an isolated
  `NavigationEventDispatcherOwner` so only the selected tab handles system
  back.
- Child routes must be mapped back to their owning top-level tab through
  `AppRoute.topLevelRoute()` so appbar title and bottom navigation selection
  remain stable when a feature pushes detail pages.
- `shell/AppScaffold.kt` owns the shared Miuix app chrome: top app bar,
  bottom navigation bar, surface color, and content padding.
- Top-level pages use Miuix `TopAppBar` with large title treatment. Child
  pages with back navigation use Miuix `SmallTopAppBar` so the back button sits
  in the compact title row instead of above the large title.
- Top-level appbar/bottom-bar metadata lives in
  `shell/AppTopLevelDestination.kt`. Add title, bottom label, icon, and route
  there before wiring a new tab into `AppShell`.
- Routes may call `AppNavigator`; Screens must only emit feature actions.
- Top-level destinations switch through `AppNavigator.navigateTopLevel(...)`
  from the bottom bar. Do not put temporary navigation buttons inside feature
  screens to model tab navigation.
- The default bottom tab chrome uses Miuix `FloatingNavigationBar`. Let the
  component handle navigation-bar insets and bottom spacing; avoid adding
  extra vertical padding around it unless a specific screen mode requires it.
- Navigation3 requires a root `NavigationEventDispatcherOwner`. `MainActivity`
  owns the dispatcher, exposes it via the ViewTree, and adapts
  `OnBackPressedDispatcher` into `NavigationEventInput` so system back stays
  centralized in the Navigation3 back stack.
- One-shot feature startup work should run from ViewModel initialization or an
  idempotent ViewModel action. Avoid using Route-level `LaunchedEffect` as the
  default data-loading entry unless it is keyed to a real external parameter.
- Repositories/use cases should be introduced behind interfaces under
  `app/core` or a dedicated new data package as features are migrated.

## blockprint-core

API reference lives in `blockprint-core/docs/` (not in this repo).
