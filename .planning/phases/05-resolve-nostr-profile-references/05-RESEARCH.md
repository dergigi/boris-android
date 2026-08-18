# Phase 5: Resolve nostr profile references - Research

**Researched:** 2026-08-18
**Domain:** Android reader markdown + NIP-19 nprofile/npub routing
**Confidence:** HIGH (in-repo seams); MEDIUM (NIP-19 spec + GFM autolink citations)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Link label
- **D-01:** Raw `nostr:nprofile` / `nostr:npub` mentions render as `@name`. Until kind 0 arrives, show a short npub (same fallback `Profile.displayName` already uses). — **Reversibility:** reversible
- **D-02:** Text only. No avatar in the mention. Fits the reading line like the webapp `NostrMentionLink`.
- **D-03:** If the author already wrote a Markdown link with a custom label (`[Gigi](nostr:nprofile1…)`), keep `Gigi`. Only raw prefixed mentions become `@name`.

### Identifier coverage
- **D-04:** Resolve both `nprofile` and `npub`. Both tap to the in-app profile for that pubkey (`Routes.profile(npub)`).
- **D-05:** Do not treat `nsec`, `note`, `nevent`, or `naddr` as profile mentions. Those keep today's reader routing.

### Relay hints
- **D-06:** Decode `nprofile` TLV relay hints. A bad or extra hint must not drop the pubkey. — **Reversibility:** reversible
- **D-07:** When fetching kind 0 for that pubkey, query hinted relays **plus** the relays Boris already uses for profiles (`RelayQuery.fetchProfile` / NIP-65 / fallback).
- **D-08:** Remember those hinted relays for that pubkey so later visits (profile screen, later articles) still use them. — **Reversibility:** costly — needs a per-pubkey relay store that other profile fetches will read.

### Bare vs prefixed
- **D-09:** Only `nostr:` and the existing `nostr://` variant. Bare `nprofile1…` / `npub1…` in the body stay plaintext.

### Claude's Discretion
- How to rewrite raw mentions inside the markdown pipeline (pre-process body vs markdown link annotator vs `LocalUriHandler`).
- Persistence for D-08 (SharedPreferences vs a small store next to EventCache). Merge strategy with NIP-65 lists.
- `nprofileDecode` shape: mirror `neventDecode` TLV (type 0 pubkey, type 1 relay).
- Whether TTS speaks `@name` instead of the bech32 blob after the rewrite (nice if cheap; not a must-have).
- Loading: short npub is enough; no spinner in the article body.
- Invalid / un-decodable `nostr:nprofile` stays visible as truncated text, not a crash.

### Deferred Ideas (OUT OF SCOPE)
- Avatars next to mentions in the article body
- Auto-linking bare `npub1` / `nprofile1` without a `nostr:` prefix
- Treating `note` / `nevent` / `naddr` mentions as profile links

None of these were requested as this phase.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| READ-03 | `nostr:nprofile` references in article Markdown render as profile links that open the in-app profile screen. Existing note, event, and article nostr links still work. Login is not required. | Decode nprofile TLV; rewrite raw prefixed mentions to `[@name](nostr:…)`; `readerLinkAction` OpenProfile → `Routes.profile(npub)`; hinted relays unioned into `RelayQuery.fetchProfile`; persist per-pubkey hints. |
| READ-01 | User can paste, share, or open a URL and read the article while logged out. Login UI sits on Home; it does not replace or block reading. | Profile rewrite and routing must not gate on session. `UrlExtractor.articleUrl` / `extract` must ignore profile URIs so a mention never becomes an article fetch. Sealed `NostrTarget.Profile` must not fall through `ReaderRepository.fetch`. |
</phase_requirements>

## Summary

Phase 5 is a reader display + routing change, not a new social surface. Raw `nostr:nprofile` / `nostr:npub` in article markdown is plaintext today because `GFMFlavourDescriptor` does not autolink the `nostr:` scheme and `NostrLink.entityRegex` only matches `naddr1` / `note1` / `nevent1`. The webapp already rewrites those URIs to `[@name](/p/{npub})` while skipping identifiers that sit inside a markdown link destination. Android should do the same rewrite, keep the `nostr:` href, and send taps to the existing profile route.

`nprofileDecode` is a copy of `Nip19.neventDecode` with type 0 as a 32-byte pubkey instead of an event id. Relay hints are optional type 1 strings. A bad hint is dropped; the pubkey stays. Fetch kind 0 on hinted relays plus the relays `fetchProfile` already uses (NIP-65 read list, which falls back to `RelayList.FALLBACK`). Remember hints in a tiny JSON file next to `EventCache`, not Room and not a fake kind 10002 event.

