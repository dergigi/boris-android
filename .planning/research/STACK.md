# Stack Research

**Domain:** Android NIP-55 / Amber login (add-on to existing Kotlin Compose reader)
**Researched:** 2026-08-14
**Confidence:** HIGH (NIP-55 spec + Dark Wisp source + official Android docs, cross-checked)

Subsequent milestone. Keep the shipped reader stack. Add only what login needs.

## Recommended Stack

### Keep (do not re-research or bump)

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | 2.1.21 | App language | Already the app. JVM 17. |
| Jetpack Compose BOM | 2025.06.01 | UI | Already the app. Material 3. |
| AndroidX Activity Compose | 1.10.1 | `ComponentActivity`, `rememberLauncherForActivityResult` | Already on the classpath. Activity Result API lives here. Do not bump to 1.13.0 for login. |
| AndroidX Lifecycle | 2.9.2 | ViewModel + `collectAsStateWithLifecycle` | Already used by `ReaderViewModel`. Same pattern for session state. |
| Navigation Compose | 2.9.2 | In-app routes | Login is a home-screen affordance, not a new nav graph. |
| minSdk / targetSdk | 26 / 35 | Device floor | Amber and Dark Wisp also require API 26+. Package visibility (`<queries>`) applies from API 30. |
| Gradle / AGP | 8.13 / 8.7.3 | Build | Version catalog in `gradle/libs.versions.toml` stays the source of truth. |

### Add for Amber login

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| NIP-55 intent (`nostrsigner:`) | draft spec, master @ 2026-08-14 | One-shot `get_public_key` | Protocol Amber implements. Dark Wisp's `RemoteSignerBridge.buildGetPublicKeyIntent` is the shape to copy: `ACTION_VIEW` + `Uri.parse("nostrsigner:")` + extra `type=get_public_key`. Do **not** set `package` on that first call. |
| Manifest `<queries>` | platform (API 30+) | Discover signer apps | Without this, `queryIntentActivities` is empty on API 30+ and login looks like "Amber missing" even when it is installed. |
| Activity Result API | already in activity-compose 1.10.1 | Receive Amber's result | NIP-55 returns via `registerForActivityResult` / `rememberLauncherForActivityResult`. Compose owns the launcher. Domain code must not launch activities. |
| `SharedPreferences` (plain) | platform | Persist pubkey hex + signer package | Two public strings. Zero new artifacts. Dark Wisp stores `pubkey` + `signer_package` the same way (they wrap it in EncryptedSharedPreferences only because they also store `nsec`; Boris must not). |
| Tiny NIP-19 helper (in-repo) | ~80–120 lines | hex ↔ npub | Display needs encode. Amber's `result` extra is `npub1…`, not hex (Dark Wisp `AuthScreen` decodes it). A library or Quartz is the wrong size. |
| Amber | `com.greenart7c3.nostrsigner` | External signer | F-Droid / GitHub / Zapstore. Boris never holds an `nsec`. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| None new | — | — | v1 adds no Maven coordinates. |
| `androidx.datastore:datastore-preferences` | 1.2.1 (stable as of 2026-07-29) | Async prefs | Later, if settings grow past this login pair. Not now. |
| `androidx.security:security-crypto` | deprecated 1.1.0 | Encrypted prefs | Never. Deprecated; Boris stores no secrets. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Version catalog | Add future libs here first | `gradle/libs.versions.toml` then `app/build.gradle.kts`. |
| JUnit 4.13.2 | Unit-test npub encode/decode | Use the official NIP-19 vector. No instrumented tests required for v1. |
| Device / emulator API 26+ with Amber installed | Manual login | Package-visibility bugs only show on API 30+. |

## Decisions (prescriptive)

### Persistence: SharedPreferences, not DataStore

**Use** `context.getSharedPreferences("boris_session", MODE_PRIVATE)` behind a small `SessionStore` in `org.dergigi.boris.data` (or `nostr/`).

Keys:

- `pubkey_hex` — 64-char lowercase hex
- `signer_package` — e.g. `com.greenart7c3.nostrsigner`

Sign out: `edit().clear().apply()`.

**Do not add DataStore.** Official Android still prefers DataStore in the abstract (Preferences DataStore 1.2.1, 2026-07-29). For two public strings in an app with no DI and no existing DataStore singleton, it is ceremony. Revisit when a third setting appears.

**Do not use EncryptedSharedPreferences.** Deprecated (`androidx.security.crypto` 1.1.0 docs: use `SharedPreferences` instead). Dark Wisp uses it because it stores `privkey`. Boris must not store keys, so encryption here only implies we have secrets.

### npub: tiny helper, not a library

Amber returns bech32 in `result`. NIP-55 says pubkeys are hex. Both are true in the wild. The helper must do both directions:

1. `npubEncode(hex)` — show the logged-in identity
2. `npubDecode(npub1…)` — normalize Amber's `result` to hex before persist

