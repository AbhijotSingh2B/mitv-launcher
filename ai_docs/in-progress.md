# In Progress

Where the work stands, as of 2026-11-08.

## Done and committed on `feat/mitv-launcher` (not pushed)

- **Task 6, home screen redesign** (`86739d4`): Poppins font, glass tiles, vector logos, a background motif per tile, night mode with a moon badge, and the Launcher Settings screen.
- **TV clean-up record** (`b5b5ae9`): the main chat applied the clean-up, which disabled 5 packages and set animations to 0.5x. Free RAM went from 109 to 147 MB. See [docs/tv-cleanup.md](../docs/tv-cleanup.md).
- **Task 7, All apps screen** (`11005fb`, merged): a 5-column grid of 16:9 glass cards in the same style as the home screen. It lists every app, including PatchWall, and Back returns to the All apps tile.
- **Task 8, Setup screen** (`5ffd584`, merged): the "Use as home screen" switch (safe; off by default) in the Launcher Settings style, with how-to steps. Plus [docs/SETUP-ON-TV.md](../docs/SETUP-ON-TV.md).
- **Fix round** (`438a1af`): focus returns correctly from All apps, the Setup how-to card fits the 540 dp screen, All apps shows a loading state and keeps its list between visits, and the icon cache holds 48.
- **On the TV:** the release build is installed. Setup, the home screen, All apps and the Back focus were all checked on the TV. Nothing has broken from the disabled packages so far.
- **Tests:** 33 unit tests pass.

## Next

1. **Final review of the whole branch** (Opus), from the main chat.
2. **Your step:** turn on **Use as home screen** in Setup, press Home and pick **Home → Always**. Then restart the TV to check that it boots into the launcher.
3. Merge to `main` when you're happy. Nothing is pushed without asking.

## Waiting on you

- Whether the 6th tile being cut off at the right edge of home is fine (the row scrolls), or whether the whole row should fit on screen like the reel.
