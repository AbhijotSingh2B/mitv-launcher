# Setting up the launcher on the Mi TV

Device details (IP, packages) are in [device-notes.md](device-notes.md).

## Install

```
./gradlew assembleRelease
adb connect 192.168.1.33:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release build is minified and signed with the local debug key. It is much faster than the debug build on this TV.

## Make it your Home

On this Mi TV, Google TV Home is a system app whose Home filter has priority 2, while a normal app's is capped at 0. So Android always opens Google TV Home, never shows a chooser, and ignores `cmd package set-home-activity`. The launcher works around this without disabling anything:

1. On the TV: **Settings → Device Preferences → Accessibility → Home button for Home launcher → Enable**, then confirm. The Home button now opens this launcher.
2. On the TV: **Settings → Device Preferences → PatchWall → Set PatchWall as default launcher → off**, so PatchWall doesn't open at start-up.
3. In **Launcher settings**, keep **Open at start-up** on (the default). A boot receiver opens the launcher when the TV starts. The service also replaces PatchWall if it opens itself in the first three minutes after boot, and replaces Google TV Home if it appears on its own.

The **TV home** pill (under the tile row, and on the About screen) opens Google TV Home on purpose. The service leaves it alone until you're back on this launcher. Pressing Home there comes back here.

Notes:
- The service only sees key events and the class name of the window in front. It can't read screen content (`canRetrieveWindowContent="false"`).
- `adb shell input keyevent KEYCODE_HOME` skips accessibility key filtering, so test the Home button with the real remote.
- A held Home press (over half a second) does nothing while the service is on, so the system's long-press Home action doesn't run.

**TVs with a Home chooser:** open **Home Setup**, turn on **Use as home screen**, press Home, choose **Home**, and select **Always**.

## Using it

- Move through the tile row with the arrow keys; OK opens a tile.
- A short press of **Menu** opens Launcher settings. A long press of **Menu** opens Setup.
- **Back** does nothing on the home screen.
- **All apps** lists every installed app, including PatchWall.
- Press Down for the shortcut pills: **TV home**, then any apps you added in **Launcher settings → Shortcuts**. **Launcher settings → Home row apps** adds apps as full tiles. Both use an on-screen A–Z keyboard, because this TV's default keyboard is the phone-only remote bridge.

## Undo

- Turn off **Home button for Home launcher** in Settings → Device Preferences → Accessibility, and **Open at start-up** in Launcher settings.
- Open Setup and turn **Use as home screen** off, then press Home. The TV falls back to Google TV Home.
- Or remove the app: `adb uninstall dev.tvlauncher`.
- If you changed PatchWall's "Set PatchWall as default launcher" option, turn it back on.

## Optional clean-up

[tv-cleanup.md](tv-cleanup.md) lists ways to trim the TV's pre-installed apps. Every step there is manual and reversible. Never disable `com.mitv.tvhome.atv` or `com.google.android.tvlauncher` while one of them is your fallback.

## Testing night mode

```
adb shell settings put global tvlauncher_night 1     # force night
adb shell settings put global tvlauncher_night 0     # force day
adb shell settings delete global tvlauncher_night    # back to auto
```
