# Codebase Structure

**Analysis Date:** 2026-08-14

## Directory Layout

```
boris-android/
├── app/                              # Sole Android application module
│   ├── build.gradle.kts              # Namespace org.dergigi.boris, Compose, deps
│   ├── proguard-rules.pro            # Release ProGuard (minify off)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Activity, intents, FileProvider
│       │   ├── java/org/dergigi/boris/   # LIVE Kotlin sources
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/             # Fetch, parse, URL, images
│       │   │   └── ui/               # Nav, screens, theme
│       │   ├── java/com/readwithboris/   # LEFTOVER — do not use
│       │   └── res/                  # Fonts, themes, icons, FileProvider paths
│       └── test/java/org/dergigi/boris/data/  # JVM unit tests (live)
├── gradle/
│   ├── libs.versions.toml            # Version catalog
│   └── wrapper/                      # Gradle wrapper
├── scripts/                          # zapstore-publish.sh
├── third_party/source-serif-4/       # Font license (OFL)
├── build.gradle.kts                  # Root plugin aliases
├── settings.gradle.kts               # include(":app")
├── gradle.properties
├── zapstore.yaml                     # Zapstore publish config
├── CHANGELOG.md
├── README.md
└── LICENSE
```

Live Kotlin package tree:

```
app/src/main/java/org/dergigi/boris/
├── MainActivity.kt
├── data/
│   ├── ImageStore.kt
│   ├── ReadableContent.kt
│   ├── ReaderRepository.kt
│   └── UrlExtractor.kt
└── ui/
    ├── BorisApp.kt
    ├── home/
    │   └── HomeScreen.kt
    ├── reader/
    │   ├── ImageGallery.kt
    │   ├── ReaderScreen.kt
    │   └── ReaderViewModel.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Directory Purposes

**`app/`:**
- Purpose: The only Gradle module. All product code lives here.
- Contains: Android application sources, resources, JVM tests, module Gradle file
- Key files: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/proguard-rules.pro`

**`app/src/main/java/org/dergigi/boris/`:**
- Purpose: Live application package (matches `namespace` / `applicationId`)
- Contains: `MainActivity.kt` plus `data/` and `ui/`
- Key files: `app/src/main/java/org/dergigi/boris/MainActivity.kt`

**`app/src/main/java/org/dergigi/boris/data/`:**
- Purpose: Network, parse, URL hygiene, image persist/share
- Contains: Kotlin classes/objects, no Android UI
- Key files: `ReaderRepository.kt`, `UrlExtractor.kt`, `ImageStore.kt`, `ReadableContent.kt`

**`app/src/main/java/org/dergigi/boris/ui/`:**
- Purpose: Compose navigation shell and feature screens
- Contains: `BorisApp.kt`, feature subpackages, `theme/`
- Key files: `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`
- Subdirectories: `home/`, `reader/`, `theme/`

**`app/src/main/java/org/dergigi/boris/ui/home/`:**
- Purpose: Start screen (URL field)
- Contains: `HomeScreen.kt` only (no ViewModel)

**`app/src/main/java/org/dergigi/boris/ui/reader/`:**
- Purpose: Article reader and image overlay
- Contains: Screen, ViewModel, gallery, UI state types
- Key files: `ReaderScreen.kt`, `ReaderViewModel.kt`, `ImageGallery.kt`

**`app/src/main/java/org/dergigi/boris/ui/theme/`:**
- Purpose: Material3 color, type, and `BorisTheme`
- Contains: `Color.kt`, `Type.kt`, `Theme.kt`

**`app/src/main/java/com/readwithboris/`:**
- Purpose: Leftover duplicate sources from an old package name
- Contains: Parallel copies of Activity/UI/data
- Do not add files here. Do not fix bugs here.

**`app/src/main/res/`:**
- Purpose: Android resources
- Contains: `font/source_serif_4.ttf`, `font/source_serif_4_italic.ttf`, `values/`, `values-night/`, `xml/file_paths.xml`, mipmap/drawable launcher assets
- Key files: `app/src/main/res/xml/file_paths.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`

