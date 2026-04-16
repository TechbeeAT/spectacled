# 🚀 Compose Multiplatform Template — AGP 9 + Kotlin 2.3 + Gradle 9 (Ready to Use)

A production-ready **Compose Multiplatform (KMP)** template fully migrated to **AGP 9.0.1**, **Kotlin 2.3**, and **Gradle 9.1**. Build apps for **Android, iOS, Desktop (Windows/macOS/Linux), Web (JS + Wasm), and Server** — all from one codebase.

> **Clone → Rename → Build → Ship.** No JDK install. No Gradle install. No version conflicts.

---

## 📋 What's Inside

| Module | Purpose | Platforms |
|---|---|---|
| `composeApp` | Shared UI with Compose Multiplatform | Android, iOS, Desktop, Web |
| `shared` | Shared business logic (no UI) | All platforms |
| `androidApp` | Android entry point | Android |
| `iosApp` | iOS entry point (Xcode project) | iOS |
| `server` | Ktor backend server | JVM |

---

## 🛠️ Tech Stack & Versions

| Tool                  | Version                                    |
|-----------------------|--------------------------------------------|
| Kotlin                | `2.3.0`                                    |
| Compose Multiplatform | `1.10.0`                                   |
| Android Gradle Plugin | `9.0.1`                                    |
| Gradle                | `9.1.0` (bundled — no install needed)      |
| JDK                   | `17` (auto-downloaded — no install needed) |
| Ktor                  | `3.3.3`                                    | 
| Max Android SDK       | `35` (Android 16.0+)                       |
| Min Android SDK       | `24` (Android 7.0+)                        |

### ⚡ No Java / Gradle Installation Needed!

This template uses **Gradle Wrapper** + **Foojay Toolchain**. When you build for the first time, everything is automatically downloaded. You only need **Android Studio** installed.

---

## 🏁 Getting Started

### Step 1 — Clone This Repo

```bash
git clone https://github.com/parmarsanket/Compose-Multiplatform-Template-AGP-9.git
```

### Step 2 — Rename to Your Project (See section below)

### Step 3 — Open in Android Studio → Click ▶️ Run

---

## ✏️ How to Change Project Name & Package Name

This template includes **rename scripts** that do everything automatically.

### 🪟 On Windows

```
1. Open the project folder in File Explorer
2. Double-click  rename_project.bat
3. A window opens showing:

   Detected Existing Project Name:  spectacled
   Detected Existing Package Name:  at.techbee.spectacled

4. Type your NEW project name, press Enter
5. Type your NEW package name, press Enter
6. Done! Open in Android Studio and build.
```

**Example input:**
```
Enter NEW Project Name: MyAwesomeApp
Enter NEW Package Name: com.mycompany.awesomeapp
```

### 🍎 On macOS / 🐧 On Linux

```bash
# Make the script executable (only once):
chmod +x rename_project.sh

# Run it:
./rename_project.sh
```

It will show:
```
Detected Existing Project Name: spectacled
Detected Existing Package Name: at.techbee.spectacled

Enter NEW Project Name: MyAwesomeApp
Enter NEW Package Name: com.mycompany.awesomeapp
```

### What the script changes automatically:

| What | Where |
|---|---|
| `rootProject.name` | `settings.gradle.kts` |
| `namespace` | All `build.gradle.kts` files |
| `applicationId` | `androidApp/build.gradle.kts` |
| `package` declarations | All `.kt` files |
| `import` statements | All `.kt` files |
| App display name | `strings.xml` |
| iOS bundle ID | `Config.xcconfig` |
| iOS product name | `Config.xcconfig` |
| Desktop package name | `composeApp/build.gradle.kts` |
| Server group name | `server/build.gradle.kts` |
| Kotlin source folders | Moved to match new package |

### After renaming, in Android Studio:

```
File → Sync Project with Gradle Files
Build → Clean Project
Build → Rebuild Project
```

---

## 📱 How to Build & Run on Each Platform

---

### 🤖 Android

