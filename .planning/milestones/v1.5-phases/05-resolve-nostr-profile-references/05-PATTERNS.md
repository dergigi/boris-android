# Phase 5: Resolve nostr profile references - Pattern Map

**Mapped:** 2026-08-18
**Files analyzed:** 27
**Analogs found:** 27 / 27

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `app/src/main/java/org/dergigi/boris/nostr/Nip19.kt` | utility | transform | same file: `neventDecode` / `neventEncode` / `normalizePubkey` | exact |
| `app/src/main/java/org/dergigi/boris/data/NostrLink.kt` | model | transform | same file: `NostrTarget.Note` + `decode` `nevent1` branch | exact |
| `app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt` | utility | request-response | same file: `readerLinkAction` | exact |
| `app/src/main/java/org/dergigi/boris/nostr/HintedRelays.kt` | store | file-I/O | `ReadingPositionStore.kt` (JSON map) + `MainActivity` init next to `RelayHealth` | exact |
| `app/src/main/java/org/dergigi/boris/data/NostrMentions.kt` | utility | transform | `Footnotes.kt` (`protectCode` / `expand`) + webapp `replaceNostrUrisSafely` | role-match |
| `app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt` | service | request-response | same file: `fetchProfile` / `fetchProfileRemote` + `fetchContactPubkeysRemote` relay union | exact |
| `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` | component | event-driven | same file: `markdownBody` remember + `UriHandler` | exact |
| `app/src/main/java/org/dergigi/boris/MainActivity.kt` | config | file-I/O | same file: `RelayHealth.init(File(filesDir, "relay_health.json"))` | exact |
| `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` | utility | transform | same file: `extract` / `articleUrl` `NostrLink.parse` return | exact |
| `app/src/main/java/org/dergigi/boris/ui/you/YouViewModel.kt` | store | request-response | same file: `refresh` relay `buildList` | exact |
| `app/src/main/java/org/dergigi/boris/tts/TtsText.kt` | utility | transform | same file: `paragraphs` after `Footnotes.expand` | exact |
| `app/src/test/java/org/dergigi/boris/nostr/Nip19Test.kt` | test | transform | same file: `neventRoundTripsRelaysAndKind` / official npub vectors | exact |
| `app/src/test/java/org/dergigi/boris/data/NostrLinkTest.kt` | test | transform | same file: `parsesSharedNostrNaddr` / `parsesNoteAndNevent` | exact |
| `app/src/test/java/org/dergigi/boris/ui/reader/ReaderLinksTest.kt` | test | request-response | same file: `mailtoAlwaysGoesOutside` | exact |
| `app/src/test/java/org/dergigi/boris/data/UrlExtractorTest.kt` | test | transform | same file: `extractsNaddrFromShareTextAndGateways` | exact |
| `app/src/test/java/org/dergigi/boris/data/NostrMentionsTest.kt` | test | transform | `FootnotesTest.kt` (`doesNotTouchFootnotesInsideCode`) | role-match |
| `app/src/test/java/org/dergigi/boris/nostr/HintedRelaysTest.kt` | test | file-I/O | `OgPreviewCacheTest.kt` (temp JSON file) + `LocalRelaysTest.resolveKeepsCitrineAndPublicWss` | role-match |
| Exhaustive `when (NostrTarget)` sites (10 files, listed below) | model | transform | existing `Article` / `Note` / `null` arms in each file | exact |

SharedPreferences (`SessionStore`, `FeedScopeStore`) is the rejected persistence analog. RESEARCH locked `hinted_relays.json` next to `RelayHealth`.

## Pattern Assignments

### `app/src/main/java/org/dergigi/boris/nostr/Nip19.kt` (utility, transform)

**Analog:** same file, `NeventPointer` + `neventDecode` / `neventEncode`

**Pointer type** (lines 12-17): copy as `NprofilePointer(pubkey, relays)` with no author/kind fields.

```kotlin
data class NeventPointer(
    val eventId: String,
    val relays: List<String> = emptyList(),
    val author: String? = null,
    val kind: Int? = null,
)
```

