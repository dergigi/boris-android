# Technology Stack

**Analysis Date:** 2026-08-14

## Languages

**Primary:**
- Kotlin 2.1.21 - All application and unit-test code under `app/src/main/java/org/dergigi/boris/` and `app/src/test/java/org/dergigi/boris/`
- JVM target 17 (`kotlinOptions.jvmTarget` and `JavaVersion.VERSION_17` in `app/build.gradle.kts`)

**Secondary:**
- XML - Android manifest and resources in `app/src/main/AndroidManifest.xml` and `app/src/main/res/`
- Gradle Kotlin DSL - Build scripts: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`
- TOML - Version catalog in `gradle/libs.versions.toml`
- Bash - Release publish script `scripts/zapstore-publish.sh`

## Runtime

**Environment:**
- Android Runtime (ART) on device/emulator
- `minSdk` 26, `targetSdk` 35, `compileSdk` 35, `buildToolsVersion` 35.0.0 (`app/build.gradle.kts`)
- Application ID and namespace: `org.dergigi.boris` (`app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`)
- App version: `versionName` 0.0.3, `versionCode` 3 (`app/build.gradle.kts`)
- JDK 17 required to compile (source/target compatibility in `app/build.gradle.kts`)

**Package Manager:**
- Gradle 8.13 via wrapper (`gradle/wrapper/gradle-wrapper.properties`, `./gradlew`)
- Android Gradle Plugin 8.7.3 (`gradle/libs.versions.toml`)
- Version catalog is the source of truth: add libraries and plugins in `gradle/libs.versions.toml`, then reference them from `app/build.gradle.kts`
- Lockfile: Gradle wrapper + version catalog. No npm/Cargo lockfile. Dependency resolution is centralized (`RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts`)

## Frameworks

**Core:**
- Jetpack Compose (BOM `2025.06.01`) - UI toolkit. Use Material 3 (`androidx.compose.material3`) and Material Icons Extended
- Navigation Compose 2.9.2 - In-app routes in `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt` (`home`, `reader?url={url}`)
- AndroidX Activity Compose 1.10.1 - `ComponentActivity` + `setContent` in `app/src/main/java/org/dergigi/boris/MainActivity.kt`
- AndroidX Lifecycle 2.9.2 - `ViewModel`, `viewModelScope`, `collectAsStateWithLifecycle` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt` and `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`
- AndroidX Core KTX 1.16.0 - `FileProvider`, window insets helpers

**Testing:**
- JUnit 4.13.2 - JVM unit tests only (`testImplementation` in `app/build.gradle.kts`)
- Test location: `app/src/test/java/org/dergigi/boris/`
- No AndroidX Test, Espresso, Compose UI test, or instrumented `androidTest` source set

**Build/Dev:**
- Android Gradle Plugin 8.7.3 - Application plugin on `:app`
- Kotlin Android plugin 2.1.21 + Kotlin Compose compiler plugin (`libs.plugins.kotlin.compose` in `app/build.gradle.kts`)
- Compose build feature enabled (`buildFeatures { compose = true }` in `app/build.gradle.kts`)
- Gradle properties in `gradle.properties`: AndroidX on, official Kotlin code style, non-transitive R classes, 2 GB heap, parallel builds
- Release minify is off; ProGuard files are declared (`proguard-android-optimize.txt`, empty `app/proguard-rules.pro`)

## Key Dependencies

