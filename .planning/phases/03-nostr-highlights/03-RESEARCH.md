# Phase 3: Nostr highlights - Research

**Researched:** 2026-08-14
**Domain:** Android NIP-84 highlights (Compose selection toolbar, Markdown marks, Amber NIP-55 `sign_event`, bunker NIP-46 `sign_event`, kind 10002/9802 over OkHttp WebSocket)
**Confidence:** HIGH for protocol and in-repo seams (official NIPs fetched; Android/webapp sources read). MEDIUM for Compose toolbar selected-text access on the current BOM.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Whose marks
- **D-01:** Paint only the logged-in user's highlights for this article URL (kind 9802, authors = session pubkey, `#r` = URL). Logged out: no fetch, no marks, no Highlight action.
- **D-02:** If relays fail or time out, keep reading. No extra error chrome for highlights.
- **D-03:** After create, paint immediately, then publish in the background. If publish fails, remove that mark.
- **D-04:** Match `r` tags after normalizing the article URL: https, drop `www`, drop trailing slash, drop query string, drop `#fragment`.
- **D-05:** If the quote is not in the fetched article body (Jina markdown), skip painting. No message. No fuzzy match.
- **D-06:** Duplicate quotes are two events. Paint both. Marker fill uses alpha so overlaps read stronger.
- **D-07:** Marker color is `HighlightMine` (`#FDE047`) with alpha. Home copy marks ("npub", "your own highlights") use the same opacity style.

