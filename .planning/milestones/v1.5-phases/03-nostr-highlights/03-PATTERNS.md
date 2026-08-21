# Phase 3: Nostr highlights - Pattern Map

**Mapped:** 2026-08-15
**Files analyzed:** 20
**Analogs found:** 19 / 20

Phase 1/2 session, Amber intent, bunker RPC, and `RelaySocket` stay the shape to copy. Highlights hang off `ReaderViewModel` + `ReaderScreen`. Do not add Quartz, Room, Hilt, or a second identity store. Do not edit `com.readwithboris`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `data/ArticleUrl.kt` | utility | transform | `data/UrlExtractor.kt` | role-match |
| `nostr/Nip84.kt` | utility | transform | `nostr/Nip01Event.kt` + `nostr/Nip19.kt` | role-match |
| `nostr/QuoteMatch.kt` | utility | transform | `data/UrlExtractor.kt` | role-match |
| `nostr/RelayList.kt` | utility | transform | `nostr/BunkerUri.kt` + `data/Session.kt` | exact |
| `nostr/RelayQuery.kt` | service | request-response | `nostr/BunkerClient.kt` + `nostr/RelaySocket.kt` | role-match |
| `ui/reader/HighlightMarks.kt` | component | transform | `ui/home/HomeScreen.kt` `HomeCopy` | exact |
| `ui/reader/HighlightTextToolbar.kt` | component | event-driven | — | none |
| `nostr/RemoteSignerBridge.kt` | utility | request-response | itself (`buildGetPublicKeyIntent`) | exact |
| `nostr/SignerResult.kt` | model | transform | itself (`SignerResults.parse`) | exact |
| `nostr/BunkerClient.kt` | service | request-response | itself (`pair` / `logout`) | exact |
| `nostr/Nip01Event.kt` | model | transform | itself (`KIND_*` + `parse` / `verify`) | exact |
| `ui/reader/ReaderViewModel.kt` | store | request-response | itself + `ui/auth/AuthViewModel.kt` | exact |
| `ui/reader/ReaderScreen.kt` | component | event-driven | itself + `ui/home/HomeScreen.kt` launcher | exact |
| `ui/home/HomeScreen.kt` | component | transform | itself (`HomeCopy`) | exact |
| `res/values/strings.xml` | config | — | itself (`auth_*`) | exact |
| `data/ArticleUrlTest.kt` | test | — | `data/UrlExtractorTest.kt` | exact |
| `nostr/Nip84Test.kt` | test | — | `nostr/Nip19Test.kt` | exact |
| `nostr/QuoteMatchTest.kt` | test | — | `data/UrlExtractorTest.kt` | exact |
| `nostr/RelayListTest.kt` | test | — | `nostr/BunkerUriTest.kt` | exact |
| `nostr/SignerResultTest.kt` | test | — | itself | exact |

Unchanged (do not edit): `data/Session.kt`, `data/SessionStore.kt`, `data/UrlExtractor.kt`, `data/ReadableContent.kt`, `data/ReaderRepository.kt`, `ui/auth/AuthViewModel.kt`, `ui/auth/AuthBar.kt`, `ui/theme/Color.kt` (keep `HighlightMine = #FDE047`; alpha lives in `HighlightMarks`).

Implied type colocated with owner (do not split): `BunkerSignResult` stays in `BunkerClient.kt` like `BunkerResult`. Highlight list stays on `ReaderUiState` / `ReaderViewModel.kt`.

## Pattern Assignments

### `data/ArticleUrl.kt` (utility, transform)

**Analog:** `data/UrlExtractor.kt`

**Object + fail-closed helper** (lines 1-27):
```kotlin
package org.dergigi.boris.data

object UrlExtractor {
    fun normalize(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
```

**URI resolve + swallow** (lines 42-50):
```kotlin
val absolute = try {
    java.net.URI(baseUrl).resolve(trimmed).toString()
} catch (_: Exception) {
    trimmed
}
```

