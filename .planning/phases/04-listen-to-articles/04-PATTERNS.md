# Phase 4: Listen to articles - Pattern Map

**Mapped:** 2026-08-18
**Files analyzed:** 18
**Analogs found:** 17 / 18

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `tts/TtsPlayback.kt` | store | event-driven | `data/SettingsSync.kt` | role-match |
| `tts/TtsPlaybackService.kt` | service | event-driven | *(none — first FGS)*; use RESEARCH + `AndroidManifest.xml` provider pattern | none |
| `tts/TtsText.kt` | utility | transform | `data/MarkdownInline.kt` + `data/Footnotes.kt` | exact |
| `tts/TtsSpeed.kt` | utility | transform | `ui/settings/ReadingFonts.kt` | role-match |
| `tts/TtsLanguage.kt` | utility | transform | `data/UserSettings.kt` theme accessors | role-match |
| `tts/TtsPreview.kt` | utility | transform | `ui/settings/ReadingPreview.kt` `PreviewCopy` | exact |
| `data/UserSettings.kt` | model | CRUD | itself (`withDouble` / TTS keys in `DEFAULT_JSON`) | exact |
| `ui/theme/Color.kt` | config | — | itself (`FindMark`) | exact |
| `ui/reader/ReaderViewModel.kt` (`PaintedHighlight`) | model | — | itself (`find` flag) | exact |
| `ui/reader/HighlightMarks.kt` | utility | transform | itself (`find` paint path) | exact |
| `ui/reader/ArticleFind.kt` | utility | transform | itself (transient mark factory) | exact |
| `ui/reader/ReaderScreen.kt` | component | event-driven | itself (top bar + find + `VolumeKeys`) | exact |
| `ui/reader/VolumeKeys.kt` | hook | event-driven | itself (`Handle(enabled=…)`) | exact |
| `ui/shell/TtsMiniPlayer.kt` | component | event-driven | `ui/shell/BorisBottomBar.kt` | role-match |
| `ui/BorisApp.kt` | component | event-driven | itself (`bottomBar` slot) | exact |
| `ui/settings/TtsSection.kt` | component | request-response | `ui/settings/ReadingSection.kt` | exact |
| `ui/settings/SettingsScreen.kt` | component | request-response | itself (`SettingsCategory.Reading`) | exact |
| `AndroidManifest.xml` | config | — | itself (`<queries>` + `FileProvider`) | role-match |
| `tts/*Test.kt` + `UserSettingsTest.kt` | test | — | `MarkdownInlineTest.kt` / `VolumeKeysTest.kt` / `UserSettingsTest.kt` | exact |

## Pattern Assignments

### `tts/TtsPlayback.kt` (store, event-driven)

**Analog:** `data/SettingsSync.kt` (process-wide `object` + `StateFlow`; UI collects, mutators write)

**Imports / StateFlow pattern** (lines 1–14):
```kotlin
package org.dergigi.boris.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsSync {
    private val _settings = MutableStateFlow(UserSettings.defaults())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()
```

**Core mutation pattern** (lines 21–28):
```kotlin
fun apply(next: UserSettings) {
    dirty = true
    _settings.value = next
}

fun markSynced(next: UserSettings) {
    _settings.value = next
    dirty = false
}
```

**Secondary analog:** `data/ReadingPositionStore.kt` — `version: StateFlow` bump for observers; `fraction(url)` for D-14 start index (lines 17–20, 54).

**Copy for planner:** `object TtsPlayback` with private `MutableStateFlow<TtsSession?>`, public `asStateFlow()`, methods `start` / `pause` / `resume` / `stop` / `skip` / `cycleSpeed` / `preview`. Service and UI both call the object; do not put session state in `ReaderViewModel`.

---

### `tts/TtsPlaybackService.kt` (service, event-driven)

**Analog:** **No in-repo `Service` exists.** Closest structural analogs:

1. Manifest component registration — `AndroidManifest.xml` `FileProvider` (`exported="false"`) lines 81–89
2. Platform snippets in `04-RESEARCH.md` (FGS + `MediaSession` + `AudioFocusRequest`)
3. Process singleton ownership from `SettingsSync` / `TtsPlayback` (service mutates the same `StateFlow`)

**Manifest registration pattern** (lines 81–89):
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
```

**Queries pattern** (existing TTS-adjacent package visibility, lines 10–16) — add a sibling intent for `android.intent.action.TTS_SERVICE`:
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="nostrsigner" />
    </intent>
</queries>
```

