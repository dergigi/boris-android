---
phase: 06-on-device-article-extraction
plan: 03
subsystem: reader
tags: [user-agent, retry, error-copy, okhttp-cache, jina-cleanup]
requires:
  - 06-01 (ArticleExtractor, origin GET, origin-only cache keys)
  - 06-02 (MIN_ARTICLE_MARKDOWN_CHARS bar, parse.markdown null on thin extracts)
provides:
  - HttpUserAgents with shared BORIS_UA and BROWSER_UA
  - One browser-UA retry on 401/403 or empty/thin extract (D-09, D-10)
  - Two locked reader error sentences as repository IOExceptions (D-12, D-13)
  - Origin FORCE_CACHE fallback on live fail (D-15)
  - 2 MiB response body cap before parse
  - Jina proxy header helpers deleted from ArticleCover and PublishedTime
affects: []
tech-stack:
  added: []
  patterns:
    - private sealed OriginResult inside the repository maps fetch outcomes to exactly two error sentences
    - stubbed OkHttp application interceptor for network-free fetch tests
key-files:
  created:
    - app/src/main/java/org/dergigi/boris/data/HttpUserAgents.kt
  modified:
    - app/src/main/java/org/dergigi/boris/data/OgMetaClient.kt
    - app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt
    - app/src/main/java/org/dergigi/boris/data/ArticleCover.kt
    - app/src/main/java/org/dergigi/boris/data/PublishedTime.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt
    - app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/org/dergigi/boris/data/ArticleCoverTest.kt
    - app/src/test/java/org/dergigi/boris/data/PublishedTimeTest.kt
    - app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt
decisions:
  - "OriginResult sealed interface keeps the outcome table private to ReaderRepository; only ERROR_UNREACHABLE and ERROR_NO_ARTICLE leave the class"
  - "The network interceptor now rewrites Cache-Control only on successful responses, so a flaky 404 can no longer overwrite a good cached copy (D-15)"
  - "looksLikeHtml reads the Content-Type header leniently: missing header is treated as HTML, image/pdf/json bodies become no-article"
  - "Fetch tests stub OkHttp with an application interceptor and og:image/og:description fixtures so withCover never reaches the network"
metrics:
  duration: 12min
  completed: 2026-08-20
status: complete
actuals:
  tokens: 3200
  tasks: 2
  commits: 3
---

# Phase 6 Plan 03: Blocked-page retry, locked error copy, and proxy helper cleanup Summary

One honest Boris-UA GET with a single Chrome-UA retry on 401/403 or thin extracts, exactly two locked reader error sentences end to end, a 2 MiB body cap with origin FORCE_CACHE fallback, and the last Jina proxy helpers deleted.

## What was built

- **HttpUserAgents (D-09):** `BORIS_UA` (`Boris/{VERSION_NAME} (Android; +https://github.com/dergigi/boris-android)`) and `BROWSER_UA` (the Chrome Mobile string moved verbatim from `OgMetaClient`, which now points at the shared constant).
- **ReaderRepository fetch mapping (D-09, D-10, D-12, D-13, D-15):** the null NostrLink arm now runs `fetchOrigin`: first GET with `BORIS_UA`; on 401/403 or an empty/thin/non-HTML result, one rebuild with `BROWSER_UA` on the same `defaultClient` (a second GET, no renderer view); then fail. Live fail or non-2xx after retry probes the origin request with `FORCE_CACHE`: a thick cached extract is Ready, a thin one throws `ERROR_NO_ARTICLE`, a miss throws `ERROR_UNREACHABLE`. Bodies are capped at 2 MiB via a buffered-source read before parse. `execute` and its `Failed to fetch readable content (code)` message are gone; no status codes or library names reach the UI. The cache interceptor rewrites `Cache-Control: public, max-age=300` only on successful responses.
- **Error surface (D-13, D-14, READ-01):** `reader_error_unreachable` and `reader_error_no_article` string resources carry the two locked sentences; the repository Kotlin constants match them exactly so JVM tests need no Resources. `ReaderViewModel` still shows `e.message` and its null-message fallback is now `Could not reach this page.`; blank URL stays `No URL to read.`; no session gate added. `ReaderScreen` keeps the same Error column and buttons, with Open original now using `stringResource(R.string.reader_open_original)`.
- **Jina helper deletion:** `ArticleCover.imageFromJina`, `descriptionFromJina`, their header regexes and `header()`, plus `PublishedTime.fromJinaHeader` and `headerField` are deleted. `stripLeadingImage`, `firstMarkdownImage`, `fromHtml`, `parse`, and `label` stay. Matching header tests removed; `fromHtmlMeta` and the image/strip tests kept.
- **Tests:** `ReaderRepositoryParseTest` gains three network-free fetch tests through a stubbed OkHttp interceptor: thin extract maps to the no-article sentence after a Boris-then-Chrome UA sequence, live fail without cache maps to the unreachable sentence, and a 403 first pass succeeds on the browser-UA retry.

