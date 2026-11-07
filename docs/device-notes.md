# Mi TV device notes

Probed over ADB on 2026-11-07.

## Device

- Model `MiTV-AXSO0`, device codename `dangal`, Android 9 (API 28). Amlogic chip (inputs come from `com.droidlogic.tvinput`).
- ADB: network debugging on port 5555. Connect with `adb connect 192.168.1.33:5555`. The IP comes from DHCP and may change, so reserve it in the router.

## Home screen

- The current default HOME app is `com.google.android.tvlauncher/.MainActivity`, Google's stock launcher.
- PatchWall `com.mitv.tvhome.atv` is installed. Pressing Home does **not** open it at the moment.
- Other launcher-related packages: `com.google.android.leanbacklauncher`, `com.google.android.tvrecommendations`, `com.google.android.leanbacklauncher.recommendations`.
- **The Home button sends `KEYCODE_HOME` (keycode 3).** The system then starts the default `MAIN`/`HOME` intent. So making this app the default Home app is enough, and plan Task 9 (Home-key catcher) is **not needed**.

## HDMI inputs

| Port | Input id | Connected device |
|---|---|---|
| HDMI 1 | `com.droidlogic.tvinput/.services.Hdmi1InputService/HW5` | none |
| HDMI 2 | `com.droidlogic.tvinput/.services.Hdmi2InputService/HW6` | CCTV camera recorder |
| HDMI 3 | `com.droidlogic.tvinput/.services.Hdmi3InputService/HW7` | not checked |

Also: AV (`.../AV1InputService/HW1`) and the antenna tuner (`.../ADTVInputService/HW16`).

**Switching works:** `am start -a android.intent.action.VIEW -d content://android.media.tv/passthrough/<url-encoded id>`. It's handled by `com.android.tv/.MainActivity`, which shows the input, or a "Not connected" screen if nothing is plugged in. This is the same intent `TvContract.buildChannelUriForPassthroughInput(id)` builds. The input labels from `loadLabel` haven't been read yet (Task 3 checks this on the TV). Sorting by id gives the order HDMI 1, 2, 3, so the fallback match by position also works.
