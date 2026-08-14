# Codebase Concerns

**Analysis Date:** 2026-08-14

Live package and application ID: `org.dergigi.boris`. Edit only `app/src/main/java/org/dergigi/boris/` and `app/src/test/java/org/dergigi/boris/`.

The leftover `com/readwithboris` tree is **not present** on disk or in git (`app/src/main/java/` contains only `org/`). Do not recreate `app/src/main/java/com/readwithboris/` or `app/src/test/java/com/readwithboris/`. Parallel map docs that still mention those paths are stale.

## Tech Debt

**Hardcoded Jina Reader proxy and format-coupled parser:**
- Issue: Every article GET is `https://r.jina.ai/{url}`. Parse assumes Jina's `Title:` / `Markdown Content:` labels.
- Files: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`
- Why: Matches the Boris webapp path with no local extractor.
- Impact: A Jina outage, rate limit, or payload-format change blanks the reader for every URL. HTML fallback still stores raw markup and then strips tags, so images and structure disappear.
- Fix approach: Isolate `toProxyUrl` and the four regexes behind a small `ReadableSource` interface. Add a second source (direct fetch + local readability, or a self-hosted proxy) without changing `ReaderViewModel`. Keep `parse` as the test seam.

**Two OkHttp clients, no shared limits:**
- Issue: `ReaderRepository` and `ImageStore` each build a process-wide `OkHttpClient` (20s connect, 45s read). Coil 3 brings a third pool. No max body size, no `User-Agent`, no shared cache.
- Files: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`, `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`, `app/build.gradle.kts`
- Impact: Three connection pools, unbounded `body.string()` / `body.bytes()` can OOM on a large article or image. Some hosts block the default OkHttp UA.
- Fix approach: One shared client (constructor-inject into `ReaderRepository`, reuse from `ImageStore`). Cap response size (for example 2MB text, 8MB image). Set a Boris `User-Agent`. Do not construct a client per request.

**Release R8 is off and ProGuard rules are empty:**
- Issue: `isMinifyEnabled = false`. `app/proguard-rules.pro` is a 0-byte file. Release still lists the default optimize ruleset.
- Files: `app/build.gradle.kts`, `app/proguard-rules.pro`
- Impact: Larger APK, no shrinking. Turning minify on later will break Compose, Coil, OkHttp, and `multiplatform-markdown-renderer` without keep rules.
- Fix approach: Add keep rules for those libraries first, enable minify on a debug-minified smoke build, then set `isMinifyEnabled = true` for release.

**URL passed through `URLEncoder` plus Navigation decode plus `URLDecoder`:**
- Issue: `Routes.reader` uses `URLEncoder.encode`. `NavType.StringType` decodes the query arg. `ReaderViewModel.decodeUrl` decodes again.
- Files: `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`
- Impact: Encoded characters in the original URL (`%26`, `%2F`, `+`) can change meaning before `fetch`. See Known Bugs.
- Fix approach: Treat Navigation's already-decoded arg as the URL. Drop `decodeUrl`, or encode with a scheme that is not decoded twice (Base64 URL-safe). Add JVM tests for `Routes.reader` + decode on URLs that contain `%` and `+`.

**HTML fallback is not a real HTML reader:**
- Issue: If the Jina body lacks `Markdown Content:`, the full HTML is stored. `ReadableContent.body` runs `stripHtml` and the result is fed to the Markdown renderer. `imageUrls` is called on that stripped `body`, so `<img>` tags are already gone.
- Files: `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt`, `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`
- Impact: Fallback articles show flattened text, no images, broken lists/tables.
- Fix approach: Run `UrlExtractor.imageUrls` on raw `html` before strip. Or render HTML with a dedicated path instead of Markdown. Add parse tests for an HTML page that contains `<img>`.

**Huawei Maven listed first:**
- Issue: Plugin and dependency repos resolve `https://repo.huaweicloud.com/repository/maven/` before `google()` and `mavenCentral()`.
- Files: `settings.gradle.kts`
- Why: Comment says `dl.google.com` is not always reachable.
- Impact: Stale or divergent artifacts can win over Google Maven. Supply-chain trust sits on a third-party mirror.
- Fix approach: Keep the mirror as a fallback, not the first repo. Prefer `google()` / `mavenCentral()` and only add the Huawei repo when a build actually cannot reach Google.

## Known Bugs