**Copy:** `object ArticleUrl` in `data/`. New `normalize` that forces `https`, drops `www`, trailing slash, query, and `#fragment`. Use `java.net.URI` like `articleUrl`. Return the input-ish string on parse failure (`catch (_: Exception)`). No Android types.

**Do not copy:** `UrlExtractor.normalize`. That function only prepends `https`. Do not overload it (D-04). Do not copy the webapp `normalizeUrl` that drops the scheme.

---

### `nostr/Nip84.kt` (utility, transform)

**Analog:** `nostr/Nip01Event.kt` companion constants + `nostr/Nip19.kt` object.

**Kind constants** (`Nip01Event.kt` lines 48-50):
```kotlin
companion object {
    const val KIND_RPC = 24133
    const val KIND_AUTH = 22242
```

**JSON object shape** (`Nip01Event.kt` lines 18-27):
```kotlin
fun toJsonString(): String {
    return JSONObject()
        .put("id", id)
        .put("pubkey", pubkey)
        .put("created_at", createdAt)
        .put("kind", kind)
        .put("tags", tagsToJson(tags))
        .put("content", content)
        .put("sig", sig)
        .toString()
}
```

**Copy:** `object Nip84` with `KIND = 9802` and locked `ALT = "Highlight created by Boris Android. readwithboris.com"`. `tags(url, context)` builds `r` + optional `context` + `alt`. Unsigned JSON: `kind`, `content`, `tags`, `created_at`; Amber URI also gets `pubkey` = session hex. Omit `id` and `sig`.

**Do not copy:** Lantern `textquoteselector` / range tags. No `comment`, `zap`, or `p` author tags.

---

### `nostr/QuoteMatch.kt` (utility, transform)

**Analog:** `data/UrlExtractor.kt` (`extract` / `imageUrls` as a small `object` of string helpers).

**Copy:** `object QuoteMatch` with `occurrences(haystack, quote): List<IntRange>`. Trim the quote. Loop `indexOf` from `start = i + q.length`. Empty quote → empty list. Title and body both call this. Gate paint with `occurrences(content.body, quote).isEmpty()` (D-05). Paint every range, including two events with the same quote (D-06).

**Do not copy:** Fuzzy / whitespace-normalized match. Do not parse relay `content` as markdown.

---

### `nostr/RelayList.kt` (utility, transform)

**Analog:** `nostr/BunkerUri.kt` (`isAllowedRelay`) and `data/Session.kt` (`fromStoredBunker` wss filter).

**wss-only relay accept** (`BunkerUri.kt` lines 37-41):
```kotlin
internal fun isAllowedRelay(value: String): Boolean {
    if (value.startsWith("wss://", ignoreCase = true)) return true
    if (!value.startsWith("ws://", ignoreCase = true)) return false
    return isLoopbackHost(wsHost(value))
}
```

**Session bunker relay split** (`Session.kt` lines 38-42):
```kotlin
val relays = relaysCsv.orEmpty()
    .split(',')
    .map { it.trim() }
    .filter { it.startsWith("wss://", ignoreCase = true) }
```

**NIP-01 tag walk** (`Nip01Event.kt` lines 45-46):
```kotlin
fun hasPTag(pubkeyHex: String): Boolean =
    tags.any { it.size >= 2 && it[0] == "p" && it[1].equals(pubkeyHex, ignoreCase = true) }
```

**Copy:** Parse kind 10002 `r` tags: `["r", url]` or `["r", url, "read"|"write"]`. Omitted marker = both. Accept only `wss://` for content relays (stricter than bunker loopback `ws://`). Newest `created_at` wins. Empty read or write list → fallback `wss://relay.damus.io`, `wss://nos.lol`, `wss://relay.primal.net`, `wss://wot.dergigi.com`.

**Do not copy:** Bunker pairing relays. Do not add `relay.nsec.app` or nostr.band. Do not cache 10002 (D-19).

---

### `nostr/RelayQuery.kt` (service, request-response)

**Analog:** `nostr/BunkerClient.kt` + `nostr/RelaySocket.kt`

