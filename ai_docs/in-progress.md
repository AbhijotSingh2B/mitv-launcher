# In Progress

Where the work stands, as of 2026-11-07 (night).

## Done, not committed yet

Everything below is in the working tree on `feat/mitv-launcher`, waiting for your OK to commit. The plan is to commit it in 2–3 parts: visual redesign, night mode with settings, then build and performance.

- **Task 6, home screen: redesigned and checked on the TV.** Poppins font, glass tiles, vector logos, a background motif for each tile, the "Switching to" overlay and gutter-aware scrolling.
- **Night mode:** automatic from 10 pm to 6 am, or Always on or Off. A moon badge shows next to the clock.
- **Launcher Settings screen:** opened with a short press of Menu on home, and from a button in Setup.
- **Release build:** R8 minify and resource shrinking. The APK is 2.2 MB and uses about 32 MB of memory on the TV.
- **Tests:** 33 unit tests pass, including the new night-hours and cameras-on-start tests.
- **Docs:** the spec has a "v1 additions" section, and `licenses/NOTICE.md` lists the font, logo and icon licences.
- **Final polish (not yet installed on the TV):** a quieter grid behind the settings list, an off-white focused settings row at night, and steadier tile callbacks so fewer tiles redraw on each focus move.

## Next

1. **Commit** the redesign once you give the OK.
2. **Tell the main chat** that the TV is free. It will then run the approved debloat: reversible `pm disable-user` on the safe list, 0.5x animations, and a background process limit of 3.
3. **Task 7 (All apps screen) and Task 8 (Setup, "Use as home screen" and the setup doc)** run in parallel.
4. **Final review** of the whole branch, then merge. Nothing is pushed without asking.

## Waiting on you

- OK to commit.
- Whether the 6th tile being cut off at the right edge is fine (the row scrolls), or whether the whole row should fit on screen like the reel.
