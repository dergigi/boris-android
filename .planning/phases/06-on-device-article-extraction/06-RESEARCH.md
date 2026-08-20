# Phase 6: On-device article extraction - Research

**Researched:** 2026-08-20
**Domain:** Android OkHttp fetch + on-device HTML readability + Markdown
**Confidence:** HIGH (in-repo seams); MEDIUM (jsoup + Mozilla algorithm citations)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Jina leftover
- **D-01:** Do not call `r.jina.ai`. Fetch the origin URL and extract locally. — **Reversibility:** costly — ReaderRepository fetch, OkHttp cache keys, and parse all assume the Jina proxy today
- **D-02:** Ignore leftover Jina cache entries. New loads always fetch the origin. Old Jina bodies age out.
- **D-03:** Android extract may diverge from the webapp. This phase is Android-only.
- **D-04:** Remove the Jina URL builder (`toProxyUrl` / `https://r.jina.ai/{url}`). Do not keep a dormant proxy path.

### Quality bar
- **D-05:** Success means Jina-like Markdown: headings, lists, blockquotes, code, tables, footnotes, plus title, byline if obvious, body, and images.
- **D-06:** Keep origin image URLs in the Markdown. Resolve relative `src` against the article URL.
- **D-07:** Strip chrome. Keep the article content node (title / byline / body / images). Drop nav, ads, comments.
- **D-08:** A near-empty extract is a failure (cookie wall, JS shell, thin paywall teaser). Do not show a hollow reader.

### Blocked / JS-heavy pages
- **D-09:** First request uses an honest Boris User-Agent. On 401, 403, or empty extract, retry once with a browser-like User-Agent. Then fail.
- **D-10:** HTTP + HTML only. Do not execute JavaScript or use a hidden WebView. — **Reversibility:** one-way for this phase — a renderer would be its own phase
- **D-11:** Paywalls use the same thin-extract rule as D-08. No special paywall detector.

