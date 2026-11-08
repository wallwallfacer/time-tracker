# Week & Month Overview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Week and Month views to the phone app: a clock-positioned rhythm grid on top, a ranked tag list with same-span deltas below, linked by tap-to-focus.

**Architecture:** All period and aggregation logic is pure Kotlin in `core/Overview.kt`, derived from the same `deriveBlocks` + `clipToDay` the Day view uses. `TimelineViewModel` owns the shared navigation state (mode, anchor date, focus) and exposes an `overview` flow next to the existing day `state`. A new `OverviewScreen.kt` renders it; `MainActivity` switches between `TimelineScreen` and `OverviewScreen` by mode.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (BOM 2024.10.01, material3 1.3: `SingleChoiceSegmentedButtonRow`), java.time, JUnit 4, Robolectric.

**Spec:** `docs/superpowers/specs/2026-11-08-week-month-overview-design.md` (read it; this plan does not repeat its UI rules). Mockup: `docs/ui-candidates/overview.html`, section D.

## Global Constraints

- No schema change, no new DAO query. Inputs are `repo.points` and `repo.tags`.
- Weeks start Monday 00:00; months the 1st 00:00; phone zone; all boundaries via `java.time` (DST days are 23 h / 25 h).
- Elapsed span = `[max(periodStart, firstPointAt), min(periodEnd, now))`.
- Durations print with `formatDuration` from `core/Format.kt`.
- Colors only from `Theme.kt` (`tagTones(color).rail` / `.container` / `.onContainer`, `MaterialTheme.colorScheme`). Change arrows use `onSurfaceVariant`, never green/red.
- Week/Month have no Mark button, no long-press delete, no label sheet.
- All new user-visible text goes in `phone/src/main/res/values/strings.xml`.

## Review Focus

1. **DST day in the grid** (Europe/Berlin, Sun 2026-10-25 is 25 h): the column must be scaled by that day's real length, no block drawn past the column. Pinned by `overview day columns use real day length across DST` (Task 1).
2. **First week of use** (first point mid-week): % labeled must reach 100 when everything since the first point is labeled. Pinned by `span starts at first point` (Task 1).
3. **Oct 31 compares to all of September**, not Sep 30 14:44. Pinned by `previous same span clamps to previous month end` (Task 1).
4. **Stepping with a focus into a period where that tag is absent** must clear the focus (else no Clear control is visible). Pinned by `focus clears when focused tag absent after stepping` (Task 2).
5. **Installing on the owner's phone must keep their data**: `adb install -r` only; if it fails with a signature mismatch, stop and ask, never uninstall (Task 5).

---

### Task 1: Core period math and overview aggregation

**Files:**
- Create: `core/src/main/kotlin/dev/timetracker/core/Overview.kt`
- Test: `core/src/test/kotlin/dev/timetracker/core/OverviewTest.kt`

**Interfaces:**
- Produces:
  - `enum class ViewMode { DAY, WEEK, MONTH }`
  - `fun periodStart(mode: ViewMode, anchor: LocalDate): LocalDate` — DAY: anchor; WEEK: Monday on/before anchor; MONTH: 1st.
  - `fun periodEnd(mode: ViewMode, anchor: LocalDate): LocalDate` — exclusive: start +1 day / +1 week / +1 month.
  - `fun stepAnchor(mode: ViewMode, anchor: LocalDate, delta: Int): LocalDate` — ±1 day, ±7 days, `plusMonths(delta)` (java.time clamps Mar 31 → Feb 28).
  - `data class DayColumn(val date: LocalDate, val start: Long, val end: Long, val blocks: List<ClippedBlock>)`
  - `data class Overview(val days: List<DayColumn>, val totals: Map<String?, Long>, val previous: Map<String?, Long>?, val elapsedMs: Long, val labeledPercent: Int, val labelTarget: LocalDate?)` — `totals`/`previous` keyed by tagId, `null` key = unlabeled ms. `previous == null` means no arrows.
  - `fun overview(points: List<Point>, mode: ViewMode, anchor: LocalDate, zone: ZoneId, now: Long): Overview` — `mode` is WEEK or MONTH.