**Encode pattern** (lines 57-67): type 0 = 32-byte key material, type 1 = UTF-8 relays. For nprofile, type 0 is the pubkey (same as `neventEncode` type 0 is the event id).

```kotlin
fun neventEncode(pointer: NeventPointer): String {
    val payload = buildList {
        add(tlv(0, pointer.eventId.hexToByteArray()))
        for (relay in pointer.relays) {
            add(tlv(1, relay.toByteArray(Charsets.UTF_8)))
        }
        pointer.author?.let { add(tlv(2, it.hexToByteArray())) }
        pointer.kind?.let { add(tlv(3, uint32be(it))) }
    }.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
    return bech32Encode("nevent", payload)
}
```

**Decode pattern** (lines 69-78): copy this block. Change HRP to `"nprofile"`, type 0 to pubkey hex length 64, keep type 1 as relays, drop types 2/3. Extra TLV types are already ignored by `parseTlv`.

```kotlin
fun neventDecode(nevent: String): NeventPointer {
    val (hrp, data) = bech32Decode(nevent)
    require(hrp == "nevent") { "Expected nevent, got $hrp" }
    val fields = parseTlv(data)
    val eventId = fields[0]?.firstOrNull()?.toHex()
    require(eventId != null && eventId.length == 64) { "nevent missing id" }
    val relays = fields[1].orEmpty().map { it.toString(Charsets.UTF_8) }
    val author = fields[2]?.firstOrNull()?.toHex()?.takeIf { it.length == 64 }
    val kind = fields[3]?.firstOrNull()?.let { be32(it) }
    return NeventPointer(eventId, relays, author, kind)
}
```

**TLV parser** (lines 105-117): reuse as-is. Truncated values `break`; unknown types accumulate and callers ignore them.

**normalizePubkey** (lines 132-146): add `trimmed.startsWith("nprofile1", ignoreCase = true) -> nprofileDecode(trimmed).pubkey` in the `when`, still wrapped in `try/catch (_: Exception) { null }`.

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

**Error handling:** `require` inside decode; callers (`NostrLink.decode`) already `catch (_: Exception) { null }`. Do not sanitize relays inside `nprofileDecode`. Sanitize after a successful pubkey decode at the `NostrLink` / `HintedRelays` boundary so a garbage type-1 string cannot drop the pubkey (D-06).

---

### `app/src/main/java/org/dergigi/boris/data/NostrLink.kt` (model, transform)

**Analog:** same file, `NostrTarget.Note` + `decode` `nevent1` branch

**Sealed subtype** (lines 17-26): copy `Note`. Profile carries `pubkeyHex`, original `encoded`, and optional `relays`.

```kotlin
data class Note(
    val eventId: String,
    val encoded: String,
    override val relays: List<String> = emptyList(),
    val author: String? = null,
    val kind: Int? = null,
) : NostrTarget() {
    override val uri get() = "nostr:$encoded"
    override val publicUrl get() = NostrLink.gatewayUrl(encoded)
}
```

**Decode switch** (lines 46-78): add `nprofile1` and `npub1` **before** `else -> null`. Keep `nsec1` on the else path (null). Sanitize nprofile relays with `LocalRelays.resolve`; keep the pubkey even if every hint is dropped.

```kotlin
private fun decode(encoded: String): NostrTarget? {
    return try {
        when {
            encoded.startsWith("naddr1") -> { /* existing */ }
            encoded.startsWith("note1") -> { /* existing */ }
            encoded.startsWith("nevent1") -> { /* existing */ }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
```

**entityRegex** (lines 89-92): same optional `nostr:(?://)?` wrapper and bech32 charset. RESEARCH recommends requiring the prefix for **profile** identifiers in `NostrLink` as well (D-09 body rule plus share-text safety). naddr/note/nevent stay optional-prefix for Home paste.

```kotlin
private val entityRegex = Regex(
    """(?:nostr:(?://)?)?(naddr1[023456789acdefghjklmnpqrstuvwxyz]+|note1[023456789acdefghjklmnpqrstuvwxyz]+|nevent1[023456789acdefghjklmnpqrstuvwxyz]+)""",
    RegexOption.IGNORE_CASE,
)
```