Copy only bech32 encode/decode + those two functions. Dark Wisp's `Nip19.kt` is ~11 KB and also does `nsec`, `note`, `nevent`, `nprofile`, `naddr`. Do not copy that file.

Pin the official NIP-19 vector in a JVM unit test:

- hex `7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e`
- npub `npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg`

If `result` is already 64 hex chars, store it. If it starts with `npub1`, decode. Anything else is a failed login.

### Manifest queries

Add the NIP-55 block (include `BROWSABLE`; Dark Wisp omits it, the spec has it):

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="nostrsigner" />
    </intent>
</queries>
```

Query by scheme, not by Amber's package. Other NIP-55 signers must work. Do not add a `<package android:name="com.greenart7c3.nostrsigner" />` query unless scheme discovery fails in testing.

Discovery (Dark Wisp `RemoteSignerBridge.isSignerAvailable` / NIP-55):

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:"))
context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
```

If empty: say Amber is missing and open `https://f-droid.org/packages/com.greenart7c3.nostrsigner/` (fallback: `https://github.com/greenart7c3/Amber/releases`). Do not send users to Play Store as the primary path.

### Activity Result API

Use what is already on the classpath:

```kotlin
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result -> /* parse extras */ }
```

Follow NIP-55's result rules (Dark Wisp `AuthScreen` skips these; Boris should not):

1. `resultCode != RESULT_OK` → signer error / crash, not a user reject
2. `data.getBooleanExtra("rejected", false)` → user rejected
3. else read `result` (pubkey) and `package` (signer package name)

Build the intent like Dark Wisp, minus permissions:

```kotlin
Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:")).apply {
    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    putExtra("type", "get_public_key")
    // do not set `package`
    // do not put Extra("permissions", …) — v1 does not sign
}
```

Compose owns the launcher. A one-shot login does not need Dark Wisp's `SignerIntentBridge` mutex / `StateFlow` queue. That exists for concurrent `sign_event` / NIP-44. Keep a thin `AmberLogin` helper that builds the intent and parses extras.

While logged in, do not call `get_public_key` again (NIP-55 + Dark Wisp).

## Installation

No new Gradle dependencies.

```kotlin
// gradle/libs.versions.toml — do not add datastore, quartz, secp256k1, security-crypto
```

Manifest: add the `<queries>` block above to `app/src/main/AndroidManifest.xml` (sibling of `<application>`, not inside it).

New code under `app/src/main/java/org/dergigi/boris/` only. Do not extend leftover `com.readwithboris` sources.

Suggested files (file-per-concern, no DI):

- `data/SessionStore.kt` — SharedPreferences read/write/clear
- `nostr/Nip19.kt` — encode/decode npub only
- `nostr/AmberLogin.kt` — availability check, intent builder, result parse
- UI: launcher + connect / npub / sign-out on the existing home surface

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Plain SharedPreferences | Preferences DataStore 1.2.1 | More than this login pair, or a later settings screen. |
| Tiny in-repo bech32 | Amethyst `quartz` `nip19Bech32` | Never for v1. Locked: no Amethyst KMP signer tree. |
| Tiny in-repo bech32 | Copy full Dark Wisp `Nip19.kt` | Only if Boris later parses `nevent` / `nprofile` in the reader. |
| `rememberLauncherForActivityResult` | `startActivityForResult` | Never. Deprecated; NIP-55 names the Activity Result API. |
| Scheme `<queries>` | Hardcoded Amber `<package>` query | Only if scheme discovery fails on a specific OEM. |
| Thin `AmberLogin` helper | Full `NostrSigner` + `SignerIntentBridge` | When something actually calls `sign_event` or NIP-44. |
| Omit `permissions` extra | Dark Wisp's sign_event/NIP-44 permissions JSON | When Boris starts signing. Not this slice. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Amethyst Quartz / KMP signer tree | Locked. Huge surface for login-only. | Dark Wisp-sized helper. |
| `nsec` import, `LocalSigner`, keygen | Boris must never hold a private key. | Amber only. |
| NIP-46 / bunker libraries | Out of scope until Amber login works. | Defer. |
| ContentResolver (`content://….GET_PUBLIC_KEY` / `SIGN_EVENT`) | Silent path is for remembered `sign_event` / encrypt. v1 does not sign. | Intent `get_public_key` once. |
| `sign_event`, NIP-04, NIP-44 | Out of scope. | Login pair only. |
| DataStore 1.2.1 | One extra artifact and a singleton for two strings. | SharedPreferences. |
| EncryptedSharedPreferences / Tink | Deprecated or implies secrets. Pubkey + package are public. | Plain prefs. |
| Hilt / Koin | Project constraint. | Construct `SessionStore` from a Context at the UI/ViewModel edge. |
| Room | Project constraint. Not a database. | Prefs. |
| `fr.acinq.secp256k1` / any secp256k1 | No signing, no keygen. | Nothing. |
| Kotlinx Serialization | Not on the classpath. Login record is two strings. | Prefs keys. |
| Hardcoding `package` on first `get_public_key` | NIP-55 and Dark Wisp: omit so the system picker / default signer can answer. | Set package only on later requests (none in v1). |
| Dark Wisp permissions JSON | Pre-authorizes kinds Boris will not sign. | Omit `permissions`. |

