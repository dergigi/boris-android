---
status: testing
phase: 06-on-device-article-extraction
source:
  - 06-VERIFICATION.md
started: 2026-08-20T10:09:00+02:00
updated: 2026-08-20T10:09:00+02:00
---

## Current Test

number: 1
name: Offline cache hit (D-15)
expected: |
  Load an https article once while online, enable airplane mode, reopen the same URL.
  Ready with the cached extract if thick enough, no cached badge. A thin cached extract shows Could not find an article on this page.
awaiting: user response

## Tests

### 1. Offline cache hit (D-15)
expected: Load an https article once while online, enable airplane mode, reopen the same URL. Ready with the cached extract if thick enough, no cached badge. A thin cached extract shows Could not find an article on this page.
result: [pending]

### 2. Offline never-cached URL
expected: Airplane mode, open a URL that was never loaded. Could not reach this page. plus Try again and Open original.
result: [pending]

### 3. JS-shell / teaser page
expected: Open a JS-heavy app-shell or paywall-teaser page. Could not find an article on this page. plus the same two buttons.
result: [pending]

### 4. Logged-out real-world read
expected: Logged out, paste a working https article URL. Ready looks like today: title, byline if obvious, body, images; no login prompt.
result: [pending]

### 5. TalkBack accessibility
expected: Navigate reader error and Ready states with TalkBack. TalkBack never announces library or proxy names.
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
