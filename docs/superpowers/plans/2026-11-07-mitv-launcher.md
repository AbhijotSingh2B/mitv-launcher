# Mi TV Custom Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A reel-style home screen app for a Mi TV on Android 9. It has one row of themed tiles that open apps or switch HDMI inputs.

**Architecture:** A plain-Kotlin `core` package (config, catalogues, tile resolution, launching) sits behind one `HomeController` interface. A Compose for TV `ui` package is the only screen layer, so a WebView UI could replace it later. Tiles and themes live in `assets/tiles.json` and `assets/themes.json`.

**Tech Stack:** Kotlin, Jetpack Compose, `androidx.tv:tv-material`, kotlinx.serialization-json, JUnit 4, Gradle (Android Gradle Plugin 9.x), ADB.

**Spec:** `docs/superpowers/specs/2026-11-07-mitv-launcher-design.md`

## Global Constraints

- Application id and package: `dev.tvlauncher`. App label: `Home`. Setup screen label: `Home Setup`.
- `minSdk 28` (the TV is Android 9, API 28). `compileSdk` and `targetSdk` use whatever Android Studio's wizard generates.
- Versions: use what Android Studio's New Project wizard generates. At research time (2026-11-07) that was AGP 9.0.x, Gradle 9.1+, JDK 17 (bundled with Studio), Compose BOM `2026.09.00` and Kotlin 2.4.x. Add `androidx.tv:tv-material:1.1.0`.
- No permissions except those named in a task. Never `QUERY_ALL_PACKAGES`. Use a `<queries>` block for the `LEANBACK_LAUNCHER` intent instead.
- The app never disables or changes any other package. ADB clean-up is documented only, never automated.
- No blur effects. No bitmaps larger than the screen. Patterns are drawn with Canvas.
- Background cross-fade: 400 ms. Focused tile scale: 1.08. Icon size: 256 px.
- User-visible copy, exactly: `Press OK to open`, `Press OK to switch`, `Switching to %s`, `Not available on this TV`, `Couldn't open %s`, `All apps`, `Settings`, `Use as home screen`.

## Review Focus

1. **An app in `tiles.json` that isn't installed, or gets uninstalled later.** Expect: its tile disappears on the next resume, and the app doesn't crash. (Task 4 test `missingAppIsHidden`, Task 6 re-resolves in `onResume`.)
2. **`tiles.json` is malformed, or names a theme that doesn't exist.** Expect: the app falls back to built-in defaults or the `default` theme and never shows an empty or crashed home screen. (Task 2 tests `malformedJsonFallsBackToDefaults`, `unknownThemeUsesDefault`.)
3. **The HDMI input list is empty or labelled differently on this Xiaomi firmware.** Expect: the tile is dimmed with `Not available on this TV`, and the next ports are matched by position. (Task 3 tests on `matchHdmiInput`, Task 4 test `missingHdmiIsUnavailable`.)
4. **Back pressed on the home screen, or Home pressed while already home.** Expect: Back does nothing. Home moves focus back to the first tile and never drops into PatchWall. (Task 6, manual check on the TV.)
5. **Fast D-pad presses during the 400 ms cross-fade.** Expect: focus follows the last press, the background ends on the focused tile's theme, and nothing stutters badly. (Task 5 keys the cross-fade on the theme id. Task 6 checks this manually on the TV.)

---

### Task 0: Tools, TV access and a probe on the real TV

No app code. The goal is to learn this TV's facts before writing code that depends on them.

**Files:**
- Create: `docs/device-notes.md`

- [ ] **Step 1 (user):** Install Android Studio (latest stable) and, in it, Android SDK Platform-Tools. Check: `adb version` prints a version.
- [ ] **Step 2 (user, on the TV):** Settings → Device Preferences → About, press **Build** 7 times. Then Developer options → turn on **USB debugging** (and **Network debugging** if listed). Note the TV's IP address from Network settings.
- [ ] **Step 3:** Run `adb connect <tv-ip>:5555`, then on the TV tick **Always allow** → **Allow**. Expected: `adb devices` shows `<tv-ip>:5555  device`.
- [ ] **Step 4: List launchers and HDMI inputs.**
  Run: `adb shell pm list packages | grep -iE "launcher|tvhome|android.tv"` and `adb shell dumpsys tv_input | grep -iE "inputId|type|label|passthrough"`
  Record every input id, its type and its label in `docs/device-notes.md`.
