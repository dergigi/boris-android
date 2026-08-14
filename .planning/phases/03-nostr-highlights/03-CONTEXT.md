# Phase 3: Nostr highlights - Context

**Gathered:** 2026-08-14
**Status:** Ready for planning

<domain>
## Phase Boundary

When logged in, the user can select article text (title or body), tap Highlight next to Copy, sign a NIP-84 kind 9802 event, and see that quote painted in the reader like a text marker. Existing highlights by the same pubkey for this URL are fetched from relays and painted the same way. Logged out, the reader is unchanged: no marks, no Highlight action. Reading stays ungated. No nsec in Boris. No comments, sidebar, bookmarks, zap splits, or friends/nostrverse levels.

</domain>

<decisions>
## Implementation Decisions

### Whose marks
- **D-01:** Paint only the logged-in user's highlights for this article URL (kind 9802, authors = session pubkey, `#r` = URL). Logged out: no fetch, no marks, no Highlight action.
- **D-02:** If relays fail or time out, keep reading. No extra error chrome for highlights.
- **D-03:** After create, paint immediately, then publish in the background. If publish fails, remove that mark.
- **D-04:** Match `r` tags after normalizing the article URL: https, drop `www`, drop trailing slash, drop query string, drop `#fragment`.
- **D-05:** If the quote is not in the fetched article body (Jina markdown), skip painting. No message. No fuzzy match.
- **D-06:** Duplicate quotes are two events. Paint both. Marker fill uses alpha so overlaps read stronger.
- **D-07:** Marker color is `HighlightMine` (`#FDE047`) with alpha. Home copy marks ("npub", "your own highlights") use the same opacity style.

