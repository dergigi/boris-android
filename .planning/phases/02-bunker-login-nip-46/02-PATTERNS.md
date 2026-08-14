# Phase 2: Bunker login (NIP-46) - Pattern Map

**Mapped:** 2026-08-14
**Files analyzed:** 22
**Analogs found:** 19 / 22

Phase 1 Amber chrome, session prefs, and `Nip19` stay the shape to copy. Bunker is a new transport under `nostr/`. Do not fold it into `RemoteSignerBridge`. Do not add a NavHost route.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `nostr/BunkerUri.kt` | utility | transform | `nostr/Nip19.kt` + `data/UrlExtractor.kt` | exact |
| `nostr/ClientKeypair.kt` | utility | transform | `nostr/Nip19.kt` | role-match |
| `nostr/Nip44.kt` | utility | transform | `nostr/Nip19.kt` | role-match |
| `nostr/Nip01Event.kt` | utility | transform | `nostr/Nip19.kt` | role-match |
| `nostr/RelaySocket.kt` | service | streaming | `data/ReaderRepository.kt` | partial |
| `nostr/BunkerClient.kt` | service | request-response | `data/ReaderRepository.kt` + `ui/auth/AuthViewModel.kt` | partial |
| `data/SecretBox.kt` | utility | file-I/O | `data/SessionStore.kt` | role-match |
| `data/Session.kt` | model | transform | itself + `nostr/SignerResult.kt` | exact |
| `data/SessionStore.kt` | store | CRUD | itself | exact |
| `ui/auth/AuthUiState.kt` | model | — | itself + `ui/reader/ReaderViewModel.kt` | exact |
| `ui/auth/AuthViewModel.kt` | hook | request-response | itself + `ui/reader/ReaderViewModel.kt` | exact |
| `ui/auth/AuthBar.kt` | component | request-response | itself + `ui/home/HomeScreen.kt` | exact |
| `ui/home/HomeScreen.kt` | component | event-driven | itself | exact |
| `ui/BorisApp.kt` | route | event-driven | itself | exact |
| `MainActivity.kt` | controller | event-driven | itself | exact |
| `AndroidManifest.xml` | config | — | itself | exact |
| `res/values/strings.xml` | config | — | itself | exact |
| `gradle/libs.versions.toml` | config | — | itself | exact |
| `app/build.gradle.kts` | config | — | itself | exact |
| `nostr/BunkerUriTest.kt` | test | — | `data/UrlExtractorTest.kt` + `data/SessionStoreTest.kt` | exact |
| `nostr/Nip44Test.kt` | test | — | `nostr/Nip19Test.kt` | exact |
| `data/SessionStoreTest.kt` | test | — | itself | exact |

Unchanged (do not edit): `nostr/RemoteSignerBridge.kt`, `nostr/SignerResult.kt`, `nostr/Nip19.kt`, `nostr/Nip19Test.kt`, `res/xml/data_extraction_rules.xml`, `res/xml/full_backup_content.xml`.

## Pattern Assignments

### `nostr/BunkerUri.kt` (utility, transform)

**Analog:** `data/UrlExtractor.kt` (parse + fail-closed `null`) and `data/Session.kt` (`fromStored` hex guard).

**Imports / object shape** (`UrlExtractor.kt` lines 1-16):
```kotlin
package org.dergigi.boris.data

object UrlExtractor {
    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()
        // ...
    }
}
```

**Fail-closed hex validation** (`Session.kt` lines 8-13):
```kotlin
fun fromStored(hex: String?, pkg: String?): Session? {
    val h = hex?.trim()?.lowercase().orEmpty()
    if (h.length != 64 || h.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
    if (p.isEmpty()) return null
    return Session(h, p)
}
```

**Copy:** `object` (or `data class` + `fun parse` on companion). Return `null` on bad input. Trim first. Reject `nsec1` anywhere, require `bunker://`, 64-char hex host, at least one `relay=wss://`. Decode query values with `URLDecoder` (Amethyst split does not). No Android types so JVM tests can run.