**Socket open / send / close** (`RelaySocket.kt` lines 20-66):
```kotlin
fun open(onOpen: () -> Unit, onMessage: (String) -> Unit, onFailure: () -> Unit = {}) { ... }
fun send(text: String) { ... }
fun close() {
    isOpen = false
    synchronized(pending) { pending.clear() }
    socket?.close(1000, null)
    socket = null
}
```

**REQ frame** (`BunkerClient.kt` lines 255-260):
```kotlin
private fun reqMessage(clientPub: String, subId: String): String {
    val filter = JSONObject()
        .put("kinds", JSONArray().put(Nip01Event.KIND_RPC))
        .put("#p", JSONArray().put(clientPub))
    return JSONArray().put("REQ").put(subId).put(filter).toString()
}
```

**EVENT publish** (`BunkerClient.kt` lines 196-197):
```kotlin
val message = JSONArray().put("EVENT").put(JSONObject(event.toJsonString())).toString()
sockets.forEach { it.send(message) }
```

**Open-then-finally-close** (`BunkerClient.kt` lines 36-81):
```kotlin
val sockets = mutableListOf<RelaySocket>()
try {
    if (!openSockets(...)) return BunkerResult.RelayTimeout
    // REQ / await
} catch (_: Exception) {
    return BunkerResult.Rejected
} finally {
    sockets.forEach { it.close() }
}
```

**Connect timeout + shared OkHttp** (`BunkerClient.kt` lines 282-292):
```kotlin
private const val RELAY_CONNECT_TIMEOUT_MS = 15_000L
private val defaultClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.SECONDS)
    .build()
```

**Incoming EVENT parse + verify** (`BunkerClient.kt` lines 240-252):
```kotlin
val event = Nip01Event.parse(arr.getJSONObject(2)) ?: return null
if (!event.verify()) return null
```

**Copy:** New type that opens `RelaySocket`s, sends REQ/EVENT, waits for EOSE/OK, closes in `finally`. Dual 9802 filters in one REQ: opened URL and D-04 normalized URL. Success = at least one `OK` true. Ignore AUTH on content relays (no user key). Query/publish timeout ~8s. Connect timeout can stay 15s. Swallow relay failure (D-02).

**Do not copy:** `answerAuth` / NIP-44 / kind 24133. Those stay on the bunker client. Do not use pairing relays for 10002/9802.

---

### `ui/reader/HighlightMarks.kt` (component, transform)

**Analog:** `ui/home/HomeScreen.kt` `HomeCopy` + `highlightRects` (lines 166-241)

**drawBehind + rounded marker** (lines 188-209):
```kotlin
var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
val padX = 5.dp
val padY = 3.dp
val radius = 3.dp
Text(
    text = annotated,
    onTextLayout = { layout = it },
    modifier = Modifier.drawBehind {
        val result = layout ?: return@drawBehind
        val padXPx = padX.toPx()
        val padYPx = padY.toPx()
        val corner = CornerRadius(radius.toPx())
        annotated.getStringAnnotations(HIGHLIGHT_TAG, 0, annotated.length).forEach { range ->
            highlightRects(result, range.start, range.end).forEach { box ->
                drawRoundRect(
                    color = HighlightMine,
                    topLeft = Offset(box.left - padXPx, box.top - padYPx),
                    size = Size(box.width + padXPx * 2, box.height + padYPx * 2),
                    cornerRadius = corner,
                )
            }
        }
    },
)
```

**Line-box helper** (lines 223-241):
```kotlin
private fun highlightRects(layout: TextLayoutResult, start: Int, end: Int): List<Rect> {
    if (start >= end) return emptyList()
    val first = layout.getLineForOffset(start)
    val last = layout.getLineForOffset(end - 1)
    return (first..last).mapNotNull { line ->
        val lineStart = maxOf(start, layout.getLineStart(line))
        val lineEnd = minOf(end, layout.getLineEnd(line, visibleEnd = true))
        if (lineEnd <= lineStart) return@mapNotNull null
        val lastChar = (lineEnd - 1).coerceAtLeast(lineStart)
        val firstBox = layout.getBoundingBox(lineStart)
        val lastBox = layout.getBoundingBox(lastChar)
        Rect(
            left = firstBox.left,
            top = minOf(firstBox.top, lastBox.top),
            right = lastBox.right,
            bottom = maxOf(firstBox.bottom, lastBox.bottom),
        )
    }
}
```

