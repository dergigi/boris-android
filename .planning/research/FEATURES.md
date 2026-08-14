# Feature Research

**Domain:** Android reader + Amber / NIP-55 identity (login only)
**Researched:** 2026-08-14
**Confidence:** HIGH

This slice adds who-you-are to an already-shipped reader. It does not add a social client, a signer, or a bookmark store. Table stakes are the four Active requirements in PROJECT.md. Everything else is either a later milestone or an anti-feature.

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = login feels broken. These four are PROJECT.md Active requirements. Reading without an account is already shipped and stays available.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Connect via Amber (`get_public_key`) | "Login with Amber" is the Android Nostr login users already know from Amethyst and Dark Wisp | MEDIUM | One NIP-55 intent. Do not set `package` on that first call. Compose owns `ActivityResultLauncher`; domain code must not launch activities. Amber may return an `npub1…` bech32 in `result`; decode to hex before persist. |
| Persist pubkey hex + signer package; show npub | Logged-in state must survive process death. Users expect to see their `npub`, not a hex blob | LOW | Store the pair (DataStore or SharedPreferences). Do not call `get_public_key` again while logged in (NIP-55 SHOULD). Display bech32 `npub` derived from stored hex. |
| Sign out | Identity login without a way out feels trapped | LOW | Delete the stored pair. No Amber call required. Next login is a fresh `get_public_key`. |
| Missing-signer UX | Amber is not on Play Store as a first-class listing. If the button silently fails, users think Boris is broken | LOW | `PackageManager` query for `nostrsigner:` (needs manifest `<queries>`). If none, say Amber is missing and point at Zapstore first, then F-Droid / GitHub. Do not hide the connect action the way Dark Wisp does. |

### Differentiators (Competitive Advantage)

Features that set this slice apart. Align with Core Value: reading stays first; login is a stored identity, never a key.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Identity-only Amber login | Boris knows who you are without ever holding an `nsec` or asking Amber to sign | LOW | Dark Wisp and Amethyst both treat Amber as a full signer (permissions list + `NostrSigner`). Boris asks only for the pubkey. No `permissions` extra on `get_public_key`. |
| Thin login helper, not a signer stack | Small enough to ship in one slice; matches "follow Dark Wisp, not Amethyst" | MEDIUM | Copy Dark Wisp's login shape (`isSignerAvailable` + `buildGetPublicKeyIntent` + persist pair). Do not port `RemoteSigner` / `NostrSigner` / Quartz until something actually signs. |
| Always-visible missing-Amber path | Users who have not installed Amber get a reason and a destination, not a missing button | LOW | Dark Wisp's `AuthScreen` only renders "Login with Signer" when `RemoteSignerBridge.isSignerAvailable` is true. Amethyst shows "Login with Amber" and errors on launch failure. Boris should explain and link install. |
| Reading is not gated on login | The product stays a reader. Login is optional identity, not a paywall | LOW | Home / share / open-in-app / markdown view keep working with no account. Login UI sits beside reading, it does not replace it. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems. Do not build these in this slice.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| `nsec` paste / import | Fastest way to "just log in"; Dark Wisp `AuthScreen` is built around an nsec field | Boris must never request or persist a private key. Pasting an `nsec` into a reader expands the attack surface the Amber slice exists to avoid | Amber holds the key. Connect via NIP-55 only |
| Bunker / NIP-46 | Works across devices and for people without Amber; Amethyst ships it | Different protocol, relays, pairing secrets, and failure modes. PROJECT.md: later, after Amber login works | Ship Amber first. Add bunker in a later milestone |
| Signing events (`sign_event`) | Next obvious Nostr step; Dark Wisp requests a long `permissions` list at login | This slice has nothing to publish. Requesting kinds 0/1/3/… at login trains Amber to treat Boris as a social client and pulls in a full signer interface | Login helper only. Add `sign_event` when a feature needs a signature |
| Bookmarks | Companion webapp already thinks in bookmarks; users will ask | Needs signed events, relays, and a sync story. Turns the reader into a client | Keep bookmarks on the companion webapp (`/Users/gigi/Development/vibe/boris`) until a later Android slice |
| Encrypt / decrypt (NIP-04 / NIP-44) | Dark Wisp asks for `nip44_encrypt` / `nip44_decrypt` on the login intent | No DMs, no private content in this slice. Extra permissions are unused attack surface | Omit `permissions` entirely on `get_public_key` |
| Highlights | Reader feature people want next | Same as bookmarks: signed events + sync. Out of scope | Companion webapp; Android later |
| Amethyst-sized signer stack | "Do it the standard way" | Quartz `NostrSignerExternal` + multi-signer picker + default permissions is the stack PROJECT.md rejected as too large for v1 | Dark Wisp login shape: detect, one intent, store pair |
| Relays, profiles, feeds, zaps | Makes login "useful" | Boris is not a social client. Those features invert the product | Identity record only |
| Watch-only `npub` paste | Dark Wisp allows npub/hex login as read-only | Second login path, no Amber, no signer package to persist. Conflicts with "Amber holds the key" | Amber `get_public_key` is the only login |
| Content Resolver signing | Background calls without opening Amber | Only works after remembered permissions. Useless until Boris signs | Intents for `get_public_key` only |
| Multi-account | Amethyst and Dark Wisp both have it | One stored pair is the whole v1 record. Switching accounts is a later product decision | Single identity. Sign out, then connect again |
| Set `package` on first `get_public_key` | Feels more reliable ("always open Amber") | NIP-55 and PROJECT.md: omit `package` on the first call so any installed signer can answer. Hard-coding `com.greenart7c3.nostrsigner` fights the protocol | Query `nostrsigner:`, launch without `package`, store the returned package |

