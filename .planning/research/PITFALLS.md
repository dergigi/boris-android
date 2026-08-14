# Pitfalls Research

**Domain:** Amber / NIP-55 Android client login (Compose reader)
**Researched:** 2026-08-14
**Confidence:** HIGH

Login-only slice. Boris stores `pubkey` hex + signer package. Amber (`com.greenart7c3.nostrsigner`) holds the `nsec`. Dark Wisp is the model; Amethyst is the fallback. Every pitfall below belongs to the Amber login phase.

## Critical Pitfalls

### Pitfall 1: Missing `<queries>` for `nostrsigner:`

**What goes wrong:**
`PackageManager.queryIntentActivities` returns empty on Android 11+ even when Amber is installed. The UI says Amber is missing, or the Connect button is hidden, and login never starts.

**Why it happens:**
Package visibility (API 30) hides other apps unless the client declares the scheme. NIP-55 requires this block in `AndroidManifest.xml`. Boris has none today.

**How to avoid:**
Add the NIP-55 `<queries>` intent for `android.intent.action.VIEW` + `BROWSABLE` + `nostrsigner` before any install check. Then probe with `Intent(ACTION_VIEW, Uri.parse("nostrsigner:"))`.

**Warning signs:**
Install check is false on a device that has Amber. `queryIntentActivities` is empty in logcat. Login works only if you hardcode Amber's package and skip discovery.

**Phase to address:**
Amber login (manifest + discovery, before the first intent).

---

### Pitfall 2: Wrong result extra names (`signature` vs `result` vs `package`)

**What goes wrong:**
Login "succeeds" with a null pubkey, or the stored package is empty so later calls cannot target Amber. Older client code reads `signature`; current NIP-55 returns the method value in `result` and the signer id in `package`.

**Why it happens:**
The extra names drifted. Dark Wisp's signing launcher used `signature` first, then `result`. Its login launcher used `result` + `package`. Amethyst `IntentResult.fromIntent` reads `result`, `package`, `event`, `id`, `rejected` and does not look at `signature`. Some older Amber / library paths still emit `signature`.

**How to avoid:**
For `get_public_key`, read `result` first, fall back to `signature` if `result` is blank. Read the signer package from the `package` extra, not from a hardcoded string and not from `callingActivity` alone. Do not treat `signature` as an event signature during login. Do not confuse the extra name `package` with `Intent.setPackage`.

**Warning signs:**
`RESULT_OK` but `getStringExtra("result")` is null. Stored package is null after a successful Amber approve. Tests that only stub `signature` pass while a current Amber build fails, or the reverse.

**Phase to address:**
Amber login (result parser).

---

### Pitfall 3: Treating Amber's `npub` as hex (or storing hex as the display string)

**What goes wrong:**
NIP-55 says all pubkeys are hex. Amber's `get_public_key` `result` is often `npub1…` bech32. Dark Wisp decoded that to hex before persist. If Boris stores the raw extra, later `current_user` values, hex checks, and npub display all break. If it displays the hex blob as the identity, the UI looks wrong.

**Why it happens:**
The spec and Amber disagree on encoding. Clients copy one or the other.

**How to avoid:**
Normalize on the way in: if the extra starts with `npub1`, bech32-decode to 32-byte hex; otherwise accept 64-char hex only. Persist hex. Show npub in the UI. Reject anything else (empty, `nsec1`, notes). Do not persist the raw extra "as returned."

**Warning signs:**
Stored identity starts with `npub1`. Display shows a 64-char hex string. A later hex-only check fails on a user who just logged in.

**Phase to address:**
Amber login (normalize + persist + display).

---

### Pitfall 4: Calling `get_public_key` on every launch

**What goes wrong:**
Amber opens on every cold start. NIP-55 forbids this while the user stays logged in: it is a fingerprinting leak (Boris keeps asking Amber who you are) and a UX failure.

**Why it happens:**
Login is written as "ask Amber for the pubkey" instead of "restore the stored pair." A `LaunchedEffect(Unit)` or splash check re-issues the intent.

**How to avoid:**
One `get_public_key` at Connect. Persist pubkey hex + signer package. While that pair exists, never send `get_public_key`. Sign out deletes the pair. Process start reads the store; it does not talk to Amber.

