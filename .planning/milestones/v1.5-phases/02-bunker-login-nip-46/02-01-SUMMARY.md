---
phase: 02-bunker-login-nip-46
plan: 01
subsystem: auth
tags: [nip-46, nip-44, bunker, secp256k1, keystore, okhttp]

requires:
  - phase: 01-amber-login
    provides: SessionStore, AuthBar, RemoteSignerBridge, boris_session backup excludes
provides:
  - BunkerUri fail-closed parse
  - BunkerClient connect then get_public_key with onAuthUrl
  - Sealed Session Amber or Bunker
  - SecretBox Keystore wrap of client privkey and bunker secret
  - Home bunker field beside Amber chrome
  - VIEW/share split so bunker tokens never enter the reader
affects: [auth, home, session]

actuals:
  tokens: 16218
  tasks: 3
  commits: 3

tech-stack:
  added: [fr.acinq.secp256k1:secp256k1-kmp 0.22.0, secp256k1-kmp-jni-android, secp256k1-kmp-jni-jvm, org.bouncycastle:bcprov-jdk18on 1.85.2]
  patterns: [Connecting(prior) auth chrome, one identity save-clears-other, pubKeyTweakMul conversation key, onAuthUrl ACTION_VIEW]

key-files:
  created:
    - app/src/main/java/org/dergigi/boris/nostr/BunkerUri.kt
    - app/src/main/java/org/dergigi/boris/nostr/BunkerClient.kt
    - app/src/main/java/org/dergigi/boris/nostr/Nip44.kt
    - app/src/main/java/org/dergigi/boris/nostr/Nip01Event.kt
    - app/src/main/java/org/dergigi/boris/nostr/RelaySocket.kt
    - app/src/main/java/org/dergigi/boris/nostr/ClientKeypair.kt
    - app/src/main/java/org/dergigi/boris/data/SecretBox.kt
    - app/src/test/java/org/dergigi/boris/nostr/BunkerUriTest.kt
    - app/src/test/java/org/dergigi/boris/nostr/Nip44Test.kt
  modified:
    - app/src/main/java/org/dergigi/boris/data/Session.kt
    - app/src/main/java/org/dergigi/boris/data/SessionStore.kt
    - app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt
    - app/src/main/java/org/dergigi/boris/ui/auth/AuthBar.kt
    - app/src/main/java/org/dergigi/boris/MainActivity.kt
    - app/src/main/java/org/dergigi/boris/ui/BorisApp.kt
    - gradle/libs.versions.toml
    - settings.gradle.kts

key-decisions:
  - "Pinned secp256k1-kmp 0.22.0 because 0.24.0 is Kotlin 2.3 metadata and this app compiles with Kotlin 2.1.21"
  - "mavenCentral() first so the ACINQ AAR resolves; Huawei mirror had the POM without the AAR"
  - "Success wraps the pair's clientPrivkey; Connecting(prior) keeps Amber chrome; refresh skips only while the pair Job isActive"

patterns-established:
  - "Pattern 1: BunkerClient.pair returns Success(userHex, clientPrivkey); ViewModel re-parses the URI and wraps that key"
  - "Pattern 2: AuthUiState.Connecting(prior) still draws Amber Connect or Zapstore-first links; only Connect bunker is disabled"
  - "Pattern 3: NIP-44 conversation key is pubKeyTweakMul then bytes 1..32 then HKDF-Extract salt nip44-v2"

requirements-completed: [AUTH-05, AUTH-01, AUTH-02, AUTH-03, AUTH-04, READ-01]

