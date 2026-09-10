# CLAUDE.md

Orientation for Claude Code working in this repo. Keep it short and factual — it is
loaded into every session, so it earns its place only by saving rediscovery.
`README.md` stays the human-facing document; this file does not duplicate it.

## What this is

One Kotlin Multiplatform / Compose Multiplatform codebase (`shared/`) built into three
apps — **Journals** and **Notes** (`VJOURNAL`), **Tasks** (`VTODO`) — each shipping on
Android, iOS, Desktop/JVM, Web/JS and Web/Wasm. Data is stored as iCalendar and synced
over CalDAV against the user's own server. There is no backend of ours except the
optional CORS proxy in `server/`, which only the browser builds need.

## The one rule that decides most changes

**Business logic and UI go in `shared/src/commonMain/kotlin/`.** The
`compose<Variant>App` / `android<Variant>App` / `ios<Variant>App` modules are thin entry
points that pick a variant and hand off. Only reach for them when the change is
genuinely per-platform (`shared/src/<platform>Main/`, via `expect`/`actual`) or
entry-point wiring.

## Layout inside `shared/src/commonMain/kotlin/at/techbee/spectacled/`

```
SpectacledApp.kt          root composable + navigation
SpectacledVariant.kt      the enum that encodes every Journals/Notes/Tasks difference
DeepLinkHandler.kt
theme/
screens/
  core/                   everything shared across features — start here for sync/iCalendar
    data/                 webdav/ (CalDAV client), ics/ (iCalendar primitives),
                          repository/ (impls), ai/ + claude/, CredentialsStore,
                          HttpClientFactory, UserAppPreferencesStore
    domain/               IcalEntry, Calendar, Attachment, … + repository/ interfaces
    mapper/               ics/ (parse + serialize), dto/ (db rows ⇄ domain)
    koin/Modules.kt       DI graph; `expect val platformModule` per target
    presentation/         shared composables, Markdown helpers
    SyncCoordinator.kt    sync orchestration
  list/ details/ account/ about/    feature packages
```

Feature packages follow `data/ · domain/ · presentation/`, and screens follow a
consistent MVI-ish naming: `XState`, `XAction`, `XViewModel`, `XScreenRoot` (wires the
ViewModel), `XScreen` (stateless UI), plus `components/` and, for the list screen, one
per-variant entry (`ListScreenJournals/Notes/Tasks.kt`).

## Conventions worth knowing before editing

- **Per-app differences belong in `SpectacledVariant`** — add a property to the enum
  rather than branching on which app is running.
- **DI:** Koin. New repository/use case/ViewModel → register it in
  `screens/core/koin/Modules.kt`; platform-specific bindings go in each target's
  `platformModule` actual.
- **Repositories:** interface in `core/domain/repository/`, implementation in
  `core/data/repository/`.
- **Database:** SQLDelight (`shared/src/commonMain/sqldelight/…`). Schema changes need a
  new numbered migration (`13.sqm` next) alongside the `.sq` edit — never edit an
  existing `.sqm`.
- **Strings:** source strings only in
  `shared/src/commonMain/composeResources/values/strings.xml`. Never hand-edit
  `values-<lang>/strings.xml` — Weblate owns those.
- **Dependencies:** versions only in `gradle/libs.versions.toml`.
- **iOS apps are not Gradle modules.** They are standalone Xcode projects under
  `iosApp/` consuming the shared framework; do not add them to `settings.gradle.kts`
  (there is a comment there explaining why).

## Verifying a change

CI (`.github/workflows/unit-tests.yml`) runs exactly:

```bash
./gradlew :shared:allTests \
  :androidJournalsApp:lintDebug :androidNotesApp:lintDebug :androidTasksApp:lintDebug
```

Run that before pushing. Tests live in `shared/src/commonTest/` and mirror the main
source packages. Other useful targets (swap the variant freely):

```bash
./gradlew :composeJournalsApp:run                          # desktop
./gradlew :composeJournalsApp:wasmJsBrowserDevelopmentRun  # web, needs :server:run for CalDAV
./gradlew :androidJournalsApp:assembleDebug                # needs local.properties → sdk.dir
./gradlew :server:test
```

Always use `./gradlew`, never a system `gradle`. Android builds need `local.properties`
with `sdk.dir=<Android SDK path>`; without it, skip the Android tasks rather than
guessing a path.

## Where to look first, by kind of change

| Change | Start at |
|---|---|
| CalDAV request/response behaviour | `core/data/webdav/` |
| iCalendar parsing or serialization | `core/mapper/ics/`, `core/data/ics/` |
| Sync scheduling, conflict or error handling | `core/SyncCoordinator.kt`, `core/domain/CalendarSync*.kt` |
| Entry list, filters, sorting, layouts | `screens/list/presentation/` |
| Entry editing screen | `screens/details/presentation/` |
| Server/account setup, credentials | `screens/account/`, `core/data/CredentialsStore.kt` |
| Persistence / schema | `shared/src/commonMain/sqldelight/` |
| Something that differs per app | `SpectacledVariant.kt` |
