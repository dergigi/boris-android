# Architecture Research

**Domain:** Android reader + NIP-55 Amber login (identity only)
**Researched:** 2026-08-14
**Confidence:** HIGH

Copy Dark Wisp's small login model. Do not copy its full signer stack. Reading stays ungated.

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Platform / Activity                          │
│   MainActivity (singleTop)  ·  AndroidManifest <queries>         │
└────────────────────────────┬────────────────────────────────────┘
                             │ incomingUrl
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Navigation + Signer Host                       │
│                 org.dergigi.boris.ui                             │
│   BorisApp + Routes     SignerHost (owns ActivityResultLauncher) │
│   AuthViewModel         BorisTheme                               │
├────────────────────────────┬────────────────────────────────────┤
│  Home + AuthBar             │  Reader (unchanged)                │
│  ui/home + ui/auth          │  ui/reader                         │
└────────────┬───────────────┴─────────────────┬──────────────────┘
             │                                 │
             ▼                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  nostr/                          data/                           │
│  RemoteSignerBridge              Session + SessionStore          │
│  SignerIntentBridge              ReaderRepository, UrlExtractor  │
│  Nip19 (npub only)               ImageStore                      │
└────────────┬────────────────────────────────────────────────────┘
             │ nostrsigner: VIEW intent (login once)
             ▼
┌─────────────────────────────────────────────────────────────────┐
│  Amber (com.greenart7c3.nostrsigner)  ·  r.jina.ai  ·  MediaStore│
└─────────────────────────────────────────────────────────────────┘
```

Login is a stored identity on top of the existing reader. It is not a new Activity, not a start-destination auth gate, and not a `NostrSigner` tree.

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `RemoteSignerBridge` | Detect a NIP-55 signer; build the one-shot `get_public_key` intent | Kotlin `object` (Dark Wisp) |
| `SignerIntentBridge` | Domain posts an intent; Compose delivers the activity result | Kotlin `object` + `StateFlow` |
| `SignerHost` | Owns `ActivityResultLauncher`; launches and calls `deliverResult` | Composable in `BorisApp` |
| `Session` / `SessionStore` | Persist pubkey hex + signer package; clear on sign-out | Data class + SharedPreferences |
| `Nip19` | `npub` encode/decode only (Amber returns `npub1…`) | Kotlin `object`, JVM-testable |
| `AuthViewModel` | Logged-in state, connect, sign-out, Amber-missing message | `AndroidViewModel` (needs `Context`) |
| `AuthBar` | Connect / npub / sign out / install Amber | Composable on Home |
| Reader stack | Unchanged; no login required to read | Existing `ui/reader` + `data/` |

## Recommended Project Structure

Exact new paths under `app/src/main/java/org/dergigi/boris/`:

```
app/src/main/java/org/dergigi/boris/
├── MainActivity.kt                         # unchanged (no login work)
├── data/
│   ├── Session.kt                          # NEW: pubkeyHex + signerPackage
│   ├── SessionStore.kt                     # NEW: load / save / clear
│   ├── ImageStore.kt                       # existing
│   ├── ReadableContent.kt                  # existing
│   ├── ReaderRepository.kt                 # existing
│   └── UrlExtractor.kt                     # existing
├── nostr/                                  # NEW package: NIP-55 + npub only
│   ├── RemoteSignerBridge.kt               # isSignerAvailable, buildGetPublicKeyIntent
│   ├── SignerIntentBridge.kt               # pending request; deliverResult
│   └── Nip19.kt                            # npubEncode / npubDecode + hex helpers
└── ui/
    ├── BorisApp.kt                         # EDIT: host SignerHost + AuthViewModel
    ├── auth/                               # NEW
    │   ├── AuthViewModel.kt
    │   ├── AuthBar.kt
    │   └── SignerHost.kt
    ├── home/
    │   └── HomeScreen.kt                   # EDIT: render AuthBar
    ├── reader/                             # unchanged this slice
    └── theme/                              # unchanged
```

Tests (mirror live packages):

```
app/src/test/java/org/dergigi/boris/
├── data/
│   └── SessionTest.kt                      # parse hex vs npub; reject junk
└── nostr/
    └── Nip19Test.kt                        # round-trip npub ↔ 32-byte hex