**Core pattern (from RESEARCH, implement in service):** `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)` after `startForegroundService`; `MediaSession.Callback` → `TtsPlayback.resume/pause/stop/skip`; `TextToSpeech` + `UtteranceProgressListener`; `AudioFocusRequest` with `CONTENT_TYPE_SPEECH` (pause on transient loss, do not duck).

**Error handling:** On init / `LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED`, push an error message into `TtsPlayback` StateFlow; keep Listen visible (D-11). Never `Log` utterance text.

---

### `tts/TtsText.kt` (utility, transform)

**Analog:** `data/MarkdownInline.kt` + `data/Footnotes.kt` + `data/ReadableContent.kt`

**Plain-text flatten** (`MarkdownInline.kt` lines 7–10):
```kotlin
object MarkdownInline {
    fun plain(text: String): String = flatten(text).first
```

**Footnote expand before render** (`ReaderScreen.kt` line 878 — same order for speak):
```kotlin
val markdownBody = remember(content.body) { Footnotes.expand(content.body) }
```

**Content fields** (`ReadableContent.kt` lines 3–17):
```kotlin
data class ReadableContent(
    val url: String,
    val title: String? = null,
    ...
    val summary: String? = null,
    ...
) {
    val body: String
        get() = markdown?.takeIf { it.isNotBlank() } ?: html?.let(::stripHtml).orEmpty()
}
```

**Core pattern (RESEARCH + analogs):**
```kotlin
fun paragraphs(content: ReadableContent): List<String> {
    val blocks = mutableListOf<String>()
    content.title?.trim()?.takeIf { it.isNotEmpty() }?.let { blocks += it }
    content.summary?.trim()?.takeIf { it.isNotEmpty() }?.let { blocks += it }
    blocks += splitMarkdownBlocks(Footnotes.expand(content.body))
        .map { MarkdownInline.plain(it).replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotEmpty() }
    return blocks
}
```

**Start index** — mirror `ReadingProgress.MIN_RESTORE_FRACTION` (0.01f) via `ReadingPositionStore.fraction(url)`.

---

### `tts/TtsSpeed.kt` (utility, transform)

**Analog:** `ui/settings/ReadingFonts.kt` preset lists + cycle/select helpers

**Presets object** (lines 12–25):
```kotlin
object ReadingFonts {
    val ALL = listOf(
        ReadingFont("system", "System Default", serif = false),
        ...
    )
    val SIZES = listOf(16, 18, 21, 24, 28, 32)
```

**Write path for numeric setting** (`UserSettings.kt` lines 88–91 — integer-looking doubles stringify as `"1"`):
```kotlin
fun withDouble(key: String, value: Double): UserSettings {
    val raw = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    return overlay(key, JsonValue.Num(raw))
}
```

**Core pattern:** `SPEED_OPTIONS = doubleArrayOf(0.8, 1.0, …, 3.0)`, `DEFAULT = 2.1`, `cycle(current): Double` snaps unknown values to nearest preset then advances (pitfall 8). Pass value straight to `setSpeechRate`.

---

### `tts/TtsLanguage.kt` (utility, transform)

**Analog:** `data/UserSettings.kt` constrained string accessors (theme / feed view)

**Constrained mode accessor pattern** (lines 28–32):
```kotlin
val theme: String
    get() = when (val value = string("theme", "system")) {
        "light", "dark" -> value
        else -> "system"
    }
```

**Core pattern (match webapp resolution order from RESEARCH):**
```kotlin
fun resolveLanguage(
    mode: String,
    text: String,
    systemLang: String,
    detect: (String) -> String?,
): String? {
    if (mode != "system" && mode != "content") return mode
    if (mode == "content") detect(text)?.let { return it }
    if (mode == "system") return systemLang
    return detect(text) ?: systemLang
}
```

Dropdown values live beside `ReadingFonts.ALL`-style list: `system`, `content`, `en-US`, `en-GB`, `zh`, `es`, `hi`, `ar`, `fr`, `pt`, `de`, `ja`, `ru`. Language write updates three keys (see Shared Patterns).

---

### `tts/TtsPreview.kt` (utility, transform)

**Analog:** `ui/settings/ReadingPreview.kt` `PreviewCopy`

**Constant object** (lines 174–178):
```kotlin
internal object PreviewCopy {
    const val TITLE = "The Quick Brown Fox"
    const val P1 =
        "Lorem ipsum dolor sit amet, ..."
```

**Copy for planner:** `object TtsPreview { const val EXAMPLE_TEXT = "Boris aims to be…" }` — exact D-10 / UI-SPEC sentence; do not paraphrase.

---

### `data/UserSettings.kt` (model, CRUD)

