# Issue Tracker

Known issues and deferred review findings. Update the status when one is fixed.

**Status key:** 🔴 open · 🟡 needs checking on the TV · 🟢 fixed

## Open

| # | Area | Issue | Status |
|---|---|---|---|
| 3 | Patterns (`drawPattern`) | Calling it without `digits` silently draws nothing for the DIGITS pattern. | 🔴 |
| 4 | Patterns | RINGS creates a new `Stroke` per circle on every frame, and digit glyphs may clip at the right and bottom edges. | 🔴 minor |
| 5 | Config (`Config.kt`) | Signed hex colours like `"-FFFFF"` are accepted, and HDMI port 0 or negative is accepted. | 🔴 minor |
| 6 | App catalogue | `installedTvApps()` and `allApps()` don't guard against exceptions. | 🔴 minor |
| 7 | HomeController | `runCatching` also swallows Errors (out of memory, stack overflow). Consider narrowing it to `Exception`. | 🔴 minor |
| 8 | Banner (`banner.xml`) | The hand-drawn "e" in "Home" is approximate and hasn't been checked visually. | 🟡 |
| 9 | Manifest | `allowBackup="true"`. Consider `false`. | 🔴 minor |
| 10 | Tests | Gaps: catalogue refresh between `tiles()` calls, failed Settings and HDMI paths, label override, and real-device HDMI labels (HDMI1/HDMI2/Computer). | 🔴 minor |
| 12 | Performance | Focus moves on the release build: median frame time about 19 ms (60 Hz needs under 16.7 ms), 95th percentile 73 ms. Smooth enough, but not perfect on the txlx chip. Profile further. | 🔴 |
| 13 | Home | The 6th tile (Settings) is partly off-screen at first view; the row scrolls to it. The reel shows the whole row. Design call for the user. | 🟡 |
| 14 | Settings | "Open Cameras when the TV turns on" hasn't been tried on the TV (it would switch to the CCTV input). Its decision logic is unit tested. | 🟡 |
| 15 | Home | The "Switching to" overlay is barely seen when opening apps, because the app covers home straight away. It shows mainly for HDMI switches. | 🟡 |
| 16 | Patterns | `drawDoodles` and `polyPath` in `Doodles.kt` are now unused (DOODLES draws media glyphs). | 🔴 minor |
| 18 | All apps | Scrolling the grid isn't smooth yet: the 95th-percentile frame is 46 ms on the release build. | 🔴 |
| 19 | All apps | When the grid scrolls, cards clip sharply under the header. A soft top fade would look better. | 🔴 minor |
| 20 | All apps | Apps with only a square icon (PatchWall, Play Games) show a small icon on a glass card, a bit box-in-box. | 🔴 minor |
| 21 | All apps | The failure pill shrinks while fading out, and two identical failures within 3 s don't restart its timer (home does the same). | 🔴 minor |
| 22 | Setup | The "Clear defaults" settings path in the how-to note hasn't been checked on this TV. | 🟡 |
| 23 | Setup | The full switch flow (turn on, press Home, Always, restart) still needs doing on the TV by you. | 🟡 |

## Fixed

| # | Area | Issue | Fixed in |
|---|---|---|---|
| F1 | Config | Wrong-typed JSON fields crashed the parser. | Task 2, `4eb9926` |
| F2 | Config | An empty or all-invalid tile list showed an empty home screen. | Task 2, `4eb9926` |
| F3 | HomeController | A throwing action could crash the launcher. | Task 4, `ec95ffc` |
| F4 | HomeController | Duplicate tiles gave duplicate keys, which would crash the tile row. | Task 4, `ec95ffc` |
| F5 | Icons | Banners were stretched into squares. Now aspect-kept, and known apps use vector brand marks. | Task 6 redesign `86739d4` |
| F6 | Background | Every focus move re-rasterised and uploaded a full-screen bitmap. Now GPU gradients plus cached alpha masks rendered off the main thread, with an alpha-only cross-fade. The 95th-percentile frame dropped from 250 ms to 73 ms. | Task 6 redesign `86739d4` |
| F7 | Build | Task 6 was interrupted. Taken over and finished in the executor session. | Task 6 redesign `86739d4` |
| F8 | Icons | Compose's path parser mangled the Netflix and Prime logos. Switched to the framework `PathParser`. | Task 6 redesign |
| F9 | Home | Back from All apps put focus on the first tile once Home had ever been pressed. | Fix round `438a1af` |
| F10 | Setup | The how-to card could be cut off with no way to scroll on the 540 dp tall screen. | Fix round `438a1af` |
| F11 | Settings | The last polish pass (grid behind the list, off-white focused row at night, stable tile callbacks) is now installed on the TV. | `86739d4`, installed 2026-11-08 |