**Do not copy:** `UrlExtractor.normalize` adding `https://`. Do not accept `npub1` as the bunker host (D-12).

---

### `nostr/ClientKeypair.kt` / `nostr/Nip44.kt` / `nostr/Nip01Event.kt` (utility, transform)

**Analog:** `nostr/Nip19.kt`

**Object + expression-body helpers** (lines 3-17):
```kotlin
package org.dergigi.boris.nostr

object Nip19 {
    fun npubEncode(pubkeyHex: String): String =
        bech32Encode("npub", pubkeyHex.hexToByteArray())
}
```

**Reuse existing hex helpers in the same package** (lines 135-142):
```kotlin
internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Odd hex length" }
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
```

**Fail-soft parse** (`normalizePubkey`, lines 19-32):
```kotlin
fun normalizePubkey(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    return try {
        when {
            trimmed.startsWith("npub1", ignoreCase = true) -> npubDecode(trimmed)
            else -> {
                val hex = trimmed.lowercase()
                if (hex.length == 64 && hex.all { it in '0'..'9' || it in 'a'..'f' }) hex else null
            }
        }
    } catch (_: Exception) {
        null
    }
}
```

**Copy:** Same package `org.dergigi.boris.nostr`. `object` for stateless crypto. Reuse `toHex` / `hexToByteArray`. After `get_public_key`, normalize with `Nip19.normalizePubkey` (a bunker may return `npub1`). Official-vector tests like `Nip19Test`.

**Do not copy:** Bech32 internals. Do not add a second hex helper. Do not use `Secp256k1.ecdh()` (hashed). NIP-44 conversation key is `pubKeyTweakMul` then bytes 1..32, then HKDF-Extract salt `nip44-v2`.

**NIP-01 JSON:** No kotlinx.serialization on the classpath. Use platform `org.json.JSONObject` / `JSONArray` the way RESEARCH specifies. Event id array must have no extra whitespace.

---

### `nostr/RelaySocket.kt` (service, streaming)

**Analog:** `data/ReaderRepository.kt` (OkHttp client construction only). No WebSocket analog exists.

**Injected client + companion default** (lines 3-17, 50-54):
```kotlin
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReaderRepository(
    private val client: OkHttpClient = defaultClient,
) {
    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
```

**Copy:** Constructor-injected `OkHttpClient` with a companion default. `Request.Builder().url(relayWss).build()`. Throw `IOException` with a short message at the IO boundary. Timeouts: 15s to `onOpen` (Amethyst relay wait), not the 20/45 HTTP values.

**Do not copy:** `newCall().execute()` HTTP. Use `client.newWebSocket(...)`. One socket per URI `wss://`. Close after login. No relay pool.

---

### `nostr/BunkerClient.kt` (service, request-response)

**Analog:** `ReaderRepository` (class + injected client) plus `AuthViewModel` (connect then persist) plus `ReaderViewModel.load()` (async IO + catch).

**Amber connect persist** (`AuthViewModel.kt` lines 35-48):
```kotlin
fun onSignerResult(resultCode: Int, data: Intent?) {
    val app = getApplication<Application>()
    when (val result = SignerResults.parse(resultCode, data)) {
        is SignerResult.Success -> {
            SessionStore.save(app, Session(result.pubkeyHex, result.signerPackage))
            _message.value = null
            _state.value = AuthUiState.LoggedIn(Nip19.npubEncode(result.pubkeyHex))
        }
        SignerResult.Rejected -> {
            _message.value = app.getString(R.string.auth_rejected)
        }
        SignerResult.Cancelled -> {
            _message.value = app.getString(R.string.auth_cancelled)
        }
    }
}
```