### Event shape
- **D-08:** Create NIP-84 events like the Boris webapp, not Lantern's extra selector tags. Kind 9802, `content` = exact selected quote, `r` = article URL, `context` = a slice of surrounding body when we can take one. — **Reversibility:** one-way — published events are a relay contract other clients will fetch.
- **D-09:** `alt` tag is `Highlight created by Boris Android. readwithboris.com` (site https://www.readwithboris.com/). Do not change the webapp's alt this phase.
- **D-10:** Fetch any of the user's kind 9802s for this URL. Paint when `content` matches the body. Do not require `context` or our `alt`.

### Create gesture
- **D-11:** Highlight is an action on the system text selection toolbar, next to Copy. Hidden when logged out.
- **D-12:** Tap Highlight goes straight to `sign_event`. No confirm sheet. No comment / note field.
- **D-13:** Title and body are both valid selection sources.
- **D-14:** If the user cancels or rejects the signer, no mark, short toast so it is obvious it did not publish.

### Who signs
- **D-15:** Sign with the stored session: Amber NIP-55 `sign_event` or bunker NIP-46 `sign_event`. Never an nsec in Boris.
- **D-16:** Publish to the user's NIP-65 (kind 10002) write relays. Fetch from their NIP-65 read relays. Same filter: kind 9802, authors = me, `#r` = normalized URL. Do not use bunker pairing relays for content. No author-inbox extra relays (web URLs only).
- **D-17:** Bootstrap / missing 10002: hardcoded `wss://relay.damus.io`, `wss://nos.lol`, `wss://relay.primal.net`, `wss://wot.dergigi.com`. Researcher may trim or add one if a relay is dead. Use this list to find 10002 and to publish/fetch when the user has no relay list.
- **D-18:** Amber prompts on every highlight. Do not request remembered `sign_event` permissions this slice. Bunker prompts on the remote signer the same way.
- **D-19:** Do not cache kind 10002. Fetch it every time. A later cache (maybe nostrdb) is out of this phase.

### Claude's Discretion
- Kotlin stack for relays, NIP-65, NIP-84 templates, and bunker `sign_event` (reuse Phase 2 NIP-46 client; do not pull Amethyst Quartz).
- How Compose exposes a Highlight action on the selection toolbar.
- How marks are drawn on Markdown (keep DRY with Home copy marks / `HighlightMine`).
- Context slice length and how it is cut from the body.
- Toast copy for cancel/reject and for a failed publish (mark already removed).
- Amber `sign_event` intent extras: follow Dark Wisp if it already signs; otherwise the smallest NIP-55 `sign_event` that Amber accepts.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Protocol
- https://github.com/nostr-protocol/nips/blob/master/84.md — kind 9802, `content` quote, `r`, `context`, `alt`
- https://github.com/nostr-protocol/nips/blob/master/65.md — kind 10002 read/write relays
- https://github.com/nostr-protocol/nips/blob/master/55.md — Amber `sign_event`
- https://github.com/nostr-protocol/nips/blob/master/46.md — bunker `sign_event` (Phase 2 client already pairs)
- https://github.com/nostr-protocol/nips/blob/master/01.md — event envelope, ids, signatures

### Product
- `.planning/PROJECT.md` — reading first, no nsec
- `.planning/REQUIREMENTS.md` — HIGH-01, AUTH-06, READ-01 must not regress
- `.planning/ROADMAP.md` — Phase 3
- `.planning/phases/01-amber-login/01-CONTEXT.md` — Compose owns the launcher, no nsec, session store
- `.planning/phases/02-bunker-login-nip-46/02-CONTEXT.md` — bunker is a client; this phase adds `sign_event` on that client
- `.planning/codebase/CONVENTIONS.md` — Kotlin/Compose style
- `.planning/codebase/STRUCTURE.md` — files under `org.dergigi.boris`
- `.planning/codebase/ARCHITECTURE.md` — ReaderViewModel + Markdown, no DI
- `DESIGN.md` — Face Split: serif reads, sans steers; marker should stay a read-layer mark

### Reference implementations (shape, not a copy of the tree)
- `/Users/gigi/Development/vibe/lantern/src/sidebar/services/nostr-highlight-adapter.ts` — Lantern UX/kind 9802; do **not** copy `textquoteselector` / range tags
- `/Users/gigi/Development/vibe/lantern/src/sidebar/services/groups.ts` — `getWriteRelays`: `loadRelayList` write relays (NIP-65)
- `/Users/gigi/Development/vibe/boris/src/services/highlightCreationService.ts` — webapp NIP-84 create (`HighlightBlueprint`, `r`, `context`, `alt`)
- `/Users/gigi/Development/vibe/boris/src/services/relayListService.ts` — kind 10002 load
- `/Users/gigi/Development/vibe/boris/src/utils/highlightMatching.tsx` — quote match in article text
- `/Users/gigi/Development/vibe/boris/src/hooks/useExternalUrlLoader.ts` — fetch `{ kinds: [9802], '#r': [url] }`
- https://www.readwithboris.com/ — companion; Android alt names this host

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ReaderScreen.kt`: `SelectionContainer` around title + Markdown body. Highlight action hangs off this selection.
- `HomeScreen.kt` `HomeCopy()`: already draws rounded yellow marks behind annotated ranges. Reuse color/opacity; apply the same alpha on Home.
- `Color.kt` `HighlightMine` = `#FDE047`.
- `Session` / `SessionStore`: Amber vs Bunker. Reader needs the stored pubkey and a sign path. Do not invent a second identity store.
- `AuthViewModel` / `RemoteSignerBridge`: today `get_public_key` only. Add `sign_event` the same way (Compose launcher, domain builds the intent).
- Phase 2 NIP-46 client: extend with `sign_event`; pairing relays stay pairing relays.

### Established Patterns
- ViewModel holds article state (`ReaderViewModel` / `ReaderUiState`). Highlight fetch/paint belongs beside that, not in a Composable network call.
- JVM JUnit 4 tests for parsers (URL normalize, NIP-84 tags, quote match).
- No Hilt, no Room, no DataStore, no nostrdb this phase (D-19).
- OkHttp for HTTPS; Phase 2 already talks WebSockets for bunker. Researcher picks how content relays are queried.

### Integration Points
- Reader route is the only place marks appear.
- Home AuthBar is unchanged except Home copy mark opacity (D-07).
- Amber `RemoteSignerBridge` stays intent-based. Bunker `sign_event` stays on the NIP-46 client. Do not make the Amber bridge speak WebSockets.

</code_context>

<specifics>
## Specific Ideas

- Interaction like Lantern (select, publish, paint). Event bytes like the Boris webapp (NIP-84).
- Lantern source: `/Users/gigi/Development/vibe/lantern`
- Webapp source: `/Users/gigi/Development/vibe/boris`
- Keep DRY. Keep the slice small.

</specifics>

<deferred>
## Deferred Ideas

- Lantern comments / kind 1111 threads
- Highlights sidebar
- Friends / nostrverse highlight levels
- Zap splits
- Bookmarks
- Relay-list cache / nostrdb
- Remembered Amber `sign_event` permissions
- Author-inbox relays for Nostr-native articles
- Copying Lantern `textquoteselector` / range tags

</deferred>

---

*Phase: 3-Nostr highlights*
*Context gathered: 2026-08-14*
