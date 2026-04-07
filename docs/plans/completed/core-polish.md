# Core Polish

Fill gaps in error handling, UX feedback, and basic app lifecycle.

## Why

The core flows worked but had rough edges — no way to log out, no feedback when permissions were denied, no handling for expired tokens or rate limits.

## Tasks

- [x] Settings screen — remove Listen Key with confirmation dialog
- [x] Pull to refresh on history
- [x] Rate limit — 429 interceptor with Retry-After, quota display in top bar
- [x] Expired/revoked token handling — global 401 interceptor clears token and redirects to token entry
- [x] Microphone permission denied state — explanation text with link to system settings