## Commits

| Task | Phase | Commit |
|------|-------|--------|
| 1 | UA retry, two errors, body cap, origin cache fallback | fd8d95f |
| 2 | tests: pin error sentences, drop proxy header fixtures | 536f5f8 |
| 2 | impl: strings, ViewModel fallback, helper deletion | cbce3b3 |

## Verification

- `./gradlew --offline :app:assembleDebug` exit 0 (after Task 1 and again after Task 2)
- Named unit tests (`ArticleCoverTest`, `PublishedTimeTest`, `ReaderRepositoryParseTest`, `ArticleExtractorTest`, `HtmlToMarkdownTest`) exit 0; full `:app:testDebugUnitTest` suite green
- Comment-stripped greps: `2 * 1024 * 1024` 1, `FORCE_CACHE` 2, `BROWSER_UA` 1, `Failed to fetch readable content` 0 in ReaderRepository; `Failed to load this article.` 0 in ReaderViewModel; `imageFromJina` 0 in ArticleCover; `fromJinaHeader` 0 in PublishedTime
- `ReaderScreen.kt` still contains `openOriginal` and `Try again`; Error column layout untouched
- The task 2 `<human-check>` (device UAT: airplane-mode cache hit, JS-shell page, TalkBack) is deferred to end-of-phase verification per `human_verify_mode: end-of-phase`

## Deviations from Plan

**1. Acceptance-grep false positive left in place (strings.xml library-name check)**
- **Found during:** Task 2 acceptance criteria
- **Issue:** `grep -ciE 'jina|jsoup|readability'` on strings.xml counts 1 because the pre-existing About-page copy (commit 1d5ac38, long before this phase) says "a focus on readability" — the English noun, not the extract library. The two new error strings contain no proxy or library names.
- **Fix:** None; rewording approved About copy is outside this plan's scope boundary.
- **Files modified:** none

## TDD Gate Compliance

Task 2 ran test-first (536f5f8 before cbce3b3), but the RED run was green: the fetch error-mapping the new tests pin down was implemented by Task 1 of this same plan, and the remaining Task 2 changes (string resources, ViewModel fallback, helper deletion) are not observable from JVM unit tests. The tests act as locking-in coverage for the D-13 sentences and the D-09 UA sequence rather than a failing-first cycle.

## Known Stubs

None. No TODO/FIXME/placeholder text in changed files; both error paths are wired end to end.

## Threat Flags

None. All mitigations from the plan's threat register landed: cached HTML re-runs the same ArticleExtractor with no renderer view (T-06-01), 2 MiB body cap before parse (T-06-04), honest Boris UA first with one browser-UA retry (T-06-07), only the two D-13 sentences reach ReaderUiState.Error (T-06-08), and no new packages were added (T-06-SC).

## Self-Check: PASSED

Created and modified files exist on disk; commits fd8d95f, 536f5f8, cbce3b3 present in git log.