**Analog:** itself — add typed getters next to existing scroll/TTS default keys

**Existing TTS defaults in `DEFAULT_JSON`** (lines 176–179):
```json
"ttsUseSystemLanguage":false,
"ttsDetectContentLanguage":true,
"ttsLanguageMode":"content",
"ttsDefaultSpeed":2.1,
```

**Accessor style** (lines 61–67, 130–133):
```kotlin
val volumeButtonScroll: Boolean get() = bool("volumeButtonScroll", true)
val volumeButtonScrollPercent: Int
    get() = int("volumeButtonScrollPercent", 90).coerceIn(25, 100)

private fun double(key: String, default: Double): Double {
    val raw = (values[key] as? JsonValue.Num)?.raw ?: return default
    return raw.toDoubleOrNull() ?: default
}
```

**Add:** `ttsDefaultSpeed`, `ttsLanguageMode`, `ttsUseSystemLanguage`, `ttsDetectContentLanguage`, `ttsFollowAlong` (default true) + `"ttsFollowAlong":true` in `DEFAULT_JSON`.

**Settings mutation path** — `SettingsViewModel.update` (lines 69–76):
```kotlin
fun update(transform: (UserSettings) -> UserSettings) {
    val next = transform(SettingsSync.settings.value)
    SettingsSync.apply(next)
    saveJob?.cancel()
    saveJob = viewModelScope.launch {
        delay(SAVE_DEBOUNCE_MS)
        requestSave(next)
    }
}
```

Do not invent a second settings store. Logged-out save no-ops at `requestSave` when `SessionStore.load` is null (same as font size today).

---

### `ui/theme/Color.kt` + `HighlightMarks.kt` + `PaintedHighlight` (follow-along mark)

**Analogs:** `FindMark` color; `ArticleFind.painted`; `drawHighlightMarks` find branch; `PaintedHighlight.find`

**Color token** (`Color.kt` lines 22–25):
```kotlin
val HighlightMine = Color(0xFFFDE047)
val HighlightFriends = Color(0xFFF97316)
val HighlightOther = Color(0xFF9333EA)
val FindMark = Color(0xFF93C5FD)
```

Add `val SpokenMark = Color(0xFF2DD4BF)` (UI-SPEC).

**Model flag** (`ReaderViewModel.kt` lines 45–56):
```kotlin
data class PaintedHighlight(
    val id: String,
    val quote: String,
    val mine: Boolean,
    ...
    val find: Boolean = false,
    val ignoreCase: Boolean = false,
)
```

Add `spoken: Boolean = false`.

**Transient factory** (`ArticleFind.kt` lines 13–40):
```kotlin
object ArticleFind {
    const val HIGHLIGHT_ID = "find"

    fun painted(query: String): PaintedHighlight? {
        val q = query.trim()
        if (q.isEmpty()) return null
        return PaintedHighlight(
            id = HIGHLIGHT_ID,
            quote = q,
            mine = false,
            find = true,
            ignoreCase = true,
        )
    }
}
```

**Paint find as filled, never underline** (`HighlightMarks.kt` lines 86–87):
```kotlin
// Find matches always use a filled selection-like mark, never underline.
paint({ it.find }, findColor, asUnderline = false, alpha = HighlightMarks.FindMarkAlpha)
```

Add a spoken paint pass with `SpokenMark` + `FindMarkAlpha` (0.38f). Rect inset stays 5dp / 3dp (`paintHighlight` lines 101–102).

**Compose append pattern** (`ReaderScreen.kt` lines 677–683) — find bypasses `visibleFor`; spoken must too:
```kotlin
val painted = HighlightJump.withFocus(
    highlights.visibleFor(settings),
    focusHighlightId,
    focusQuote,
) + listOfNotNull(
    ArticleFind.painted(findQuery),
)
```

Also append `spokenMark(paragraph)` when follow-along is on and session matches this URL. Prefer appending outside `visibleFor` (same as Find). If updating `visibleFor`, spoken must survive `showHighlights == false` (today Find never enters `visibleFor`).

**Auto-scroll:** reuse `HighlightJump.scrollTarget` + `awaitStop` / find jump `LaunchedEffect` (lines 721–743). Set a `followAlongScrolling` flag so the position-saver `snapshotFlow { scrollState.value }` (lines 775–782) and D-15 user-scroll pause do not treat programmatic motion as user scroll.

---

### `ui/reader/ReaderScreen.kt` (component, event-driven) — top bar Listen

**Analog:** itself — `actions` after Save, before MoreVert

