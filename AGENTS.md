# AGENTS.md — סדר יום (Seder Yom)

Android app (Kotlin, Jetpack Compose, Hebrew / RTL) for a daily schedule: a weekly template of time blocks,
per-day overrides, and reminders.

> Note: the source brief began mid-document, so the original project overview was not available.
> The summary above is inferred from the milestones. Please correct it if needed.

## Project structure

```
app/src/main/java/app/sederyom/
  di/                 AppContainer
  domain/model/       BlockType, TimeBlock, DayPlan, DayOverride, ... (pure Kotlin, no android.* imports)
  domain/repository/  ScheduleRepository, SettingsStore, BackupFileAccess (interfaces; implemented in data/)
  domain/usecase/     ResolveDayPlan, ValidateBlock, ComputeReminderPlan, Save*/Reset* use cases, backup and settings use cases
  data/db/            AppDatabase, entities, DAOs, TypeConverters
  data/settings/      DataStore wrapper
  data/backup/        JSON export and import (JsonBackupSerializer, SAF file access)
  reminders/          AlarmScheduler, AlarmReceiver, BootReceiver, ReminderWorker, NotificationHelper
  ui/theme/
  ui/navigation/
  ui/today/
  ui/template/
  ui/dayedit/
  ui/settings/
  ui/onboarding/      (empty: the `onboardingDone` flag exists in settings, but no onboarding screen was in the milestones)
  ui/components/
```

The domain layer must stay free of Android imports so it can be tested on the JVM.

## Code conventions

- Follow the official Kotlin style. Use 4-space indentation and a maximum line length of 120.
- Layer flow: UI → ViewModel → use case → repository. UI never touches Room or AlarmManager directly.
- Do not use `!!`. Do not use wildcard imports.
- Use immutable data classes. Model results with sealed interfaces where useful.
- All user-visible text lives in `res/values/strings.xml`. No Hebrew literals in Kotlin code.
- Room stores LocalTime as "HH:mm" text through a TypeConverter. Dates are stored as epochDay Long values.
- Naming: entities end in `Entity`, DAOs in `Dao`, receivers in `Receiver`, workers in `Worker`.
- Do not add a dependency without listing it in AGENTS.md and giving one line explaining why.
- Commit messages use Conventional Commits prefixes: feat, fix, test, refactor, chore, docs.

## Commands

- Build debug: `./gradlew assembleDebug`
- Install on a connected device: `./gradlew installDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumented tests (needs an emulator or device): `./gradlew connectedDebugAndroidTest`
- Lint: `./gradlew lintDebug`
- Formatting and static analysis: `./gradlew ktlintCheck` (**choice: ktlint**, via the Gradle plugin)

Run unit tests and lint before every commit.

## Dependencies

| Dependency | Why |
|---|---|
| `androidx.core:core-ktx` | Kotlin extensions for Android framework APIs. |
| `androidx.activity:activity-compose` | Hosts Compose content in `ComponentActivity`. |
| `androidx.compose:compose-bom` | Keeps all Compose library versions aligned. |
| `androidx.compose.ui:ui` | Core Compose UI. |
| `androidx.compose.ui:ui-tooling-preview` | `@Preview` support. |
| `androidx.compose.ui:ui-tooling` (debug only) | Preview rendering in Android Studio. |
| `androidx.compose.material3:material3` | Material 3 components and theming. |
| `androidx.room:room-runtime`, `room-ktx` | Local SQLite storage for the template and day overrides; `room-ktx` adds coroutine/Flow and `withTransaction`. |
| `androidx.room:room-compiler` (KSP) | Generates DAO and database code at build time. |
| Plugin `com.google.devtools.ksp` | Annotation processing for Room. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Coroutines and Flow with the Android main dispatcher. |
| `androidx.test:core`, `androidx.test:runner`, `androidx.test.ext:junit` (androidTest) | Instrumented DAO and repository tests with an in-memory database. |
| `androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | ViewModels in Compose and `collectAsStateWithLifecycle`. |
| `androidx.navigation:navigation-compose` | Screen navigation (string routes). |
| `androidx.datastore:datastore-preferences` | Stores settings (lead time, sound/vibrate, onboarding flag) and the codes of scheduled alarms. |
| `androidx.work:work-runtime-ktx` | Daily periodic job that keeps the 7-day reminder window rolling forward. |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + plugin `kotlin-serialization` | Pure Kotlin JSON for backup export/import (JVM-testable, unlike `org.json`). |
| `junit:junit` (test) | JVM unit tests. |
| Plugin `org.jlleitschuh.gradle.ktlint` | Runs ktlint formatting checks (`ktlintCheck`). |

