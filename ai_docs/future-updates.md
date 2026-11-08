# Future Updates

Ideas agreed for after v1 (the current plan) is finished.

## Planned

| # | Feature | Notes |
|---|---|---|
| 1 | **Custom tile images** | Use your own image for each tile (device photos, logos), like the reel's device tiles, instead of app icons. Images will be sourced from the internet, so check the licences, then store them in `assets/`. |
| 4 | **"More" tab** | A tile at the end of the row that opens all apps. Replaces adding Hotstar and SonyLIV as separate tiles. |
| 5 | **Screensaver / Art Mode** | A slow photo slideshow after a few minutes of no input, like the reel's Art Mode. Implemented as an Android "Daydream" screensaver. |
| 6 | **Edit tiles on the TV** | Reorder, hide or add tiles with the remote, with no PC or reinstall needed. The biggest item, and the most useful long-term. |

## Done (pulled into v1 on 2026-11-07)

| Feature | Notes |
|---|---|
| **Time-of-day themes** | ✅ Night mode: automatic from 10 pm to 6 am (changeable), or Always on or Off. Moon badge next to the clock. |
| **Auto-open Cameras on startup** | ✅ A Launcher Settings option, off by default. Fires on screen-on or within 5 minutes of boot. Not yet tried on the TV. |
| **Launcher settings menu** | ✅ Short press of Menu on home. |

## Maybe

| Feature | Notes |
|---|---|
| **Cameras tile preview** | When the Cameras tile is focused, show a still image or icon as its background instead of a pattern. |
| **Weather next to the clock** | Needs internet and a free weather API. |

## Not planned

- Remember the last focused tile
- Phone remote, voice or AI assistant, watch history (see [couch-tv](https://github.com/Seagey/couch-tv) if these are ever wanted)