**Top bar actions** (lines 419–437):
```kotlin
actions = {
    if (loggedIn && state is ReaderUiState.Ready) {
        SaveLibraryButton(...)
    }
    if (articleUrl != null) {
        ...
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
```

**Insert:** `IconButton` with `Icons.Filled.PlayArrow` / `Pause`, `contentDescription` from UI-SPEC (`Listen to article` / `Pause playback` / `Resume playback`), only when `state is ReaderUiState.Ready`, tint `colorScheme.onSurface`, 48dp. Collect `TtsPlayback` StateFlow; on click call `start` / `pause` / `resume`. Do not hide on TTS failure (D-11).

---

### `ui/reader/VolumeKeys.kt` (hook, event-driven)

**Analog:** itself

**Enabled gate** (`VolumeKeys.kt` lines 34–45 + call site `ReaderScreen.kt` 864):
```kotlin
@Composable
fun Handle(enabled: Boolean = true, onVolume: (up: Boolean) -> Boolean) {
    val latest = rememberUpdatedState(onVolume)
    DisposableEffect(enabled) {
        if (!enabled) {
            listener = null
            return@DisposableEffect onDispose { }
        }
        listener = { up -> latest.value(up) }
        onDispose { listener = null }
    }
}

// Call site today:
VolumeKeys.Handle(enabled = volumeScroll && settings.volumeButtonScroll) { up ->
```

**Change to:** `enabled = volumeScroll && settings.volumeButtonScroll && !speaking` so volume keys reach the system while TTS is active (D-19).

---

### `ui/shell/TtsMiniPlayer.kt` + `ui/BorisApp.kt` (component, event-driven)

**Analogs:** `BorisBottomBar.kt` + `BorisApp.kt` `bottomBar` slot

**Shell bottom bar** (`BorisApp.kt` lines 120–127):
```kotlin
bottomBar = {
    if (selectedTab != null) {
        BorisBottomBar(
            selected = selectedTab,
            pictureUrl = pictureUrl,
            onSelect = ::goToTab,
        )
    }
},
```