**Parse catch** (lines 76-78): keep `catch (_: Exception) { null }`. Invalid nprofile stays visible plaintext, never a crash.

---

### `app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt` (utility, request-response)

**Analog:** same file, `readerLinkAction` + `openWeblink`

**Sealed action** (lines 5-8): add `data class OpenProfile(val pubkeyHex: String)`.

```kotlin
sealed interface ReaderLinkAction {
    data object Ignore : ReaderLinkAction
    data class OpenInReader(val url: String) : ReaderLinkAction
    data class OpenExternal(val url: String) : ReaderLinkAction
}
```

**Core routing** (lines 11-20): parse `NostrLink` **first**. Profile returns `OpenProfile` before `UrlExtractor.articleUrl`. Article/Note/`null` fall through to today's Ignore / OpenInReader / OpenExternal.

```kotlin
internal fun readerLinkAction(
    uri: String,
    currentUrl: String,
    openInReader: Boolean,
): ReaderLinkAction {
    val article = UrlExtractor.articleUrl(uri, currentUrl)
    if (article != null && article == currentUrl) return ReaderLinkAction.Ignore
    if (openInReader && article != null) return ReaderLinkAction.OpenInReader(article)
    return ReaderLinkAction.OpenExternal(article ?: uri)
}
```

**openWeblink** (lines 22-32): exhaustive `when`. Settings weblinks are http(s). Add `is ReaderLinkAction.OpenProfile -> Unit` (or ignore) so compilation stays exhaustive; profile taps go through `ReaderScreen` `UriHandler`, not this helper.

On `OpenProfile`, persist hints from the parsed `NostrTarget.Profile.relays` (D-03 custom labels never hit rewrite).

---

### `app/src/main/java/org/dergigi/boris/nostr/HintedRelays.kt` (store, file-I/O)

**Analog:** `ReadingPositionStore.kt` for the JSON keyed map; `MainActivity.kt` + `RelayHealth.kt` for init location and filename style. **Not** `SessionStore` SharedPreferences.

**Init + load** (`ReadingPositionStore.kt` lines 22-40):

```kotlin
fun init(target: File) {
    synchronized(lock) {
        file = target
        positions.clear()
        if (target.exists()) {
            runCatching {
                val obj = JSONObject(target.readText())
                for (key in obj.keys()) {
                    // ...
                }
            }
        }
    }
}
```

**Persist** (`ReadingPositionStore.kt` lines 88-102): sync `JSONObject` + `file.writeText` inside `runCatching`. Cap entries (`MAX_ENTRIES` analog). HintedRelays keys are lowercase pubkey hex; values are JSON arrays of sanitized relay URLs.

```kotlin
private fun put(key: String, entry: Entry) {
    positions.remove(key)
    positions[key] = entry
    while (positions.size > MAX_ENTRIES) {
        positions.remove(positions.keys.first())
    }
    val target = file ?: return
    runCatching {
        val obj = JSONObject()
        positions.forEach { (k, v) ->
            obj.put(k, JSONObject().put("f", v.fraction.toDouble()).put("t", v.updatedAt))
        }
        target.writeText(obj.toString())
    }
}
```

**Relay URL hygiene** (`LocalRelays.kt` line 40): `fun resolve(url: String): String? = canonical(url) ?: Nip66.normalize(url)`. `Nip66.normalize` (lines 14-28) returns null for non-`wss://`. Drop bad hints; never throw.

**Cap analog:** `OutboxRouter.MIN_REDUNDANCY` is 3 and is for outbox placement, not hint storage. RESEARCH picked 8 extras per pubkey. Use a `HintedRelays` constant (`MAX_HINTS = 8`), `distinct()`, union on `remember`.

**Init call site** (`MainActivity.kt` lines 57-69):

```kotlin
EventCache.init(File(filesDir, "event_cache"))
OfflineStore.init(File(filesDir, "offline_downloads.json"))
RelayHealth.init(File(filesDir, "relay_health.json"))
ReadingPositionStore.init(File(filesDir, "reading_positions.json"))
```

