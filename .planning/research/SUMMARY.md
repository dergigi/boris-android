# Project Research Summary

**Project:** Boris
**Domain:** Android reader + Amber / NIP-55 identity (login only)
**Researched:** 2026-08-14
**Confidence:** HIGH

## Executive Summary

Boris is a shipped Kotlin Compose reader. The next slice adds who-you-are through Amber, not a social client and not a key store. Experts on Android Nostr do this with NIP-55: declare `nostrsigner` in `<queries>`, fire one `get_public_key` intent with no `package` pin, persist pubkey hex plus signer package, and never ask again while that pair exists. Dark Wisp is the model to copy. Amethyst's Quartz signer tree is the model to refuse.

Recommended approach: keep the existing reader stack, add no Maven coordinates, and ship a thin login helper under `org.dergigi.boris`. Compose owns `ActivityResultLauncher`. Domain code builds the intent and parses extras. Persist two public strings in plain SharedPreferences. Decode Amber's usual `npub1…` result to hex before save; show npub in the UI. Reading stays ungated. Home gets Connect / npub / sign out, not an auth route.

The risks that actually break this slice are all in one place: missing `<queries>` (Amber looks uninstalled on API 30+), re-calling `get_public_key` on every launch (NIP-55 fingerprinting plus a hostile Amber prompt), treating Amber's bech32 as hex, pinning Amber's package on the first intent, and launching from a ViewModel so the result never returns. Mitigate by following Dark Wisp's login shape and stopping there. No `nsec`, no bunker, no `sign_event`, no `permissions` JSON.

## Key Findings

### Recommended Stack

See [STACK.md](STACK.md). Stay on the shipped catalog. Login needs platform APIs already on the classpath, not new libraries.

**Core technologies:**
- Kotlin 2.1.21 + Compose BOM 2025.06.01 + activity-compose 1.10.1 — already the app; Activity Result API lives here. Do not bump for login.
- NIP-55 `nostrsigner:` intent + manifest `<queries>` — the Amber handshake. Include `BROWSABLE`. Query by scheme, not by Amber's package.
- Plain SharedPreferences (`boris_session`) — two public strings (`pubkey_hex`, `signer_package`). No DataStore, no EncryptedSharedPreferences, no Room, no DI.
- Tiny in-repo NIP-19 helper — encode for display, decode Amber's `npub1` to hex. Official NIP-19 vector in a JVM test. Not Quartz, not Dark Wisp's full `Nip19.kt`.
- Amber `com.greenart7c3.nostrsigner` — holds the key. Install pointer: F-Droid first, GitHub releases as fallback.

Do not add DataStore, security-crypto, secp256k1, Kotlinx Serialization, Hilt, or Koin. A later `sign_event` slice can grow a `SignerIntentBridge` mutex. This slice does not.

### Expected Features

See [FEATURES.md](FEATURES.md). Table stakes are exactly PROJECT.md Active. There is no P2 in this slice.

**Must have (table stakes):**
- Connect via Amber (`get_public_key`, no `package` on first call) — the only way to become logged in
- Persist pubkey hex + signer package; show npub — survives process death; do not re-ask Amber
- Sign out deletes that pair — local only; Amber keeps the key
- Missing-signer UX — say Amber is missing and point at F-Droid / GitHub; do not dead-tap Connect
- Reading remains ungated — already shipped; login sits beside Home, it does not replace it

**Should have (competitive):**
- Identity-only login — no `permissions` extra, no signer stack
- Thin helper, not `NostrSigner` — Dark Wisp login shape without Amethyst's tree
- Always-visible missing-Amber path — explain and link install; do not hide the affordance the way Dark Wisp hides the button

**Defer (v2+):**
- Bunker / NIP-46 — after Amber login works
- `sign_event`, ContentResolver, NIP-44 — when a named reader action needs a signature
- Bookmarks, highlights, relays, profiles, feeds, zaps — companion webapp / social client
- `nsec` paste, watch-only npub paste, multi-account — anti-features for v1

### Architecture Approach

