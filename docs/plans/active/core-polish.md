# Core Polish

Fill gaps in error handling, UX feedback, and basic app lifecycle before adding new features.

## Why

The core flows work but there are rough edges — no way to log out, no feedback when permissions are denied, no handling for expired tokens or rate limits. These need fixing before the app is usable day-to-day.

## Tasks

- [ ] Settings screen — log out (clear token), display account info (name, role), rate limit status
- [ ] Pull to refresh on history
- [ ] Rate limit display — show remaining quota, handle 429 responses gracefully with retry-after
- [ ] Expired/revoked token handling — detect 401 responses globally, redirect to token entry
- [ ] Microphone permission denied state — explain why mic is needed, link to system settings
- [ ] Duplicate handling — verify "Already in your queue" displays correctly for `duplicate` status
