# Week & Month Overview — Design

Date: 2026-11-08
Status: draft, awaiting owner approval
Mockups: `docs/ui-candidates/overview.html`, candidate **D · Hybrid A + B** (chosen)

## 1. Intent

Today the phone app shows one day at a time. The owner wants to zoom out to a
week and a month to answer two questions on one screen:

- **How much** time went to each tag in the period, and how that compares to
  the previous period.
- **When** that time happened (the shape of the days: wake-up, work day, late
  nights), and where unlabeled time is still waiting.

This deliberately crosses the MVP non-goal "statistics, charts, summaries"
(MVP spec §2). Capture stays untouched: the overview is read-only plus
navigation.

## 2. Goals, non-goals, anti-goals

**Goals**

- A Week view and a Month view, reachable from the existing Day screen.
- Per-tag totals for the period, ranked, with change vs the same span of the
  previous period.
- A clock-positioned grid of the period's days (one column per day) that can
  isolate a single tag, or unlabeled time.
- A fast path from "something is unlabeled" to the Day view that fixes it.

**Non-goals**

- Goals, targets, streaks, reminders, notifications.
- Custom ranges, years, quarters, export of the summary (CSV export is unchanged).
- Anything on the watch.
- Per-tag "hide from overview" settings (Sleep is shown like any tag).

**Anti-goals**

- Any change to the capture path (watch, side button, Mark button on Day).
- Totals that disagree with the Day screen. Both views must derive from the
  same blocks (`deriveBlocks` + `clipToDay`).
- Judging tags: no green/red, no "good" or "bad" direction on the change arrows.

## 3. Approaches considered

Four visual approaches were mocked up in `docs/ui-candidates/overview.html`:

| | Approach | Strength | Weakness |
|---|---|---|---|
| A | **Rhythm**: one clock-positioned rail per day (columns) | Shows *when*; matches the Day rail | Weak at "how many hours of X" |
| B | **Totals**: % labeled + ranked tag list with deltas | Answers *how much* directly | Says nothing about timing |
| C | **Calendar**: day cards (week), calendar of tag rings (month) | Familiar; easy to find unlabeled days | Imprecise for comparing amounts |
| D | **Hybrid A + B**: A's grid on top, B's list below, linked by tap | Both questions on one screen | Both halves smaller; screen scrolls |

**Chosen: D.** The owner picked it after seeing all four. The tap link between
the list and the grid (tap a tag, the grid shows only that tag) is what neither
A nor B can do alone.

## 4. Screen

```text
┌────────────────────────────────┐
│ ‹  Oct 19 – 25  ›          ⚙   │  app bar: ‹ › step one period, title tap = current period
│ [ Day | ✓Week | Month ]        │  segmented button (also added to the Day screen)
│      M   T   W   T   F   S   S │
│ 00  ███ ███ ███ ███ ███ ███ ███│  rhythm grid: one column per day,
│ 06  ▓▓▓ ▓▓▓ ▓▓▓ ▓▓▓ ▓▓▓ ▨▨▨ ▓▓▓│  00:00 at top, 24:00 at bottom,
│ 12  ███ ███ ▨▨▨ ███ ███ ▨▨▨ ███│  colored by tag, unlabeled hatched
│ 18  ▓▓▓ ▓▓▓ ▓▓▓ ▓▓▓ ▓▓▓ ▨▨▨ ▓▓▓│
│ 24                             │
│ 85% labeled · 23h 55m  [Label] │  labeled line (stands in for the Unlabeled row)
│ ● Sleep       60h 15m   ▾ 35m  │  ranked tag list
│ ● Work        43h 30m  ▾ 1h 5m │
│ ● Reading     21h 50m  ▾ 7h 50m│
│   …  (screen scrolls)          │
└────────────────────────────────┘
```

### 4.1 Navigation

- A three-way segmented button **Day · Week · Month** sits directly under the
  app bar on all three views. On Day it goes between the header and the rows;
  the Day screen otherwise keeps its rows, Mark button, snackbar and empty
  state unchanged.
- App bar title: Day keeps `Today · Oct 26` / `Sun, Oct 25`. Week shows
  `Oct 19 – 25` (or `Oct 26 – Nov 1` across months). Month shows `October`
  (plus the year when it is not the current year). The current period shows the
  same small primary dot the Day title uses for today.
- `‹` / `›` step one week or one month. Tapping the title returns to the
  current period. Like Day, the current period follows the clock (rolls over at
  Monday 00:00 / the 1st at 00:00) until the owner steps away from it.