coverage:
  - id: D1
    description: Fail-closed bunker:// parse (scheme, 64 hex, wss relays, reject nsec1, decode percent-encoded relays)
    requirement: AUTH-05
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/BunkerUriTest.kt
        status: pass
    human_judgment: false
  - id: D2
    description: Official NIP-44 conversation_key and one v2 encrypt/decrypt vector
    requirement: AUTH-05
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/nostr/Nip44Test.kt
        status: pass
    human_judgment: false
  - id: D3
    description: Amber fromStored still works; bunker fromStored is fail-closed; save of one kind lists the other keys to clear
    requirement: AUTH-02
    verification:
      - kind: unit
        ref: app/src/test/java/org/dergigi/boris/data/SessionStoreTest.kt
        status: pass
    human_judgment: false
  - id: D4
    description: Debug APK builds with bunker scheme, Home chrome, and connect then get_public_key client
    requirement: AUTH-05
    verification:
      - kind: other
        ref: ./gradlew :app:test :app:assembleDebug
        status: pass
    human_judgment: false
  - id: D5
    description: Live bunker pair, restart, sign out, Amber replace, VIEW fill-without-connect, read while logged out
    requirement: AUTH-05
    verification: []
    human_judgment: true
    rationale: Pairing needs a live bunker token and human approval on another device; JVM tests cannot complete connect or auth_url

duration: 12min
completed: 2026-08-14
status: complete
---

# Phase 2 Plan 01: Bunker login (NIP-46) Summary

**Thin NIP-46 client on Home: paste or VIEW a bunker token, connect then get_public_key, persist wrapped secrets, show npub**

## Performance

- **Duration:** 12 min
- **Started:** 2026-08-14T20:20:02Z
- **Completed:** 2026-08-14T20:32:00Z
- **Tasks:** 3
- **Files modified:** 23

## Accomplishments

- Home has a bunker field beside Amber Connect or Zapstore-first install links. Connecting(prior) keeps that Amber chrome and disables only Connect bunker.
- Pairing is NIP-46 `connect` then `get_public_key` over OkHttp WebSockets to URI `wss://` relays. `onAuthUrl` opens the bunker-supplied URL with `FLAG_ACTIVITY_NEW_TASK`. Success wraps the same client privkey the pair generated.
- Session is sealed Amber | Bunker in `boris_session`. Save of one kind clears the other. Keystore AES-GCM wraps client privkey and bunker secret. Sign out wipes prefs and the alias.
- VIEW/share of `bunker://` fills the Home field and does not open the reader or start pairing. Article URLs still go to Read.

## Task Commits

1. **Task 1: End-to-end bunker login on Home** - `487c131` (feat)
2. **Task 2: BunkerUri and NIP-44 official vector tests** - `c87430e` (test)
3. **Task 3: Session kind tests and Amber non-regression** - `3ef6d02` (test)

## Files Created/Modified

- `app/src/main/java/org/dergigi/boris/nostr/BunkerUri.kt` - Fail-closed bunker token parse
- `app/src/main/java/org/dergigi/boris/nostr/BunkerClient.kt` - Subscribe, connect, auth_url, get_public_key
- `app/src/main/java/org/dergigi/boris/nostr/Nip44.kt` - pubKeyTweakMul conversation key, v2 encrypt, NIP-04 receive fallback
- `app/src/main/java/org/dergigi/boris/nostr/Nip01Event.kt` - Kind 24133 id, schnorr sign/verify
- `app/src/main/java/org/dergigi/boris/nostr/RelaySocket.kt` - One OkHttp WebSocket per URI relay
- `app/src/main/java/org/dergigi/boris/nostr/ClientKeypair.kt` - Disposable secp256k1 client key
- `app/src/main/java/org/dergigi/boris/data/SecretBox.kt` - Keystore AES-GCM wrap/unwrap/wipe
- `app/src/main/java/org/dergigi/boris/data/Session.kt` - Sealed Amber | Bunker
- `app/src/main/java/org/dergigi/boris/data/SessionStore.kt` - kind keys; save replaces
- `app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt` - connectBunker pair Job; refresh skips only while isActive
- `app/src/main/java/org/dergigi/boris/ui/auth/AuthBar.kt` - Bunker field; Connecting keeps Amber chrome
- `app/src/main/java/org/dergigi/boris/MainActivity.kt` - incomingBunker split
- `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` - incomingBunker; reader only for http(s)
- `gradle/libs.versions.toml` / `app/build.gradle.kts` / `settings.gradle.kts` - ACINQ + BouncyCastle; mavenCentral first
- `app/src/test/java/org/dergigi/boris/nostr/BunkerUriTest.kt`
- `app/src/test/java/org/dergigi/boris/nostr/Nip44Test.kt`
- `app/src/test/java/org/dergigi/boris/data/SessionStoreTest.kt`

