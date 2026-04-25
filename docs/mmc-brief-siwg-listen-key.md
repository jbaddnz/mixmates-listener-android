# Brief for MMC — Sign in with Google → Listen Key

## Context

The Android app currently requires users to paste a Listen Key generated on the web. This causes friction and requires users to context-switch between browser and app. Sign in with Google replaces this with a one-tap native sign-in that acquires the Listen Key automatically.

The Listen Key infrastructure doesn't change. Google Sign-In becomes a new front door — the user authenticates with Google, the server maps that identity to a MixMates account, generates (or retrieves) a Listen Key, and returns it. The Android app stores it locally and everything downstream works exactly as it does today.

## Proposed flow

1. User taps "Sign in with Google" on the token entry screen
2. Android presents the Credential Manager / One Tap sign-in sheet
3. User selects their Google account — authenticates with biometrics if required
4. Android receives a Google ID token (JWT)
5. App sends the ID token to a new server endpoint
6. Server validates the ID token against Google's public keys
7. Server finds or creates a MixMates account linked to the Google identity
8. Server generates a Listen Key (bearer token) for that account
9. Returns the token to the Android app
10. App stores the token — everything from here is identical to the current paste flow

## What the server needs

### New endpoint

`POST /api/v1/listener/auth/google`

**Request body:**
```json
{
  "id_token": "eyJ...",
  "nonce": "random-nonce-string",
  "name": "Jamie Baddeley",
  "email": "jamie@example.com"
}
```

Name and email are provided from the Google ID token claims. Unlike Apple, Google always includes these — but the server should treat them as supplementary (the `sub` claim is the stable identity).

Nonce: set via `setNonce()` on the Credential Manager request. Server uses it for replay protection.

**Response (success):**
```json
{
  "data": {
    "token": "encrypted-bearer-token",
    "is_new_account": true,
    "listen_enabled": false
  }
}
```

The token is the same encrypted bearer token used by all Listener API calls. `listen_enabled` is included to avoid an extra `/auth/me` round trip — if false, show upgrade guidance without storing then clearing the token.

**Response (error):**
```json
{
  "error": {
    "code": "token_invalid",
    "message": "Could not verify identity"
  }
}
```

Standard Listener API error envelope — matches existing error handling.

### Google Client ID

Uses the **web client ID** (the server's existing OAuth client ID). Android Credential Manager expects the web client ID, not an Android-specific one. Same `GOOGLE_CLIENT_ID` already deployed.

### Unauthenticated endpoint

No bearer token required — this IS the sign-in. Excluded from auth middleware.

### ID token validation

Google's ID token is a JWT signed with RS256. The server validates it by:

1. Fetch Google's public keys from `https://www.googleapis.com/oauth2/v3/certs`
2. Verify the JWT signature against those keys
3. Check the `iss` is `https://accounts.google.com`
4. Check the `aud` matches the Google Client ID (web client ID)
5. Check `email_verified` is true (only store verified emails)
6. Check the nonce matches the one sent in the request (replay protection — stored in KV with 5-min TTL)
7. Check the token hasn't expired

Google's public keys rotate — cache them in KV (24hr TTL) but refresh on signature failure. Same pattern as the Apple SIWA endpoint for iOS.

### Rate limiting

5 attempts per minute per IP. Sign-in action, not high-frequency.

### Account scenarios

1. **New Google identity, no existing account** — create a new account with `google_id`, generate bearer token. `is_new_account: true`
2. **Known Google identity** — find the linked MixMates account, generate bearer token. `is_new_account: false`
3. **Email matches existing account** — do NOT auto-link. Per project policy, users control identity explicitly. Create a new account. User can link accounts later from the web

### Listen enablement

Google Sign-In creates a free account. Listen requires a paid tier — consistent with web. The app should guide users to upgrade if Listen is not enabled.

If the Google identity is already linked to an existing paid account, Listen is already enabled and the token works immediately.

## What the Android side will build

- "Sign in with Google" button on the token entry screen using Credential Manager API (or legacy One Tap for older devices)
- Set nonce on the Credential Manager request via `setNonce()` for replay protection
- Send the ID token + nonce to the new endpoint
- Check `listen_enabled` in response before storing token — if false, show upgrade guidance
- Store the returned bearer token in encrypted shared preferences (existing pattern)
- Fall back to the existing "Paste Listen Key" flow for users who prefer it
- Handle the case where the user has multiple Google accounts — Credential Manager handles this natively

### Dependencies

- `androidx.credentials:credentials` — Credential Manager
- `androidx.credentials:credentials-play-services-auth` — Google Sign-In provider
- `com.google.android.libraries.identity.googleid` — Google ID token support

All standard Google/AndroidX libraries. No third-party SDKs.

## What doesn't change

- The bearer token is still the auth credential for all API calls
- Push notification registration still works against the same account
- The `ListenerApi` client doesn't change — still uses Bearer token auth
- Web users who paste a Listen Key still work exactly as before
- Share target functionality unchanged

## Relationship to iOS

The iOS Listener app is getting the same feature via Sign in with Apple (`POST /api/v1/listener/auth/apple`). Both endpoints follow the same pattern:

- Native platform sign-in → ID token → server validates → bearer token returned
- Same account model, same bearer token format, same downstream behaviour
- A user could sign in with Apple on iOS and Google on Android — same MixMates account if they link both identities on the web

## Bigger picture

MixMates is moving to an identity-first architecture where Apple/Google are the sign-in providers and music platforms (Spotify, Tidal, Apple Music) are connected separately. This aligns the mobile apps with the web — one identity model everywhere.

## Answers from server team

1. **Client ID** — use the existing web client ID (`GOOGLE_CLIENT_ID`). No separate Android credential needed
2. **Unauthenticated** — endpoint excluded from auth middleware. No bearer token required
3. **`listen_enabled` in response** — included to avoid `/auth/me` round trip
4. **Error shape** — standard `{ "error": { "code": "...", "message": "..." } }` envelope
5. **Rate limiting** — 5/min per IP
6. **Nonce** — yes, set `setNonce()` on Credential Manager request. Server validates for replay protection

## Timing

Build after the server endpoint ships. The Android side is straightforward — Credential Manager is well-documented and the token storage pattern already exists.
