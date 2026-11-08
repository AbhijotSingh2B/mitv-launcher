# Mi TV Custom Launcher: Design

Date: 2026-11-07
Status: draft, awaiting review

## Goal

A custom home screen for a Xiaomi Mi TV in the style of the Snazzy Labs "Frame TV" reel. It has
one row of large tiles, and the whole screen takes on the selected tile's theme. Tiles can open
apps or switch to HDMI inputs.

## Target device (fixed)

- Model: MiTV-AXSO0 (Mi TV, 2021)
- Android TV 9 (API 28), with the PatchWall launcher (`com.mitv.tvhome.atv`)
- Modest hardware: assume a budget chip and 1-2 GB RAM.

## What the user sees

1. **Home screen.** Full-screen themed background, a large bold title for the focused tile, a hint
   line under it ("Press OK to open" for apps, "Press OK to switch" for HDMI), and one horizontal row of
   large square tiles. Clock in the top-right corner.
2. **Focus.** The focused tile is white, slightly larger (scale 1.08) and has a soft shadow. The other
   tiles are dark and semi-transparent. Moving focus cross-fades the background to the new tile's
   theme in 400 ms.
3. **Opening a tile.** A "Switching to <name>" overlay shows over the themed background, then the
   app or input opens. When the user comes back, the overlay is gone and focus is on the same tile.
4. **Built-in tiles.** "All apps" opens a grid of every TV app, so the user is never stuck without
   PatchWall. "Settings" opens the Android TV settings.
5. **Errors.** If an app is missing, its tile is hidden. If an HDMI input can't be found, the tile is
   shown dimmed with "Not available on this TV". If launching fails, a short message appears on
   the home screen, and the user stays there.

## Architecture

Shared core plus a swappable UI layer (decided in conversation):

- **core** (plain Kotlin, no UI): tile and theme config, the app catalogue, the HDMI input catalogue,
  launching, and the home-screen switch. The UI only talks to core through `HomeController`.
- **ui** (Compose for TV): screens only. A future WebView UI would call the same `HomeController`.
- **assets**: `tiles.json` (which tiles, their order and theme) and `themes.json` (colours, pattern,
  accent). Both UIs would read the same files.

## Tiles

Tiles come from `assets/tiles.json`, edited and redeployed. There is no on-TV editor in v1.
Tile types:

| type | fields | action |
|---|---|---|
| `app` | `package`, optional `label`, `theme` | open the app's leanback launch intent, else its launch intent |
| `hdmi` | `port` (1-based), optional `label`, `theme` | switch input by `ACTION_VIEW` on `TvContract.buildChannelUriForPassthroughInput(id)` |
| `allApps` | `theme` | open the All apps screen |
| `settings` | `theme` | open `Settings.ACTION_SETTINGS` |

HDMI port matching: among the TV's passthrough inputs of type `TYPE_HDMI`, the one whose label,
with spaces removed and in lowercase, equals `hdmi<port>`. Otherwise the `port`-th one, sorted by input id.

## Themes

Each theme has two gradient colours (top and bottom), an accent colour, and a pattern drawn in code
(`none`, `grid`, `dots`, `rings`, `digits`) at low opacity. Patterns are drawn with Canvas, not images,
to keep memory low. There is no blur, because real-time blur needs Android 12 or newer.

## Home button and PatchWall

- The HOME intent filter sits on an `activity-alias` that is **disabled by default**. Turning on "Use as
  home screen" in the Setup screen enables it. Installing the app never takes over Home.
- The user turns off PatchWall's own "Set PatchWall as default launcher" toggle and picks this app
  when Android asks which app to use for Home.
- The app never disables any system package. Any ADB clean-up is a manual, documented, reversible
  step.
- If the Mi remote's Home button does not reach the app (one report says it sends `KEYCODE_ALL_APPS`),
  an optional accessibility service that catches that key is added as a follow-up. This needs to be checked
  on the real TV first.

## Escape hatches

- Long-press Menu on the home screen opens Setup.
- The Setup screen is a normal app in the TV's app list. It can turn "Use as home screen" off.
- Back on the home screen does nothing, so it never drops into PatchWall by accident.

## Performance budget

- Focus move to background settled: under 450 ms, with no dropped frames visible on the TV.
- Home press to home screen drawn: under 1 s on a warm start.
- Icons are loaded once at 256 px, from each app's highest-density drawable, and cached in memory.

## v1 additions (user-directed, 2026-11-07)

The user directed these during the home-screen redesign. They are part of v1.

- **Look.** Poppins font (OFL). Vector brand marks for known apps (Simple Icons, CC0) and Material Symbols
  for the built-in tiles (see `licenses/NOTICE.md`). Glass tiles that turn white and lift (scale 1.1) on focus.
  Each tile has its own background motif. Pattern values added: `ribbons`, `viewfinder`, `tiles`.
  `doodles` now draws media glyphs and `rings` draws a reticle.
- **Night mode.** Darker, dimmer theme colours, a softer glow and quieter patterns. Auto runs from 22:00 to 06:00
  (the times can be changed), or it can be set to Always on or Off. A moon "Night" badge shows next to the
  clock while it is on.
- **Launcher Settings screen.** A short press of Menu on home opens it (a long press still opens Setup), and
  Setup has a button for it too. Settings: night mode, when night starts and ends, the night badge, "Open
  Cameras when the TV turns on" (off by default; it only acts on screen-on or within 5 minutes of boot),
  reduce motion, and a link to Home setup. Settings are stored in SharedPreferences.
- **Release build.** R8 minify and resource shrinking (needed for smooth focus moves on the 1 GB TV), signed
  with the local debug key for sideloading.

## Out of scope for v1

Phone remote, web server, voice and Claude assistant, watch history, on-TV tile editor, wallpapers
from the internet, the WebView UI.