**Warning signs:**
Amber appears after rotate, after process death, or when opening a shared URL. Logcat shows `type=get_public_key` with no user tap.

**Phase to address:**
Amber login (session restore). This is the NIP-55 fingerprinting rule.

---

### Pitfall 5: Pinning Amber's package on the first `get_public_key`

**What goes wrong:**
The first login intent sets `intent.package = "com.greenart7c3.nostrsigner"` (or the first `ResolveInfo` from the query). The user never gets a chooser if another signer is installed. If the pin is wrong (typo, old Amber id, F-Droid vs GitHub confusion), the intent fails with no useful UI. If extras.`package` is ignored and Boris keeps the guessed pin, later calls go to the wrong app.

**Why it happens:**
NIP-55 says omit `package` only on `get_public_key`, then store whatever the signer returns. Hardcoding Amber looks simpler. PROJECT.md already names `com.greenart7c3.nostrsigner`; that is the install target, not the first-intent pin.

**How to avoid:**
First `get_public_key`: no `package` extra/field. After approve, persist extras.`package`. If that extra is missing, fail login rather than invent a package. Do not pin from `queryIntentActivities()[0]`. After login, later methods (out of scope for v1) must set `package` to the stored value.

**Warning signs:**
Login works only when Amber is the sole `nostrsigner` app. `get_public_key` intent already has a package in the debugger. Stored package does not match the app the user approved.

**Phase to address:**
Amber login (intent builder + persist).

---

### Pitfall 6: Rotation or process death while Amber is open

**What goes wrong:**
User taps Connect, Amber comes to the front, Boris is killed or recreated. The `ActivityResultLauncher` callback never reaches the in-flight request, or a new composition fires Connect again. Boris already has this shape of bug for share/VIEW (`incomingUrl` + `LaunchedEffect` after rotate).

**Why it happens:**
Login state lives in `remember` / a one-shot flag. Domain code holds a `CompletableDeferred` that dies with the process. Compose re-registers the launcher (that part is fine) but also re-launches the intent.

**How to avoid:**
Register `rememberLauncherForActivityResult` at the Activity/Compose owner, not in a ViewModel. Treat "Amber is open" as a saved flag (`rememberSaveable` or DataStore). Do not launch from `LaunchedEffect` keyed on a value that comes back after config change. After result, write the identity before navigating. Dark Wisp deferred navigation until `RESUMED` because the callback fires in `STARTED` and a navigate can be dropped.

**Warning signs:**
Rotate on the Amber screen, approve, return to a logged-out Home. Double Amber prompt after rotate. Same `incomingUrl` pattern copied onto login.

**Phase to address:**
Amber login (launcher ownership + config-change).

---

### Pitfall 7: Collapsing user reject and back-press into one error

**What goes wrong:**
User hits Back in Amber (or Amber crashes). Boris shows "request rejected" or, worse, "Amber is not installed." User taps Reject in Amber (`RESULT_OK` + `rejected=true`) and Boris retries until Amber loops, or treats it as success because `resultCode` is OK.

**Why it happens:**
NIP-55 splits these on purpose: non-`RESULT_OK` is signer failure / dismiss, not a reject. Reject is `RESULT_OK` with extra `rejected=true`. Dark Wisp modeled this as `SignResult.Success` / `Rejected` / `Cancelled` and threw `SignerRejectedException` vs `SignerCancelledException`. Its login launcher was weaker: it only read extras on `RESULT_OK` and ignored `rejected`.

**How to avoid:**
Three outcomes, three UI strings:
- `RESULT_OK` + `rejected=true`: user said no. Stay logged out. Do not retry.
- `RESULT_OK` + pubkey + package: persist and show npub.
- Any other `resultCode` (Back, Amber killed): cancelled / failed. Stay logged out. Do not claim Amber is missing if the install check already passed.

Do not launch a second `get_public_key` from the cancel path.

**Warning signs:**
Back from Amber shows "rejected." Reject in Amber opens Amber again. Login spinner never clears.

**Phase to address:**
Amber login (result mapping + copy).

---

### Pitfall 8: Logging identity extras or requesting an `nsec`