- [ ] **Step 1: Write the failing tests** in `OverviewTest.kt` (zone `Asia/Singapore` unless stated; helper `ms(month, day, hour, min=0)` like `DaysTest`):
  - `week starts monday`: `periodStart(WEEK, 2026-10-22) == 2026-10-19`; `periodEnd == 2026-10-26`; anchor on a Monday returns itself.
  - `month bounds`: `periodStart(MONTH, 2026-10-22) == 2026-10-01`, `periodEnd == 2026-11-01`.
  - `step month clamps day`: `stepAnchor(MONTH, 2026-03-31, -1) == 2026-02-28`; `stepAnchor(WEEK, d, 1) == d + 7`; `stepAnchor(DAY, d, -1) == d - 1`.
  - `totals split block crossing period start`: points Sleep at Sun Oct 18 22:00, Work at Mon Oct 19 08:00, unlabeled at Oct 19 12:00; now = Oct 19 14:00; WEEK anchor Oct 19 → `totals["sleep"] == 8h`, `totals["work"] == 4h`, `totals[null] == 2h`, `elapsedMs == 14h`, `labeledPercent == 85` (floor of 12/14).
  - `span starts at first point`: first point Wed Oct 21 09:00 tagged work, now Oct 21 17:00, WEEK → `elapsedMs == 8h`, `labeledPercent == 100`, `previous == null`.
  - `previous same span week`: points cover both weeks; now = Wed Oct 21 10:00 → `previous` sums only Mon Oct 12 00:00 – Wed Oct 14 10:00.
  - `previous same span clamps to previous month end`: now = Oct 31 14:44, MONTH → previous span = all of September (Sep 1 00:00 – Oct 1 00:00).
  - `past period compares whole previous period`: anchor Oct 5 (week Oct 5–11), now Oct 21 → `elapsedMs == 7 × 24h`, previous = whole week Sep 28 – Oct 4.
  - `label target skips current block`: unlabeled completed block on Tue, current unlabeled block today → `labelTarget == Tue`; only the current block unlabeled → `labelTarget == today`; all labeled → `null`.
  - `overview day columns use real day length across DST`: zone `Europe/Berlin`, WEEK anchor 2026-10-25 → the column for Oct 25 has `end - start == 25h`; columns count 7.
  - `future days are empty columns`: MONTH Oct, now Oct 26 → 31 columns, `days[30].blocks.isEmpty()`.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :core:test --tests '*OverviewTest*'`
Expected: compilation FAIL (`ViewMode` unresolved).

- [ ] **Step 3: Implement `Overview.kt`**

Use `deriveBlocks(points, now)` and `clipToDay` per day column (`date.atStartOfDay(zone)` for both bounds). Totals: overlap of each block with the span, keyed by `block.start.tagId`. Previous span algorithm (not determined by the signature):

```kotlin
// prevStart = periodStart of the previous period; prevEnd mirrors the elapsed end.
// If the current period is fully elapsed: prevEnd = this period's start.
// WEEK: prevEnd = elapsedEnd (as ZonedDateTime) minusWeeks(1).
// MONTH: if elapsedEnd.dayOfMonth > prevMonth.lengthOfMonth() -> prevEnd = this period's start,
//        else elapsedEnd minusMonths(1).
// Clamp prevStart up to firstPointAt; previous = null when prevEnd <= firstPointAt.
```

`labeledPercent = floor((elapsed - unlabeled) * 100 / elapsed)`; 100 when `elapsedMs == 0`. No points at all → empty columns (dates still present), empty totals, `previous = null`, `labelTarget = null`.

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew :core:test`
Expected: BUILD SUCCESSFUL, all core tests pass.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/dev/timetracker/core/Overview.kt core/src/test/kotlin/dev/timetracker/core/OverviewTest.kt
git commit -m "feat(core): week and month overview aggregation"
```

### Task 2: Navigation, focus and overview state in `TimelineViewModel`

**Files:**
- Modify: `phone/src/main/kotlin/dev/timetracker/phone/ui/TimelineViewModel.kt`
- Modify: `phone/src/test/kotlin/dev/timetracker/phone/ui/TimelineViewModelTest.kt` (rename calls; add tests)

**Interfaces:**
- Consumes: Task 1 `ViewMode`, `stepAnchor`, `periodStart`, `overview`, `Overview`.
- Produces:
  - `sealed interface Focus { data class OnTag(val tagId: String) : Focus; data object Unlabeled : Focus }`
  - `data class TagTotal(val tag: Tag, val ms: Long, val deltaMs: Long?)` — `deltaMs` null when `overview.previous == null`.
  - `data class OverviewState(val mode: ViewMode, val anchor: LocalDate, val isCurrent: Boolean, val overview: Overview, val rows: List<TagTotal>, val tagsById: Map<String, Tag>, val focus: Focus?, val isEmpty: Boolean)` — `rows` ranked by `ms` desc, ties by `tag.name`; tags with 0 ms omitted; `isEmpty` = no points at all.
  - `val mode: StateFlow<ViewMode>`, `val overview: StateFlow<OverviewState?>` (null while mode == DAY)
  - `fun previous()`, `fun next()`, `fun current()` — replace `previousDay/nextDay/today`; step by the current mode via `stepAnchor`; `current()` unpins (follow today).
  - `fun setMode(mode: ViewMode)` — keeps the anchor (pinned or following today), clears focus.
  - `fun openDay(date: LocalDate)` — pins `date`, sets DAY.
  - `fun toggleFocus(focus: Focus)` — same focus again → null.

- [ ] **Step 1: Write the failing tests** (rename existing `previousDay/nextDay/today` calls to `previous/next/current` first):
  - `week next steps seven days`: `setMode(WEEK)`; `next()` → `overview.anchor == today + 7`.
  - `mode switch keeps anchor`: pin Oct 22 via `previous()` calls in DAY, `setMode(MONTH)` → anchor Oct 22; `setMode(DAY)` → `state.day == Oct 22`.
  - `following today stays following`: `setMode(WEEK)` without pinning → `isCurrent == true`.
  - `open day pins and switches to day`: `setMode(WEEK)`, `openDay(d)` → `mode == DAY`, `state.day == d`.
  - `rows ranked with deltas`: tags work/sleep with known totals this and last week → order and `deltaMs` values.
  - `focus toggles`: `toggleFocus(OnTag("work"))` twice → `focus == null`.
  - `focus clears on mode switch`.
  - `focus clears when focused tag absent after stepping`: focus a tag that has no time in the previous week, `previous()` → `focus == null`. Same for `Unlabeled` when the stepped period is 100 % labeled.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :phone:testDebugUnitTest --tests '*TimelineViewModelTest*'`