**Primary recommendation:** Pre-process markdown (webapp `replaceNostrUrisSafely` shape, prefix required), add `NostrTarget.Profile`, route it in `readerLinkAction` before `UrlExtractor.articleUrl`, and union remembered hints into `RelayQuery.fetchProfile` / `YouViewModel.refresh`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| nprofile / npub decode | API / Backend (domain `nostr/`) | — | Bech32 + TLV already live in `Nip19`; UI must not reimplement. |
| Raw mention → markdown link | Browser / Client (reader) | — | Display transform on article body before `Markdown` parse. |
| Tap routing to profile | Browser / Client | Navigation / shell | `readerLinkAction` + `Routes.profile(npub)`. |
| Kind 0 fetch | API / Backend (`RelayQuery`) | Database / Storage (`EventCache`) | Existing stale-while-revalidate profile fetch. |
| Relay-hint persist (D-08) | Database / Storage | API / Backend | Per-pubkey extra relays, read on every later `fetchProfile`. |
| Profile screen | Browser / Client | — | Already shipped (`ProfileScreen` + `YouViewModel.refresh(hex)`). |

## Project Constraints (from .cursor/rules/)

- Release checklist in `release-zapstore.mdc` applies only when cutting a release. This phase does not ship a version. Do not bump `versionCode` / `versionName` or publish Zapstore as part of Phase 5.

From `.planning/codebase/CONVENTIONS.md` and `ARCHITECTURE.md` (must honor):

- New Kotlin under `org.dergigi.boris` only. Do not edit `com.readwithboris`.
- No Hilt/Koin, no Room, no new DI.
- JVM JUnit tests under `app/src/test/java/org/dergigi/boris/`. Filename `{Type}Test.kt`.
- No `Log` / Timber. No KDoc. No TODO comments in Kotlin.
- UI does not import into data; data does not import UI.
- `runCatching` / `null` for parse failures. Do not crash the reader on a bad `nprofile`.
- `minSdk` 26. Cleartext traffic is off; relay URLs stay `wss://` (local `ws://` only via `LocalRelays`).

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Existing `Nip19` | in-repo | bech32 + `parseTlv` | Already round-trips npub / naddr / nevent. [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/Nip19.kt:69-78] `fun neventDecode(nevent: String): NeventPointer` |
| Existing `Profile` | in-repo | `@name` fallback | [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/Profile.kt:9-12] `fun displayName(pubkeyHex: String, profile: Profile?): String` |
| Existing `RelayQuery` | in-repo | kind 0 fetch | [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt:67-74] `fun fetchProfile(pubkeyHex: String): Profile?` |
| Existing `Routes` | in-repo | in-app profile | [VERIFIED: app/src/main/java/org/dergigi/boris/ui/BorisApp.kt:72] `fun profile(npub: String): String = "profile/$npub"` |
| multiplatform-markdown-renderer | 0.35.0 | Compose markdown | [VERIFIED: gradle/libs.versions.toml:12] `markdown = "0.35.0"` |
| JUnit | 4.13.2 | JVM tests | [VERIFIED: gradle/libs.versions.toml:13] `junit = "4.13.2"` |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `GFMFlavourDescriptor` | bundled via mikepenz | Parse article markdown | Already used in `ReaderScreen`. Do not switch flavour. |
| `EventCache` | in-repo | Cached kind 0 for labels | `EventCache.latest(Nip01Event.KIND_METADATA, pubkey)` — [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/Nip01Event.kt:58] `const val KIND_METADATA = 0` |
| `Nip66.normalize` / `LocalRelays.resolve` | in-repo | Sanitize relay hints | Drop non-`wss://` (except local Citrine). |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Markdown pre-process | Custom mikepenz annotator | Annotator only sees AST text nodes; harder to skip `](url)` ranges; not JVM-easy. Rejected. |
| Markdown pre-process | `LocalUriHandler` only | Handler never fires: raw `nostr:` is not a markdown link. Required for custom-label taps, insufficient for raw mentions. |
| JSON `HintedRelays` file | SharedPreferences | Prefs match `SessionStore`, but this is a growing pubkey→relays map. File store matches `RelayHealth` / `ReadingPositionStore` and is unit-testable with a temp `File`. |
| JSON `HintedRelays` file | Fake kind 10002 in `EventCache` | Would pollute NIP-65 parse. Hints are extras, not an author relay list. |
| Keep nprofile in href | Rewrite href to `/p/{npub}` like webapp | Webapp has React routes. Android already keys profile by npub on `Routes.profile`. Keep `nostr:` href so `NostrLink.parse` works and nprofile hints survive D-03 custom labels. |

**Installation:** none. Do not add packages.

**Version verification:** stack is already in the repo (`gradle/libs.versions.toml`). No registry install this phase.

## Package Legitimacy Audit