### Error copy
- **D-12:** Two reader errors: fetch failed vs no article found.
- **D-13:** Copy: `Could not reach this page.` and `Could not find an article on this page.` No mention of Jina or extract libraries.
- **D-14:** Open original stays on both errors.
- **D-15:** If the live origin fetch fails and OkHttp has a cached local extract from an earlier visit, serve that cache (same idea as today's FORCE_CACHE fallback).

### Claude's Discretion
- HTML-to-Markdown / readability library vs a small in-repo extractor, as long as D-05 through D-08 hold and there is no new cloud service.
- Empty-extract threshold (word count or similar).
- Exact Boris User-Agent and the browser-like retry UA.
- How OkHttp cache keys move from Jina URLs to origin URLs.
- Whether leftover Jina-format parse helpers stay as test-only fixtures or go away with D-04.
- RSS and Nostr paths stay untouched unless a shared helper must move.

### Deferred Ideas (OUT OF SCOPE)
- Hidden WebView / JS render for app-shell pages (D-10). Own phase if we ever need it.
- Changing the companion webapp off Jina. Android-only this phase (D-03).
- Restoring a remote extract fallback. Out of scope once D-01/D-04 land.

None else — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| **READ-04** (proposed) | Ordinary http(s) articles load by fetching the origin page on-device and extracting readable Markdown locally. Reading does not call `r.jina.ai`. Nostr and RSS paths stay as they are. A previously cached local extract still opens offline. Login is not required. | Delete `toProxyUrl`. GET the origin with Boris UA (one browser-UA retry). `ArticleExtractor` + jsoup strips chrome. `HtmlToMarkdown` emits D-05 markdown. Thin extract fails. Cache key is the origin URL. |
| READ-01 | User can paste, share, or open a URL and read the article while logged out. Login UI sits on Home; it does not replace or block reading. | Keep `ReaderViewModel.load` → `ReaderRepository.fetch` with no session check. Error UI still offers Open original. Do not add an auth gate. |

Planner must add READ-04 to `.planning/REQUIREMENTS.md` and map Phase 6 to it. READ-01 stays implemented and must not regress.
</phase_requirements>

## Summary

Ordinary web reads today go to `https://r.jina.ai/{url}` with `Accept: text/plain`. A 401 from that host blanks every http(s) article (GitHub #54). Phase 6 deletes that proxy. The `null` `NostrLink` branch fetches the origin, extracts an article node on-device, and turns it into Markdown. Article / Note / Profile / RSS `when` arms stay as they are.

The repo already converts simple article HTML: `HtmlToMarkdown` covers headings, lists, blockquotes, code, links, and images, and RSS already rejects bodies shorter than 500 characters. What it does not do is strip nav/ads/comments (D-07) or emit tables and footnotes (D-05). Add `org.jsoup:jsoup:1.23.1` for a real DOM. Put chrome stripping in a new in-repo `ArticleExtractor`. Extend `HtmlToMarkdown` for GFM tables, `[^n]` footnotes, and ordered lists. Do not add Readability4J (2018 Mozilla snapshot, slf4j, Kotlin 1.3.72, jsoup 1.11.2). Do not add Crux (article plugin removed in v5). Do not add a remote fallback.

**Primary recommendation:** Fetch origin HTML with OkHttp. Parse with `Jsoup.parse(html, baseUri)`. Extract the article node in-repo. Convert with the existing `HtmlToMarkdown`. Fail under 500 characters of extracted Markdown. Cache the origin URL, never `r.jina.ai`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Origin HTTP GET + UA retry | API / Backend (`data/ReaderRepository`) | — | Fetch already lives here. UI must not call OkHttp. |
| Chrome strip (D-07) | API / Backend (`data/ArticleExtractor`) | — | DOM work on the IO dispatcher. |
| HTML → Markdown (D-05) | API / Backend (`data/HtmlToMarkdown`) | — | Shared with RSS. Keep one converter. |
| Thin-extract / error kind | API / Backend | Browser / Client (`ReaderUiState.Error`) | Repository throws `IOException` with D-13 copy. ViewModel maps `e.message`. |
| Offline cache (D-15) | Database / Storage (OkHttp `Cache`) | API / Backend | Key is the request URL. `FORCE_CACHE` already exists. |
| Open original (D-14) | Browser / Client | — | Error UI already shows the button when `url` is not blank. |
| Images | CDN / origin via Coil | `UrlExtractor` | Keep origin `src`, resolve relative, `preferHttps` for Coil. |
| Nostr / RSS | API / Backend | — | Do not change those `fetch` arms. |

## Project Constraints (from .cursor/rules/)

- Release checklist in `release-zapstore.mdc` applies only when cutting a release. This phase does not ship a version. Do not bump `versionCode` / `versionName` or publish Zapstore as part of Phase 6.

From `.planning/codebase/CONVENTIONS.md` and `ARCHITECTURE.md` (must honor):

- New Kotlin under `org.dergigi.boris` only. Do not edit `com.readwithboris`.
- No Hilt/Koin, no Room, no new DI.
- JVM JUnit tests under `app/src/test/java/org/dergigi/boris/`. Filename `{Type}Test.kt`.
- No `Log` / Timber. No KDoc. No TODO comments in Kotlin.
- UI does not import into data; data does not import UI.
- `minSdk` 26. `android:usesCleartextTraffic="false"`.
- Reuse the `ReaderRepository` companion `OkHttpClient`. Do not construct a client per request.
- Fetch/parse stays in `ReaderRepository`, not a Composable.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Existing OkHttp | 4.12.0 | Origin GET + disk cache | [VERIFIED: gradle/libs.versions.toml:10] `okhttp = "4.12.0"` |
| Existing `HtmlToMarkdown` | in-repo | HTML → Markdown | Already used for RSS and HTML fallback. [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:141] `val markdown = HtmlToMarkdown.convert(html)` |
| Existing `OgMeta` / `PublishedTime` | in-repo | Title, cover, published | Keep for metadata after local extract. |
| jsoup | 1.23.1 | HTML5 DOM + CSS selectors | Official current release, MIT, no runtime deps. Android + Kotlin listed. [CITED: https://jsoup.org/download] POM `200` on Maven Central this session. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit | 4.13.2 | JVM fixture tests | [VERIFIED: gradle/libs.versions.toml:13] `junit = "4.13.2"` |
| `com.android.tools:desugar_jdk_libs_nio` | resolve at plan time | jsoup Android requirement | Official jsoup Android note: enable core library desugaring with the NIO spec. [CITED: https://jsoup.org/download] Google Maven was unreachable this session (A2). |
| Existing `Footnotes.expand` | in-repo | Render `[^n]` in the reader | Emit GFM footnotes from HTML so this path lights up. [VERIFIED: app/src/main/java/org/dergigi/boris/data/Footnotes.kt:10] `fun expand(markdown: String): String` |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| jsoup + in-repo `ArticleExtractor` | `net.dankito.readability4j:readability4j:1.0.8` | Closest Mozilla port. Rejected: last matched Readability.js commit is 2018; POM depends on `kotlin-stdlib` 1.3.72, `slf4j-api` 1.7.25, `jsoup` 1.11.2. Clash with Kotlin 2.1.21 and jsoup 1.23.1. [CITED: https://github.com/dankito/Readability4J] [VERIFIED: Maven Central POM 1.0.8] |
| jsoup + in-repo extractor | `com.chimbori.crux:crux` 5.x | Official README removed `ArticleExtractorPlugin` and points at Readability4J. Do not add. [CITED: https://github.com/chimbori/crux] |
| Extend `HtmlToMarkdown` | flexmark / Remark html2md | Extra Maven graph. Repo already converts the tags D-05 needs except tables and footnotes. |
| In-repo extractor | Hidden WebView / JS | Deferred (D-10). |
| Origin-only cache | Keep serving leftover Jina bodies | Violates D-02. |

**Installation:**

```toml
# gradle/libs.versions.toml
jsoup = "1.23.1"
# plus a desugar_jdk_libs_nio version resolved from Google / Huawei Maven
```

```kotlin
// app/build.gradle.kts
compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    implementation(libs.jsoup)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
}
```

**Version verification:** `https://repo1.maven.org/maven2/org/jsoup/jsoup/1.23.1/jsoup-1.23.1.pom` returned HTTP 200 this session. Official download page lists the same version. [CITED: https://jsoup.org/download]

Do not call `Jsoup.connect`. Boris already fetches with OkHttp. Parse the response string.

## Package Legitimacy Audit

`gsd-tools query package-legitimacy check` accepts only `npm|pypi|crates`. Maven is unsupported. Gate used: official project docs + Maven Central POM `200` + source repo on the official site.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `org.jsoup:jsoup` 1.23.1 | Maven Central | since 2009 (inceptionYear on POM) | n/a (Maven) | github.com/jhy/jsoup (jsoup.org) | OK (official docs) | Approved |
| `com.android.tools:desugar_jdk_libs_nio` | Google Maven | n/a | n/a | Android official | OK (official docs) | Approved; resolve version at plan time (A2) |
| `net.dankito.readability4j:readability4j` | Maven Central | 1.0.8 published 2021; algorithm 2018 | n/a | github.com/dankito/Readability4J | not installed | Rejected (stale + dep clash) |
| `com.chimbori.crux:crux` | Maven Central | v5 dropped article extract | n/a | github.com/chimbori/crux | not installed | REMOVED from stack |

**Packages removed due to [SLOP] verdict:** none (Maven seam not available)
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
paste / share / VIEW URL
        │
        ▼
 UrlExtractor.normalize   (http(s) only in this branch)
        │
        ▼
 ReaderViewModel.load  ──►  ReaderRepository.fetch
        │                         │
        │                         ├─ NostrTarget.Article / Note / Profile  [unchanged]
        │                         ├─ rssContent(...)                       [unchanged]
        │                         └─ null (ordinary web)
        │                                   │
        │                                   ▼
        │                         GET origin URL
        │                           UA = Boris/{version} (Android; +https://github.com/dergigi/boris-android)
        │                           Accept = text/html,application/xhtml+xml
        │                                   │
        │                    2xx HTML ──────┤──────── 401/403/network fail
        │                         │         │                    │
        │                         ▼         │                    ▼
        │                  ArticleExtractor │           retry once, browser UA
        │                   (jsoup DOM)     │                    │
        │                         │         │         2xx ── or ── fail
        │                         │         │          │            │
        │                    thin? ──yes──► retry UA   │            ▼
        │                         │                    │   FORCE_CACHE same origin URL
        │                        no                    │     │
        │                         │                    │     ├─ hit → extract again
        │                         ▼                    │     └─ miss → "Could not reach this page."
        │                  HtmlToMarkdown.convert      │
        │                   (article node only)        │
        │                         │                    │
        │                    < 500 chars? ──yes──► "Could not find an article on this page."
        │                         │
        │                        no
        │                         ▼
        │                  ReadableContent (title, byline in markdown, body, images)
        │                         │
        ▼                         ▼
 ReaderUiState.Ready / Error      Coil loads origin image URLs
        │
        ├─ Error + url not blank → Open original + Try again
        └─ Ready → existing Markdown / highlights / TTS
```

### Recommended Project Structure

```
app/src/main/java/org/dergigi/boris/data/
├── ReaderRepository.kt     # origin GET, UA retry, FORCE_CACHE, two IOException messages
├── ArticleExtractor.kt     # NEW: jsoup chrome strip, returns article Element or null
├── HtmlToMarkdown.kt       # extend: tables, footnotes, ol, convert(Element)
├── OgMeta.kt               # keep title / image / description
├── PublishedTime.kt        # keep fromHtml; delete fromJinaHeader once unused
├── ArticleCover.kt         # keep stripLeadingImage; delete imageFromJina / descriptionFromJina
├── UrlExtractor.kt         # preferHttps on the web fetch URL (cleartext is off)
└── ReadableContent.kt      # unchanged
app/src/main/res/values/strings.xml
└── reader_error_unreachable / reader_error_no_article
app/src/test/java/org/dergigi/boris/data/
├── ArticleExtractorTest.kt
├── HtmlToMarkdownTest.kt   # add table + footnote + chrome-free fixtures
└── ReaderRepositoryParseTest.kt  # HTML-only; drop Jina header fixtures from production path
```

### Pattern 1: Origin fetch, no proxy

**What:** In the `null` `NostrLink` arm, GET `preferHttps(normalize(url))`. Delete `toProxyUrl`.

Today [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:23-29]:

```kotlin
val targetUrl = UrlExtractor.normalize(url)
rssContent(url, targetUrl) ?: run {
    val request = Request.Builder()
        .url(toProxyUrl(targetUrl))
        .header("Accept", "text/plain")
        .get()
        .build()
```

[VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:215] `private fun toProxyUrl(url: String): String = "https://r.jina.ai/$url"`

Prescribed:

```kotlin
val targetUrl = UrlExtractor.preferHttps(UrlExtractor.normalize(url))
val request = Request.Builder()
    .url(targetUrl)
    .header("User-Agent", BORIS_UA)
    .header("Accept", "text/html,application/xhtml+xml")
    .get()
    .build()
```

`preferHttps` already exists [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:38-44]:

```kotlin
fun preferHttps(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.startsWith("http://", ignoreCase = true)) {
        "https://" + trimmed.substring(7)
    } else {
        trimmed
    }
}
```

Cleartext is off [VERIFIED: app/src/main/AndroidManifest.xml:35] `android:usesCleartextTraffic="false"`. After Jina is gone, a raw `http://` origin GET fails on the device. Upgrade the article request the same way images already do.

### Pattern 2: Two errors, one retry, cache on fetch fail

**What:** Map outcomes to D-12 / D-13. Retry once per D-09. Serve origin cache per D-15.

Constants (verbatim D-13):

```kotlin
const val ERROR_UNREACHABLE = "Could not reach this page."
const val ERROR_NO_ARTICLE = "Could not find an article on this page."
```

Rules:

| Outcome | Action |
|---------|--------|
| Network / timeout / DNS / non-2xx after retry | `IOException(ERROR_UNREACHABLE)` unless `executeFromCache` on the **origin** request returns a body |
| 401 or 403 on first try | Rebuild the same URL with `BROWSER_UA`, once |
| 2xx but not HTML (`Content-Type` image/*, application/pdf, application/json, empty) | Treat as empty extract: retry UA if first try, then `ERROR_NO_ARTICLE` |
| 2xx HTML, extract &lt; 500 Markdown chars | Retry UA if first try, then `ERROR_NO_ARTICLE` |
| Live fail + origin `FORCE_CACHE` hit | Re-run extract on the cached HTML. Thin → `ERROR_NO_ARTICLE`. Good → success |
| Live fail + no origin cache | `ERROR_UNREACHABLE` |

Do not probe `https://r.jina.ai/...` in `cachedBodyBytes` (D-02, D-04). Today [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:233-236]:

```kotlin
val candidates = listOf(
    "https://r.jina.ai/$normalized",
    normalized,
).distinct()
```

After: `listOf(UrlExtractor.preferHttps(normalized))` only.

Keep the network interceptor that rewrites `Cache-Control` to `public, max-age=300` so origins that send `no-store` still land in `reader_http_cache` for D-15. Change the comment; it currently says jina sends no cache headers [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:261-267].

Cap the response body (2 MiB). `body.string()` is unbounded today.

### Pattern 3: ArticleExtractor (jsoup, no JS)

**What:** Parse, drop chrome, pick the article node, or return null.

Official parse [CITED: https://jsoup.org/cookbook/input/parse-document-from-string]:

```java
Document doc = Jsoup.parse(html, baseUri);
```

`baseUri` is the origin URL so `absUrl("src")` / `absUrl("href")` resolve relative links (D-06).

Prescribed Kotlin:

```kotlin
object ArticleExtractor {
    fun article(html: String, baseUrl: String): org.jsoup.nodes.Element? {
        val doc = org.jsoup.Jsoup.parse(html, baseUrl)
        doc.select(CHROME).remove()
        val candidates = doc.select(ARTICLE)
        val best = candidates.maxByOrNull { score(it) } ?: doc.body() ?: return null
        if (score(best) < MIN_CHARS) return null
        return best
    }

    private fun score(el: org.jsoup.nodes.Element): Int {
        val text = el.text().trim()
        if (text.length < MIN_CHARS) return 0
        val links = el.select("a").text().length
        val density = links.toDouble() / text.length
        if (density > 0.5) return 0
        return text.length
    }
}
```

Selectors (use these strings):

- `CHROME` = `script, style, noscript, iframe, svg, canvas, form, nav, footer, aside, [role=navigation], [role=banner], [role=contentinfo], [role=complementary], #comments, .comments, .comment-list, .sidebar, .ad, .ads, .advertisement, [aria-hidden=true]`
- `ARTICLE` = `article, [itemprop=articleBody], [role=article], main, [role=main], #content, .post-content, .entry-content, .article-body`

This is a small Mozilla-shaped grabber, not a JS port. `charThreshold` default in Mozilla Readability is 500. [CITED: https://github.com/mozilla/readability/blob/master/README.md] `charThreshold (number, default 500)`

Do not run scripts. Do not use WebView. jsoup builds a WHATWG tree from bytes only [CITED: https://jsoup.org/].

### Pattern 4: Markdown from the article node

**What:** Convert only the extracted node. Extend `HtmlToMarkdown` for D-05 gaps.

Already present [VERIFIED: app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt:36-53]: headings `h1`–`h6`, `<strong>`/`<em>`, `<blockquote>`, `<li>` as `- `, `<pre>`/`<code>`, `<img>` with `UrlExtractor.articleUrl` + `preferHttps`.

Add:

1. `fun convert(element: org.jsoup.nodes.Element, baseUrl: String): String` that walks the node (or `element.outerHtml()` into the existing pipeline after chrome strip).
2. GFM tables from `table / thead / tbody / tr / th / td`.
3. Footnotes: `sup a[href^=#]` plus matching `li[id]` / `.footnotes li` → `[^id]` and `[^id]: text`. `Footnotes.expand` already renders that form.
4. `<ol><li>` → `1. ` not `- `.
5. Byline if obvious: `meta[name=author]`, `[rel=author]`, `[itemprop=author]`. Prepend `*${byline}*` to the markdown. Do not add a `ReadableContent` field.

Title / cover / published stay on `OgMeta.parse` + `PublishedTime.fromHtml` + `ArticleCover.stripLeadingImage`.

Empty-extract threshold: **500 characters of converted Markdown**, same number as RSS teasers and Mozilla `charThreshold`. [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:277-278]

```kotlin
/** Feed bodies shorter than this are teasers; fetch the web page instead. */
private const val MIN_RSS_MARKDOWN_CHARS = 500
```

Reuse that constant (rename to a shared `MIN_ARTICLE_MARKDOWN_CHARS = 500` if the helper moves). Do not invent a second number.

### Pattern 5: User-Agents

Honest first UA (D-09):

```kotlin
const val BORIS_UA =
    "Boris/${org.dergigi.boris.BuildConfig.VERSION_NAME} (Android; +https://github.com/dergigi/boris-android)"
```

`BuildConfig` is already generated [VERIFIED: app/build.gradle.kts:89] `buildConfig = true` and used for `VERSION_NAME` in settings.

Browser retry UA: reuse the string already sent by OG fetches [VERIFIED: app/src/main/java/org/dergigi/boris/data/OgMetaClient.kt:33-34]:

```kotlin
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
```

Move both constants to one file (`HttpUserAgents` or `ArticleExtractor` companion) so OG and reader retry stay identical.

### Pattern 6: Delete Jina parse from production

`parse()` currently branches on `Markdown Content:` [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:160-178]. After D-04 / D-02, live and cached origin bodies are HTML. Delete:

- `toProxyUrl`
- `markdownBlockRegex` / `titleRegex` / `markdownRegex`
- `ArticleCover.imageFromJina` / `descriptionFromJina`
- `PublishedTime.fromJinaHeader`
- Jina candidate in `cachedBodyBytes`

Rewrite `ReaderRepositoryParseTest` around HTML fixtures (the file already has `parsesHtmlFallback` and `htmlFallbackKeepsImagesInMarkdown`). Do not keep Jina-format helpers as test-only fixtures. Dead format code will rot.

`parse` can become `internal fun parseHtml(targetUrl: String, html: String)` used by fetch and tests.

### Anti-Patterns to Avoid

- **Call `Jsoup.connect`:** second HTTP stack, no Boris UA, no OkHttp cache.
- **Add Readability4J + jsoup 1.23.1:** 1.15+ removed `Whitelist`; the 1.11.2-era port can `NoClassDefFoundError`.
- **Convert the full `body` without chrome strip:** D-07 fails; nav and comments become Markdown.
- **Serve leftover `r.jina.ai` cache:** D-02.
- **Keep `toProxyUrl` unused:** D-04.
- **WebView / evaluateJavascript:** D-10.
- **Mention Jina or jsoup in UI copy:** D-13.
- **Gate fetch on session:** READ-01.
- **Construct a new `OkHttpClient` for the retry:** reuse `defaultClient`.
- **Touch Nostr / RSS `when` arms** unless moving `MIN_RSS_MARKDOWN_CHARS`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTML parse | Regex DOM | jsoup `Jsoup.parse(html, baseUri)` | Tag soup, encoding, relative URLs. Official WHATWG tree. |
| Image / link absolutizing | Ad-hoc string join | jsoup `absUrl` + existing `UrlExtractor.articleUrl` / `preferHttps` | D-06 + Coil HTTPS. |
| Markdown of a clean article node | New converter | Existing `HtmlToMarkdown` | Already tested. Extend for tables / footnotes / `ol`. |
| Origin GET + cache | New HTTP client | Existing OkHttp + `FORCE_CACHE` | D-15 is this cache. |
| Footnote display | New renderer | `Footnotes.expand` | Reader already understands `[^n]`. |
| Title / OG / published | New meta parser | `OgMeta` / `PublishedTime.fromHtml` | Already used on the HTML fallback. |

**Key insight:** Chrome stripping is the only new algorithm. HTML parse is jsoup. Markdown is the in-repo converter. HTTP is OkHttp. Do not import a stale Readability port just to avoid a 100-line selector + density function.

## Runtime State Inventory

This phase moves cache keys from Jina URLs to origin URLs.

| Category | Items Found | Action Required |
|----------|-------------|-----------------|
| Stored data | OkHttp disk cache at `File(filesDir, "reader_http_cache")` [VERIFIED: app/src/main/java/org/dergigi/boris/MainActivity.kt:72] `ReaderRepository.init(File(filesDir, "reader_http_cache"), CacheLimit.bytes(this))`. Entries are keyed by request URL. Existing keys are `https://r.jina.ai/{origin}`. | Code edit only. Do not migrate or delete the directory. New loads write origin keys. Old Jina entries age out of the 50 MiB cache (D-02). [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:274] `private const val HTTP_CACHE_BYTES = 50L * 1024L * 1024L` |
| Live service config | None. No Jina account or API key. | none |
| OS-registered state | None. Cache is app-private files. | none |
| Secrets/env vars | None for article fetch. | none |
| Build artifacts | Gradle caches of current deps. New jsoup / desugar artifacts after sync. | `./gradlew :app:testDebugUnitTest` after catalog edit |

**Nothing found in category:** Live service config, OS-registered state, secrets: none, verified by reading `ReaderRepository`, `MainActivity`, `INTEGRATIONS.md`, and `.env` not being referenced by the reader path.

## Common Pitfalls

### Pitfall 1: Cache key stays on Jina

**What goes wrong:** Fetch goes to the origin but `cachedBodyBytes` / `executeFromCache` still builds `https://r.jina.ai/$url`. Offline size shows 0. D-15 never hits.

**Why it happens:** OkHttp cache key is the request URL (MD5 of the URL string). [CITED: https://square.github.io/okhttp/4.x/okhttp/okhttp3/-cache/index.html] plus `Cache.key(request.url)` in OkHttp source.

**How to avoid:** One origin URL for live GET, FORCE_CACHE, and `cachedBodyBytes`. Delete the Jina candidate list.

**Warning signs:** Tests or settings "available offline" still mention `r.jina.ai`.

### Pitfall 2: Cleartext `http://` article GET

**What goes wrong:** `normalize` keeps `http://`. [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:25-31] Comment still says article opens keep `http://` for Jina [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:36-37]. Device blocks the request.

**How to avoid:** `preferHttps` on the web-fetch URL. Update that comment.

**Warning signs:** "Could not reach this page." on every `http://` paste.

### Pitfall 3: Full-page Markdown (chrome leak)

**What goes wrong:** `HtmlToMarkdown.convert(fullHtml)` includes nav, cookie banner, comments.

**How to avoid:** Extract first, convert the article node only. Tests: fixture with `<nav>Home</nav><article><p>…500+ chars…</p></article><div id="comments">nope</div>` must not contain `Home` or `nope`.

**Warning signs:** Reader shows site menus.

### Pitfall 4: Thin extract shown as an article

**What goes wrong:** Cookie wall or JS shell becomes a 40-word "page".

**How to avoid:** 500-char Markdown threshold after convert (D-08 / D-11). Same as `MIN_RSS_MARKDOWN_CHARS`.

**Warning signs:** Paywall teaser renders as Ready.

### Pitfall 5: jsoup without desugaring

**What goes wrong:** D8 / runtime miss on `java.nio` or `java.util.function` helpers jsoup 1.23 expects.

**How to avoid:** `isCoreLibraryDesugaringEnabled = true` and `desugar_jdk_libs_nio` as official jsoup Android support requires. [CITED: https://jsoup.org/download] [CITED: https://github.com/jhy/jsoup CHANGES.md min Android API 21 + desugaring]

**Warning signs:** Debug install crashes in `org.jsoup` on API 26.

### Pitfall 6: UA retry overwrites a good cache with a fail

**What goes wrong:** First request 200 + thin extract. Retry 403. Cache stores the 403 if you write the error response.

**How to avoid:** Only cache successful HTML bodies (OkHttp already skips unsuccessful). Do not rewrite the interceptor to force-cache 401/403. Retry is a second GET.

### Pitfall 7: Error copy leaks internals

**What goes wrong:** Today's `Failed to fetch readable content (${response.code})` [VERIFIED: app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:48] and ViewModel fallback `Failed to load this article.` [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt:219] still appear.

**How to avoid:** Those two D-13 strings only. Add `reader_error_unreachable` / `reader_error_no_article` in `strings.xml`. Open original stays [VERIFIED: app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:860-866].

### Pitfall 8: RSS / Nostr regression

**What goes wrong:** Shared `HtmlToMarkdown` table/footnote changes break feed bodies.

**How to avoid:** Keep `rssContent` calling `HtmlToMarkdown.convert(html)` without `ArticleExtractor`. Run `HtmlToMarkdownTest` and RSS tests every wave.

## Code Examples

### jsoup parse (official)

```java
// Source: https://jsoup.org/cookbook/input/parse-document-from-string
String html = "<html><head><title>First parse</title></head>"
  + "<body><p>Parsed HTML into a doc.</p></body></html>";
Document doc = Jsoup.parse(html);
```

Use the two-arg form with the article URL as `baseUri`.

### Fetch arm (prescribed)

```kotlin
// Source: replace ReaderRepository null-NostrLink branch
val origin = UrlExtractor.preferHttps(UrlExtractor.normalize(url))
val html = getHtml(origin, BORIS_UA)
    ?: getHtml(origin, BROWSER_UA)
    ?: executeFromCache(originRequest(origin, BORIS_UA))
    ?: throw IOException(ERROR_UNREACHABLE)
val markdown = extractMarkdown(origin, html)
    ?: if (usedBorisUaOnly) {
        extractMarkdown(origin, getHtml(origin, BROWSER_UA) ?: html)
    } else {
        null
    }
    ?: throw IOException(ERROR_NO_ARTICLE)
```

Keep the retry logic in one private function so tests can drive `parse` / `extract` without sockets.

### JVM tests to add

Run: `./gradlew :app:testDebugUnitTest --tests org.dergigi.boris.data.ArticleExtractorTest --tests org.dergigi.boris.data.HtmlToMarkdownTest --tests org.dergigi.boris.data.ReaderRepositoryParseTest`

| File | Cases |
|------|-------|
| `ArticleExtractorTest` | `article` kept, `nav`/`#comments` dropped; link-dense node score 0; under-500 text → null; relative `img src` becomes absolute via baseUri |
| `HtmlToMarkdownTest` | existing cases stay; GFM table; `[^1]` footnote pair; `ol` numbering; script tags gone |
| `ReaderRepositoryParseTest` | delete `parsesJinaMarkdownPayload` / `parsesPublishedTimeFromJinaHeader` / `parsesCoverFromJinaHeaderAndStripsTheLeadImage`; keep HTML title + images; add thin-html → empty markdown; add chrome fixture |

No live network. No Robolectric.

Issue #54 acceptance to cover in tests / UAT: origin fetch does not use `r.jina.ai`; 401 from a remote extract host cannot happen; fetch fail vs no-article copy; offline origin cache still works.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Proxy every URL through `r.jina.ai` | Fetch origin, extract on device | This phase (D-01) | 401 on Jina no longer blocks reading |
| Jina `Title:` / `Markdown Content:` parse | HTML + jsoup + `HtmlToMarkdown` | This phase (D-04) | Android may diverge from the webapp (D-03) |
| Crux built-in article plugin | Removed; Crux points at Readability4J | Crux v5 | Do not add Crux for extract |
| Mozilla Readability.js in Firefox | JS + DOM; `charThreshold` 500 | ongoing | Algorithm reference only; D-10 forbids running it |

**Deprecated/outdated:**

- Jina URL builder and Jina header regexes in production.
- Readability4J 1.0.x as a current Mozilla port (frozen at 2018).
- `UrlExtractor` comment that article opens keep `http://` for Jina.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A 500-character Markdown threshold matches "near-empty" well enough for D-08 (cookie walls, JS shells, teasers) | Pattern 4 | Some real short essays fail; some teasers pass. Tune only if UAT shows it. Same number as RSS + Mozilla. |
| A2 | Current `desugar_jdk_libs_nio` version on Google / Huawei Maven | Standard Stack | Wrong pin fails Gradle sync. Planner resolves from maven-metadata (Google Maven was unreachable this session). |
| A3 | jsoup 1.23.1 `Jsoup.parse(String, String)` does not need NIO at the call site, but official docs still require desugaring | Pitfall 5 | If D8 is happy without desugar on minSdk 26, the desugar dep is unused but harmless. |
| A4 | In-repo chrome selectors are enough for D-07 on typical article HTML | Pattern 3 | App-shell pages fail (accepted under D-10). Unusual CMSes may leak chrome; add a selector when a fixture shows it. |

## Open Questions

### Resolved (planner should not re-ask)

1. **Library vs in-repo:** jsoup (parse) + in-repo `ArticleExtractor` (chrome) + extend `HtmlToMarkdown`. No Readability4J. No Crux. No remote fallback.
2. **Empty-extract threshold:** 500 Markdown characters. Shared with RSS.
3. **Boris UA:** `Boris/${BuildConfig.VERSION_NAME} (Android; +https://github.com/dergigi/boris-android)`.
4. **Retry UA:** existing OgMetaClient Chrome Mobile string.
5. **Cache keys:** origin URL only. Ignore leftover Jina entries. Keep the force-`max-age=300` interceptor.
6. **Jina parse helpers:** delete from production. Rewrite tests. No test-only Jina fixtures.
7. **RSS / Nostr:** do not change those `fetch` arms. Shared `HtmlToMarkdown` extensions must keep existing RSS tests green.

### Still open (non-blocking)

1. **Exact `desugar_jdk_libs_nio` version**
   - What we know: jsoup official Android support requires NIO desugaring. Huawei Maven is the fallback repo in `settings.gradle.kts`.
   - What's unclear: latest artifact version (Google Maven unreachable this session).
   - Recommendation: planner Wave 0 resolves the version from Google or Huawei metadata. Do not skip desugaring.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | JVM unit tests | ✓ | OpenJDK 17.0.20 | — |
| Gradle | `./gradlew :app:test` | ✓ | 8.13 | — |
| Maven Central | jsoup 1.23.1 | ✓ | POM HTTP 200 | Huawei Cloud Maven (already in `settings.gradle.kts`) |
| Google Maven | desugar_jdk_libs_nio | ✗ this session | — | Huawei Cloud Maven fallback |
| ctx7 / Context7 | docs lookup | ✗ | — | Official URLs via WebFetch |
| Room / Hilt / WebView extract | — | n/a | — | Forbidden / deferred |

**Missing dependencies with no fallback:** none for jsoup. Desugar version pin is a Wave 0 resolve, not a blocker.

**Missing dependencies with fallback:** Google Maven (use Huawei mirror already configured).

Step 2.6: JDK, Gradle, Maven Central checked this session. Graph: none (`.planning/graphs/graph.json` absent).

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Reading is ungated (READ-01). |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes | jsoup parse; drop `script`/`iframe`; `UrlExtractor` scheme denylist; cap body size; no JS. |
| V6 Cryptography | no | No new crypto. |

### Known Threat Patterns for on-device HTML extract

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Script in article HTML | Elevation / XSS in WebView | D-10: no WebView. Markdown renderer, not `loadData`. `HtmlToMarkdown` already strips `<script>` / `<style>` / `<head>`. [VERIFIED: app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt:17-19] |
| `javascript:` image / link | Tampering | `UrlExtractor.nonHttpSchemes` includes `javascript`. [VERIFIED: app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:196] `private val nonHttpSchemes = setOf("mailto", "tel", "javascript", "sms", "geo", "blob", "data")` |
| Huge HTML OOM | Denial of Service | 2 MiB body cap before parse. |
| Hostile UA fingerprinting | Information Disclosure | Honest Boris UA first (D-09). Browser UA only on retry. |
| SSRF to LAN / cleartext | Tampering | `usesCleartextTraffic=false`; `preferHttps` on article GET. Do not add a user-controlled proxy. |
| Residual Jina privacy leak | Information Disclosure | Delete `toProxyUrl`. Stop sending every URL to a third party. |

Mozilla's own security note: sanitize Readability output and do not execute scripts. [CITED: https://github.com/mozilla/readability/blob/master/README.md] Boris never feeds extract HTML to a WebView, so the Markdown path is the sanitizer.

## Sources

### Primary (HIGH confidence)

- `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` — fetch, `toProxyUrl`, parse, cache
- `app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt` — current converter
- `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` — normalize / preferHttps / schemes
- `app/src/main/java/org/dergigi/boris/data/OgMetaClient.kt` — browser UA
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` — `ReaderUiState.Error`
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` — Open original
- `app/src/main/AndroidManifest.xml` — INTERNET, cleartext off
- `app/src/main/res/values/strings.xml` — `reader_open_original`
- `gradle/libs.versions.toml` — OkHttp / JUnit versions
- `.planning/phases/06-on-device-article-extraction/06-CONTEXT.md` — D-01..D-15
- https://jsoup.org/download — 1.23.1, Gradle/Maven coords, Android desugaring
- https://jsoup.org/cookbook/input/parse-document-from-string — `Jsoup.parse`
- https://github.com/dankito/Readability4J/blob/master/README.md — API + 2018 match table
- Maven Central POM `net.dankito.readability4j:readability4j:1.0.8` — kotlin-stdlib 1.3.72, slf4j, jsoup 1.11.2
- https://github.com/chimbori/crux — no article plugin as of v5
- https://github.com/mozilla/readability/blob/master/README.md — `charThreshold` 500, JS-only
- https://github.com/dergigi/boris-android/issues/54 — 401 report + acceptance ideas

### Secondary (MEDIUM confidence)

- https://square.github.io/okhttp/4.x/okhttp/okhttp3/-cache/index.html — FORCE_CACHE / only-if-cached
- https://github.com/jina-ai/reader — default `Title:` / `Markdown Content:` header format; default path uses readability then markdown
- https://github.com/jhy/jsoup/blob/master/CHANGES.md — min Android API 21, desugaring required

### Tertiary (LOW confidence)

- classify-confidence seam rated webfetch/websearch LOW even for official pages. In-repo quotes remain HIGH via `Read`.
- Desugar artifact version (Google Maven unreachable).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH for jsoup 1.23.1 + in-repo converter (official download + POM 200 + file reads). MEDIUM for desugar pin (A2).
- Architecture: HIGH — fetch/parse/cache/error seams read this session.
- Pitfalls: HIGH for cache key, cleartext, chrome leak, D-13 copy; MEDIUM for desugaring runtime (official docs, not reproduced on a device this session).

**Research date:** 2026-08-20
**Valid until:** 2026-09-19 (30 days; jsoup and OkHttp are stable)