### Event shape
- **D-08:** Create NIP-84 events like the Boris webapp, not Lantern's extra selector tags. Kind 9802, `content` = exact selected quote, `r` = article URL, `context` = a slice of surrounding body when we can take one. — **Reversibility:** one-way — published events are a relay contract other clients will fetch.
- **D-09:** `alt` tag is `Highlight created by Boris Android. readwithboris.com` (site https://www.readwithboris.com/). Do not change the webapp's alt this phase.
- **D-10:** Fetch any of the user's kind 9802s for this URL. Paint when `content` matches the body. Do not require `context` or our `alt`.

### Create gesture
- **D-11:** Highlight is an action on the system text selection toolbar, next to Copy. Hidden when logged out.
- **D-12:** Tap Highlight goes straight to `sign_event`. No confirm sheet. No comment / note field.
- **D-13:** Title and body are both valid selection sources.
- **D-14:** If the user cancels or rejects the signer, no mark, short toast so it is obvious it did not publish.

### Who signs
- **D-15:** Sign with the stored session: Amber NIP-55 `sign_event` or bunker NIP-46 `sign_event`. Never an nsec in Boris.
- **D-16:** Publish to the user's NIP-65 (kind 10002) write relays. Fetch from their NIP-65 read relays. Same filter: kind 9802, authors = me, `#r` = normalized URL. Do not use bunker pairing relays for content. No author-inbox extra relays (web URLs only).
- **D-17:** Bootstrap / missing 10002: hardcoded `wss://relay.damus.io`, `wss://nos.lol`, `wss://relay.primal.net`, `wss://wot.dergigi.com`. Researcher may trim or add one if a relay is dead. Use this list to find 10002 and to publish/fetch when the user has no relay list.
- **D-18:** Amber prompts on every highlight. Do not request remembered `sign_event` permissions this slice. Bunker prompts on the remote signer the same way.
- **D-19:** Do not cache kind 10002. Fetch it every time. A later cache (maybe nostrdb) is out of this phase.

### Claude's Discretion
- Kotlin stack for relays, NIP-65, NIP-84 templates, and bunker `sign_event` (reuse Phase 2 NIP-46 client; do not pull Amethyst Quartz).
- How Compose exposes a Highlight action on the selection toolbar.
- How marks are drawn on Markdown (keep DRY with Home copy marks / `HighlightMine`).
- Context slice length and how it is cut from the body.
- Toast copy for cancel/reject and for a failed publish (mark already removed).
- Amber `sign_event` intent extras: follow Dark Wisp if it already signs; otherwise the smallest NIP-55 `sign_event` that Amber accepts.

### Deferred Ideas (OUT OF SCOPE)
- Lantern comments / kind 1111 threads
- Highlights sidebar
- Friends / nostrverse highlight levels
- Zap splits
- Bookmarks
- Relay-list cache / nostrdb
- Remembered Amber `sign_event` permissions
- Author-inbox relays for Nostr-native articles
- Copying Lantern `textquoteselector` / range tags
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| HIGH-01 | While logged in, select article text, publish a NIP-84 highlight, and see own highlights for that URL painted. Logged out, the reader is unchanged. | Selection toolbar Highlight; NIP-84 template; optimistic paint; RelayQuery 9802; HomeCopy-style marks; hide all of that when `SessionStore.load` is null |
| AUTH-06 | Sign kind 9802 through the stored session (Amber NIP-55 `sign_event` or bunker NIP-46 `sign_event`). Never hold an `nsec`. | `RemoteSignerBridge.buildSignEventIntent` + Compose launcher; `BunkerClient.signEvent` on the existing client; parse `event` extra / RPC result into `Nip01Event` |
| READ-01 | Paste, share, or open a URL and read while logged out. Login UI stays on Home. | Reader fetch/paint must not block `ReaderUiState.Ready`. Logged-out path is today's `SelectionContainer` + Markdown with no Highlight item and no relay work |
</phase_requirements>

## Summary

Phase 3 is a thin slice on top of the existing reader and the two login paths. While a session exists, the user selects title or body text, taps Highlight next to Copy, signs a kind 9802 through Amber or the bunker, and sees that quote marked in the article. Relays are queried for the same pubkey + URL so a highlight made on the webapp can paint here when the quote is still in the Jina body. Logged out, nothing new happens. Boris never holds an `nsec`.

The protocol work is small and already half-built: `Nip01Event`, `RelaySocket`, `BunkerClient`, and `RemoteSignerBridge` exist. Do not add Quartz, a Nostr SDK, Room, or a new Maven library. Hand-roll a NIP-84 tag builder, a URL normalizer, a quote matcher, and a short REQ/EVENT helper on `RelaySocket`.

The two hard UI seams are (1) getting selected text plus a Highlight item on the system toolbar under Compose BOM `2025.06.01`, and (2) painting overlapping alpha marks on mikepenz Markdown without forking it. Use a custom `LocalTextToolbar` (Copy stays; Highlight only when logged in; read the quote via the existing copy callback). Draw marks with the HomeCopy `drawBehind` + `highlightRects` helper and an alpha `HighlightMine`, including on Home.

**Primary recommendation:** Extend the existing `nostr/` + `ReaderViewModel` files. Custom `TextToolbar` for Highlight. Shared mark drawer for Home and reader. Amber intent from NIP-55 (Dark Wisp no longer signs). `BunkerClient.signEvent` as one more RPC. Content relays via `RelaySocket`, never bunker pairing relays.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Highlight action on selection toolbar | Browser / Client (Compose) | — | D-11. Compose owns the launcher and the toolbar. Domain builds the unsigned event only |
| Paint marks on title + Markdown | Browser / Client | — | Read-layer mark. Same drawer as Home copy. No network in a Composable |
| Session lookup (Amber vs bunker) | Database / Storage (`SessionStore`) | Browser / Client | One store. Reader reads it; do not invent a second identity |
| Amber `sign_event` | Browser / Client (intent) | — | NIP-55. `RemoteSignerBridge` builds; Compose launches. No ContentResolver |
| Bunker `sign_event` | API / Backend (`BunkerClient`) | Browser / Client (`auth_url` VIEW) | Same kind 24133 + NIP-44 client as pair. Pairing relays only |
| Kind 10002 / 9802 query and publish | API / Backend (`RelaySocket`) | CDN / Static (user relays) | NIP-01 REQ/EVENT. Fallback list when 10002 is missing. Not Quartz |
| URL normalize + quote match | API / Backend (pure Kotlin) | — | Testable JVM helpers. Gate paint (D-05) and `#r` match (D-04) |
| Optimistic mark + rollback | Browser / Client (`ReaderViewModel`) | API / Backend (publish) | Sign success paints. Publish failure removes. Signer cancel never paints |
| Article fetch (Jina) | API / Backend (`ReaderRepository`) | — | Unchanged. Highlights must not block Ready |

## Project Constraints (from .cursor/rules/)

No `.cursor/rules/` files in this repo. Follow `.planning/codebase/CONVENTIONS.md` and `STRUCTURE.md`: one type per file under `org.dergigi.boris`, no Hilt/Koin/Room, no `Log`, JVM JUnit 4 only, do not edit `com.readwithboris`.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin + Compose BOM | Kotlin `2.1.21`, BOM `2025.06.01` | UI, `SelectionContainer`, `LocalTextToolbar` | Already on the classpath. Stay here; do not bump to get 1.9/1.12 APIs this slice |
| mikepenz markdown | `0.35.0` | Article body | Already used in `ReaderScreen`. Annotator/components exist; do not fork |
| OkHttp | `4.12.0` | HTTPS + WebSocket | `ReaderRepository` and `RelaySocket` already share this |
| secp256k1-kmp | `0.22.0` | Verify signed 9802; bunker client AUTH | Pinned. `0.24.0` is Kotlin 2.3 metadata |
| org.json (Android) | platform | Unsigned event JSON, REQ/EVENT frames | `Nip01Event` / `BunkerClient` already use it |
| JUnit | `4.13.2` | JVM tests | Existing test style |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| activity-compose | `1.10.1` | `rememberLauncherForActivityResult` | Reader-scoped Amber `sign_event` launcher, same as Home |
| BouncyCastle bcprov | `1.85.2` | NIP-44 | Already used by bunker; do not touch |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom `LocalTextToolbar` | Bump BOM to `2025.08.00`+ (`appendTextContextMenuComponents`) | Official item API, but `TextContextMenuSession` only has `close()`. Still no selected text until `rememberSelectionState` (foundation `1.12.0-beta02`). Not worth a BOM bump |
| Custom `LocalTextToolbar` | MagicalSelection or similar | Unverified third-party. Do not add |
| `RelaySocket` REQ/EVENT | Amethyst Quartz / rust-nostr | Locked out. Too large |
| HomeCopy `drawBehind` | Markdown `annotator` + `SpanStyle` background | Span styles do not stack; D-06 needs overlapping alpha |
| HomeCopy `drawBehind` | mikepenz `extendedSpans` | Extra painter API; Home already has the drawer we need |

**Installation:** none. No new Maven packages.

**Version verification:** versions above are from `gradle/libs.versions.toml` read this session. Kotlin `2.1.21`, BOM `2025.06.01`, markdown `0.35.0`, okhttp `4.12.0`, secp256k1 `0.22.0`, junit `4.13.2`.

## Package Legitimacy Audit

No new packages this phase.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| — | — | — | — | — | — | None to install |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
Select title/body ──► TextToolbar Highlight (logged in only)
                         │
                         ▼
              Nip84.unsigned(quote, url, context, alt)
                         │
         ┌───────────────┴────────────────┐
         ▼                                ▼
 Amber: NIP-55 intent                Bunker: BunkerClient.signEvent
 (package + type + current_user)     (pairing relays, kind 24133)
         │                                │
         └────────── signed 9802 ─────────┘
                         │
                         ▼
              ReaderViewModel: paint now
                         │
                         ▼
         load 10002 on fallback list (no cache)
                         │
         ┌───────────────┴────────────────┐
         ▼                                ▼
 write relays: EVENT 9802            read relays: REQ 9802
                                         authors=me, #r=url
                         │
              OK missing / all fail ──► remove mark + toast
              relay miss / timeout  ──► keep reading (D-02)
```

Logged out: no toolbar item, no REQ, no marks. Jina fetch unchanged.

### Recommended Project Structure

```
app/src/main/java/org/dergigi/boris/
├── data/
│   ├── ArticleUrl.kt          # D-04 normalize (not UrlExtractor.normalize)
│   ├── Session.kt             # unchanged
│   └── SessionStore.kt        # unchanged
├── nostr/
│   ├── Nip84.kt               # kind 9802 tags + unsigned JSON
│   ├── QuoteMatch.kt          # exact quote in title/body
│   ├── RelayList.kt           # parse kind 10002 r tags
│   ├── RelayQuery.kt          # REQ / EVENT / EOSE / OK on RelaySocket
│   ├── RemoteSignerBridge.kt  # add buildSignEventIntent
│   ├── SignerResult.kt        # add Signed(event)
│   ├── BunkerClient.kt        # add signEvent
│   └── Nip01Event.kt          # add KIND_HIGHLIGHT, KIND_RELAY_LIST
└── ui/
    ├── reader/
    │   ├── ReaderScreen.kt    # toolbar + title/body marks + launcher
    │   ├── ReaderViewModel.kt # fetch/paint/publish (AndroidViewModel)
    │   └── HighlightMarks.kt  # shared drawBehind + alpha
    ├── home/HomeScreen.kt     # HomeCopy uses same alpha
    └── theme/Color.kt         # keep HighlightMine = #FDE047
```

Tests: `ArticleUrlTest`, `Nip84Test`, `QuoteMatchTest`, `RelayListTest`, `SignerResult` signed-event cases. Mirror packages under `app/src/test/java/org/dergigi/boris/`.

### Pattern 1: Custom TextToolbar for Highlight

**What:** Provide a `TextToolbar` via `CompositionLocalProvider(LocalTextToolbar provides …)` around the existing `SelectionContainer`. Show Copy always. Show Highlight only when a session exists. On Highlight, run the framework `onCopyRequested`, read `LocalClipboardManager`, then start `sign_event`.

**When to use:** Current BOM. `appendTextContextMenuComponents` needs foundation 1.9. `rememberSelectionState` needs 1.12 beta. `TextContextMenuSession` only exposes `close()`.

**Example:**

```kotlin
// Source: androidx.compose.ui.platform.TextToolbar + AndroidTextToolbar pattern
// https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/TextToolbar
class HighlightTextToolbar(
    private val view: View,
    private val showHighlight: Boolean,
    private val onHighlight: (String) -> Unit,
    private val clipboard: ClipboardManager,
) : TextToolbar {
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        view.startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                if (onCopyRequested != null) menu.add(0, 1, 0, android.R.string.copy)
                if (showHighlight) menu.add(0, 2, 1, "Highlight")
                return true
            }
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    1 -> onCopyRequested?.invoke()
                    2 -> {
                        val prior = clipboard.getText()
                        onCopyRequested?.invoke()
                        val quote = clipboard.getText()?.text.orEmpty()
                        if (prior != null) clipboard.setText(prior) else clipboard.setText(AnnotatedString(""))
                        if (quote.isNotBlank()) onHighlight(quote)
                    }
                }
                mode.finish()
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
            override fun onDestroyActionMode(mode: ActionMode) {}
        }, ActionMode.TYPE_FLOATING)
    }
}
```

Hide Highlight when `SessionStore.load` is null (D-01, D-11). Do not wrap Home's `SelectionContainer` (npub copy). Scope the provider to `ArticleBody`.

### Pattern 2: Shared marker drawer

**What:** Extract HomeCopy's `highlightRects` + `drawBehind` into `HighlightMarks`. Use `HighlightMine.copy(alpha = HighlightMarkAlpha)` on Home and in the reader.

**When to use:** Title `Text` and each Markdown text block that contains a quote.

**Example:**

```kotlin
// Source: app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt:192-207
// verbatim HomeCopy draw:
// color = HighlightMine
// drawRoundRect(... padX 5.dp, padY 3.dp, radius 3.dp)
fun HighlightMineFill(): Color = HighlightMine.copy(alpha = 0.45f)
```

`0.45f` is discretion. Two overlapping rects read stronger (D-06). Do not use `SpanStyle(background=…)` for reader marks.

For Markdown: keep the library. Override `components` (`paragraph` / headings / text) so the rendered `Text` gets `onTextLayout` + `drawBehind` using the same helper. Match the quote against that block's displayed string, not the raw markdown source, after D-05 has already gated on `content.body` / title.

### Pattern 3: Sign then paint then publish

**What:** Toolbar tap builds an unsigned 9802 and launches the stored signer. Only a signed event paints. Publish is IO. Failure removes that id.

**When to use:** Every create. Fetch of existing 9802s is a separate IO job that must not block `ReaderUiState.Ready`.

### Anti-Patterns to Avoid

- **ContentResolver `SIGN_EVENT`:** D-18. Amber must prompt every time.
- **Quartz / rust-nostr / new relay SDK:** locked out.
- **Fork mikepenz:** components + shared drawer are enough.
- **`SpanStyle` background only:** overlaps do not stack.
- **Fuzzy / whitespace-normalized match:** D-05.
- **Lantern `textquoteselector` / range tags:** deferred.
- **Zap / comment / `p` author tags:** deferred; web URLs only.
- **Cache kind 10002:** D-19.
- **Publish or fetch on bunker pairing relays:** D-16.
- **Network inside a Composable.**
- **Edit `com.readwithboris`.**
- **Reuse Home's Amber launcher from Reader:** different Nav back stack. Reader owns its own launcher.
- **Overload `UrlExtractor.normalize`:** that function only prepends `https`. Highlight matching needs D-04.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Schnorr verify / event id | New crypto | `Nip01Event.parse` + `verify()` | Already in repo |
| NIP-44 + kind 24133 | Second bunker client | `BunkerClient` + `publish` / `awaitRpc` | Phase 2 already pairs |
| WebSocket | New client | `RelaySocket` + shared OkHttp | Phase 2 |
| Amber intent launch | `startActivity` in domain | Compose `rememberLauncherForActivityResult` | Phase 1 D-07 |
| Markdown parse/render | Custom renderer | mikepenz `Markdown` | Already shipping |
| nsec / local sign of 9802 | In-app schnorr of user key | Amber or bunker only | AUTH-06 |

**Key insight:** The missing pieces are a 20-line tag template, a URL normalizer, a quote matcher, and a REQ/EVENT loop. The expensive mistakes are a new Nostr stack or a markdown fork.

Do hand-roll NIP-84 tags and URL normalize. They are small and must match locked strings.

## Common Pitfalls

### Pitfall 1: Jina body is not the original HTML
**What goes wrong:** A webapp highlight paints on the original page but not on Android, or a fresh selection does not match `content.body`.
**Why it happens:** Jina markdown drops chrome, rewrites whitespace, and keeps `**`, `[text](url)`, etc. Selection is rendered text.
**How to avoid:** `content` = exact selected string. Paint only if that string is in `title` or `body` (`ReadableContent.body`). No fuzzy match. Title is a separate source (D-13); D-05 skip applies to body quotes that are not in the markdown.
**Warning signs:** User highlights a linked phrase and nothing paints.

### Pitfall 2: Selection across markdown nodes
**What goes wrong:** Quote includes a stray newline or joins two paragraphs; `indexOf` fails; create still signs.
**Why it happens:** `SelectionContainer` concatenates child `Text`s.
**How to avoid:** Publish the exact selected string. Paint only on exact title/body hits. Crossing a block boundary will often skip paint (D-05). That is acceptable.
**Warning signs:** Highlight succeeds in Amber, toast for publish, no mark.

### Pitfall 3: Amber `package` extra
**What goes wrong:** Amber opens the account picker or the wrong app; sign never returns.
**Why it happens:** First `get_public_key` omits `package`. Later methods must set it to the stored `Session.Amber.signerPackage`.
**How to avoid:** `intent.\`package\` = session.signerPackage` on every `sign_event`. Never omit it here.
**Warning signs:** Home login works; reader Highlight does not return.