**What goes wrong:**
Logcat prints the full result intent, a pasted `nsec`, or Zapstore `SIGN_WITH`. A debug build ships that habit. Or the login UI grows an "import key" field because that is how other Nostr apps work.

**Why it happens:**
Intent extras look harmless. PROJECT.md forbids holding an `nsec`. Dark Wisp logs request `type` only, which is enough.

**How to avoid:**
Never request, persist, or log an `nsec`. Log `type` and result class (`Success` / `Rejected` / `Cancelled`), not `result` / `signature` / `event`. Keep Zapstore `.env` / `local.properties` out of the app process. Login UI is Connect + missing-Amber + sign out. No key paste.

**Warning signs:**
A text field that accepts `nsec1`. `Log.d` of `activityResult.data.extras`. Tests that fixture an `nsec` into DataStore.

**Phase to address:**
Amber login (helper + UI + logging).

---

### Pitfall 9: `allowBackup` copies the login record

**What goes wrong:**
Boris already has `android:allowBackup="true"`. Once pubkey + package live in DataStore or SharedPreferences, a backup restore on another device shows a logged-in npub whose signer package is not installed there. Auto-backup can also upload that pair to the user's Google account.

**Why it happens:**
Identity feels public (npub is public). The package binding is not. CONCERNS.md already flagged backup of cache; login makes it an account problem.

**How to avoid:**
Exclude the login file from backup (`dataExtractionRules` / `fullBackupContent`, or `allowBackup="false"` until there is data worth backing up). After restore, if the stored package is not installed, treat as signed out or show the missing-Amber path. Sign out must delete the pair.

**Warning signs:**
Fresh install via backup is already "logged in." Restore on a device without Amber still shows an npub.

**Phase to address:**
Amber login (storage + manifest backup rules).

---

### Pitfall 10: Launching the signer without an Activity-owned launcher

**What goes wrong:**
`context.startActivity` from a ViewModel, Application, or repository. No result comes back. Or Compose uses `LocalContext` that is not an Activity after a wrap. Amethyst's first gotcha: Activity context is required; a background path cannot open Amber.

**Why it happens:**
Domain code wants to "just login." PROJECT.md already says Compose owns `ActivityResultLauncher` and domain code must not launch activities. Dark Wisp's `SignerIntentBridge` exists for that split (mutex + UI delivers the result).

**How to avoid:**
Thin login helper builds the intent. UI registers the launcher and calls `launch`. Repository only persists the parsed pair. No `startActivity` in `data/` or `nostr/`. v1 does not need ContentResolver or a `NostrSigner` interface.

**Warning signs:**
`startActivity` in a non-UI file. Login function takes `Context` instead of returning an `Intent`. Tests that mock `Activity`.

**Phase to address:**
Amber login (layering). Same phase as the mutex: one in-flight Connect.

---

### Pitfall 11: Showing Connect when no signer is installed

**What goes wrong:**
Tap Connect, Android shows a blank resolve error or nothing. User thinks Boris is broken. Amethyst hides the external-signer button when `getExternalSignersInstalled` is empty and tells the user to install a signer.

**Why it happens:**
The install check was skipped, or it failed because `<queries>` is missing (Pitfall 1), so the button stays up.

**How to avoid:**
If the query is empty: do not offer Connect as a working action. Show that Amber is missing and point at install (GitHub / Zapstore / F-Droid for `com.greenart7c3.nostrsigner`). If the query is non-empty: show Connect. Do not deep-link a store as the only success path when Amber is already present.

**Warning signs:**
Connect is always enabled in the emulator without Amber. Copy says "rejected" when the real problem is "not installed."