```

Edits that are not new Kotlin types:

- `app/src/main/AndroidManifest.xml` — add `<queries>` for `nostrsigner`
- `app/src/main/res/values/strings.xml` — connect, sign out, Amber missing, install Amber

Do not add files under `app/src/main/java/com/readwithboris/`.

### Structure Rationale

- **`nostr/`:** Protocol boundary. Intent builders and bech32. No Compose, no prefs. Matches Dark Wisp's `com.darkwisp.app.nostr` split without dragging in `LocalSigner` / `RemoteSigner`.
- **`data/Session*.kt`:** Persistence is data-layer work, same folder as `ImageStore`. Two strings, not a keystore.
- **`ui/auth/`:** Feature UI package, lowercase singular, same rule as `ui/home/` and `ui/reader/`.
- **`SignerHost` in `ui/auth/`:** Keeps `ActivityResultLauncher` out of `HomeScreen` and out of domain code. `BorisApp` stays composed for the whole process, so a mid-login rotation does not drop the launcher.
- **No `Routes.AUTH`:** Reading is first. Home stays the start destination.

## Architectural Patterns

### Pattern 1: Thin RemoteSignerBridge (login only)

**What:** An `object` with two functions: `isSignerAvailable(context)` and `buildGetPublicKeyIntent()`. No `package` on that first intent. No `permissions` JSON in v1.
**When to use:** The one Amber handshake at connect time.
**Trade-offs:** User must approve in Amber once. Later slices can add `permissions` when something actually signs.

**Example:**

```kotlin
object RemoteSignerBridge {
    fun isSignerAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:"))
        return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }

    fun buildGetPublicKeyIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "get_public_key")
        }
    }
}
```

Dark Wisp's `AuthScreen` also stuffs a large `permissions` array (`sign_event` kinds, `nip44_*`). Skip that. Boris v1 does not sign.

### Pattern 2: Compose-owned SignerIntentBridge

**What:** Domain posts an `Intent` and suspends (or the ViewModel waits on a result callback). Only Compose calls `launcher.launch`. Domain never calls `startActivity` / `startActivityForResult`.
**When to use:** Every NIP-55 intent, including this login. Dark Wisp uses a second launcher on `AuthScreen` for `get_public_key` and `SignerIntentBridge` only for later `sign_event`. Boris should use one bridge and one launcher from day one.
**Trade-offs:** A process-wide `object` is a small bit of global state. That is acceptable: the app already uses `object` helpers (`UrlExtractor`, `ImageStore`) and has no DI.

**Example:**

```kotlin
object SignerIntentBridge {
    private val _pending = MutableStateFlow<LoginRequest?>(null)
    val pending: StateFlow<LoginRequest?> = _pending

    fun requestGetPublicKey(intent: Intent) {
        _pending.value = LoginRequest(intent)
    }

    fun deliverResult(result: LoginResult) {
        _pending.value = null
        // AuthViewModel observes / is called from SignerHost
    }
}
```

Keep this login-shaped (`LoginResult.Success(pubkeyHex, signerPackage)`, `Rejected`, `Cancelled`). Do not copy Dark Wisp's `SignResult` / mutex / `requestSignWithRetry` until a later slice signs events.

### Pattern 3: Persist the pair; never re-ask while logged in

**What:** After a successful `get_public_key`, store `pubkeyHex` + `signerPackage`. Cold start reads the store. If both are present, the user is logged in. Do not fire `get_public_key` again.
**When to use:** Always. This is NIP-55 setup step 3 and Dark Wisp's `savePubkeyOnly`.
**Trade-offs:** If the user uninstalls Amber, Boris still shows the npub until they sign out. That is correct for v1 (identity, not live signing).

**Example:**

```kotlin
data class Session(
    val pubkeyHex: String,
    val signerPackage: String,
)

class SessionStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("boris_session", Context.MODE_PRIVATE)

    fun load(): Session? { /* pubkey + signer_package or null */ }
    fun save(session: Session) { /* both keys */ }
    fun clear() { prefs.edit().clear().apply() }
}
```

Use plain `SharedPreferences`. Dark Wisp uses `EncryptedSharedPreferences` because it stores `nsec`. Boris must never hold an `nsec`. Encryption here is theater and adds the security-crypto dependency.

Do not add DataStore for two strings. Do not add Room. Do not add Hilt/Koin.

### Pattern 4: Identity chrome on Home, not an auth gate

**What:** `AuthBar` on `HomeScreen`. Logged out: Connect. No signer: say so and point at Amber. Logged in: npub + Sign out. Reader routes stay reachable from share/VIEW with no account.
**When to use:** This slice.
**Trade-offs:** npub is only on Home unless a later slice adds chrome to the reader toolbar.

## Data Flow

### Login

```
Home AuthBar "Connect"
    ↓
