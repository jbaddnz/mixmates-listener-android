# Play Store Distribution

Ship the app to Google Play Store.

## Why

The app works. Distribution is how it reaches people. Play Store removes the sideloading barrier.

## Prerequisites

### D-U-N-S number

- [x] MixMat Ltd D-U-N-S: **594340831**

### Developer account

- [ ] Google Play Developer account ($25 one-time) - register as organisation (MixMat Ltd), not personal
- [ ] Identity verification for Jamie as account administrator (government ID)
- [ ] Organisation verification via D-U-N-S number 594340831
- [ ] Legal name "MixMat Ltd" and registered address must match Companies Office records exactly
- [ ] Note: organisation accounts skip the 14-day/12-tester closed testing requirement

### Timeline expectations

| Step | Duration |
|---|---|
| Play Console setup + identity verification | 2-7 days |
| First production submission review | 3-7 days (new developers get longer reviews) |
| Rejection fix + resubmission (if needed) | 3-7 days per cycle |
| **Best case** | **~1 week** |
| **Worst case** | **~3 weeks** (identity verification delay + rejection cycle) |

## Jamie

- [ ] Feature graphic (1024x500) - for store listing header
- [ ] Screenshots - capture 4-6 from emulator or device: listen screen, recording in progress, recognition result, history, track detail, settings
- [ ] Content rating questionnaire - fill out in Play Console (guidance below)
- [ ] Permissions Declaration Form for RECORD_AUDIO in Play Console
- [ ] Submit for review in Play Console
- [ ] Link from MixMates install page once live

## Implementation

- [x] App icon — MuseoModerno mmL adaptive icon (Inkscape-outlined paths, gradient)
- [x] Splash screen — mmL wordmark + Listener text
- [ ] Generate upload keystore and configure release signing in build files
- [x] Build signed AAB (Android App Bundle) — CI builds release bundle
- [ ] Test release build on device
- [x] Review and tighten proguard rules for release
- [ ] Write store listing copy — title, short description (80 chars), full description
- [x] Prepare data safety form answers — documented below
- [x] Update README with CI badge and current features
- [x] Verify targetSdk meets current Play Store requirements (35, bump to 36 by Aug 2026)
- [x] Mic disclosure dialog — prominent disclosure before RECORD_AUDIO
- [x] Legal screen — in-app privacy policy, terms, source link
- [x] Account deletion — settings row opens mixmat.es/account/delete
- [x] CI/CD — GitHub Actions (build + release bundle + tests)
- [x] Unit tests — 20 ViewModel tests

## Privacy policy

Already hosted at https://mixmat.es/privacy — covers audio recordings, the Listener, token storage, and third-party data sharing. Sections 2, 5, and 7 are directly relevant. Link to this in the store listing.

## Content rating guidance

- No violence, sexual content, profanity, or controlled substances
- No user-generated content visible to others directly in this app
- No location data collected
- Microphone access: yes, for audio recognition only, user-initiated
- Expected rating: PEGI 3 / Everyone

## Data safety form

- **Audio** — recorded and sent to mixmat.es for recognition. Not stored on device beyond temporary cache. Processed by a third-party recognition service (privacy policy Section 5)
- **Authentication token** — stored locally in encrypted storage. Sent with every API request
- **No analytics, no advertising, no tracking SDKs**
- **No location, contacts, or other device data accessed**
- **Data can be deleted** — user removes token in app settings, recordings expire server-side after 30 days, full account deletion available at mixmat.es

## Store listing

### Direction

Match the MixMates voice - personal, direct, grounded. Not a tech product pitch. Think "hear something, know it, share it with the people who matter" not "AI-powered music recognition platform."

### Title
```
MixMates Listener
```

### Short description (80 chars max)
```
Identify songs around you. Open them on Spotify, Tidal, or Apple Music.
```

### Full description (4000 chars max)
```
MixMates Listener identifies music playing around you and gives you cross-platform links to open it wherever you listen.

Record a short clip, and the app identifies the track - then gives you one-tap links to Spotify, Tidal, and Apple Music. Your recognition history is saved so you can come back to tracks later.

Features:
- 11-second audio recognition - hold your phone up and tap record
- Cross-platform links - Spotify, Tidal, and Apple Music, one tap each
- Share target - share a Spotify, Tidal, or Apple Music link from any app to get cross-platform links
- Recognition history - browse everything you've identified
- Group sharing - share recognised tracks to your MixMates groups
- Offline queue - recordings sync when you're back online
- No tracking, no analytics, no third-party SDKs

MixMates Listener is open source. The full source code is available on GitHub under the MIT license.

A MixMates account with Listen enabled is required. Visit mixmat.es to get started.
```

### Screenshots (minimum 4, recommended 6)

1. Listen screen idle - mmL mic button, greeting
2. Mic disclosure dialog - prominent disclosure before recording
3. Recording in progress - countdown ring
4. Recognition result - track card with platform links
5. History list - several recognised tracks
6. Settings - legal, sign out, delete account visible

Minimum 320px, maximum 3840px. 16:9 or 9:16.

### Category
Music & Audio

### Keywords
Do not say "like Shazam" or "Shazam alternative" (trademark). Differentiate on privacy, open source, cross-platform, group sharing.

## Permissions Declaration Form

For RECORD_AUDIO, state clearly:

> "Audio recognition - user initiates recording by tapping the record button. An 11-second clip is sent to the MixMates server for music identification via AudD, then discarded unless the user has opted into recording storage. The app does not record in the background."

## Approval tips

### Improve first-attempt odds

- Google rejected 1.75 million apps in 2025 - new developer accounts get extra scrutiny
- **Permissions consistency** - your declaration form, data safety form, store listing, and actual APK permissions must all tell the same story. Inconsistencies trigger automatic rejection
- Do NOT request RECORD_AUDIO at app launch - request it when the user first taps record
- Prominent disclosure must appear BEFORE the system permission dialog, not after
- Test the release build (not debug) on a real device before submission
- Test on multiple API levels if possible (API 29+ through current)
- Privacy policy URL must be live, HTTPS, and not behind auth
- Account deletion URL must work and be linked from both the app and the store listing
- No placeholder content, no "coming soon" screens
- Don't mention AI in the listing - "audio recognition" not "AI-powered recognition"

### Data Safety form - be precise

- Audio: collected, not shared with third parties, purpose is app functionality, processed by AudD on your behalf as a service provider
- Authentication token: collected, stored locally in encrypted storage
- Declare encryption in transit (HTTPS)
- Declare account deletion mechanism with URL
- "No analytics, no advertising, no tracking" - select No for all. This is an advantage

### What NOT to do

- Don't add Firebase or Google Analytics just to look legitimate - zero SDKs is a strength
- Don't declare "no data collected" when you clearly send audio to a server
- Don't use Shazam's name anywhere in the listing
- Don't select under-13 target audience unless you want COPPA requirements

## Brand assets

- App icon: MuseoModerno mmL - green (#1DB954) to cyan (#2CCCD3) gradient, Inkscape-outlined paths
- Splash: mmL wordmark (MuseoModerno Bold 700) + "Listener" (HelveticaNeue, white 80%)
- Colour palette: dark background (#1A1A2E), Spotify green, Tidal cyan
