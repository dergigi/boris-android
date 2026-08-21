---
phase: 06-on-device-article-extraction
plan: 01
subsystem: reader
tags: [jsoup, okhttp, article-extraction, markdown]
requires: []
provides:
  - ArticleExtractor chrome strip and article-node scoring
  - Origin GET with Boris User-Agent in the null NostrLink arm
  - Origin-only OkHttp cache keys (live GET, FORCE_CACHE, cachedBodyBytes)
  - HtmlToMarkdown.convert(element, baseUrl) overload
affects:
  - 06-02 (tables, footnotes, ordered lists, thin-extract fail, UA retry)
  - 06-03 (D-13 error copy, Jina helper deletion in ArticleCover/PublishedTime)
tech-stack:
  added:
    - org.jsoup:jsoup 1.23.1
    - com.android.tools:desugar_jdk_libs_nio 2.1.5 (core library desugaring)
  patterns:
    - stateless data-layer object (ArticleExtractor mirrors HtmlToMarkdown)
    - repository throws, ViewModel maps (unchanged)
key-files:
  created:
    - app/src/main/java/org/dergigi/boris/data/ArticleExtractor.kt
    - app/src/test/java/org/dergigi/boris/data/ArticleExtractorTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt
    - app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt
    - app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt
    - app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt
decisions:
  - "parse falls back to full-document HtmlToMarkdown.convert when ArticleExtractor returns null, so short HTML still parses this plan; the 500-char thin-extract fail lands in 06-02"
  - "BORIS_UA lives as a private companion val in ReaderRepository; the shared HttpUserAgents object arrives with the UA retry in 06-02"
metrics:
  duration: 50min
  completed: 2026-08-20
status: complete
actuals:
  tokens: 4500
  tasks: 2
  commits: 2
---

# Phase 6 Plan 01: End-to-end origin GET to extracted Markdown Summary

Origin GET plus in-repo jsoup ArticleExtractor replaces the r.jina.ai proxy on the ordinary web read path, with origin-only cache keys and JVM fixtures for chrome strip and relative images.

## What was built

- **Catalog pins (Wave 0):** jsoup 1.23.1 and desugar_jdk_libs_nio 2.1.5 in `gradle/libs.versions.toml`; core library desugaring enabled in `app/build.gradle.kts`. No version bump, no release.
- **ArticleExtractor (D-03, D-07, D-10):** `Jsoup.parse(html, baseUrl)` only, never a jsoup HTTP connect. CHROME selector removes script/style/iframe/nav/footer/comments/ads; best ARTICLE candidate by score, else body. Score is 0 under 500 text chars or above 0.5 link-text density; `article()`/`markdown()` return null when the best node scores 0.
- **HtmlToMarkdown:** new `convert(element, baseUrl)` overload forwards to the existing string pipeline via `element.outerHtml()`, so RSS keeps calling `convert(html)` unchanged.
- **ReaderRepository (D-01, D-02, D-04, D-09, D-15):** the null NostrLink arm GETs `preferHttps(normalize(url))` with `User-Agent: Boris/{VERSION_NAME} (Android; +https://github.com/dergigi/boris-android)` and `Accept: text/html,application/xhtml+xml`. `executeFromCache` reuses the same origin request. `parse` extracts via `ArticleExtractor.markdown` (falling back to full-document convert for short HTML) plus existing OgMeta / PublishedTime.fromHtml / ArticleCover.stripLeadingImage / upgradeImageHttpUrls. `toProxyUrl` and the Jina `Markdown Content:` regex branch are deleted; `cachedBodyBytes` probes only the https origin URL. Nostr Article/Note/Profile and rssContent arms untouched (READ-04, READ-01).
- **Tests:** `ArticleExtractorTest` covers chrome strip, 500-char floor, link density, and relative image resolution against baseUri. `ReaderRepositoryParseTest` drops the three Jina-format cases and adds a chrome-wrapped fixture through `repository.parse` whose markdown omits `Home` and `nope`. `UrlExtractor.preferHttps` comment no longer names the proxy.

## Commits

| Task | Name | Commit |
|------|------|--------|
| 1 (tracer) | End-to-end origin GET to extracted Markdown | a3f3f43 |
| 2 | JVM fixtures for chrome strip, relative images, and HTML parse | 6fc157f |

## Verification

- `./gradlew :app:assembleDebug` exit 0 (tracer verify, run end-to-end after all task 1 changes)
- `./gradlew :app:testDebugUnitTest --tests ...ArticleExtractorTest --tests ...ReaderRepositoryParseTest --tests ...HtmlToMarkdownTest --tests ...RssParserTest` exit 0
- Full `:app:testDebugUnitTest` suite green (regression guard)
- Comment-stripped greps: `toProxyUrl` 0, `r.jina.ai` 0 in ReaderRepository; `Jsoup.connect` 0 in ArticleExtractor; `readability4j|crux` 0 in the catalog; `jina` 0 in UrlExtractor
- Nostr/RSS fetch arms and ReaderViewModel.load untouched (READ-01 stays ungated)

## Deviations from Plan

None to code - plan executed as written. One environment note: the first online Gradle run hung twice on dependency resolution because dl.google.com connections stall in this environment. Resolved by killing the daemon and rebuilding with `-Dorg.gradle.internal.http.connectionTimeout=10000 -Dorg.gradle.internal.repository.max.tentatives=1`, which blacklists the unreachable Google repo after one failure and fails over to Maven Central / the Huawei mirror. Both new artifacts then downloaded and subsequent runs used `--offline`.

## Known Stubs

None. Jina-only helpers still present in production (`ArticleCover.imageFromJina`, `descriptionFromJina`, `PublishedTime.fromJinaHeader`) are unreferenced from the fetch path and scheduled for deletion in 06-03; they are existing code, not new stubs.

## Threat Flags

None. All five threat-register mitigations landed as planned: script/iframe dropped by CHROME (T-06-01), image src resolved via articleUrl + preferHttps (T-06-02), preferHttps on the article GET (T-06-03), proxy builder deleted (T-06-06), and only the two officially pinned artifacts added (T-06-SC).

## Self-Check: PASSED

Created files exist on disk; commits a3f3f43 and 6fc157f present in git log.