**Phase to address:**
Amber login (Home / account affordance).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hardcode Amber package on first intent | Skips chooser code | Breaks other signers; wrong pin if extras are empty | Never for `get_public_key` |
| Full Amethyst / Quartz signer tree | "Correct" NIP-55 client | Huge surface for login-only | Never in this slice |
| `NostrSigner` interface + ContentResolver | Matches Dark Wisp later | Unused encrypt/sign paths, mutex complexity | After something actually signs |
| SharedPreferences without backup exclude | Fast persist | Identity rides along in `allowBackup` | Only if backup rules exclude the file |
| Re-call `get_public_key` to "refresh" | Feels like a session check | Fingerprinting + Amber popup | Never while logged in |
| Import `nsec` "just for debug" | Easy local test | Violates the product rule; leaks into UI | Never |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| NIP-55 intents | Set `package` on the first `get_public_key` | Omit package; store extras.`package` after approve |
| NIP-55 extras | Read only `signature`, or ignore `package` / `rejected` | `result` then `signature`; `package`; `rejected` vs `resultCode` |
| Amber pubkey | Persist raw extra | Decode `npub1` to hex; display npub |
| PackageManager | Query without `<queries>` | Manifest `<queries>` then `queryIntentActivities` |
| Compose launcher | Launch from ViewModel / `LaunchedEffect(Unit)` | Activity-owned launcher; user tap only; survive rotate |
| Dark Wisp bridge | Skip mutex; fire two Connect taps | One in-flight request; serialize like `SignerIntentBridge` |
| Missing Amber | Same error as reject / cancel | Dedicated install copy; hide or disable Connect |
| Backup | Default `allowBackup="true"` on the new prefs | Exclude login keys or disable backup |

## Performance Traps

Login does not have a scale problem. The traps are device-local.

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Two Connect taps, no mutex | Two Amber activities, first result dropped | Disable the button while pending; one launcher | First dual-tap on a slow device |
| `get_public_key` in a loop / splash | Amber flicker, battery, fingerprinting | Persist pair; never re-ask | Every process start |
| ContentResolver "health check" on launch | Null cursor treated as logout | Do not probe Amber while logged in | First resume after login |
| Huge `permissions` JSON on login | Amber shows a wall of kinds Boris will not use | Omit `permissions` for v1 (login only) | First Connect if copied from Dark Wisp AuthScreen |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Re-asking `get_public_key` while logged in | Fingerprinting; Amber confirms identity to Boris on a schedule | Store hex + package; ask once |
| Logging `result` / `signature` / extras | Pubkey and future signatures in logcat | Log outcome enum only |
| Accepting `nsec` in the login field | Key leaves Amber | No import UI; reject `nsec1` if any paste path exists |
| Backup of the login pair | Identity + signer binding leave the device | Backup exclude / `allowBackup` policy |
| Trusting a guessed package | Later calls (or a future sign) go to the wrong app | Persist extras.`package` only |
| Treating ContentResolver `rejected` as "try intent" | User chose always-reject; client nags | NIP-55: do not fall back to an intent when `rejected` is present (future sign path) |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Connect with no signer | Dead tap | Missing-Amber message + install pointer |
| Same string for Back and Reject | User cannot tell what happened | Cancelled vs rejected copy |
| Amber on every launch | Feels broken / spyware | Silent restore of stored npub |
| Hex shown as the account | Ugly, not copy-paste friendly | Show npub; keep hex in storage |
| Sign out leaves the pair | "Logged out" still has an identity | Delete hex + package together |
| Login spinner across Amber | Boris looks frozen behind Amber | Pending state; cancel = clear spinner |

## "Looks Done But Isn't" Checklist

- [ ] **Manifest queries:** `<queries>` for `nostrsigner` is in the merged manifest, not only in a comment
- [ ] **Install check:** false without Amber, true with Amber, on API 30+
- [ ] **First intent:** `get_public_key` has no `package` set
- [ ] **Extras:** parser accepts `result` or `signature`; reads `package`; honors `rejected`
- [ ] **Encoding:** Amber `npub1` becomes stored hex; UI shows npub
- [ ] **Session:** kill and relaunch does not open Amber
- [ ] **Rotate:** rotate while Amber is open, then approve, still logs in once
- [ ] **Back vs Reject:** Back is cancel; Reject is reject; neither is "not installed"
- [ ] **Sign out:** both hex and package gone; Connect works again
- [ ] **Backup:** login keys excluded, or restore without Amber does not look logged in
- [ ] **Layers:** no `startActivity` in domain code; launcher lives in Compose
- [ ] **Secrets:** no `nsec` request; no extra dumps in logs
- [ ] **Permissions JSON:** not copied from Dark Wisp's social-client kind list

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Missing `<queries>` | LOW | Add the NIP-55 block; reinstall; re-run the install check |
| Wrong extras | LOW | Fix the parser; do not migrate garbage rows; user Connects again |
| npub stored as hex key | LOW | On read, if value starts with `npub1`, decode once and rewrite |
| `get_public_key` every launch | LOW | Gate on stored pair; delete the splash call |
| Package pinned too early | MEDIUM | Stop setting package on login; clear stored package; user logs in again |
| Rotate drops the result | MEDIUM | Move launcher to Activity scope; persist before navigate; add a rotate UAT |
| Reject / Back mixed up | LOW | Split the three result paths; fix strings |
| Backup restore ghost login | MEDIUM | Exclude the file; if package missing, sign out |
| `nsec` field shipped | HIGH | Remove the field; wipe any stored key; treat as incident |