- [ ] **Step 5: Probe HDMI switching.** For one HDMI id from Step 4, run:
  `adb shell am start -a android.intent.action.VIEW -d "content://android.media.tv/passthrough/<url-encoded-input-id>"`
  Expected: the TV switches to that HDMI input. Record the result. If it doesn't switch, also try `adb shell am start -n com.android.tv/.TvActivity` and record what happens. **If neither works, stop and bring the result back before Task 3.**
- [ ] **Step 6: Probe the Home key.** Run `adb logcat -c; adb logcat | grep -iE "KEYCODE_(HOME|ALL_APPS)|keycode=3|keycode=284"` and press the remote's Home button. Record which key code appears. `KEYCODE_HOME` means Task 9 is skipped.
- [ ] **Step 7:** `git init`, add a `.gitignore` for Android (`/build`, `/.gradle`, `local.properties`, `.idea`, `*.apk`), then commit the docs:
```bash
git add .gitignore docs/
git commit -m "docs: launcher spec, plan and Mi TV device notes"
```

---

### Task 1: Project skeleton that builds and installs

**Files:**
- Create: Android Studio project at the repo root (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`)
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/dev/tvlauncher/ui/setup/SetupActivity.kt` (placeholder that shows the app version text)
- Create: `app/src/main/res/drawable/banner.xml` (320×180 TV banner: a dark rectangle with the word "Home")
- Test: `app/src/test/java/dev/tvlauncher/SmokeTest.kt`

**Interfaces:**
- Produces: a module `app` with package `dev.tvlauncher` and the Gradle tasks `testDebugUnitTest` and `installDebug`.

- [ ] **Step 1:** In Android Studio, create a new project ("Empty Activity", Kotlin, Gradle Kotlin DSL) with package `dev.tvlauncher` and minimum SDK API 28. Add `androidx.tv:tv-material:1.1.0` and `org.jetbrains.kotlinx:kotlinx-serialization-json` (with the matching `kotlin("plugin.serialization")`) to the version catalog and `app`.
- [ ] **Step 2: Manifest.** Add `uses-feature android.software.leanback required=false` and `android.hardware.touchscreen required=false`, plus `android:banner="@drawable/banner"`. `SetupActivity` gets `MAIN` + `LAUNCHER` + `LEANBACK_LAUNCHER`, a landscape screen orientation and the label `Home Setup`. Add `<queries><intent><action MAIN/><category LEANBACK_LAUNCHER/></intent></queries>`.
- [ ] **Step 3: Write the smoke test** `SmokeTest.packageIsCorrect`, asserting `"dev.tvlauncher" == BuildConfig.APPLICATION_ID`. Enable `buildFeatures { buildConfig = true }`.
- [ ] **Step 4:** Run `./gradlew testDebugUnitTest`. Expected: `BUILD SUCCESSFUL`, 1 test passed.
- [ ] **Step 5:** Run `./gradlew installDebug` with the TV connected. Expected: "Home Setup" appears in the TV's app list and opens.
- [ ] **Step 6: Commit** `git commit -am "feat: TV app skeleton"` (add the new files first).

---

### Task 2: Tile and theme config

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/core/config/Config.kt`
- Create: `app/src/main/assets/tiles.json`, `app/src/main/assets/themes.json`
- Test: `app/src/test/java/dev/tvlauncher/core/config/ConfigTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface TileSpec { val theme: String; val label: String? }` with `data class AppTileSpec(val packageName: String, override val label: String?, override val theme: String)`, `data class HdmiTileSpec(val port: Int, override val label: String?, override val theme: String)`, `data class AllAppsTileSpec(override val theme: String)` (label fixed `All apps`), `data class SettingsTileSpec(override val theme: String)` (label fixed `Settings`).
  - `enum class Pattern { NONE, GRID, DOTS, RINGS, DIGITS }`
  - `data class ThemeSpec(val id: String, val top: Long, val bottom: Long, val accent: Long, val pattern: Pattern)`. The colours are ARGB longs parsed from `"#RRGGBB"`.
  - `object ConfigParser { fun tiles(json: String): List<TileSpec>; fun themes(json: String): Map<String, ThemeSpec>; val DEFAULT_TILES: List<TileSpec>; val DEFAULT_THEME: ThemeSpec }`
  - `fun Map<String, ThemeSpec>.themeFor(id: String): ThemeSpec`, which returns the matching theme or `DEFAULT_THEME`.

- [ ] **Step 1: Write the failing tests** in `ConfigTest`:
  - `parsesAllFourTileTypes`: JSON with `app`, `hdmi`, `allApps` and `settings` entries gives four specs of the right classes, in that order, with `packageName` and `port` filled in.
  - `unknownTileTypeIsSkipped`: one `{"type":"weather"}` entry among two valid ones gives 2 specs.
  - `malformedJsonFallsBackToDefaults`: `tiles("{not json")` equals `DEFAULT_TILES`.
  - `parsesThemeColoursAndPattern`: `{"id":"red","top":"#C8102E","bottom":"#7A0019","accent":"#FFFFFF","pattern":"dots"}` gives `top == 0xFFC8102E`, `pattern == Pattern.DOTS`.
  - `unknownThemeUsesDefault`: `themes(validJson).themeFor("nope") == DEFAULT_THEME`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests "*ConfigTest*"`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement** `Config.kt` with kotlinx.serialization `Json { ignoreUnknownKeys = true }`. Read the entries as `JsonObject` and switch on `"type"`. Catch `SerializationException` and `IllegalArgumentException` and fall back. `DEFAULT_TILES = [AllAppsTileSpec("default"), SettingsTileSpec("default")]`. `DEFAULT_THEME = ThemeSpec("default", 0xFF1E1B2E, 0xFF0B0A12, 0xFFFFFFFF, Pattern.DIGITS)`.
- [ ] **Step 4: Write the assets.** `tiles.json`: YouTube (`com.google.android.youtube.tv`, theme `red`), Netflix (`com.netflix.ninja`, theme `dark`), Prime Video (`com.amazon.amazonvideo.livingroom`, theme `blue`), HDMI port 1 (label `HDMI 1`, theme `purple`), All apps (theme `grey`), Settings (theme `grid`). `themes.json` defines `red`, `dark`, `blue`, `purple`, `grey`, `grid`, `default`. The user edits both later.
- [ ] **Step 5:** Run the tests again. Expected: 5 passed.
- [ ] **Step 6: Commit** `feat: tile and theme config with safe fallbacks`.

---

### Task 3: App and HDMI catalogues

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/core/catalog/Catalogs.kt` (interfaces and data classes, plus `matchHdmiInput`)
- Create: `app/src/main/java/dev/tvlauncher/core/catalog/AndroidAppCatalog.kt`
- Create: `app/src/main/java/dev/tvlauncher/core/catalog/AndroidInputCatalog.kt`
- Test: `app/src/test/java/dev/tvlauncher/core/catalog/HdmiMatchTest.kt`

**Interfaces:**
- Produces:
  - `data class InstalledApp(val packageName: String, val label: String)`
  - `interface AppCatalog { fun installedTvApps(): List<InstalledApp>; fun icon(packageName: String): android.graphics.Bitmap? }`
  - `data class HdmiInput(val id: String, val label: String)`
  - `interface InputCatalog { fun hdmiInputs(): List<HdmiInput> }`
  - `fun matchHdmiInput(port: Int, inputs: List<HdmiInput>): HdmiInput?`

- [ ] **Step 1: Write the failing tests** in `HdmiMatchTest`:
  - `matchesByLabelIgnoringSpaceAndCase`: inputs `[("b","HDMI 2"),("a","hdmi1")]`, port 1 → id `a`.
  - `fallsBackToNthSortedById`: inputs `[("x2","Game"),("x1","Box")]` (no "hdmi" labels), port 2 → id `x2`.
  - `returnsNullWhenPortOutOfRange`: port 3 with 2 inputs → `null`.
  - `returnsNullForEmptyList`: port 1, `emptyList()` → `null`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests "*HdmiMatchTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement** `matchHdmiInput` as the spec says: compare `label.replace(" ","").lowercase()` with `"hdmi$port"`. Otherwise use `inputs.sortedBy { it.id }.getOrNull(port - 1)`.
- [ ] **Step 4: Implement the Android adapters.**
  - `AndroidAppCatalog(context)`: `queryIntentActivities(Intent(MAIN).addCategory(LEANBACK_LAUNCHER), 0)`, distinct by package, excluding `dev.tvlauncher`, sorted by lowercase label. `icon()` loads the app's banner or icon with `getResourcesForApplication(pkg).getDrawableForDensity(res, DENSITY_XXXHIGH, null)`, falls back to `loadIcon`, and draws it to a 256×256 bitmap. Results are cached in an `LruCache<String, Bitmap>` of 24 entries.
  - `AndroidInputCatalog(context)`: `TvInputManager.tvInputList.filter { it.isPassthroughInput && it.type == TvInputInfo.TYPE_HDMI }`, mapped to `HdmiInput(id, loadCustomLabel ?: loadLabel ?: id)`. Wrap it in `runCatching`, so an empty list comes back on failure.
- [ ] **Step 5:** Run the tests. Expected: 4 passed.
- [ ] **Step 6: Check on the TV.** Add a temporary debug line to `SetupActivity` listing `hdmiInputs()`, then `installDebug`. Expected: the same ids as Task 0, Step 4. If the list is empty but Task 0 found inputs, add `<uses-permission android:name="android.permission.READ_TV_LISTINGS"/>`, reinstall and re-check, and record which one was needed in `docs/device-notes.md`. Remove the debug line.
- [ ] **Step 7: Commit** `feat: app and HDMI input catalogues`.

---

### Task 4: HomeController (the shared boundary both UIs use)

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/core/home/HomeController.kt`
- Create: `app/src/main/java/dev/tvlauncher/core/home/AndroidActions.kt`
- Test: `app/src/test/java/dev/tvlauncher/core/home/HomeControllerTest.kt` (with fakes for `AppCatalog`, `InputCatalog`, `Actions`)

**Interfaces:**
- Consumes: `TileSpec`, `ThemeSpec`, `themeFor` (Task 2). `AppCatalog`, `InputCatalog`, `matchHdmiInput` (Task 3).
- Produces:
  - `sealed interface Target { data class App(val packageName: String) : Target; data class Hdmi(val inputId: String) : Target; data object AllApps : Target; data object Settings : Target }`
  - `data class HomeTile(val key: String, val label: String, val hint: String, val theme: ThemeSpec, val target: Target?, val available: Boolean)`. `target` is null when the tile isn't available. `key` is stable (`app:<pkg>`, `hdmi:<port>`, `allApps`, `settings`).
  - `sealed interface LaunchResult { data object Opened : LaunchResult; data object ShowAllApps : LaunchResult; data class Failed(val message: String) : LaunchResult }`
  - `interface Actions { fun openApp(packageName: String): Boolean; fun switchInput(inputId: String): Boolean; fun openSettings(): Boolean }`
  - `interface HomeController { fun tiles(): List<HomeTile>; fun allApps(): List<InstalledApp>; fun open(tile: HomeTile): LaunchResult; fun openApp(app: InstalledApp): LaunchResult }`
  - `class DefaultHomeController(specs: List<TileSpec>, themes: Map<String, ThemeSpec>, apps: AppCatalog, inputs: InputCatalog, actions: Actions) : HomeController`
  - `class AndroidActions(context: Context) : Actions`

- [ ] **Step 1: Write the failing tests** in `HomeControllerTest`:
  - `missingAppIsHidden`: a spec for `com.missing`, not in the fake catalogue → no tile with key `app:com.missing`.
  - `appTileUsesInstalledLabelWhenNoneGiven`: the fake has `("com.yt","YouTube")` → label `YouTube`, hint `Press OK to open`.
  - `missingHdmiIsUnavailable`: HDMI port 2, the fake has 1 input → tile `available == false`, hint `Not available on this TV`, `target == null`.
  - `hdmiTileHasSwitchHint`: port 1 matched → hint `Press OK to switch`, target `Hdmi(id)`.
  - `openFailedAppReturnsMessage`: `actions.openApp` returns false → `Failed("Couldn't open YouTube")`.
  - `allAppsTileReturnsShowAllApps`: `open(allAppsTile) == LaunchResult.ShowAllApps`, with no action called.
  - `unavailableTileIsNotOpened`: `open(unavailableHdmi)` returns `Failed("Couldn't open HDMI 2")`, and `switchInput` is never called.
  - `openAppFromAllApps`: `openApp(InstalledApp("com.yt","YouTube"))` calls `actions.openApp("com.yt")` and returns `Opened`.
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests "*HomeControllerTest*"`. Expected: FAIL.
- [ ] **Step 3: Implement `DefaultHomeController`.** `tiles()` re-reads the catalogues on every call, which is cheap, so uninstalls are picked up on resume. HDMI label is `spec.label ?: "HDMI $port"`. The `Settings` target maps to `actions.openSettings()`.
- [ ] **Step 4: Implement `AndroidActions`.**
  - `openApp`: `getLeanbackLaunchIntentForPackage ?: getLaunchIntentForPackage`, with `FLAG_ACTIVITY_NEW_TASK`.
  - `switchInput`: `Intent(ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id))`. If Task 0 showed only `com.android.tv/.TvActivity` works, start that with the same data URI instead.
  - `openSettings`: `Settings.ACTION_SETTINGS`.
  - All three catch `ActivityNotFoundException` and `SecurityException` and return false.
- [ ] **Step 5:** Run all unit tests. Expected: every test passes.
- [ ] **Step 6: Commit** `feat: HomeController boundary and Android actions`.

---

### Task 5: Themed background

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/ui/theme/ThemedBackground.kt`
- Create: `app/src/main/java/dev/tvlauncher/ui/theme/Patterns.kt`