**Async IO + catch at ViewModel** (`ReaderViewModel.kt` lines 52-68):
```kotlin
fun load() {
    viewModelScope.launch {
        _state.value = ReaderUiState.Loading
        try {
            val content = withContext(Dispatchers.IO) { repository.fetch(url) }
            _state.value = ReaderUiState.Ready(content)
        } catch (e: Exception) {
            _state.value = ReaderUiState.Error(
                e.message ?: "Failed to load this article.",
                url,
            )
        }
    }
}
```

**Copy for the client class:** File-per-concern in `nostr/`. Inject `OkHttpClient`. Sequence: parse URI (caller) → generate key → REQ kind 24133 `#p=client` → wait `onOpen` 15s → NIP-44 `connect` (metadata `name=Boris`, empty perms) → maybe `auth_url` → `get_public_key` 65s → return user hex → close sockets. Distinguish bad URI / relay timeout / reject / missing pubkey as separate exceptions or a small sealed result (same idea as `SignerResult`).

**Copy for the ViewModel call site:** `viewModelScope.launch` + `withContext(Dispatchers.IO)`. Map failures to `_message` strings, not a crash. Persist with `SessionStore.save` of `Session.Bunker`, which replaces Amber.

**Do not copy:** Amber `rememberLauncherForActivityResult` for bunker. Do not put WebSockets on `RemoteSignerBridge`. Skip NIP-46 `logout` RPC. Do not reconnect on resume.

**`auth_url`:** Treat `result == "auth_url"` as continue, not reject. Open with `Intent.ACTION_VIEW` once per distinct URL (same shape as `AuthBar` `InstallLink`, lines 86-96). Keep the same request id open.

---

### `data/SecretBox.kt` (utility, file-I/O)

**Analog:** `data/SessionStore.kt` (`object` + `Context`). No Keystore analog in the repo.

**Object + Context + wipe** (`SessionStore.kt` lines 5-31):
```kotlin
object SessionStore {
    const val PREFS_NAME = "boris_session"

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
```

**Copy:** `object SecretBox` with `Context` methods: wrap / unwrap / wipe. Alias `boris_bunker_wrap`. Persist iv+ciphertext strings in the existing `boris_session` prefs via `SessionStore`, not a second file. `SessionStore.clear()` must also `SecretBox.wipe()`.

**Do not copy:** EncryptedSharedPreferences (deprecated). Do not unit-test Keystore on the JVM. Test `Session` parsing of already-wrapped strings, the way `SessionStoreTest` tests `fromStored` without prefs.

---

### `data/Session.kt` (model, transform)

**Analog:** itself, plus `SignerResult` sealed types.

**Today** (lines 3-16):
```kotlin
data class Session(
    val pubkeyHex: String,
    val signerPackage: String,
) {
    companion object {
        fun fromStored(hex: String?, pkg: String?): Session? {
            // requires non-empty package
        }
    }
}
```

**Sealed result analog** (`SignerResult.kt` lines 6-10):
```kotlin
sealed class SignerResult {
    data class Success(val pubkeyHex: String, val signerPackage: String) : SignerResult()
    data object Rejected : SignerResult()
    data object Cancelled : SignerResult()
}
```

**Copy:** Become `sealed interface Session` with `Amber` and `Bunker` (RESEARCH Pattern 3). Keep `pubkeyHex` on the interface so `AuthViewModel.readState()` can still `Nip19.npubEncode(session.pubkeyHex)`. Amber `fromStored` must still work when `kind` is missing and `signer_package` is present (existing installs). Bunker `fromStored` requires user hex, remote-signer hex, ≥1 relay, and wrapped client privkey. Blank `signer_package` is valid for bunker.

**Do not copy:** Requiring a package on every session. That is the Phase 1 Amber-only rule and will reject bunker.

---

### `data/SessionStore.kt` (store, CRUD)

**Analog:** itself.

**Keys and replace-in-one-edit** (lines 6-24):
```kotlin
const val PREFS_NAME = "boris_session"
const val KEY_PUBKEY_HEX = "pubkey_hex"
const val KEY_SIGNER_PACKAGE = "signer_package"

fun save(context: Context, session: Session) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PUBKEY_HEX, session.pubkeyHex)
        .putString(KEY_SIGNER_PACKAGE, session.signerPackage)
        .apply()
}
```

