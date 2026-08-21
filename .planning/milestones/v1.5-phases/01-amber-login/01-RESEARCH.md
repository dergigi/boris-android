# Phase 1: Amber login - Research

**Researched:** 2026-08-14
**Confidence:** HIGH

Phase research reuses project research. Do not re-litigate the stack.

## What to know to plan this phase

1. NIP-55 login is one `get_public_key` intent (`nostrsigner:`), then persist pubkey hex + signer package.
2. No new Gradle libraries. Activity Result API, SharedPreferences, in-repo Nip19.
3. Manifest `<queries>` for scheme `nostrsigner` + `BROWSABLE` is required on API 30+ or Amber looks missing.
4. Amber often returns `npub1…` in `result` (sometimes `signature`). Decode to hex. Show npub.
5. Compose owns the launcher. Domain must not `startActivity`.
6. Missing signer: Zapstore first (`https://zapstore.dev/apps/com.greenart7c3.nostrsigner`), then F-Droid and GitHub.
7. v1 does not need `sign_event`, ContentResolver, or `permissions`.
8. Exclude session prefs from backup. Never log nsec.

## Files to create (recommended)

- `app/src/main/java/org/dergigi/boris/nostr/RemoteSignerBridge.kt`
- `app/src/main/java/org/dergigi/boris/nostr/Nip19.kt`
- `app/src/main/java/org/dergigi/boris/data/Session.kt`
- `app/src/main/java/org/dergigi/boris/data/SessionStore.kt`
- `app/src/main/java/org/dergigi/boris/ui/auth/AuthViewModel.kt`
- `app/src/main/java/org/dergigi/boris/ui/auth/AuthBar.kt`
- Tests: `Nip19Test.kt`, `SessionStoreTest.kt` (and intent-parse helper if extracted)
- `AndroidManifest.xml`: `<queries>`, backup rules
- `strings.xml`: connect, sign out, missing Amber, Zapstore / F-Droid / GitHub labels

## Canonical sources

- `.planning/research/SUMMARY.md`
- `.planning/research/STACK.md`
- `.planning/research/ARCHITECTURE.md`
- `.planning/research/PITFALLS.md`
- `.planning/research/FEATURES.md`
- Dark Wisp `RemoteSignerBridge` / `SignerIntentBridge` / `AuthScreen`
- NIP-55, NIP-19

---

*Phase: 01-amber-login*
*Researched: 2026-08-14*
