# MixMates Listener for Android

[![CI](https://github.com/jbaddnz/mixmates-listener-android/actions/workflows/ci.yml/badge.svg)](https://github.com/jbaddnz/mixmates-listener-android/actions/workflows/ci.yml)

An open-source reference implementation of a music recognition app for Android, built on the [MixMates Listener API](https://github.com/jbaddnz/mixmates-listener-api).

Hold your phone up to a song, and it records a short clip, identifies the track, and gives you one-tap links to open it on Spotify, Tidal, or Apple Music. Think of it as an open, cross-platform Shazam — with the full source code in your hands.

This is a working app, but it's also a starting point. Fork it, restyle it, add features, build something better. The API is documented, the code is MIT-licensed, and the architecture is intentionally straightforward.

## What it does

- **Audio recognition** — 11-second recording, automatic identification via the MixMates API
- **Cross-platform links** — Spotify, Tidal, and Apple Music deep links for every match
- **Listen queue** — browse your recognition history with swipe-to-delete
- **Group sharing** — share tracks to your MixMates groups
- **Offline support** — queued recordings sync when connectivity returns
- **Share sheet** — send share links to friends via any app
- **Permission-aware UI** — microphone disclosure and clear handling of permission states

## Make it your own

This reference implementation covers the core flows — record, recognise, browse, share. There's plenty of room to build on top of it:

**System integrations**

- Add a Quick Settings tile or home screen widget for one-tap recognition
- Build a Wear OS companion
- Implement notifications for background recognition results

**UI and experience**

- Design your own UI and branding
- Add local history search and filtering
- Audio waveform visualisation during the recording countdown
- Playlist integrations

Or take it in a completely different direction.

## Getting started

### Requirements

- Android 8.0 (API 26) or higher
- [Android Studio](https://developer.android.com/studio) (latest stable) and JDK 17
- A [MixMates](https://mixmat.es) account with Listen enabled
- A Listen Key (generated in MixMates Settings > Listening)

### Build and run

1. Clone the repository:
   ```
   git clone https://github.com/jbaddnz/mixmates-listener-android.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and run — Android Studio will prompt to sync, then click **Run** to deploy to a device or emulator

### Emulator tips

- Create a virtual device via **Tools > Device Manager**
- Enable host microphone access in **Extended Controls > Microphone** for audio recognition to work

## Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Dependency injection**: Hilt
- **Networking**: Retrofit + OkHttp + Kotlinx Serialization
- **Local storage**: Room (offline queue), EncryptedSharedPreferences (token)
- **Background sync**: WorkManager
- **Image loading**: Coil
- **Testing**: JUnit + MockK + Turbine

## API

This app integrates with the [MixMates Listener API v1](https://github.com/jbaddnz/mixmates-listener-api). The API handles audio recognition, listen queue management, group sharing, and cross-platform link resolution. A copy of the OpenAPI spec is included at [`docs/listener-v1-openapi.yaml`](docs/listener-v1-openapi.yaml).

## On openness

This is an open-source client for a commercial API. The code is MIT-licensed and entirely yours to read, fork, and modify. The service behind the API is not — it runs on infrastructure that costs money to operate because we're serious about providing a good base for musical expression.

What we can do is make everything around it open: the client code, the API specification, the documentation. You can see exactly what data leaves your device (an audio clip and a bearer token), exactly where it goes (mixmat.es), and exactly what comes back. There are no analytics, no tracking, no telemetry of any kind in this app.

We think that's an honest trade-off, and we'd rather be upfront about it than pretend it isn't there.

## See also

- [MixMates Listener for iOS](https://github.com/jbaddnz/mixmates-listener-ios) — the iOS sibling of this app

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug reports, feature ideas, and pull requests are all welcome.

## License

[MIT](LICENSE) — use it however you like.

