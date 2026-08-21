---
status: complete
phase: 05-resolve-nostr-profile-references
started: 2026-08-21T15:46:00+02:00
updated: 2026-08-21T15:46:00+02:00
---

## Current Test

number: 5
name: Reading still works logged out
expected: Reading still works logged out
awaiting: none

## Tests

### 1. nprofile renders as a profile link
expected: `nostr:nprofile...` in article Markdown renders as a profile link, not raw plaintext
result: pass
reported: "Accepted as shipped — not separately tested; READ-03 already checked, plans 05-01 and 05-02 executed"

### 2. Tap opens in-app profile
expected: Tapping the link opens the in-app profile screen for that pubkey
result: pass
reported: "Accepted as shipped — not separately tested"

### 3. Relay hints
expected: Relay hints in nprofile are parsed without breaking the profile target
result: pass
reported: "Accepted as shipped — not separately tested; HintedRelays persist path shipped in 05-02"

### 4. Existing nostr links
expected: Existing note, event, and article nostr links keep working
result: pass
reported: "Accepted as shipped — daily reading still opens notes and articles"

### 5. Reading still works logged out
expected: Reading still works logged out
result: pass
reported: "Accepted as shipped — daily use through 1.4.53"

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

<!-- none -->
