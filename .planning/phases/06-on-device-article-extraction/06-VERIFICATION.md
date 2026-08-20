---
phase: 06-on-device-article-extraction
verified: 2026-08-20T08:15:00Z
status: human_needed
score: 15/16 must-haves verified
behavior_unverified: 1
overrides_applied: 0
behavior_unverified_items:
  - truth: "D-15: Live origin fail plus OkHttp FORCE_CACHE hit on the origin URL serves that cached HTML through extract (thick cache is Ready, no cached badge)"
    test: "Load an https article once while online, enable airplane mode, reopen the same URL"
    expected: "Reader shows Ready with the extracted article, no cached badge; a never-cached URL shows Could not reach this page."
    why_human: "The FORCE_CACHE fallback code is present and wired (ReaderRepository.fetchOrigin lines 49-53) and the cache-miss arm is JVM-tested, but the cache-hit arm needs a real OkHttp disk cache populated by a prior request; the JVM stub client has no cache, so only a device test exercises this transition"
human_verification:
  - test: "Load an https article once while online, enable airplane mode, reopen the same URL"
    expected: "Ready with the cached extract if thick enough (D-15), no cached badge; thin cached extract shows Could not find an article on this page."
    why_human: "FORCE_CACHE hit path requires a populated on-device OkHttp disk cache; JVM tests only cover the miss path"
  - test: "Airplane mode on a never-cached URL"
    expected: "Could not reach this page. plus Try again and Open original buttons"
    why_human: "End-to-end offline flow on a device; the JVM test covers the repository mapping but not the rendered screen"
  - test: "Open a JS-shell or paywall-teaser page"
    expected: "Could not find an article on this page. plus the same Try again and Open original buttons"
    why_human: "Needs a real JS-heavy origin; JVM fixtures approximate but cannot reproduce live app-shell responses"
  - test: "Logged out, paste a working https article URL"
    expected: "Ready renders like today: title, byline if obvious, body, images; no login prompt"
    why_human: "Visual appearance and extract quality on real-world pages cannot be judged by grep or fixtures"
  - test: "Navigate the reader error and Ready states with TalkBack"
    expected: "TalkBack never announces library or proxy names (jsoup, Jina)"
    why_human: "Screen-reader output requires a device with TalkBack enabled"
---

# Phase 6: On-device article extraction Verification Report

**Phase Goal:** Ordinary http(s) articles load by fetching the page on-device and extracting readable Markdown locally, so reading does not depend on `r.jina.ai` being available or authenticated. Nostr and RSS paths stay as they are. Offline cache still works. GitHub issue #54.
**Verified:** 2026-08-20T08:15:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

Merged from the three PLAN frontmatter `must_haves.truths` blocks (ROADMAP Phase 6 defines no separate Success Criteria list; the goal decomposes into these D-decisions plus READ-04/READ-01).

