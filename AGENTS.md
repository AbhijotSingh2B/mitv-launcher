# Guide for AI coding agents

This file tells an AI agent (Claude Code, Codex, Copilot, Cursor and others) how to change this launcher safely. Read it before editing anything. People reading on GitHub can skim it too.

## What this is

This is an Android TV home screen (launcher) written in Kotlin with Jetpack Compose for TV. It was built for a Xiaomi Mi TV running Android TV 9 (API 28) with 1 GB of RAM, and it works on most Android TVs. The UI is one row of large tiles over full-screen animated backgrounds, with thin shortcut pills under the row.

## Golden rules

1. **Start with config.** Most requests ("add Disney+", "change the YouTube colour", "reorder tiles") are edits to [`tiles.json`](app/src/main/assets/tiles.json) and [`themes.json`](app/src/main/assets/themes.json), not Kotlin.
2. **The UI only talks to `HomeController`.** Screens in `ui/` never call `PackageManager`, `TvInputManager` or `startActivity` for apps directly. They go through `core/home/HomeController.kt`. New behaviour goes into the controller first, with a unit test.
3. **Treat it as a slow, low-memory TV.**
   - Nothing allocates bitmaps or big objects per frame.
   - Patterns are drawn **once**, off the main thread, into a cached mask (`ui/theme/ThemedBackground.kt`). Never draw a pattern live in a draw lambda.
   - Animate only while something is visible, as the About light wave does.
4. **Everything works with a remote.** Every interactive thing must be focusable and reachable with the D-pad. Never rely on touch, hover or a text keyboard. The picker has its own on-screen keyboard because the TV's keyboard can be phone-only.
5. **Back never leaves home. Home returns home** (via the Home button service on TVs where Google TV Home wins the Home button). Don't break `HomeActivity`'s `onNewIntent` and saved-signal logic. See the comments there.
6. **Keep the core plain Kotlin.** `core/` has no Compose imports, so it stays unit-testable on the JVM.
7. **Credit third-party assets.** Every new logo, icon path or font gets a row in [`licenses/NOTICE.md`](licenses/NOTICE.md). Brand logos are trademarks: use them only to label that brand's own app.

## Project map

```
app/src/main/
  assets/tiles.json, themes.json     ← the home row and its themes (edit these first)
  java/dev/tvlauncher/
    App.kt                           ← wiring: catalog, controller, settings
    core/                            ← plain Kotlin, unit-tested
      config/Config.kt               ← JSON parsing, Pattern enum, safe fallbacks
      catalog/                       ← installed apps and HDMI inputs
      home/HomeController.kt         ← tiles, shortcuts, open/launch logic (the UI's only API)
      home/AndroidActions.kt         ← the real startActivity calls
      settings/LauncherSettings.kt   ← SharedPreferences: night mode, home row apps, shortcuts…
    system/                          ← Home button service and boot receiver
    ui/
      home/                          ← HomeScreen, TileCard, Marks (logos), ShortcutPills, chooser
      theme/                         ← ThemedBackground, Patterns, Motifs (per-tile art), night mode
      apps/ info/ settings/ setup/   ← All apps, About, Launcher settings + app picker, Setup
app/src/test/                        ← JUnit tests for core/ and the shipped JSON
```

## Recipes

### Add or reorder a tile
Edit `tiles.json`. The tile types are:
- `app` with `package`
- `hdmi` with `port`
- `allApps`
- `settings`
- `info`

Each tile can also have `label` and `theme`. An app that isn't installed is skipped. `AssetsTest` checks that every tile parses and that every theme it names exists.

Users can also add apps without code: **Launcher settings → Home row apps**, or **Shortcuts** for the pills.

### Give an app its own background colours
Add a theme to `themes.json` with `id`, `top` and `bottom` gradient colours (hex), `accent`, and `pattern`. Then point the tile's `theme` at it.

