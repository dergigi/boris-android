<!-- refreshed: 2026-08-14 -->
# Architecture

**Analysis Date:** 2026-08-14

## System Overview

```text
┌─────────────────────────────────────────────────────────────────┐
│                     Platform / Activity                          │
│              `app/src/main/java/org/dergigi/boris/`              │
│   MainActivity  ·  AndroidManifest  ·  FileProvider              │
└────────────────────────────┬────────────────────────────────────┘
                             │ incomingUrl
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Navigation + Theme Shell                       │
│                 `.../org/dergigi/boris/ui/`                      │
│          BorisApp + Routes          BorisTheme                   │
│         `ui/BorisApp.kt`            `ui/theme/`                  │
├────────────────────────────┬────────────────────────────────────┤
│  Home                       │  Reader                            │
│  `ui/home/HomeScreen.kt`    │  `ui/reader/ReaderScreen.kt`       │
│  local rememberSaveable     │  `ui/reader/ReaderViewModel.kt`    │
│                             │  `ui/reader/ImageGallery.kt`       │
└────────────┬───────────────┴─────────────────┬──────────────────┘
             │ UrlExtractor                    │ fetch / save
             ▼                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Data Layer                               │
│              `.../org/dergigi/boris/data/`                       │
│  UrlExtractor   ReaderRepository   ReadableContent   ImageStore  │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  r.jina.ai article proxy  ·  origin image URLs  ·  MediaStore    │
└─────────────────────────────────────────────────────────────────┘
```

Live application ID and Kotlin package: `org.dergigi.boris` (`app/build.gradle.kts`). A leftover `com.readwithboris` tree still exists under `app/src/main/java/com/readwithboris/` and `app/src/test/java/com/readwithboris/`. Do not edit or extend it.

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| MainActivity | Host Compose, parse launcher/share/VIEW intents, pass `incomingUrl` | `app/src/main/java/org/dergigi/boris/MainActivity.kt` |
| BorisApp | NavHost, route encoding, deep-link navigation from share/VIEW | `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` |
| Routes | Home and reader route strings | `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` |
| HomeScreen | URL field, extract/normalize, emit `onRead` | `app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt` |
| ReaderViewModel | Load article, hold `ReaderUiState` and gallery overlay | `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` |
| ReaderScreen | Loading/error/ready UI, share, open original, markdown | `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` |
| ImageGallery | Full-screen pager, zoom, save/share images | `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt` |
| ReaderRepository | Fetch via `https://r.jina.ai/{url}`, parse markdown/HTML | `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` |
| UrlExtractor | Pull URLs from share text, normalize, resolve article/image links | `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt` |
| ReadableContent | Article model plus `body` (markdown or stripped HTML) | `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt` |
| ImageStore | Download images, write Pictures/Boris, FileProvider share | `app/src/main/java/org/dergigi/boris/data/ImageStore.kt` |
| BorisTheme | Light/dark Material3 schemes and Source Serif typography | `app/src/main/java/org/dergigi/boris/ui/theme/Theme.kt` |

## Pattern Overview

**Overall:** Single-module Android Compose reader (MVVM-lite, no DI)

**Key Characteristics:**
- One `:app` Gradle module (`settings.gradle.kts`). One `ComponentActivity`.
- Navigation Compose with two destinations: `home` and `reader?url={url}`.
- Presentation state lives in `ReaderViewModel` (`StateFlow`). Home keeps local `rememberSaveable` state.
- Data layer is plain Kotlin: one repository class, two singleton objects, one data class.
- Network goes through OkHttp. Article text is proxied by Jina Reader (`r.jina.ai`). Images load from origin URLs via Coil.
- No Hilt, Koin, Application class, Room, or DataStore.

## Layers

