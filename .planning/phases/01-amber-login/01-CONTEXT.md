# Phase 1: Amber login - Context

**Gathered:** 2026-08-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Optional Amber identity on Home. User can connect via NIP-55 `get_public_key`, see npub after restart, sign out, and get a missing-signer path. Reading stays ungated. No nsec, bunker, sign_event, bookmarks, or auth gate.

</domain>

<decisions>
## Implementation Decisions

### Signer
- **D-01:** Amber / NIP-55 only. Copy Dark Wisp's smaller login shape (`RemoteSignerBridge` + Compose launcher). Do not port Amethyst Quartz or a `NostrSigner` / `RemoteSigner` sign_event stack.
- **D-02:** First `get_public_key` intent must not set `package`. Persist extras `package` after approval. Do not call `get_public_key` again while the stored pair exists.
- **D-03:** No `permissions` JSON on login.

### Persistence
- **D-04:** Plain SharedPreferences (`boris_session`): `pubkey_hex` + `signer_package`. No DataStore, no EncryptedSharedPreferences, no Room, no DI.
- **D-05:** Normalize Amber's `result` (hex or `npub1…`) to hex before save. Display npub in the UI. Tiny in-repo NIP-19 helper + official vector test.

### UI
- **D-06:** Auth chrome on Home, not a new route or Activity. Logged out: Connect. No signer: missing-Amber copy + install links. Logged in: npub + Sign out.
- **D-07:** Compose owns `rememberLauncherForActivityResult` at Activity/Home scope. Domain code builds the intent and parses extras; it must not call `startActivity`.
- **D-08:** Missing Amber: Zapstore first (`https://zapstore.dev/apps/com.greenart7c3.nostrsigner`), F-Droid and GitHub releases secondary. Do not send users to Play Store. Do not hide Connect the way Dark Wisp hides the button.

### Security
- **D-09:** Never request, persist, or log an `nsec`. Exclude the session prefs file from backup (`dataExtractionRules` / `fullBackupContent`, smallest change).
- **D-10:** Distinguish reject vs back vs missing signer. Three outcomes, three strings.

### Claude's Discretion
- Exact Home layout of AuthBar (match existing zinc/paper theme).
- Whether `SignerIntentBridge` is a tiny object or a callback on the helper, as long as launch stays in Compose.
- Exact SharedPreferences file/key names if `boris_session` / `pubkey_hex` / `signer_package` need a prefix.

</decisions>

<specifics>
## Specific Ideas

- Follow Dark Wisp: https://github.com/barrydeen/dark-wisp-android (`RemoteSignerBridge`, `SignerIntentBridge`, `AuthScreen`). Amethyst only if Dark Wisp is unclear.
- Amber package: `com.greenart7c3.nostrsigner`
- Manifest `<queries>` for `nostrsigner:` with `BROWSABLE` before any install check (API 30+).
- One in-flight Connect. Persist the pair before any navigation. Do not copy Home's `incomingUrl` / `LaunchedEffect` rotate bug.

</specifics>

<canonical_refs>
## Canonical References

### Protocol
- https://github.com/nostr-protocol/nips/blob/master/55.md — intents, extras, `<queries>`, omit package on first `get_public_key`, store pair
- https://github.com/nostr-protocol/nips/blob/master/19.md — npub encode/decode vectors

### Product
- `.planning/PROJECT.md` — core value, locked decisions
- `.planning/REQUIREMENTS.md` — AUTH-01..04, READ-01
- `.planning/ROADMAP.md` — Phase 1 success criteria
- `.planning/research/SUMMARY.md` — one-phase approach
- `.planning/research/STACK.md` — no new Maven deps, SharedPreferences, Nip19 helper
- `.planning/research/ARCHITECTURE.md` — file placement under `org.dergigi.boris`
- `.planning/research/PITFALLS.md` — queries, extras, npub/hex, re-ask, package pin, rotation, backup
- `.planning/codebase/CONVENTIONS.md` — Kotlin/Compose style
- `.planning/codebase/STRUCTURE.md` — where new files go

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `HomeScreen.kt`: URL field + Read. Add AuthBar here. Keep `rememberSaveable` for the URL field.
- `ReaderViewModel.kt`: `_state` / `state` StateFlow pattern for AuthViewModel if needed.
- `UrlExtractorTest.kt`: JVM JUnit 4 style for Nip19 and SessionStore tests.
- `AndroidManifest.xml`: add `<queries>`; do not drop share/VIEW filters.

### Established Patterns
- Single Activity, NavHost home/reader, no Hilt/Koin.
- Live package `org.dergigi.boris` only. Do not touch `com.readwithboris`.

### Integration Points
- `HomeScreen` / `BorisApp` for launcher + auth chrome.
- Manifest for `<queries>` and backup exclude.
- `app/src/test/java/org/dergigi/boris/` for Nip19 + session tests.

</code_context>

<deferred>
## Deferred Ideas

- Bunker / NIP-46
- `sign_event`, NIP-44, ContentResolver signing
- Bookmarks / highlights
- Multi-account
- `nsec` paste (never)

</deferred>

---

*Phase: 01-amber-login*
*Context gathered: 2026-08-14*
