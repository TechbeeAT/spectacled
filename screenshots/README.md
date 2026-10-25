# Store screenshots

Renders the real screens of all three apps off-screen on the JVM, at App Store and Play Store
sizes. You don't need an emulator, a simulator or a CalDAV server.

```bash
./gradlew :screenshots:generateScreenshots                 # all apps
./gradlew :screenshots:generateScreenshotsTasks \
  -Pscreenshots.locales=en,de \
  -Pscreenshots.themes=light \
  -Pscreenshots.devices=ios-phone,android-phone \
  -Pscreenshots.scenes=01,02
```

Output: `screenshots/build/screenshots/<app>/<locale>/<device>/<scene>-<theme>.png`. These are
plain screen contents, ready to be framed by AppLaunchFlow or any other tool.

Each filter takes comma-separated values and matches a device or scene by the start of its name.
Defaults: locale `en`, both themes, all devices, all scenes.

## How it works

- **Separate module, not a test.** `:shared:allTests` and CI never run it.
- **Isolated data.** Each run gets a fresh `user.home` under `build/screenshots-home/` with its
  own database, preferences and a fake account on `dav.example.com`. Credentials go to the
  software key vault, so your real desktop app data and the OS keychain are never touched.
- **Sample data.** Entries come from `sample-data/screenshot_entries.sql`, filtered by uid prefix
  (`sample-journal-`, `sample-note-`, `sample-task-`). Their dates are shifted so that the file's
  "today" (2026-07-16) becomes the day of the run, which keeps "due tomorrow" due tomorrow.
- **Real ViewModels.** Each scene gets fresh ones and renders the same `*ScreenRoot` composables
  the app uses. Wide devices get the app's two-pane layout.

## Adding or changing a screenshot

Edit `src/main/kotlin/.../Scenes.kt`. A scene opens a screen and then sends it actions, the same
ones a tap sends. That's how sheets, dialogs, filters and sort orders are opened:

```kotlin
Scene("03_status") {
    openDetails("sample-task-plan-japan")   // uid from screenshot_entries.sql
    details(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.TASK_STATUS_PICKER))
}
```

Available calls: `openAccounts()`, `openList()`, `openDetails(uid)`, and then `accounts(...)`,
`list(...)` and `details(...)` with any `AccountListAction`, `ListAction` or `DetailsAction`. To add
a device size, edit `Device.kt`.

## Limits

- There's no system chrome (status bar, home indicator) and the UI is the same on iOS and
  Android, as it is in the app.
- Emoji and missing glyphs fall back to the fonts installed on the machine that renders. Render
  on a Mac for Apple emoji.
- UI held only in composable state (a dropdown menu, for example) can't be opened from a scene.
  Anything driven by a ViewModel action can.
