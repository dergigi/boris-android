# Phase 2: Bunker login (NIP-46) - Research

**Researched:** 2026-08-14
**Domain:** Android NIP-46 client (`bunker://` pair, kind 24133, NIP-44, OkHttp WebSocket)
**Confidence:** HIGH (NIP-46/44/01 fetched this session + Amethyst login shape read + existing Boris Phase 1 code read + Maven Central versions)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

### Deferred Ideas (OUT OF SCOPE)
- `nostrconnect://` (client shows a QR for the signer to scan)
- Camera QR scan of `bunker://`
- `sign_event` / NIP-44 through the stored bunker (AUTH-06)
- Heartbeat / connection-status chrome
- Boris as a bunker server
- Multi-account
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-05 | Pair a bunker by pasting `bunker://` (or opening one via VIEW). Boris is the NIP-46 client, stores user pubkey, shows npub. Amber still works. Never hold an `nsec`. | `BunkerUri` + `BunkerClient` (`connect` then `get_public_key`); extend `Session`/`SessionStore`; split VIEW so `bunker://` does not open the reader |
| AUTH-01 | Connect via NIP-55 `get_public_key` in Amber; first intent has no `package` | Do not touch `RemoteSignerBridge.buildGetPublicKeyIntent()`. Keep Amber Connect on Home |
| AUTH-02 | Persist pubkey + signer package; restart shows npub without opening Amber | Existing Amber prefs keys stay valid. Bunker save overwrites them. Restore is `SessionStore.load()` only |
| AUTH-03 | Sign out deletes the stored pair; next connect is fresh | `SessionStore.clear()` plus Keystore wipe of the client key. Do not reuse the old client-keypair |
| AUTH-04 | Missing Amber: Zapstore first, then F-Droid / GitHub; Connect does not fail silently | Keep `AuthUiState.MissingSigner` + install links. Add bunker field beside them |
| READ-01 | Paste / share / open a URL and read while logged out | Login chrome stays on Home. `bunker://` VIEW must not navigate to `Routes.reader` |
</phase_requirements>

## Summary

Phase 2 adds a second identity path on Home: the user pastes or opens a `bunker://` token, Boris acts as a NIP-46 *client*, talks to the URI's `wss://` relays over OkHttp WebSockets, and learns the *user* pubkey via `connect` then `get_public_key`. After that, the live socket can close. Restart shows the stored npub the same way Amber does. Sign out wipes the disposable client-keypair. Amber login stays. Reading stays ungated. Boris never holds the user's `nsec`.

The protocol is small and well specified. A full Nostr client (Quartz, rust-nostr-sdk) is the wrong size. The stack that fits this Compose app is: in-repo URI/RPC/event glue, ACINQ `secp256k1-kmp` for keygen/Schnorr/unhashed ECDH, BouncyCastle lightweight ChaCha20 for NIP-44, OkHttp WebSocket (already on the classpath), Android Keystore AES-GCM to wrap the two secrets inside the existing `boris_session` prefs file.