## Decisions Made

- Followed PLAN.md for `Connecting(prior)` and `Success(userHex, clientPrivkey)`, not the stale PATTERNS `data object Connecting`.
- secp256k1-kmp 0.22.0 instead of 0.24.0 so the app stays on Kotlin 2.1.21. Same vendor and `pubKeyTweakMul` API.
- mavenCentral() before the Huawei mirror so the JNI AAR actually downloads.
- Cancel the pair Job if Amber succeeds or the user signs out, so one identity cannot be overwritten by a late bunker result.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Resolve ACINQ AAR from Maven Central**
- **Found during:** Task 1
- **Issue:** Huawei Cloud Maven had metadata for secp256k1-kmp-jni-android 0.24.0 but not the AAR.
- **Fix:** Put `mavenCentral()` first in `dependencyResolutionManagement`.
- **Files modified:** `settings.gradle.kts`
- **Verification:** AAR resolved; debug APK later assembled
- **Committed in:** `487c131`

**2. [Rule 3 - Blocking] Pin secp256k1-kmp 0.22.0**
- **Found during:** Task 1
- **Issue:** 0.24.0 (and 0.23.0) ship Kotlin 2.3 metadata. This project compiles with Kotlin 2.1.21 (reads up to 2.2.0).
- **Fix:** Use 0.22.0 (kotlin-stdlib 2.2.21). API still has `pubKeyTweakMul`, `signSchnorr`, `pubkeyCreate`.
- **Files modified:** `gradle/libs.versions.toml`
- **Verification:** `:app:compileDebugKotlin` succeeded; Nip44 official vectors passed
- **Committed in:** `487c131`

**3. [Rule 3 - Blocking] Exclude duplicate OSGi manifest**
- **Found during:** Task 1
- **Issue:** `bcprov-jdk18on` and `jspecify` both ship `META-INF/versions/9/OSGI-INF/MANIFEST.MF`.
- **Fix:** Packaging exclude that path.
- **Files modified:** `app/build.gradle.kts`
- **Verification:** `:app:assembleDebug` exits 0
- **Committed in:** `487c131`

**4. [Rule 2 - Missing Critical] Cancel pair Job on Amber success and sign out**
- **Found during:** Task 1
- **Issue:** A late bunker Success could overwrite a just-saved Amber session (D-06).
- **Fix:** `pairJob?.cancel()` in `onSignerResult` Success and `signOut`; ignore bunker result when the job is no longer active.
- **Files modified:** `app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt`
- **Verification:** Compile; one-identity save path still clears the other kind
- **Committed in:** `487c131`

---

**Total deviations:** 4 auto-fixed (3 blocking, 1 missing critical)
**Impact on plan:** Needed to compile and keep one identity. No extra pairing schemes or Quartz.

## Issues Encountered

- 0.24.0 published 2026-08-13 with Kotlin 2.3. Plan lock is newer than this app's compiler. 0.22.0 is the last ACINQ release that compiles here.
- Official NIP-44 conversation_key `c41c7753…` and the `plaintext=a` payload both passed on the JVM.

## User Setup Required

A live bunker token from Amber remote or nsec.app is still required for device UAT (pair, auth_url, restart, sign out, Amber replace). Amber should stay installed so Connect can replace a bunker session. Network required (URI relays are wss only).

## Next Phase Readiness

- Identity store can hold Amber or Bunker. Later `sign_event` (AUTH-06) should reopen a socket from stored relays + unwrapped client key, not invent a second session file.
- Do not reconnect on resume just to show npub. Restore is prefs only.
- Remaining UAT is on-device with a real bunker token.

## Self-Check: PASSED