**Existing alpha on notice chrome** (`AuthBar.kt` lines 278-279) — visual cousin, not the marker drawer:
```kotlin
.border(1.dp, HighlightMine.copy(alpha = 0.35f), LoginShape)
.background(HighlightMine.copy(alpha = 0.10f))
```

**Copy:** Move `highlightRects` + `drawBehind` into `HighlightMarks`. Fill with `HighlightMine.copy(alpha = HighlightMarkAlpha)` (`0.45f` is discretion). HomeCopy and reader title/body Text both call this. Two overlapping rects read stronger (D-06).

**Do not copy:** `SpanStyle(background=…)` for reader marks (overlaps do not stack). Do not fork mikepenz. Override Markdown `components` so each block `Text` gets `onTextLayout` + the same drawer. Match against the displayed string, not raw markdown, after D-05 has gated on `content.body` / title.

---

### `ui/reader/HighlightTextToolbar.kt` (component, event-driven)

**Analog:** none in-repo. Planner uses RESEARCH.md Pattern 1 (`TextToolbar` + `ActionMode`).

**Closest Compose local swap** (`ReaderScreen.kt` lines 274-275):
```kotlin
CompositionLocalProvider(LocalUriHandler provides uriHandler) {
    Markdown(
```

**Selection host** (`ReaderScreen.kt` lines 251-256):
```kotlin
SelectionContainer(
    modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxWidth()
        .padding(bottom = 48.dp),
) {
```

**Copy:** One type per file (`HighlightTextToolbar : TextToolbar`). Provide via `CompositionLocalProvider(LocalTextToolbar provides …)` around the existing `SelectionContainer` in `ArticleBody` only. Copy always. Highlight only when a session exists. On Highlight, run `onCopyRequested`, read `LocalClipboardManager`, restore prior clipboard, then call `onHighlight(quote)`.

**Do not copy:** Home / AuthBar `SelectionContainer` (npub copy). Do not bump the Compose BOM. Do not add MagicalSelection.

---

### `nostr/RemoteSignerBridge.kt` (utility, request-response)

**Analog:** itself

**Existing intent builder** (lines 24-29):
```kotlin
fun buildGetPublicKeyIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:")).apply {
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("type", "get_public_key")
    }
}
```

**Copy:** Add `buildSignEventIntent(unsignedJson, signerPackage, currentUserHex)` with URI `nostrsigner:$unsignedJson`, same flags, `type=sign_event`, `current_user` = session hex, and `intent.package = signerPackage`. Domain builds; Compose launches.

**Do not copy:** Omitting `package` (that is only for first `get_public_key`). Do not send `permissions`. Do not query `content://….SIGN_EVENT`. Do not put WebSockets on this bridge.

---

### `nostr/SignerResult.kt` (model, transform)

**Analog:** itself

**Login parse** (lines 6-36):
```kotlin
sealed class SignerResult {
    data class Success(val pubkeyHex: String, val signerPackage: String) : SignerResult()
    data object Rejected : SignerResult()
    data object Cancelled : SignerResult()
}

object SignerResults {
    fun parse(resultCode: Int, data: Intent?): SignerResult {
        return parse(
            resultCode = resultCode,
            rejected = data?.getBooleanExtra("rejected", false) == true,
            result = data?.getStringExtra("result"),
            signature = data?.getStringExtra("signature"),
            packageName = data?.getStringExtra("package"),
        )
    }
}
```

**Pubkey normalize** (`Nip19.kt` lines 19-32):
```kotlin
fun normalizePubkey(value: String): String? { ... }
```

**Event parse + verify** (`Nip01Event.kt` lines 30-42, 68-90):
```kotlin
fun verify(): Boolean { ... }
fun parse(json: JSONObject): Nip01Event? {
    return try { ... } catch (_: Exception) { null }
}
```

