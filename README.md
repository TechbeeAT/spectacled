# Spectacled

**Open, standards-based journals, notes, and tasks — synced over CalDAV, on your own server.**

Spectacled is a family of apps for Android, iOS, Web, and Desktop, built on the `VJOURNAL` and `VTODO` components of the iCalendar standard and synced over CalDAV. No proprietary backend, no account required — your data lives on whatever CalDAV server you already trust (Nextcloud, Radicale, or anything else that speaks CalDAV), and Spectacled is just the client.

It's one shared [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) / [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) codebase, built into three apps:

| App                     | Uses       | For                                    |
|-------------------------|------------|----------------------------------------|
| **Spectacled Journals** | `VJOURNAL` | Dated journal entries                  |
| **Spectacled Notes**    | `VJOURNAL` | Free-form notes                        |
| **Spectacled Tasks**    | `VTODO`    | To-dos, with status/priority/due dates |

---

## 📸 Screenshots

<!--
TODO: add real screenshots before publishing. Suggested layout - one row per app,
one column per platform. Drop images under docs/screenshots/ and reference them below.

| | Android | iOS | Web | Desktop |
|---|---|---|---|---|
| **Journals** | ![](docs/screenshots/journals-android.png) | ![](docs/screenshots/journals-ios.png) | ![](docs/screenshots/journals-web.png) | ![](docs/screenshots/journals-desktop.png) |
| **Notes**    | ![](docs/screenshots/notes-android.png)    | ![](docs/screenshots/notes-ios.png)    | ![](docs/screenshots/notes-web.png)    | ![](docs/screenshots/notes-desktop.png)    |
| **Tasks**    | ![](docs/screenshots/tasks-android.png)    | ![](docs/screenshots/tasks-ios.png)    | ![](docs/screenshots/tasks-web.png)    | ![](docs/screenshots/tasks-desktop.png)    |
-->

*Screenshots coming soon.*

---

## Why

