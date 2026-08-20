---
phase: 06-on-device-article-extraction
plan: 02
subsystem: reader
tags: [markdown, gfm-tables, footnotes, byline, thin-extract]
requires:
  - 06-01 (ArticleExtractor, origin GET, convert(element, baseUrl) overload)
provides:
  - GFM tables, footnote pairs, ordered lists, and byline in HtmlToMarkdown
  - Shared MIN_ARTICLE_MARKDOWN_CHARS = 500 for RSS and web extracts
  - parse.markdown null for thin and paywall-teaser bodies
affects:
  - 06-03 (maps null parse markdown to the D-13 no-article IOException)
tech-stack:
  added: []
  patterns:
    - regex string pipeline extension (no second converter library)
    - one internal companion constant shared across data-layer objects
key-files:
  created: []
  modified:
    - app/src/main/java/org/dergigi/boris/data/HtmlToMarkdown.kt
    - app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt
    - app/src/main/java/org/dergigi/boris/data/ArticleExtractor.kt
    - app/src/test/java/org/dergigi/boris/data/HtmlToMarkdownTest.kt
    - app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt
decisions:
  - "Footnote ids are renumbered sequentially ([^1], [^2]) by first reference; only sup refs with a matching li[id] become pairs, others keep today's plain-link rendering"
  - "MIN_ARTICLE_MARKDOWN_CHARS is internal in the ReaderRepository companion; ArticleExtractor node scoring reuses it so 500 exists exactly once"
  - "parse keeps the 06-01 full-document fallback but gates the final markdown on the 500 bar, so thin pages return null instead of a hollow Ready"
  - "Byline extraction reads the original html string before head strip, so meta name=author works even though the pipeline removes head"
metrics:
  duration: 7min
  completed: 2026-08-20
status: complete
actuals:
  tokens: 3000
  tasks: 2
  commits: 4
---

# Phase 6 Plan 02: Jina-like extract quality and the shared 500-character bar Summary

HtmlToMarkdown now emits GFM tables, Footnotes.expand-compatible footnote pairs, numbered ordered lists, and an obvious-author byline, while one shared 500-character constant fails thin and paywall-teaser extracts for both RSS and web parse.

## What was built

- **HtmlToMarkdown extras (D-05, D-06):**
  - GFM tables from `table`/`tr`/`th`/`td` with a `| --- |` separator after the header row; cells are tag-stripped after inline rules run, so bold/links inside cells survive as Markdown.
  - Footnotes: `sup > a[href^=#]` references with a matching `li[id]` become `[^n]` / `[^n]: text` pairs, renumbered sequentially so `Footnotes.expand` renders superscripts and the notes list. Refs without a matching definition keep the previous plain-link rendering.
  - Ordered lists: `<ol><li>` items become `1.` `2.` numbering via a rule placed before the generic dash-`li` rule; `<ul>` stays dashes.
  - Byline: `meta[name=author]` / `meta[itemprop=author]` content, or the text of a `rel=author` / `itemprop=author` element, is prepended as `*Name*` before the body. Read from the original html string before head-strip; no `ReadableContent` field added.
  - Image handling unchanged: relative `src` still resolves through `UrlExtractor.articleUrl` + `preferHttps` against the article URL (D-06).
- **Shared thin-extract bar (D-08, D-11, READ-04):** `MIN_RSS_MARKDOWN_CHARS` renamed to `internal MIN_ARTICLE_MARKDOWN_CHARS = 500` in the `ReaderRepository` companion. `rssContent` keeps rejecting teaser feed bodies with it, and `parse` now returns null markdown when the converted body (extractor output or full-document fallback) is under 500 characters. Paywall teasers fail by the same rule; no separate paywall detector. `ArticleExtractor` node scoring reuses the constant, so no second empty-extract number exists. No D-13 error copy thrown here; 06-03 maps null markdown to that IOException.
- **Tests:** `HtmlToMarkdownTest` gains table, footnote (including a `Footnotes.expand` round trip), ordered-list, and two byline tests. `ReaderRepositoryParseTest` gains `thinHtmlBodyYieldsNullMarkdown` and `paywallTeaserYieldsNullMarkdown`; `parsesHtmlFallback` and the image fixture are lengthened past 500 characters so they are no longer thin bodies. `ArticleExtractorTest` needed no change; its 500-char cases align with the shared constant.

## Commits

| Task | Phase | Commit |
|------|-------|--------|
| 1 | RED: failing converter tests | 80cc149 |
| 1 | GREEN: tables, footnotes, ordered lists, byline | aa9bd46 |
| 2 | RED: failing thin-extract tests | 1dbd02b |
| 2 | GREEN: shared 500-character bar | c8e1d64 |

## Verification

- `./gradlew --offline :app:testDebugUnitTest --tests ...HtmlToMarkdownTest --tests ...RssParserTest` exit 0
- `./gradlew --offline :app:testDebugUnitTest --tests ...ReaderRepositoryParseTest --tests ...ArticleExtractorTest --tests ...RssParserTest` exit 0
- Full `:app:testDebugUnitTest` suite: 544 tests, 0 failures
- Comment-stripped grep for `MIN_RSS_MARKDOWN_CHARS` in ReaderRepository.kt: 0
- No `ReadableContent.byline` field; RSS/Nostr fetch arms untouched apart from the constant rename (READ-04, READ-01)
- versionCode/versionName untouched; no new dependencies (T-06-SC)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Lengthened the image-fixture body too**
- **Found during:** Task 2
- **Issue:** The plan only named `parsesHtmlFallback`, but `htmlFallbackKeepsImagesInMarkdown` also used the two-character `Hi` body, which the new 500 bar nulls out.
- **Fix:** Reused the same 500-plus `longBody` string in that fixture so the image assertions still exercise a Ready article.
- **Files modified:** app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt
- **Commit:** 1dbd02b

## Known Stubs

None. No TODO/FIXME/placeholder text in changed files; all new behavior is wired and tested.

## TDD Gate Compliance

Both tasks ran RED then GREEN: `test(06-02)` commits 80cc149 and 1dbd02b each preceded their `feat(06-02)` commits aa9bd46 and c8e1d64, with failures confirmed before implementation.

## Self-Check: PASSED

Modified files exist on disk; commits 80cc149, aa9bd46, 1dbd02b, c8e1d64 present in git log.