### Pitfall 4: `#r` exact match vs webapp URLs
**What goes wrong:** Webapp highlight for `https://www.citadel21.com/…` never arrives because Android queried only the normalized host.
**Why it happens:** Relays match `#r` exactly. Webapp `HighlightBlueprint` writes `r` as the opened URL (`includeSingletonTag(["r", source])`). Webapp `normalizeUrl` drops the scheme and lowercases for client-side filter only.
**How to avoid:** Publish `r` = `ReadableContent.url` (opened URL, already https via `UrlExtractor`). REQ two filters in one REQ: opened URL and D-04 normalized URL. Keep events whose `r` normalizes equal to the article. D-16's "normalized URL" is the match key, not the only filter value.
**Warning signs:** Same npub, same article, webapp mark missing on Android.

### Pitfall 5: Bunker reconnect
**What goes wrong:** Second highlight times out; first worked.
**Why it happens:** `BunkerClient.pair` opens sockets and closes them in `finally`. There is no live session socket.
**How to avoid:** `signEvent` repeats open / AUTH / RPC / close. Reuse `RPC_TIMEOUT_MS` (65000) and `onAuthUrl`. Unwrap `SecretBox` for the client privkey, then drop the byte array. Do not use content relays for this RPC.
**Warning signs:** Amber highlights work; bunker highlights fail after the first, or after the phone sleeps.