**Share or VIEW URL re-opens after Back, then rotate:**
- Symptoms: User opens a shared link, presses Back to Home, rotates the device, and the reader opens again.
- Files: `app/src/main/java/org/dergigi/boris/MainActivity.kt`, `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`
- Trigger: `ACTION_SEND` or `ACTION_VIEW` into Boris, Back to Home, configuration change.
- Workaround: Leave the reader on screen, or force-stop the app.
- Root cause: `onCreate` always sets `incomingUrl` from the same intent. `LaunchedEffect(incomingUrl)` navigates whenever composition restarts. `launchSingleTop` does not help once the user has popped to Home.
- Fix: Consume the URL once (clear `incomingUrl` after navigate, or store a handled-intent extra / `savedInstanceState` flag). Only navigate from `onNewIntent`, not from every `onCreate` after the first.

**Double-decode changes article URLs:**
- Symptoms: Articles whose path or query contains percent-encoded reserved characters fetch the wrong URL or 404.
- Files: `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`
- Trigger: Open `https://example.com/foo?q=a%26b` (or any URL with `%2F` in a path segment).
- Workaround: None in-app.
- Root cause: `URLEncoder.encode` then `NavType.StringType` decode then `URLDecoder.decode`. `%26` becomes `&` and splits the query.
- Fix: Stop calling `decodeUrl` on an already-decoded Navigation arg. Cover with a JVM test.

**`http://` images fail under cleartext block:**
- Symptoms: Gallery / inline images stay empty when the markdown still points at `http://`.
- Files: `app/src/main/AndroidManifest.xml` (`usesCleartextTraffic="false"`), `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`, `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`
- Trigger: Article whose image URLs are `http://` (normalize only adds `https://` when the scheme is missing; it does not upgrade `http://`).
- Workaround: Open original in a browser.
- Root cause: Coil and `ImageStore.fetch` hit the origin URL. Cleartext is disabled app-wide.
- Fix: Upgrade extracted image URLs to `https://` when the host supports it, or proxy images through the same HTTPS path as articles.

**In-flight `load()` is not cancelled:**
- Symptoms: Fast retry or a second navigation can show a stale article if the older GET finishes last.
- Files: `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`
- Trigger: Tap Try again twice, or open a second reader destination while the first fetch is still running (in-article links push a new destination; the old ViewModel stays).
- Workaround: Wait for the spinner.
- Root cause: Each `load()` launches a new coroutine. No `Job` cancel, no generation token.
- Fix: Hold a `Job` and cancel it at the start of `load()`, or use a single `viewModelScope` collector.

**VIEW intents skip `UrlExtractor`:**
- Symptoms: A VIEW `dataString` that is not a clean http(s) article URL still becomes `incomingUrl`.
- Files: `app/src/main/java/org/dergigi/boris/MainActivity.kt`, `app/src/main/AndroidManifest.xml`
- Trigger: `ACTION_VIEW` with an unexpected `dataString` (extra fragments, non-article schemes that still used http/https).
- Workaround: Paste the URL on Home instead.
- Root cause: Share uses `UrlExtractor.extract`; VIEW uses `intent.dataString` raw.
- Fix: Run VIEW through `UrlExtractor.extract` / `articleUrl` the same way as share.

## Security Considerations

