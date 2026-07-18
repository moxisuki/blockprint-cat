# UI Rebuild Notes

## Current Runtime Switch

- Runtime entry: `MainActivity.setContent`.
- New active root: `app.BlockPrintApp`.
- New architecture root package: `io.github.moxisuki.blockprint.cat.app`.
- Legacy source root: `reference/legacy-source`, kept for reference and excluded from Android source sets.
- `MainActivity` is the only file intentionally kept outside the new `app` package.
- New design system root: `app.core.design`, backed by Miuix `MiuixTheme`.
- App theme mode state is held by `AppThemeState` and provided through
  `LocalAppThemeState`. Settings currently demonstrates app-level theme changes
  by mapping feature actions to `AppThemeState.selectMode(...)`,
  `selectColorSource(...)`, and `selectSeedColor(...)` in `SettingsRoute`.
  Theme state has two axes: display mode (`System`, `Light`, `Dark`) and color
  source (`Default`, `Monet`, `Custom`). Monet is only a color source; custom
  palette stores a seed color and lets Miuix generate the full scheme through
  `ThemeController.keyColor`. The palette picker is collapsed by default and
  its expansion state belongs to `SettingsState`.
- Miuix visual language should be expressed with Miuix components first:
  `TopAppBar`, `Scaffold`, `FloatingNavigationBar`, `Card`, `Text`, and
  `MiuixIcons` from the separate `miuix-icons` artifact.
- Current color stack: shell/page background uses `surface`; grouped content
  and the floating bottom bar use `surfaceContainer`.
- Hilt is retained for dependency injection as the rebuilt app grows. The
  active Hilt root is `app.BlockPrintCatApplication`; Compose-hosting
  activities use `@AndroidEntryPoint`, and feature routes resolve
  `@HiltViewModel` instances with `hiltViewModel()`.
- AndroidX Startup and ProfileInstaller are explicit runtime dependencies.
  Startup owns the merged `InitializationProvider`; keeping it direct prevents
  provider/class drift when other AndroidX libraries contribute startup metadata.

## Legacy UI Map

- Main tabs: Home, Connection, Community, Tools, Settings.
- Home flow: local blueprint list, PC section, category filters, import entry, detail navigation.
- Connection flow: PC bridge state, paired/discovered devices, QR scanner, transfer progress.
- Community flow: MCS/CMS source switch, login web view, list/detail/download states.
- Tools flow: image-to-blueprint, text-to-blueprint, block paint, export/preview shared components.
- Settings flow: theme, language, cache/backup, community settings, about, changelog, terms.
- Preview/detail flow: blueprint detail, 3D preview, render asset manager, fullscreen preview controls.

## Rebuild Boundaries

- Treat everything under `reference/legacy-source` as read-only reference material.
- Rebuild app code under `app/*`, split into `core`, `shell`, and `feature` packages.
- Keep `MainActivity` thin: lifecycle entry only, no app state orchestration.
- Do not re-enable legacy `ui.navigation.AppNavGraph`; port behavior into the new package structure when needed.

## First Home Shell

- `app.BlockPrintApp` renders the new Miuix theme and `AppShell`.
- `AppShell` owns the Navigation3 graph for the current Home, Settings, and
  Settings-owned About pages.
- `AppScaffold` owns the Miuix top app bar and floating bottom navigation bar.
  The floating bar relies on Miuix's own bottom inset spacing, with no extra
  vertical wrapper padding. Top-level pages use large `TopAppBar`; child pages
  with back navigation use `SmallTopAppBar` so the back action remains in the
  compact title row.
- `AppTopLevelDestination` is the single scaffold metadata table for top-level
  tabs: route, appbar title, bottom label, and icon.
- `AppRoute` keys are serializable Navigation3 keys. `AppNavigator` uses one
  saved `rememberNavBackStack(...)` per top-level tab and a saveable selected
  tab id, so switching tabs preserves each tab's child stack.
- Top-level tab roots stay mounted. The shell renders each tab's own
  `NavDisplay` in a layered host and changes only visibility/z-order during
  bottom-tab switching, which avoids recreating Route/ViewModel/screen content.
  Hidden tabs are wrapped with an isolated `NavigationEventDispatcherOwner` so
  their retained back stacks do not intercept system back.
- Future child routes must update `AppRoute.topLevelRoute()` to point back to
  the owning tab; shell chrome derives title/selection from that top-level
  owner instead of assuming every route is a bottom tab.
- Root tab switching uses a perceptible fade/scale transition instead of
  Navigation3's default push/pop slide, because the slide/dim transition is
  intended for pushed child pages and feels wrong for bottom tabs.
- Motion is centralized in `app.core.design.AppMotion`. The shell currently
  keeps top-level tabs mounted and uses `AppMotion.topLevelVisibilitySpec()`
  to animate selected/unselected tab visibility. Child route pushes use
  `AppMotion.contentForwardTransition()` and pops use
  `AppMotion.contentBackTransition()`, so appbar back and system back share the
  same Navigation3 pop animation. Appbar shape changes are handled by
  `AppMotion.appBarTransition(...)` in `AppScaffold`.
- Interaction effects are centralized in `app.core.design.AppInteraction`.
  Miuix provides theme-level indication and overscroll, so feature screens
  should only opt into `Modifier.appScrollEndHaptic()` for meaningful scroll
  boundaries and should use Miuix component parameters for press feedback.
- Static cards stay passive. Clickable cards should normally use
  `AppInteraction.DefaultPressFeedback`; stronger tilt feedback is reserved
  for large, deliberate touch targets.
- `MainActivity` provides the Navigation3 `NavigationEventDispatcherOwner`
  through the ViewTree and adapts system back into the Navigation3 back stack.
- No old NavController is created.
- No old screen ViewModel is resolved.
- No old bridge/community event collectors are launched.
- No old import-preview sheet or ACTION_VIEW import handling is triggered.

## Feature Template

- `FeatureRoute`: stateful boundary. Owns ViewModel, collects state, handles one-shot effects and navigation callbacks.
- `FeatureScreen`: stateless rendering. Accepts state and callbacks, never imports ViewModel or platform services.
- `FeatureState`: immutable UI state for rendering.
- `FeatureAction`: user or lifecycle actions flowing back to the ViewModel.
- `FeatureViewModel`: translates actions into state changes and later delegates business work to repositories/use cases.
- Navigation: `AppShell` owns the Navigation3 graph and `AppNavigator` owns the back stack mutations.
- Top-level routes are selected from the shared bottom bar through `navigateTopLevel`.
- Top-level app chrome is configured in `shell/AppTopLevelDestination.kt`; feature screens must not create their own appbar or bottom navigation.
- Routes may depend on `AppNavigator`; Screens stay navigation-agnostic and emit actions only.
- Route-level startup effects are not the default loading mechanism. Prefer
  ViewModel `init` or idempotent actions so recomposition and tab switches do
  not accidentally restart work.

Current Home example:

- `app.feature.home.HomeRoute`
- `app.feature.home.HomeScreen`
- `app.feature.home.HomeState`
- `app.feature.home.HomeAction`
- `app.feature.home.HomeViewModel`