## Feature Dependencies

```
Manifest <queries> for nostrsigner:
    └──requires──> Detect signer installed
                       ├──requires──> Missing-signer UX (install pointer)
                       └──requires──> Connect via Amber (get_public_key, no package)
                                          └──requires──> Compose ActivityResultLauncher
                                          └──requires──> Persist pubkey hex + signer package
                                                             ├──requires──> Show npub (bech32 from hex)
                                                             ├──requires──> Sign out (delete pair)
                                                             └──requires──> Skip get_public_key while logged in

Existing reader (paste / share / open / markdown)
    └──independent──> Login (must not gate reading)

nsec paste          ──conflicts──> Amber-only identity
Bunker / NIP-46     ──conflicts──> Amber-only this slice
sign_event          ──conflicts──> login-only this slice
Bookmarks           ──requires──> sign_event + relays (defer both)
```

### Dependency Notes

- **Connect via Amber requires signer detection:** NIP-55 clients must declare `<queries>` for `nostrsigner:` or `PackageManager` will lie on Android 11+. Detection is also the missing-signer branch.
- **Show npub requires the persisted pair:** Display from stored hex. Do not round-trip Amber on every Home composition.
- **Sign out requires the same store:** Clearing identity is a delete of that pair, not an Amber logout API (NIP-55 has none).
- **Skip `get_public_key` while logged in:** Spec SHOULD. Re-prompting Amber on every launch is the bug Dark Wisp and NIP-55 both warn against.
- **Bookmarks require `sign_event`:** Do not sneak a bookmark button into this slice. It forces the signer stack this milestone exists to avoid.
- **`nsec` paste conflicts with Amber-only identity:** One text field for "any key" is how Dark Wisp AuthScreen works. Boris must not grow that field.

## MVP Definition

### Launch With (v1)

Minimum viable product. Matches PROJECT.md Active requirements exactly. Nothing else.

- [ ] Connect via Amber (NIP-55 `get_public_key`) — only way to become "logged in"
- [ ] Persist pubkey hex and signer package; show npub while logged in — identity that survives restart
- [ ] Sign out clears that stored identity — leave as cleanly as you arrived
- [ ] If Amber is missing, say so and point the user at installing it — login cannot silently no-op

### Add After Validation (v1.x)

Features to add once Amber login is proven on device.

- [ ] Bunker / NIP-46 — trigger: Amber login works and a desktop/web pairing need shows up
- [ ] `sign_event` for one concrete reader action — trigger: a later phase names the event (not "so we have a signer")
- [ ] Content Resolver path — trigger: a signed action exists and remembered permissions matter

### Future Consideration (v2+)

Defer until the reader-plus-identity product is real.

- [ ] Bookmarks / highlights on Android — belong with the companion webapp until sync is designed
- [ ] Relays, profiles, feeds, zaps — social client; not Boris
- [ ] Multi-account — only after one account is boring
- [ ] Full `NostrSigner` interface — only when encrypt/decrypt or many methods exist

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Connect via Amber | HIGH | MEDIUM | P1 |
| Persist pair + show npub | HIGH | LOW | P1 |
| Sign out | HIGH | LOW | P1 |
| Missing-signer UX + install pointer | HIGH | LOW | P1 |
| Reading remains ungated | HIGH | LOW | P1 (already shipped; do not regress) |
| Thin login helper (no `NostrSigner`) | MEDIUM | LOW | P1 (scope control, not a user-facing feature) |
| Bunker / NIP-46 | MEDIUM | HIGH | P3 |
| `sign_event` | MEDIUM | HIGH | P3 |
| Bookmarks | MEDIUM | HIGH | P3 |
| `nsec` paste | LOW (and harmful) | LOW | Do not build |
| Highlights / feeds / zaps | LOW for this product | HIGH | Do not build this milestone |

