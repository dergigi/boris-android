# Phase 2: Bunker login (NIP-46) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-14
**Phase:** 2-Bunker login (NIP-46)
**Areas discussed:** reference implementation (pre-discuss), then skip remaining gray areas

---

## Reference implementation

| Option | Description | Selected |
|--------|-------------|----------|
| Follow Amethyst `bunker://` login | Same as Dark Wisp was for Amber | ✓ |
| Point at another bunker client | User picks a different model | |
| Park bunker | Stay Amber-only | |

**User's choice:** Follow Amethyst’s `bunker://` login
**Notes:** Dark Wisp does not implement NIP-46. Its remote signer is Amber/NIP-55.

---

## Remaining gray areas (pairing, Home, switch, nostrconnect)

| Option | Description | Selected |
|--------|-------------|----------|
| Discuss pairing / Home / switch / connect direction | Lock those in conversation | |
| Skip. You decide from Amethyst and plan. | Claude locks Amethyst-shaped defaults | ✓ |

**User's choice:** Skip these. You decide from Amethyst and plan.
**Notes:** Defaults: paste `bunker://` on Home (plus VIEW), one identity replacing Amber, no `nostrconnect://` or camera QR, login-only `connect` + `get_public_key`.

---

## Claude's Discretion

- Relay/NIP-44 stack (not Quartz)
- Secret storage for the disposable client-keypair
- Home field layout
- Timeouts / reconnect

## Deferred Ideas

- nostrconnect://, camera QR, sign_event, heartbeat UI, Boris-as-bunker, multi-account