**Copy:** Same prefs file. Same `load` / `save` / `clear` names. `save(Amber)` writes Amber keys and removes bunker keys + Keystore entry. `save(Bunker)` writes bunker keys and removes `signer_package`. `clear()` wipes prefs and Keystore. Missing `kind` + present package ⇒ Amber.

**New keys (planner names):** `kind`, `remote_signer_pubkey`, `relays`, `client_privkey`, `bunker_secret`. Never log the last two.

**Do not copy:** A second prefs name. Backup excludes already cover `boris_session.xml`.

---

### `ui/auth/AuthUiState.kt` (model)

**Analog:** itself + `ReaderUiState` loading object.

**Today** (lines 3-6):
```kotlin
sealed interface AuthUiState {
    data object LoggedOut : AuthUiState
    data object MissingSigner : AuthUiState
    data class LoggedIn(val npub: String) : AuthUiState
}
```

**Loading analog** (`ReaderViewModel.kt` lines 79-82):
```kotlin
sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Ready(val content: ReadableContent) : ReaderUiState
    data class Error(val message: String, val url: String) : ReaderUiState
}
```

**Copy:** Add `data object Connecting : AuthUiState`. Keep errors on `_message`, not a new Error branch (Amber already uses `_message` for reject/cancel). LoggedIn stays npub-only; do not show bunker vs Amber in the chrome.

---

### `ui/auth/AuthViewModel.kt` (hook, request-response)

**Analog:** itself for Amber + session restore; `ReaderViewModel` for bunker IO.

**State + restore** (lines 17-28, 52-69):
```kotlin
class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun refresh() {
        _state.value = readState()
    }

    fun signOut() {
        SessionStore.clear(getApplication())
        _message.value = null
        _state.value = readState()
    }

    private fun readState(): AuthUiState {
        val session = SessionStore.load(app)
        if (session != null) {
            return AuthUiState.LoggedIn(Nip19.npubEncode(session.pubkeyHex))
        }
        return if (RemoteSignerBridge.isSignerAvailable(app)) {
            AuthUiState.LoggedOut
        } else {
            AuthUiState.MissingSigner
        }
    }
}
```

**Copy:** Keep `AndroidViewModel`, `_state` / `_message`, `refresh()`, `connectIntent()`, `onSignerResult()`, `signOut()`, `readState()`. Add `connectBunker(uri: String)` that sets `Connecting`, runs `BunkerClient` on `Dispatchers.IO`, then `SessionStore.save(Session.Bunker(...))` and `LoggedIn`. Distinct strings for bad URI / timeout / reject / missing pubkey (same `_message` channel as `auth_rejected` / `auth_cancelled`). `signOut` stays a local wipe; it already calls `SessionStore.clear`.

**Do not copy:** Launching Amber from the ViewModel (launcher stays in `HomeScreen`). Do not reconnect bunker on `refresh()` / `ON_RESUME`. Restore is prefs only.

**Amber must not regress:** `connectIntent()` still returns null unless `LoggedOut`. `onSignerResult` Success still `SessionStore.save` of Amber (now `Session.Amber`), which clears bunker keys.

---

### `ui/auth/AuthBar.kt` (component, request-response)

**Analog:** itself + Home URL field.

**LoggedOut / MissingSigner / LoggedIn + message** (lines 40-81):
```kotlin
when (state) {
    AuthUiState.LoggedOut -> {
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.auth_connect))
        }
    }
    AuthUiState.MissingSigner -> {
        Text(/* missing Amber */)
        InstallLink(...)
        InstallLink(...)
        InstallLink(...)
    }
    is AuthUiState.LoggedIn -> {
        SelectionContainer { Text(state.npub, /* monospace */) }
        TextButton(onClick = onSignOut) { Text(stringResource(R.string.auth_sign_out)) }
    }
}
if (!message.isNullOrBlank()) {
    Text(text = message, color = MaterialTheme.colorScheme.error)
}
```

