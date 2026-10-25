# Time Tracker

A personal time logger for a Samsung phone and a Galaxy Watch. To record a moment, press the watch's Home key twice (or the phone's side button twice). That stores a timestamp and nothing else: no category is picked at capture time. Later, on the phone, you label the blocks of time between the recorded points. The MVP has no statistics, charts or cloud sync; the only export is CSV.

## Modules

- `core/`: pure JVM domain code (points to blocks, CSV formatting) with unit tests.
- `phone/`: Room database, timeline, label sheet, settings and export, side-button capture, Data Layer listener.
- `wear/`: mark activity, confirm screen, complication.

```text
watch:  Home x2 -> MarkActivity -> DataItem /point/<uuid> --Data Layer--> phone
phone:  PointListenerService -> Room -> /latest written back to watch
phone:  side button x2 -> MarkPointActivity -> Room (directly)
```

## Requirements

- Phone: Samsung phone on Android 8.0 or later (`minSdk` 26).
- Watch: Galaxy Watch6 or later, Wear OS 4 (`minSdk` 33).
- JDK 17.
- Android SDK with `platforms;android-35` and `platform-tools` installed.
- Either `ANDROID_HOME` set in your environment, or a `local.properties` file in the repo root containing `sdk.dir=/path/to/Android/sdk`. The file is git-ignored.

## Build and test

```bash
./gradlew test
./gradlew :phone:assembleDebug :wear:assembleDebug
```

Output APKs:

- `phone/build/outputs/apk/debug/phone-debug.apk`
- `wear/build/outputs/apk/debug/wear-debug.apk`

## Install

Both apps share the `applicationId` `dev.timetracker.app` and must be signed with the same key. Debug builds already are, so no extra setup is needed.

Phone, over USB:

```bash
adb install -r phone/build/outputs/apk/debug/phone-debug.apk
```

Watch, over Wi-Fi ADB:

1. On the watch, open Settings → Developer options and turn on **ADB debugging** and **Debug over Wi-Fi**.
2. Connect to the watch by its IP address and port:

   ```bash
   adb connect <watch-ip>:5555
   ```

3. Install the watch APK:

   ```bash
   adb -s <watch-ip>:5555 install -r wear/build/outputs/apk/debug/wear-debug.apk
   ```

## Bind the keys

**Watch Home key.** Settings → Advanced features → Customize keys → Home key → Double press → **Time Tracker**.

**Phone side button.** Settings → Advanced features → Side button → Double press → Open app, then pick one:

- **Mark**, if the picker lists it. This records a point and closes right away.
- **Time Tracker**, if it does not. This opens the timeline; tap the **● Mark** button at the bottom to record a point.

**Watch face complication.** Long-press the watch face, add a complication, and choose **Time Tracker**. It shows the time since the last mark and refreshes about every 5 minutes. Tapping it records a mark.

## Using the phone app

- The timeline shows one row per block: start time, label (or "Unlabeled"), and duration. The last row is the current block and ticks live.
- Tap a row to open the label sheet. There you can pick a tag, add an optional note, change the start or end time, insert a point in the middle of the block, or delete the start point.
- Long-press a row to delete its start point. A snackbar offers **Undo**.
- The arrows at the top move between days.
- The gear icon opens Settings, where you manage tags and tap **Export CSV**.

## CSV columns

The export has this header:

```csv
start_iso,end_iso,duration_sec,tag,note,source
```

- `start_iso`, `end_iso`: ISO 8601 timestamps; each carries its own UTC offset in the device's current time zone (so rows on either side of a DST change show different offsets).
- `end_iso`: empty for the current (open) block, which is the last row.
- `duration_sec`: whole seconds.
- `tag`, `note`: empty when unset. Values containing a comma, quote or line break are wrapped in double quotes.
- `source`: `WATCH`, `PHONE` or `MANUAL`.

One row per block. Example:

```csv
start_iso,end_iso,duration_sec,tag,note,source
2026-10-25T07:10:00+08:00,2026-10-25T07:45:00+08:00,2100,Breakfast,,WATCH
```