## Design notes

- Today screen: timeline = today's blocks plus the tail of yesterday's overnight blocks; "now"/"next" refresh every 30s.
- Day edit works on a draft; Save writes a `DayOverride` (skipped if nothing changed), "reset to template" deletes it.
- `BlockListEditor` (ui/components) is shared by the template and day-edit screens. Each block opens its own bottom
  sheet (start time, duration, type and title) and reports exactly one changed block.
- `ReminderRescheduler` is implemented by `ReminderCoordinator`: cancels the previously stored alarm codes, then registers a
  fresh 7-day plan with `setAlarmClock` (no exact-alarm permission needed; Android shows an alarm icon for it).
- Two notification channels (sound / vibrate) because channel settings cannot change after creation.
- `BootReceiver` handles BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_SET (ACTION_TIME_CHANGED) and TIMEZONE_CHANGED
  and reschedules via `goAsync()`. `ReminderWorker` runs daily (unique periodic work, KEEP).
- Backup file: `{version: 1, template: [...], overrides: [{date, blocks}]}`; times `HH:mm`, dates ISO. Block ids are not
  exported and are reissued on import. Import = read, decode, validate (incl. overlaps) -> preview -> user confirms ->
  one transaction replaces everything and reschedules. Nothing is written if validation fails. Settings are not exported.
- Files are chosen with the Storage Access Framework (`CreateDocument` / `OpenDocument`), so no storage permission.
- POST_NOTIFICATIONS is requested on launch (Android 13+).
- Room: `template_blocks`, `day_overrides` (PK epochDay) and `day_override_blocks` (composite PK epochDay+blockId,
  cascade delete). Time is `HH:mm` text, dates are epochDay Long. `exportSchema = false` (version 1, no migrations yet).
- DAO and repository tests are instrumented (`app/src/androidTest`) and need an emulator or device.
- One daily template (list of `TimeBlock`) applies to every day; a `DayOverride` replaces the template for one date.
- A block has a start time and a duration; it may run past midnight. Overlap checks work on a 24h circle.
- `tools/check.py` is a stdlib-only static check (import order, unused imports, line length, `!!`, Hebrew literals).
  It exists because ktlint could not run in the authoring environment; `ktlintCheck` remains the source of truth.
- Hebrew is the default language; `android:supportsRtl="true"` and the theme forces `LayoutDirection.Rtl`.
- Numbers and times inside Hebrew text: times, ranges and technical paths are wrapped with `isolateLtr()` (U+2066/U+2069)
  so `23:00–06:00` keeps its order. Plain counts (`%1$d דק׳`) are left alone. Dates use the `he-IL` locale.
- Accessibility: icon buttons name their block ("מחיקת הבלוק …"), the duration slider reports its value as text,
  the reminder switch announces on/off, timeline rows are merged into one spoken sentence, and section titles are headings.
- minSdk 26, compile/target SDK 35, JDK 17.

## Verification status

The authoring environment had no Android SDK and no network, so these have **never been run**: `assembleDebug`,
`testDebugUnitTest`, `lintDebug`, `ktlintCheck`, `connectedDebugAndroidTest`. Only `python3 tools/check.py` ran.
Expect to fix a few compile, lint or ktlint findings on the first real build. The Gradle wrapper is also missing:
run `gradle wrapper --gradle-version 8.9` once (or open the project in Android Studio).

## Building without a local toolchain

`.github/workflows/build-apk.yml` builds `app-debug.apk` on GitHub Actions (JDK 17, Android SDK, Gradle 8.9) on every push
and on manual run. Download it from the run's "Artifacts" section. The debug APK is signed with the debug key and installs directly.

## Manual test checklist

Run on a real device or emulator with the system language set to Hebrew, then repeat once with English.

