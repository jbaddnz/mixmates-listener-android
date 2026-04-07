# Play Store Distribution

Ship the app to Google Play Store.

## Why

The app works. Distribution is how it reaches people. Play Store removes the sideloading barrier.

## Jamie

- [ ] Google Play Developer account ($25 one-time) — create or confirm existing under MixMat Ltd
- [ ] Feature graphic (1024x500) — for store listing header
- [ ] Screenshots — capture 4-6 from emulator or device: listen screen, recording in progress, recognition result, history, track detail, share sheet
- [ ] Content rating questionnaire — fill out in Play Console (guidance below)
- [ ] Submit for review in Play Console
- [ ] Link from MixMates install page once live

## Implementation

- [ ] App icon — convert MML brand mark (mml-square.svg) to adaptive icon format
- [ ] Generate upload keystore and configure release signing in build files
- [ ] Build signed AAB (Android App Bundle)
- [ ] Test release build on emulator
- [ ] Review and tighten proguard rules for release
- [ ] Write store listing copy — title, short description (80 chars), full description
- [ ] Prepare data safety form answers
- [ ] Update README with Play Store badge/link after launch
- [ ] Verify targetSdk meets current Play Store requirements

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

## Store listing direction

Match the MixMates voice — personal, direct, grounded. Not a tech product pitch. Think "hear something, know it, share it with the people who matter" not "AI-powered music recognition platform."

## Brand assets

- App icon: MML mark from /assets/mml-square.svg — green (#1DB954) to cyan (#2CCCD3) gradient
- Colour palette: dark background, Spotify green, Tidal cyan (per existing MixMates design)