Add `HintedRelays.init(File(filesDir, "hinted_relays.json"))` next to `RelayHealth.init`.

`RelayHealth` debounce/executor (lines 127-155) is heavier than this map needs. Copy `ReadingPositionStore`'s synchronous write.

---

### `app/src/main/java/org/dergigi/boris/data/NostrMentions.kt` (utility, transform)

**Analogs:** `Footnotes.expand` / private `protectCode` (Android pipeline); webapp `replaceNostrUrisSafely` (skip `](url)` destinations). No existing Android mention rewriter.

**Pipeline placement** (`Footnotes.kt` lines 10-12, `ReaderScreen.kt` line 1068): `object` with `fun rewrite(markdown: String): String`. Call after `Footnotes.expand`, before `rememberMarkdownState`. Do not mutate `ReadableContent.body`.

```kotlin
fun expand(markdown: String): String {
    if (!markdown.contains("[^")) return markdown
    val (protected, restore) = protectCode(markdown)
    // ...
}
```

**protectCode** (`Footnotes.kt` lines 79-94): stash fenced then inline code, rewrite, restore. `protectCode` is **private**. Copy the stash/restore (same `\u0000$i\u0000` tokens and `FENCE` / `INLINE_CODE` regexes) into `NostrMentions` rather than widening Footnotes unless the planner prefers extracting a shared helper. Extracting is the DRY move; copying is acceptable because Footnotes already owns footnote-specific regexes.

```kotlin
private fun protectCode(text: String): Pair<String, (String) -> String> {
    val slots = mutableListOf<String>()
    fun stash(match: MatchResult): String {
        slots += match.value
        return "\u0000${slots.lastIndex}\u0000"
    }
    val fenced = FENCE.replace(text, ::stash)
    val protected = INLINE_CODE.replace(fenced, ::stash)
    return protected to { restored ->
        var next = restored
        slots.indices.reversed().forEach { i ->
            next = next.replace("\u0000$i\u0000", slots[i])
        }
        next
    }
}
```

**Skip destinations** (webapp `nostrUriResolver.tsx` lines 164-241): walk `](` then matching `)` with depth, skip matches whose range sits inside those URL spans. Android must **not** copy `nostrLinkPattern` (optional prefix + `nsec1`). Prefix-required regex from RESEARCH:

```kotlin
val PROFILE_MENTION = Regex(
    """(?<![/\w])(?:nostr:(?://)?)(nprofile1[023456789acdefghjklmnpqrstuvwxyz]+|npub1[023456789acdefghjklmnpqrstuvwxyz]+)""",
    RegexOption.IGNORE_CASE,
)
```

Charset matches `NostrLink.entityRegex` (lines 89-92).

**Label** (`Profile.kt` lines 9-21): `"@" + Profile.displayName(pubkeyHex, cached)`. Cached kind 0 via `EventCache.latest(Nip01Event.KIND_METADATA, pubkey)?.let { Profile.parse(it.content) }`. Android `shortNpub` (`npub.take(12) + "…"`), not the webapp `npub.slice(5, 12)` fallback.

```kotlin
fun displayName(pubkeyHex: String, profile: Profile?): String {
    profile?.name?.takeIf { it.isNotBlank() }?.let { return it }
    return shortNpub(pubkeyHex)
}
```

**Href:** keep `nostr:$encoded` (nprofile stays nprofile so hints survive D-03 taps). Webapp rewrites to `/p/{npub}`; Android does not.

**Invalid decode:** `return@replace match.value`. No crash.

**Webapp mention chrome to skip:** `NostrMentionLink.tsx` lines 49-77 loading class and `/p/{npub}` href. D-02 / discretion: text only, short npub, no spinner.

---

### `app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt` (service, request-response)

**Analog:** same file `fetchProfile` / `fetchProfileRemote` plus the relay union in `fetchContactPubkeysRemote`

**Stale-while-revalidate** (lines 67-74): keep. Add `extraRelays: List<String> = emptyList()` and pass it into remote.

