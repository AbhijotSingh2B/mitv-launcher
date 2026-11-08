# Mi TV clean-up (user-approved safe tier)

Approved on 2026-11-07: the safe tier plus a background process limit of 3.

**Applied on 2026-11-08** (steps 1 and 2, over ADB). Before → after, with no restart yet: free RAM 109 → 147 MB, swap used 221 → 130 MB. YouTube, Netflix, Prime Video, the HDMI input service, the remote service and both launchers are still enabled. Step 3 (process limit) is manual and still to do.

Baseline on 2026-11-08: 109 MB free of 990 MB RAM, swap 221/255 MB used. The package snapshot is in the scratchpad (`debloat/packages-before.txt`).

Set `ADB` to `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s 192.168.1.33:5555`.

## 1. Disable bloat (reversible, nothing is uninstalled)

| Package | What it is |
|---|---|
| `com.xiaomi.mitv.updateservice` | Xiaomi firmware updater, about 73 MB RAM (last update was 2021) |
| `com.miui.tv.analytics` | Xiaomi usage analytics |
| `com.google.android.tvrecommendations` | Google recommendation rows |
| `com.google.android.leanbacklauncher.recommendations` | Recommendation rows for the old launcher |
| `com.xiaomi.mitv.handbook` | Mi TV user-guide app |

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe -s 192.168.1.33:5555"
for p in com.xiaomi.mitv.updateservice com.miui.tv.analytics com.google.android.tvrecommendations com.google.android.leanbacklauncher.recommendations com.xiaomi.mitv.handbook; do $ADB shell pm disable-user --user 0 $p; done
```

Not included (not in the approved list, but similar telemetry): `com.xiaomi.statistic`. Add it only if you want to.
PatchWall ad channels (`michannel`, `mitvplus`, `oemtab`) are **not installed** on this TV, so there's nothing to do for them.

## 2. Animation scales to 0.5x

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe -s 192.168.1.33:5555"
$ADB shell settings put global window_animation_scale 0.5; $ADB shell settings put global transition_animation_scale 0.5; $ADB shell settings put global animator_duration_scale 0.5
```

0.5x does not trigger the launcher's reduce-motion mode; only 0 does.

## 3. Background process limit: 3 (on the TV, by hand)

Android 9 has no ADB command for this, and it **resets on every reboot**.
On the TV, go to Settings → Device Preferences → Developer options → **Background process limit** → **At most 3 processes**.

## Undo everything

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe -s 192.168.1.33:5555"
for p in com.xiaomi.mitv.updateservice com.miui.tv.analytics com.google.android.tvrecommendations com.google.android.leanbacklauncher.recommendations com.xiaomi.mitv.handbook; do $ADB shell pm enable $p; done; $ADB shell settings put global window_animation_scale 1; $ADB shell settings put global transition_animation_scale 1; $ADB shell settings put global animator_duration_scale 1
```

The process limit: set it back to **Standard limit** in Developer options, or just restart the TV.

## After applying

Restart the TV, then check that YouTube, Netflix, Prime Video, the HDMI 2 cameras and the remote all work. Then run `$ADB shell free -m` and compare with the baseline above.