See [ARCHITECTURE.md](ARCHITECTURE.md). Login is a stored identity on the existing reader, not a new Activity and not a start-destination gate. New code stays under `org.dergigi.boris`. Do not extend leftover `com.readwithboris` sources.

STACK's `AmberLogin` helper and ARCHITECTURE's `RemoteSignerBridge` + `SignerIntentBridge` are the same thin surface with different names. Use the Dark Wisp names so the copy map stays obvious, but keep them login-shaped: no sign mutex, no `RemoteSigner`, no ContentResolver.

**Major components:**
1. `RemoteSignerBridge` — `isSignerAvailable` + `buildGetPublicKeyIntent` (no package, no permissions)
2. `SignerHost` in Compose — owns `rememberLauncherForActivityResult`; domain never launches
3. `Session` / `SessionStore` — persist / restore / clear the pair; cold start reads the store
4. `Nip19` — npub encode/decode only
5. `AuthViewModel` + `AuthBar` on Home — Connect / Amber missing / npub + sign out

`SignerIntentBridge` is optional glue so the ViewModel can post an intent and the host can deliver a result. If a single `AmberLogin` helper plus a callback is smaller and still keeps launch out of domain code, that is fine. Do not port Dark Wisp's `requestSignWithRetry`.

### Critical Pitfalls

See [PITFALLS.md](PITFALLS.md). All of them belong in the Amber login phase.

1. **Missing `<queries>`** — Amber looks uninstalled on API 30+. Add the NIP-55 block before any install check.
2. **Re-call `get_public_key` while logged in** — fingerprinting plus Amber on every launch. Restore from `SessionStore`.
3. **Amber `npub1` stored as the key** — spec says hex, Amber often returns bech32. Normalize on the way in; display npub.
4. **Pin Amber's package on first intent** — skips other signers and fails if the pin is wrong. Omit `package`; persist extras.`package`.
5. **Domain launches Amber / rotate drops the result** — Compose owns the launcher at Activity scope; one in-flight Connect; persist before any navigate.
6. **Reject vs Back vs missing Amber as one error** — three outcomes, three strings. Never request or log an `nsec`. Exclude the login file from backup.

## Implications for Roadmap

Research does not force a split. Manifest, discovery, intent, result parse, persist, Home chrome, and the pitfall list are one vertical. Coarse granularity: **one phase**.

### Phase 1: Amber login
**Rationale:** Every Active requirement, every architecture component, and every listed pitfall is this handshake. Splitting "manifest + store" from "UI" would leave login untestable on device. Bunker and `sign_event` are later milestones, not later phases of this one.
**Delivers:** Connect via Amber, persisted identity (hex + signer package), npub on Home, sign out, missing-Amber install path. Reader paste / share / open / markdown unchanged and still usable logged out.
**Addresses:** FEATURES.md P1 table stakes (connect, persist + show npub, sign out, missing-signer UX, ungated reading) plus the thin-helper scope control.
**Avoids:** All PITFALLS.md items 1–11 (queries, extras, npub/hex, re-ask, package pin, rotation, reject/back, nsec/logging, backup, launcher layering, dead Connect). Also avoids STACK/ARCHITECTURE anti-patterns: Quartz, DataStore, encrypted prefs, `permissions` JSON, auth gate, second Activity.
**Uses:** Existing Compose / activity-compose / Lifecycle. New files only: `data/Session*.kt`, `nostr/*`, `ui/auth/*`, manifest `<queries>`, strings.
**Implements:** Architecture build order 1–8 in one phase (queries → bridge → Nip19 + tests → SessionStore → launcher host → AuthBar on Home).

### Phase Ordering Rationale

- There is no Phase 2 in this milestone. Dependencies inside the phase are implementation order (manifest before discovery, store before "skip Amber while logged in"), not roadmap phases.
- Grouping follows the architecture: protocol (`nostr/`), persistence (`data/`), UI (`ui/auth` on Home). Reader stays untouched.
- Parking pitfalls in a later bunker / sign-event phase would ship a login that fails on API 30+, re-prompts Amber, or holds the wrong encoding.

