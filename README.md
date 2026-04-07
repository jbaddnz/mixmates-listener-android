# MixMates Listener for Android

An Android app that identifies music playing around you and finds it on every streaming platform. Built for the [MixMates](https://mixmat.es) music sharing service.

Hold your phone up to a song, and MixMates Listener records a short clip, identifies the track, and gives you one-tap links to open it on Spotify, Tidal, or Apple Music. Recognised tracks are saved to your MixMates listen queue, where you can share discoveries with your groups.

If you're offline, recordings are queued locally and submitted automatically when you're back online.

## Features

- **Audio recognition** — 11-second recording, automatic identification
- **Cross-platform links** — Spotify, Tidal, and Apple Music deep links for every match
- **Listen queue** — browse and manage your recognition history
- **Group sharing** — share tracks to your MixMates groups
- **Offline support** — queued recordings sync when connectivity returns
- **Share sheet** — send mixmat.es share links to friends via any app

## Requirements

- Android 8.0 (API 26) or higher
- A [MixMates](https://mixmat.es) account with Listen enabled
- A Listen Key (generated in MixMates Settings > Listening)

## Building

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 17

### Steps

1. Clone the repository:
   ```
   git clone https://github.com/jbaddnz/mixmates-listener-android.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build:
   - Android Studio will prompt to sync — accept
   - Click **Run** (green play button) to build and deploy to a device or emulator

### Running on emulator

- Create a virtual device via **Tools > Device Manager**
- Enable host microphone access in the emulator's **Extended Controls > Microphone** settings for audio recognition to work

## Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Dependency injection**: Hilt
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization
- **Local storage**: Room (offline queue), EncryptedSharedPreferences (token)
- **Background sync**: WorkManager
- **Image loading**: Coil

## API

This app integrates with the [MixMates Listener API v1](https://github.com/jbaddnz/mixmates-listener-api). A copy of the OpenAPI spec is included at [`docs/listener-v1-openapi.yaml`](docs/listener-v1-openapi.yaml).

## License

[MIT](LICENSE)
