# External Integrations

**Analysis Date:** 2026-08-14

## APIs & External Services

**Payment Processing:**
- Not applicable. No billing SDK or payment endpoints.

**Email/SMS:**
- Not applicable. No mail or SMS provider.

**Article extraction:**
- Jina Reader (`https://r.jina.ai/{url}`) - Converts a web article into a text/markdown payload the reader parses
  - SDK/Client: OkHttp 4.12.0 in `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`
  - Auth: None. No API key, bearer token, or env var
  - Request: GET, `Accept: text/plain`, 20s connect / 45s read timeout
  - Response: Prefer the `Title:` / `Markdown Content:` text format; fall back to raw HTML title + body (`ReaderRepository.parse`)
  - Call this only from `ReaderRepository.fetch`. Do not add a second HTTP path for article text
  - Same extraction path as the companion webapp at `https://readwithboris.com/` (`README.md`, `zapstore.yaml`)

**Image hosts:**
- Arbitrary `http`/`https` image URLs found in article markdown or HTML
  - Display: Coil 3 + OkHttp (`coil3.compose.AsyncImage` in `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`; `Coil3ImageTransformerImpl` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`)
  - Download/share: OkHttp GET in `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`
  - Auth: None. URLs come from article content via `UrlExtractor.imageUrls` / `UrlExtractor.articleUrl` in `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`
  - Rate limits: Host-dependent. No client-side limiter

**Default sample article:**
- `https://www.citadel21.com/the-paranoid-wallet` - Placeholder URL when the home field is empty (`DEFAULT_ARTICLE_URL` in `app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt`). Not an API contract

**Maven repositories (build-time):**
- Huawei Cloud Maven (`https://repo.huaweicloud.com/repository/maven/`), Google Maven, Maven Central, Gradle Plugin Portal (`settings.gradle.kts`)
  - Used only to resolve Gradle plugins and Android libraries
  - Auth: None in-repo

## Data Storage

**Databases:**
- None. No Room, SQLite, DataStore, SharedPreferences, or remote database client

**File Storage:**
- Device MediaStore Pictures album `Boris` - Saved article images (`ImageStore.writeToPictures` in `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`)
  - API 29+: `RELATIVE_PATH` + `IS_PENDING`. API 28 and below: `WRITE_EXTERNAL_STORAGE` (declared `maxSdkVersion="28"` in `app/src/main/AndroidManifest.xml`; requested at runtime in `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`)
- App cache `cacheDir/shared/` - Temporary files for image share intents (`ImageStore.shareIntent`)
  - Exposed through `FileProvider` authority `${applicationId}.fileprovider` (`org.dergigi.boris.fileprovider`)
  - Paths: `app/src/main/res/xml/file_paths.xml` (`<cache-path name="shared" path="shared/" />`)
- Bundled fonts only: `app/src/main/res/font/source_serif_4.ttf`, `app/src/main/res/font/source_serif_4_italic.ttf`
- No cloud object storage

**Caching:**
- None configured. OkHttp clients in `ReaderRepository` and `ImageStore` have no cache directory. Coil uses its default in-memory/disk cache for displayed images only

## Authentication & Identity

**Auth Provider:**
- None in the Android app. No accounts, sessions, or tokens (`zapstore.yaml`: "No accounts. No Nostr. Just reading.")

**OAuth Integrations:**
- None

**Publisher identity (release tooling only, not in-app):**
- Zapstore / Nostr publisher pubkey in `zapstore.yaml`: `npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc`
  - Signing method: `SIGN_WITH` env var or gitignored `.env` (`nsec1…`, `bunker://…`, or `browser`) — `scripts/zapstore-publish.sh`
  - Optional NIP-C1 certificate link via `zsp identity --link-key` and `nak event` to `wss://relay.zapstore.dev`, `wss://relay.damus.io`, `wss://relay.primal.net`
  - Keystore password for linking: `KEYSTORE_PASSWORD` or `OEM_STORE_PASSWORD` from `local.properties`

## Monitoring & Observability

**Error Tracking:**
- None. No Sentry, Crashlytics, or similar SDK

**Analytics:**
- None. No product analytics or advertising IDs

**Logs:**
- None dedicated. Fetch failures become `ReaderUiState.Error` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`. Gallery save/share failures use `Toast` in `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`. No Timber / structured logger

## CI/CD & Deployment

**Hosting:**
- Zapstore - Android APK distribution for `org.dergigi.boris`
  - Config: `zapstore.yaml` (repo `https://github.com/dergigi/boris-android`, `metadata_sources: github`, icon `zapstore-icon.png`, notes from `CHANGELOG.md`)
  - Publish: `scripts/zapstore-publish.sh` via `zsp` (https://github.com/zapstore/zsp)
  - Default APK path: `app/build/outputs/apk/release/app-release.apk`
- GitHub Releases - Version tags documented in `CHANGELOG.md` (`https://github.com/dergigi/boris-android/releases`)
- Companion website (not an app backend): `https://readwithboris.com/`

**CI Pipeline:**
- None. No `.github/workflows` directory. Builds and publishes are local (`./gradlew :app:assembleDebug`, `./gradlew :app:assembleRelease`, then the Zapstore script)

## Environment Configuration

**Development:**
- Required for compile: Android SDK path in gitignored `local.properties` (Android Studio writes `sdk.dir`)
- Required for signed release: `OEM_STORE_FILE`, `OEM_STORE_PASSWORD`, `OEM_KEY_ALIAS`, `OEM_KEY_PASSWORD` in `local.properties` or the environment (`app/build.gradle.kts`, `README.md`)
- Required for Zapstore publish: `SIGN_WITH` in the environment or gitignored `.env` (`scripts/zapstore-publish.sh`)
- Optional publish vars: `GITHUB_TOKEN` (falls back to `gh auth token`), `KEYSTORE_PASSWORD`, `SKIP_CERT_LINK=1`, `ZSP_EXTRA_ARGS`
- Secrets location: gitignored `local.properties`, `.env`, `keystore/`, `*.jks`, `*.keystore` (`.gitignore`). Both `local.properties` and `.env` exist on this machine — note existence only; never commit or quote values
- Mock/stub services: Unit tests parse fixture strings only (`app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt`, `UrlExtractorTest.kt`, `ImageStoreTest.kt`). No Jina mock server

**Staging:**
- Not detected. No staging flavor, product flavor, or separate application ID

**Production:**
- Secrets stay on the publisher machine (`local.properties` / `.env`)
- Release signing is optional at build time; without the four OEM keys, `assembleRelease` produces an unsigned (or debug-signed) APK
- Failover: If Jina Reader fails, the UI offers "Open original" via `Intent.ACTION_VIEW` (`app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`)

## Webhooks & Callbacks

**Incoming:**
- None. This is a client app with no HTTP server

**Android system intents (inbound):**
- `MAIN` / `LAUNCHER` - Cold start (`app/src/main/AndroidManifest.xml`, `app/src/main/java/org/dergigi/boris/MainActivity.kt`)
- `ACTION_SEND` + `text/plain` - Shared text; first URL extracted by `UrlExtractor.extract`
- `ACTION_VIEW` + `http`/`https` - Open-with links; `intent.dataString` becomes the reader URL
- Activity is `singleTop`; `onNewIntent` updates `incomingUrl`

**Outgoing:**
- OkHttp GET `https://r.jina.ai/{articleUrl}` - Article load (`ReaderRepository`)
- OkHttp GET `{imageUrl}` - Image save/share (`ImageStore`)
- Coil GET `{imageUrl}` - On-screen images
- `Intent.ACTION_VIEW` - Open original article or image URL in another app
- `Intent.ACTION_SEND` - Share article text or a `FileProvider` image URI
- `zsp publish` - Zapstore upload from `scripts/zapstore-publish.sh` (release machine only)
- Retry: Reader has a manual "Try again" (`ReaderViewModel.load`). Image save-all continues after individual failures (`ImageStore.saveAll`). No automatic HTTP retry interceptor

---

*Integration audit: 2026-08-14*
*Update when adding/removing external services*