**Copy:** Keep `parse` for login. Add `parseSignedEvent` that reads extra `event` (and `result` as fallback JSON). `Nip01Event.parse`. Require `kind == 9802`, `pubkey` equals session hex, `verify()`. `rejected` → `Rejected`. `resultCode != RESULT_OK` → `Cancelled`. New `SignerResult.Signed(event)` (or a sibling sealed type if login `Success` must stay login-shaped).

**Do not copy:** Treating `result` / `signature` as an npub on the sign path (Pitfall 7).

---

### `nostr/BunkerClient.kt` (service, request-response)

**Analog:** itself (`pair` / `logout`)

**RPC publish + await** (lines 58-76, 200-237, 262-267):
```kotlin
publish(sockets, keypair, parsed.remoteSignerPubkey, rpcJson(gpId, "get_public_key", JSONArray()))
val gpOutcome = awaitRpc(...) ?: return BunkerResult.RelayTimeout
if (gpOutcome.rejected) return BunkerResult.Rejected

private fun rpcJson(id: String, method: String, params: JSONArray): String =
    JSONObject().put("id", id).put("method", method).put("params", params).toString()
```

**auth_url VIEW** (`AuthViewModel.kt` lines 145-152):
```kotlin
private fun openAuthUrl(url: String) {
    val app = getApplication<Application>()
    runCatching {
        app.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
```

**Unwrap then drop** (`AuthViewModel.kt` lines 97-111):
```kotlin
val privkey = bunker?.let { SecretBox.unwrap(app, it.clientPrivkeyCiphertext) }
...
if (privkey != null && remote != null && relays.isNotEmpty()) {
    viewModelScope.launch(Dispatchers.IO) {
        BunkerClient(onAuthUrl = {}).logout(relays, remote, privkey)
    }
}
```

**Copy:** Add `signEvent(relays, remoteSignerPubkey, clientPrivkey, unsignedJson)`. Repeat open / AUTH / RPC / close. Reuse `RPC_TIMEOUT_MS` (65000) and `onAuthUrl`. Params: `[unsignedJson]`. Parse `result` with `Nip01Event.parse`. Map reject / timeout like `pair`. Colocate `BunkerSignResult` in this file.

**Do not copy:** A live session socket. `pair` already closes in `finally`; `signEvent` must reopen (Pitfall 5). Do not use content relays for this RPC.

---

### `nostr/Nip01Event.kt` (model, transform)

**Analog:** itself

**Copy:** Add `KIND_HIGHLIGHT = 9802` and `KIND_RELAY_LIST = 10002` next to `KIND_RPC` / `KIND_AUTH` (lines 49-50). Reuse `parse` + `verify` before paint. Do not add a second event type.

---

### `ui/reader/ReaderViewModel.kt` (store, request-response)

**Analog:** itself for article load; `ui/auth/AuthViewModel.kt` for session + signer + IO.

**Article load must stay unblocked** (`ReaderViewModel.kt` lines 52-68):
```kotlin
fun load() {
    if (url.isBlank()) {
        _state.value = ReaderUiState.Error("No URL to read.", url)
        return
    }
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

**AndroidViewModel + SessionStore** (`AuthViewModel.kt` lines 28-32, 154-159):
```kotlin
class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    ...
    val session = SessionStore.load(app)