- All three modes share **one anchor date** (the date Day shows today), or
  "follow today" when nothing is pinned. Day `‹ ›` moves it ±1 day, Week ±7
  days, Month ±1 month (day of month clamped, e.g. Mar 31 → Feb 28). Switching
  mode keeps the anchor: from Day "Oct 22", Week shows the week containing
  Oct 22 and Month shows October; a week that straddles two months opens the
  month of the anchor. Back to Day shows the anchor date. Following today stays
  following today in every mode. *(Builder's call, settled in review.)*
- On app launch the mode is Day (not persisted).
- Settings (gear) and system back behave as today; back from Week/Month goes to
  Day.

### 4.2 Period definitions

- Weeks run **Monday 00:00 to next Monday 00:00**, local time zone, fixed (not
  locale-dependent).
- Months run from the 1st 00:00 to the next 1st 00:00.
- All boundaries are computed with `java.time` calendar arithmetic in the
  phone's zone, so DST days are 23 h or 25 h long, never assumed 24 h.
- The **elapsed span** of a period is
  `[max(periodStart, firstPointAt), min(periodEnd, now))`. Future time and time
  before the very first point are never counted (no blocks exist there). A
  period entirely outside that range is empty. *(Clamp to the first point:
  builder's call, settled in review.)*

### 4.3 Rhythm grid

- One column per calendar day of the period (7 for a week, 28–31 for a month).
- Each column is that day's blocks from `clipToDay`, positioned by clock time:
  `y = (visibleStart − dayStart) / (dayEnd − dayStart) × height`. A DST day is
  therefore scaled into the same column height.
- Labeled blocks fill with the tag's rail color (the Day rail's light/dark
  tones); unlabeled blocks use the Day rail's hatch. A 1 px surface-colored gap
  separates consecutive blocks; blocks shorter than 1 px still draw at 1 px.
- Hairlines at 06, 12, 18. Axis labels `00 06 12 18 24` on the left.
- Column headers: Week shows weekday initial + date (`M 19`), weekends in
  primary. Month shows `M` over Mondays only and dates `1, 5, 12, 19, 26, 31`
  under the grid (the Mondays plus first and last day).
- Today's column ends at a pulsing now-dot. Future days are empty outlined
  columns. Days before the first point ever are empty (no fill).
- Grid height: ~210 dp on Week, ~200 dp on Month (the mockup values). The grid
  scrolls away with the list (no sticky header).
- **Tap a column** → Day view pinned to that date.

### 4.4 Labeled line

- One row between grid and list: `85% labeled · 23h 55m unlabeled` and a
  tonal **Label** button. (The mockup says "left"; "unlabeled" is clearer.)
- `% labeled = floor(labeled time / elapsed span × 100)`. Unlabeled time
  includes the current in-progress block when it has no tag, and blocks whose
  tag was deleted (their `tagId` is already null).
- The line always shows; at 100 % the Label button is hidden and the line
  reads `100% labeled`.
- **Tap the line** → focus Unlabeled (see 4.6).
- **Tap Label** → Day view pinned to the earliest day in the period with an
  unlabeled visible span, ignoring the in-progress block (it is still open).
  A block crossing midnight counts on the first day it is visible in the
  period. If the only unlabeled block is the in-progress one, Label opens
  today. *(Builder's call, settled in review.)*

### 4.5 Tag list

- One row per tag with time > 0 in the elapsed span, ranked by total
  descending (ties: tag name). Unlabeled is not a row; the labeled line covers it.
- Each row: color dot, name, total (`formatDuration`-style `43h 30m`, minutes
  precision; under a minute shows seconds as `formatDuration` does), a bar whose
  length is total / largest total, and the change.
- **Change** = this elapsed span vs the **same span of the previous period**:
  - Week: previous week from its Monday 00:00 to the same weekday and clock
    time as now. Month: previous month from its 1st 00:00 to the same day of
    month and clock time, clamped to that month's end (Oct 31 14:44 compares to
    all of September).
  - For a past period (fully elapsed), the whole previous period.
  - Shown as `▴ 20m` / `▾ 1h 5m` in neutral on-surface-variant color;
    `same` when |change| < 5 min. A tag with no time in the previous span shows
    `▴` its full total.
  - No arrows at all when the previous span is entirely before the first point
    ever recorded (nothing to compare with).
  - A tag with time in the previous span but none now is simply not listed.
- **Tap a row** → focus that tag (see 4.6).

### 4.6 Focus

- Focus is one of: none, a tag, or Unlabeled. Default none.
- While focused, every block in the grid that is not the focus fades to ~14 %
  opacity; the focused row (or line) is highlighted with its tag container
  color and a 2 dp ring; the labeled line is replaced by
  `Showing Work only  [× Clear]`.
- Tap the focused row again, or Clear, to return to none.
- Focus is kept while stepping `‹ ›` (follow Work across weeks) and cleared when
  switching mode or when the focus has no time in the new period (a tag absent,
  or Unlabeled at 100 %), so a focus never sticks without its Clear control.

### 4.7 What Week/Month do not have

- No Mark button. The watch, the side button and the Day screen still mark.
- No long-press delete, no label sheet. Editing happens in Day.

### 4.8 Empty states

- No points at all: the same `empty_hint` text as Day, centered.
- A period with no points but earlier/later data: grid draws empty columns,
  labeled line and list are hidden, and a single line reads `Nothing recorded`.

## 5. Constraints

- Phone only, Jetpack Compose, the existing Rail theme (`Theme.kt`): tag rail
  tones, hatch, Google Sans Flex, M3 segmented button.
- Pure logic (period bounds, same-span previous period, per-day clipping,
  per-tag totals, % labeled) lives in `core/` as plain Kotlin with unit tests,
  next to `Blocks.kt`/`Days.kt`. The phone module only renders.
- Data stays as is: no schema change, no new DAO queries. The overview derives
  from the same `repo.points` / `repo.tags` flows the Day view uses.
- Durations follow `core/Format.kt` (`formatDuration`).
- Accessibility: the grid is decorative for screen readers except that each
  column is a button labeled with its date; the tag list carries the numbers.

## 6. Spec change

MVP spec §2 non-goals currently list "Statistics, charts, summaries, goals,
reminders." Change to "Goals, reminders." and point to this document for the
Week/Month overview.

## 7. Left to the builder

- Composable and view-model structure (a separate `OverviewViewModel` vs
  extending `TimelineViewModel`), and how the mode state is hoisted in
  `MainActivity`.
- Drawing the grid (a `Canvas` per column or one for the grid).
- Recomputation cadence: the Day view's 1 s ticker is acceptable; a 1 min tick
  for the overview is fine too.
- Exact dp values beyond those above, motion for focus fade.
- Test layout in `core/src/test` and any render test in `phone/src/testDebug`
  following `RenderScreensTest.kt`.