### Pitfall 6: Optimistic paint before sign
**What goes wrong:** Mark appears, user rejects Amber, mark stays.
**Why it happens:** Misreading D-03.
**How to avoid:** D-03 is after a signed event, before publish. D-14: cancel/reject = no mark + toast.
**Warning signs:** Reject still leaves yellow.

### Pitfall 7: `SignerResults.parse` is login-shaped
**What goes wrong:** Signed event JSON is treated as a pubkey; highlight is dropped.
**Why it happens:** Today's parse takes `result` / `signature` as npub/hex (`SignerResult.Success(pubkeyHex, signerPackage)`).
**How to avoid:** New `parseSignedEvent` that reads extra `event` (and `result` as fallback JSON). `Nip01Event.parse`. Require `kind == 9802`, `pubkey` equals session hex, `verify()`.
**Warning signs:** Amber returns OK, Boris toasts cancel.

## Code Examples

### NIP-84 unsigned event (webapp shape, Android alt)

```kotlin
// Source: applesauce-common@5.1.0 HighlightBlueprint + setSource/setContext
// HighlightBlueprint(content, source, options) =>
//   setHighlightContent(content), setSource(source),
//   options.context => ["context", context],
//   includeAltTag(options.alt ?? "A text highlight")
// setSource(string) => includeSingletonTag(["r", source])
// Locked alt (D-09): "Highlight created by Boris Android. readwithboris.com"
object Nip84 {
    const val KIND = 9802
    const val ALT = "Highlight created by Boris Android. readwithboris.com"

    fun tags(url: String, context: String?): List<List<String>> = buildList {
        add(listOf("r", url))
        if (!context.isNullOrBlank()) add(listOf("context", context))
        add(listOf("alt", ALT))
    }
}
```

