# Phase 1 Pattern Map

**Mapped:** 2026-08-14
**Phase:** 01-amber-login

## File Classification

| New file | Role | Analog |
|----------|------|--------|
| `nostr/RemoteSignerBridge.kt` | utility / intent | `data/UrlExtractor.kt` (object + pure helpers) |
| `nostr/Nip19.kt` | utility | `data/UrlExtractor.kt` |
| `data/Session.kt` | model | `data/ReadableContent.kt` |
| `data/SessionStore.kt` | store | `data/ImageStore.kt` (`object` + `Context`) |
| `ui/auth/AuthViewModel.kt` | ViewModel | `ui/reader/ReaderViewModel.kt` |
| `ui/auth/AuthBar.kt` | composable | `ui/home/HomeScreen.kt` |
| `ui/home/HomeScreen.kt` | modify | itself |
| `ui/BorisApp.kt` | modify | itself (wire ViewModel/launcher) |
| `AndroidManifest.xml` | config | itself |
| `res/values/strings.xml` | config | itself |
| `Nip19Test.kt` | test | `data/UrlExtractorTest.kt` |
| `SessionStoreTest.kt` | test | `data/UrlExtractorTest.kt` |

No existing SharedPreferences analog. SessionStore is the first prefs user. Copy `ImageStore`'s `object` + `Context` shape, not its MediaStore internals.

## Pattern Assignments

### AuthViewModel ← `ReaderViewModel.kt`

Copy:
- `_state` / `state` `MutableStateFlow` + `asStateFlow()` (lines 23-24)
- Verb actions (`load()`, `openGallery()`)
- `sealed interface` UI state at file bottom (lines 79+)
- Catch at ViewModel boundary, map to error state (lines 57-67)

Do not copy: `SavedStateHandle` URL arg, `Dispatchers.IO` fetch, gallery overlay.

### AuthBar + Home ← `HomeScreen.kt`

Copy:
- `MaterialTheme.colorScheme` / `typography` (lines 57-67)
- `Button` + `Text` fillMaxWidth (lines 82-88)
- Nested `fun` handlers (`submit()`)
- `rememberSaveable` for the URL field only; auth state comes from ViewModel

Add AuthBar above the greeting or below Read. Do not add a new NavHost route.

### SessionStore ← `ImageStore.kt`

Copy: `object` with `Context` methods. New: `getSharedPreferences("boris_session", MODE_PRIVATE)` keys `pubkey_hex`, `signer_package`. `load(): Session?`, `save(Session)`, `clear()`.

### Nip19 / RemoteSignerBridge ← `UrlExtractor.kt`

Copy: `object`, expression-body helpers, no Android UI. Tests in `app/src/test/java/org/dergigi/boris/` with JUnit 4 `assertEquals` like `UrlExtractorTest.kt`.

### Manifest ← `AndroidManifest.xml`

Keep share/VIEW filters. Add `<queries>` for `nostrsigner` + `BROWSABLE` before `<application>`. `allowBackup` is currently `true` (line 10); exclude `boris_session` via `dataExtractionRules` / `fullBackupContent` (D-09). Do not drop `INTERNET`.

### Strings ← `strings.xml`

Only `app_name` today. Add connect, sign out, missing Amber, Zapstore / F-Droid / GitHub labels as resources (AuthBar `contentDescription` / buttons).

### Wiring ← `BorisApp.kt`

`HomeScreen` is constructed in `composable(Routes.HOME)` (lines 46-49). Put `rememberLauncherForActivityResult` here or in `HomeScreen` (Activity-scoped Compose). Do not launch Amber from a ViewModel. Do not add a new start destination.

## Shared Patterns

- Package `org.dergigi.boris` only. Never `com.readwithboris`.
- No Hilt/Koin. Construct ViewModel the same way `ReaderScreen` does (`viewModel()`).
- Official Kotlin style, 4-space indent, trailing commas.
- JVM unit tests only; no instrumented tests in this repo yet.

---
*Phase: 01-amber-login*