Expected: compilation FAIL (`previous` / `ViewMode` unresolved).

- [ ] **Step 3: Implement**

Add `MutableStateFlow`s for `mode` and `focus` beside `pinned`. Build `overview` by `combine(repo.points, repo.tags, pinned, mode, focus, ticker)` (use the nested-combine or array overload; `combine` takes max 5 typed flows). Clear focus inside `previous()/next()/current()` after computing the new period if the focus has no time there (compute from current points via `overview(...)`), so the state never emits a dangling focus.

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew :phone:testDebugUnitTest --tests '*TimelineViewModelTest*'`
Expected: PASS, including the pre-existing day tests.

- [ ] **Step 5: Commit**

```bash
git add phone/src/main/kotlin/dev/timetracker/phone/ui/TimelineViewModel.kt phone/src/test/kotlin/dev/timetracker/phone/ui/TimelineViewModelTest.kt
git commit -m "feat(phone): shared day/week/month navigation and overview state"
```

### Task 3: `OverviewScreen` and the mode switch

**Files:**
- Create: `phone/src/main/kotlin/dev/timetracker/phone/ui/OverviewScreen.kt`
- Modify: `phone/src/main/kotlin/dev/timetracker/phone/ui/TimelineScreen.kt` (generalize `DayHeader` into `PeriodHeader(title: String, isCurrent: Boolean, onPrev, onNext, onCurrent, onSettings)` made `internal`; add `ModeSwitch` under it)
- Modify: `phone/src/main/kotlin/dev/timetracker/phone/ui/TimelineRow.kt` (extract the stripe loop of `hatched` into `internal fun DrawScope.stripes(left: Float, top: Float, w: Float, h: Float, color: Color)`; `hatched` calls it inside its clip)
- Modify: `phone/src/main/res/values/strings.xml`
- Test: `phone/src/testDebug/kotlin/dev/timetracker/phone/ui/RenderScreensTest.kt`

**Interfaces:**
- Consumes: Task 2 `OverviewState`, `Focus`, `TagTotal`; `tagTones`, `tabular` from `Theme.kt`.
- Produces:
  - `@Composable internal fun ModeSwitch(mode: ViewMode, onMode: (ViewMode) -> Unit)` — `SingleChoiceSegmentedButtonRow`, 16 dp side padding, labels from strings.
  - `@Composable fun OverviewScreen(state: OverviewState, zone: ZoneId, onPrev: () -> Unit, onNext: () -> Unit, onCurrent: () -> Unit, onMode: (ViewMode) -> Unit, onOpenDay: (LocalDate) -> Unit, onFocus: (Focus) -> Unit, onSettings: () -> Unit)`
  - `TimelineScreen` gains parameter `onMode: (ViewMode) -> Unit` (after `onToday`).

Strings to add (exact):

```xml
<string name="mode_day">Day</string>
<string name="mode_week">Week</string>
<string name="mode_month">Month</string>
<string name="labeled_line">%1$d%% labeled · %2$s unlabeled</string>
<string name="all_labeled">100% labeled</string>
<string name="label_action">Label</string>
<string name="showing_only">Showing %1$s only</string>
<string name="clear">Clear</string>
<string name="nothing_recorded">Nothing recorded</string>
<string name="delta_same">same</string>
```

Titles: week `Oct 19 – 25` / `Oct 26 – Nov 1` (`MMM d` + en dash + `d` or `MMM d`); month `October`, plus ` 2025` when not the current year; current period gets the primary dot like Day's today title (spec §4.1).

Layout: one `LazyColumn` (grid item, labeled-line/focus item, tag rows) so the grid scrolls away (spec §4.3). Grid: one `Canvas` per column inside a `Row`; Week 206 dp tall, Month 196 dp; block y from `(visibleStart − col.start) / (col.end − col.start)`; labeled fill `tagTones(tag.color).rail`, unlabeled `stripes(..., scheme.outline)`; non-focused blocks at alpha 0.14 while focused; 1 px `surface` gap above each block; hairlines at 25/50/75 %; now-dot on today's column (static circle is fine, pulse optional). Column `clickable` → `onOpenDay(date)` with `contentDescription` = the date. Delta text: `▴ 20m` / `▾ 1h 5m` / `same` (< 5 min), `formatDuration`, no arrows when `deltaMs == null`.

- [ ] **Step 1: Add render tests** `weekLight`, `weekDarkFocusWork`, `monthLight`, `monthFocusUnlabeled` to `RenderScreensTest`, building `OverviewState` from a generated multi-week sample (reuse the day sample for today; add 3 prior weeks of Sleep/Breakfast/Commute/Work/Lunch points with a few unlabeled gaps). Update the existing `timeline()` helper for the new `onMode` parameter.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :phone:testDebugUnitTest --tests '*RenderScreensTest*'`
Expected: compilation FAIL (`OverviewScreen` unresolved).