**Bottom bar surface** (`BorisBottomBar.kt` lines 28–31):
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
) {
```

**Core pattern:** When `TtsPlayback.session != null` and current route is not that article URL, show slim 56dp row (title + speed chip + prev/play/next) above `BorisBottomBar` inside the same `bottomBar` column. On a different reader (no tab bar), overlay the bottom of the reader. Title tap → `navController.navigate(Routes.reader(url))`. Speed chip writes via `SettingsViewModel.update` / `UserSettings.withDouble("ttsDefaultSpeed", …)` and notifies `TtsPlayback` to re-speak current paragraph. 200ms fade; flat surface + 1dp top outline (UI-SPEC).

---

### `ui/settings/TtsSection.kt` + `SettingsScreen.kt` (component, request-response)

**Analogs:** `ReadingSection.kt`, `SettingsControls.kt`, `MediaSection.kt`, `SettingsCategoryDetail`

**Section composition** (`ReadingSection.kt` lines 40–57, 96–100):
```kotlin
@Composable
fun ReadingSection(
    settings: UserSettings,
    darkTheme: Boolean,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingRow(stringResource(R.string.settings_reading_font)) {
            FontDropdown(
                selected = settings.readingFont,
                onSelect = { onUpdate(settings.withString("readingFont", it)) },
            )
        }
        ...
        SettingCheckbox(
            label = stringResource(R.string.settings_open_links_in_reader),
            checked = settings.openLinksInReader,
            onCheckedChange = { onUpdate(settings.withBoolean("openLinksInReader", it)) },
        )
    }
}
```

**Dropdown** — copy `FontDropdown` (`ReadingSection.kt` lines 106–148): `ExposedDropdownMenuBox` + read-only `OutlinedTextField`, `RoundedCornerShape(8.dp)`.

**Chip / toggle** — `IconToggle` / `SettingChipSize` (`SettingsControls.kt` lines 32–33, 89–119) for speed cycle (`Icons.Filled.Speed` 18dp + `{rate}x`).

**Category wiring** (`SettingsScreen.kt` lines 300–303):
```kotlin
SettingsCategory.Reading -> {
    ReadingSection(settings = settings, darkTheme = darkTheme, onUpdate = onUpdate)
    ReadingPreview(settings = settings, darkTheme = darkTheme)
}
```

**Change to:** `ReadingSection` → **`TtsSection`** → `ReadingPreview` (24dp spacing already from parent `spacedBy(24.dp)`).

**Preview box:** bordered column like `ReadingPreview` (lines 57–61) but 12dp padding / `surfaceVariant` per UI-SPEC; play calls `TtsPlayback.preview(EXAMPLE_TEXT)` (one-shot, no mini player).

**D-11 deep link:** start `Intent(Settings.ACTION_TEXT_TO_SPEECH_SETTINGS)` similar to `openExternalUri` / `FLAG_ACTIVITY_NEW_TASK` patterns in ViewModels; keep all controls visible.

---

### Tests (`tts/*Test.kt`, `UserSettingsTest.kt`)

**Analogs:** `MarkdownInlineTest.kt`, `VolumeKeysTest.kt`, `UserSettingsTest.kt`, `HighlightVisibilityTest.kt`

**JVM JUnit 4 style** (`MarkdownInlineTest.kt` lines 1–14):
```kotlin
package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownInlineTest {
    @Test
    fun flattensMarkdownLinksToLabels() {
```

**Defaults test extension** (`UserSettingsTest.kt` lines 9–12):
```kotlin
@Test
fun defaultsMatchWebappReadingDisplay() {
    val settings = UserSettings.defaults()
    assertEquals("source-serif-4", settings.readingFont)
```

Add assertions for `ttsDefaultSpeed == 2.1`, `ttsLanguageMode == "content"`, `ttsFollowAlong == true`, etc.

**Coverage targets:**
- `TtsTextTest` — fences/images/tables dropped; title+summary+body; startIndex noise floor
- `TtsSpeedTest` — cycle presets; snap `"1"` / unknown rates
- `TtsLanguageTest` — resolve order; script heuristic on JVM (no Android TextClassifier)

---

### `AndroidManifest.xml` (config)

**Analog:** itself

Add permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`; TTS `<queries>` intent; `<service android:name=".tts.TtsPlaybackService" android:exported="false" android:foregroundServiceType="mediaPlayback" />`. Do not add a second Activity.

## Shared Patterns

### Process-wide StateFlow singleton
**Source:** `data/SettingsSync.kt` lines 8–14, 21–24  
**Apply to:** `TtsPlayback`, service, ReaderScreen, mini player, settings preview  
```kotlin
object SettingsSync {
    private val _settings = MutableStateFlow(UserSettings.defaults())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()
    fun apply(next: UserSettings) {
        dirty = true
        _settings.value = next
    }
}
```

### Settings write + NIP-78 publish
**Source:** `SettingsViewModel.kt` lines 69–76 + `UserSettings.with*`  
**Apply to:** speed cycle (settings + mini player), language dropdown, follow-along checkbox  
Language dropdown must write three keys (webapp contract):
```kotlin
onUpdate(
    settings
        .withString("ttsLanguageMode", value)
        .withBoolean("ttsUseSystemLanguage", value == "system")
        .withBoolean("ttsDetectContentLanguage", value == "content")
)
```

### Transient reader mark (not NIP-84)
**Source:** `ArticleFind` + `ReaderScreen` painted append + `HighlightMarks` find paint  
**Apply to:** spoken paragraph follow-along  
Distinct id `"spoken"`, teal fill, never underline, never publish.

### Auto-scroll jump
**Source:** `HighlightJump.scrollTarget` / find `LaunchedEffect` in `ReaderScreen`  
**Apply to:** follow-along when enabled and not user-paused  

### Volume keys disable gate
**Source:** `VolumeKeys.Handle(enabled = …)`  
**Apply to:** `!speaking` while session playing on this reader  

### No DI / no Media3 / no second Activity
**Source:** project conventions + RESEARCH anti-patterns  
**Apply to:** all new TTS files — `object` + constructor params / Application context only  

### No Log / no body in notifications
**Source:** codebase convention + security research  
**Apply to:** service metadata = title + author only  

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `tts/TtsPlaybackService.kt` | service | event-driven | Repo has zero `Service` / FGS / `MediaSession` implementations. Use RESEARCH platform snippets + Manifest `exported="false"` component style; own speech engine inside the service, mutate `TtsPlayback`. |

## Metadata

**Analog search scope:** `app/src/main/java/org/dergigi/boris/{data,ui,tts,nostr}`, `AndroidManifest.xml`, `app/src/test/java/org/dergigi/boris`  
**Files scanned:** ~160 Kotlin sources under `org.dergigi.boris` + 76 JVM tests  
**Pattern extraction date:** 2026-08-18  
**Strong analogs used (stop at 5+):** `SettingsSync`, `ArticleFind`/`HighlightMarks`, `ReadingSection`/`SettingsControls`, `ReaderScreen` top bar + volume keys, `MarkdownInline`/`Footnotes`, `UserSettings`, `BorisApp`/`BorisBottomBar`