AuthViewModel.connect()
    ↓
RemoteSignerBridge.isSignerAvailable(context)
    ├─ false → AuthUiState.AmberMissing (install hint, no intent)
    └─ true  → SignerIntentBridge.requestGetPublicKey(
                   RemoteSignerBridge.buildGetPublicKeyIntent()
               )
                    ↓
               SignerHost collects pending
                    ↓
               ActivityResultLauncher.launch(intent)
                    ↓
               Amber (no package extra on this first call)
                    ↓
               RESULT_OK + extras:
                 result  = npub1… or 64-char hex
                 package = signer applicationId
                 rejected = true  → AuthUiState.Error
               else resultCode → Cancelled / failed
                    ↓
               Nip19: npub1 → 64-char lowercase hex
                    ↓
               SessionStore.save(Session(pubkeyHex, signerPackage))
                    ↓
               AuthUiState.LoggedIn(npub)
```

Rules for that path:

1. Manifest `<queries>` must list `nostrsigner` or `queryIntentActivities` is empty on API 30+.
2. Do not set `intent.package` on the first `get_public_key`. Amber (or another signer) is chosen by the system.
3. Amber's `result` extra is usually `npub1…`. Dark Wisp decodes that to hex before save. Persist hex. Display npub.
4. Persist `package` from the result extra (expected: `com.greenart7c3.nostrsigner`). Needed later if anything signs; required now so we never call `get_public_key` again.
5. After `SessionStore.load()` returns a session, skip the intent entirely.

### Sign-out

```
Home AuthBar "Sign out"
    ↓
AuthViewModel.signOut()
    ↓
SessionStore.clear()          // deletes pubkey + signer_package
    ↓
AuthUiState.LoggedOut
    ↓
No Amber intent. No get_public_key. No ContentResolver.
```

Sign-out is local. Amber keeps the key. A later Connect is a new `get_public_key`.

### Cold start

```
AuthViewModel.init
    ↓
SessionStore.load()
    ├─ Session → show npub (Nip19.npubEncode). Do not launch Amber.
    └─ null    → show Connect
```

### State Management

```
SessionStore (SharedPreferences)
    ↓ load/save/clear
AuthViewModel (StateFlow<AuthUiState>)
    ↓ collect
AuthBar / HomeScreen
    ↓ connect / signOut
SignerIntentBridge ←→ SignerHost (launcher)
```

`HomeScreen` keeps its URL field as `rememberSaveable`. Do not move URL state into `AuthViewModel`.

### Key Data Flows

1. **Connect:** UI → availability check → one `get_public_key` intent → parse `result`/`package` → persist pair → show npub.
2. **Restore:** Store → ViewModel → npub. No signer IPC.
3. **Sign-out:** ViewModel → `SessionStore.clear()` → Connect chrome. No signer IPC.
4. **Read:** Unchanged Home → `UrlExtractor` → reader. Login does not sit on this path.

## Build Order

Implement in this order. Each step should compile before the next.

1. **Manifest queries** in `app/src/main/AndroidManifest.xml` (sibling of `<application>`):

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="nostrsigner" />
    </intent>
</queries>
```

   Dark Wisp omits `BROWSABLE`. NIP-55 includes it. Use the NIP-55 form.

2. **`RemoteSignerBridge.kt`** — availability + intent. No UI.

3. **`Nip19.kt`** — `npubEncode` / `npubDecode` plus hex helpers only. JVM tests first. Do not copy `nsec*`, `nevent*`, `nprofile*`.

4. **`Session.kt` + `SessionStore.kt`** — two prefs keys, e.g. `pubkey_hex` and `signer_package`. Reject blank or non-64 hex on save.

5. **`SignerIntentBridge.kt` + `LoginResult`** — pending intent + deliver. No sign mutex.