```

**Signer result → message** (`AuthViewModel.kt` lines 79-94):
```kotlin
fun onSignerResult(resultCode: Int, data: Intent?) {
    when (val result = SignerResults.parse(resultCode, data)) {
        is SignerResult.Success -> { ... }
        SignerResult.Rejected -> {
            _message.value = app.getString(R.string.auth_rejected)
        }
        SignerResult.Cancelled -> {
            _message.value = app.getString(R.string.auth_cancelled)
        }
    }
}
```

**IO side effect + early return** (`ReaderViewModel.kt` lines 33-38, 52-56):
```kotlin
fun openGallery(urls: List<String>, index: Int) {
    if (urls.isEmpty()) return
    _gallery.value = ImageGalleryState(...)
}
```

**Copy:** Become `AndroidViewModel` so `SessionStore.load` and `SecretBox.unwrap` work. Keep `load()` painting `Ready` without waiting on relays. Separate IO job for 10002 + 9802 fetch. After signed event: paint immediately, publish on `Dispatchers.IO`, remove that id if no `OK`. Signer cancel/reject: no mark. Private `_state` / public `state`. Verb actions: `load`, `highlight`, `onSignedEvent`, `onSignerCancelled`.

**Do not copy:** AuthBar `NoticeCard` for highlight errors (D-02 / D-14). Reader toasts. Do not block `ReaderUiState.Ready`. Do not invent a second session store. Do not paint before sign (Pitfall 6).

---

### `ui/reader/ReaderScreen.kt` (component, event-driven)

**Analog:** itself + `ui/home/HomeScreen.kt` launcher + `ui/reader/ImageGallery.kt` toast.

**Screen wrapper collects, Content renders** (`ReaderScreen.kt` lines 64-81):
```kotlin
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery by viewModel.gallery.collectAsStateWithLifecycle()
    ReaderScreenContent(...)
}
```

**Reader-owned Amber launcher** (`HomeScreen.kt` lines 82-86):
```kotlin
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
) { result ->
    viewModel.onSignerResult(result.resultCode, result.data)
}
```

**Short toast** (`ImageGallery.kt` lines 131-135):
```kotlin
Toast.makeText(
    context,
    if (ok) "Saved to Pictures" else "Couldn't save this image",
    Toast.LENGTH_SHORT,
).show()
```

**Copy:** Keep `ReaderScreen` / `ReaderScreenContent`. Add a Reader-scoped `rememberLauncherForActivityResult`. Wire `HighlightTextToolbar` only around `ArticleBody`. Pass marks + `loggedIn` into `ArticleBody`. Toast cancel / reject / publish-fail from a message flow or a one-shot callback. Nested `fun`s for local handlers (`openOriginal`, `shareArticle` style).

**Do not copy:** Home's Amber launcher (different Nav back stack). Do not put network in a Composable. Do not wrap Home's `SelectionContainer`.

---

### `ui/home/HomeScreen.kt` (component, transform)

**Analog:** itself (`HomeCopy`)

**Copy:** Keep annotations and `highlightRects` usage, but draw with `HighlightMarks` / `HighlightMine.copy(alpha = HighlightMarkAlpha)` (D-07). AuthBar and login launcher stay as they are.

**Do not copy:** Highlight toolbar onto Home.

---

### `res/values/strings.xml` (config)

**Analog:** itself (`auth_rejected` / `auth_cancelled`, lines 16-17)

```xml
<string name="auth_rejected">Amber declined the request.</string>
<string name="auth_cancelled">Amber did not return a key.</string>
```

**Copy:** Add short highlight strings. RESEARCH recommendation: `Highlight cancelled.`, `Highlight rejected.`, `Highlight not published.` Same three outcomes for bunker. Use `stringResource` / `getString` like auth.

**Do not copy:** AuthBar `NoticeCard` wording into the reader.

---

### Tests

**Analog:** `data/UrlExtractorTest.kt`, `nostr/Nip19Test.kt`, `nostr/BunkerUriTest.kt`, `nostr/SignerResultTest.kt`

**JUnit 4 shape** (`UrlExtractorTest.kt` lines 6-27):
```kotlin
package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {
    @Test
    fun normalizesProtocolLessUrls() {
        assertEquals("https://www.example.com", UrlExtractor.normalize("www.example.com"))
    }
}
```

**Signer extras** (`SignerResultTest.kt` lines 38-59):
```kotlin
@Test
fun rejectedMapsToRejected() { ... }
@Test
fun canceledMapsToCancelled() { ... }
```

**Copy:** Mirror packages under `app/src/test/java/org/dergigi/boris/`. `{Type}Test.kt`. No Robolectric. `ArticleUrlTest`: www, http, trailing slash, `?utm`, `#section`, already-clean https. `Nip84Test`: tags + locked alt, no selector tags. `QuoteMatchTest`: exact hits, miss, duplicates. `RelayListTest`: read/write/both, reject non-`wss://`, fallback when empty. Extend `SignerResultTest` with signed-event JSON, reject, cancel, bad kind, failed verify.

