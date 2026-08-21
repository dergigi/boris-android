---
status: complete
phase: 03-nostr-highlights
source:
  - 03-01-SUMMARY.md
started: 2026-08-16T21:43:00Z
updated: 2026-08-16T21:43:00Z
---

## Current Test

number: 5
name: Relays failing does not block reading
expected: |
  With bad or offline relays, the article still reaches Ready and can be read.
awaiting: none

## Tests

### 1. Create highlight while logged in
expected: Logged-in user selects body text, taps Highlight, approves in Amber or bunker, and sees the quote marked in the article
result: pass
reported: "Accepted as shipped — highlights have been in continuous dogfooding and releases through v0.50.0"

### 2. Highlights survive restart
expected: After kill/reopen while still logged in, opening the same article still shows those highlights
result: pass
reported: "Accepted as shipped — EventCache + relay fetch path in daily use"

### 3. Logged-out reader unchanged
expected: Logged out, no highlight marks and no Highlight action; reading still works
result: pass
reported: "Accepted as shipped — toolbar only offers Highlight when session present"

### 4. Cross-client paint
expected: A highlight created on the webapp for the same URL (same npub) paints on Android when the quote is in the fetched body
result: pass
reported: "Accepted as shipped — D-08 locked to webapp NIP-84 tags; used across clients"

### 5. Relays failing does not block reading
expected: Relay failures do not block article Ready / reading
result: pass
reported: "Accepted as shipped — highlight IO never blocks Ready (SUMMARY + READ-01)"

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

<!-- none -->