## Stack Patterns by Variant

**If Amber is installed:**
- Query succeeds → launch `get_public_key` → persist hex + package → show npub.

**If Amber is missing:**
- Query empty → explain and link F-Droid / GitHub. Do not crash. Do not pretend login succeeded.

**If the user rejects in Amber:**
- `rejected=true` → stay logged out. No partial persist.

**If a later phase adds `sign_event`:**
- Then add `SignerIntentBridge`-style UI ownership, set `package` on follow-up intents, and consider ContentResolver for remembered permissions. Do not build that now.

**If a later phase adds bunker / NIP-46:**
- Separate stack. Do not fold bunker SDKs into this login helper.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| activity-compose 1.10.1 | Compose BOM 2025.06.01, Lifecycle 2.9.2 | Already compiling. Activity Result API is here. |
| minSdk 26 | Amber, Dark Wisp | Same floor. `<queries>` is ignored below API 30 and required at 30+. |
| DataStore 1.2.1 | AGP 8.7.3 / Gradle 8.13 | Compatible if added later. Not added now. |
| Amber `com.greenart7c3.nostrsigner` | NIP-55 intents | Confirm `result` may be `npub1` (Dark Wisp `AuthScreen`, 2026). Always normalize to hex. |

## Confidence

| Claim | Level | Why |
|-------|-------|-----|
| NIP-55 login shape (`get_public_key`, extras, `<queries>`, no repeat while logged in) | HIGH | Read [NIP-55](https://raw.githubusercontent.com/nostr-protocol/nips/master/55.md) 2026-08-14. |
| Dark Wisp is the model to copy | HIGH | Read `NostrSigner.kt` (`RemoteSignerBridge`), `SignerIntentBridge.kt`, `AuthScreen.kt`, manifest `<queries>`, `KeyRepository.savePubkeyOnly`. |
| Amber `result` is often `npub1`, not hex | HIGH | Dark Wisp `AuthScreen` comment and decode path. Cross-checked against NIP-55's "hex" rule: handle both. |
| SharedPreferences over DataStore for v1 | HIGH | Two public strings; no DI; EncryptedSharedPreferences deprecated; DataStore 1.2.1 exists but is optional. |
| Tiny bech32 helper over a library | HIGH | v1 needs encode + decode only; Quartz locked out; Dark Wisp already inlines bech32. |
| Do not add Maven deps | HIGH | Everything v1 needs is platform + existing activity-compose. |
| Amber package / install URLs | HIGH | Amber README + F-Droid id `com.greenart7c3.nostrsigner`. |
| Skip ContentResolver / permissions JSON | HIGH | v1 does not sign; NIP-55 ContentResolver is for remembered methods. |

## Sources

- [NIP-55](https://github.com/nostr-protocol/nips/blob/master/55.md) (raw 2026-08-14) — intents, extras, `<queries>`, hex pubkeys, do not re-call `get_public_key`
- [NIP-19](https://github.com/nostr-protocol/nips/blob/master/19.md) — npub test vectors
- [Dark Wisp `NostrSigner.kt`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/nostr/NostrSigner.kt) — `RemoteSignerBridge.isSignerAvailable` / `buildGetPublicKeyIntent`
- [Dark Wisp `SignerIntentBridge.kt`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/nostr/SignerIntentBridge.kt) — UI owns the launcher (needed later, not v1)
- [Dark Wisp `AuthScreen.kt`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/ui/screen/AuthScreen.kt) — `rememberLauncherForActivityResult`, Amber returns `npub1`
- [Dark Wisp `Nip19.kt`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/nostr/Nip19.kt) — inlined bech32 (copy encode/decode only)
- [Dark Wisp `AndroidManifest.xml`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/AndroidManifest.xml) — `<queries>` for `nostrsigner`
- [Dark Wisp `KeyRepository.kt`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/repo/KeyRepository.kt) — `savePubkeyOnly(pubkeyHex, signerPackage)`
- [Amber README](https://github.com/greenart7c3/Amber/blob/master/README.md) — package `com.greenart7c3.nostrsigner`, F-Droid, NIP-55
- [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore) — 1.2.1 stable (2026-07-29)
- [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity) — 1.13.0 latest (2026-03-11); Boris stays on 1.10.1
- [EncryptedSharedPreferences](https://developer.android.com/reference/kotlin/androidx/security/crypto/EncryptedSharedPreferences) — deprecated
- [`<queries>`](https://developer.android.com/guide/topics/manifest/queries-element) — package visibility API 30+
- Local `.planning/codebase/STACK.md` — existing reader stack (2026-08-14)

---
*Stack research for: Boris Amber / NIP-55 login*
*Researched: 2026-08-14*