Do not add `comment`, `zap`, `p`, `textquoteselector`, `textpositionselector`, or `rangeselector`.

Context slice: copy webapp `extractContext` in `highlightCreationService.ts` (paragraph containing the quote, then previous + selected + next sentence). Return null when the quote is missing or the paragraph is a single sentence. Discretion.

### Amber `sign_event` (NIP-55; Dark Wisp removed this)

```kotlin
// Source: https://github.com/nostr-protocol/nips/blob/master/55.md
// val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:$payload")).apply {
//   `package` = signerPackageName        // omit only for get_public_key
//   putExtra("type", "sign_event")
//   putExtra("id", event.id)             // optional
//   putExtra("current_user", userPubkey)
// }
fun buildSignEventIntent(unsignedJson: String, signerPackage: String, currentUserHex: String): Intent {
    return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:$unsignedJson")).apply {
        `package` = signerPackage
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("type", "sign_event")
        putExtra("current_user", currentUserHex)
    }
}
```

`current_user` is hex per NIP-55. Session already stores hex. Do not send `permissions`. Do not query `content://….SIGN_EVENT`.

Result extras: `rejected` (boolean), `event` (signed JSON), `result` (signature). `resultCode != RESULT_OK` is cancel/crash (Phase 1 already maps that to `Cancelled`).