**Do not copy:** Tests under `com.readwithboris`.

## Shared Patterns

### Session lookup
**Source:** `data/SessionStore.kt` lines 17-32, `data/Session.kt` lines 3-17
**Apply to:** `ReaderViewModel` (logged-in gate, Amber vs bunker sign path)
```kotlin
sealed interface Session {
    val pubkeyHex: String
    data class Amber(override val pubkeyHex: String, val signerPackage: String) : Session
    data class Bunker(...) : Session
}
fun load(context: Context): Session?
```
Logged out (`load` is null): no toolbar item, no REQ, no marks.

### Amber intent launch
**Source:** `HomeScreen.kt` lines 82-86 + `RemoteSignerBridge.kt` lines 24-29
**Apply to:** Reader Highlight (own launcher, not Home's)
```kotlin
rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    viewModel.onSignerResult(result.resultCode, result.data)
}
```

### Bunker RPC
**Source:** `BunkerClient.kt` `pair` / `logout` (open, `rpcJson`, `awaitRpc`, close)
**Apply to:** `BunkerClient.signEvent` only. Pairing relays stay pairing relays.

### Event verify before trust
**Source:** `Nip01Event.kt` `parse` + `verify`, `BunkerClient.incomingEvent` lines 240-252
**Apply to:** Every fetched or signed 9802 before paint. Also `pubkey == session.pubkeyHex`.

### Error handling
**Source:** `ReaderViewModel.load` (article errors → `ReaderUiState.Error`); `ImageGallery.kt` toast for side effects; `BunkerClient` `catch (_: Exception)` + sealed results
**Apply to:** Article fetch unchanged. Highlight relay miss/timeout: keep reading, no chrome (D-02). Signer cancel/reject and publish fail: short toast, no mark (or remove the optimistic mark).

### Validation
**Source:** `UrlExtractor` / `BunkerUri` return `null`; `Nip01Event.parse` returns `null`
**Apply to:** `ArticleUrl`, `RelayList`, `QuoteMatch`, unsigned/signed JSON. No custom exception hierarchy.

### Logging
**Source:** CONVENTIONS.md — no `Log`, Timber, or `println`
**Apply to:** All new Kotlin. Do not log quotes or `SecretBox` material.

### Imports and file rules
**Source:** `.planning/codebase/CONVENTIONS.md`
**Apply to:** All new files
- One primary type per file; filename matches the type
- Import order: Android → AndroidX → third-party → Kotlin/Java → `org.dergigi.boris.*`; no blank lines between groups; no wildcards
- No KDoc / no obvious comments
- JVM JUnit 4 only; tests mirror the production package
- No Hilt, Koin, Room, DataStore, nostrdb

### Marker color
**Source:** `ui/theme/Color.kt` line 22
**Apply to:** Home copy marks and reader marks
```kotlin
val HighlightMine = Color(0xFFFDE047)
```
Alpha is a `HighlightMarks` constant, not a new Color.kt token unless the planner prefers one.

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `ui/reader/HighlightTextToolbar.kt` | component | event-driven | No `TextToolbar` / `ActionMode` implementation in-repo. Use RESEARCH.md Pattern 1. Closest Compose seam is `ReaderScreen` `CompositionLocalProvider` + `SelectionContainer`. |

## Metadata

**Analog search scope:** `app/src/main/java/org/dergigi/boris/` (`data/`, `nostr/`, `ui/reader/`, `ui/home/`, `ui/auth/`, `ui/theme/`), `app/src/test/java/org/dergigi/boris/`, `app/src/main/res/values/strings.xml`
**Files scanned:** 24 production/test sources plus CONVENTIONS.md / STRUCTURE.md
**Pattern extraction date:** 2026-08-15
