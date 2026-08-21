# Phase 6: On-device article extraction - Context

**Gathered:** 2026-08-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Ordinary http(s) articles load by fetching the page on-device and extracting readable Markdown locally. `r.jina.ai` is not used. Nostr article/note fetches and RSS stay as they are. Offline cache of locally extracted content still works. Reading stays ungated. No nsec. No API keys. GitHub issue #54.

</domain>

<decisions>
## Implementation Decisions

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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product
- `.planning/PROJECT.md` — reading first, no nsec, no API keys
- `.planning/REQUIREMENTS.md` — READ-01 must not regress
- `.planning/ROADMAP.md` — Phase 6
- `.planning/codebase/ARCHITECTURE.md` — fetch/parse lives in `ReaderRepository`, not the UI
- `.planning/codebase/INTEGRATIONS.md` — today's Jina contract (this phase removes it)
- `.planning/codebase/STACK.md` — OkHttp, HTTPS only, `usesCleartextTraffic=false`
- `https://github.com/dergigi/boris-android/issues/54` — acceptance ideas and the 401 report

### Android reader
- `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` — `fetch`, `toProxyUrl`, `parse`, FORCE_CACHE
- `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt` — article model
- `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` — normalize, image/article URLs
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` — `ReaderUiState.Error`
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` — error UI, Open original
- `app/src/main/res/values/strings.xml` — `reader_open_original`
- `app/src/main/AndroidManifest.xml` — `INTERNET`, `usesCleartextTraffic=false`
- `app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt` — current Jina/HTML parse tests

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ReaderRepository.fetch`: only the `null` `NostrLink` branch (ordinary http(s)) should change. Article/Note/Profile/RSS stay.
- `ReadableContent` + `parse`: reader already accepts markdown or stripped HTML via `body`.
- `UrlExtractor.normalize` / `articleUrl` / `imageUrls`: keep HTTPS hygiene and relative image resolution.
- OkHttp client on `ReaderRepository` companion: reuse it; do not construct a client per request.
- `ReaderUiState.Error` + `reader_open_original`: two error strings, same Open original action.

### Established Patterns
- ViewModel catches exceptions and maps to `ReaderUiState.Error`. Repository throws `IOException` with a human-readable message.
- JVM unit tests with fixture strings, no Robolectric, no live network.
- No Hilt/Koin, no Room, no DataStore for this path.
- Package `org.dergigi.boris` only. Do not touch `com.readwithboris`.

### Integration Points
- `ReaderViewModel.load` → `ReaderRepository.fetch`
- Offline: `executeFromCache` / cache size helpers that currently key on `r.jina.ai/{url}`
- `OfflineStore.markDownloaded` after a successful fetch
- Images still load from origin via Coil

</code_context>

<specifics>
## Specific Ideas

- Reported symptom (Globe99, 2026-08-17): every external web page errors with 401 because the hosted URL-to-Markdown service refuses the request.
- Issue njump: `https://njump.to/nevent1qqsqqqru2dyv30putw0rn2dhn8efjlladls6d4pudjkw4s4x7d7v2mspzemhxue69uhhyetvv9ujuurjd9kkzmpwdejhgq3q47fp2j606qpfysp38phhzvempt7ewsdqwm6uww9uycp6tdvavu0sn38ls4`

</specifics>

<deferred>
## Deferred Ideas

- Hidden WebView / JS render for app-shell pages (D-10). Own phase if we ever need it.
- Changing the companion webapp off Jina. Android-only this phase (D-03).
- Restoring a remote extract fallback. Out of scope once D-01/D-04 land.

None else — discussion stayed within phase scope.

</deferred>

---

*Phase: 6-On-device article extraction*
*Context gathered: 2026-08-20*