### Bunker `sign_event`

```kotlin
// Source: https://github.com/nostr-protocol/nips/blob/master/46.md
// method: sign_event
// params: [json_stringified({ content, kind, tags, created_at })]
// result: json_stringified(signed event)
// BunkerClient already: rpcJson(id, method, params), publish(), awaitRpc(), onAuthUrl
fun signEvent(
    relays: List<String>,
    remoteSignerPubkey: String,
    clientPrivkey: ByteArray,
    unsignedJson: String,
): BunkerSignResult
```

Reuse `openSockets` / `answerAuth` / `awaitRpc`. Close sockets in `finally`. Treat `auth_url` like pair. Map reject / timeout like pair. Parse `result` with `Nip01Event.parse`.

Unsigned JSON for bunker: `kind`, `content`, `tags`, `created_at` only (NIP-46 example). For Amber URI, same fields plus `pubkey` = session hex. Omit `id` and `sig`.

### URL normalize (D-04)

```kotlin
// Locked: https, drop www, trailing slash, query, fragment
// Do not copy webapp normalizeUrl (it drops the scheme and lowercases host+path).
fun normalize(url: String): String {
    val raw = url.trim().let { if (it.contains("://")) it else "https://$it" }
    val parsed = java.net.URI(raw)
    val host = (parsed.host ?: "").lowercase().removePrefix("www.")
    val path = parsed.path.orEmpty().trimEnd('/')
    return "https://$host$path"
}
```

`UrlExtractor.normalize` only prepends https. Leave it. New `ArticleUrl` object. Tests: www, http, trailing slash, `?utm`, `#section`, already-clean https.

Match: `normalize(eventR) == normalize(articleUrl)`.

### Quote match (D-05, D-06)

```kotlin
// Source: boris/src/utils/highlightMatching/textMatching.ts
// searchText = highlight.content.trim()
// index = content.indexOf(searchText) in a loop (all occurrences)
// normalizeWhitespace exists in that file but is not used by findHighlightMatches
fun occurrences(haystack: String, quote: String): List<IntRange> {
    val q = quote.trim()
    if (q.isEmpty()) return emptyList()
    val out = mutableListOf<IntRange>()
    var start = 0
    while (true) {
        val i = haystack.indexOf(q, start)
        if (i < 0) break
        out.add(i until i + q.length)
        start = i + q.length
    }
    return out
}
```

