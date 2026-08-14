# Phase 2: Bunker login (NIP-46) - Context

**Gathered:** 2026-08-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Optional bunker identity on Home. User can paste a `bunker://` token (or open one via VIEW), Boris connects as a NIP-46 client, learns the user pubkey, shows npub after restart, and sign out deletes the local client keypair. Amber login stays. Reading stays ungated. No nsec in Boris. No sign_event. Boris is not a bunker server.

</domain>

<decisions>
## Implementation Decisions

### Role
- **D-01:** Boris is the NIP-46 *client*. Copy Amethyst's bunker *login* shape (`BunkerLoginUseCase`: parse `bunker://`, ephemeral client signer, subscribe on URI relays, `connect`, then `get_public_key`). Do not port Quartz. Do not copy Amethyst's bunker *server* (`Nip46SignerScreen`, consent UI, `NostrConnectSignerService`). Amber remains the on-device signer.
- **D-02:** Pairing is `bunker://` only this phase. Paste on Home. Also accept `ACTION_VIEW` / share of a `bunker://` URL into the same field (same as article VIEW). No camera QR. No `nostrconnect://` (other direction; needs a client-shown QR and extra relays).

### Protocol
- **D-03:** Login methods only: `connect` then `get_public_key`. Send client metadata `name=Boris` (and app URL if cheap). Requested permissions empty / none. No `sign_event`, NIP-04/44, ping heartbeat chrome, or `switch_relays` unless the researcher finds connect cannot complete without it.
- **D-04:** Relays come from the `bunker://` query (`relay=wss://…`). At least one relay is required. No Boris social relay list.
- **D-05:** Fail closed on bad URI, timeout, reject, or missing user pubkey. Distinguish those in the Home message the way Amber distinguish reject vs cancel vs missing signer.

### Persistence
- **D-06:** One identity. A successful bunker pair replaces an Amber session and a successful Amber connect replaces a bunker session. No multi-account.
- **D-07:** Extend the existing session store, do not invent a second login store. Amber record stays `pubkey_hex` + `signer_package`. Bunker record stores user pubkey hex, remote-signer pubkey, relays, disposable client-keypair, and the bunker `secret` if present. Client-keypair and bunker secret are secrets: never log them; exclude the session file from backup (already true for `boris_session`).
- **D-08:** Sign out deletes the bunker client-keypair and the stored pair locally. Sending NIP-46 `logout` is a courtesy if cheap; local delete is mandatory either way. Next bunker connect is a fresh client-keypair.

### UI
- **D-09:** Auth chrome stays on Home. No new NavHost route. Logged out / missing Amber: keep Amber Connect or install links, plus a bunker URI field and Connect bunker. Logged in: npub + Sign out (same as Phase 1). Reading still works logged out.
- **D-10:** Amber path (NIP-55, queries, missing-Amber Zapstore links) must not regress.

### Security
- **D-11:** Never request, persist, or log an `nsec`. The disposable client-keypair is Boris-generated and is not the user's key. Treat it as a secret anyway.
- **D-12:** Validate `bunker://` like Amethyst `validateBunkerUri`: scheme, 64-char hex remote-signer pubkey, at least one `relay=wss://`. Reject `nsec1` in that field.

### Claude's Discretion
- Which small Kotlin stack talks to relays and does NIP-44 (researcher picks; do not pull Amethyst Quartz).
- How the secret client-keypair is stored (EncryptedSharedPreferences vs Keystore vs similar), as long as it is not plaintext logs and is wiped on sign out.
- Exact Home layout of the bunker field (match zinc/paper AuthBar).
- Timeout values, reconnect-on-resume while logged in with bunker (Amethyst waits 15s for relay connect).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Protocol
- https://github.com/nostr-protocol/nips/blob/master/46.md — `bunker://`, client-keypair, `connect`, `get_public_key`, kind 24133, NIP-44, auth_url, logout
- https://github.com/nostr-protocol/nips/blob/master/44.md — encrypt request/response content
- https://github.com/nostr-protocol/nips/blob/master/19.md — npub display from stored user hex (already in Boris)

### Product
- `.planning/PROJECT.md` — core value, no nsec, reading first
- `.planning/REQUIREMENTS.md` — AUTH-05, AUTH-01..04 must not regress, READ-01
- `.planning/ROADMAP.md` — Phase 2 success criteria
- `.planning/phases/01-amber-login/01-CONTEXT.md` — D-04 session store, D-06 Home chrome, D-09 no nsec
- `.planning/research/ARCHITECTURE.md` — later bunker: same Session, new transport
- `.planning/research/STACK.md` — bunker is a separate stack; do not fold into the Amber helper
- `.planning/codebase/CONVENTIONS.md` — Kotlin/Compose style
- `.planning/codebase/STRUCTURE.md` — files under `org.dergigi.boris`

### Reference implementation (login shape, not a copy of the tree)
- `/tmp/amethyst/commons/src/commonMain/kotlin/com/vitorpamplona/amethyst/commons/domain/nip46/BunkerLoginUseCase.kt` — parse URI, ephemeral signer, wait for relay, `connect`, `getPublicKey`
- `/tmp/amethyst/desktopApp/src/jvmMain/kotlin/com/vitorpamplona/amethyst/desktop/account/BunkerUriUtils.kt` — URI validation
- https://github.com/vitorpamplona/amethyst — Amethyst bunker login, not the bunker-server settings screens

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AuthBar.kt` / `AuthViewModel.kt` / `HomeScreen.kt`: add bunker paste + connect beside Amber; keep `rememberLauncherForActivityResult` for Amber only.
- `Session.kt` / `SessionStore.kt`: extend with a kind; Amber still requires package; bunker will not have a package.
- `Nip19.kt`: display npub from user pubkey hex after bunker `get_public_key`.
- `MainActivity.kt`: parse VIEW/share; today only http(s). Add `bunker://` the same way URLs land on Home.

### Established Patterns
- SharedPreferences `boris_session` for identity. One pair. Sign out is a clear.
- JVM JUnit 4 tests next to the parser (`Nip19Test`, `SessionStoreTest`).
- No Hilt, no Room, no DataStore for Amber. Bunker secrets may need a tighter store than plaintext prefs.

### Integration Points
- Home AuthBar is the only login chrome.
- Amber `RemoteSignerBridge` stays intent-based. Bunker is a new transport in `nostr/` (or similar). Do not make `RemoteSignerBridge` speak WebSockets.

</code_context>

<specifics>
## Specific Ideas

- Follow Amethyst bunker login, the way Phase 1 followed Dark Wisp Amber login.
- User generates `bunker://` in Amber remote / nsec.app / similar and pastes it into Boris.
- Dark Wisp is not a bunker reference (its "remote signer" is NIP-55).

</specifics>

<deferred>
## Deferred Ideas

- `nostrconnect://` (client shows a QR for the signer to scan)
- Camera QR scan of `bunker://`
- `sign_event` / NIP-44 through the stored bunker (AUTH-06)
- Heartbeat / connection-status chrome
- Boris as a bunker server
- Multi-account

</deferred>

---

*Phase: 2-Bunker login (NIP-46)*
*Context gathered: 2026-08-14*