The iCalendar standard ([RFC 5545](https://www.rfc-editor.org/rfc/rfc5545)) has included the `VJOURNAL` component for journal/note entries since 1998, but almost nothing actually supports it: [KOrganizer](https://apps.kde.org/korganizer/) and [GNOME Evolution](https://gitlab.gnome.org/GNOME/evolution) offer rudimentary support on Linux only, and [jtx Board](https://github.com/TechbeeAT/jtxBoard) — an earlier project by the same author — was the first mobile app to support it fully, but relies on [DAVx5](https://www.davx5.com/) for sync and only exists on Android.

Spectacled aims to close that gap: a single, standards-based app for journals, notes, and tasks that works the same way on Android, iOS, Web, and Desktop, talks CalDAV directly, and leaves your data exactly where you put it. No vendor lock-in, no proprietary sync protocol — just iCalendar and CalDAV, the same open standards your calendar app probably already uses.

## Features

- Connect to any CalDAV server and browse its collections
- Create, edit, and delete journal/note/task entries, with summary, description, categories, and classification
- Search and filter your entries
- Attachments (inline or linked)
- Parent/child relationships between entries (`RELATED-TO`)
- Sync runs in the background and locally caches everything, so the app stays usable offline
- Light/dark theme, one consistent UI across every platform

## 📥 Download

Every available build is listed on the download page:
**[spectacled.techbee.at/download](https://spectacled.techbee.at/download/)**

---

## 🚀 Getting started

New to Spectacled? Connecting your first server takes about five minutes.

**[📖 Getting started guide](docs/getting-started.md)** — the whole setup in
writing: choosing a provider, connecting your account, creating a folder, and
writing your first entry, plus a troubleshooting table for when a server doesn't
behave.

**[🎬 Watch the walkthrough](https://youtu.be/lu-Grqnp4no)** — the same thing as
a four-minute video.

One thing worth knowing before you pick a server: notes and journals are stored
as `VJOURNAL`, which far fewer CalDAV servers implement than the calendar and
task parts of the standard. Nextcloud-based servers handle it; several
CalDAV-capable mail providers don't, and can only be used with Spectacled Tasks.
The app suggests providers that are known to work, and the guide covers this in
more detail.

## Support the project

Spectacled is, and will stay, open source. Maintaining it across five platforms (Android, iOS, Web, Desktop, plus the shared backend logic) is ongoing work — if it's useful to you, consider supporting it:

- [GitHub Sponsors](https://github.com/sponsors/patrickunterwegs)
- [Liberapay](https://liberapay.com/techbee.at)
- [PayPal](https://www.paypal.com/ncp/payment/XB7HH4BWXYFKJ)

(See [`.github/FUNDING.yml`](.github/FUNDING.yml) for the current list.)

---

## 🛠️ Tech Stack

Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) and [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/). Local storage and sync use [SQLDelight](https://sqldelight.github.io/sqldelight/) (SQLite everywhere, including a persisted IndexedDB-backed store on Web), networking is [Ktor](https://ktor.io/), and dependency injection is [Koin](https://insert-koin.io/).

Exact versions aren't listed here to avoid this file going stale — see [`gradle/libs.versions.toml`](gradle/libs.versions.toml) for the current pinned versions of everything, including the supported Android SDK range.

### ⚡ No Java / Gradle installation needed

This project uses the **Gradle Wrapper** and the **Foojay toolchain resolver**. The first build downloads Gradle and a JDK automatically — you only need Android Studio (for Android/Desktop/Web) and, for iOS, Xcode.

---

## 📁 Project Structure

```
📦 spectacled/
 ┣ 📂 shared/                  ← Almost everything lives here: UI screens, CalDAV sync,
 ┃                                iCalendar parsing, local database, repositories — used
 ┃                                by every app and every platform
 ┃
 ┣ 📂 composeJournalsApp/      ← Platform entry point for the Journals app
 ┣ 📂 composeNotesApp/         ← Platform entry point for the Notes app
 ┣ 📂 composeTasksApp/         ← Platform entry point for the Tasks app
 ┃    (each targets Android, iOS, Desktop/JVM, Web/JS, and Web/Wasm; each is a thin
 ┃     wrapper that picks a variant and hands off to shared/'s UI)
 ┃
 ┣ 📂 androidJournalsApp/      ← Android entry point for Journals
 ┣ 📂 androidNotesApp/         ← Android entry point for Notes
 ┣ 📂 androidTasksApp/         ← Android entry point for Tasks
 ┃
 ┣ 📂 iosApp/                  ← iOS Xcode projects (kept out of the repo root so
 ┃    ┣ 📂 iosJournalsApp/       Android Studio treats the root as a pure Gradle
 ┃    ┣ 📂 iosNotesApp/          project; the Kotlin Multiplatform plugin still
 ┃    ┣ 📂 iosTasksApp/          discovers these for iOS run configurations)
 ┃    ┗ 📄 spectacled.xcworkspace  ← Xcode workspace combining all three iOS apps
 ┃
 ┣ 📂 server/                  ← Ktor CORS proxy for the Web build (see server/README.md).
 ┃                                Only the browser needs it; native apps talk CalDAV directly.
 ┃
 ┗ 📂 gradle/
    ┗ 📄 libs.versions.toml    ← ★ All dependency versions live here
```

Why three separate apps instead of one with a mode switch? Each is published as its own listing (its own icon, name, and store page), which matches how people actually think about journals vs. notes vs. tasks — but all three share every line of business logic *and* almost all of their UI, which both live in `shared/`. `compose<Variant>App` is just the per-platform entry point: it picks a variant and wires it up for Android/iOS/Desktop/Web. If you're contributing a fix, it almost always belongs in `shared/`.

## ✏️ Where to Write Your Code

| What you want to do                                                  | Put code here                                |
|----------------------------------------------------------------------|----------------------------------------------|
| UI screens, CalDAV sync, iCalendar parsing, database, business logic | `shared/src/commonMain/kotlin/`              |
| Platform-specific logic (e.g. credential storage, file access)       | `shared/src/<platform>Main/kotlin/`          |
| Platform entry-point wiring for one app (rarely needed)              | `compose<Variant>App/src/commonMain/kotlin/` |
| Static web assets (favicon, index.html, the sql.js worker)           | `compose<Variant>App/src/webMain/resources/` |
| Android-only entry point code                                        | `android<Variant>App/src/main/kotlin/`       |
| iOS-only Swift code                                                  | `ios<Variant>App/ios<Variant>App/`           |
| Shared strings/images (used by all 3 apps)                           | `shared/src/commonMain/composeResources/`    |

---

## 🏁 Building & Running Each Platform

Each command below works for any of the three apps — just swap `composeJournalsApp` / `androidJournalsApp` / `iosJournalsApp` for the `Notes` or `Tasks` equivalent.

### 🤖 Android

```bash
# Debug build
# Requires Android SDK available on the machine. Simplest way is to setup
# Android Studio. As the installer sets up SDK, note the path.
# Create a local.properties file in the project root directory with the content
# `sdk.dir=<absolute path of SDK location>`
./gradlew :androidJournalsApp:assembleDebug
# APK: androidJournalsApp/build/outputs/apk/debug/

# Or just open the project in Android Studio, select an androidJournalsApp run
# configuration and a device/emulator, and click ▶️ Run.
```

### 🖥️ Desktop (Windows / macOS / Linux)

```bash
# Run directly
./gradlew :composeJournalsApp:run

# Native installers
./gradlew :composeJournalsApp:packageMsi   # Windows
./gradlew :composeJournalsApp:packageDmg   # macOS
./gradlew :composeJournalsApp:packageDeb   # Linux

# Output: composeJournalsApp/build/compose/binaries/
```

### 🌐 Web (JavaScript)

```bash
./gradlew :composeJournalsApp:jsBrowserDevelopmentRun
# Opens http://localhost:8080 with hot reload

# Production bundle:
./gradlew :composeJournalsApp:jsBrowserProductionWebpack
```

### 🌐 Web (WebAssembly)

```bash
./gradlew :composeJournalsApp:wasmJsBrowserDevelopmentRun
```

Requires a recent browser (Chrome 119+, Firefox 120+, Safari 18.2+).

> **⚠️ The Web build needs the CORS proxy.** Browsers block cross-origin WebDAV requests, so the
> web app routes CalDAV traffic through the small Ktor proxy in [`server/`](server/README.md), which
> adds the required CORS headers (native apps talk to CalDAV directly and don't need it). Run it
> locally with `./gradlew :server:run` and point **Settings → Proxy server** at
> `http://localhost:8088`. For hosting, **self-host your own** instance (a shared proxy can see your
> credentials in transit) — see [`server/README.md`](server/README.md) for Docker/Fly.io setup and
> the trust caveats.

### 🍎 iOS

Requires macOS + Xcode 16+.

```
1. Open iosApp/spectacled.xcworkspace (or iosApp/iosJournalsApp/iosJournalsApp.xcodeproj
   directly) in Xcode.
2. Select a simulator or device.
3. Click ▶️ Run.
```

For a physical device, set your Apple Developer Team in `ios<Variant>App/Configuration/Config.xcconfig`:

```
DEVELOPMENT_TEAM=YOUR_APPLE_TEAM_ID
```

### 📝 Useful Gradle commands

| Command                      | What it does                                                   |
|------------------------------|----------------------------------------------------------------|
| `./gradlew :shared:allTests` | Run the shared module's test suite                             |
| `./gradlew :server:run`      | Run the Web CORS proxy locally on `http://localhost:8088`      |
| `./gradlew :server:test`     | Run the proxy's test suite                                     |
| `./gradlew clean`            | Delete all build outputs                                       |
| `./gradlew --stop`           | Stop all Gradle daemons (useful after a bad incremental build) |

> 💡 Always use `./gradlew` (the wrapper), not a system-installed `gradle` — it pins the exact Gradle version this project needs.

---

## 🌍 Translations

Spectacled is translated with [Weblate](https://weblate.org/), a libre web-based
translation platform — thanks to Weblate for hosting our translations free of charge for
open-source projects.

Help translate the apps into your language at
**[hosted.weblate.org/projects/spectacled](https://hosted.weblate.org/projects/spectacled/)**.
No development setup and no Git knowledge required: sign in, pick your language, and start
translating in the browser. Weblate opens the pull requests against this repository for you.

[![Translation status](https://hosted.weblate.org/widget/spectacled/multi-auto.svg)](https://hosted.weblate.org/engage/spectacled/)

The source strings live in [`shared/src/commonMain/composeResources/values/strings.xml`](shared/src/commonMain/composeResources/values/strings.xml)
and are shared by all three apps on every platform. Translations land in sibling
`values-<lang>/strings.xml` directories — please don't edit those by hand, let Weblate
manage them so nothing gets overwritten on the next sync.

---

## Contributing

Issues and pull requests are welcome — this is an open-source project and stays that way. A few starting points:

- **Bug reports / feature requests:** open a [GitHub Issue](../../issues)
- **Questions / discussion:** [GitHub Discussions](../../discussions)
- **Translations:** join us on [Weblate](https://hosted.weblate.org/projects/spectacled/) — see [Translations](#-translations) above
- **Chat / updates:** Mastodon handle coming soon

If you're touching CalDAV sync or iCalendar parsing, start in `shared/src/commonMain/kotlin/at/techbee/spectacled/screens/core/` — that's where the platform-independent logic lives, and it's the part every app and every platform depends on.

## License

This project is licensed under the Apache License 2.0 — see [LICENSE](LICENSE) for details.