**Priority key:**
- P1: Must have for launch (Active requirements)
- P2: Should have, add when possible (none in this slice)
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Dark Wisp | Amethyst | Our Approach |
|---------|-----------|----------|--------------|
| Amber / NIP-55 login | `AuthScreen` + `RemoteSignerBridge`. "Login with Signer" only if a signer is installed. Compose launcher; `loginWithSigner(pubkeyHex, pkg)` | `ExternalSignerButton` + Quartz `ExternalSignerLogin`. Always shows "Login with Amber"; picker if several signers | Follow Dark Wisp's smaller login shape. Always offer connect; if none installed, explain and point at Amber |
| Show npub | Keys screen now reads `getPubkeyHex()` so REMOTE accounts are not blank (PR #43, 2026-06) | Account model shows npub after external login | Show npub in Boris while the pair is stored. No keys/reveal screen |
| Sign out | Deletes account / pair as part of a multi-account key store | Full account switcher | Delete the one stored pair |
| Missing signer | Hide the button | Launch and surface an error | Explicit copy + install destinations: Zapstore first, then F-Droid and GitHub. Amber is not a Play Store first-class app |
| `nsec` paste | Primary AuthScreen field (`nsec` or `npub`) | First-class login path | Anti-feature. Do not add the field |
| Bunker | Not the Android path | First-class (`bunker://`) | Out of scope this slice |
| Permissions on login | Large JSON: many `sign_event` kinds + NIP-44 | `DefaultSignerPermissions` | Send no `permissions`. Login is `get_public_key` only |
| Signer abstraction | `NostrSigner` / `RemoteSigner` / `LocalSigner` | Quartz `NostrSignerExternal` tree | Thin helper until a feature signs |
| Reading without login | Social client; auth is the front door | Social client; auth is the front door | Reader stays the front door |

Copy from Dark Wisp, do not clone it: detect `nostrsigner:`, one `get_public_key` without `package`, store pubkey + package, Compose owns the launcher, decode Amber's possible `npub1` result to hex. Leave behind nsec fields, signup, Tor chrome, and the permissions list.

## Sources

- PROJECT.md Active / Out of Scope (2026-08-14) — v1 must match the four Active requirements
- [NIP-55](https://github.com/nostr-protocol/nips/blob/master/55.md) (also [nips.nostr.com/55](https://nips.nostr.com/55)) — `get_public_key`, store pubkey + package, do not re-call while logged in, omit `package` on first intent, `<queries>` for `nostrsigner:`
- [Dark Wisp AuthScreen](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/ui/screen/AuthScreen.kt) — primary reference: `RemoteSignerBridge.isSignerAvailable`, Compose launcher, `loginWithSigner`, Amber `npub1` decode. Hides signer button when none installed; includes nsec field and a large permissions JSON (do not copy those)
- [Dark Wisp README](https://github.com/barrydeen/dark-wisp-android) — optional Amber / NIP-55; `NostrSigner` + `RemoteSigner` (too large for Boris v1)
- [Dark Wisp PR #43](https://github.com/barrydeen/dark-wisp-android/pull/43) (merged 2026-06-30) — REMOTE accounts must show npub from stored pubkey, not from a local keypair
- [Amethyst ExternalSignerButton](https://github.com/vitorpamplona/amethyst/blob/main/amethyst/src/main/java/com/vitorpamplona/amethyst/ui/screen/loggedOff/login/ExternalSignerButton.kt) — fallback reference: always-visible Amber button, multi-signer picker, `DefaultSignerPermissions`
- [Amber](https://github.com/greenart7c3/Amber) / [F-Droid](https://f-droid.org/packages/com.greenart7c3.nostrsigner/) — package `com.greenart7c3.nostrsigner`; install via F-Droid, GitHub releases, Zapstore, Obtainium
- [Wisp PR #531](https://github.com/barrydeen/wisp/pull/531) — upstream Wisp removed NIP-55; Dark Wisp still has it. Do not treat Wisp main as the Amber reference

---
*Feature research for: Boris Android Amber / NIP-55 login*
*Researched: 2026-08-14*