**Field analog** (`HomeScreen.kt` lines 100-111):
```kotlin
OutlinedTextField(
    value = url,
    onValueChange = { url = it },
    modifier = Modifier.fillMaxWidth(),
    placeholder = { Text(DEFAULT_ARTICLE_URL) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Uri,
        imeAction = ImeAction.Go,
    ),
    keyboardActions = KeyboardActions(onGo = { submit() }),
)
```

**Copy:** Keep Amber Connect and the three install links. Add bunker `OutlinedTextField` + Connect bunker button on **both** `LoggedOut` and `MissingSigner`. Hide the bunker field when `LoggedIn`. Disable Connect bunker while `Connecting`. `Modifier` last among layout params. Strings via `stringResource`. `InstallLink` `ACTION_VIEW` is the pattern for `auth_url` (call from ViewModel/Activity, not a new composable unless cheap).

**Do not copy:** Replacing MissingSigner with only the bunker field. Do not add a new route.

---

### `ui/home/HomeScreen.kt` (component, event-driven)

**Analog:** itself.

**Auth wiring + Amber launcher** (lines 43-98):
```kotlin
fun HomeScreen(
    onRead: (String) -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    var url by rememberSaveable { mutableStateOf("") }
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val authMessage by viewModel.message.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignerResult(result.resultCode, result.data)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    AuthBar(
        state = authState,
        message = authMessage,
        onConnect = {
            viewModel.connectIntent()?.let(launcher::launch)
        },
        onSignOut = viewModel::signOut,
    )
}
```

**Copy:** Keep `viewModel()`, `collectAsStateWithLifecycle`, Amber launcher, `ON_RESUME` → `refresh()`. Add `rememberSaveable` bunker field (or lift into ViewModel). Incoming VIEW token fills that field; do not auto-connect (RESEARCH A1). Pass `onConnectBunker` into `AuthBar`. Article `OutlinedTextField` stays below AuthBar.

**Do not copy:** A second `rememberLauncherForActivityResult` for bunker. Bunker is not an Activity result.

---

### `ui/BorisApp.kt` (route, event-driven)

**Analog:** itself.

**Incoming URL always goes to reader** (lines 30-38):
```kotlin
fun BorisApp(incomingUrl: String? = null) {
    val navController = rememberNavController()

    LaunchedEffect(incomingUrl) {
        if (!incomingUrl.isNullOrBlank()) {
            navController.navigate(Routes.reader(incomingUrl)) {
                launchSingleTop = true
            }
        }
    }
}
```

**Copy:** Add `incomingBunker: String? = null`. Only navigate `Routes.reader` for article URLs. Pass bunker into `HomeScreen` so the field fills. Do not add a route. `startDestination` stays `Routes.HOME`.

---

### `MainActivity.kt` (controller, event-driven)

**Analog:** itself.

**Intent split today** (lines 16-40):
```kotlin
private var incomingUrl by mutableStateOf<String?>(null)

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    incomingUrl = urlFrom(intent)
}

private fun urlFrom(intent: Intent): String? {
    return when (intent.action) {
        Intent.ACTION_SEND -> UrlExtractor.extract(intent.getStringExtra(Intent.EXTRA_TEXT))
        Intent.ACTION_VIEW -> intent.dataString
        else -> null
    }
}
```

**Copy:** Same `mutableStateOf` + `onCreate` / `onNewIntent`. Split `bunker://` (trim, starts with `bunker://`) onto `incomingBunker`. `ACTION_SEND` must check bunker **before** `UrlExtractor.extract` (extractor only finds `http(s)`). `ACTION_VIEW` `dataString` that is `bunker://` must not become `incomingUrl`.

---

### `AndroidManifest.xml` (config)