6. **`AuthViewModel.kt`** — `AndroidViewModel`, constructs `SessionStore(getApplication())`. States: `LoggedOut`, `AmberMissing`, `WaitingForSigner`, `LoggedIn(npub)`, `Error(message)`.

7. **`SignerHost.kt` wired from `BorisApp.kt`** — `rememberLauncherForActivityResult(StartActivityForResult)`. On `RESULT_OK`, read `rejected`, then `result` and `package`. Decode npub, call ViewModel `onSignerResult`. On other result codes, treat as cancel.

8. **`AuthBar.kt` on `HomeScreen`** — Connect / missing Amber / npub + Sign out. Strings in `strings.xml`.

9. **Do not** add `NostrSigner`, `LocalSigner`, `RemoteSigner`, ContentResolver `SIGN_EVENT`, or a second Activity.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| This slice (one identity) | SharedPreferences + two bridge objects. Fine. |
| Later: sign notes / NIP-44 | Add `RemoteSigner` + ContentResolver fallback; reuse stored `signerPackage`; grow `SignerIntentBridge` toward Dark Wisp's mutex. Still no `nsec`. |
| Later: bunker / NIP-46 | New transport behind the same `Session` (pubkey + how we talk to the signer). Do not invent a second login store. |
| Multi-account | Out of scope. Dark Wisp's `accounts` JSON + `EncryptedSharedPreferences` is the wrong size. |

### Scaling Priorities

1. **First bottleneck:** Package visibility / missing `<queries>` makes Amber look uninstalled. Fix the manifest before debugging UI.
2. **Second bottleneck:** Re-calling `get_public_key` on every launch. Users will hate the Amber prompt. The store is the fix.

## Anti-Patterns

### Anti-Pattern 1: Full NostrSigner / LocalSigner / RemoteSigner in v1

**What people do:** Port Dark Wisp's `NostrSigner.kt` wholesale (`signEvent`, NIP-44, ContentResolver, intent fallback).
**Why it's wrong:** Nothing in Boris signs. That file is the Amethyst-sized stack PROJECT.md ruled out.
**Do this instead:** Copy `RemoteSignerBridge` (bottom of that file) and the Compose/domain split. Leave `RemoteSigner` for a later phase.

### Anti-Pattern 2: Domain code launches Amber

**What people do:** `context.startActivity(getPublicKeyIntent)` from `SessionStore` or `AuthViewModel`.
**Why it's wrong:** Results never come back through a registered launcher. Dark Wisp's comment on `SignerIntentBridge` exists for this reason.
**Do this instead:** ViewModel asks the bridge; `SignerHost` launches.

### Anti-Pattern 3: `get_public_key` on every process start

**What people do:** Treat Amber as the session store.
**Why it's wrong:** NIP-55 says store pubkey + package and do not call `get_public_key` again while logged in. Extra prompts train users to reject Boris.
**Do this instead:** `SessionStore.load()` is the session.

### Anti-Pattern 4: Set `package` on the first login intent

**What people do:** Hardcode `com.greenart7c3.nostrsigner` on `buildGetPublicKeyIntent`.
**Why it's wrong:** NIP-55: omit package only for `get_public_key`. Hardcoding skips other signers and breaks if Amber's id changes.
**Do this instead:** No package on connect. Save whatever `package` extra comes back.

### Anti-Pattern 5: Auth route that blocks reading

**What people do:** `startDestination = AUTH`, pop Home only after login (Dark Wisp splash/auth).
**Why it's wrong:** Boris is a reader. Share/VIEW must still open articles with no account.
**Do this instead:** Keep `Routes.HOME`. Put identity on Home.

### Anti-Pattern 6: Encrypted prefs, DataStore, or DI for this record

**What people do:** `EncryptedSharedPreferences` "because keys", or Hilt for one store.
**Why it's wrong:** There is no private key. The app has no `Application` class and no DI. PROJECT.md forbids introducing Hilt/Koin for one feature.
**Do this instead:** `SessionStore(context)` constructed by `AuthViewModel`, same style as `ReaderViewModel` constructing `ReaderRepository()`.

### Anti-Pattern 7: nsec field or nsec decode

**What people do:** Port `AuthScreen`'s nsec box and `Nip19.nsecDecode`.
**Why it's wrong:** Out of scope. Amber holds the key.
**Do this instead:** Connect via signer, or stay logged out.

### Anti-Pattern 8: Second Activity for login