**Interfaces:**
- Consumes: `ThemeSpec`, `Pattern` (Task 2).
- Produces: `@Composable fun ThemedBackground(theme: ThemeSpec, modifier: Modifier = Modifier)`, and `fun DrawScope.drawPattern(pattern: Pattern, color: Color)`.

- [ ] **Step 1: Implement `ThemedBackground`.** Use `Crossfade(targetState = theme, animationSpec = tween(400), label = "bg")`, keyed on `theme.id`, so fast focus moves settle on the last theme. Each frame draws a vertical gradient `top → bottom` plus `drawPattern(pattern, accent.copy(alpha = 0.08f))`.
- [ ] **Step 2: Implement `drawPattern`.**
  - `GRID`: lines every 48 dp.
  - `DOTS`: 3 dp circles every 40 dp.
  - `RINGS`: 5 concentric circles centred at 75% width and 35% height.
  - `DIGITS`: a fixed seeded grid of single digits (seed 42), drawn in the right 55% of the screen with `drawText` and a `TextMeasurer`. Cache the measured layouts with `remember` so they aren't rebuilt every frame.
  - `NONE`: draws nothing.
- [ ] **Step 3: Add `@Preview` functions** for all five patterns. Check each in Android Studio's preview at 1920×1080. Expected: each pattern is visible but faint, and nothing is clipped.
- [ ] **Step 4: Commit** `feat: themed background with cross-fade and drawn patterns`.