- [ ] **Step 3: Implement** `OverviewScreen.kt`, `ModeSwitch`, `PeriodHeader`, `stripes`, strings as specified above.

- [ ] **Step 4: Run and inspect renders**

Run: `./gradlew :phone:testDebugUnitTest --tests '*RenderScreensTest*'`
Expected: PASS; open `phone/build/renders/06-week-light.png` … `09-month-focus-unlabeled.png` and compare against mockup section D (layout, hatch, fade, delta column). Fix visual gaps before committing.

- [ ] **Step 5: Commit**

```bash
git add phone/src/main/kotlin/dev/timetracker/phone/ui/OverviewScreen.kt phone/src/main/kotlin/dev/timetracker/phone/ui/TimelineScreen.kt phone/src/main/kotlin/dev/timetracker/phone/ui/TimelineRow.kt phone/src/main/res/values/strings.xml phone/src/testDebug/kotlin/dev/timetracker/phone/ui/RenderScreensTest.kt
git commit -m "feat(phone): week and month overview screen"
```

### Task 4: Wire into `MainActivity`, update the MVP spec

**Files:**
- Modify: `phone/src/main/kotlin/dev/timetracker/phone/MainActivity.kt`
- Modify: `docs/superpowers/specs/2026-10-25-time-tracker-mvp-design.md` (§2 non-goals line → `- Goals, reminders. (Week/Month summaries: see 2026-11-08-week-month-overview-design.md.)`)
- Add: `docs/superpowers/specs/2026-11-08-week-month-overview-design.md`, `docs/ui-candidates/overview.html`, this plan