| #   | Truth | Status | Evidence |
| --- | ----- | ------ | -------- |
| 1 | D-01: Ordinary http(s) loads GET the origin URL and extract Markdown on-device; never the remote proxy | ✓ VERIFIED | `ReaderRepository.fetch` null arm calls `fetchOrigin(preferHttps(normalize(url)))`; repo-wide grep for `r.jina.ai`/`toProxyUrl` in `app/src/main` returns 0 |
| 2 | D-02: cachedBodyBytes and FORCE_CACHE use only the origin URL after preferHttps | ✓ VERIFIED | `cachedBodyBytes` candidates = `listOf(UrlExtractor.preferHttps(normalized))` (ReaderRepository.kt:265); `executeFromCache` reuses `originRequest` |
| 3 | D-03: Android extract is in-repo ArticleExtractor plus jsoup 1.23.1 | ✓ VERIFIED | `ArticleExtractor.kt` exists (35 lines, substantive); `libs.versions.toml` pins `jsoup = "1.23.1"` and `desugarJdkLibsNio = "2.1.5"`; `app/build.gradle.kts` wires both |
| 4 | D-04: The Jina URL builder is gone; no dormant proxy path | ✓ VERIFIED | Case-insensitive grep for `jina`, `toProxyUrl`, `readability4j`, `crux` across `app/src/main`: 0 matches |
| 5 | D-05: Extracted Markdown includes headings, lists, blockquotes, code, GFM tables, footnotes, ordered lists, byline, images | ✓ VERIFIED | `HtmlToMarkdown` has table (line 78), ol-numbering (line 98), footnote-pair (lines 27-44), and byline (line 126) rules; `HtmlToMarkdownTest` covers each, all passing |
| 6 | D-06: Relative img src resolves against the article URL and stays origin https | ✓ VERIFIED | `HtmlToMarkdown.image` uses `UrlExtractor.articleUrl` + `preferHttps`; `relativeImageResolvesAgainstBaseUrl` and `htmlFallbackKeepsImagesInMarkdown` pass |
| 7 | D-07: Nav, ads, comments dropped; article content node kept | ✓ VERIFIED | CHROME selector strips script/style/iframe/nav/footer/ads/comments; `parseStripsChromeFromWrappedArticle` asserts `Home` and `nope` absent, passing |
| 8 | D-08/D-11: Extract under 500 Markdown chars (including paywall teasers) is a failure, one shared constant | ✓ VERIFIED | Single `MIN_ARTICLE_MARKDOWN_CHARS = 500` (ReaderRepository.kt:310) used by `parse`, `rssContent`, and `ArticleExtractor.score`; `MIN_RSS_MARKDOWN_CHARS` grep = 0; `thinHtmlBodyYieldsNullMarkdown` and `paywallTeaserYieldsNullMarkdown` pass |
| 9 | D-09: First GET sends Boris UA; on 401/403 or empty/thin extract, one BROWSER_UA retry, then fail | ✓ VERIFIED | Behavior-dependent truth with behavioral evidence: `fetchRetriesWithBrowserUaWhenBlocked` and `fetchMapsThinExtractToNoArticleAfterUaRetry` assert the exact `[BORIS_UA, BROWSER_UA]` sequence through a stubbed client, both passing |
| 10 | D-10: HTTP plus HTML only; no script execution, no renderer view | ✓ VERIFIED | `Jsoup.parse` only (no `Jsoup.connect`); no `WebView` anywhere in `org.dergigi.boris`; retry is a second `client.newCall` |
| 11 | D-12/D-13: Exactly two reader error sentences with locked copy and keys; no library/proxy names on screen | ✓ VERIFIED | `ERROR_UNREACHABLE`/`ERROR_NO_ARTICLE` are the only IOException messages leaving the web arm; `strings.xml` lines 180-181 carry both keys verbatim; old `Failed to fetch readable content` / `Failed to load this article.` greps = 0 |
| 12 | D-14: Open original stays on both errors when url is not blank, same outlined button | ✓ VERIFIED | ReaderScreen.kt Error column (lines 841-869): centered `bodyLarge` message, filled Try again, `OutlinedButton` with `stringResource(R.string.reader_open_original)` gated on `state.url.isNotBlank()` |
| 13 | D-15: Live fail plus origin FORCE_CACHE hit serves cached HTML through extract; thin cache no-article; miss unreachable | ⚠️ PRESENT_BEHAVIOR_UNVERIFIED | Code present and wired (`fetchOrigin` lines 49-53: cache probe, re-extract, thin→`ERROR_NO_ARTICLE`); miss path tested (`fetchMapsLiveFailWithoutCacheToUnreachable` passes); the cache-HIT transition needs a populated on-device disk cache — see Human Verification |
| 14 | READ-04: Nostr Article/Note/Profile and rssContent arms stay as they are; RSS still calls `convert(html)` without ArticleExtractor | ✓ VERIFIED | `fetch` when-arms for Article/Note/Profile unchanged; `rssContent` calls `HtmlToMarkdown.convert(html)` (line 192) with no extractor; `RssParserTest` and `noteMarkdownEmbedsImageLinksAndKeepsLineBreaks` pass |
| 15 | READ-01: Reading stays ungated — `ReaderViewModel.load` calls fetch with no session check; blank URL stays "No URL to read." | ✓ VERIFIED | `load()` (ReaderViewModel.kt:166-225) has no `SessionStore` gate before `repository.fetch`; blank URL yields `Error("No URL to read.", url)`; null-message fallback is `"Could not reach this page."` |
| 16 | UI states: existing Loading spinner (no Retrying label), one error sentence at a time, existing Error column, no new chrome | ✓ VERIFIED | No `Retrying` string in the codebase; Error column renders exactly one `Text(state.message)`; no new sealed variants, icons, snackbars, or third button in ReaderScreen |

