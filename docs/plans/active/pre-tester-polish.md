# Pre-Tester Polish

Get the app ready for the first external tester on a real device.

## Why

First impressions matter. The app works but has a placeholder icon and hasn't been tested on a real phone. Fix the obvious rough edges before handing it to someone.

## Implementation — done

- [x] App icon — MML adaptive icon with green-to-cyan gradient
- [x] Verify duplicate status code path is correct
- [x] Rebuild APK and publish updated GitHub release (v0.1.1)
- [x] Splash screen with MML Audiowide branding
- [x] Branded mic button with gradient
- [x] Dark mode by default
- [x] AGP 9 compatibility fixes
- [x] Warning-free build

## Jamie

- [ ] Test on a real device — verify mic recording, recognition, and platform deep links work outside the emulator
- [ ] Decide whether to host APK on mixmat.es/install or use GitHub release link for now
- [ ] Hand to tester, collect feedback
