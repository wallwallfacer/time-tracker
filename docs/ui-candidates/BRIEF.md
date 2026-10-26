# UI redesign candidates — shared brief

Five visual redesigns of the Time Tracker Android app (phone + Galaxy Watch).
**Features must not change.** Every screen, action and string below must still exist;
only layout, typography, color, shape, hierarchy and motion change.

## Product in one paragraph

A personal time logger. The only capture action is "mark a point in time": double-press the
watch Home key or phone side key and a timestamp is stored, nothing else. Later, on the phone,
the owner labels the *blocks* of time between consecutive points. No stats, no charts, no cloud.
Capture must be zero-decision; labeling happens later, quickly.

## Screens every candidate must show (one phone frame each, side by side)

1. **Timeline (today)** — default screen.
   - Header: day title (`Today · Oct 26`, tap = jump to today), prev `‹` / next `›` day, settings gear.
   - One row per block: start time `HH:mm`, tag chip (or dashed/ghost "Unlabeled"), duration.
   - Last row is the *current* block: `In progress · 12m`, ticks live (animate a pulse or seconds).
   - Big **● Mark** button at thumb reach (bottom).
   - Show long-press-to-delete affordance somehow is NOT required; but show the undo snackbar
     ("Point deleted  ·  Undo") in at least one frame or as a variant state.
2. **Label sheet** — bottom sheet over the dimmed timeline, for block `12:45 → 14:32`.
   - Times `12:45 → 14:32` (each tappable to change); for the current block the end shows `now`.
   - Sub-line: `1h 47m · tap a time to change it`.
   - Tag chips (tap = apply + close), ordered by last used; plus `+ New tag`.
   - `Insert a point in the middle` button.
   - `Delete start point` destructive button.
   - `Note (optional)` single-line text field.
   - Error line state (optional to show): `Outside the neighbouring points`.
3. **Settings** — top bar with back arrow + "Settings".
   - Tag list: color dot, name, `Rename`, `Delete` actions.
   - Primary `Export CSV` button.
4. **Phone capture confirm overlay** — full-screen dimmed overlay shown ~2 s after a side-button mark:
   big time `14:32`, `● Recorded`, `Previous block 1h 47m`.
5. **Watch** (round 1:1 frame, ~200px) — confirm screen: big `14:32`, `● Recorded`,
   `Previous block 1h 47m`. Also show a small watch-face complication showing `47m`
   (time since last mark). Watch is black background (OLED); keep the candidate's accent/type.

Optional extra frame: empty state — `Press Mark, or bind the side button's double press to "Mark".`

## Sample data (use exactly, so candidates are comparable)

Timeline, Today · Oct 26:

| start | tag        | duration          |
|-------|------------|-------------------|
| 07:10 | Breakfast  | 35m               |
| 07:45 | Commute    | 50m               |
| 08:35 | Work       | 3h 10m            |
| 11:45 | (Unlabeled)| 12s               |
| 11:45 | Lunch      | 1h 0m             |
| 12:45 | (Unlabeled)| 1h 47m            |
| 14:32 | (Unlabeled)| In progress · 12m |

Tags (last-used order): Work, Commute, Breakfast, Lunch, Sleep, Reading, Exercise.
Current tag palette (stored per tag, may be restyled): #E57373 #FFB74D #FFF176 #81C784 #4DB6AC #64B5F6 #9575CD #F06292.
Durations format: `12s`, `35m`, `3h 10m` (the app prints `1h 0m`). Times are 24-hour `HH:mm`.

## Technical rules for the HTML file

- One self-contained `.html` file, no build step. Google Fonts `<link>` allowed; no other network deps.
- Page = a title block (candidate name, one-line concept, 3–5 bullet design principles, palette swatches,
  type specimen) followed by the frames in a horizontally wrapping row.
- Phone frames: 360×780 CSS px, rounded corners ~36px, status bar mock (time 14:44, battery), gesture bar.
- Must look polished: real spacing scale, consistent radii, accessible contrast (WCAG AA for text).
- Light and dark: show the Timeline in BOTH light and dark (two frames) unless the concept is dark-only;
  other frames can use the candidate's primary mode.
- Small tasteful CSS animation allowed (live pulse on current block, sheet shadow). No JS frameworks.
  A tiny inline script is fine (e.g. ticking seconds).
- Use real text, not lorem ipsum. Annotate non-obvious design decisions with small callout labels
  under each frame (one or two lines).