**Catch-all http(s) VIEW filter:**
- Risk: Boris is offered as a handler for every web link. A user can send an arbitrary URL to Jina (privacy leak of the reading list) and then render whatever markdown Jina returns.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/org/dergigi/boris/MainActivity.kt`
- Current mitigation: `usesCleartextTraffic="false"`. `UrlExtractor.articleUrl` drops `javascript`, `data`, `blob`, and a few other schemes. FileProvider is `exported="false"` and limited to `cache-path` `shared/` in `app/src/main/res/xml/file_paths.xml`.
- Recommendations: Narrow the VIEW filter (https only, or a documented host list) if the share-sheet noise is unwanted. Always run VIEW through `UrlExtractor`. Keep rendering in Compose Markdown (no WebView / JS).

**Unbounded downloads from article-controlled URLs:**
- Risk: A malicious or compromised article can list huge or non-image URLs. `ImageStore.fetch` writes the entire body into a `ByteArray`. `saveAll` walks every URL. SVG (`image/svg+xml`) can be saved to Pictures.
- Files: `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`
- Current mitigation: Filename characters are restricted to `[A-Za-z0-9._-]`. Non-http schemes in `UrlExtractor.articleUrl` are dropped. Share files stay under app cache `shared/`.
- Recommendations: Enforce Content-Length / max bytes, require `Content-Type` `image/*` (deny SVG if you do not want scriptable files in Pictures), and unique share filenames. Treat `..` as an invalid filename.

**Reading list leaves the device:**
- Risk: Every article URL is sent to `r.jina.ai`. Jina sees what the user reads. Image GETs go to the origin CDN (origin sees the device IP).
- Files: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`, `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`
- Current mitigation: No accounts, no first-party analytics. HTTPS only for the proxy.
- Recommendations: Document this in the app UI. Offer a self-hosted or on-device extractor for users who do not want a third party in the path.

**Backup and signing material:**
- Risk: `android:allowBackup="true"` can include cache (`shared/` images). Release passwords live in gitignored `local.properties`. `scripts/zapstore-publish.sh` can load `SIGN_WITH` (nsec) from `.env` and export `KEYSTORE_PASSWORD`.
- Files: `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `scripts/zapstore-publish.sh`, `README.md`
- Current mitigation: `local.properties`, `.env`, `*.jks`, `*.keystore`, and `keystore/` are in `.gitignore`.
- Recommendations: Keep those ignores. Do not log `SIGN_WITH` or keystore passwords. Consider `allowBackup="false"` until there is user data worth backing up.

**Scheme denylist is incomplete (low residual risk):**
- Risk: `nonHttpSchemes` omits `file`, `content`, `intent`, `ftp`, `package`.
- Files: `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`
- Current mitigation: `articleUrl` ends in `extract()`, which only accepts `http://` / `https://` or host-like strings. `file://` and `intent://` therefore become null.
- Recommendations: Add those schemes to the denylist anyway so a future change to `extract()` cannot open them. Add tests for `file:`, `content:`, and `intent:`.

## Performance Bottlenecks

**Full article body loaded on the calling thread's IO dispatcher:**
- Problem: `response.body?.string()` loads the entire Jina payload. The Markdown composable then walks the whole `body` on the UI tree.
- Files: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`
- Measurement: Not measured in-repo. Read timeout is 45s, so a hung proxy can spin the reader for 45s with no cancel-on-Back if the destination stays in the stack.
- Cause: No size cap, no disk cache, no incremental parse.
- Improvement path: Cap bytes, stream until the markdown block, cache the last N articles on disk if history is added. Keep fetch off the main thread (already `Dispatchers.IO`).

**Download all images is serial and unbounded:**
- Problem: `ImageStore.saveAll` downloads one image at a time, each up to the 45s read timeout, each fully in RAM.
- Files: `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`
- Measurement: Not measured. Worst case is roughly `N * 45s` with the gallery `busy` flag set.
- Cause: Sequential `forEachIndexed` + `fetch` + MediaStore write. Failures are swallowed per URL.
- Improvement path: Cap `N`, cap bytes, bound concurrency to 2, surface a cancel action. Share a single OkHttp client with Coil.

**Share cache is never cleaned:**
- Problem: `ImageStore.shareIntent` writes `cacheDir/shared/{filename}` and leaves the file.
- Files: `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`, `app/src/main/res/xml/file_paths.xml`
- Cause: No unique prefix and no eviction.
- Improvement path: Name files with a nonce, delete on a short delay after share, or wipe `shared/` on process start.

## Fragile Areas

**Jina parse regexes:**
- Files: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`
- Why fragile: Title and markdown cuts are line-label regexes (`Title:`, `Markdown Content:`). A wording change on `r.jina.ai` silently falls through to HTML mode.
- Common failures: Empty title, whole payload treated as HTML, `stripHtml` body with no images.
- Safe modification: Change regexes only with new cases in `app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt`. Keep `parse` `internal`.
- Test coverage: Two happy-path tests (markdown + HTML). No tests for missing title, extra headers, or `Markdown Content:` appearing in the article body.

**Image gallery gestures:**
- Files: `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt` (largest source file, 391 lines)
- Why fragile: Custom `awaitEachGesture` zoom/pan/double-tap plus `HorizontalPager`, plus save/share permission flow, all in one composable.
- Common failures: Pager swipe fighting zoom, `busy` stuck if a future change throws outside `runCatching`, storage permission callback after leave composition.
- Safe modification: Keep pager state in `ImageGalleryState` / ViewModel (rotation already depends on that). Extract `ZoomableImage` tests only if you add Compose UI tests. Do not move save/share onto the main thread.
- Test coverage: None for the gallery. `ImageStoreTest` covers filename/MIME only.

**Incoming-URL navigation:**
- Files: `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`, `app/src/main/java/org/dergigi/boris/MainActivity.kt`
- Why fragile: Activity `mutableStateOf` plus `LaunchedEffect(incomingUrl)` is the only bridge from intents to NavHost.
- Common failures: Duplicate reader entries, re-navigation after rotate, same-URL share ignored if the string does not change.
- Safe modification: Add a consumed-intent flag before changing the effect. Do not add a second Activity for share/VIEW (`singleTop` is assumed).
- Test coverage: None.

**Theme window cast:**
- Files: `app/src/main/java/org/dergigi/boris/ui/theme/Theme.kt`
- Why fragile: `view.context as android.app.Activity` inside `SideEffect`.
- Common failures: Crash if the view context is wrapped (or in a non-Activity host). Previews are skipped via `isInEditMode`.
- Safe modification: Use `findActivity()` / `LocalActivity` and no-op when missing. Do not put more window work in `BorisTheme`.
- Test coverage: None.

**Default `viewModel()` factory + `SavedStateHandle`:**
- Files: `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`
- Why fragile: Changelog 0.0.1 already records a crash when creating `ReaderViewModel`. The screen relies on the Compose default factory providing `SavedStateHandle` from the nav back stack entry.
- Common failures: Custom factory that forgets `SavedStateHandle` will crash again. Blank `url` arg becomes `ReaderUiState.Error`.
- Safe modification: Keep the URL in the route arg. If you add a factory, use `AbstractSavedStateViewModelFactory` / `CreationExtras`.
- Test coverage: None.

## Scaling Limits

**In-memory, one-article process:**
- Current capacity: One fetched article string plus Coil's image cache plus any in-flight `ImageStore` byte arrays. No Room, DataStore, or on-disk article cache.
- Limit: Device RAM and the 45s OkHttp read timeout. A long markdown article plus "Download all" is the first place it falls over.
- Symptoms at limit: Process kill (OOM), ANR-looking spinner, Toasts "Couldn't save images".
- Scaling path: Persist last-read URL and body, evict images, cap download batch size. Do not keep a back-stack of live ViewModels for deep in-article walks (pop or reuse one reader destination).

**No history store:**
- Current capacity: Navigation back stack only.
- Limit: Process death drops everything except the current route URL in `SavedStateHandle`.
- Symptoms at limit: User cannot return to an article after kill. Home field is `rememberSaveable` only.
- Scaling path: DataStore or a small Room table for recent URLs. Keep the data layer free of UI types.

## Dependencies at Risk

**`r.jina.ai` (no SDK, hardcoded host):**
- Risk: Third-party proxy with no contract in-repo. Format, ToS, and rate limits can change. Single point of failure for reading.
- Impact: `ReaderRepository.fetch` fails, reader shows `ReaderUiState.Error`.
- Migration plan: `ReadableSource` interface (see Tech Debt). Ship a fallback or let "Open original" remain the escape hatch.

**Huawei Cloud Maven mirror:**
- Risk: First-listed repo in `settings.gradle.kts`. Mirror lag or compromise affects AGP, Compose BOM, and every library.
- Impact: Wrong or unavailable artifacts; builds fail or resolve unexpected versions.
- Migration plan: Put `google()` and `mavenCentral()` first. Keep the Huawei repo as an opt-in fallback.

**`com.mikepenz:multiplatform-markdown-renderer` 0.35.0:**
- Risk: All article rendering, link handling, and image taps go through this library (`ReaderScreen.kt`). API is Compose-specific (`ImageTransformer`, `Coil3ImageTransformerImpl`).
- Impact: A breaking upgrade blanks the reader or drops clickable images.
- Migration plan: Pin in `gradle/libs.versions.toml`. Upgrade in a branch with a golden markdown fixture (headings, images, links, tables).

**Android / Gradle pin vs current platform:**
- Risk: `compileSdk` / `targetSdk` 35, AGP 8.7.3, Gradle 8.13, Kotlin 2.1.21, Compose BOM `2025.06.01` (`app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`). Android 16 (API 36) is already the current platform as of this audit.
- Impact: Play target-API requirements and library updates will force a bump. Edge-to-edge and photo-picker APIs may shift.
- Migration plan: Bump `compileSdk`/`targetSdk` together, then AGP/Gradle. Keep minify off until ProGuard rules exist.

**OkHttp 4.12.0:**
- Risk: 4.x is maintenance; 5.x is the current line. Still fine, but Coil 3 already depends on OkHttp.
- Impact: Duplicate OkHttp on the classpath if versions drift.
- Migration plan: Let the shared client (when introduced) use the version Coil 3 pulls, or align the catalog.

## Missing Critical Features

**No reading history, bookmarks, or offline cache:**
- Problem: Killing the process loses everything except the current nav URL. There is no way to reopen yesterday's article without the original share.
- Blocks: "Continue reading", offline use, a recents list on Home.
- Current workaround: System recents / re-share from the browser.
- Implementation complexity: Low for a recent-URL list (DataStore). Medium for cached bodies and expiry.

**No reader settings:**
- Problem: Font size, theme override, and default proxy are fixed (`BorisTheme` follows system dark; body is 21sp Source Serif in `app/src/main/java/org/dergigi/boris/ui/theme/Type.kt`).
- Blocks: Accessibility (larger type) and users who want a local extractor.
- Implementation complexity: Low for font scale + theme toggle (DataStore). Higher if a second fetch backend is added.

**No CI:**
- Problem: No `.github/` workflows. Tests and release builds run only on a developer machine.
- Blocks: Catching a broken `parse` regex or a package rename before a Zapstore publish (`scripts/zapstore-publish.sh`).
- Implementation complexity: Low. `./gradlew :app:testDebugUnitTest` and `assembleDebug` on GitHub Actions is enough for this module.

**No crash or error reporting:**
- Problem: No Timber, no `Log` wrapper, no Crashlytics/Sentry (`ARCHITECTURE.md` cross-cutting section).
- Blocks: Diagnosing field failures of Jina timeouts and gallery saves.
- Implementation complexity: Low for `Log` tags. Product decision before adding a third-party reporter (conflicts with the "no accounts / just reading" stance in `zapstore.yaml`).

## Test Coverage Gaps

**Network fetch and image IO:**
- What's not tested: `ReaderRepository.fetch`, `ImageStore.save` / `saveAll` / `shareIntent` / `writeToPictures`.
- Files: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`, `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`
- Risk: Proxy URL construction, HTTP error mapping, MediaStore pending flags, FileProvider authority `${applicationId}.fileprovider`.
- Priority: High for `toProxyUrl` + HTTP error mapping (extract and unit-test). Medium for MediaStore (needs Robolectric or instrumented tests).
- Difficulty: JVM unit tests cannot touch `Context`. Keep using `internal` seams; do not mock OkHttp unless a phase adds MockWebServer.

**ViewModel, navigation, and intents:**
- What's not tested: `ReaderViewModel.load` / gallery state, `Routes.reader` + `decodeUrl`, `MainActivity.urlFrom`, `BorisApp` incoming-URL effect.
- Files: `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`, `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`, `app/src/main/java/org/dergigi/boris/MainActivity.kt`
- Risk: The rotate-after-share bug and double-decode bug can ship again.
- Priority: High
- Difficulty: `decodeUrl` / `Routes.reader` are JVM-testable today. ViewModel needs `SavedStateHandle` + a fake repository (no mock library is in use; prefer a constructor-injected repository).

**Compose UI and gallery:**
- What's not tested: `HomeScreen`, `ReaderScreenContent`, `ImageGallery`, `ZoomableImage`, permission + Toast paths.
- Files: `app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`, `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`
- Risk: Gesture regressions, broken error/retry actions, gallery not restoring on rotate.
- Priority: Medium
- Difficulty: No `androidTest` source set, no Compose UI test dependency. `ReaderScreenContent` is already stateless and is the right entry if a phase adds that harness.

**Pure helpers still untested:**
- What's not tested: `readingTimeLabel` in `ReaderScreen.kt`, `stripHtml` in `ReadableContent.kt`, `UrlExtractor` rejection of `file:` / `content:` / `intent:`, `looksLikeUrl` for IPs and localhost.
- Files: `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`, `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt`, `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`
- Risk: Reading-time off-by-one is minor. `stripHtml` missing an entity or leaving a `<script>` fragment is worse if HTML fallback is common.
- Priority: Medium for `stripHtml` and extra schemes. Low for `readingTimeLabel`.
- Difficulty: Low. These are JVM-pure. Follow `UrlExtractorTest` style in `app/src/test/java/org/dergigi/boris/`.

**No coverage gate:**
- What's not tested: There is no JaCoCo/Kover task. `TESTING.md` documents this.
- Risk: New code can land with zero tests and CI will not exist to fail.
- Priority: Low until CI exists.
- Difficulty: Low to add a report; do not block builds on a percentage until the gaps above are closed.

---

*Concerns audit: 2026-08-14*
*Update as issues are fixed or new ones discovered*