## Pitfall-to-Phase Mapping

All of these are the Amber login phase. Do not park them in a later bunker / sign-event phase. v1 does not sign.

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Missing `<queries>` | Amber login | Install check true with Amber on API 30+; false when uninstalled |
| Extra names (`result` / `signature` / `package`) | Amber login | Fixture intents for each extra name; live Amber approve stores both fields |
| npub vs hex | Amber login | Amber login stores 64-char hex; UI shows `npub1` |
| `get_public_key` every launch | Amber login | Cold start while logged in never starts Amber |
| Package pin on first login | Amber login | First intent has no package; stored package equals extras.`package` |
| Rotation while Amber is open | Amber login | Rotate + approve once; no second prompt; session present |
| Reject vs Back | Amber login | Reject copy vs cancel copy vs missing-Amber copy are distinct |
| Logging / `nsec` | Amber login | Grep: no `nsec` in app sources; logs do not print extras |
| `allowBackup` of identity | Amber login | Backup rules exclude the login store, or restore-without-Amber is logged out |
| Activity launcher / mutex | Amber login | Double-tap Connect opens Amber once; no `startActivity` in `data/` |
| Connect with no signer | Amber login | No Amber: install message, no dead Connect |

## Sources

- [NIP-55](https://github.com/nostr-protocol/nips/blob/master/55.md) (raw 2026-08-14): `<queries>`, hex pubkeys, omit `package` on first `get_public_key`, store pubkey + package, do not call `get_public_key` while logged in, extras `result` / `package` / `rejected`, `RESULT_OK`+`rejected` vs other `resultCode`
- [Amber README](https://github.com/greenart7c3/Amber): package `com.greenart7c3.nostrsigner`; points at NIP-55; GitHub vs F-Droid cert fingerprints differ
- [Dark Wisp `SignerIntentBridge`](https://github.com/barrydeen/dark-wisp-android/blob/main/app/src/main/kotlin/com/darkwisp/app/nostr/SignerIntentBridge.kt): mutex, `SignResult.Success` / `Rejected` / `Cancelled`, UI-owned launcher
- Dark Wisp `RemoteSigner` / `RemoteSignerBridge` (removed upstream in wisp#531; still the model): ContentResolver then intent; login `get_public_key` without package; `signature` then `result` on sign; login path decoded Amber `npub1` to hex and read extras.`package`
- [Amethyst `nip55-android-signer.md`](https://github.com/vitorpamplona/amethyst/blob/main/.claude/skills/auth-signers/references/nip55-android-signer.md): Activity context required; hide button if no signer; persist the approved signer package
- [Amethyst `IntentResult`](https://github.com/vitorpamplona/amethyst/blob/main/quartz/src/androidMain/kotlin/com/vitorpamplona/quartz/nip55AndroidSigner/api/foreground/intents/results/IntentResult.kt): extras `result`, `package`, `rejected`
- [NostrAndroid notes](https://github.com/chebizarro/NostrAndroid): older signers may still return `signature` instead of `result`
- Boris `app/src/main/AndroidManifest.xml`: `allowBackup="true"`, no `<queries>`
- `.planning/PROJECT.md` and `.planning/codebase/CONCERNS.md`: login shape, no `nsec`, backup warning

---
*Pitfalls research for: Amber / NIP-55 login in Boris*
*Researched: 2026-08-14*