```kotlin
fun fetchProfile(pubkeyHex: String): Profile? {
    val cached = EventCache.latest(Nip01Event.KIND_METADATA, pubkeyHex)
    if (cached != null) {
        refreshOnce("profile:${pubkeyHex.lowercase()}") { fetchProfileRemote(pubkeyHex) }
        return Profile.parse(cached.content)
    }
    return fetchProfileRemote(pubkeyHex)?.let { Profile.parse(it.content) }
}
```

**Remote query** (lines 76-90): today's `relays = fetchRelayList(pubkeyHex).read`. Union extras + remembered hints + NIP-65 read (FALLBACK already sits inside `fetchRelayList` / `LocalRelays.withLocal`).

**Union pattern** (lines 138-142):

```kotlin
val relays = buildList {
    addAll(RelayList.FALLBACK)
    addAll(fetchRelayList(pubkeyHex).read)
}.distinct()
```

Prescribed: `(extraRelays + HintedRelays.forPubkey(pubkeyHex) + fetchRelayList(pubkeyHex).read).distinct()`. Do not fork a second kind-0 path.

Existing `fetchProfile(pubkeyHex)` call sites (`YouViewModel`, `ReaderViewModel`, `AuthViewModel`, `HighlightedArticles`) keep working via the default empty `extraRelays`. Hints still apply through `HintedRelays.forPubkey` inside remote.

---

### `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` (component, event-driven)

**Analog:** same file, `UriHandler` + `markdownBody`

**Tap handler** (lines 775-787): add `onOpenProfile` to `remember` keys. Handle `OpenProfile` with `onOpenProfile(action.pubkeyHex)`.

```kotlin
val uriHandler = remember(content.url, onOpenArticle, defaultUriHandler, openLinksInReader) {
    object : UriHandler {
        override fun openUri(uri: String) {
            when (val action = readerLinkAction(uri, content.url, openLinksInReader)) {
                ReaderLinkAction.Ignore -> Unit
                is ReaderLinkAction.OpenInReader -> onOpenArticle(action.url)
                is ReaderLinkAction.OpenExternal -> defaultUriHandler.openUri(action.url)
            }
        }
    }
}
```

**Navigation already wired** (`BorisApp.kt` lines 72, 321-324): login-free.

```kotlin
fun profile(npub: String): String = "profile/$npub"
// ...
onOpenProfile = { pubkeyHex ->
    runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
        navController.navigate(Routes.profile(npub))
    }
},
```

**Markdown remember** (lines 1062-1074): one rewrite at first paint. Keys stay `content.body` so a later kind-0 arrival does not remount.

```kotlin
val markdownBody = remember(content.body) { Footnotes.expand(content.body) }
```

Becomes `Footnotes.expand` then `NostrMentions.rewrite` inside the same `remember(content.body)`.

---

### `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` (utility, transform)

**Analog:** same file lines 10-17 and 49-52

Today any successful `NostrLink.parse` becomes an article URL. After Profile exists, that would send `nostr:npub…` into the reader / Jina.

```kotlin
fun extract(text: String?): String? {
    if (text.isNullOrBlank()) return null
    val trimmed = text.trim()
    NostrLink.parse(trimmed)?.uri?.let { return it }
    // ...
}

fun articleUrl(href: String?, baseUrl: String? = null): String? {
    if (href.isNullOrBlank()) return null
    val trimmed = href.trim()
    NostrLink.parse(trimmed)?.uri?.let { return it }
    // ...
}
```

Change both to return a URI only for `Article` and `Note`. `is NostrTarget.Profile ->` skips (`null` / continue). Same skip in the later `NostrLink.parse(absolute)?.uri` inside `articleUrl`.

`alreadyLinked` (lines 126-130) is the skip-if-already-in-`(` analog for image autolink. Mention rewrite should use the webapp `](` range walker, not this one-char check: a custom label `[Gigi](nostr:nprofile1…)` needs the whole destination skipped.

---

### `app/src/main/java/org/dergigi/boris/ui/you/YouViewModel.kt` (store, request-response)

**Analog:** same file lines 165-172