**What you need:** Android Studio (Narwhal 2025.1 or newer)

**Steps:**
```
1. Open project in Android Studio
2. Wait for Gradle sync to finish (first time takes a few minutes)
3. Select an emulator or connected device from the toolbar
4. Click ▶️ Run
```

**Or from terminal:**
```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# APK location:
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

**Or build release APK:**
```bash
./gradlew :androidApp:assembleRelease
```

---

### 🖥️ Desktop (Windows / macOS / Linux)

**What you need:** Nothing extra! Just the project.

**Run from Android Studio:**
```
1. In the Run Configurations dropdown (top toolbar), select "composeApp"
2. Click ▶️ Run
```

**Run from terminal:**
```bash
# Launch the desktop app directly
./gradlew :composeApp:run
```

**Create native installer:**
```bash
# Windows → .msi installer
./gradlew :composeApp:packageMsi

# macOS → .dmg installer
./gradlew :composeApp:packageDmg

# Linux → .deb package
./gradlew :composeApp:packageDeb
```

**Installer output location:**
```
composeApp/build/compose/binaries/
```

---

### 🌐 Web (JavaScript)

**What you need:** Nothing extra! Just a modern browser.

**Run from terminal:**
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

**What happens:**
```
1. Gradle compiles your Kotlin code to JavaScript
2. A dev server starts automatically
3. Opens your browser at  http://localhost:8080
4. You can see your app running in the browser!
```

**Build production bundle:**
```bash
./gradlew :composeApp:jsBrowserProductionWebpack

# Output: composeApp/build/dist/js/productionExecutable/
```

---

### 🌐 Web (WebAssembly / Wasm)

**What you need:** A modern browser (Chrome 119+, Firefox 120+, Safari 18.2+)

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

> ⚠️ **Note:** Wasm support is experimental. For production web apps, use the JS target above.

---

### 🍎 iOS

**What you need:** macOS + Xcode 16+

**Option A — Build from Xcode (recommended):**
```
1. Open  iosApp/iosApp.xcodeproj  in Xcode
2. Select a simulator (e.g. iPhone 16)
3. Click ▶️ Run
```

**Option B — Build from Android Studio:**
```
1. Install the "Kotlin Multiplatform" plugin in Android Studio
   (Settings → Plugins → search "Kotlin Multiplatform")
2. Select the iOS run configuration from the dropdown
3. Click ▶️ Run
```

**For physical device:** Edit `iosApp/Configuration/Config.xcconfig`:
```
TEAM_ID=YOUR_APPLE_TEAM_ID
```

---

### ⚙️ Server (Ktor Backend)

**What you need:** Nothing extra!

```bash
./gradlew :server:run
```

**What happens:**
```
Server starts at  http://localhost:8080
```

---

## 📁 Project Structure

```
📦 project-root/
 ┣ 📂 androidApp/              ← Android app entry point
 ┃ ┣ 📂 src/main/kotlin/       ← MainActivity.kt
 ┃ ┗ 📄 build.gradle.kts
 ┃
 ┣ 📂 composeApp/              ← Shared Compose UI (★ main code here)
 ┃ ┣ 📂 src/
 ┃ ┃ ┣ 📂 commonMain/          ← ★ Shared UI code (App.kt)
 ┃ ┃ ┣ 📂 androidMain/         ← Android-specific UI
 ┃ ┃ ┣ 📂 iosMain/             ← iOS-specific UI
 ┃ ┃ ┣ 📂 jvmMain/             ← Desktop entry point
 ┃ ┃ ┣ 📂 jsMain/              ← Web JS entry point
 ┃ ┃ ┗ 📂 wasmJsMain/          ← Web Wasm entry point
 ┃ ┗ 📄 build.gradle.kts
 ┃
 ┣ 📂 shared/                  ← Shared business logic (no UI)
 ┃ ┣ 📂 src/
 ┃ ┃ ┣ 📂 commonMain/          ← ★ Platform-independent logic
 ┃ ┃ ┣ 📂 androidMain/         ← Android expect/actual
 ┃ ┃ ┣ 📂 iosMain/             ← iOS expect/actual
 ┃ ┃ ┣ 📂 jvmMain/             ← JVM expect/actual
 ┃ ┃ ┗ 📂 jsMain/              ← JS expect/actual
 ┃ ┗ 📄 build.gradle.kts
 ┃
 ┣ 📂 iosApp/                  ← iOS Xcode project
 ┃ ┣ 📂 iosApp/                ← Swift source files
 ┃ ┗ 📂 Configuration/         ← Config.xcconfig (Team ID here)
 ┃
 ┣ 📂 server/                  ← Ktor backend server
 ┃
 ┣ 📂 gradle/
 ┃ ┣ 📄 libs.versions.toml     ← ★ All dependency versions here
 ┃ ┗ 📂 wrapper/               ← Gradle wrapper (auto-downloads Gradle)
 ┃
 ┣ 📄 rename_project.bat       ← 🪟 Windows rename script
 ┣ 📄 rename_project.sh        ← 🍎🐧 macOS/Linux rename script
 ┣ 📄 rename_project.ps1       ← PowerShell logic (used by .bat)
 ┗ 📄 gradlew / gradlew.bat    ← ★ Always use this, NOT system Gradle
