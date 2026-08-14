# Plan 01-01 Summary

**Completed:** 2026-08-14
**Phase:** 01-amber-login

Optional Amber identity on Home. Connect via NIP-55 `get_public_key`, persist hex + signer package, show npub, sign out. Missing Amber points at Zapstore first. Reading stays ungated.

## Delivered

- `RemoteSignerBridge`, `SignerResults`, `Nip19`
- `Session` / `SessionStore` (`boris_session`)
- `AuthViewModel` / `AuthBar` on `HomeScreen`
- Manifest `<queries>` + backup exclude
- JVM tests: Nip19 official vectors, signer extras, session parse

## Verify

`./gradlew :app:test :app:assembleDebug` passed.