**`app/src/test/java/org/dergigi/boris/data/`:**
- Purpose: JVM unit tests mirroring the live data package
- Contains: `ReaderRepositoryParseTest.kt`, `UrlExtractorTest.kt`, `ImageStoreTest.kt`
- Leftover tests under `app/src/test/java/com/readwithboris/data/` are unused. Do not extend them.

**`gradle/`:**
- Purpose: Version catalog and wrapper
- Key files: `gradle/libs.versions.toml`

**`scripts/`:**
- Purpose: Release publish helper
- Key files: `scripts/zapstore-publish.sh`

**`third_party/source-serif-4/`:**
- Purpose: Upstream font license text (`OFL.txt`). Runtime fonts are copied into `app/src/main/res/font/`.

## Key File Locations

**Entry Points:**
- `app/src/main/java/org/dergigi/boris/MainActivity.kt`: Process/UI entry; share and VIEW intents
- `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`: NavHost and `Routes`
- `app/src/main/AndroidManifest.xml`: Launcher, SEND, VIEW, FileProvider, permissions

**Configuration:**
- `settings.gradle.kts`: `include(":app")`, Huawei/Google/Maven repos
- `build.gradle.kts`: Root plugin aliases
- `app/build.gradle.kts`: SDK 35, minSdk 26, Compose, signing from `local.properties`
- `gradle/libs.versions.toml`: Dependency versions
- `gradle.properties`: Gradle JVM/Android flags
- `app/proguard-rules.pro`: Release rules (minify currently disabled)
- `zapstore.yaml`: Zapstore metadata
- `local.properties`: SDK path and OEM signing props (gitignored)