**Analog:** itself, VIEW filter at lines 42-48.

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
</intent-filter>
```

**Copy:** Sibling filter with `android:scheme="bunker"` only. Keep SEND `text/plain`, `nostrsigner` queries, `INTERNET`, `usesCleartextTraffic="false"`, backup rule refs.

**Do not copy:** A `nostrconnect` scheme. Do not add a second Activity.

---

### `res/values/strings.xml` (config)

**Analog:** itself (lines 4-11). Add `auth_connect_bunker`, bunker field hint, `auth_bunker_bad_uri`, `auth_bunker_timeout`, `auth_bunker_rejected`, `auth_bunker_missing_pubkey`, connecting label. Keep Amber strings unchanged.

---

### `gradle/libs.versions.toml` + `app/build.gradle.kts` (config)

**Analog:** themselves.

Catalog pattern (`libs.versions.toml` lines 9, 28):
```toml
okhttp = "4.12.0"
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
```

Module wire (`app/build.gradle.kts` lines 96, 103):
```kotlin
implementation(libs.okhttp)
testImplementation(libs.junit)
```

**Copy:** Version first, then `[libraries]` alias, then `implementation(libs.…)` / `testImplementation(libs.secp256k1.jni.jvm)`. Add ACINQ `secp256k1-kmp` 0.24.0 + `jni-android` + `jni-jvm`, BouncyCastle `bcprov-jdk18on` 1.85.2. Do not add rust-nostr, Quartz, EncryptedSharedPreferences, kotlinx.serialization, Hilt, Room, DataStore.

---

### Tests

**`BunkerUriTest.kt` analog:** `UrlExtractorTest.kt` + `SessionStoreTest.kt`

JUnit 4, `org.junit.Assert.assertEquals` / `assertNull`, camelCase test names, package mirrors production (`org.dergigi.boris.nostr`). Cases: valid `bunker://` + relays + secret; reject `nsec1`; reject missing/non-`wss` relay; reject short/non-hex host; decode `wss%3A%2F%2F`.

**`Nip44Test.kt` analog:** `Nip19Test.kt` official vectors (lines 6-13):
```kotlin
class Nip19Test {
    @Test
    fun roundTripsOfficialNpubVector() {
        val hex = "7e7e9c42a91bfef19fa929e5fda1b72e0ebc1a4c1141673e2794234d86addf4e"
        val npub = "npub10elfcs4fr0l0r8af98jlmgdh9c8tcxjvz9qkw038js35mp4dma8qzvjptg"
        assertEquals(npub, Nip19.npubEncode(hex))
        assertEquals(hex, Nip19.npubDecode(npub))
    }
}
```

Pin NIP-44 conversation-key vector `c41c7753…` **before** wiring sockets. Add one official encrypt/decrypt vector. Needs `testImplementation(libs.secp256k1.jni.jvm)`.

**`SessionStoreTest.kt`:** Keep Amber `fromStored` cases. Add bunker accept; Amber still loads when `kind` is absent; bunker rejects blank client ciphertext; do not require `signer_package` for bunker. Still no Robolectric: test companion parsers, not live prefs/Keystore.

## Shared Patterns

### Package and file layout
**Source:** `.planning/codebase/CONVENTIONS.md`, Phase 1 `01-PATTERNS.md`
**Apply to:** All new Kotlin

- Package `org.dergigi.boris` only. Never `com.readwithboris`.
- One primary type per file. Filename matches the type.
- Tests `{Type}Test.kt` under `app/src/test/java/org/dergigi/boris/<same/path>/`.
- No Hilt/Koin. `viewModel()` like `HomeScreen` line 45.
- Official Kotlin style, 4-space indent, trailing commas, no wildcard imports, no KDoc/`TODO`, no `Log`/`println`.

### Authentication (two transports, one session)
**Source:** `AuthViewModel.kt`, `RemoteSignerBridge.kt`, `SessionStore.kt`
**Apply to:** Auth UI + session files

