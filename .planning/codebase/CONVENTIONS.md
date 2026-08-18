# Coding Conventions

**Analysis Date:** 2026-08-14

Write new Kotlin under `org.dergigi.boris` only. Do not add files to `com.readwithboris`.

## Naming Patterns

**Files:**
- One primary type per file. Filename matches the type: `UrlExtractor.kt`, `ReaderViewModel.kt`, `HomeScreen.kt`.
- Place files in the matching package directory under `app/src/main/java/org/dergigi/boris/`.
- Tests: `{Type}Test.kt` or `{Type}{Slice}Test.kt` under `app/src/test/java/org/dergigi/boris/` (`UrlExtractorTest.kt`, `ReaderRepositoryParseTest.kt`).

**Functions:**
- camelCase. No `get`/`set` prefixes on Kotlin properties.
- Composables are PascalCase and named for the UI they own: `HomeScreen`, `ReaderScreen`, `BorisTheme`, `ImageGallery`.
- Event callbacks use `on` + noun/verb: `onRead`, `onBack`, `onOpenArticle`, `onOpenGallery`, `onDismiss`.
- Local UI handlers are nested `fun`s: `submit()`, `openOriginal()`, `shareArticle()` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`.
- ViewModel actions are verbs: `load()`, `openGallery()`, `closeGallery()`, `setGalleryIndex()` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`.

**Variables:**
- camelCase for locals and properties (`targetUrl`, `incomingUrl`, `canRead`).
- Private `MutableStateFlow` uses a leading underscore; public is the same name without it: `_state` / `state`, `_gallery` / `gallery` in `ReaderViewModel.kt`.
- `UPPER_SNAKE_CASE` for constants: `DEFAULT_ARTICLE_URL` in `app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt`, `URL_ARG` / `HOME` / `READER` in `ReaderViewModel.kt` and `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`.
- Color tokens are PascalCase + shade: `Zinc900`, `Indigo500`, `Paper` in `app/src/main/java/org/dergigi/boris/ui/theme/Color.kt`.

**Types:**
- PascalCase. No `I` prefix.
- `data class` for immutable models: `ReadableContent` in `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt`, `ImageGalleryState` in `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`.
- `sealed interface` + `data object` / `data class` for UI state: `ReaderUiState` in `ReaderViewModel.kt`.
- `object` for stateless utilities and route tables: `UrlExtractor`, `ImageStore`, `Routes`.
- `class` for repositories, ViewModels, and Activities: `ReaderRepository`, `ReaderViewModel`, `MainActivity`.

## Code Style

**Formatting:**
- Official Kotlin style (`kotlin.code.style=official` in `gradle.properties`).
- 4-space indent. Trailing commas on multiline argument and parameter lists.
- Double quotes for strings. Raw `"""` + `trimIndent()` for multiline fixtures and regex-heavy samples.
- Expression bodies for one-liners (`fun toProxyUrl(url: String): String = "https://r.jina.ai/$url"` in `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`).
- No ktlint, detekt, or Prettier. Match neighboring files by eye.

**Linting:**
- Android Lint only, configured in `app/build.gradle.kts`.
- `NullSafeMutableLiveData` is disabled. Do not re-enable it without a reason.
- Run: `./gradlew :app:lintDebug` (same task CI uses). Fatal errors fail the build; warnings do not.

**Compose:**
- Collect Flow in the screen wrapper; pass immutable state + lambdas into a `*Content` composable (`ReaderScreen` / `ReaderScreenContent` in `ReaderScreen.kt`).
- Use `MaterialTheme.colorScheme` and `MaterialTheme.typography` in screens. Raw `Color.*` tokens belong in `ui/theme/` (or the black overlay in `ImageGallery.kt`).
- Give icons a `contentDescription` string. Use `null` only when the image is decorative (`ZoomableImage` in `ImageGallery.kt`).
- Prefer `rememberSaveable` for form fields that must survive rotation (`HomeScreen.kt`).

## Import Organization

**Order:**
1. Android SDK (`android.*`)
2. AndroidX (`androidx.*`)
3. Third-party (`okhttp3`, `coil3`, `com.mikepenz`)
4. Kotlin / Java stdlib (`kotlin.*`, `kotlinx.*`, `java.*`)
5. App packages (`org.dergigi.boris.*`)