No new packages. Existing JUnit and mikepenz stay as-is.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| *(none)* | — | — | — | — | — | No install |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
article markdown (ReadableContent.body)
        │
        ▼
 Footnotes.expand  ──►  NostrMentions.rewrite (prefix required)
        │                      │
        │                      ├─ skip ](url) destinations          [D-03]
        │                      ├─ skip code fences / inline code
        │                      ├─ skip nsec / note / nevent / naddr [D-05]
        │                      ├─ skip bare nprofile1 / npub1       [D-09]
        │                      ├─ label = "@" + Profile.displayName
        │                      └─ HintedRelays.remember(pubkey, relays)
        ▼
 GFMFlavourDescriptor + Markdown  ──►  tappable [@name](nostr:…)
        │
        ▼ tap
 LocalUriHandler.openUri
        │
        ▼
 readerLinkAction
        │
        ├─ NostrTarget.Profile  → OpenProfile(pubkeyHex)
        │                              │
        │                              ▼
        │                         Nip19.npubEncode → Routes.profile(npub)
        │                              │
        │                              ▼
        │                         ProfileScreen / YouViewModel.refresh(hex)
        │                              │
        │                              ▼
        │                         RelayQuery.fetchProfile(hex)
        │                              extra = HintedRelays ∪ NIP-65 read ∪ FALLBACK
        │
        ├─ NostrTarget.Article / Note → existing OpenInReader / OpenExternal
        └─ http(s) / mailto           → existing path (UrlExtractor.articleUrl)
```

### Recommended Project Structure

```
app/src/main/java/org/dergigi/boris/
├── nostr/
│   ├── Nip19.kt              # nprofileDecode / nprofileEncode / normalizePubkey
│   ├── HintedRelays.kt       # NEW: per-pubkey extra relays (JSON file)
│   └── RelayQuery.kt         # fetchProfile(..., extraRelays)
├── data/
│   ├── NostrLink.kt          # NostrTarget.Profile + entityRegex
│   └── NostrMentions.kt      # NEW: rewrite raw prefixed mentions
└── ui/reader/
    ├── ReaderLinks.kt        # OpenProfile branch before articleUrl
    └── ReaderScreen.kt       # rewrite in markdownBody; handle OpenProfile
app/src/test/java/org/dergigi/boris/
├── nostr/Nip19Test.kt
├── nostr/HintedRelaysTest.kt
├── data/NostrLinkTest.kt
├── data/NostrMentionsTest.kt
├── data/UrlExtractorTest.kt
└── ui/reader/ReaderLinksTest.kt
```

### Pattern 1: nprofileDecode mirrors neventDecode

**What:** Same `bech32Decode` + `parseTlv`. Type 0 is 32-byte pubkey. Type 1 is UTF-8 relay URLs (repeatable). Extra types ignored.

**When to use:** Any `nprofile1…` identifier, with or without `nostr:` prefix, after stripping the scheme.

**NIP-19 contract** [CITED: https://github.com/nostr-protocol/nips/blob/master/19.md]:

- type `0` special for `nprofile`: 32 bytes of the profile public key
- type `1` relay: optional ASCII relay URL, may be included multiple times
- "TLVs that are not recognized or supported should be ignored, rather than causing an error."

Official vector [CITED: https://github.com/nostr-protocol/nips/blob/master/19.md]:

- `nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p`
- pubkey: `3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d`
- relay: `wss://r.x.com`
- relay: `wss://djbas.sadkb.com`

Existing analog [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/Nip19.kt:69-78]:

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

Prescribed `nprofileDecode`:

```kotlin
data class NprofilePointer(
    val pubkey: String,
    val relays: List<String> = emptyList(),
)

fun nprofileDecode(nprofile: String): NprofilePointer {
    val (hrp, data) = bech32Decode(nprofile)
    require(hrp == "nprofile") { "Expected nprofile, got $hrp" }
    val fields = parseTlv(data)
    val pubkey = fields[0]?.firstOrNull()?.toHex()
    require(pubkey != null && pubkey.length == 64) { "nprofile missing pubkey" }
    val relays = fields[1].orEmpty().map { it.toString(Charsets.UTF_8) }
    return NprofilePointer(pubkey, relays)
}
```

Sanitize relays **after** a successful pubkey decode (D-06): `relays.mapNotNull { LocalRelays.resolve(it) }`. Empty or garbage type-1 values must not throw. Missing / short type 0 **does** fail (no profile without a pubkey). Catch at `NostrLink.decode` like today's `catch (_: Exception) { null }`.

Also extend `normalizePubkey` so it accepts `nprofile1…` (CONTEXT notes it does not today). [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/Nip19.kt:137] `trimmed.startsWith("npub1", ignoreCase = true) -> npubDecode(trimmed)`

Add `nprofileEncode` for round-trip tests, mirroring `neventEncode` (type 0 pubkey bytes, type 1 relay UTF-8).

### Pattern 2: Rewrite raw prefixed mentions, keep custom labels

**What:** One pure function `NostrMentions.rewrite(markdown): String` that turns raw `nostr:nprofile1…` / `nostr:npub1…` / `nostr://…` into `[label](nostr:…)`.

**When to use:** Once per article paint, after `Footnotes.expand`, before `rememberMarkdownState`. Also from `TtsText.paragraphs` so speech gets `@name` for free.

**Do not** mutate `ReadableContent.body`. Highlight matching uses the stored body. Rewrite is a display/TTS view.

