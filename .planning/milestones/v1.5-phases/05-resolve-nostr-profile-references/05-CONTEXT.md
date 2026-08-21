# Phase 5: Resolve nostr profile references - Context

**Gathered:** 2026-08-18
**Status:** Ready for planning

<domain>
## Phase Boundary

`nostr:nprofile` and `nostr:npub` references in article Markdown render as tappable `@name` profile links that open the existing in-app profile screen. Relay hints on `nprofile` are used (plus the app's usual relays) when fetching that profile, and remembered for later visits. Existing `naddr` / `note` / `nevent` links keep working. Reading stays ungated. No avatars in the article line. No bare bech32 (must have a `nostr:` or `nostr://` prefix). No nsec. GitHub issue #5.

</domain>

<decisions>
## Implementation Decisions

### Link label
- **D-01:** Raw `nostr:nprofile` / `nostr:npub` mentions render as `@name`. Until kind 0 arrives, show a short npub (same fallback `Profile.displayName` already uses). — **Reversibility:** reversible
- **D-02:** Text only. No avatar in the mention. Fits the reading line like the webapp `NostrMentionLink`.
- **D-03:** If the author already wrote a Markdown link with a custom label (`[Gigi](nostr:nprofile1…)`), keep `Gigi`. Only raw prefixed mentions become `@name`.

### Identifier coverage
- **D-04:** Resolve both `nprofile` and `npub`. Both tap to the in-app profile for that pubkey (`Routes.profile(npub)`).
- **D-05:** Do not treat `nsec`, `note`, `nevent`, or `naddr` as profile mentions. Those keep today's reader routing.

### Relay hints
- **D-06:** Decode `nprofile` TLV relay hints. A bad or extra hint must not drop the pubkey. — **Reversibility:** reversible
- **D-07:** When fetching kind 0 for that pubkey, query hinted relays **plus** the relays Boris already uses for profiles (`RelayQuery.fetchProfile` / NIP-65 / fallback).
- **D-08:** Remember those hinted relays for that pubkey so later visits (profile screen, later articles) still use them. — **Reversibility:** costly — needs a per-pubkey relay store that other profile fetches will read.

### Bare vs prefixed
- **D-09:** Only `nostr:` and the existing `nostr://` variant. Bare `nprofile1…` / `npub1…` in the body stay plaintext.

### Claude's Discretion
- How to rewrite raw mentions inside the markdown pipeline (pre-process body vs markdown link annotator vs `LocalUriHandler`).
- Persistence for D-08 (SharedPreferences vs a small store next to EventCache). Merge strategy with NIP-65 lists.
- `nprofileDecode` shape: mirror `neventDecode` TLV (type 0 pubkey, type 1 relay).
- Whether TTS speaks `@name` instead of the bech32 blob after the rewrite (nice if cheap; not a must-have).
- Loading: short npub is enough; no spinner in the article body.
- Invalid / un-decodable `nostr:nprofile` stays visible as truncated text, not a crash.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Product
- `.planning/PROJECT.md` — reading first, no nsec
- `.planning/REQUIREMENTS.md` — READ-03 is this phase; READ-01 must not regress
- `.planning/ROADMAP.md` — Phase 5
- `.planning/codebase/CONVENTIONS.md` — Kotlin/Compose, no DI
- `https://github.com/dergigi/boris-android/issues/5` — acceptance criteria and repro article

### Webapp (match mention rendering, do not invent a second contract)
- `/Users/gigi/Development/vibe/boris/src/components/NostrMentionLink.tsx` — `@name` profile link from npub/nprofile
- `/Users/gigi/Development/vibe/boris/src/utils/nostrUriResolver.tsx` — nprofile → npub → `/p/{npub}`
- `/Users/gigi/Development/vibe/boris/src/utils/nostrPatterns.ts` — identifier pattern (Android still requires `nostr:` prefix per D-09)

### Android reader
- `app/src/main/java/org/dergigi/boris/data/NostrLink.kt` — today only naddr/note/nevent; profile types must be added
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderLinks.kt` — `readerLinkAction` tap routing
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` — markdown + `LocalUriHandler`
- `app/src/main/java/org/dergigi/boris/nostr/Nip19.kt` — `neventDecode` TLV is the nprofile analog; `normalizePubkey` does not handle nprofile yet
- `app/src/main/java/org/dergigi/boris/nostr/Profile.kt` — `displayName` short-npub fallback
- `app/src/main/java/org/dergigi/boris/nostr/RelayQuery.kt` — `fetchProfile` / `fetchProfileRemote`
- `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` — `Routes.profile(npub)`
- `app/src/test/java/org/dergigi/boris/ui/reader/ReaderLinksTest.kt` — existing routing tests to extend
- `app/src/test/java/org/dergigi/boris/data/NostrLinkTest.kt` — parse tests to extend

### Repro
- Issue comment naddr: `nostr:naddr1qqwhwmmjw35xcetnwvkk6mmwv4uj6arfd4jkcetnwvkkzun595pzq634npfz8rwfq2hdr8am76s9t7dt7gwpe2y3t5wyufl4phe09yxeqvzqqqr4gu7cgak5`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Nip19.neventDecode` + `parseTlv`: copy the TLV layout for `nprofileDecode` (0 = pubkey, 1 = relay URLs).
- `Profile.displayName` / `Profile.parse`: label text and short-npub fallback.
- `RelayQuery.fetchProfile`: kind 0 via NIP-65 + cache; extend to accept extra relays rather than a second fetch path.
- `Routes.profile` + `onOpenProfile(pubkeyHex)`: already wired from the reader author byline.
- `readerLinkAction`: add a profile branch before the generic external-open fallback.

### Established Patterns
- JVM JUnit 4 tests for decode and routing (see `NostrLinkTest`, `ReaderLinksTest`).
- `nostr:` and `nostr://` already accepted for naddr/note/nevent in `NostrLink.entityRegex`.
- No Hilt, no Room. Persist remembered relays in SharedPreferences or a tiny store, not a new database.

### Integration Points
- Markdown body in `ReaderScreen` (`LocalUriHandler` / link styles): raw `nostr:nprofile…` is not a markdown link today, so something must turn it into one (or an annotated span) before paint.
- Profile screen already loads metadata; remembered relays should feed that fetch too (D-08).
- `UrlExtractor.articleUrl` must not swallow profile URIs as articles.

</code_context>

<specifics>
## Specific Ideas

- Match the webapp mention: `@` plus display name, tap to `/p/{npub}` equivalent.
- Amethyst and Wisp are references for expected tap behavior (in-app profile), not for avatars in the article.
- Repro article is the naddr in issue #5; raw `nostr:nprofile…` is visible there today.

</specifics>

<deferred>
## Deferred Ideas

- Avatars next to mentions in the article body
- Auto-linking bare `npub1` / `nprofile1` without a `nostr:` prefix
- Treating `note` / `nevent` / `naddr` mentions as profile links

None of these were requested as this phase.

</deferred>

---

*Phase: 5-Resolve nostr profile references*
*Context gathered: 2026-08-18*