**What people do:** `LoginActivity` + manifest entry.
**Why it's wrong:** Share/VIEW and `singleTop` assume one Activity.
**Do this instead:** `AuthBar` + `SignerHost` inside the existing `NavHost` shell.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Amber (`com.greenart7c3.nostrsigner`) | NIP-55 intent, `type=get_public_key` | Detect via `nostrsigner:` query, not by hardcoding the package on the first intent |
| Other NIP-55 signers | Same intent | Store whatever `package` extra is returned |
| Play / GitHub Amber listing | Outbound link when missing | Do not deep-link `nostrsigner:` as the install path |
| Jina / MediaStore | Unchanged | Login must not touch `ReaderRepository` |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| `ui/auth` ↔ `nostr` | Direct calls to bridge objects | UI launches; nostr builds intents |
| `ui/auth` ↔ `data/SessionStore` | ViewModel owns the store | UI never reads prefs |
| `nostr` ↔ `data` | None | Bridges do not persist; store does not build intents |
| `ui/home` ↔ `ui/auth` | `AuthBar` composed from Home; state from `AuthViewModel` | Home does not own the launcher |
| `ui/reader` ↔ auth | None this slice | Do not thread session into `ReaderViewModel` |
| `MainActivity` ↔ auth | None | Keep share/VIEW URL parsing as-is |

### Manifest and Activity

- Add `<queries>` only. Do not add an `<activity>` or change `launchMode`.
- `MainActivity` stays the only Activity.
- `allowBackup` can stay `true`. The session is a public key and a package name, not an `nsec`. Dark Wisp sets `allowBackup=false` because it stores private keys.

### Amber-missing UX

If `isSignerAvailable` is false, do not launch. Show that Amber is required and point at installing it (`com.greenart7c3.nostrsigner`). This is an Active requirement.

## What to Copy from Dark Wisp (and what to skip)

| Dark Wisp file | Copy | Skip |
|----------------|------|------|
| `NostrSigner.kt` → `RemoteSignerBridge` | `isSignerAvailable`, `buildGetPublicKeyIntent` | `NostrSigner`, `LocalSigner`, `RemoteSigner`, ContentResolver, NIP-44 |
| `SignerIntentBridge.kt` | Compose owns launcher; domain does not launch | `requestSign` mutex, `requestSignWithRetry`, `SignResult.event` |
| `AuthScreen.kt` | `rememberLauncherForActivityResult`, npub-or-hex parse, `package` extra | nsec field, sign-up, permissions JSON, Tor button |
| `AuthViewModel.loginWithSigner` / `logOut` | save pubkey+package; clear on sign-out | multi-account, `SigningMode`, `EncryptedSharedPreferences` |
| `KeyRepository.savePubkeyOnly` | two-field persist | `privkey_*`, account registry, relay prefs |
| `Navigation.kt` launcher | one host launcher | feed/wallet signer injection |
| `AndroidManifest.xml` | `<queries>` for `nostrsigner` | camera, FGS, Tor, `WispApp` |
| `Nip19.kt` | `npubEncode` / `npubDecode` + bech32 internals | nsec, note, nevent, nprofile, naddr, QR helpers |

Primary sources (read 2026-08-14):

- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/nostr/NostrSigner.kt
- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/nostr/SignerIntentBridge.kt
- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/ui/screen/AuthScreen.kt
- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/Navigation.kt
- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/AndroidManifest.xml
- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/viewmodel/AuthViewModel.kt
- https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/repo/KeyRepository.kt
- https://github.com/nostr-protocol/nips/blob/master/55.md

## Sources

- NIP-55 (Android Signer Application), nostr-protocol/nips — official setup, extras, "do not call `get_public_key` again" — HIGH
- Dark Wisp `RemoteSignerBridge` / `SignerIntentBridge` / `AuthScreen` / `KeyRepository.savePubkeyOnly` (main, fetched 2026-08-14) — HIGH
- Boris `.planning/codebase/ARCHITECTURE.md` and `STRUCTURE.md` (2026-08-14) — HIGH
- Amber application id `com.greenart7c3.nostrsigner` from PROJECT.md / NIP-55 examples — HIGH for the well-known package; still do not hardcode it on the first intent

---
*Architecture research for: Boris NIP-55 login*
*Researched: 2026-08-14*