### Draw a new background pattern
1. Add a value to `enum class Pattern` in `core/config/Config.kt`. The build then fails in exactly two places, and those are the rest of the job.
2. Write `internal fun DrawScope.drawYourThing(color: Color)` in `ui/theme/Motifs.kt`. Use relative sizes (`size.width`, `size.height`) and a seeded `Random` so it never shifts.
3. In `ui/theme/Patterns.kt`:
   - Add a branch to `drawPattern` that calls your function.
   - Add a `PatternStyle` to `buildStyle`: its strength, how much it fades towards the title, and `fullColour = true` if it draws its own colours. `RIBBONS` (Netflix) and `PRISM` (Nuvio) use `fullColour`; the rest are accent-tinted masks.
4. Use it from `themes.json`, add its lowercase name to the README pattern list, and check it on a TV. `AssetsTest` fails if `themes.json` names a pattern that doesn't exist.

### Add a brand logo to a tile
Add a `Mark` (an SVG path in a 24-unit box) in `ui/home/Marks.kt`, map the package in `byPackage`, and credit the source in `licenses/NOTICE.md`.
- Parse paths with `androidx.core.graphics.PathParser`, not Compose's parser. Compose's mangles some logos.
- Set `gradient` for a brand gradient on focus, as Nuvio does.

### Add a new kind of tile (beyond app, hdmi, allApps, settings, info)
`TileSpec`, `Target` and `LaunchResult` are sealed, and every `when` over them is exhaustive. Add the new types and the compiler lists each place that needs a branch. The About tile (`info`) is the worked example to copy:
1. In `core/config/Config.kt`, add a `TileSpec` data class and a `"yourtype"` case in the tile parser. Add a parser test in `ConfigTest`.
2. In `core/home/HomeController.kt`:
   - Add a `Target`.
   - Map the spec in `buildTiles`.
   - Decide in `open()`: either launch through `Actions`, or return a new `LaunchResult` (for example `ShowYourScreen`) for the UI to handle.
   - Add a test in `HomeControllerTest`.
3. In the UI:
   - Give it an icon in `markFor` (`ui/home/Marks.kt`).
   - Handle any new `LaunchResult` in `HomeScreen`'s `onOpen`.
   - If it opens a screen of its own, show it from `HomeActivity`, as `AboutScreen` is shown, with a `BackHandler`.

### Add a launcher setting
1. Add a property with a safe default in `LauncherSettings.kt`.
2. Add a `SettingItem` row in `ui/settings/LauncherSettingsActivity.kt`.
3. Read it in Compose with `rememberSetting { it.yourSetting }`.

## Build, test, run

You need JDK 17 or newer. Android Studio's bundled JBR works: on Windows, set `JAVA_HOME` to `C:\Program Files\Android\Android Studio\jbr`.

```bash
./gradlew testDebugUnitTest      # must stay green
./gradlew assembleRelease        # minified build; much smoother on a TV than debug
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

Handy while testing:

```bash
adb shell settings put global tvlauncher_night 1   # force night mode (0 = day, delete = auto)
adb exec-out screencap -p > shot.png                 # see what the TV shows
```

## Before you call it done

- `./gradlew testDebugUnitTest` passes, and you added tests for new `core/` logic.
- `./gradlew assembleRelease` builds with no new warnings in files you touched.
- You checked the change on a TV or emulator with a screenshot: focus visible, nothing clipped at 960×540 dp, day and night.
- Docs match: the README (features, controls, pattern list) and `docs/SETUP-ON-TV.md` if setup changed.
- New third-party assets are credited in `licenses/NOTICE.md`.

## Don'ts

- Don't push to `main`, merge branches or change the GitHub default branch unless the repo owner asks. Work on a branch.

- Don't add heavy dependencies (image loaders, DI frameworks, analytics). Runtime dependencies are AndroidX only.
- Don't commit APKs, keystores or `local.properties`. Never capture or commit screenshots of an HDMI camera input (a home CCTV feed is private).
- Don't disable or uninstall any package, including the TV's stock launchers, from code or scripts (`pm disable`, `pm uninstall`). The Home button service exists so that's never needed.
- Don't let a fresh install take over Home. Anything that opens the launcher by itself (boot, the service) must sit behind an opt-in the user switched on.
- Don't read screen content in the accessibility service. It only needs key events and the class name of the window in front.
