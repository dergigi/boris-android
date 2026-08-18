# Phase 5: Resolve nostr profile references - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-18
**Phase:** 5-Resolve nostr profile references
**Areas discussed:** Link label, Identifier coverage, Relay hints, Bare vs prefixed

---

## Link label

| Option | Description | Selected |
|--------|-------------|----------|
| Resolved `@name` | Match the webapp. Short npub until metadata loads. | ✓ |
| Short npub only | Always `@npub1abc…`, no metadata fetch in the body. | |
| Keep the `nostr:nprofile…` string | Just make it tappable. | |
| You decide | | |

**User's choice:** Resolved `@name`
**Notes:** Text only, no avatar. Custom Markdown labels (`[Gigi](nostr:nprofile1…)`) stay as written; only raw prefixed mentions become `@name`.

---

## Identifier coverage

| Option | Description | Selected |
|--------|-------------|----------|
| `nprofile` and `npub` | Same `@name` link, same in-app profile. | ✓ |
| `nprofile` only | Stick to the issue. Leave `npub` for later. | |
| You decide | | |

**User's choice:** `nprofile` and `npub`

---

## Relay hints

| Option | Description | Selected |
|--------|-------------|----------|
| Parse and ignore | Decode pubkey, drop relays. | |
| Use them for this profile fetch | Query hinted relays when loading metadata. | ✓ |
| You decide | | |

**User's choice:** Use hinted relays for this profile fetch

| Option | Description | Selected |
|--------|-------------|----------|
| Hinted plus existing | Ask nprofile relays and the usual set. | ✓ |
| Hinted only | Only those relays for this fetch. | |
| You decide | | |

**User's choice:** Hinted plus existing

| Option | Description | Selected |
|--------|-------------|----------|
| This fetch only | Use hints to load the name, then forget them. | |
| Remember for that profile | Keep hints for later visits to that person. | ✓ |
| You decide | | |

**User's choice:** Remember for that profile

---

## Bare vs prefixed

| Option | Description | Selected |
|--------|-------------|----------|
| Prefixed and bare | `nostr:` plus bare `nprofile1` / `npub1`. | |
| `nostr:` prefix only | Ignore bare bech32. | ✓ |
| You decide | | |

**User's choice:** `nostr:` prefix only (`nostr://` still counts, matching existing naddr parsing)

---

## Claude's Discretion

- Markdown rewrite vs annotator vs UriHandler
- Where remembered relays persist
- `nprofileDecode` TLV shape (mirror `neventDecode`)
- TTS speaking `@name` if cheap
- No spinner in the body; invalid nprofile does not crash

## Deferred Ideas

- Avatars next to mentions
- Bare bech32 auto-link
- Profile-linking note/nevent/naddr
