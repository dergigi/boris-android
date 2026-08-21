# Phase 6: On-device article extraction - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-20
**Phase:** 6-On-device article extraction
**Areas discussed:** Jina leftover, Quality bar, Blocked / JS-heavy pages, Error copy

---

## Jina leftover

| Option | Description | Selected |
|--------|-------------|----------|
| Gone this phase | Fetch the origin and extract locally; no r.jina.ai call | ✓ |
| Local first, Jina last resort | Only if on-device extract finds no article | |
| You decide | | |

**User's choice:** Gone this phase
**Notes:** 401 is the hosted service. A Jina fallback still leaves that failure mode.

| Option | Description | Selected |
|--------|-------------|----------|
| Ignore leftover Jina cache | New loads fetch the origin; old entries age out | ✓ |
| Still serve cached Jina bodies | Then switch to local on miss | |
| You decide | | |

**User's choice:** Ignore leftover Jina cache

| Option | Description | Selected |
|--------|-------------|----------|
| Android may diverge from the webapp | On-device extract is the Android product | ✓ |
| Stay close to the webapp | Same article should look the same on both | |
| You decide | | |

**User's choice:** Android may diverge

| Option | Description | Selected |
|--------|-------------|----------|
| Remove the Jina URL builder | No dormant proxy path | ✓ |
| Keep the Jina markdown parser | In case a fallback returns later | |
| You decide | | |

**User's choice:** Remove the Jina URL builder

---

## Quality bar

| Option | Description | Selected |
|--------|-------------|----------|
| Title, byline, body, images | Calm-read minimum | |
| Full Jina-like markdown | Headings, lists, blockquotes, code, tables, footnotes | ✓ |
| You decide | | |

**User's choice:** Full Jina-like markdown

| Option | Description | Selected |
|--------|-------------|----------|
| Keep origin image URLs | Resolve relative src against the article URL | ✓ |
| Only absolute https images | Drop relative ones | |
| You decide | | |

**User's choice:** Keep origin image URLs

| Option | Description | Selected |
|--------|-------------|----------|
| Strip chrome | Keep article content node; drop nav/ads/comments | ✓ |
| Prefer main/article as-is | Chrome may leak | |
| You decide | | |

**User's choice:** Strip chrome

| Option | Description | Selected |
|--------|-------------|----------|
| Thin extract is a failure | Error + Open original | ✓ |
| Show whatever we got | Thin page beats an error | |
| You decide | | |

**User's choice:** Thin extract is a failure

---

## Blocked / JS-heavy pages

| Option | Description | Selected |
|--------|-------------|----------|
| Fail + Open original | Do not impersonate a browser | |
| Retry once with a browser-like UA | Then fail + Open original | ✓ |
| You decide | | |

**User's choice:** Retry once with a browser-like User-Agent

| Option | Description | Selected |
|--------|-------------|----------|
| No JavaScript | HTTP + HTML only | ✓ |
| Hidden WebView | Extract after render | |
| You decide | | |

**User's choice:** No JavaScript

| Option | Description | Selected |
|--------|-------------|----------|
| Same thin-extract rule for paywalls | No special detector | ✓ |
| Show the teaser | If any paragraphs exist | |
| You decide | | |

**User's choice:** Same thin-extract rule

| Option | Description | Selected |
|--------|-------------|----------|
| Honest Boris UA first | Browser-like only on retry | ✓ |
| Browser-like UA from the first request | | |
| You decide | | |

**User's choice:** Honest Boris UA first

---

## Error copy

| Option | Description | Selected |
|--------|-------------|----------|
| Two errors | Fetch failed vs no article found | ✓ |
| One generic error | Could not load article | |
| You decide | | |

**User's choice:** Two errors

| Option | Description | Selected |
|--------|-------------|----------|
| Open original on both | | ✓ |
| Open original only on no-article | | |
| You decide | | |

**User's choice:** Open original on both

| Option | Description | Selected |
|--------|-------------|----------|
| Serve cached local extract on live-fetch miss | Same idea as FORCE_CACHE today | ✓ |
| Show fetch-failed even if cache exists | | |
| You decide | | |

**User's choice:** Serve cached local extract

| Option | Description | Selected |
|--------|-------------|----------|
| Plain copy | Could not reach this page. / Could not find an article on this page. | ✓ |
| Write strings later | | |
| You decide | | |

**User's choice:** Plain copy

---

## Claude's Discretion

- Extract library vs in-repo HTML-to-Markdown
- Empty-extract threshold
- Exact User-Agent strings
- OkHttp cache-key migration off Jina URLs
- Whether Jina-format parse tests stay as fixtures

## Deferred Ideas

- Hidden WebView / JS render for app-shell pages
- Moving the companion webapp off Jina
- Restoring a remote extract fallback