**Layout and RTL**
- [ ] Every screen is right-to-left: titles, back arrows, bottom bar order (Today at the right), switches and chips.
- [ ] Bottom sheets, dialogs and snackbars are right-to-left too.
- [ ] In English system locale the app is still Hebrew and RTL, and dates are Hebrew.
- [ ] Font size 200% and display size "largest": nothing is cut off, buttons stay reachable.
- [ ] Dark mode: contrast is readable on all screens.

**Numbers inside Hebrew text**
- [ ] A time range such as `23:00–06:00` reads left-to-right and is not reversed.
- [ ] Chip labels "התחלה 08:00" and "משך 1 שע׳ ו-30 דק׳" look right; the notification text "(08:00)" has intact parentheses.
- [ ] Import error text shows paths like `overrides[2026-11-13]` unscrambled.

**Template (milestone 4)**
- [ ] Add a block: it appears in the first free slot. With a full day, "no room" is shown.
- [ ] Change start time via sheet; change duration via slider and presets; change type and name.
- [ ] Editing one block never changes another. An overlapping edit is refused with a message naming the other block.
- [ ] A block 23:00 for 7 hours shows "חוצה חצות" and ends at 06:00. Blocks that only touch are allowed.
- [ ] Toggling the reminder switch on one block affects only that block.

**Today and day edit (milestone 5)**
- [ ] Right after midnight the tail of last night's block is the current item. "Now" and "next" update within ~30 seconds.
- [ ] Previous/next day arrows work; "back to today" appears when not on today.
- [ ] Day edit: change a block, Save -> Today shows "יום זה שונה מהתבנית"; the template is unchanged.
- [ ] "איפוס לתבנית" removes the override and the day follows the template again.

**Reminders (milestones 6-7)**
- [ ] Android 13+: the notification permission prompt appears once; denying it does not crash.
- [ ] A reminder set 2 minutes ahead fires on time, with the screen off and in battery saver.
- [ ] Sound mode vs vibrate mode produce the expected behaviour. Lead time 0 says "מתחיל עכשיו".
- [ ] Editing a block, deleting a block, saving or resetting a day, importing, or changing lead time moves/removes the alarms.
- [ ] Reboot the device, change the clock, change the time zone, update the app: reminders are still registered
      (check with `adb shell dumpsys alarm | grep sederyom`).
- [ ] After 24h+ the daily worker keeps a full 7-day window.

**Backup (milestone 8)**
- [ ] Export, then import the same file: the preview counts match; after confirming nothing visibly changed.
- [ ] Import a hand-edited file with a bad time, an unknown type, a duplicate date, overlapping blocks, a non-JSON file
      and a file of a few MB: each is refused with a clear message and the existing data is untouched.
- [ ] Cancelling the preview changes nothing.

**Accessibility (TalkBack on, Hebrew)**
- [ ] Each timeline row is read as one sentence: name, range, duration.
- [ ] Icon buttons are announced with the block name; the slider announces the duration as text, not a raw number.
- [ ] Swiping between section headings works in Settings, Today and in sheets.
- [ ] All touch targets are at least 48dp.

## Milestones

1. [x] AGENTS.md, Gradle skeleton, empty Compose app with a Hebrew theme.
2. [x] Domain models, ValidateBlock, ResolveDayPlan, and the overnight logic, with unit tests.
3. [x] Room entities, DAOs, TypeConverters, and the repository, with DAO tests.
4. [x] Template screen. Each block is edited independently, and each block opens a bottom sheet for its time or duration.
5. [x] Today screen (timeline and next item) and day-edit screen. Saving writes a DayOverride, and "reset to template" deletes it.
6. [x] Reminders: AlarmScheduler using setAlarmClock, AlarmReceiver, the notification channel, and ComputeReminderPlan with unit tests for the 7-day plan.
7. [x] Reschedule on BOOT_COMPLETED, MY_PACKAGE_REPLACED, TIME_SET, TIMEZONE_CHANGED, and the daily WorkManager job.
8. [x] Settings (lead time, sound or vibrate) and JSON export and import with validation and a preview.
9. [x] Hebrew polish: RTL checks, numbers inside Hebrew text, content descriptions for accessibility, and a manual test checklist in AGENTS.md.

Workflow: one commit per milestone, then stop, summarize (changes, commands run, results), and wait for review.

## Definition of done

For each milestone: `assembleDebug`, `testDebugUnitTest`, and `lintDebug` pass, AGENTS.md matches the code,
and the commit is clean.