```kotlin
val list = RelayQuery.fetchRelayList(key)
val relays = buildList {
    addAll(RelayList.FALLBACK)
    addAll(list.write)
    addAll(list.read)
}.distinct()
```

Add `addAll(HintedRelays.forPubkey(key))` so later profile-screen visits use remembered hints (D-08). `fetchProfile(key)` at line 228 already goes through `RelayQuery`; extraRelays default is enough there if `fetchProfileRemote` reads `HintedRelays`.

---

### `app/src/main/java/org/dergigi/boris/tts/TtsText.kt` (utility, transform)

**Analog:** same file lines 8-16 + `TtsTextTest.paragraphsFlattenLinksToLabels` (lines 50-56)

```kotlin
fun paragraphs(content: ReadableContent): List<String> {
    val out = mutableListOf<String>()
    content.title?.let { addCleaned(out, it) }
    content.summary?.let { addCleaned(out, it) }
    for (block in splitMarkdownBlocks(Footnotes.expand(content.body))) {
        addCleaned(out, block)
    }
    return out
}
```

Run `NostrMentions.rewrite` after `Footnotes.expand`. `MarkdownInline.plain` already speaks markdown link labels, so `@name` is spoken instead of the bech32 blob once the rewrite exists.

---

### Exhaustive `when (NostrTarget)` sites (model, transform)

**Analog:** existing `Article` / `Note` / `null` arms in each file. Adding `Profile` is a compile error until every `when` is updated.

| File | Current pattern | Profile arm |
|------|-----------------|-------------|
| `ReaderRepository.kt:18-21` | `Article` fetch, `Note` fetch, `null` Jina | skip / never fetch as article |
| `ReadingPositionStore.kt:48-51` | coordinate / eventId / normalize | skip (`url` unchanged or unused) |
| `ArticleUrl.kt:5-8` | identifier / `"nostr"` / URI host | skip (null host, like non-article) |
| `ContinueReading.kt:16-19` | identifier / `"nostr"` / host | skip |
| `Nip85.kt:24-26` | coordinate / `Note -> null` | `null` |
| `ArchivedArticles.kt:21-22` | `a:` / `e:` keys | skip (`null`) |
| `HighlightedArticles.kt` (several) | decorate article/note | skip |
| `RandomArticles.kt:27-29` | identifier / `"nostr"` | skip |
| `ArticlePreview.kt:31-37` | article/note keys | skip |
| `OfflineDownloader.kt:122-131` | fetch article/note | skip |

Copy the skip style from `Nip85.kt` (`is NostrTarget.Note -> null`): Profile is not an article identity.

---

### Tests

**`Nip19Test.kt` analog:** official vector + round-trip. `neventRoundTripsRelaysAndKind` (lines 92-105) is the nprofile template. `roundTripsOfficialNpubVector` (lines 7-13) is the style for the NIP-19 nprofile vector (`nprofile1qqsrhuxx8…`, pubkey `3bf0c63f…`, two relays). Add: unknown TLV ignored; type-1 garbage still returns pubkey; `normalizePubkey(nprofile)`; `nprofileDecode("npub1…")` throws; nsec HRP rejected.

**`NostrLinkTest.kt` analog:** `parsesSharedNostrNaddr` / `parsesNostrUriWithSlashes` / `parsesNoteAndNevent` (lines 12-41). Add `nostr:` and `nostr://` nprofile/npub → `NostrTarget.Profile`; naddr/note/nevent unchanged; `nostr:nsec1…` → null. Issue #5 nprofile fixture belongs here.

**`ReaderLinksTest.kt` analog:** `mailtoAlwaysGoesOutside` (lines 46-54). Add nprofile/npub → `OpenProfile(hex)`; naddr still `OpenInReader` when `openInReader=true`; nsec URI is not `OpenProfile`.

**`UrlExtractorTest.kt` analog:** `extractsNaddrFromShareTextAndGateways` (lines 170-180) stays green. Add `articleUrl("nostr:nprofile…")` null; `extract("nostr:npub1…")` null.