**Score:** 15/16 truths verified (1 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `gradle/libs.versions.toml` | jsoup 1.23.1 + desugarJdkLibsNio 2.1.5 pins | ✓ VERIFIED | Both versions and both library entries present (lines 13-14, 35-36) |
| `app/src/main/java/org/dergigi/boris/data/ArticleExtractor.kt` | Chrome strip and article node scoring | ✓ VERIFIED | Exists, substantive, wired: imported by `ReaderRepository.parse` and tested by `ArticleExtractorTest` |
| `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` | Origin GET, origin cache key, UA retry, two errors, 2 MiB cap, FORCE_CACHE | ✓ VERIFIED | `fetchOrigin`/`originAttempt`/`readCapped` (`MAX_BODY_BYTES = 2 * 1024 * 1024L`)/`executeFromCache` all present and reached from `fetch` |
| `app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt` | GFM tables, footnotes, ordered lists, byline, element overload | ✓ VERIFIED | All four D-05 rules plus `convert(element, baseUrl)` overload present; wired from ArticleExtractor and rssContent |
| `app/src/main/java/org/dergigi/boris/data/HttpUserAgents.kt` | BORIS_UA and BROWSER_UA | ✓ VERIFIED | Both constants present; used by ReaderRepository and OgMetaClient |
| `app/src/main/res/values/strings.xml` | reader_error_unreachable, reader_error_no_article | ✓ VERIFIED | Both keys with the exact D-13 sentences (lines 180-181) |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | -- | --- | ------ | ------- |
| ReaderRepository | ArticleExtractor | `ArticleExtractor.markdown` in parse | ✓ WIRED | ReaderRepository.kt:215 |
| ArticleExtractor | HtmlToMarkdown | `HtmlToMarkdown.convert(element, baseUrl)` | ✓ WIRED | ArticleExtractor.kt:16 |
| ReaderRepository | UrlExtractor | `preferHttps(normalize(url))` as live GET / FORCE_CACHE / cachedBodyBytes key | ✓ WIRED | ReaderRepository.kt:26, 265 |
| HtmlToMarkdown | Footnotes | Emits `[^n]` / `[^n]: text` matching Footnotes REFERENCE/DEFINITION regexes | ✓ WIRED | Emission shape confirmed against Footnotes.kt:4-5; round-trip test `convertsFootnotePairsThatFootnotesExpands` passes |
| ReaderRepository | ArticleExtractor | Reject converted markdown under `MIN_ARTICLE_MARKDOWN_CHARS` | ✓ WIRED | `takeIf { it.length >= MIN_ARTICLE_MARKDOWN_CHARS }` in parse; score reuses the constant |
| ReaderRepository | ReaderViewModel | IOException message becomes `ReaderUiState.Error.message` | ✓ WIRED | VM catch block maps `e.message` with `"Could not reach this page."` fallback |
| ReaderViewModel | ReaderScreen | Error column shows message + Try again + Open original | ✓ WIRED | ReaderScreen.kt:841-869 |
| OgMetaClient | HttpUserAgents | OG fetch uses the shared BROWSER_UA | ✓ WIRED | OgMetaClient.kt:18 |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| ReaderScreen Ready body | `content.markdown` | origin HTTP body → ArticleExtractor → HtmlToMarkdown → parse | Yes (behavioral fetch test returns real extracted markdown through a stub network) | ✓ FLOWING |
| ReaderScreen Error message | `state.message` | Repository IOException constants → VM catch → Error state | Yes (fetch tests assert both exact sentences propagate) | ✓ FLOWING |
| Offline reopen body | FORCE_CACHE response → parse | On-device OkHttp disk cache | Wired; hit path unexercised in JVM | ⚠️ device UAT |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Chrome strip, thin bar, image resolution, tables/footnotes/ol/byline, UA retry, error mapping, RSS regression | `./gradlew --offline :app:testDebugUnitTest --tests ...ArticleExtractorTest --tests ...ReaderRepositoryParseTest --tests ...HtmlToMarkdownTest --tests ...ArticleCoverTest --tests ...PublishedTimeTest --tests ...RssParserTest` | BUILD SUCCESSFUL, exit 0 | ✓ PASS |
| Full-suite regression | Full `:app:testDebugUnitTest` (run by orchestrator before this verification) | Green per environment gate | ✓ PASS (not re-run per single-full-run rule) |

### Probe Execution

No `scripts/*/tests/probe-*.sh` probes exist in this repository and no PLAN declares any. SKIPPED (no probes declared).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| READ-04 | 06-01, 06-02, 06-03 | Origin fetch + local extract, no `r.jina.ai`, Nostr/RSS unchanged, cached extract opens offline, no login | ✓ SATISFIED (offline-cache clause routed to human UAT) | Truths 1-13; the "previously cached local extract still opens offline" clause is the D-15 cache-hit device test |
| READ-01 | 06-01, 06-02, 06-03 | Read while logged out; login UI never blocks reading | ✓ SATISFIED | Truth 15; no session check on the fetch path |

No orphaned requirements: REQUIREMENTS.md maps exactly READ-04 and READ-01 (must-not-regress) to Phase 6, and all three plans declare both.

### Prohibitions

All PLAN `must_haves.prohibitions` are explicit-verification tier and all hold:

| Prohibition | Status | Evidence |
| ----------- | ------ | -------- |
| No versionCode/versionName bump, no release | ✓ HELD | `versionCode = 125`, `versionName = "1.4.18"` unchanged; no release commits in phase range |
| No Readability4J, Crux, Hilt, Koin, Room, second OkHttpClient | ✓ HELD | Catalog grep 0; retry and OG fetch reuse existing clients |
| No `com.readwithboris` edits | ✓ HELD | All phase commits touch `org.dergigi.boris` and build files only |
| Nostr/RSS bodies unchanged except shared 500-char rename | ✓ HELD | fetchArticle/fetchNote/rssContent intact; only the constant rename touches rssContent |
| No `Jsoup.connect` | ✓ HELD | Grep 0; `Jsoup.parse` only |
| No flexmark/Remark second converter, no ReadableContent.byline field, no second empty-extract number | ✓ HELD | Single regex pipeline; byline prepended into markdown string; one 500 constant |
| No new Error sealed type, icon, snackbar, third button; no proxy/library names in UI copy | ✓ HELD | ReaderUiState unchanged; strings grep clean (the pre-existing About-page "a focus on readability" is the English noun, accepted false positive) |
| No leftover Jina parse helpers as test fixtures | ✓ HELD | `jina` grep across `app/src/test`: 0 matches |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | none | — | No TODO/FIXME/XXX/TBD/placeholder markers in any phase-modified file (the `XXX` in Rss.kt is a pre-existing date-format pattern, not a debt marker) |

All nine SUMMARY commit hashes (a3f3f43, 6fc157f, 80cc149, aa9bd46, 1dbd02b, c8e1d64, fd8d95f, 536f5f8, cbce3b3) exist in git log.

### Human Verification Required

Harvested from the 06-03 PLAN `<human-check>` block (deferred to end-of-phase per `human_verify_mode: end-of-phase`) plus the D-15 behavior-unverified truth.

### 1. Offline cache hit (D-15)

**Test:** Load an https article once while online, enable airplane mode, reopen the same URL.
**Expected:** Ready with the cached extract if thick enough, no cached badge. A thin cached extract shows "Could not find an article on this page."
**Why human:** The FORCE_CACHE hit path needs a populated on-device OkHttp disk cache; JVM stub clients have no cache, so only the miss path is test-covered.

### 2. Offline never-cached URL

**Test:** Airplane mode, open a URL that was never loaded.
**Expected:** "Could not reach this page." plus Try again and Open original.
**Why human:** End-to-end offline flow on the rendered screen; JVM tests cover the repository mapping only.

### 3. JS-shell / teaser page

**Test:** Open a JS-heavy app-shell or paywall-teaser page.
**Expected:** "Could not find an article on this page." plus the same two buttons.
**Why human:** Requires a live JS-heavy origin; fixtures approximate but cannot reproduce real app-shell responses.

### 4. Logged-out real-world read

**Test:** Logged out, paste a working https article URL.
**Expected:** Ready looks like today: title, byline if obvious, body, images; no login prompt.
**Why human:** Extract quality and visual appearance on real pages cannot be judged programmatically.

### 5. TalkBack accessibility

**Test:** Navigate reader error and Ready states with TalkBack.
**Expected:** TalkBack never announces library or proxy names.
**Why human:** Screen-reader output requires a device with TalkBack enabled.

### Gaps Summary

No gaps. All production code the phase promised exists, is substantive, and is wired end-to-end: the proxy path is fully deleted, the origin GET + jsoup extract pipeline is behaviorally tested (including the UA retry sequence and both locked error sentences through a stubbed network), the shared 500-character bar fails thin and paywall bodies, and Nostr/RSS arms are untouched. The single behavior-unverified truth is the D-15 offline cache-hit transition, which structurally cannot be exercised on the JVM and is queued for device UAT along with the four other deferred human checks.

---

_Verified: 2026-08-20T08:15:00Z_
_Verifier: Claude (gsd-verifier)_