**Core Logic:**
- `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt`: Jina fetch + parse
- `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`: Share-text and link URL rules
- `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`: Download, Pictures/Boris, share URI
- `app/src/main/java/org/dergigi/boris/data/ReadableContent.kt`: Article model
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`: Load + gallery state
- `app/src/main/java/org/dergigi/boris/ui/reader/ReaderScreen.kt`: Markdown reader UI
- `app/src/main/java/org/dergigi/boris/ui/home/HomeScreen.kt`: URL entry

**Testing:**
- `app/src/test/java/org/dergigi/boris/data/ReaderRepositoryParseTest.kt`
- `app/src/test/java/org/dergigi/boris/data/UrlExtractorTest.kt`
- `app/src/test/java/org/dergigi/boris/data/ImageStoreTest.kt`
- No `app/src/androidTest/` source set

**Documentation:**
- `README.md`: Build, signing, Zapstore
- `CHANGELOG.md`: Version notes
- `.planning/codebase/`: Mapper output (this file and siblings)

## Naming Conventions

**Files:**
- PascalCase `.kt` for every Kotlin type: `ReaderViewModel.kt`, `HomeScreen.kt`, `UrlExtractor.kt`
- Screen composable file matches the public `@Composable`: `ReaderScreen.kt` → `fun ReaderScreen`
- ViewModel file matches the class: `ReaderViewModel.kt`
- Theme tokens: `Color.kt`, `Type.kt`, `Theme.kt`
- Tests: `{Type}Test.kt` or `{Type}{Focus}Test.kt` (`UrlExtractorTest.kt`, `ReaderRepositoryParseTest.kt`)
- XML resources: snake_case (`file_paths.xml`, `ic_launcher.xml`)

**Directories:**
- Feature UI packages are lowercase singular: `ui/home/`, `ui/reader/`, `ui/theme/`
- Data is a flat `data/` package (no subpackages yet)
- Test packages mirror production: `org.dergigi.boris.data`

**Special Patterns:**
- UI state types sit in the ViewModel file (`ReaderUiState`) or next to the overlay (`ImageGalleryState` in `ImageGallery.kt`)
- Route constants live in `object Routes` inside `BorisApp.kt`, not a separate file
- Stateless screen content is `*ScreenContent` in the same file as `*Screen`
- No barrel `package` files or `index` equivalents

## Where to Add New Code

**New feature screen (for example history or settings):**
- Composable: `app/src/main/java/org/dergigi/boris/ui/<feature>/<Feature>Screen.kt`
- ViewModel only if the screen loads async or holds more than form state: `app/src/main/java/org/dergigi/boris/ui/<feature>/<Feature>ViewModel.kt`
- Register the route on `Routes` and add a `composable(...)` in `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`
- Keep Home-style local state in the screen if it is only a field + button (`HomeScreen.kt` has no ViewModel)

**New reader UI (chrome, typography, overlays):**
- Screen/overlay: `app/src/main/java/org/dergigi/boris/ui/reader/`
- State changes: `ReaderViewModel` / `ReaderUiState` in `app/src/main/java/org/dergigi/boris/ui/reader/ReaderViewModel.kt`
- Theme tokens: `app/src/main/java/org/dergigi/boris/ui/theme/Color.kt` or `Type.kt`

**New data / network / parse behavior:**
- Article fetch/parse: `app/src/main/java/org/dergigi/boris/data/ReaderRepository.kt` (or a new type in the same `data/` folder)
- URL rules: `app/src/main/java/org/dergigi/boris/data/UrlExtractor.kt`
- Image download/save/share: `app/src/main/java/org/dergigi/boris/data/ImageStore.kt`
- New model: `app/src/main/java/org/dergigi/boris/data/<Name>.kt`
- Tests: `app/src/test/java/org/dergigi/boris/data/<Name>Test.kt`

**New navigation destination:**
- Definition: `object Routes` in `app/src/main/java/org/dergigi/boris/ui/BorisApp.kt`
- Screen: `app/src/main/java/org/dergigi/boris/ui/<feature>/`
- Do not add a second Activity or a new manifest `<activity>`

**New Android resource:**
- Strings: `app/src/main/res/values/strings.xml`
- XML theme / window: `app/src/main/res/values/themes.xml` and `values-night/themes.xml`
- Fonts: `app/src/main/res/font/` (wire in `app/src/main/java/org/dergigi/boris/ui/theme/Type.kt`)
- FileProvider paths: `app/src/main/res/xml/file_paths.xml`
- Permissions / intent filters: `app/src/main/AndroidManifest.xml`

**New dependency:**
- Version + library alias: `gradle/libs.versions.toml`
- `implementation(libs....)` in `app/build.gradle.kts`

**New JVM unit test:**
- Mirror the production package: `app/src/test/java/org/dergigi/boris/<same/path>/<Type>Test.kt`
- Prefer testing `data/` (no Robolectric/instrumentation in repo today)
- Do not add tests under `app/src/test/java/com/readwithboris/`

**Utilities:**
- Shared non-UI helpers: `app/src/main/java/org/dergigi/boris/data/` (current home for objects)
- If a true UI helper is needed, keep it `internal` in the feature file (see `readingTimeLabel` in `ReaderScreen.kt`) rather than a new top-level `util/` package

**Do not put new code in:**
- `app/src/main/java/com/readwithboris/`
- `app/src/test/java/com/readwithboris/`
- `.cursor/gsd-core/`
- `app/build/` or root `build/`

## Special Directories

**`app/src/main/java/com/readwithboris/`:**
- Purpose: Leftover old package. Not the live app.
- Generated: No
- Committed: Yes
- Action: Ignore when adding features. Delete only as an explicit cleanup task.

**`app/build/` and `/build`:**
- Purpose: Gradle outputs
- Generated: Yes
- Committed: No (`.gitignore`)

**`local.properties`:**
- Purpose: Android SDK path and OEM signing props
- Generated: Local
- Committed: No

**`keystore/` and `*.jks` / `*.keystore`:**
- Purpose: Signing material
- Generated: No
- Committed: No (`.gitignore`)

**`third_party/source-serif-4/`:**
- Purpose: Font license
- Generated: No
- Committed: Yes

**`.planning/`:**
- Purpose: GSD planning artifacts
- Generated: By GSD commands
- Committed: Per project convention

---

*Structure analysis: 2026-08-14*
*Update when directory structure changes*
