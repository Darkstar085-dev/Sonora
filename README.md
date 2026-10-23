<div align="center">

<p align="center">
  <img src=".github/assets/sonora_banner.png" alt="Sonora logo" width="70%">
</p>

A beautiful, privacy-friendly music player for Android, built around your local library.

<p align="center">
  <a href="https://github.com/Darkstar085/Sonora">
    <img src="https://img.shields.io/badge/Android-API%2029%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android API 29+">
  </a>
  <a href="https://github.com/JetBrains/kotlin/releases/tag/v2.4.10">
    <img src="https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 2.4.10">
  </a>
  <a href="https://developer.android.com/compose">
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  </a>
  <a href="https://m3.material.io/">
  <img src="https://img.shields.io/badge/Material%203-Expressive-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material 3 Expressive">
</a>
  <a href="https://github.com/Darkstar085/Sonora/releases">
    <img src="https://img.shields.io/github/v/release/Darkstar085/Sonora?style=for-the-badge&label=Latest%20Release&color=FF6F00" alt="Latest Release">
  </a>
  <a href="https://github.com/Darkstar085/Sonora/releases">
    <img src="https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/Darkstar085/Sonora/main/.github/badges/downloads.json&style=for-the-badge&label=APK%20Downloads" alt="APK Downloads">
  </a>
  <a href="https://github.com/Darkstar085/Sonora/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/Darkstar085/Sonora?style=for-the-badge&color=E91E63" alt="MIT License">
  </a>
</p>

</div>

---

## About

Sonora is a local-first music player designed to keep your music experience focused and personal.

It scans the music stored on your device, organizes your collection into songs, albums, artists and
playlists, and provides a modern Android playback experience without turning the app into a
streaming service.

## Screenshots

### Getting Started

<p align="center">
  <img src=".github/assets/onboarding-welcome.png" width="31%" alt="Sonora welcome screen">
  <img src=".github/assets/onboarding-library.png" width="31%" alt="Sonora library onboarding">
  <img src=".github/assets/onboarding-permission.png" width="31%" alt="Sonora permission onboarding">
</p>

### Home

<p align="center">
  <img src=".github/assets/home_idle.png" width="31%" alt="Sonora Home with playback idle">
  <img src=".github/assets/home_play.png" width="31%" alt="Sonora Home during playback">
  <img src=".github/assets/home_recent.png" width="31%" alt="Sonora recently added and listening history">
</p>

### Library

<p align="center">
  <img src=".github/assets/songs.png" width="31%" alt="Sonora songs">
  <img src=".github/assets/albums.png" width="31%" alt="Sonora albums">
  <img src=".github/assets/artists.png" width="31%" alt="Sonora artists">
</p>

### Playback

<p align="center">
  <img src=".github/assets/nowplaying.png" width="31%" alt="Sonora Now Playing screen">
  <img src=".github/assets/nowplaying_more.png" width="31%" alt="Sonora Now Playing additional controls">
  <img src=".github/assets/lyrics.png" width="31%" alt="Sonora lyrics">
</p>

### Settings & Personalization

<p align="center">
  <img src=".github/assets/settings.png" width="31%" alt="Sonora settings">
  <img src=".github/assets/settings2.png" width="31%" alt="Sonora more settings">
  <img src=".github/assets/about.png" width="31%" alt="Sonora About screen">
</p>


---

## Features

### Local Music Library

- Scan and browse music stored on the device, with song, album, and artist detail pages
- Search songs by title, artist, and album
- Create and manage playlists, mark favorites, and sort library collections
- Home overview for recently added tracks, listening history, and recently played music
- Exclude selected folders from the library
- Display embedded album artwork and local audio metadata

### Playback

- Media3-powered playback with queue, previous/next, shuffle, and repeat controls
- Gapless playback and adjustable crossfade transitions
- Optional resume playback, playback-position memory, and listening history
- Optional volume normalization and keep-screen-awake setting
- Playback speed controls, background playback, and system media controls
- Media notification and a 4×2 home-screen widget with artwork and playback controls
- Open audio files directly from compatible file managers

### Lyrics

- Fetch lyrics from LRCLIB, with fast timeouts and targeted matching
- Cache lyrics on disk and show cached results immediately while refreshing online in the background
- Fall back to matching local `.lrc` sidecar files and lyrics embedded in audio tags
- Parse synced, enhanced word-timestamped, and plain-text lyrics

### Metadata

- Inspect track tags and audio-format information
- Edit title, artist, album artist, genre, year, track/disc number, composer, comments, grouping,
  lyrics, copyright, and BPM
- Add, replace, or remove embedded album artwork

### Themes & Personalization

- Follow the system theme or choose light, dark, and pure-black dark modes
- Use Android dynamic color with selectable palette styles
- Choose solid accent colors or gradient presets, including custom gradient colors
- Adjust hue and personalize the look of the interface

### Android Integration

- First-launch onboarding and guided media-permission setup
- Battery-optimization exemption prompt to help support reliable background playback
- Manage media, notification, metadata-editing, and related permissions
- WorkManager-based app-update checks, verified update installation, and update notifications
- Theme-aware settings and dialogs throughout the app

---

## Downloads