Webapp contract to copy (skip destinations, not labels) from `replaceNostrUrisSafely` in `/Users/gigi/Development/vibe/boris/src/utils/nostrUriResolver.tsx`: walk `](` … matching `)`, skip matches whose span sits inside those URL ranges. Android **must not** copy the webapp regex that also matches bare ids and `nsec1`:

[VERIFIED: /Users/gigi/Development/vibe/boris/src/utils/nostrPatterns.ts:5] `export const nostrLinkPattern = /\b(?:nostr:)?((?:npub1|note1|nevent1|nprofile1|naddr1|nsec1|nrelay1)[a-z0-9]+)\b/gi`

Android rewrite regex (prefix **required**, nsec **excluded**):

```kotlin
val PROFILE_MENTION = Regex(
    """(?<![/\w])(?:nostr:(?://)?)(nprofile1[023456789acdefghjklmnpqrstuvwxyz]+|npub1[023456789acdefghjklmnpqrstuvwxyz]+)""",
    RegexOption.IGNORE_CASE,
)
```

Bech32 charset is the same set already used in `NostrLink.entityRegex`. [VERIFIED: app/src/main/java/org/dergigi/boris/data/NostrLink.kt:89-92]

```kotlin
private val entityRegex = Regex(
    """(?:nostr:(?://)?)?(naddr1[023456789acdefghjklmnpqrstuvwxyz]+|note1[023456789acdefghjklmnpqrstuvwxyz]+|nevent1[023456789acdefghjklmnpqrstuvwxyz]+)""",
    RegexOption.IGNORE_CASE,
)
```

Label: `"@" + Profile.displayName(pubkeyHex, cached)` where `cached` is `EventCache.latest(KIND_METADATA, pubkey)?.let { Profile.parse(it.content) }`. No spinner. Do not re-parse markdown when kind 0 later arrives (`rememberMarkdownState` remount kills selection and scroll). [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:1062-1064]

```kotlin
// The markdown parse state must survive recomposition. Fresh parser inputs would
// re-parse asynchronously, collapse the article to an empty box, clamp the scroll
// to zero, and remount every paragraph, killing the active selection.
```

Call site [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:1068] `val markdownBody = remember(content.body) { Footnotes.expand(content.body) }` becomes `Footnotes.expand` then `NostrMentions.rewrite`.

Protect fenced / inline code the way `Footnotes.protectCode` already does (stash, rewrite, restore).

Invalid decode: leave the original `nostr:nprofile…` text. Do not crash. Truncation-as-span is the webapp fallback; Android's equivalent is "still visible as plaintext".

### Pattern 3: readerLinkAction profile branch before articleUrl

**What:** Parse `NostrLink` first. Profile → `OpenProfile(pubkeyHex)`. Article/Note keep today's path. Then `UrlExtractor.articleUrl` for http(s).

Today [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt:5-20]:

```kotlin
sealed interface ReaderLinkAction {
    data object Ignore : ReaderLinkAction
    data class OpenInReader(val url: String) : ReaderLinkAction
    data class OpenExternal(val url: String) : ReaderLinkAction
}

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

Prescribed:

```kotlin
data class OpenProfile(val pubkeyHex: String) : ReaderLinkAction

