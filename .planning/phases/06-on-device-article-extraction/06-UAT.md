---
status: complete
phase: 06-on-device-article-extraction
source:
  - 06-VERIFICATION.md
started: 2026-08-20T10:09:00+02:00
updated: 2026-08-21T15:41:00+02:00
---

## Current Test

number: 5
name: TalkBack accessibility
expected: |
  Navigate reader error and Ready states with TalkBack. TalkBack never
  announces library or proxy names.
awaiting: none

## Tests

### 1. Offline cache hit (D-15)
expected: Load an https article once while online, enable airplane mode, reopen the same URL. Ready with the cached extract if thick enough, no cached badge. A thin cached extract shows Could not find an article on this page.
result: pass
reported: "Accepted as shipped — extractor works in daily use through 1.4.53, including reopen of already-read pages"

### 2. Offline never-cached URL
expected: Airplane mode, open a URL that was never loaded. Could not reach this page. plus Try again and Open original.
result: pass
reported: "Accepted as shipped — extractor works in daily use through 1.4.53"

### 3. JS-shell / teaser page
expected: Open a JS-heavy app-shell or paywall-teaser page. Could not find an article on this page. plus the same two buttons.
result: pass
reported: "Accepted as shipped — most extractor cases work in daily use. Dedicated JS-shell/paywall page not separately reproduced."

### 4. Logged-out real-world read
expected: Logged out, paste a working https article URL. Ready looks like today: title, byline if obvious, body, images; no login prompt.
result: pass
reported: "Accepted as shipped — extractor works in daily use through 1.4.53"

### 5. TalkBack accessibility
expected: Navigate reader error and Ready states with TalkBack. TalkBack never announces library or proxy names.
result: pass
reported: "Accepted as shipped — UI copy has no library/proxy names. TalkBack session not separately reproduced."

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

<!-- none -->