---

### Task 6: Home screen

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/ui/home/HomeActivity.kt`
- Create: `app/src/main/java/dev/tvlauncher/ui/home/HomeScreen.kt` (layout: background, title and hint, tile row, clock, overlay)
- Create: `app/src/main/java/dev/tvlauncher/ui/home/TileCard.kt`
- Create: `app/src/main/java/dev/tvlauncher/ui/home/Clock.kt`
- Create: `app/src/main/java/dev/tvlauncher/App.kt` (an `Application` that builds one `DefaultHomeController` from the assets, held as `val controller`)
- Modify: `app/src/main/AndroidManifest.xml` (add `HomeActivity` and the HOME alias)

**Interfaces:**
- Consumes: `HomeController`, `HomeTile`, `LaunchResult` (Task 4). `ThemedBackground` (Task 5). `AppCatalog.icon` (Task 3).
- Produces: `@Composable fun HomeScreen(controller: HomeController, icon: (String) -> ImageBitmap?, onShowAllApps: () -> Unit)`. Also the manifest alias `dev.tvlauncher.HomeAlias`, which Task 8 toggles.

- [ ] **Step 1: Manifest.**
  - `HomeActivity` has `exported=false`, `launchMode=singleTask`, `stateNotNeeded=true`, `excludeFromRecents=true` and a landscape orientation.
  - `activity-alias` `.HomeAlias` targets it, with `enabled=false`, `exported=true` and an intent filter of `MAIN` + `HOME` + `DEFAULT`. This is the pattern from couch-tv (MIT). Credit it in a code comment.
- [ ] **Step 2: Lay out `HomeScreen`.**
  - `ThemedBackground` uses the focused tile's theme.
  - Title: 56 sp, bold, white, in the left 8% gutter at 40% height. Hint: 16 sp, at 70% white opacity.
  - Tile row: tiles are 200 dp square with 24 dp gaps, at 62% height, in a `LazyRow`.
  - `Clock`: top-right, 20 sp. It follows `DateFormat.is24HourFormat` and updates on `ACTION_TIME_TICK`.
- [ ] **Step 3: Build `TileCard`.**
  - Use a tv-material `Card` with `scale = CardDefaults.scale(focusedScale = 1.08f)`. Focused: white container, dark label. Not focused: `Color.Black.copy(alpha = 0.35f)`, white label.
  - Content: the app icon (from `icon(pkg)`), or a built-in vector for HDMI, All apps and Settings. The label sits at the bottom.
  - Unavailable tiles are drawn at 40% opacity but can still be focused.
- [ ] **Step 4: Handle opening.**
  - OK on a tile: `Opened` shows a `Switching to %s` overlay over the background, which clears in `onResume`. `ShowAllApps` calls `onShowAllApps`. `Failed` shows its message at the bottom for 3 s.
  - The focused key is stored in `rememberSaveable`, so focus returns to the same tile.
- [ ] **Step 5: Handle keys and the window.** In `HomeActivity`:
  - Back does nothing (it's consumed).
  - Long-pressing Menu starts `SetupActivity`.
  - `onNewIntent` (Home pressed while home) moves focus to the first tile.
  - `onResume` re-reads `controller.tiles()`.
  - Hide the system bars with the API-28 `systemUiVisibility` immersive flags.
- [ ] **Step 6: Check on the TV.** Add an `Open home now` button to `SetupActivity` that starts `HomeActivity` (Task 8 keeps it). Run `./gradlew installDebug`, open **Home Setup** and press the button. Checklist:
  - [ ] Moving focus cross-fades the background.
  - [ ] Pressing left and right 10 times quickly ends on the right theme.
  - [ ] An app opens.
  - [ ] HDMI 1 switches.
  - [ ] Back from the app returns to the same tile.
  - [ ] Back on home does nothing.
  - [ ] Long-pressing Menu opens Setup.
  - [ ] The clock is correct.
  - [ ] Performance budget: run `adb shell am start -W -n dev.tvlauncher/.ui.setup.SetupActivity`, open home, then come back from an app. The home screen should be drawn in under 1 s on a warm start, and the background should settle in under 450 ms with no visible stutter. Record the numbers in `docs/device-notes.md`.
- [ ] **Step 7: Commit** `feat: reel-style home screen`.

---

### Task 7: All apps screen

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/ui/apps/AllAppsScreen.kt`
- Modify: `app/src/main/java/dev/tvlauncher/ui/home/HomeActivity.kt` (a simple `showAllApps` state that swaps the screen. Back returns to home.)