**Grouping:**
- No blank lines between import groups.
- One blank line after the `package` line, then imports, then a blank line, then the first declaration.
- Wildcard imports are not used. Import each symbol.

**Path Aliases:**
- Not applicable. Use fully qualified packages. Fully qualify a rare one-off (`java.net.URI`, `android.app.Activity`) instead of adding an import for a single use.

## Error Handling

**Patterns:**
- Throw `IOException` with a short human-readable message at IO boundaries (`ReaderRepository.fetch`, `ImageStore.fetch` / `writeToPictures`).
- Catch at the ViewModel (or UI) boundary. Map to `ReaderUiState.Error(message, url)` in `ReaderViewModel.load()`. Use `e.message` when present, otherwise a fallback string.
- Use `runCatching` for best-effort side effects (save/share/open). Do not crash the UI. Show a `Toast` in `ImageGallery.kt`.
- Swallow parse failures with `catch (_: Exception)` and a fallback value (`UrlExtractor.articleUrl`, `ImageStore.filenameFor`).
- On MediaStore write failure, delete the pending URI then rethrow (`ImageStore.writeToPictures`).

**Error Types:**
- Return `null` for expected "nothing found" cases (`UrlExtractor.extract`, `UrlExtractor.articleUrl`, `readingTimeLabel`).
- Throw only when an operation the caller asked for cannot complete (HTTP failure, empty body, MediaStore insert/write).
- Do not introduce a custom exception hierarchy. `IOException` and `Exception` are the current types.

## Logging

**Framework:** None. No `Log`, Timber, or `println` in `org.dergigi.boris`.

**Patterns:**
- Surface failures as UI state (`ReaderUiState.Error`) or a `Toast`.
- Do not add logging in new code unless a phase explicitly requires it.

## Comments

**When to Comment:**
- Do not comment the obvious. Live sources in `org.dergigi.boris` have no `//` or KDoc.
- Add a comment only for a non-obvious constraint (scheme denylist, MediaStore `IS_PENDING`, gesture thresholds).

**KDoc:**
- Not used. Public APIs are documented by name and types.

**TODO Comments:**
- None present. Do not leave `TODO` / `FIXME` in committed Kotlin. Track work in planning docs.

## Function Design

**Size:**
- Keep data helpers small and single-purpose (`extract`, `normalize`, `filenameFor`, `mimeFor`).
- Split a screen when it grows: wrapper collects state, `*Content` renders, private helpers (`ArticleBody`, `ZoomableImage`) own a subtree.
- `ImageGallery.kt` is the large-file exception. Extract a new private composable rather than growing `ImageGallery` further.

**Parameters:**
- Default arguments over overloads (`baseUrl: String? = null`, `viewModel: ReaderViewModel = viewModel()`).
- Inject testable deps via constructor defaults (`ReaderRepository(client: OkHttpClient = defaultClient)`).
- Composables take `Modifier = Modifier` last among layout params. Callbacks stay explicit lambdas, not a single events object.

**Return Values:**
- Early-return guards: blank URL, empty list, busy flag (`ReaderViewModel.load`, `ImageGallery`, `UrlExtractor.extract`).
- Prefer `String?` / `List` over exceptions for parse/extract.
- Expose `StateFlow` as the read-only type; keep `MutableStateFlow` private.

## Module Design

**Exports:**
- Public: types and functions other packages need (`UrlExtractor`, `ReaderRepository`, `ReadableContent`, screens, `BorisTheme`).
- `internal` for JVM-testable helpers that must stay hidden from other modules: `ReaderRepository.parse`, `readingTimeLabel` in `ReaderScreen.kt`.
- `private` for file-local helpers (`stripHtml`, `toProxyUrl`, `ClickableCoilImageTransformer`, `ZoomableImage`).
- Put shared constants on a `companion object` or a top-level `const val` in the file that owns them.

**Barrel Files:**
- Not used. Import the concrete file/type.
- Keep `ReaderUiState` next to `ReaderViewModel` and `ImageGalleryState` next to `ImageGallery`. Do not create a separate `models` package for a single type.
- File-private `data class` is fine for IO DTOs (`FetchedImage` in `ImageStore.kt`).

---

*Convention analysis: 2026-08-14*
*Update when patterns change*
