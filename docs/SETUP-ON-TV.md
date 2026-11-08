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

1. Open **Home** from the TV's apps.
2. Turn on **Use as home screen**.
3. Press **Home** on the remote.
4. Choose **Home** and select **Always**.

**No chooser appears?** Another launcher is set as the default. Go to Settings -> Apps -> See all apps -> show system apps -> Google TV Home (`com.google.android.tvlauncher`) or PatchWall (`com.mitv.tvhome.atv`) -> Clear defaults, then press Home again. On PatchWall you can also turn off its "Set PatchWall as default launcher" option.

## Using it

- Move through the tile row with the arrow keys; OK opens a tile.
- A short press of **Menu** opens Launcher settings. A long press of **Menu** opens Setup.
- **Back** does nothing on the home screen.
- **All apps** lists every installed app, including PatchWall.

## Undo

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