Amber stays intent-based (`RemoteSignerBridge.buildGetPublicKeyIntent()`, launcher in `HomeScreen`). Bunker is a new `nostr/` client. One identity: `save` of one kind clears the other. Restore is `SessionStore.load()` + `Nip19.npubEncode`. Sign out is `SessionStore.clear()` + Keystore wipe. Reading stays ungated on Home.

### Error handling
**Source:** `AuthViewModel.onSignerResult` (lines 43-48), `ReaderViewModel.load` (lines 62-67), `UrlExtractor` null returns
**Apply to:** Parse, client, ViewModel

- Parse / validate: return `null` (`BunkerUri.parse`, `Nip19.normalizePubkey`).
- IO the caller asked for: throw `IOException` with a short message (`ReaderRepository` line 21).
- Catch at the ViewModel. Map to `_message` strings. Distinct: bad URI, relay timeout, rejected, missing pubkey.
- `auth_url` is not an error.
- `runCatching` only for best-effort side effects. Do not crash the UI.

### Validation
**Source:** `Session.fromStored` (lines 8-13), `UrlExtractor.extract` (lines 10-16)
**Apply to:** `BunkerUri`, session restore, `get_public_key` result

Trim, lowercase hex, length 64, charset `0-9a-f`. Reject `nsec1` in the bunker field. Relays must start with `wss://`. User pubkey after RPC goes through `Nip19.normalizePubkey`.

### OkHttp
**Source:** `ReaderRepository.kt` companion `defaultClient` (lines 50-54)
**Apply to:** `RelaySocket`, `BunkerClient`

Reuse OkHttp 4.12.0 already on the classpath. Inject the client. Do not add a second HTTP stack. WebSocket is new; HTTP `execute()` is not the pattern for relays.

### Hex and npub
**Source:** `Nip19.kt` lines 19-32 and 135-142
**Apply to:** `ClientKeypair`, `Nip44`, `Nip01Event`, bunker persist

Reuse `toHex` / `hexToByteArray` / `normalizePubkey` / `npubEncode`. Do not add a bech32 library.

### Backup / secrets
**Source:** `data_extraction_rules.xml` line 4, `full_backup_content.xml` line 3
**Apply to:** `SessionStore`, `SecretBox`

Keep excludes for `boris_session.xml`. Do not add a second prefs file. Never log client privkey or bunker secret.

### Compose chrome
**Source:** `AuthBar.kt`, `HomeScreen.kt`
**Apply to:** bunker field

`MaterialTheme.colorScheme` / `typography`. `fillMaxWidth` buttons. `rememberSaveable` for the bunker field. `stringResource` for labels. No new `Routes` entry. `AuthBar` stays on Home above the article field.

## No Analog Found

| File / capability | Role | Data Flow | Reason |
|-------------------|------|-----------|--------|
| `SecretBox` Keystore AES-GCM wrap | utility | file-I/O | No Keystore usage in repo. Copy `SessionStore` object+Context+wipe; crypto from RESEARCH Keystore example |
| `RelaySocket` `WebSocketListener` | service | streaming | OkHttp is HTTP-only today (`ReaderRepository`, `ImageStore`). Copy client construction; WS protocol from RESEARCH |
| `BunkerClient` NIP-46 RPC | service | request-response | No kind 24133 / NIP-44 client. Copy Amethyst login sequence from RESEARCH, persist like `AuthViewModel.onSignerResult` |

## Do not touch

| File | Why |
|------|-----|
| `nostr/RemoteSignerBridge.kt` | NIP-55 intents only. WebSockets do not belong here |
| `nostr/SignerResult.kt` | Amber Activity-result mapping |
| `nostr/Nip19.kt` | Reuse for display + normalize |
| Backup XML | Already excludes `boris_session.xml` |

## Metadata

**Analog search scope:** `app/src/main/java/org/dergigi/boris/`, `app/src/test/java/org/dergigi/boris/`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.planning/phases/01-amber-login/01-PATTERNS.md`
**Files scanned:** 24 live Kotlin/XML/Gradle files + Phase 1 pattern map
**Pattern extraction date:** 2026-08-14