Get the latest available Sonora build from [**GitHub
Releases**](https://github.com/Darkstar085/Sonora/releases/latest).

For development builds, clone the repository and build the debug APK locally using the instructions
below.

---

## Design

Sonora is built with **Jetpack Compose**, **AndroidX**, and **Material 3 / M3 Expressive**.

The UI is designed around:

- Large, readable typography
- Clear visual hierarchy
- Expressive color and dynamic theming
- Rounded Material surfaces
- Simple, predictable navigation
- Responsive playback controls
- Theme-aware components
- Android-standard interaction patterns

The core interface does not depend on a third-party UI framework.

### Inspiration

Sonora's design and UI direction is inspired in part
by [Gramophone](https://github.com/FoedusProgramme/Gramophone), particularly its clean music-player
experience, Material-oriented interface, and focus on following Android's platform conventions.

Sonora is an independent implementation built with its own architecture, visual language, and
feature set.

---

## Tech Stack

| Area               | Technology                   |
|--------------------|------------------------------|
| Language           | Kotlin 2.4.10                |
| UI                 | Jetpack Compose              |
| Design             | Material 3 / M3 Expressive   |
| Android foundation | AndroidX                     |
| Navigation         | Navigation Compose 2.10.2    |
| Playback           | AndroidX Media3 1.11.1       |
| Background work    | WorkManager 2.11.2           |
| Image loading      | Coil 3.6.3                   |
| Lyrics             | LRCLIB                       |
| Metadata           | jaudiotagger-kt 1.0.0        |
| Serialization      | Kotlinx Serialization 1.11.0 |
| Build              | Gradle + AGP 9.4.0           |
| Java               | Java 17                      |
| Minimum Android    | Android 10 / API 29          |
| Compile / Target   | Android API 37               |

---

## Project Structure

The layout below reflects the current source tree. Feature screens are grouped by user-facing area;
shared services and reusable UI live in their own packages.

```text
.
├── .github/
│   ├── assets/                         # README screenshots and banner
│   ├── badges/downloads.json           # Download badge data
│   ├── scripts/                        # Build, release, badge, and cleanup helpers
│   └── workflows/                      # Debug build, release, badge, and cleanup workflows
├── app/
│   ├── build.gradle.kts                # Android config, version, dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/sipun/sonora/
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/update/         # Update checks, verification, installation, worker
│       │   │   ├── data/
│       │   │   │   ├── lyrics/          # LRCLIB client, parser, cache, repository
│       │   │   │   ├── media/           # Local music discovery and metadata editing
│       │   │   │   └── preferences/     # Library, playback, theme, onboarding settings
│       │   │   ├── domain/
│       │   │   ├── feature/
│       │   │   │   ├── album/
│       │   │   │   ├── artist/
│       │   │   │   ├── favorites/
│       │   │   │   ├── home/
│       │   │   │   ├── library/
│       │   │   │   ├── lyrics/
│       │   │   │   ├── nowplaying/
│       │   │   │   ├── playlists/
│       │   │   │   ├── settings/
│       │   │   │   └── sorting/
│       │   │   ├── navigation/          # App navigation and mini-player overlay
│       │   │   ├── player/              # Media3 controller, service, state, artwork
│       │   │   ├── ui/
│       │   │   │   ├── components/      # Reusable UI components and dialogs
│       │   │   │   ├── onboarding/
│       │   │   │   ├── theme/
│       │   │   │   └── update/
│       │   │   └── widget/              # Home-screen widget provider
│       │   └── res/                     # Drawables, layouts, strings, themes, XML resources
│       ├── test/
│       │   ├── java/com/sipun/sonora/ExampleUnitTest.kt
│       │   └── kotlin/com/sipun/sonora/
│       │       ├── core/update/         # Update-version tests
│       │       ├── data/lyrics/         # Lyrics parser tests
│       │       └── feature/nowplaying/  # Playback-time formatter tests
│       └── androidTest/java/com/sipun/sonora/ExampleInstrumentedTest.kt
├── gradle/
│   ├── libs.versions.toml               # Centralized dependency versions
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── LICENSE
```

### Package Responsibilities

- **`core/update`** handles release discovery, verification, scheduling, notifications, and
  installation.
- **`data`** contains local-media access, lyrics lookup/parsing/cache, metadata editing, and
  preference stores.
- **`domain`** defines the `Song` model and `MusicRepository` contract.
- **`feature`** contains screens organized around library, playback, lyrics, settings, and other app
  features.
- **`navigation`**, **`player`**, **`ui`**, and **`widget`** own app navigation, Media3 playback,
  shared visual components, and the home-screen widget.
- **`app/src/test`** and **`app/src/androidTest`** contain local unit tests and Android
  instrumentation tests.

---

## Building

Clone the repository:

~~~bash
git clone https://github.com/Darkstar085/Sonora.git
cd Sonora
~~~

Open the project in Android Studio and let Gradle sync.

For a debug build:

~~~bash
./gradlew assembleDebug
~~~

The release build is minified and resource-shrunk. Release signing is configured through environment
variables:

~~~text
ANDROID_KEYSTORE_FILE
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
~~~

After configuring the signing credentials, build the release APK with:

~~~bash
./gradlew assembleRelease
~~~

---

## Privacy & Network

Sonora follows a **local-first** approach: your music library and playback are centered around media
stored on your device, and no streaming account is required.

The app does use network access for specific features, including:

- Lyrics retrieval from LRCLIB
- Update-related functionality

Media permissions are requested according to the Android version and the features used by the app.
The music library permission is required for Sonora to discover and organize local music.

---

## License

Sonora is released under the **MIT License**.

See [LICENSE](LICENSE) for the full license text.

---

<div align="center">

**Sonora — Your music, simply.**

Made with ❤️ for local music.

</div>