**Primary recommendation:** Copy Amethyst's *login sequence*, not its tree. Parse `bunker://`, generate a disposable secp256k1 client key, subscribe for kind `24133` `p`-tagged to the client pubkey, NIP-44-encrypt `connect` (metadata `name=Boris`) then `get_public_key`, persist the user hex plus wrapped secrets, show npub, close the socket. Split incoming `bunker://` away from the article reader before anything else.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Paste / validate `bunker://` | Browser / Client (Home Compose) | — | Auth chrome stays on Home. Validation is local, fail-closed |
| `ACTION_VIEW` / share of `bunker://` | Browser / Client (`MainActivity`) | Home AuthBar | Same entry as article VIEW, but must *not* enter the reader |
| Disposable client-keypair | API / Backend (in-process `nostr/`) | Database / Storage (Keystore wrap) | Generated in Boris; never the user key; secret at rest |
| Kind 24133 + NIP-44 RPC | API / Backend (`BunkerClient`) | — | Protocol glue. Not Amber intents. Not Quartz |
| Relay WebSocket | API / Backend (`RelaySocket` + OkHttp) | CDN / Static (the bunker's relays) | Relays come from the URI only. `wss://` required |
| `connect` + `get_public_key` | API / Backend | Browser / Client (`auth_url` VIEW) | RPC on the socket. `auth_url` opens a browser and keeps waiting |
| Persist identity | Database / Storage (`SessionStore`) | — | One store, one identity. Amber keys unchanged |
| Show npub / sign out | Browser / Client (`AuthBar`) | Database / Storage | Restore is a prefs read. Sign out is a local wipe |
| Amber NIP-55 | Browser / Client (existing) | — | Do not fold bunker into `RemoteSignerBridge` |
| Article read | Browser / Client + API (existing Jina path) | — | Ungated. Login does not sit on this path |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| OkHttp WebSocket | 4.12.0 (already in catalog) | One-or-few `wss://` relay connections | Already used for article fetch. No new HTTP client. [VERIFIED: gradle/libs.versions.toml:9] `okhttp = "4.12.0"` |
| `fr.acinq.secp256k1:secp256k1-kmp` | 0.24.0 | Keygen, `pubkeyCreate`, Schnorr sign/verify | Official Kotlin wrapper of Bitcoin Core libsecp256k1. [VERIFIED: repo1.maven.org/.../secp256k1-kmp/maven-metadata.xml] `<latest>0.24.0</latest>` |
| `fr.acinq.secp256k1:secp256k1-kmp-jni-android` | 0.24.0 | Native JNI on device | Required Android artifact per [CITED: github.com/ACINQ/secp256k1-kmp README] |
| `fr.acinq.secp256k1:secp256k1-kmp-jni-jvm` | 0.24.0 | Native JNI for JVM unit tests | Needed so `Nip44Test` / event-sign tests run under `./gradlew :app:test` |
| `org.bouncycastle:bcprov-jdk18on` | 1.85.2 | ChaCha20 (RFC 8439) for NIP-44 | Android `Cipher.getInstance("ChaCha20")` is API 28+; `minSdk` is 26. Use the *lightweight* API (`ChaCha7539Engine`), do not register the JCE provider (Android ships a stale `org.bouncycastle`). [VERIFIED: repo1.maven.org/.../bcprov-jdk18on/maven-metadata.xml] `<latest>1.85.2</latest>` |
| Android Keystore AES-GCM | platform (API 23+) | Wrap client privkey + bunker secret | EncryptedSharedPreferences is deprecated in 1.1.0. [CITED: developer.android.com/reference/kotlin/androidx/security/crypto/EncryptedSharedPreferences] `Deprecated in 1.1.0` |
| `org.json` / `JSONObject` | Android platform | Request/response + NIP-01 event JSON | No kotlinx.serialization on the classpath. Keep it that way |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `javax.crypto.Mac` HMAC-SHA256 | platform | NIP-44 HMAC + HKDF-Extract/Expand | Prefer platform HMAC; implement HKDF from RFC 5869 (tiny). Do not add a HKDF library |
| `java.security.MessageDigest` SHA-256 | platform | NIP-01 event id | Already implicit on the JVM |
| Existing `Nip19` | in-repo | Display npub from user hex; normalize bunker `get_public_key` if a bunker returns `npub1` | Do not add a bech32 library |
| Existing `SessionStore` | in-repo | One identity file `boris_session` | Extend, do not replace |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| In-repo NIP-46 client | Amethyst Quartz `nip46RemoteSigner` | Locked out. Full KMP signer tree |
| In-repo NIP-46 client | `org.rust-nostr:nostr-sdk` (0.45.0-alpha.7) | Full relay-pool SDK, JNA, ALPHA API, its own networking (not OkHttp). Wrong size |
| ACINQ secp256k1-kmp | Hand-rolled curve math | Do not. Schnorr + unhashed ECDH are easy to get wrong |
| BouncyCastle ChaCha20 | Android `ChaCha20` Cipher | API 28+ only. `minSdk` 26 |
| Keystore AES-GCM wrap | `EncryptedSharedPreferences` | Deprecated 1.1.0; official docs say use `SharedPreferences` instead. We still must encrypt the two secrets, so wrap them ourselves |
| Keystore AES-GCM wrap | DataStore + Tink | Project forbids new DI/DataStore for this record. Two ciphertext strings fit prefs |
| `org.json` | kotlinx.serialization | Not on the classpath. Do not add it for one RPC |

**Installation** (version catalog, then `app/build.gradle.kts`):

```kotlin
// gradle/libs.versions.toml
secp256k1 = "0.24.0"
bouncycastle = "1.85.2"

secp256k1-kmp = { group = "fr.acinq.secp256k1", name = "secp256k1-kmp", version.ref = "secp256k1" }
secp256k1-jni-android = { group = "fr.acinq.secp256k1", name = "secp256k1-kmp-jni-android", version.ref = "secp256k1" }
secp256k1-jni-jvm = { group = "fr.acinq.secp256k1", name = "secp256k1-kmp-jni-jvm", version.ref = "secp256k1" }
bouncycastle-bcprov = { group = "org.bouncycastle", name = "bcprov-jdk18on", version.ref = "bouncycastle" }
```

```kotlin
implementation(libs.secp256k1.kmp)
implementation(libs.secp256k1.jni.android)
implementation(libs.bouncycastle.bcprov)
testImplementation(libs.secp256k1.jni.jvm)
```

**Version verification:** Maven Central metadata fetched 2026-08-14. ACINQ `lastUpdated` 20260813132209. BouncyCastle `lastUpdated` 20260807034313.

## Package Legitimacy Audit

The GSD npm legitimacy seam cannot rate Maven coordinates (`fr.acinq.secp256k1` / `org.bouncycastle` returned `SLOP` / `does-not-exist` on npm, as expected). Verdicts below are from official Maven Central metadata + official GitHub / project sites.

| Package | Registry | Age | Source Repo | Verdict | Disposition |
|---------|----------|-----|-------------|---------|-------------|
| `fr.acinq.secp256k1:secp256k1-kmp` (+ `jni-android`, `jni-jvm`) | Maven Central | since 0.5.x; current 0.24.0 | github.com/ACINQ/secp256k1-kmp | OK | Approved |
| `org.bouncycastle:bcprov-jdk18on` | Maven Central | jdk18on line since 1.71 (2022); current 1.85.2 | github.com/bcgit/bc-java | OK | Approved |
| `org.rust-nostr:nostr-sdk` | Maven Central | ALPHA | github.com/rust-nostr/nostr-sdk-ffi | OK as a product, wrong size | REMOVED from recommendation |
| `androidx.security:security-crypto` EncryptedSharedPreferences | AndroidX | deprecated 1.1.0 | AndroidX | deprecated | REMOVED |

**Packages removed due to [SLOP] verdict:** none (npm seam N/A)
**Packages flagged as suspicious [SUS]:** none
**Packages rejected for product reasons:** rust-nostr-sdk (full client, ALPHA, own networking); EncryptedSharedPreferences (deprecated); Quartz (locked)

## Architecture Patterns

### System Architecture Diagram

```
                    VIEW / SEND
                         │
            ┌────────────┴────────────┐
            │      MainActivity        │
            │  bunker://  vs  http(s)  │
            └────────────┬────────────┘
                         │
         bunkerUri       │        articleUrl
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ Home (AuthBar + URL field)                                   │
│  LoggedOut / MissingSigner: Amber chrome + bunker field      │
│  LoggedIn: npub + Sign out                                   │
│  AuthViewModel                                               │
└───────────────┬───────────────────────────────┬─────────────┘
                │                               │
                ▼                               ▼
┌───────────────────────────┐     ┌───────────────────────────┐
│ nostr/ (NEW bunker)       │     │ nostr/ (EXISTING Amber)   │
│ BunkerUri                 │     │ RemoteSignerBridge        │
│ ClientKeypair             │     │ SignerResults             │
│ Nip44 + Nip01Event        │     │ Nip19 (shared display)    │
│ RelaySocket (OkHttp WS)   │     └─────────────┬─────────────┘
│ BunkerClient              │                   │
│  1. subscribe kind 24133  │                   │ NIP-55 intent
│  2. connect (NIP-44)      │                   ▼
│  3. get_public_key        │              Amber (on device)
│  auth_url → ACTION_VIEW   │
└───────────────┬───────────┘
                │ wss:// from URI only
                ▼
         bunker relays  ──►  remote signer (nsec.app / Amber remote / …)
                │
                ▼
┌───────────────────────────┐
│ data/                     │
│ Session (Amber \| Bunker) │
│ SessionStore boris_session│
│ SecretBox (Keystore wrap) │
└───────────────────────────┘
```

Trace: paste/open `bunker://` → validate → generate client key → open URI relays → REQ kind 24133 `#p=client` → EVENT connect → maybe `auth_url` → EVENT get_public_key → persist user hex + wrapped secrets → show npub → close sockets.

### Recommended Project Structure

```
app/src/main/java/org/dergigi/boris/
├── MainActivity.kt                 # EDIT: split bunker:// from http(s)
├── data/
│   ├── Session.kt                  # EDIT: Amber | Bunker
│   ├── SessionStore.kt             # EDIT: kind + bunker fields; save replaces
│   └── SecretBox.kt                # NEW: Keystore AES-GCM wrap/unwrap/wipe
├── nostr/
│   ├── BunkerUri.kt                # NEW: parse + validate (pure Kotlin)
│   ├── ClientKeypair.kt            # NEW: generate / pub from priv
│   ├── Nip44.kt                    # NEW: v2 encrypt/decrypt
│   ├── Nip01Event.kt               # NEW: serialize, id, schnorr sign kind 24133
│   ├── RelaySocket.kt              # NEW: OkHttp WS REQ/EVENT/CLOSE
│   ├── BunkerClient.kt             # NEW: subscribe, connect, get_public_key, auth_url
│   ├── RemoteSignerBridge.kt       # UNCHANGED
│   ├── SignerResult.kt             # UNCHANGED
│   └── Nip19.kt                    # UNCHANGED (reuse normalizePubkey)
└── ui/
    ├── BorisApp.kt                 # EDIT: incomingBunker vs incomingUrl
    ├── auth/
    │   ├── AuthBar.kt              # EDIT: bunker field + Connect bunker
    │   ├── AuthViewModel.kt        # EDIT: connectBunker / replace session
    │   └── AuthUiState.kt          # EDIT: optional Connecting
    └── home/HomeScreen.kt          # EDIT: pass bunker URI + callbacks

app/src/test/java/org/dergigi/boris/
├── data/SessionStoreTest.kt        # EDIT: Amber still loads; bunker; replace
├── nostr/BunkerUriTest.kt          # NEW
├── nostr/Nip44Test.kt              # NEW: official vectors
└── nostr/Nip19Test.kt              # UNCHANGED
```

Do not add files under `com.readwithboris`. No new NavHost route. No second Activity.

### Pattern 1: Amethyst login sequence (copy the shape)

**What:** Parse URI → ephemeral signer → subscribe → wait for a URI relay → `connect` → `get_public_key`.
**When to use:** This is the whole pairing flow.
**Do not port:** Quartz `NostrSignerRemote`, `RemoteSignerManager`, serialization tree, bunker *server*.

Amethyst `BunkerLoginUseCase` (read this session):

```kotlin
const val RELAY_CONNECT_TIMEOUT_MS = 15_000L
// then: fromBunkerUri → openSubscription → wait until a URI relay is connected
// then: remoteSigner.connect() → remoteSigner.getPublicKey()
```

[VERIFIED: /tmp/amethyst/commons/.../BunkerLoginUseCase.kt:31-51]

`switch_relays` is a NIP-46 "should", not required for connect. Amethyst login does not send it. Skip it (D-03).

### Pattern 2: Kind 24133 request / response

NIP-46 request event (client → bunker):

- `kind` = `24133`
- `pubkey` = client pubkey
- `tags` = `[["p", <remote-signer-pubkey>]]`
- `content` = NIP-44 encrypt of `{"id","method","params"}`

Response event (bunker → client):

- `kind` = `24133`
- `pubkey` = remote-signer pubkey
- `tags` = `[["p", <client-pubkey>]]`
- `content` = NIP-44 encrypt of `{"id","result","error?"}`

Subscribe **before** publishing. Filter: `kinds=[24133]`, `#p=[client-pubkey]`. Kind 24133 is ephemeral (20000–29999); relays are not expected to store it. If you publish before the REQ is live, the response can vanish.

[CITED: github.com/nostr-protocol/nips/blob/master/46.md]
[CITED: github.com/nostr-protocol/nips/blob/master/01.md] ephemeral range `20000 <= n < 30000`

`connect` params (positional):

`[<remote-signer-pubkey>, <optional_secret>, <optional_requested_perms>, <optional_client_metadata>]`

Because metadata is present and perms are empty, send:

```json
["<remote-signer-hex>", "<secret-or-empty>", "", "{\"name\":\"Boris\",\"url\":\"https://github.com/dergigi/boris-android\"}"]
```

Empty string in slot 3 keeps metadata in slot 4. [CITED: nips/46.md Client metadata]

`connect` result is `"ack"` or a user pubkey. Still call `get_public_key`. Do not treat connect's result as the identity. [CITED: nips/46.md Changes: "must call get_public_key after connect"]

`get_public_key` params: `[]`. Result: user pubkey hex (normalize with `Nip19.normalizePubkey` in case a bunker returns `npub1`).

### Pattern 3: One Session, two kinds

Today `Session` is Amber-only and rejects a blank package:

```kotlin
data class Session(
    val pubkeyHex: String,
    val signerPackage: String,
)
```

[VERIFIED: app/src/main/java/org/dergigi/boris/data/Session.kt:3-14]

```kotlin
const val PREFS_NAME = "boris_session"
const val KEY_PUBKEY_HEX = "pubkey_hex"
const val KEY_SIGNER_PACKAGE = "signer_package"
```

[VERIFIED: app/src/main/java/org/dergigi/boris/data/SessionStore.kt:6-8]

Extend to a sealed type. Keep Amber load working when `kind` is absent (existing installs):

```kotlin
sealed interface Session {
    val pubkeyHex: String
    data class Amber(override val pubkeyHex: String, val signerPackage: String) : Session
    data class Bunker(
        override val pubkeyHex: String,
        val remoteSignerPubkey: String,
        val relays: List<String>,
        val clientPrivkeyCiphertext: String,
        val bunkerSecretCiphertext: String?,
    ) : Session
}
```

Prefs keys to add (names are planner-facing; Amber keys stay):

- `kind` = `amber` | `bunker` (missing + `signer_package` present ⇒ Amber)
- `remote_signer_pubkey`
- `relays` (comma-separated `wss://` URLs)
- `client_privkey` (Keystore-wrapped, never log)
- `bunker_secret` (Keystore-wrapped, optional)

`save()` writes one kind and clears the other kind's keys in the same `edit()`. That is how Amber replaces bunker and bunker replaces Amber.

### Pattern 4: Pairing is live; restore is not

After a successful `get_public_key`, close the WebSockets. Cold start reads `SessionStore` and shows npub. Do not reconnect on resume. Discretion allowed a live reconnect; it is not needed to meet the success criteria and would add heartbeat chrome (deferred).

NIP-46 `logout` needs a live socket, a new EVENT, and a wait. That is not cheap. Skip the RPC. Local wipe is the security boundary. [CITED: nips/46.md "The logout request is a courtesy hint"]

### Anti-Patterns to Avoid

- **Port Quartz / rust-nostr-sdk:** Locked / full client. Write ~6 small files.
- **Put bunker on `RemoteSignerBridge`:** That object speaks NIP-55 intents. WebSockets do not belong there.
- **`Secp256k1.ecdh()` for NIP-44:** ACINQ JNI calls `secp256k1_ecdh`, which hashes. NIP-44 wants the unhashed x coordinate. Use `pubKeyTweakMul` and take bytes 1..32 of the 65-byte uncompressed point. [VERIFIED: ACINQ JNI `secp256k1_ecdh(ctx, output, &pubkey, seckeyBytes, NULL, NULL)`] [CITED: nips/44.md "NIP44 doesn't do hashing of the output"]
- **Navigate `bunker://` to the reader:** `BorisApp` currently does `navController.navigate(Routes.reader(incomingUrl))` for every non-blank incoming string. [VERIFIED: app/src/main/java/org/dergigi/boris/ui/BorisApp.kt:33-38]
- **Plaintext client privkey in prefs:** Phase 1 stored public strings. This phase stores secrets. Wrap them.
- **EncryptedSharedPreferences:** Deprecated 1.1.0.
- **Register BouncyCastle as a JCE provider on Android:** Conflicts with the platform's old BC. Use the lightweight engine API.
- **Send NIP-04 ciphertext:** Current NIP-46 says NIP-44. Decrypt NIP-04 only as a receive fallback when the payload looks like `?iv=`.
- **Treat `auth_url` as an error:** `result == "auth_url"`, URL in `error`. Open it, keep waiting on the same request id. [VERIFIED: /tmp/amethyst/.../BunkerResponse.kt:29-30] `RESULT_AUTH_URL = "auth_url"`
- **Require `switch_relays` or `ping`:** Connect works without them.
- **New route / auth gate:** Reading stays first.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| secp256k1 keygen / Schnorr | Custom curve math | ACINQ secp256k1-kmp | BIP-340 edge cases, JNI-tested |
| NIP-44 ECDH | `Secp256k1.ecdh()` | `pubKeyTweakMul` + x-only | Hashed ECDH fails official vectors |
| ChaCha20 | Home-grown ARX | BouncyCastle `ChaCha7539Engine` | RFC 8439; API 26 must work |
| AES key storage | Roll a password KDF | Android Keystore AES-GCM | Hardware-backed when available |
| Relay pool / gossip | Mini Amethyst client | One OkHttp `WebSocket` per URI relay | Login talks to 1–few relays, then closes |
| npub display | New bech32 lib | Existing `Nip19` | Already tested against official vectors |

**Key insight:** The expensive mistakes are crypto (hashed ECDH, NIP-04-only, leaking the client key) and intent routing (`bunker://` opening the reader). The RPC itself is two JSON methods.

## Common Pitfalls

### Pitfall 1: `bunker://` opens the article reader

**What goes wrong:** VIEW or share of a bunker token navigates to `Routes.reader` and Jina fails.
**Why it happens:** `MainActivity.urlFrom` returns `intent.dataString` for every `ACTION_VIEW`. `BorisApp` treats any incoming string as an article URL.
**How to avoid:** Split in `MainActivity` (and share text): if the string starts with `bunker://`, expose `incomingBunker`; otherwise keep the article path. `BorisApp` must not call `Routes.reader` for bunker.
**Warning signs:** Home never shows the token; reader shows a fetch error.

Manifest today only has `http` / `https` VIEW filters. Add a sibling filter for `android:scheme="bunker"`. Do not add `nostrconnect`.

Share (`ACTION_SEND` `text/plain`): `UrlExtractor.extract` only finds `http(s)`. Parse `bunker://` first.

### Pitfall 2: Hashed ECDH (NIP-44 will not decrypt)

**What goes wrong:** `connect` is published; the bunker never answers, or Boris cannot decrypt the reply.
**Why it happens:** `Secp256k1.ecdh()` → `secp256k1_ecdh` SHA-256-hashes the shared point. NIP-44 forbids that.
**How to avoid:** `pubKeyTweakMul(remotePub33or65, clientPriv32)`, take the 32-byte x coordinate, then HKDF-Extract with salt `nip44-v2`. Pin official vectors in `Nip44Test` before wiring the socket.
**Warning signs:** Conversation-key vector `c41c7753…` does not match.

### Pitfall 3: `auth_url` treated as failure

**What goes wrong:** nsec.app / some bunkers return `{"result":"auth_url","error":"https://…"}`. UI shows an error; user never approves.
**Why it happens:** Naive code treats any `error` field as reject.
**How to avoid:** If `result == "auth_url"`, `ACTION_VIEW` the URL (once per distinct URL), keep the same request id open until the real response or timeout. Amethyst uses an unlimited channel so the challenge and the real reply both fit. [VERIFIED: /tmp/amethyst/.../RemoteSignerManager.kt:102-121]
**Warning signs:** Pairing dies in ~1s with a URL-shaped "error".

### Pitfall 4: NIP-04 vs NIP-44 bunkers

**What goes wrong:** Older bunkers (historically nsec.app) speak NIP-04 (`base64?iv=`). Current spec and nak/Amber/NDK default to NIP-44.
**Why it happens:** NIP-46 migrated; the wild is mixed.
**How to avoid:** Always *send* NIP-44. On receive: if ciphertext contains `?iv=` at the NIP-04 offset, decrypt NIP-04; else NIP-44. Do not implement the `nip04_*` / `nip44_*` *RPC methods* (D-03). Transport only.
**Warning signs:** Decrypt throws on a payload that ends with `?iv=`.

### Pitfall 5: Timeout too short for human approval

**What goes wrong:** User is still tapping Approve on another device; Boris already failed.
**Why it happens:** Relay-up (15s) is not the same as "user approved".
**How to avoid:** 15s to get at least one URI relay `onOpen`. 65s per RPC after that (Amethyst `RemoteSignerManager` default `timeout: Long = 65_000`). Distinct Home strings: bad URI, relay timeout, rejected, missing pubkey.
**Warning signs:** Works on a pre-approved bunker, fails on first-time nsec.app.

### Pitfall 6: Subscribe after publish

**What goes wrong:** Connect EVENT is sent; response is ephemeral and already gone.
**Why it happens:** Kind 24133 is ephemeral.
**How to avoid:** REQ first, wait for socket open, then EVENT. Same order as Amethyst `openSubscription` → wait → `connect`.

### Pitfall 7: Replacing Amber without wiping bunker secrets (or the reverse)

**What goes wrong:** Sign out or Amber login leaves a client privkey in prefs; next bunker connect reuses it, or a leftover Amber package makes `fromStored` build a hybrid.
**Why it happens:** Today's `fromStored` requires a non-empty `signer_package`. A naive bunker save that leaves that key set will still look like Amber — or fail closed.
**How to avoid:** `save(Bunker)` clears `signer_package`. `save(Amber)` clears bunker keys and deletes the Keystore entry. `clear()` wipes prefs *and* the Keystore alias.

### Pitfall 8: Logging or backing up the client key

**What goes wrong:** Logcat or cloud backup contains the transport key. Anyone with it can impersonate this pairing.
**Why it happens:** Phase 1 had no secrets; people debug by printing the session.
**How to avoid:** No `Log` (project convention). Backup already excludes `boris_session.xml`:

```xml
<exclude domain="sharedpref" path="boris_session.xml" />
```

[VERIFIED: app/src/main/res/xml/data_extraction_rules.xml:4]
[VERIFIED: app/src/main/res/xml/full_backup_content.xml:3]

Keep those excludes. Do not add a second prefs file. Wipe Keystore on sign out.

### Pitfall 9: `nsec1` pasted into the bunker field

**What goes wrong:** User pastes an nsec. Boris must refuse, not try to parse it as a pubkey.
**How to avoid:** `BunkerUri.parse` rejects `nsec1` anywhere in the input, plus the Amethyst checks (scheme, 64 hex, `relay=wss://`). [VERIFIED: /tmp/amethyst/desktopApp/.../BunkerUriUtils.kt:25-41]

### Pitfall 10: Amber Connect regresses when Amber is missing

**What goes wrong:** Bunker field replaces MissingSigner chrome.
**Why it happens:** One `when` branch, one child.
**How to avoid:** MissingSigner keeps the three install links *and* shows the bunker field. LoggedOut keeps Amber Connect *and* the bunker field.

## Code Examples

### Validate and parse `bunker://`

```kotlin
// Shape from Amethyst validateBunkerUri + fromBunkerUri. Pure Kotlin for JVM tests.
// Source: /tmp/amethyst/desktopApp/.../BunkerUriUtils.kt and NostrSignerRemote.fromBunkerUri
data class BunkerUri(
    val remoteSignerPubkey: String,
    val relays: List<String>,
    val secret: String?,
)

fun parseBunkerUri(input: String): BunkerUri? {
    val trimmed = input.trim()
    if (trimmed.contains("nsec1", ignoreCase = true)) return null
    if (!trimmed.startsWith("bunker://", ignoreCase = true)) return null
    val after = trimmed.substring("bunker://".length)
    val parts = after.split("?", limit = 2)
    val remote = parts[0].lowercase()
    if (remote.length != 64 || remote.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
    if (parts.size < 2) return null
    val relays = mutableListOf<String>()
    var secret: String? = null
    for (param in parts[1].split("&")) {
        val eq = param.indexOf('=')
        if (eq <= 0) continue
        val key = param.substring(0, eq)
        val value = java.net.URLDecoder.decode(param.substring(eq + 1), Charsets.UTF_8.name())
        when (key) {
            "relay" -> if (value.startsWith("wss://", ignoreCase = true)) relays.add(value)
            "secret" -> if (value.isNotEmpty()) secret = value
        }
    }
    if (relays.isEmpty()) return null
    return BunkerUri(remote, relays, secret)
}
```

Decode query values. Amethyst's split does not; encoded `wss%3A%2F%2F` would fail.

### NIP-44 conversation key (unhashed ECDH)

```kotlin
// Source: nips/44.md get_conversation_key + Quartz computeConversationKey shape (do not port Quartz)
// ACINQ: pubKeyTweakMul, NOT ecdh
fun conversationKey(priv32: ByteArray, pubHex: String): ByteArray {
    val pub = hexToBytes(pubHex)
    val pubEnc = when (pub.size) {
        32 -> byteArrayOf(0x02) + pub // x-only → even-y compressed; tweak_mul needs a full point
        33, 65 -> pub
        else -> error("bad pub")
    }
    val uncompressed = Secp256k1.pubKeyTweakMul(pubEnc, priv32) // 65 bytes: 04 || x || y
    val sharedX = uncompressed.copyOfRange(1, 33)
    return hkdfExtract(ikm = sharedX, salt = "nip44-v2".toByteArray())
}
```

Pin this vector before any network work [CITED: nips/44.md]:

- `sec1` = `0000…0001`
- `sec2` = `0000…0002`
- `conversation_key` = `c41c775356fd92eadc63ff5a0dc1da211b268cbea22316767095b2871ea1412d`

If x-only `02||x` fails a vector, try the on-curve prefix (`02`/`03`) via `pubkeyCreate` of `sec2` instead of guessing. Official `get_conversation_key` uses `sec1` and `pub2`.

### Kind 24133 + OkHttp WebSocket

```kotlin
// Source: nips/01.md client-to-relay + nips/46.md kind 24133
// OkHttp 4.12 already on classpath (ReaderRepository)
val ws = client.newWebSocket(
    Request.Builder().url(relayWss).build(),
    object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send("""["REQ","b1",{"kinds":[24133],"#p":["$clientPubHex"]}]""")
        }
        override fun onMessage(webSocket: WebSocket, text: String) { /* EVENT / OK / EOSE / CLOSED / NOTICE */ }
    },
)
// after onOpen: ws.send("""["EVENT",$signedConnectJson]""")
```

NIP-01 id serialization is a JSON array with *no extra whitespace* and the listed escapes only. Sign `id` with `Secp256k1.signSchnorr`. Verify incoming events before decrypt. [CITED: nips/01.md] [CITED: nips/44.md "signature MUST be validated before decrypting"]

### Keystore wrap (not EncryptedSharedPreferences)

```kotlin
// Source: developer.android.com KeyGenParameterSpec AES-GCM example
val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
keyGenerator.init(
    KeyGenParameterSpec.Builder(
        "boris_bunker_wrap",
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build(),
)
keyGenerator.generateKey()
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
cipher.init(Cipher.ENCRYPT_MODE, secretKey)
// persist iv + ciphertext (hex or base64) in boris_session; never the raw privkey
```

[CITED: developer.android.com/reference/android/security/keystore/KeyGenParameterSpec]

On sign out: `prefs.edit().clear()` and `keyStore.deleteEntry("boris_bunker_wrap")`.

`SecretBox` uses Android APIs; do not unit-test it on the JVM. Test `Session` parsing of already-wrapped strings.

### Home chrome (no new route)

```kotlin
// LoggedOut: existing Amber Connect button + bunker field + Connect bunker
// MissingSigner: existing missing copy + Zapstore/F-Droid/GitHub + same bunker field
// LoggedIn: existing npub + Sign out (no bunker field)
```

`AuthUiState` today:

```kotlin
sealed interface AuthUiState {
    data object LoggedOut : AuthUiState
    data object MissingSigner : AuthUiState
    data class LoggedIn(val npub: String) : AuthUiState
}
```

[VERIFIED: app/src/main/java/org/dergigi/boris/ui/auth/AuthUiState.kt:3-6]

Add `data object Connecting : AuthUiState` (or a `message` + disable the button) so the 15s+65s wait is visible. Reuse `_message` for bad URI / timeout / reject, same as Amber `auth_rejected` / `auth_cancelled`.

Incoming bunker from VIEW: set the field via a `rememberSaveable` (or ViewModel) string. Do not auto-fire connect without a tap unless discuss later says so. CONTEXT says the token lands in the same field; a tap on Connect bunker is the explicit confirm. Auto-connect on VIEW is acceptable if the URI already validated; planner should pick one and keep it fail-closed. Recommendation: fill the field, do not auto-connect (avoids surprise network + `auth_url` from a shared link).

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| NIP-46 content = NIP-04 | NIP-46 content = NIP-44 | NIP-46 master (fetched 2026-08-14); NDK default NIP-44 (2025-10) | Send NIP-44; decrypt both |
| `nostrconnect://` as the only pair | `bunker://` from the signer | Current NIP-46 | This phase is bunker-only |
| EncryptedSharedPreferences | Keystore + ordinary prefs / DataStore | security-crypto 1.1.0 deprecated | Wrap two secrets; keep `boris_session` |
| Full Quartz remote signer | Thin login use case | Amethyst split `BunkerLoginUseCase` | Copy the use case, not Quartz |

**Deprecated/outdated:**

- EncryptedSharedPreferences: deprecated 1.1.0. Do not add `androidx.security:security-crypto`.
- NIP-04 as the *only* NIP-46 transport: spec now says NIP-44.
- rust-nostr-sdk Android bindings: ALPHA, full client. Do not adopt.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Filling the bunker field on VIEW without auto-connect is the right UX | Code Examples / UI | User may expect tap-free pair; easy to flip |
| A2 | Skipping NIP-46 `logout` RPC is acceptable (not cheap) | Pattern 4 | Some bunkers may keep a stale client-pubkey until they expire it; next pair uses a new key anyway |
| A3 | x-only remote pubkeys in `bunker://` are always 64 hex (no `npub1` host) | BunkerUri | A bunker that puts `npub1` in the host will fail closed (correct per D-12) |
| A4 | Official NIP-44 `pub2` can be derived with `pubkeyCreate(sec2)` if `02\|\|x` is the wrong prefix | NIP-44 example | Vector test will catch this before network work |

No compliance / retention assumptions. A1 is the only product choice left for the planner.

## Open Questions

1. **Auto-connect on VIEW?**
   - What we know: CONTEXT says the token lands in the same field as paste.
   - What's unclear: whether opening `bunker://` should start pairing immediately.
   - Recommendation: fill the field only (A1). Safer with `auth_url`.

2. **Which bunkers will we UAT?**
   - What we know: CONTEXT names Amber remote / nsec.app / similar.
   - What's unclear: whether the first device test is nsec.app (often `auth_url`) or Amber's bunker token.
   - Recommendation: planner's verify step should name both; `auth_url` must be implemented either way.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | Compile + JVM tests (JNI secp256k1) | ✓ | Temurin 17.0.20 | — |
| Gradle wrapper | Build | ✓ | 8.13 | — |
| Maven Central / Huawei Cloud Maven | New artifacts | ✓ | repos already in `settings.gradle.kts` | — |
| OkHttp | WebSocket | ✓ | 4.12.0 in catalog | — |
| INTERNET permission | Relays | ✓ | manifest | — |
| Device / emulator API 26+ + network | Manual UAT | ✓ (dev machine) | — | Cannot automate bunker approve in JVM tests |
| Live bunker (nsec.app or Amber remote) | UAT success criteria 1–3 | outside repo | — | Fail-closed unit tests still ship |

**Missing dependencies with no fallback:** none for compile. UAT needs a real bunker and network.

**Missing dependencies with fallback:** none.

Step 2.6 note: no extra CLI (no Redis, no Docker) required.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | NIP-46 `connect` + `get_public_key`; Amber NIP-55 unchanged |
| V3 Session Management | yes | One `boris_session`; sign out wipes prefs + Keystore; no live token besides the client key |
| V4 Access Control | no | No server-side user roles in Boris |
| V5 Input Validation | yes | `BunkerUri.parse` fail-closed; reject `nsec1`; `wss://` only (`usesCleartextTraffic=false`) |
| V6 Cryptography | yes | ACINQ secp256k1 + NIP-44 v2 + Keystore AES-GCM. Never hand-roll Schnorr/ChaCha/ECDH |

### Known Threat Patterns for NIP-46 Android client

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| User `nsec` entered in the bunker field | Information Disclosure | Reject `nsec1`; never persist or log it |
| Client-keypair stolen from backup / log | Information Disclosure / Spoofing | Keystore wrap; backup exclude already on `boris_session.xml`; no `Log`; wipe on sign out |
| Pairing spoof without secret | Spoofing | Send URI `secret` on `connect`; bunker should ignore reused secrets [CITED: nips/46.md] |
| `auth_url` phishing | Spoofing | Open the URL the bunker sent; do not inject a Boris-owned URL. Treat as display-only |
| Client metadata used as auth | Elevation of Privilege | Metadata is a UI hint only (`name=Boris`). Empty perms |
| Cleartext `ws://` relay | Information Disclosure | Require `relay=wss://`; app already `usesCleartextTraffic=false` |
| Hostile kind 24133 on the relay | Tampering | Verify Schnorr + `p` tag + NIP-44 MAC before trusting JSON |
| NIP-04 transport (key-leak class) | Information Disclosure | Send NIP-44 only |
| Hybrid Amber+bunker leftover keys | Tampering | `save()` of one kind clears the other |
| `bunker://` handled as http | Tampering / Denial | Split VIEW/share before `Routes.reader` |

## Project Constraints (from .cursor/rules/ and codebase)

No `.cursor/rules/` files exist in this repo.

From `.planning/codebase/CONVENTIONS.md` and PROJECT.md (treat as binding):

- New Kotlin only under `org.dergigi.boris`. Do not extend `com.readwithboris`.
- No Hilt/Koin, no Room, no DataStore for this record.
- Official Kotlin style, 4-space indent, no wildcard imports, no KDoc/`TODO` in committed Kotlin.
- No `Log` / Timber / `println`.
- File-per-concern; tests `{Type}Test.kt` mirroring the production package.
- Version catalog first (`gradle/libs.versions.toml`), then `app/build.gradle.kts`.
- `minSdk` 26. Stay on `0.x.y`.
- Never request, persist, or log an `nsec`.

## Sources

### Primary (HIGH confidence)

- https://github.com/nostr-protocol/nips/blob/master/46.md — `bunker://`, `connect`, `get_public_key`, kind 24133, NIP-44, `auth_url`, `logout` (raw fetched 2026-08-14)
- https://github.com/nostr-protocol/nips/blob/master/44.md — v2 ECDH/HKDF/ChaCha20/HMAC, unhashed shared x, official vectors (raw fetched 2026-08-14)
- https://github.com/nostr-protocol/nips/blob/master/01.md — event id serialization, REQ/EVENT, ephemeral kinds (raw fetched 2026-08-14)
- `/tmp/amethyst/commons/.../BunkerLoginUseCase.kt` — login sequence, 15s relay wait
- `/tmp/amethyst/desktopApp/.../BunkerUriUtils.kt` — URI validation
- `/tmp/amethyst/quartz/.../NostrSignerRemote.kt` — `fromBunkerUri`, `connect`, `getPublicKey` (protocol shape only)
- `/tmp/amethyst/quartz/.../RemoteSignerManager.kt` — `auth_url` + 65s timeout
- `/tmp/amethyst/quartz/.../BunkerRequestConnect.kt` — metadata 4th param
- Boris Phase 1 sources read this session: `Session.kt`, `SessionStore.kt`, `AuthBar.kt`, `AuthViewModel.kt`, `AuthUiState.kt`, `MainActivity.kt`, `BorisApp.kt`, `HomeScreen.kt`, backup XML, `libs.versions.toml`
- Maven Central metadata for secp256k1-kmp 0.24.0 and bcprov-jdk18on 1.85.2
- https://github.com/ACINQ/secp256k1-kmp — README + `Secp256k1.kt` + JNI `secp256k1_ecdh`
- https://developer.android.com/reference/kotlin/androidx/security/crypto/EncryptedSharedPreferences — deprecated 1.1.0
- https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec — AES-GCM example

### Secondary (MEDIUM confidence)

- NDK commit 8f116fa (2025-10-23) — NIP-46 default encrypt NIP-44, NIP-04 fallback
- nips#1248 / nips#1095 — mixed bunker NIP-04/44 rollout; Amber auto both; nsec.app historically weaker on NIP-44
- rust-nostr-sdk Maven page — ALPHA full SDK (rejected)

### Tertiary (LOW confidence)

- Blog posts recommending DataStore+Tink after EncryptedSharedPreferences deprecation (not used; project forbids DataStore here)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions from Maven Central this session; OkHttp already in the app; Keystore is platform API 23+
- Architecture: HIGH — locked CONTEXT + Amethyst login files + live Boris Phase 1 types
- Pitfalls: HIGH — VIEW/reader split and Session package requirement were read in-repo; ECDH hash confirmed in ACINQ JNI; `auth_url` confirmed in Quartz + NIP-46

**Research date:** 2026-08-14
**Valid until:** 2026-09-13 (30 days; NIP-46/44 are stable; Maven patch versions may move)

---

## Recommended approach (planner execute this)

1. **Intent split first.** `MainActivity` / `BorisApp`: `bunker://` → Home field. `http(s)` → reader. Add a `bunker` VIEW filter. Do not auto-connect.
2. **URI + session types (JVM-tested).** `BunkerUri.parse` (D-12 + `nsec1` reject). Sealed `Session.Amber` | `Session.Bunker`. Existing Amber prefs still load. `save` of one kind clears the other.
3. **Crypto before sockets.** ACINQ 0.24.0 + BC 1.85.2. `Nip44Test` official conversation-key + one encrypt/decrypt vector. Use `pubKeyTweakMul`, not `ecdh`. Lightweight ChaCha20. `Nip01Event` for kind 24133.
4. **`BunkerClient`.** OkHttp `WebSocket` per URI relay. REQ kind 24133 `#p=client` → wait `onOpen` (15s) → NIP-44 `connect` with `name=Boris` and empty perms → handle `auth_url` via `ACTION_VIEW` → `get_public_key` (65s each RPC) → return user hex. Then close sockets.
5. **Secrets.** `SecretBox` Keystore AES-GCM alias `boris_bunker_wrap`. Store iv+ciphertext in `boris_session`. Never log. Keep backup excludes. Wipe prefs + Keystore on sign out. Skip `logout` RPC.
6. **Home chrome.** LoggedOut / MissingSigner: keep Amber / install links; add bunker field + Connect bunker. LoggedIn: npub + Sign out. `Connecting` state. Distinct error strings. Amber launcher unchanged.
7. **Do not** port Quartz, add rust-nostr, touch `RemoteSignerBridge`, add a route, add Hilt/Room/DataStore, send `sign_event`, scan QR, or reopen a socket just to show npub.

That is the whole phase.
