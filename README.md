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
  <img src=".github/assets/permissions.png" width="31%" alt="Sonora permission management">
  <img src=".github/assets/about.png" width="31%" alt="Sonora About screen">
</p>


---

## Features

### Local Library

- Scan and browse music stored on the device
- Search your local music library
- Songs, albums and artists with dedicated detail pages
- Playlists and favorites
- Recently added tracks and continue-listening history on Home
- Album artwork and local metadata
- Exclude selected folders from library scanning

### Playback

- Play, pause, previous and next
- Queue management
- Shuffle and repeat modes
- Gapless playback between tracks
- Adjustable crossfade transitions
- Optional resume playback from where you left off
- Optional ReplayGain volume normalization
- Seeking with an expressive playback control
- Playback speed control
- Background playback
- Android system media controls
- Media notification
- Audio format information
- 4×2 home screen widget with album artwork, playback progress, and controls
- Optional keep-screen-awake setting while music is playing

### Lyrics

- Dedicated lyrics playback surface
- Synced lyric highlighting
- Lyrics retrieval through LRCLIB
- Local playback-aware lyrics presentation

### Metadata

- View local track metadata and audio format information
- Edit tags including title, artist, album artist, genre, year, track/disc number, composer, comments, grouping, lyrics, copyright, and BPM
- Add, change, or remove embedded album artwork

### Personalization

- Device theme
- Dynamic color
- Palette and accent customization
- Hue adjustment
- Pure black dark mode

### Android Integration

- Media3-based playback service and background playback
- System media controls and playback notifications
- Open audio files directly from compatible file managers
- Permission management for music, artwork/photos, notifications, metadata editing, and media deletion
- WorkManager-based update flow with theme-aware dialogs and clear update status messaging
- First-launch onboarding and media permission flow

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

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── kotlin/com/sipun/sonora/
    │   │   ├── MainActivity.kt
    │   │   ├── core/
    │   │   │   └── update/
    │   │   ├── data/
    │   │   │   ├── lyrics/
    │   │   │   ├── media/
    │   │   │   └── preferences/
    │   │   ├── domain/
    │   │   │   ├── model/
    │   │   │   └── repository/
    │   │   ├── feature/
    │   │   │   ├── album/
    │   │   │   ├── artist/
    │   │   │   ├── favorites/
    │   │   │   ├── home/
    │   │   │   │   ├── HomeScreen.kt
    │   │   │   │   ├── HomeOverview.kt
    │   │   │   │   ├── HomeOverviewComponents.kt
    │   │   │   │   ├── HomeLibraryContent.kt
    │   │   │   │   ├── HomePlaylistContent.kt
    │   │   │   │   └── HomeScreenEmptyStates.kt
    │   │   │   ├── library/
    │   │   │   ├── lyrics/
    │   │   │   ├── nowplaying/
    │   │   │   │   ├── NowPlayingScreen.kt
    │   │   │   │   ├── NowPlayingComponents.kt
    │   │   │   │   ├── NowPlayingVisuals.kt
    │   │   │   │   └── PlaybackTimeFormatter.kt
    │   │   │   ├── playlists/
    │   │   │   └── settings/
    │   │   │       ├── SettingsScreen.kt
    │   │   │       └── SettingsComponents.kt
    │   │   ├── navigation/
    │   │   ├── player/
    │   │   └── ui/
    │   │       ├── components/
    │   │       ├── onboarding/
    │   │       │   ├── WelcomeScreen.kt
    │   │       │   └── WelcomeVisuals.kt
    │   │       ├── theme/
    │   │       └── update/
    │   └── res/
    │       ├── drawable/
    │       ├── mipmap-anydpi-v26/
    │       ├── raw/
    │       ├── values/
    │       └── xml/
    ├── test/
    └── androidTest/
```

- **Feature UI:** Screens are grouped by feature, with supporting UI extracted into focused files.
- **Shared UI:** Reusable components live under `ui/components/`; onboarding visuals and theme code have their own packages.
- **Data and domain:** Local media, metadata, preferences, lyrics, and domain models are kept separate from feature screens.
- **Playback and updates:** Media3 playback lives under `player/`; update handling is grouped under `core/update/`.

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