internal fun readerLinkAction(...): ReaderLinkAction {
    when (val target = NostrLink.parse(uri)) {
        is NostrTarget.Profile -> return ReaderLinkAction.OpenProfile(target.pubkeyHex)
        is NostrTarget.Article, is NostrTarget.Note, null -> Unit
    }
    val article = UrlExtractor.articleUrl(uri, currentUrl)
    // existing Ignore / OpenInReader / OpenExternal
}
```

`ReaderScreen` `UriHandler` must handle `OpenProfile` with `onOpenProfile(hex)` and include `onOpenProfile` in `remember` keys. [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:777-785]

`BorisApp` already does [VERIFIED: app/src/main/java/org/dergigi/boris/ui/BorisApp.kt:321-324]:

```kotlin
onOpenProfile = { pubkeyHex ->
    runCatching { Nip19.npubEncode(pubkeyHex) }.getOrNull()?.let { npub ->
        navController.navigate(Routes.profile(npub))
    }
},
```

This path is login-free (READ-01).

### Pattern 4: HintedRelays union, never replace NIP-65

**What:** `object HintedRelays` with `remember(pubkeyHex, relays)`, `forPubkey(pubkeyHex)`, in-memory map + JSON file.

**Init:** `HintedRelays.init(File(filesDir, "hinted_relays.json"))` next to [VERIFIED: app/src/main/java/org/dergigi/boris/MainActivity.kt:57] `EventCache.init(File(filesDir, "event_cache"))` and [VERIFIED: app/src/main/java/org/dergigi/boris/MainActivity.kt:63] `RelayHealth.init(File(filesDir, "relay_health.json"))`.

**Merge:** union, lowercase pubkey key, `LocalRelays.resolve` each URL, `distinct()`, cap per pubkey (use `OutboxRouter.MIN_REDUNDANCY` analog: keep at most 8 extra hints). Never write into kind 10002 events.

**Fetch (D-07):** change `fetchProfileRemote` from [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt:76-82]

```kotlin
private fun fetchProfileRemote(pubkeyHex: String): Nip01Event? {
    val relays = fetchRelayList(pubkeyHex).read
    val filter = JSONObject()
        .put("kinds", JSONArray().put(Nip01Event.KIND_METADATA))
        .put("authors", JSONArray().put(pubkeyHex))
        .put("limit", 5)
```

to `relays = (extraRelays + HintedRelays.forPubkey(pubkeyHex) + fetchRelayList(pubkeyHex).read).distinct()`. Expose `extraRelays: List<String> = emptyList()` on `fetchProfile` so a just-decoded nprofile can pass hints before persist lands.

`YouViewModel.refresh` already builds [VERIFIED: app/src/main/java/org/dergigi/boris/ui/you/YouViewModel.kt:167-172]:

```kotlin
val list = RelayQuery.fetchRelayList(key)
val relays = buildList {
    addAll(RelayList.FALLBACK)
    addAll(list.write)
    addAll(list.read)
}.distinct()
```

Add `addAll(HintedRelays.forPubkey(key))` so later profile-screen visits use remembered hints (D-08). `FALLBACK` is [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/RelayList.kt:8-13]:

```kotlin
val FALLBACK = listOf(
    "wss://relay.damus.io",
    "wss://nos.lol",
    "wss://relay.primal.net",
    "wss://wot.dergigi.com",
)
```

Persist hints at rewrite time **and** at tap time (D-03 custom labels never go through rewrite).

### Anti-Patterns to Avoid

- **Copy webapp `nostrLinkPattern`:** it matches `nsec1` and optional `nostr:` (bare ids). Violates D-05, D-09, and nsec-never.
- **Put Profile into `UrlExtractor.articleUrl`:** today's `NostrLink.parse(trimmed)?.uri?.let { return it }` would treat a profile as an article. [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:49-52]
- **Fall through `ReaderRepository.fetch`:** adding `NostrTarget.Profile` without a `when` arm is a compile error; a `null`/else arm would try Jina. [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:18-21]
- **Remount markdown when kind 0 arrives:** kills selection/scroll.
- **Avatars in the mention:** D-02 / deferred.
- **Room, Hilt, DataStore** for this map.
- **Decode `nsec` to hex and treat as pubkey:** nsec is a private key with a different HRP. Never add `nsec1` to `entityRegex` or the mention regex.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bech32 / TLV | New decoder | `Nip19.parseTlv` + `bech32Decode` | Already matches naddr/nevent. |
| Display name | New formatter | `Profile.displayName` | Short-npub fallback is locked (D-01). |
| Profile UI | New screen | `Routes.profile` + `ProfileScreen` | Already wired from the byline. |
| Kind 0 query | Second fetch path | Extend `RelayQuery.fetchProfile` | D-07 says plus existing relays, not a fork. |
| Relay URL hygiene | Ad-hoc startsWith | `LocalRelays.resolve` / `Nip66.normalize` | wss-only + local Citrine. |

**Key insight:** The only new logic that does not already exist is (1) the markdown rewrite with destination-skip and (2) the per-pubkey hint map. Both stay small and JVM-testable. Everything else is an extension of a sealed type and a fetch argument.

## Common Pitfalls

### Pitfall 1: UrlExtractor swallows profiles as articles

**What goes wrong:** After `NostrLink` learns `nprofile`/`npub`, `articleUrl` and `extract` return `nostr:nprofile…`. `readerLinkAction` then `OpenInReader`, and Home share/paste opens the reader instead of a profile. `ReadingPositionStore.key` / `ContinueReading` / `ArticleUrl.host` `when`s become non-exhaustive.

**Why it happens:** [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:13] `NostrLink.parse(trimmed)?.uri?.let { return it }` and [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:52] same for `articleUrl`. [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReadingPositionStore.kt:48-51]

```kotlin
fun key(url: String): String = when (val target = NostrLink.parse(url)) {
    is NostrTarget.Article -> target.ref.coordinate
    is NostrTarget.Note -> target.eventId.lowercase()
    null -> UrlExtractor.normalize(url)
}
```

**How to avoid:** `UrlExtractor.extract` / `articleUrl` return a URI only for `Article` and `Note`. `when` on `NostrTarget` gets an explicit `is NostrTarget.Profile ->` that skips (null / continue). Compile sweep every `when (val target = NostrLink.parse`.

**Warning signs:** A test that `articleUrl("nostr:npub1…")` is non-null; Gradle non-exhaustive when errors.

### Pitfall 2: Bare bech32 becomes a link (D-09)

**What goes wrong:** `entityRegex` today makes the `nostr:` prefix optional so share-text `naddr1…` still parses. Copying that for nprofile would autolink bare `npub1` in the body if the rewrite regex is the same.

**How to avoid:** Rewrite regex **requires** `nostr:` or `nostr://`. `NostrLink.parse` may still accept prefixed hrefs (and, if needed for share, prefixed-only profiles). Do not rewrite unprefixed `nprofile1` / `npub1`.

**Warning signs:** Test body `"hello npub1…"` becomes a markdown link.

### Pitfall 3: nsec treated as a profile

**What goes wrong:** Webapp pattern includes `nsec1`. Decoding nsec yields 32-byte key material. Navigating or caching it as a pubkey is a secret leak.

**How to avoid:** Never put `nsec1` in `entityRegex` or `PROFILE_MENTION`. `NostrLink.decode` else-branch stays `null` for unknown HRPs. Tests: official nsec vector stays plaintext and `parse` returns null.

Official nsec vector [CITED: https://github.com/nostr-protocol/nips/blob/master/19.md]: `nsec1vl029mgpspedva04g90vltkh6fvh240zqtv9k0t9af8935ke9laqsnlfe5`

**Warning signs:** Any code path that `bech32Decode`s then ignores HRP.

### Pitfall 4: Markdown parser does not autolink `nostr:` today

**What goes wrong:** Planning only `UriHandler` / annotator work, shipping with raw `nostr:nprofile…` still visible.

**Why it happens:** GFM autolink extension covers `www.` / `http://` / `https://` / email, not arbitrary schemes [CITED: https://github.github.com/gfm/#autolinks-extension-]. CommonMark URI autolinks need angle brackets `<scheme:…>` [CITED: https://spec.commonmark.org/0.31.2/#autolinks]. Boris uses `GFMFlavourDescriptor()` [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:1065].

**How to avoid:** Pre-process raw prefixed mentions into markdown links. Also handle `<nostr:nprofile…>` because that **is** a CommonMark autolink; `openUri` will receive it, so `readerLinkAction` must route it.

**Warning signs:** Repro article from issue #5 still shows a bech32 blob.

### Pitfall 5: Custom markdown labels get overwritten (D-03)

**What goes wrong:** Naive `replace` turns `[Gigi](nostr:nprofile1…)` into `[@npub1…](nostr:nprofile1…)`.

**How to avoid:** Skip matches inside `](…)` destinations (webapp `replaceNostrUrisSafely`). Custom-label taps still work because the href is already a link; persist hints on tap.

### Pitfall 6: Markdown remount on profile resolve

**What goes wrong:** Updating `markdownBody` when kind 0 arrives resets scroll and kills text selection.

**How to avoid:** One rewrite at first paint. Cached kind 0 name if present. No spinner in the body (discretion, locked as "short npub is enough").

### Pitfall 7: Hint flood / ws:// hints

**What goes wrong:** A hostile nprofile with dozens of type-1 relays or `ws://` / `javascript:` strings blows the query set.

**How to avoid:** `LocalRelays.resolve` (wss or local only), cap extras, union with existing profile relays rather than replacing them.

## Code Examples

### NostrTarget.Profile

```kotlin
// Source: extend app/src/main/java/org/dergigi/boris/data/NostrLink.kt
data class Profile(
    val pubkeyHex: String,
    val encoded: String,
    override val relays: List<String> = emptyList(),
) : NostrTarget() {
    override val uri get() = "nostr:$encoded"
    override val publicUrl get() = NostrLink.gatewayUrl(encoded)
}
```

`decode` branches: `nprofile1` → `nprofileDecode` (sanitize relays, keep pubkey); `npub1` → `npubDecode` (empty relays). `nsec1` / `note1` / `nevent1` / `naddr1` stay on today's paths. Extend `entityRegex` with `nprofile1…|npub1…` using the same charset class.

### Mention rewrite (D-03 skip)

```kotlin
// Source: webapp replaceNostrUrisSafely — skip ](url) ranges, require prefix
fun rewrite(markdown: String): String {
    val (protected, restore) = protectCode(markdown)
    val linkUrls = markdownLinkUrlRanges(protected)
    val out = PROFILE_MENTION.replace(protected) { match ->
        if (linkUrls.any { match.range.first in it || match.range.last in it }) return@replace match.value
        val encoded = match.groupValues[1]
        val pointer = decodeProfile(encoded) ?: return@replace match.value
        HintedRelays.remember(pointer.pubkey, pointer.relays)
        val cached = EventCache.latest(Nip01Event.KIND_METADATA, pointer.pubkey)
            ?.let { Profile.parse(it.content) }
        val label = "@" + Profile.displayName(pointer.pubkey, cached)
        "[$label](nostr:$encoded)"
    }
    return restore(out)
}
```

`decodeProfile`: `npub` → pubkey + no relays; `nprofile` → `nprofileDecode`; anything else null.

### JVM tests to add

Run: `./gradlew :app:testDebugUnitTest --tests org.dergigi.boris.nostr.Nip19Test --tests org.dergigi.boris.data.NostrLinkTest --tests org.dergigi.boris.ui.reader.ReaderLinksTest --tests org.dergigi.boris.data.UrlExtractorTest --tests org.dergigi.boris.data.NostrMentionsTest --tests org.dergigi.boris.nostr.HintedRelaysTest`

| File | Cases |
|------|--------|
| `Nip19Test` | Official nprofile vector (pubkey + two relays); round-trip encode/decode; extra unknown TLV ignored; type-1 garbage still returns pubkey; `normalizePubkey(nprofile)` ; `nprofileDecode("npub1…")` throws; nsec HRP rejected. |
| `NostrLinkTest` | `nostr:nprofile…` and `nostr://nprofile…` → `NostrTarget.Profile`; `nostr:npub…` → Profile; naddr/note/nevent unchanged; `nostr:nsec1…` → null; official NIP-19 nprofile vector. |
| `ReaderLinksTest` | nprofile/npub → `OpenProfile(hex)`; naddr still `OpenInReader` when `openInReader=true`; mailto still `OpenExternal`; nsec URI is not `OpenProfile`. |
| `UrlExtractorTest` | `articleUrl("nostr:nprofile…")` is null; `extract("nostr:npub1…")` is null; naddr extract still works. |
| `NostrMentionsTest` | Raw `nostr:nprofile` becomes `[@` + `Profile.displayName` + `](nostr:nprofile…)`; `[Gigi](nostr:nprofile1…)` unchanged; bare `nprofile1` / `npub1` unchanged; `nostr:nsec1` unchanged; `nostr:naddr1` unchanged; fenced code unchanged. |
| `HintedRelaysTest` | Union merge; bad URL dropped; cap; remember then `forPubkey`. |

Issue #5 fixtures (must appear in tests):

- nprofile from the issue body: `nprofile1qyv8wue69uhk6mmwv9jzu6nzx56jucm0d5arsvpcxqq3qamn8ghj7atdvfex2mp6xsurgwqqyzdkm9dhdgq3jxjvw7qc26q76lememf0l75wg9gka3uzgzepx2zl2ewxw6s`
- repro naddr: `naddr1qqwhwmmjw35xcetnwvkk6mmwv4uj6arfd4jkcetnwvkkzun595pzq634npfz8rwfq2hdr8am76s9t7dt7gwpe2y3t5wyufl4phe09yxeqvzqqqr4gu7cgak5`

`Profile.displayName` fallback [VERIFIED: app/src/main/java/org/dergigi/boris/nostr/Profile.kt:14-17]:

```kotlin
fun shortNpub(pubkeyHex: String): String {
    return try {
        val npub = Nip19.npubEncode(pubkeyHex)
        if (npub.length > 16) npub.take(12) + "…" else npub
```

Webapp fallback is `npub.slice(5, 12) + "..."`. Android must use `Profile.displayName` / `shortNpub` (D-01), not the webapp slice.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Clients pasted bare npub in notes | NIP-19 `nprofile` TLV with relay hints | NIP-19 shareable identifiers | Mentions can carry where to find kind 0 |
| Android reader shows raw `nostr:nprofile` | Webapp rewrites to `@name` profile links | Boris webapp `NostrMentionLink` | Android should match mention rendering, not avatars |
| Optional `nostr:` + `nsec1` in one regex | Prefix-required, nsec-excluded matcher | This phase | D-05 / D-09 / never-nsec |

**Deprecated/outdated:**

- NIP-19 `nrelay` prefix: deprecated in the spec. Do not parse as a profile.
- Treating nprofile as an naddr/article: wrong entity class.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | GFM autolink extension never turns bare `nostr:` into a link in intellij-markdown's `GFMFlavourDescriptor` | Pitfall 4 | If it does autolink, rewrite is still required for `@name` labels and D-03, but tests should assert parser behavior. Tagged [CITED] not [VERIFIED] against the Android parser this session. |

**If this table is empty:** All claims in this research were verified or cited — no user confirmation needed.

A1 is the only assumed-adjacent gap: official GFM/CommonMark were cited; a one-line `MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString("nostr:nprofile1…")` test should confirm no `AUTOLINK` node.

## Open Questions

### Resolved (planner should not re-ask)

1. **Rewrite strategy:** Pre-process markdown (`NostrMentions.rewrite` after footnotes). UriHandler still needed for taps, including D-03 custom labels and `<nostr:…>` autolinks. Annotator is unnecessary.
2. **D-08 persistence:** JSON file `hinted_relays.json` initialized from `MainActivity`, same pattern as `RelayHealth`. Not SharedPreferences, not Room, not a fake 10002 event. Merge = union + sanitize + cap. NIP-65 stays source of author relays; hints are extras.
3. **nprofileDecode shape:** Mirror `neventDecode` (type 0 pubkey, type 1 relays). Ignore extra TLV types.
4. **TTS:** Cheap: run the same rewrite inside `TtsText.paragraphs` after `Footnotes.expand`. `MarkdownInline.plain` already speaks markdown link labels. [VERIFIED: app/src/test/java/org/dergigi/boris/tts/TtsTextTest.kt:50-56] `paragraphsFlattenLinksToLabels`
5. **Loading:** Short npub / cached kind 0 name. No spinner. No remount.
6. **Invalid nprofile:** Leave original text. `parse` returns null. No crash.
7. **Href after rewrite:** Keep `nostr:{encoded}` (nprofile stays nprofile so hints remain on D-03 taps). Do not convert to `/p/{npub}`.

### Still open (non-blocking)

1. **Should `NostrLink.parse` accept bare `nprofile1` / `npub1` (no scheme)?**
   - What we know: naddr/note/nevent already match with optional `nostr:` for share/paste. D-09 is about **article body** plaintext.
   - What's unclear: whether Home paste of a bare npub should open the profile screen.
   - Recommendation: parse profiles only with `nostr:` / `nostr://` in `NostrLink` as well, so `extract` cannot pick up a bare npub from share text even if a later bug forgets the Profile skip. Body rewrite already requires the prefix. Planner can follow this without a user checkpoint.

2. **Cap on stored hints per pubkey**
   - Recommendation: 8. Not locked. Harmless to pick 8 in the plan.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | JVM unit tests | ✓ | OpenJDK 17.0.20 | — |
| Gradle | `./gradlew :app:test` | ✓ | 8.13 | — |
| New npm/Maven packages | — | n/a | — | Do not install |
| Room / Hilt | — | n/a | — | Forbidden |

**Missing dependencies with no fallback:** none

**Missing dependencies with fallback:** none

Step 2.6: code/config + existing Gradle test runner only.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Reading is ungated (READ-01). No session check on mention taps. |
| V3 Session Management | no | — |
| V4 Access Control | no | Profile screen is already public. |
| V5 Input Validation | yes | Bech32 + HRP check; TLV type 0 length 32; relay URLs through `LocalRelays.resolve`; rewrite regex excludes `nsec1`. |
| V6 Cryptography | yes | Never decode/store/log `nsec`. Reuse existing `Nip19` bech32. Do not hand-roll crypto. |

### Known Threat Patterns for NIP-19 mentions in markdown

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| `nostr:nsec1…` in article treated as profile | Information Disclosure | Exclude `nsec1` from parse and rewrite; tests with official nsec vector. |
| Hostile relay hints (`ws://`, huge lists) | Denial of Service | Sanitize + cap; union with known relays; `RelayHealth` cooldown already skips dead relays. |
| Profile URI opens reader / Jina | Tampering | `NostrTarget.Profile` never returned from `articleUrl`; `ReaderRepository.fetch` does not fetch profiles as articles. |
| Markdown remount / untrusted HTML | Tampering | GFM renderer already used; rewrite emits only `[label](nostr:bech32)` with a decoded label from `Profile.displayName` (no raw author HTML). |
| Bech32 bomb (>5000 chars) | Denial of Service | NIP-19 SHOULD limit 5000 characters [CITED: https://github.com/nostr-protocol/nips/blob/master/19.md]; reject overlong identifiers before decode. |

## Sources

### Primary (HIGH confidence)

- `app/src/main/java/org/dergigi/boris/nostr/Nip19.kt` — `neventDecode` / `parseTlv` / `normalizePubkey`
- `app/src/main/java/org/dergigi/boris/data/NostrLink.kt` — entity regex, decode switch
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt` — tap routing
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` — markdown + UriHandler
- `app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt` — `fetchProfile` / `fetchProfileRemote`
- `app/src/main/java/org/dergigi/boris/nostr/Profile.kt` — `displayName` / `shortNpub`
- `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` — `Routes.profile`
- `app/src/main/java/org/dergigi/boris/ui/you/YouViewModel.kt` — profile fetch relays
- `/Users/gigi/Development/vibe/boris/src/components/NostrMentionLink.tsx` — `@name` + `/p/{npub}`
- `/Users/gigi/Development/vibe/boris/src/utils/nostrUriResolver.tsx` — `replaceNostrUrisSafely`
- `.planning/phases/05-resolve-nostr-profile-references/05-CONTEXT.md` — D-01..D-09

### Secondary (MEDIUM confidence)

- https://github.com/nostr-protocol/nips/blob/master/19.md — nprofile TLV + official vector + ignore unknown TLV + nsec vector
- https://github.com/dergigi/boris-android/issues/5 — acceptance criteria and repro naddr/nprofile
- https://github.github.com/gfm/#autolinks-extension- — GFM autolink schemes
- https://spec.commonmark.org/0.31.2/#autolinks — angle-bracket URI autolinks

### Tertiary (LOW confidence)

- classify-confidence seam rated webfetch/websearch LOW even for the NIP-19 raw file; in-repo quotes remain HIGH via `Read`.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new libraries; versions read from `libs.versions.toml`
- Architecture: HIGH — seams read this session (`NostrLink`, `ReaderLinks`, `RelayQuery`, `ReaderScreen`, webapp rewrite)
- Pitfalls: HIGH — UrlExtractor swallow, sealed when sweep, nsec, D-09, markdown remount all grounded in file reads; GFM non-autolink is CITED (A1)

**Research date:** 2026-08-18
**Valid until:** 2026-09-17 (30 days; NIP-19 and in-repo APIs are stable)