**Interfaces:**
- Consumes: `HomeController.allApps()`, `HomeController.openApp(app)` (Task 4) and `AppCatalog.icon` (Task 3).
- Produces: `@Composable fun AllAppsScreen(apps: List<InstalledApp>, icon: (String) -> ImageBitmap?, onOpen: (InstalledApp) -> Unit, onBack: () -> Unit)`

- [ ] **Step 1: Build the grid.** A `LazyVerticalGrid` with 6 columns of 160×90 dp app cards, focused scale 1.08, on a dark default theme background with the title `All apps`. Back calls `onBack`.
- [ ] **Step 2:** Icons load lazily, per visible item, through `icon(pkg)`. The Task 3 cache stops reloads.
- [ ] **Step 3: Check on the TV.**
  - [ ] Every TV app is listed, including PatchWall, so the user can always get back to it.
  - [ ] Scrolling stays smooth.
  - [ ] Back returns home with focus on All apps.
- [ ] **Step 4: Commit** `feat: all apps screen`.

---

### Task 8: Setup screen and the home-screen switch

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/core/home/HomeSwitch.kt`
- Modify: `app/src/main/java/dev/tvlauncher/ui/setup/SetupActivity.kt`
- Create: `docs/SETUP-ON-TV.md`

**Interfaces:**
- Produces: `class HomeSwitch(context: Context) { var enabled: Boolean }`. Its getter and setter use `PackageManager.get/setComponentEnabledSetting` on `ComponentName(context, "dev.tvlauncher.HomeAlias")` with `DONT_KILL_APP`.

- [ ] **Step 1: Implement `HomeSwitch`.**
- [ ] **Step 2: Build the Setup screen** (Compose, tv-material).
  - A `Use as home screen` switch bound to `HomeSwitch.enabled`.
  - Below it, three numbered steps of text: turn off PatchWall's "Set PatchWall as default launcher" toggle (Settings → Device Preferences → PatchWall), press Home, and choose **Home** with "Always".
  - Keep the `Open home now` button from Task 6.
- [ ] **Step 3: Write `docs/SETUP-ON-TV.md`.** Cover:
  - installing (`adb install -r app-debug.apk`)
  - the three steps above
  - how to undo: turn the switch off, turn PatchWall's toggle back on, `adb uninstall dev.tvlauncher`
  - an **optional, manual** clean-up section with the exact `pm disable-user --user 0 <pkg>` and `pm enable <pkg>` pair for each PatchWall ad package found in Task 0, and a warning never to disable `com.mitv.tvhome.atv` itself.
- [ ] **Step 4: Check on the TV.**
  - [ ] Turn the switch on, follow the steps, and press Home. Expected: the custom home appears.
  - [ ] Restart the TV. Expected: it boots into the custom home.
  - [ ] Turn the switch off and press Home. Expected: PatchWall is back.
- [ ] **Step 5: Commit** `feat: setup screen with safe home-screen switch`.

---

### Task 9 (only if Task 0, Step 6 did not show `KEYCODE_HOME`): Catch the Home key

**Files:**
- Create: `app/src/main/java/dev/tvlauncher/core/home/HomeKeyService.kt`
- Create: `app/src/main/res/xml/home_key_service.xml`
- Modify: `AndroidManifest.xml`, `SetupActivity.kt`, `docs/SETUP-ON-TV.md`

**Interfaces:**
- Produces: `class HomeKeyService : AccessibilityService`. It listens for key events only (`flagRequestFilterKeyEvents`, no window or content events). On key-down of the code recorded in Task 0, while `HomeSwitch.enabled`, it starts `HomeActivity` and consumes the key.

- [ ] **Step 1: Implement the service.** Its config sets `canRequestFilterKeyEvents="true"` and `accessibilityEventTypes=""`. The description string says it only watches the Home button.
- [ ] **Step 2:** Add a Setup row: `Fix the Home button`, which opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`, plus a line of instruction.
- [ ] **Step 3: Check on the TV.**
  - [ ] Turn on the service and press Home from inside YouTube. Expected: the custom home appears.
  - [ ] Other keys still work normally in apps.
- [ ] **Step 4: Commit** `feat: optional Home key catcher for Mi remotes`.
