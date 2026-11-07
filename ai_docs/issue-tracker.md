# Issue Tracker

Known issues and deferred review findings. Update the status when one is fixed.

**Status key:** 🔴 open · 🟡 needs checking on the TV · 🟢 fixed

## Open

| # | Area | Issue | Status |
|---|---|---|---|
| 1 | Icons (`AndroidAppCatalog.icon`) | 320×180 app banners are drawn into a 256×256 square, so logos may look stretched on tiles. | 🟡 Task 6 to check on the TV |
| 2 | Background (`ThemedBackground`) | During a cross-fade every layer redraws the DIGITS pattern (about 196 text draws each), which may stutter on the TV's chip during fast D-pad presses. | 🟡 Task 6 to measure |
| 3 | Patterns (`drawPattern`) | Calling it without `digits` silently draws nothing for the DIGITS pattern. | 🔴 |
| 4 | Patterns | RINGS creates a new `Stroke` per circle on every frame, and digit glyphs may clip at the right and bottom edges. | 🔴 minor |
| 5 | Config (`Config.kt`) | Signed hex colours like `"-FFFFF"` are accepted, and HDMI port 0 or negative is accepted. | 🔴 minor |
| 6 | App catalogue | `installedTvApps()` and `allApps()` don't guard against exceptions. | 🔴 minor |
| 7 | HomeController | `runCatching` also swallows Errors (out of memory, stack overflow). Consider narrowing it to `Exception`. | 🔴 minor |
| 8 | Banner (`banner.xml`) | The hand-drawn "e" in "Home" is approximate and hasn't been checked visually. | 🟡 |
| 9 | Manifest | `allowBackup="true"`. Consider `false`. | 🔴 minor |
| 10 | Tests | Gaps: catalogue refresh between `tiles()` calls, failed Settings and HDMI paths, label override, and real-device HDMI labels (HDMI1/HDMI2/Computer). | 🔴 minor |
| 11 | Build | Task 6 (home screen) was interrupted when the session ended. Resume it and check for partial work. | 🔴 |

## Fixed

| # | Area | Issue | Fixed in |
|---|---|---|---|
| F1 | Config | Wrong-typed JSON fields crashed the parser. | Task 2, `4eb9926` |
| F2 | Config | An empty or all-invalid tile list showed an empty home screen. | Task 2, `4eb9926` |
| F3 | HomeController | A throwing action could crash the launcher. | Task 4, `ec95ffc` |
| F4 | HomeController | Duplicate tiles gave duplicate keys, which would crash the tile row. | Task 4, `ec95ffc` |