**`NostrMentionsTest.kt` analog:** `FootnotesTest.doesNotTouchFootnotesInsideCode` (lines 66-85). Cases: raw `nostr:nprofile` → `[@` + `Profile.displayName` + `](nostr:…)`; `[Gigi](nostr:nprofile1…)` unchanged; bare `nprofile1` / `npub1` unchanged; `nostr:nsec1` unchanged; `nostr:naddr1` unchanged; fenced/inline code unchanged.

**`HintedRelaysTest.kt` analog:** `OgPreviewCacheTest` temp file (lines 10-16) + `LocalRelaysTest.resolveKeepsCitrineAndPublicWss` (lines 27-31). Give `HintedRelays` an `internal fun clear()` like `OgPreviewCache.clear` so tests do not leak singleton state. Cases: union merge; bad URL dropped; cap; remember then `forPubkey`; reload from the same `File`.

**JUnit style:** JUnit 4, `{Type}Test.kt`, `org.junit.Assert.*`, no Robolectric. Filename `{Type}Test.kt` under `app/src/test/java/org/dergigi/boris/`.

## Shared Patterns

### Parse failures return null
**Source:** `NostrLink.decode` lines 76-78; `Nip19.normalizePubkey` lines 143-145; `UrlExtractor.articleUrl` lines 63-64
**Apply to:** `nprofileDecode` callers, `NostrMentions.rewrite`, `readerLinkAction`
```kotlin
} catch (_: Exception) {
    null
}
```

### Relay URL sanitize
**Source:** `LocalRelays.resolve` line 40; `Nip66.normalize` lines 14-28; `LocalRelaysTest` lines 27-31
**Apply to:** nprofile type-1 hints at remember time, not inside `nprofileDecode`
```kotlin
fun resolve(url: String): String? = canonical(url) ?: Nip66.normalize(url)
```

### JSON file store, no Room / no prefs
**Source:** `ReadingPositionStore.init` + `put`; `MainActivity` `RelayHealth.init(File(filesDir, "relay_health.json"))`
**Apply to:** `HintedRelays`
Rejected analog: `SessionStore` SharedPreferences (`data/SessionStore.kt` lines 18-35).

### Display name fallback
**Source:** `Profile.displayName` / `shortNpub` lines 9-21
**Apply to:** mention labels (D-01). Do not copy webapp `getProfileDisplayName` nip05 branch or `slice(5, 12)`.

### Login-free navigation
**Source:** `BorisApp.kt` `onOpenProfile` lines 321-324; `Routes.profile` line 72
**Apply to:** `OpenProfile` taps. No session check (READ-01).

### Markdown state must not remount
**Source:** `ReaderScreen.kt` lines 1062-1068
**Apply to:** rewrite keys on `content.body` only. Cached kind 0 name if present at first paint. No spinner in the body.

### Layering
**Source:** `.planning/codebase/CONVENTIONS.md`
UI does not import into `data`; `data` does not import `ui`. `NostrMentions` in `data/` may call `nostr/` (`Profile`, `EventCache`, `HintedRelays`, `Nip19`). `HintedRelays` stays in `nostr/` next to `RelayHealth`.

### Naming / style
**Source:** `.planning/codebase/CONVENTIONS.md`
- `object` for the new stores/utilities (`HintedRelays`, `NostrMentions`)
- `data class` for `NprofilePointer` / `NostrTarget.Profile`
- `internal` on `readerLinkAction` (already)
- No KDoc, no `TODO`, no `Log`
- Tests: `{Type}Test.kt`

## No Analog Found

None. `NostrMentions.rewrite` is the only new behavior; it is a composition of `Footnotes.protectCode` and webapp `replaceNostrUrisSafely`, not a green-field design.

## Metadata

**Analog search scope:** `app/src/main/java/org/dergigi/boris/{nostr,data,ui,tts}`, matching tests, `MainActivity.kt`, webapp `boris/src/components/NostrMentionLink.tsx` and `boris/src/utils/nostrUriResolver.tsx`
**Files scanned:** 40+ Kotlin sources and tests plus 2 webapp files
**Pattern extraction date:** 2026-08-18
