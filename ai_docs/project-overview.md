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
- **ui/theme:** gradient background plus a pattern drawn with Canvas (`none`/`grid`/`dots`/`rings`/`digits`), with a 400 ms cross-fade
- **ui/home, ui/apps, ui/setup:** home screen, All apps grid, Setup screen with a "Use as home screen" switch

## Key decisions

- **Kotlin + Compose for TV**, chosen over a WebView UI for speed on a weak chip. The core layer stays UI-agnostic, so the UI can be swapped later.
- **The HOME alias is off by default**, so installing never takes over the Home button. The app never disables system packages.
- **The remote's Home sends the standard `KEYCODE_HOME`**, so being the default Home app is enough and no key-catcher service is needed.
- **No blur**, because real-time blur needs Android 12 or newer.

## Tech stack

AGP 9.5.1, Gradle 9.9.0, Kotlin 2.4.21, Compose BOM 2026.09.00, `androidx.tv:tv-material` 1.1.0, kotlinx-serialization-json 1.12.0, compileSdk 37, minSdk 28. Build with the JDK bundled with Android Studio.

## Docs

- Spec: [docs/superpowers/specs/2026-11-07-mitv-launcher-design.md](../docs/superpowers/specs/2026-11-07-mitv-launcher-design.md)
- Plan: [docs/superpowers/plans/2026-11-07-mitv-launcher.md](../docs/superpowers/plans/2026-11-07-mitv-launcher.md)
- Future work: [future-updates.md](future-updates.md)
- Known issues: [issue-tracker.md](issue-tracker.md)