### Research Flags

Phases likely needing deeper research during planning:
- None. NIP-55, Dark Wisp sources, and Amber's package/install path were read 2026-08-14. Planning should copy those files, not re-research the protocol.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Amber login):** Well-documented intent + Activity Result + prefs. Device UAT with Amber installed (and missing) on API 30+ is the real gate, not more library research.

If a later milestone adds `sign_event` or bunker, that milestone needs its own research. Do not pre-build the signer tree here.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | NIP-55 raw + Dark Wisp source + official Android docs. No new deps. |
| Features | HIGH | Four Active requirements match NIP-55 + Dark Wisp login shape. Anti-features are explicit in PROJECT.md. |
| Architecture | HIGH | Dark Wisp file map + existing Boris package layout. One naming tension (AmberLogin vs bridges) resolved above. |
| Pitfalls | HIGH | Spec + Dark Wisp + Amethyst extras, all mapped to this phase. |

**Overall confidence:** HIGH

### Gaps to Address

- **Amber `result` encoding:** Spec says hex; Dark Wisp sees `npub1`. Handle both. Confirm on a current Amber build during UAT.
- **Install URL:** STACK picks F-Droid as primary. FEATURES also lists GitHub, Zapstore, Obtainium. Ship F-Droid + GitHub; do not send users to Play Store first.
- **Backup exclude:** PITFALLS wants `dataExtractionRules` / `fullBackupContent` (or `allowBackup=false`). Decide the smallest manifest change in planning; do not leave default backup on the new prefs file.
- **Bridge thickness:** Prefer Dark Wisp names with a login-only result type. If the mutex object feels heavy for one Connect, collapse to a helper plus `SignerHost`. Do not grow a `NostrSigner` interface to resolve the naming.
- **Rotation + share collision:** Boris already has an `incomingUrl` / `LaunchedEffect` rotate bug. Login must not copy that pattern. UAT: rotate while Amber is open, then approve once.

## Sources

### Primary (HIGH confidence)
- [NIP-55](https://github.com/nostr-protocol/nips/blob/master/55.md) (raw 2026-08-14) — intents, extras, `<queries>`, omit package on first `get_public_key`, store pair, do not re-call
- [NIP-19](https://github.com/nostr-protocol/nips/blob/master/19.md) — npub test vectors
- Dark Wisp `RemoteSignerBridge`, `SignerIntentBridge`, `AuthScreen`, `KeyRepository.savePubkeyOnly`, `Nip19.kt`, manifest `<queries>` (fetched 2026-08-14)
- [Amber README](https://github.com/greenart7c3/Amber) — package `com.greenart7c3.nostrsigner`, F-Droid / GitHub
- Official Android: [`<queries>`](https://developer.android.com/guide/topics/manifest/queries-element), [Activity](https://developer.android.com/jetpack/androidx/releases/activity), [EncryptedSharedPreferences deprecated](https://developer.android.com/reference/kotlin/androidx/security/crypto/EncryptedSharedPreferences)
- Local `.planning/PROJECT.md` and `.planning/codebase/` (2026-08-14)

### Secondary (MEDIUM confidence)
- [Amethyst `IntentResult` / ExternalSignerButton](https://github.com/vitorpamplona/amethyst) — extras `result` / `package` / `rejected`; always-visible Amber button (fallback, not the v1 stack)
- [Dark Wisp PR #43](https://github.com/barrydeen/dark-wisp-android/pull/43) — REMOTE accounts must show npub from stored pubkey
- [DataStore 1.2.1](https://developer.android.com/jetpack/androidx/releases/datastore) — exists; not used in v1

### Tertiary (LOW confidence)
- [NostrAndroid notes](https://github.com/chebizarro/NostrAndroid) — older signers may still return `signature`; parser should fall back
- [Wisp PR #531](https://github.com/barrydeen/wisp/pull/531) — upstream Wisp removed NIP-55; do not treat Wisp main as the Amber reference

---
*Research completed: 2026-08-14*
*Ready for roadmap: yes*