**Critical:**
- OkHttp 4.12.0 - Synchronous HTTPS client for article fetch (`app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`) and image download (`app/src/main/java/org/dergigi/boris/data/ImageStore.kt`). Timeouts: connect 20s, read 45s
- Coil 3.2.0 (`coil-compose`, `coil-network-okhttp`) - Async image loading in the gallery (`coil3.compose.AsyncImage` in `app/src/main/java/org/dergigi/boris/ui/reader/ImageGallery.kt`) and markdown images
- multiplatform-markdown-renderer 0.35.0 (Android + Material 3 + Coil3 artifacts) - Article body rendering in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt` via `com.mikepenz.markdown.m3.Markdown` and `Coil3ImageTransformerImpl`

**Infrastructure:**
- Android platform MediaStore + `ContentResolver` - Persist images to `Pictures/Boris` (`app/src/main/java/org/dergigi/boris/data/ImageStore.kt`)
- AndroidX `FileProvider` - Share cached images from `cacheDir/shared/` (`app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/file_paths.xml`)
- Bundled Source Serif 4 variable fonts at `app/src/main/res/font/source_serif_4.ttf` and `app/src/main/res/font/source_serif_4_italic.ttf`, wired in `app/src/main/java/org/dergigi/boris/ui/theme/Type.kt`
- No Retrofit, Room, DataStore, Hilt/Koin, Kotlinx Serialization, Firebase, or Play Services

## Configuration

**Environment:**
- Runtime app needs no API keys. Article fetch uses a public Jina Reader URL prefix in `ReaderRepository.toProxyUrl`
- Release signing reads `OEM_STORE_FILE`, `OEM_STORE_PASSWORD`, `OEM_KEY_ALIAS`, `OEM_KEY_PASSWORD` from gitignored `local.properties`, falling back to process environment (`localProp()` in `app/build.gradle.kts`). Signing config is created only when all four are set and the store file exists
- Zapstore publish reads `SIGN_WITH` from the environment or gitignored `.env` (`scripts/zapstore-publish.sh`). Optional: `GITHUB_TOKEN`, `KEYSTORE_PASSWORD`, `SKIP_CERT_LINK`, `ZSP_EXTRA_ARGS`
- `local.properties` and `.env` are present locally and gitignored (`.gitignore`). Do not commit them. Do not read or copy their values into docs
- XML theme shells: `app/src/main/res/values/themes.xml` (light) and `app/src/main/res/values-night/themes.xml` (dark). Compose colors live in `app/src/main/java/org/dergigi/boris/ui/theme/Color.kt` and `Theme.kt`

**Build:**
- `settings.gradle.kts` - Root name `Boris`, single module `:app`, Huawei Cloud Maven first, then `google()`, `mavenCentral()`, `gradlePluginPortal()`
- `build.gradle.kts` - Root plugin aliases only (`apply false`)
- `app/build.gradle.kts` - SDK, signing, Compose, dependencies
- `gradle/libs.versions.toml` - Versions, libraries, plugins
- `gradle.properties` - JVM and Android/Kotlin flags
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.13 distribution
- Lint: `NullSafeMutableLiveData` disabled in `app/build.gradle.kts`

## Platform Requirements

**Development:**
- macOS, Linux, or Windows with JDK 17 and Android SDK 35
- Android Studio or command-line SDK (`sdk.dir` typically in `local.properties`)
- Network access to Huawei Cloud Maven (`https://repo.huaweicloud.com/repository/maven/`), Google Maven, and Maven Central (`settings.gradle.kts`)
- Debug build: `./gradlew :app:assembleDebug` (`README.md`)
- Unit tests: `./gradlew :app:test`
- Device or emulator API 26+ to run the APK
- Release signing keystore is local-only (paths listed in `README.md`; files under `keystore/` and `*.jks` / `*.keystore` are gitignored)

**Production:**
- Sideloaded / store APK for `org.dergigi.boris` on Android 8.0+ (API 26)
- Distributed via Zapstore (`zapstore.yaml`, `scripts/zapstore-publish.sh`) and GitHub Releases (`https://github.com/dergigi/boris-android`)
- HTTPS only (`android:usesCleartextTraffic="false"` in `app/src/main/AndroidManifest.xml`)
- Permissions: `INTERNET`; `WRITE_EXTERNAL_STORAGE` only through API 28 (`app/src/main/AndroidManifest.xml`)
- No Play Store, F-Droid, or CI workflow files in this repo

---

*Stack analysis: 2026-08-14*
*Update after major dependency changes*
