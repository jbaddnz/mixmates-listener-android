# Initial Scaffold

Standalone Android app for the MixMates Listener API. Record audio, identify tracks, manage listen queue, share to groups.

## Why

Android has no native shortcut equivalent for mic access. A lightweight native app gives high-quality audio capture for reliable recognition.

## Scope

- [x] Project structure — Kotlin, Jetpack Compose, MVVM, Hilt DI, Retrofit, Room
- [x] Token entry screen — validate via `GET /auth/me`, store in EncryptedSharedPreferences
- [x] Audio recording — MediaRecorder, AAC, 44.1kHz, 128kbps, mono, 11 seconds
- [x] Recognition — `POST /recognize`, display result with platform links
- [x] Platform deep links — Spotify, Tidal, Apple Music open in native apps
- [x] Share link — Android share sheet with "Artist - Title" and mixmat.es URL
- [x] History list — `GET /history` with cursor pagination
- [x] Track detail — `GET /history/:id` with shared_to display
- [x] Share to groups — `POST /history/:id/share` with group selection
- [x] Delete from history — `DELETE /history/:id`
- [x] Offline queue — Room DB for pending recognitions, WorkManager periodic sync
- [x] API DTOs matching OpenAPI spec
- [x] Open-source setup — MIT license, GitHub repo