Paint every range, including two events with the same quote (two overlapping rects). Title: run the same helper on `content.title`. Body gate: skip body paint when `occurrences(content.body, quote)` is empty. No toast.

### Relay 10002 + 9802

```kotlin
// NIP-65: ["r", url] | ["r", url, "read"|"write"]
// omitted marker = both
// NIP-01 REQ may carry two filters (OR)
// Fallback (D-17): wss://relay.damus.io, wss://nos.lol, wss://relay.primal.net, wss://wot.dergigi.com
```

Keep that fallback list. Do not add nostr.band. Do not use `wss://relay.nsec.app`.

Flow: REQ `{kinds:[10002], authors:[me], limit:10}` on the fallback list. Newest `created_at` wins. Write = write or both. Read = read or both. Empty list => fallback for both. Then REQ `{kinds:[9802], authors:[me], "#r":[openedUrl]}` plus `{kinds:[9802], authors:[me], "#r":[normalized]}` on read relays. Publish EVENT to write relays. Success = at least one `OK` true. Ignore AUTH on content relays (no user key to answer with). Close sockets when EOSE or timeout (~8s query, ~8s publish). Connect timeout can stay 15s.

`Nip01Event` today:

```
const val KIND_RPC = 24133
const val KIND_AUTH = 22242
```

Add `KIND_HIGHLIGHT = 9802` and `KIND_RELAY_LIST = 10002`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Dark Wisp NIP-55 sign stack | Dark Wisp removed remote signer | 2026 (wisp PR 531) | Copy NIP-55, not Dark Wisp `RemoteSigner` |
| Compose custom toolbar = copy `AndroidTextToolbar` | `appendTextContextMenuComponents` (1.9), `SelectionState` (1.12 beta) | 2025-08 / 2026 | Stay on BOM `2025.06.01`; custom `TextToolbar` |
| applesauce `HighlightBlueprint` | `HighlightFactory` in v6 | applesauce v6 | Webapp is still v5 blueprint. Copy that tag layout |
| Lantern selector tags | NIP-84 `r` + `context` + `alt` | locked D-08 | Do not copy Lantern |

**Deprecated/outdated:**
- Dark Wisp as a `sign_event` reference: removed. Use NIP-55.
- MagicalSelection: do not add.
- Remembered Amber permissions / ContentResolver: deferred (D-18).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Compose BOM `2025.06.01` is foundation 1.8.x (no `appendTextContextMenuComponents`) | Standard Stack / Pattern 1 | If the BOM already has 1.9, the custom toolbar still works; the 1.9 API is optional |
| A2 | Amber accepts `current_user` as hex (NIP-55). Older Amber README used npub | Amber intent | If Amber requires npub, send `Nip19.npubEncode(hex)` instead |
| A3 | Public fallback relays accept unauthenticated REQ/EVENT for 10002/9802 | Relay query | Some write relays may drop EVENT; one OK is enough |
| A4 | Marker alpha `0.45f` | Pattern 2 | Visual only. Change the constant |
| A5 | Clipboard copy-then-restore is an acceptable way to read the selection | Pattern 1 | If copy is empty, skip sign and toast. Rare race with another clipboard writer |
| A6 | Webapp `r` is the opened URL, not the scheme-stripped normalize | Pitfall 4 | Dual `#r` filter is the mitigation |

**If this table is empty:** All claims in this research were verified or cited — no user confirmation needed.

A2 is the only one that can break AUTH-06 on device. Planner: if hex fails in UAT, switch to npub. Do not block planning.

## Open Questions

1. **`current_user` hex vs npub**
   - What we know: NIP-55 says hex. Amber's current README only points at NIP-55. An older Amber README used npub.
   - What's unclear: this Amber build's parser.
   - Recommendation: send hex (matches stored session). UAT on device. Fallback npub if Amber rejects.

2. **Publish `r` original vs normalized**
   - What we know: webapp writes the opened URL. D-04/D-16 talk about matching/filtering with the normalized form.
   - What's unclear: none if we publish opened URL and query both.
   - Recommendation: do that. Do not publish only the normalized host.

3. **Toast strings**
   - What we know: Home uses `auth_rejected` = `Amber declined the request.` and `auth_cancelled` = `Amber did not return a key.`
   - What's unclear: highlight-specific copy.
   - Recommendation: `Highlight cancelled.`, `Highlight rejected.`, `Highlight not published.` Short. Same three outcomes for bunker.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | unit tests / compile | ✓ | OpenJDK 17.0.20 | — |