```

---

## ✏️ Where to Write Your Code

| What you want to do | Put code here |
|---|---|
| Shared UI screens & components | `composeApp/src/commonMain/kotlin/` |
| Android-only code | `androidApp/src/main/kotlin/` |
| iOS-only Swift code | `iosApp/iosApp/` |
| Business logic (all platforms) | `shared/src/commonMain/kotlin/` |
| Platform-specific logic | `shared/src/<platform>Main/kotlin/` |
| Server API endpoints | `server/src/main/kotlin/` |
| Images & shared resources | `composeApp/src/commonMain/composeResources/` |
| Android resources (xml, drawables) | `androidApp/src/main/res/` |

---

## 📝 Quick Gradle Commands Reference

| Command | What it does |
|---|---|
| `./gradlew :androidApp:assembleDebug` | Build Android debug APK |
| `./gradlew :androidApp:assembleRelease` | Build Android release APK |
| `./gradlew :composeApp:run` | Run Desktop app |
| `./gradlew :composeApp:packageMsi` | Create Windows .msi installer |
| `./gradlew :composeApp:packageDmg` | Create macOS .dmg installer |
| `./gradlew :composeApp:packageDeb` | Create Linux .deb package |
| `./gradlew :composeApp:jsBrowserDevelopmentRun` | Run Web app (JS) in browser |
| `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` | Run Web app (Wasm) in browser |
| `./gradlew :server:run` | Run Ktor server |
| `./gradlew allTests` | Run all tests |
| `./gradlew clean` | Delete all build outputs |
| `./gradlew --stop` | Stop all Gradle daemons |

> 💡 **Always use `./gradlew`** (not `gradle`). This uses the bundled Gradle version and avoids version mismatch errors.

---

## 🔧 Troubleshooting

### ❌ "SDK location not found"

Android Studio creates `local.properties` automatically. If missing, create it manually:

```properties
# Windows
sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk

# macOS
sdk.dir=/Users/YOUR_NAME/Library/Android/sdk

# Linux
sdk.dir=/home/YOUR_NAME/Android/Sdk
```

### ❌ "Unsupported class file major version" or JDK errors

```bash
./gradlew clean
./gradlew --stop
./gradlew :androidApp:assembleDebug
```

### ❌ Gradle version errors

**Never use system Gradle.** Always use the bundled wrapper:

```bash
# ✅ Correct
./gradlew :androidApp:assembleDebug

# ❌ Wrong (will cause version errors)
gradle :androidApp:assembleDebug
```

### ❌ iOS "Framework not found"

```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### ❌ First build is very slow

This is normal! The first build downloads:
- Gradle `9.1.0` 
- JDK 17 
- All dependencies 

Subsequent builds are much faster (cached).

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.