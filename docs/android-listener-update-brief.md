# Android Listener Update — Briefing for Android Claude

## Context

MixMates is a music link converter and social music sharing platform at [mixmat.es](https://mixmat.es). The Listener is its audio recognition feature — point your phone at a song, it identifies it and gives you links on Spotify, Tidal, and Apple Music.

Two native Listener apps, both open source (MIT, MixMat Ltd):

- **iOS** — [github.com/jbaddnz/mixmates-listener-ios](https://github.com/jbaddnz/mixmates-listener-ios). Production-quality, App Store submission in progress. The reference implementation.
- **Android** — [github.com/jbaddnz/mixmates-listener-android](https://github.com/jbaddnz/mixmates-listener-android). v0.1.1. Closer to parity than it looks — needs polish, compliance, and tests, not a rebuild.

Both apps talk to the same backend: the [Listener API v1](https://jbaddnz.github.io/mixmates-listener-api/).

## Reference material

- **API reference** (interactive): [jbaddnz.github.io/mixmates-listener-api](https://jbaddnz.github.io/mixmates-listener-api/)
- **Developer guide**: [DEVELOPER_GUIDE.md](https://github.com/jbaddnz/mixmates-listener-api/blob/main/DEVELOPER_GUIDE.md)
- **OpenAPI spec**: [listener-v1-openapi.yaml](https://github.com/jbaddnz/mixmates-listener-api/blob/main/listener-v1-openapi.yaml)
- **iOS source** (reference implementation): [github.com/jbaddnz/mixmates-listener-ios](https://github.com/jbaddnz/mixmates-listener-ios)

## What Android already has

### All 10 API endpoints implemented (`ListenerApi.kt`)

`GET /health`, `GET /auth/me`, `POST /recognize`, `GET /history`, `GET /history/{id}`, `DELETE /history/{id}`, `POST /history/{id}/share`, `GET /groups`, `GET /recordings`, `DELETE /recordings` — all done in Retrofit.

### 5 screens with full navigation (`NavGraph.kt`)

| Screen | What it does |
|---|---|
| **Listen** | Record 11s → recognise → show result. Branded mic button, gradient, splash |
| **History** | Paginated list (cursor-based), tap to detail |
| **History Detail** | Track info, platform links, multi-select group sharing with per-group feedback |
| **Token Entry** | Paste Listen Key, validate against `/auth/me` |
| **Settings** | Dark mode toggle, sign out with confirmation, version info |

### Infrastructure

- **Offline queue** — Room + `RecognitionSyncWorker` (WorkManager, 15-min cycle, network-constrained). iOS doesn't have this — keep it
- **Auth interceptor** — auto-injects Bearer token
- **Rate limit interceptor** — parses `X-RateLimit-*` headers
- **Repositories** — Auth, Group, History, Recognition
- **Hilt DI** — AppModule, DatabaseModule, NetworkModule
- **EncryptedSharedPreferences** — AES-256 token storage
- **TrackCard** — reusable track display component
- **Branded splash** — MML Audiowide, dark by default
- **Pre-tester polish done** — adaptive icon, splash, branded mic, AGP 9 compat, warning-free build

---

## Google Play compliance requirements

These are mandatory for Play Store submission, not optional.

### 1. Data Safety form

Must be completed in Play Console before publishing.

| Category | Collected? | Shared? | Purpose |
|---|---|---|---|
| Audio files (voice/sound recordings) | Yes — 11s clip sent to server | No (AudD processes on our behalf as service provider) | Core app functionality |
| User IDs (Bearer token) | Yes | No | Authentication |

Declare: encryption in transit (HTTPS), deletion mechanism (`/account/delete`), no optional data collection.

### 2. Account deletion — REQUIRED

Google Play requires in-app account/data deletion (since May 2024). The app accepts a pre-existing token rather than creating accounts in-app, which is a grey area — but best practice and consistency with iOS means we should include it regardless.

Add a Settings row that opens `https://mixmat.es/account/delete` in the system browser. The web page handles sign-in and typed confirmation. Also declare the deletion URL in the Data Safety form.

### 3. Prominent disclosure for microphone

Google requires an in-app disclosure screen shown BEFORE the system `RECORD_AUDIO` permission dialog. Cannot just pop the system dialog cold.

The disclosure must contain:

- What data is collected (audio from the microphone)
- Why (to identify music playing nearby)
- How it's used (sent to recognition service, not stored permanently)

Must offer at least "Allow" and "Not now". Navigation away must not be interpreted as consent. Google may request a short video demonstrating this flow during review.

### 4. Privacy policy — in-app and in store listing

Required for any app requesting sensitive permissions. `mixmat.es/privacy` already covers audio collection, AudD disclosure, retention periods, deletion rights, and contact info. The app needs a Legal screen that links to it.

### 5. App Bundle format

Play Store requires Android App Bundle (.aab), not APK. Current GitHub Releases APK is fine for sideloading. The build config should produce both: AAB for Play Store, APK for GitHub Releases.

### 6. Target SDK

Current `targetSdkVersion 35` meets the minimum. Must bump to 36 by August 2026.

### 7. Play App Signing

Required for AAB submissions. Google manages the app signing key. You keep a separate upload key locally.

---

## The gap vs iOS

### Screens to add

| Screen | Why | Priority |
|---|---|---|
| **Legal** | Privacy policy link, terms link, GitHub repo link, version/build, copyright. Required for Play Store (privacy policy must be accessible in-app) | High |
| **Mic disclosure** | Prominent disclosure before first RECORD_AUDIO permission request. Google Play requirement | High |

### Settings additions

| Item | Why | Priority |
|---|---|---|
| **Delete account** | Opens `https://mixmat.es/account/delete` in browser. Google Play requirement | High |
| **Splash toggle** | iOS Settings lets users skip splash. Low priority for Play Store | Low |

### Infrastructure

| Item | Why | Priority |
|---|---|---|
| **CI/CD** | GitHub Actions workflow (build + test on push/PR). iOS has this; Android has none | High |
| **Tests** | iOS has 11 test files with stub injection. Android has zero | High |
| **AAB build** | Play Store requires .aab. Configure alongside existing APK output | High |

### UX polish (check against iOS)

| Item | Priority |
|---|---|
| History swipe-to-delete with optimistic UI and error recovery | Medium |
| History pull-to-refresh | Low |
| History empty state view | Low |
| Listen screen: user greeting from profile, rate limit in toolbar | Low |
| Token Entry: specific error messages for listen-not-enabled | Low |

---

## Architecture — don't change it

Existing patterns are idiomatic and sound:

- MVVM + Compose + ViewModels + StateFlow
- Hilt for DI
- Retrofit + Kotlinx Serialization for networking
- Room for offline queue
- EncryptedSharedPreferences for token storage
- WorkManager for background sync

Add features within these patterns. Don't restructure.

## Design principles

- **No analytics, no tracking, no telemetry.** Non-negotiable. No Firebase Analytics, no Crashlytics, no third-party SDKs that phone home. This is a competitive advantage in Play Store review — declare it proudly in the Data Safety form
- **No new dependencies without discussion.** Open an issue first
- **Privacy by default.** The only data leaving the device is an audio clip and a bearer token
- **Simple over clever.** Don't abstract until you need to

## Brand assets

The `mml-wordmark` package is at `assets/mml-wordmark/`:

- **mmL** — the Listener wordmark (height-matched m + L)
- **mms** — the MixMates wordmark (all lowercase)
- Brand gradient: Spotify green `#1DB954` → cyan `#2CCCD3`
- PNGs at @1x/@2x/@3x plus 1024 and 2048, SVG sources, MuseoModerno font, build script

## Suggested order of work

1. **Mic disclosure screen** — Play Store blocker. Must exist before RECORD_AUDIO is requested
2. **Legal screen** — Play Store requirement (privacy policy in-app)
3. **Account deletion in Settings** — Play Store requirement
4. **CI/CD** — GitHub Actions (build + test on push/PR)
5. **Tests** — ViewModel tests with fake repositories
6. **AAB build config** — alongside existing APK
7. **UX polish** — swipe-to-delete, pull-to-refresh, empty states, error message parity

## What NOT to do

- Don't add Firebase or any Google analytics/tracking services
- Don't add Crashlytics or any crash reporting SDK
- Don't port Swift patterns literally — use idiomatic Kotlin/Compose
- Don't remove the offline queue — it's a genuine advantage over iOS
- Don't redesign the UX — match iOS feature set, adapted to Material 3 conventions

## Links

| Resource | URL |
|---|---|
| Listener API docs | [jbaddnz.github.io/mixmates-listener-api](https://jbaddnz.github.io/mixmates-listener-api/) |
| iOS reference | [github.com/jbaddnz/mixmates-listener-ios](https://github.com/jbaddnz/mixmates-listener-ios) |
| Android repo | [github.com/jbaddnz/mixmates-listener-android](https://github.com/jbaddnz/mixmates-listener-android) |
| MixMates web | [mixmat.es](https://mixmat.es) |
| Account deletion | [mixmat.es/account/delete](https://mixmat.es/account/delete) |
| Privacy policy | [mixmat.es/privacy](https://mixmat.es/privacy) |
| Terms | [mixmat.es/terms](https://mixmat.es/terms) |
| Google Play Data Safety | [support.google.com](https://support.google.com/googleplay/android-developer/answer/10787469) |
| Google Play Account Deletion | [support.google.com](https://support.google.com/googleplay/android-developer/answer/13327111) |
| Google Play Prominent Disclosure | [support.google.com](https://support.google.com/googleplay/android-developer/answer/11150561) |