| `./gradlew` | compile + `:app:test` | ✓ | wrapper | — |
| OkHttp / WebSocket | relays + bunker | ✓ | 4.12.0 (deps) | — |
| secp256k1-kmp | verify 9802 | ✓ | 0.22.0 | do not upgrade |
| Amber on device | AUTH-06 Amber path | n/a (device) | — | bunker path |
| adb | on-device UAT | ✗ in this environment | — | human device UAT at phase end |
| Network to fallback relays | 10002 / 9802 | assumed | — | D-02 silent miss |

**Missing dependencies with no fallback:**
- none for compile/test

**Missing dependencies with fallback:**
- adb: JVM tests cover parsers; Highlight toolbar + Amber prompt need a device

Step 2.6: external tools are JDK + Gradle + network. Device signer is a UAT concern, not a plan blocker.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Stored session only. Amber intent or bunker RPC. Never nsec |
| V3 Session Management | yes | Existing `boris_session` + `SecretBox`. No new store. No 10002 cache |
| V4 Access Control | yes | Paint/fetch only `authors = session.pubkeyHex` |
| V5 Input Validation | yes | `Nip01Event.parse` + `verify()`. Quote is plain text, not evaluated as markdown from the relay |
| V6 Cryptography | yes | Existing schnorr verify + NIP-44. Do not sign 9802 locally with a user key |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Forged 9802 from a relay | Spoofing | `verify()` and `pubkey == session` before paint |
| Event JSON / quote injection | Tampering | Compose `Text` only. Do not parse relay content as markdown/HTML |
| Clipboard leak of selection | Information disclosure | Restore prior clipboard after Highlight. Do not log the quote |
| Client privkey in memory | Information disclosure | Unwrap, sign, drop. Never log `SecretBox` material |
| Wrong signer package | Elevation | Set `package` from stored Amber session only |
| Relay URL injection in 10002 | Tampering | Accept only `wss://` URLs when parsing `r` tags (same rule as `Session.fromStoredBunker`) |

## Sources

### Primary (HIGH confidence)
- https://github.com/nostr-protocol/nips/blob/master/84.md — kind 9802, `r`, `context`
- https://github.com/nostr-protocol/nips/blob/master/65.md — kind 10002 read/write
- https://github.com/nostr-protocol/nips/blob/master/55.md — `sign_event` intent extras
- https://github.com/nostr-protocol/nips/blob/master/46.md — `sign_event` params/result, `auth_url`
- applesauce-common@5.1.0 `HighlightBlueprint` + `operations/highlight.js` (jsdelivr)
- In-repo: `ReaderScreen.kt`, `HomeScreen.kt` 166-241, `Color.kt` 22, `Session.kt`, `RemoteSignerBridge.kt`, `BunkerClient.kt`, `RelaySocket.kt`, `Nip01Event.kt` 49-50, `SignerResult.kt`, `AuthViewModel.kt`, `ReadableContent.kt`, `UrlExtractor.kt` 18-27, `libs.versions.toml`
- Webapp: `highlightCreationService.ts`, `textMatching.ts`, `urlHelpers.ts`, `relayListService.ts`
- Lantern: `nostr-highlight-adapter.ts` (kind/UX only), `groups.ts` `getWriteRelays`

### Secondary (MEDIUM confidence)
- https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/TextToolbar
- https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/contextmenu/modifier/appendTextContextMenuComponents.modifier (1.9)
- https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/selection/rememberSelectionState.composable (1.12.0-beta02)
- mikepenz multiplatform-markdown-renderer README (annotator / components)
- Amber README (defers to NIP-55; older README used npub `current_user`)

### Tertiary (LOW confidence)
- classify-confidence seam rated `webfetch` / `websearch` LOW even with `--verified`
- BOM-to-foundation mapping for `2025.06.01` (POM fetch failed)
- Public-relay AUTH behavior

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — versions read from `gradle/libs.versions.toml`; no new packages
- Architecture: HIGH — existing `nostr/` + reader seams read; NIP-84/55/46/65 fetched
- Pitfalls: HIGH for Jina/selection/`package`/`#r`/reconnect; MEDIUM for Amber hex vs npub

**Research date:** 2026-08-14
**Valid until:** 2026-09-13 (30 days; NIPs and in-repo APIs are stable)

## RESEARCH COMPLETE