**Platform layer:**
- Purpose: Android process entry, intent filters, permissions, FileProvider
- Location: `app/src/main/java/org/dergigi/boris/MainActivity.kt`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/file_paths.xml`
- Contains: Activity lifecycle, `ACTION_SEND` / `ACTION_VIEW` parsing
- Depends on: `UrlExtractor`, `BorisApp`, `BorisTheme`
- Used by: Android system (launcher, share sheet, browser VIEW)

**Navigation / shell layer:**
- Purpose: Route graph and Material theme wrapper
- Location: `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`, `app/src/main/java/org/dergigi/boris/ui/theme/`
- Contains: `Routes`, `NavHost`, `BorisTheme`, color/type tokens
- Depends on: screen composables
- Used by: `MainActivity`

**Presentation layer:**
- Purpose: Screens, ViewModel UI state, in-screen overlays
- Location: `app/src/main/java/org/dergigi/boris/ui/home/`, `app/src/main/java/org/dergigi/boris/ui/reader/`
- Contains: Composables, `ReaderViewModel`, `ReaderUiState`, `ImageGalleryState`
- Depends on: data layer types and objects; Navigation callbacks from `BorisApp`
- Used by: `BorisApp`

**Data layer:**
- Purpose: URL hygiene, article fetch/parse, image download/persist
- Location: `app/src/main/java/org/dergigi/boris/data/`
- Contains: `ReaderRepository`, `UrlExtractor`, `ImageStore`, `ReadableContent`
- Depends on: OkHttp, Android `Context`/`MediaStore`/`FileProvider` (ImageStore only)
- Used by: Activity, screens, and ViewModel

## Data Flow

### Primary Request Path (paste URL and read)

1. User types or accepts the default URL on `HomeScreen` (`app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt:36`)
2. `UrlExtractor.extract` normalizes the string (`app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt:10`)
3. `onRead` navigates to `Routes.reader(url)` (`app/src/main/java/org/dergigi/boris/ui/BorisApp.kt:47`)
4. `ReaderViewModel` reads `SavedStateHandle` arg `url` and calls `load()` (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt:21`)
5. `ReaderRepository.fetch` GETs `https://r.jina.ai/{url}` on `Dispatchers.IO` (`app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:11`)
6. `parse` builds `ReadableContent` from Jina markdown or HTML fallback (`app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:28`)
7. `ReaderUiState.Ready` is collected by `ReaderScreen` and rendered as Markdown (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:70`)

### Share or VIEW intent

1. Manifest delivers `ACTION_SEND` (text/plain) or `ACTION_VIEW` (http/https) to `MainActivity` (`app/src/main/AndroidManifest.xml:27`)
2. `urlFrom` uses `UrlExtractor.extract` for share text, or `intent.dataString` for VIEW (`app/src/main/java/org/dergigi/boris/MainActivity.kt:35`)
3. `onNewIntent` updates `incomingUrl` because launch mode is `singleTop` (`app/src/main/java/org/dergigi/boris/MainActivity.kt:29`)
4. `BorisApp` `LaunchedEffect(incomingUrl)` navigates to the reader (`app/src/main/java/org/dergigi/boris/ui/BorisApp.kt:33`)

### In-article link and image gallery

1. Markdown links go through a custom `UriHandler` that prefers `UrlExtractor.articleUrl` and navigates another reader destination (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:225`)
2. Image taps collect `UrlExtractor.imageUrls` and call `ReaderViewModel.openGallery` (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt:237`)
3. `ImageGallery` overlays the reader; save/share call `ImageStore` on `Dispatchers.IO` (`app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt:122`)
4. `ImageStore.save` writes `Pictures/Boris` via MediaStore; `shareIntent` uses FileProvider cache path `shared/` (`app/src/main/java/org/dergigi/boris/data/ImageStore.kt:46`, `app/src/main/res/xml/file_paths.xml`)

**State Management:**
- Reader article and gallery: `MutableStateFlow` on `ReaderViewModel` (`state`, `gallery`)
- Home URL field: `rememberSaveable` in `HomeScreen`
- Incoming share/VIEW URL: Activity-level `mutableStateOf` on `MainActivity`
- No persisted reading history, cache, or settings store
- Navigation back stack is the only multi-article stack

## Key Abstractions

**Repository:**
- Purpose: Blocking network + parse for one article URL
- Examples: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`
- Pattern: Instantiated by the ViewModel (`ReaderViewModel` constructs `ReaderRepository()`). OkHttp client is a companion singleton. `parse` is `internal` for JVM unit tests.

**Singleton utility object:**
- Purpose: Stateless helpers shared by Activity, screens, and tests
- Examples: `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`, `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`
- Pattern: Kotlin `object`. `ImageStore` holds its own OkHttp client.

**UI state sealed type:**
- Purpose: Exhaustive loading / ready / error for the reader
- Examples: `ReaderUiState` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`
- Pattern: `sealed interface` next to the ViewModel. Screen splits into `ReaderScreen` (collects) and `ReaderScreenContent` (stateless).

**Route object:**
- Purpose: Encode/decode the reader URL query arg
- Examples: `Routes` in `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`
- Pattern: URL-encode in `Routes.reader`; decode in `ReaderViewModel.decodeUrl`

**Content model:**
- Purpose: Normalized article payload
- Examples: `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt`
- Pattern: Data class with computed `body` (prefer markdown, else strip HTML)

## Entry Points

**Launcher / share / VIEW:**
- Location: `app/src/main/java/org/dergigi/boris/MainActivity.kt`
- Triggers: `MAIN`/`LAUNCHER`, `ACTION_SEND` text/plain, `ACTION_VIEW` http/https (`app/src/main/AndroidManifest.xml`)
- Responsibilities: Edge-to-edge, theme, extract URL, host `BorisApp`

**Compose navigation:**
- Location: `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`
- Triggers: Home submit, incoming URL, in-article http(s) links
- Responsibilities: `NavHost` start at `Routes.HOME`; reader arg is `ReaderViewModel.URL_ARG`

**Unit tests:**
- Location: `app/src/test/java/org/dergigi/boris/data/`
- Triggers: `./gradlew :app:test`
- Responsibilities: Pure JVM tests for parse, URL extraction, image filename/mime. No `androidTest` source set.

## Architectural Constraints

- **Threading:** UI on main. `ReaderViewModel.load` wraps `repository.fetch` in `withContext(Dispatchers.IO)`. `ImageGallery` does the same for `ImageStore.save` / `shareIntent`. `ReaderRepository.fetch` and `ImageStore` methods are blocking; do not call them on the main thread.
- **Global state:** Shared OkHttp clients on `ReaderRepository` companion and `ImageStore`. `UrlExtractor` and `ImageStore` are process-wide objects. No `Application` subclass.
- **Circular imports:** Not present. UI imports data; data does not import UI. Keep that one-way.
- **Dependency injection:** None. `viewModel()` default factory + constructor `ReaderRepository()`. Do not introduce Hilt/Koin in a single feature.
- **Single Activity:** New screens are NavHost destinations, not new Activities. Manifest already uses `singleTop` so share/VIEW reuse the same Activity.
- **Package:** New Kotlin belongs under `org.dergigi.boris`. Ignore `com.readwithboris`.
- **Cleartext:** `android:usesCleartextTraffic="false"` in `app/src/main/AndroidManifest.xml`. Article URLs are normalized to `https://` by `UrlExtractor.normalize`.

## Anti-Patterns

### Editing leftover `com.readwithboris` sources

**What happens:** Parallel copies exist under `app/src/main/java/com/readwithboris/` and `app/src/test/java/com/readwithboris/`.
**Why it's wrong:** Namespace is `org.dergigi.boris` (`app/build.gradle.kts`). Changes there never ship.
**Do this instead:** Edit `app/src/main/java/org/dergigi/boris/` and tests under `app/src/test/java/org/dergigi/boris/`.

### Fetching or parsing inside a Composable

**What happens:** Putting OkHttp or Jina parse in `ReaderScreen` / `HomeScreen`.
**Why it's wrong:** Blocks composition, skips `ReaderUiState`, and is untested. Fetch/parse already live in `ReaderRepository`.
**Do this instead:** Call data types from a ViewModel (or keep Home's extract-only call to `UrlExtractor`). Put new network work in `app/src/main/java/org/dergigi/boris/data/`.

### Adding a second Activity for a new screen

**What happens:** New `Activity` + manifest entry for settings, history, etc.
**Why it's wrong:** Share/VIEW and `incomingUrl` assume one `singleTop` Activity.
**Do this instead:** Add a route on `Routes` in `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` and a screen under `ui/<feature>/`.

### Constructing a new OkHttpClient per request

**What happens:** `OkHttpClient()` inside `fetch` or a Composable.
**Why it's wrong:** Loses connection reuse. Clients already live on `ReaderRepository` companion and `ImageStore`.
**Do this instead:** Inject via constructor (as `ReaderRepository(client)`) or reuse the existing object client.

## Error Handling

**Strategy:** Catch at the ViewModel / UI action boundary; show a message, never crash the Activity.

**Patterns:**
- `ReaderViewModel.load` catches `Exception`, maps to `ReaderUiState.Error` with `e.message` or a fallback string (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt:62`)
- `ReaderRepository.fetch` throws `IOException` on non-success HTTP (`app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt:20`)
- `ImageGallery` uses `runCatching` and Toasts (`app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt:128`)
- `ImageStore.writeToPictures` deletes the MediaStore row if the write fails (`app/src/main/java/org/dergigi/boris/data/ImageStore.kt:118`)
- Blank reader URL becomes `ReaderUiState.Error` without a network call (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt:53`)

## Cross-Cutting Concerns

**Logging:** Not detected. No Timber, no `Log` wrapper.

**Validation:** `UrlExtractor` at the intent and home-field boundary. Reader treats a blank URL as an error state. Link clicks drop non-http schemes via `UrlExtractor.articleUrl`.

**Authentication:** Not applicable. No user accounts. Release signing keys are read from gitignored `local.properties` in `app/build.gradle.kts` (OEM_* props). Do not commit keystores.

**Permissions:** `INTERNET` always; `WRITE_EXTERNAL_STORAGE` maxSdk 28 (`app/src/main/AndroidManifest.xml`). API 29+ save path uses MediaStore `IS_PENDING` without that permission.

**Theming:** XML `Theme.Boris` is a transparent-system-bar splash (`app/src/main/res/values/themes.xml`). Runtime colors/type come from Compose `BorisTheme`.

---

*Architecture analysis: 2026-08-14*
*Update when major patterns change*
