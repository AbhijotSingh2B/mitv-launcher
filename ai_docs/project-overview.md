# Project Overview: Mi TV Custom Launcher

A custom home screen for a Xiaomi Mi TV, styled after the Snazzy Labs "Frame TV" reel. It has one row of large tiles, and the whole screen takes on the focused tile's theme. Tiles open apps or switch HDMI inputs.

## Target device

- **Model:** MiTV-AXSO0 (codename `dangal`), Android TV 9 (API 28), Amlogic chip, modest hardware
- **Stock launchers:** Google `com.google.android.tvlauncher` (current default Home) and PatchWall `com.mitv.tvhome.atv`
- **HDMI:** HDMI 1 is empty, HDMI 2 is the CCTV camera recorder, HDMI 3 is labelled "Computer"
- **ADB:** `adb connect 192.168.1.33:5555`
- Full probe results: [docs/device-notes.md](../docs/device-notes.md)

## Architecture

```
ui/      Compose for TV screens (swappable; a WebView UI could replace it)
  └─ talks only to → core/home/HomeController
core/    plain Kotlin: config, app and HDMI catalogues, launching
assets/  tiles.json (which tiles, in what order) and themes.json (colours, patterns)
```

- **core/config:** parses `tiles.json` and `themes.json`, with safe fallbacks
- **core/catalog:** installed TV apps (with sharp 256 px icons) and HDMI inputs (TvInputManager)
- **core/home:** `HomeController` resolves tiles and opens them. HDMI switches through `ACTION_VIEW` on `TvContract.buildChannelUriForPassthroughInput(id)`, which is confirmed working on the TV.
- **core/settings:** the launcher's own settings (SharedPreferences) and the pure "open Cameras when the TV turns on" decision
- **ui/theme:** gradient background drawn live on the GPU, plus a per-tile motif pre-rendered off the main thread into an alpha mask (`digits`, `doodles` media glyphs, `lines`, `rings` reticle, `ribbons`, `viewfinder`, `tiles`, `grid`, `dots`), with a 400 ms cross-fade. Poppins font, night-mode colours.
- **ui/home:** home screen: glass tiles with vector brand marks, title and hint, clock, night badge, "Switching to" overlay
- **ui/settings:** Launcher Settings (short press of Menu on home)
- **ui/apps, ui/setup:** All apps grid (Task 7), Setup screen with a "Use as home screen" switch (Task 8)

## What the home screen looks like now

- **Font:** Poppins (OFL), bundled.
- **Tiles:** 156 dp dark glass squares. The focused tile turns white (off-white at night), lifts to 1.1 scale with a soft shadow, and its logo takes the brand colour. Logos are real vector marks (Simple Icons for YouTube, Netflix and Prime Video; Material Symbols for Cameras, All apps and Settings).
- **Backgrounds:** each tile has its own: YouTube has red with media icons, Netflix has red light ribbons on black, Prime Video has blue flowing lines, Cameras has a purple camera viewfinder, All apps has a grey app grid, and Settings has a warm grey reticle.
- **Night mode:** automatic from 10 pm to 6 am, or Always on or Off. Colours, glow and patterns are dimmer. A moon "Night" badge shows next to the clock.
- **Launcher Settings:** night mode and its hours, the night badge, "Open Cameras when the TV turns on" (off by default), reduce motion, and a link to Home setup.
- **Third-party assets and licences:** [licenses/NOTICE.md](../licenses/NOTICE.md)

## Key decisions

- **Kotlin + Compose for TV**, chosen over a WebView UI for speed on a weak chip. The core layer stays UI-agnostic, so the UI can be swapped later.
- **The HOME alias is off by default**, so installing never takes over the Home button. The app never disables system packages.
- **The remote's Home sends the standard `KEYCODE_HOME`**, so being the default Home app is enough and no key-catcher service is needed.
- **No blur**, because real-time blur needs Android 12 or newer.
- **Install the release build** (`./gradlew assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk`). It's minified with R8 and signed with the local debug key. The debug build is several times slower on this TV.
- **Testing night mode:** `adb shell settings put global tvlauncher_night 1` forces night, `0` forces day, and `adb shell settings delete global tvlauncher_night` returns to auto.

## Tech stack

AGP 9.5.1, Gradle 9.9.0, Kotlin 2.4.21, Compose BOM 2026.09.00, `androidx.tv:tv-material` 1.1.0, kotlinx-serialization-json 1.12.0, compileSdk 37, minSdk 28. Build with the JDK bundled with Android Studio.

## Docs

- Spec: [docs/superpowers/specs/2026-11-07-mitv-launcher-design.md](../docs/superpowers/specs/2026-11-07-mitv-launcher-design.md)
- Plan: [docs/superpowers/plans/2026-11-07-mitv-launcher.md](../docs/superpowers/plans/2026-11-07-mitv-launcher.md)
- In progress: [in-progress.md](in-progress.md)
- Future work: [future-updates.md](future-updates.md)
- Known issues: [issue-tracker.md](issue-tracker.md)
