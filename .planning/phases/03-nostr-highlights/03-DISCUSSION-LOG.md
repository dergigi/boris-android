# Phase 3: Nostr highlights - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-14
**Phase:** 3-Nostr highlights
**Areas discussed:** Whose marks, Event shape, Create gesture, Who signs

---

## Whose marks

| Option | Description | Selected |
|--------|-------------|----------|
| Everyone for this URL | Lantern; kind 9802 `#r` = URL | |
| Only yours | Nothing to paint until logged in | ✓ |
| You decide | | |

**User's choice:** Only yours.
**Notes:** Logged out, the article looks like today.

| Option | Description | Selected |
|--------|-------------|----------|
| Keep reading | No marks, no extra error on relay miss | ✓ |
| Quiet notice | Non-blocking "highlights didn't load" | |
| You decide | | |

**User's choice:** Keep reading.

| Option | Description | Selected |
|--------|-------------|----------|
| Immediately then publish | Drop the mark if publish fails | ✓ |
| After the relay accepts | | |
| You decide | | |

**User's choice:** Immediately.

| Option | Description | Selected |
|--------|-------------|----------|
| Exact `r` string | Trailing slash mismatch misses | |
| Normalize | https, drop www / trailing slash | ✓ |
| You decide | | |

**User's choice:** Normalize. Later also strip query and fragment.

| Option | Description | Selected |
|--------|-------------|----------|
| Skip unmatched quotes | Jina body often differs from the original page | ✓ |
| Loosen matching | Whitespace / quotes | |
| You decide | | |

**User's choice:** Skip.

| Option | Description | Selected |
|--------|-------------|----------|
| Both duplicates | Two events, opacity stacks | ✓ |
| Dedupe | One mark if quote text matches | |
| You decide | | |

**User's choice:** Both, with opacity so multiple draws read stronger.

| Option | Description | Selected |
|--------|-------------|----------|
| Home / webapp yellow `#FDE047` with alpha | Reuse `HighlightMine` | ✓ |
| Classic highlighter yellow | Separate from Home copy | |
| You decide | | |

**User's choice:** `#FDE047` with alpha. Home copy marks use the same opacity style.

---

## Event shape

| Option | Description | Selected |
|--------|-------------|----------|
| NIP-84 like the Boris webapp | quote, `r`, `context`, `alt` | ✓ |
| Lantern extra selector tags | `textquoteselector`, ranges | |
| You decide | | |

**User's choice:** NIP-84 like the webapp.

| Option | Description | Selected |
|--------|-------------|----------|
| Include `context` | Slice of body around the quote | ✓ |
| Skip `context` | quote + `r` only | |
| You decide | | |

**User's choice:** Include context.

| Option | Description | Selected |
|--------|-------------|----------|
| Same alt as webapp | `Highlight created by Boris. read.withboris.com` | |
| Skip `alt` | | |
| Custom | | ✓ |

**User's choice:** `Highlight created by Boris Android. readwithboris.com` (https://www.readwithboris.com/). Webapp alt unchanged.

| Option | Description | Selected |
|--------|-------------|----------|
| Paint any of yours for this URL | content matches body | ✓ |
| Only Boris-shaped events | require `context` or our `alt` | |
| You decide | | |

**User's choice:** Any of yours for this URL.

---

## Create gesture

| Option | Description | Selected |
|--------|-------------|----------|
| Highlight in the selection toolbar | Next to Copy | ✓ |
| Highlight chip above the selection | | |
| You decide | | |

**User's choice:** Toolbar. Hidden when logged out.

| Option | Description | Selected |
|--------|-------------|----------|
| Straight to sign | No comment box | ✓ |
| Confirm sheet with optional note | | |
| You decide | | |

**User's choice:** Straight to sign.

| Option | Description | Selected |
|--------|-------------|----------|
| Body only | | |
| Title and body | | ✓ |
| You decide | | |

**User's choice:** Title and body.

| Option | Description | Selected |
|--------|-------------|----------|
| Nothing extra on cancel/reject | | |
| Short toast | | ✓ |
| You decide | | |

**User's choice:** Short toast.

---

## Who signs

| Option | Description | Selected |
|--------|-------------|----------|
| Amber or bunker | Stored session `sign_event` | ✓ |
| Amber only this slice | | |
| You decide | | |

**User's choice:** Whichever you logged in with.

| Option | Description | Selected |
|--------|-------------|----------|
| Small hardcoded list | damus, nos.lol, primal, wot.dergigi.com | |
| Full webapp default list | | |
| You decide / NIP-65 | Lantern publishes via NIP-65 relay lists | ✓ |

**User's choice:** NIP-65 write to publish, NIP-65 read to fetch. Small list only to find 10002 or if no list. Confirmed as that package.

| Option | Description | Selected |
|--------|-------------|----------|
| Amber every highlight | | ✓ |
| Remember kind 9802 | | |
| You decide | | |

**User's choice:** Every highlight.

| Option | Description | Selected |
|--------|-------------|----------|
| Cache 10002 with the session | | |
| Fetch 10002 every time | | ✓ |
| You decide | | |

**User's choice:** Fetch every time. Cache later (maybe nostrdb).

---

## Claude's Discretion

- Kotlin relay / NIP-84 / bunker `sign_event` stack
- Compose selection-toolbar Highlight action
- How marks are drawn on Markdown
- Context slice length
- Toast copy
- Amber `sign_event` intent extras (Dark Wisp if it already signs)

## Deferred Ideas

- Lantern comments / kind 1111 threads
- Highlights sidebar
- Friends / nostrverse highlight levels
- Zap splits
- Bookmarks
- Relay-list cache / nostrdb
- Remembered Amber `sign_event` permissions
- Author-inbox relays for Nostr-native articles
- Lantern `textquoteselector` / range tags