**Interfaces:**
- Consumes: Task 2 VM API, Task 3 composables.

- [ ] **Step 1: Wire it**: collect `viewModel.mode`; when not DAY show `OverviewScreen` (callbacks → `previous/next/current/setMode/openDay/toggleFocus`, settings → `screen = "settings"`) with `BackHandler { viewModel.setMode(ViewMode.DAY) }`; Day passes `onMode = viewModel::setMode` and the renamed `previous/next/current`.

- [ ] **Step 2: Full verification**

Run: `./gradlew test :phone:assembleDebug :wear:assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass, `phone/build/outputs/apk/debug/phone-debug.apk` exists.

- [ ] **Step 3: Commit**

```bash
git add phone/src/main/kotlin/dev/timetracker/phone/MainActivity.kt docs/superpowers/specs/2026-10-25-time-tracker-mvp-design.md docs/superpowers/specs/2026-11-08-week-month-overview-design.md docs/superpowers/plans/2026-11-08-week-month-overview.md docs/ui-candidates/overview.html
git commit -m "feat(phone): open week and month overviews from the day screen"
```

### Task 5: On-device test on the owner's phone (wireless adb)

Devices seen this session: phone `192.168.3.254:43529` (SM-S938), watch `192.168.3.91:43445` (SM-L310). Re-check with `adb devices -l`; the watch port changes when its Wi-Fi sleeps.

- [ ] **Step 1: Install without losing data**

Run: `adb -s 192.168.3.254:43529 install -r phone/build/outputs/apk/debug/phone-debug.apk`
Expected: `Success`. On `INSTALL_FAILED_UPDATE_INCOMPATIBLE` or any signature error: STOP and ask the owner. Never `adb uninstall` (it deletes their points).

- [ ] **Step 2: Walk the flows**, capturing each with `adb -s <phone> exec-out screencap -p > <scratchpad>/NN-name.png` and finding tap targets with `adb shell uiautomator dump`:
  - Launch (`adb shell monkey -p dev.timetracker.app -c android.intent.category.LAUNCHER 1`): Day screen shows the segmented button; rows, Mark and undo still work.
  - Week: grid, labeled line, ranked rows with arrows; `‹` then `›`; title tap returns to current week.
  - Tap a tag row → grid fades others, row highlighted, Clear resets. Tap labeled line → Unlabeled focus.
  - Tap a column → Day for that date. Tap Label → earliest unlabeled day.
  - Month: 28–31 columns, future days outlined; system back → Day.
  - Dark mode: `adb shell cmd uimode night yes`, screenshot Week, then restore `night no` (or the owner's original value read first with `adb shell cmd uimode night`).

- [ ] **Step 3: Watch sanity check** (only the phone app changed): press-mark on the watch, confirm the new point appears in Day and in Week totals.

- [ ] **Step 4:** Any device-found code issue is fixed in a separate follow-up commit (never amend Task 4's commit).
