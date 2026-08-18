# Phase 4: Listen to articles - Research

**Researched:** 2026-08-18
**Domain:** Android on-device TTS, MediaSession foreground playback, reader follow-along
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

### Playback
- **D-01:** Background playback. Speech continues with the screen off, with a media notification and lock-screen / headset controls. — **Reversibility:** costly — needs a foreground service, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and a `MediaSession`.
- **D-02:** Podcast-like: keep speaking when the user leaves the reader for Home or another screen, until they pause or the article ends.
- **D-03:** One article speaking at a time. Browsing other articles does not interrupt. Pressing play on a different article stops the current one and starts the new one.
- **D-04:** Media notification / lock screen: play, pause, stop, skip to next paragraph, skip to previous paragraph.

### Voice engine
- **D-05:** On-device `android.speech.tts.TextToSpeech` only. No cloud TTS, no API keys, no network required for speech.
- **D-06:** Match the webapp Text-to-Speech settings: default playback speed, speaker language, and the preview paragraph with play.
- **D-07:** Sync speed and language through the existing NIP-78 settings JSON. Keys already in Android defaults: `ttsDefaultSpeed`, `ttsLanguageMode`, `ttsUseSystemLanguage`, `ttsDetectContentLanguage`.
- **D-08:** Speed presets are the webapp list: `0.8, 1, 1.2, 1.4, 1.6, 1.8, 2, 2.1, 2.4, 2.8, 3`. Default `2.1`. Cycle button writes `ttsDefaultSpeed`.
- **D-09:** Speaker language matches the webapp dropdown: System Language, Content (auto-detect), then `en-US`, `en-GB`, `zh`, `es`, `hi`, `ar`, `fr`, `pt`, `de`, `ja`, `ru`. Stored as `ttsLanguageMode`.
- **D-10:** Preview sentence is exactly: `Boris aims to be a calm reader app with clean typography, beautiful design, and a focus on readability. Boris does not and will never have ads, trackers, paywalls, subscriptions, or any other distractions.`
- **D-11:** If TTS is missing or the language cannot be spoken, show a short message and a link to system TTS settings. Reading still works. Do not hide the listen control.

### Follow-along
- **D-12:** While speaking, highlight the current paragraph and auto-scroll it on screen (reuse reader jump / find-mark machinery).
- **D-13:** Follow-along is on by default and can be turned off in settings.
- **D-14:** Play starts at the paragraph nearest the saved reading position (`ReadingPositionStore`). From the top if there is none.
- **D-15:** If the user scrolls the article while it is speaking, stop auto-scrolling until they tap play again or skip. Keep speaking.

### Listen control
- **D-16:** Play / pause lives in the reader top bar, not only in the overflow menu.
- **D-17:** When listening and the user is not on that article, show a slim in-app mini player (title + play/pause/skip). Tapping the title opens that article.
- **D-18:** Speed cycle is on the in-app player as well as in Settings (same presets, same setting key).
- **D-19:** While speaking, volume keys change volume. They do not scroll the article and they do not skip paragraphs.

### Claude's Discretion
- How the spoken-paragraph mark looks versus NIP-84 highlights and find marks (must stay distinct).
- Mini-player placement (bottom bar vs other chrome) as long as it is slim and always reachable while speaking.
- Whether TTS settings is a new Settings category or a section under Reading / Media. Shape must match the webapp section.
- Follow-along setting key name; persist in `UserSettings` / NIP-78 if a new key is added.
- Mapping webapp 3x speed onto Android `setSpeechRate` (engines often clamp).
- How to chunk the article into paragraphs for skip and follow-along.
- How to strip markdown / images / code so spoken text is clean.
- Language auto-detect implementation (webapp uses `tinyld`).
- Standard Android audio focus: pause for calls / other apps, resume when appropriate.
- Foreground service + `MediaSession` wiring. No second Activity.
- Exact top-bar play icon and notification metadata (title, author if known).

### Deferred Ideas (OUT OF SCOPE)
- Cloud / neural TTS voices
- Playlist / queue of articles
- Sleep timer
- Download spoken audio files
- Syncing playback position itself over Nostr (reading-position sync already exists as scroll; spoken-offset sync is out of this phase)

None of these were requested as this phase.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| TTS-01 | User can listen to the current article with on-device TTS (background playback, webapp-matched speed/language settings, follow-along in the reader). Login is not required. Reading still works if TTS is missing. | Platform `TextToSpeech` in a `mediaPlayback` FGS + `MediaSession`; settings keys already in `UserSettings` `DEFAULT_JSON`; play control stays visible on D-11 failure; no session gate on `TtsPlayback`. |
| READ-01 | User can paste, share, or open a URL and read the article while logged out. Login UI sits on Home; it does not replace or block reading. | Do not move reader behind login. Keep listen optional. TTS missing must not change `ReaderUiState` load/error/ready. Volume-key scroll stays for reading when not speaking. |
</phase_requirements>

## Summary

Phase 4 adds listen-to-this-article on the existing Compose reader. Speech must outlive `ReaderViewModel` and the reader destination, so the engine and `MediaSession` belong in a process-wide foreground service, not in the reader ViewModel or a Composable. Settings already travel in the NIP-78 blob (`com.dergigi.boris.user-settings`); Android has the four TTS keys in defaults and no typed accessors yet. Follow-along is a transient paint like Find, not a NIP-84 highlight.

Android TTS has no pause API. Pause is stop-the-current-utterance and keep the session; resume speaks the current paragraph again (optionally the remainder via `onRangeStart`). `setSpeechRate` uses the same 1.0 = normal scale as the web Speech API, so pass `ttsDefaultSpeed` through (including 2.1 and 3). Do not add Media3, ML Kit, tinyld, Hilt, Room, DataStore, or a second Activity.

**Primary recommendation:** Own playback in `org.dergigi.boris.tts.TtsPlaybackService` (FGS `mediaPlayback` + platform `MediaSession` + `TextToSpeech`), expose `TtsPlayback` as a `StateFlow` singleton, and keep spoken-text / speed / language as JVM-tested helpers.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Speak article text | Device / app process (FGS) | System TTS engine | Engine is a bound system service; Boris only queues utterances. |
| Background / lock-screen controls | Device / app process (FGS + MediaSession) | System UI | `mediaPlayback` FGS is what keeps speech alive with the screen off. |
| Play / pause in reader chrome | Client (Compose reader) | `TtsPlayback` StateFlow | Top bar is UI; engine state must not live only in composition. |
| Mini player while browsing | Client (shell `BorisApp`) | `TtsPlayback` | Session outlives the reader route. |
| Speed / language settings | Client (Settings) | NIP-78 relays | Same JSON keys as the webapp; `SettingsViewModel` already publishes. |
| Follow-along highlight + auto-scroll | Client (reader paint/scroll) | `HighlightJump` / `HighlightMarks` | Visual only; do not publish a highlight event. |
| Start paragraph from reading position | Device storage (`ReadingPositionStore`) | Client | Store holds scroll fraction 0..1, not a paragraph index. |
| Language auto-detect | Device (TextClassifier API 29+) | Pure Kotlin script heuristic | No tinyld, no ML Kit, no Play Services. |
| Audio focus / calls | System AudioManager | FGS | Target 35 may only request focus from a FGS or the top app. |

## Project Constraints (from .cursor/rules/)

`.cursor/rules/` contains `release-zapstore.mdc` only:

- A Boris Android release must bump `versionCode` / `versionName`, update `CHANGELOG.md`, commit `chore: release X.Y.Z`, tag, push, create the GitHub release, assemble the signed APK, upload it, and publish to Zapstore in the same turn.
- This phase is implementation, not a release cut. Do not bump version or publish as part of listen-to-articles work.

From `.planning/codebase/` (treat as project law for this phase):

- Kotlin + Compose, `minSdk` 26, `targetSdk` 35, JUnit 4 JVM tests only.
- No Hilt/Koin, no Room, no DataStore, no new Activity, no Media3, no Play Services.
- New code under `org.dergigi.boris` only. Do not extend `com.readwithboris`.
- No `Log` / Timber. Do not log article body. Never request or persist `nsec`.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `android.speech.tts.TextToSpeech` | Platform API 26+ (class since API 4) | On-device synthesis | Locked D-05. No extra artifact. |
| `android.media.session.MediaSession` | Platform API 21+ | Lock screen / headset / notification transport | Locked D-01. Avoid Media3. |
| `android.app.Notification.MediaStyle` | Platform API 21+ | Bind notification to the session token | Already on minSdk 26; no `androidx.media` needed. |
| `androidx.core:core-ktx` | 1.16.0 (already in catalog) | `ServiceCompat.startForeground` with a type | Required for typed FGS on API 34+ without a new dependency. |
| Jetpack Compose + Material3 | BOM `2025.06.01` (existing) | Top bar, mini player, settings section | Match existing screens. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `android.media.AudioFocusRequest` | Platform API 26+ | Pause for calls / other apps | Always, from the FGS. |
| `android.view.textclassifier.TextClassifier.detectLanguage` | Platform API 29+ | Content language | `ttsLanguageMode == "content"` on API 29+. |
| Existing `UserSettings` / `SettingsSync` / `Nip78` | in-repo | Speed, language, follow-along | Never invent a second settings store. |
| Existing `MarkdownInline`, `Footnotes`, `HighlightJump`, `ArticleFind`, `VolumeKeys` | in-repo | Spoken text + follow-along + D-19 | Reuse, do not fork. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Platform `MediaSession` + own FGS | Media3 `MediaSessionService` | Google's current media docs auto-publish MediaStyle notifications. Adds a large library STACK.md does not have. Locked decision is platform MediaSession. Do not add Media3. |
| Script heuristic + TextClassifier | ML Kit language ID or npm `tinyld` | Better Latin-language detect. ML Kit pulls Play Services (forbidden). tinyld is JS. |
| `NotificationCompat.MediaStyle` (`androidx.media`) | Platform `Notification.MediaStyle` | Extra AndroidX media artifact for no minSdk benefit. |

**Installation:** none. Do not add Gradle libraries for this phase.

**Version verification:** SDK already `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35` in `app/build.gradle.kts`. androidx-core 1.16.0 already on the classpath. No registry packages to install, so the Package Legitimacy Gate has an empty input set.

## Package Legitimacy Audit

No new npm / PyPI / Maven packages. Platform APIs plus existing `androidx.core`.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| (none) | — | — | — | — | — | — |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
  User tap Play (reader top bar / mini player / settings preview)
           |
           v
  TtsPlayback.start(content, startIndex)     [process singleton, StateFlow]
           |
           v
  Context.startForegroundService(TtsPlaybackService)
           |
           +-- startForeground(type = MEDIA_PLAYBACK)
           +-- MediaSession.setCallback / setPlaybackState / setMetadata
           +-- AudioManager.requestAudioFocus(AudioFocusRequest)   [must be in FGS on target 35]
           +-- TextToSpeech(appContext) OnInitListener
           |
           v
  For each speakable paragraph:
      tts.speak(text, QUEUE_FLUSH, params, utteranceId="p$index")
           |
           +-- UtteranceProgressListener.onStart  -> StateFlow paragraphIndex
           +-- onRangeStart (optional)            -> resume offset inside paragraph
           +-- onDone                             -> speak next or stop session
           +-- onError / LANG_MISSING_DATA        -> UI message + TTS settings intent
           |
           +--> System UI: MediaStyle notification / lock screen / headset
           +--> ReaderScreen: spoken mark + HighlightJump auto-scroll (if follow-along)
           +--> BorisApp: mini player if current route is not this article URL
```

### Recommended Project Structure

```
app/src/main/java/org/dergigi/boris/
├── tts/
│   ├── TtsPlayback.kt           # object: StateFlow, play/pause/resume/stop/skip/cycleSpeed
│   ├── TtsPlaybackService.kt    # FGS + MediaSession + TextToSpeech + audio focus
│   ├── TtsText.kt               # markdown -> speakable paragraphs
│   ├── TtsSpeed.kt              # presets + cycle
│   ├── TtsLanguage.kt           # mode resolve + locale + detect
│   └── TtsPreview.kt            # exact preview sentence constant
├── ui/reader/                   # top-bar play, spoken mark, VolumeKeys off while speaking
├── ui/shell/                    # TtsMiniPlayer in BorisApp
└── ui/settings/                 # TtsSection under Reading
app/src/test/java/org/dergigi/boris/tts/
├── TtsTextTest.kt
├── TtsSpeedTest.kt
└── TtsLanguageTest.kt
```

Do not put the engine in `ReaderViewModel`. That object dies when the reader destination leaves the back stack (D-02).

### Pattern 1: Process-wide playback owner

**What:** `object TtsPlayback` holds `StateFlow<TtsSession?>`. The service mutates it. UI collects it.
**When to use:** Any control that must work off the reader (mini player, notification, settings preview).
**Session fields (planner):** `url`, `title`, `author`, `paragraphs`, `index`, `playing`, `paused`, `followAlongEnabled`, `followAlongPaused`, `rate`.

Settings preview (`TtsSection`) speaks the exact D-10 sentence through the same engine. Preview is a one-shot utterance: do not start an article session, do not show the mini player, do not take over an in-progress article. If an article is speaking, preview stops it (one TTS engine).

### Pattern 2: One utterance per paragraph

**What:** Skip next/prev and follow-along both need a stable paragraph index. Speak `paragraphs[i]` with `utteranceId = "p$i"`. On `onDone`, increment and speak the next. Skip is `index++` / `index--` plus `QUEUE_FLUSH`.
**When to use:** Always. Do not dump the whole article into one `speak()` (engine length cap + no paragraph skip).

### Pattern 3: Follow-along as a transient spoken mark

**What:** Add `spoken: Boolean = false` on `PaintedHighlight` (same idea as `find: Boolean = false`). Id `"spoken"`. Quote is the current paragraph's display text. Paint with a new `SpokenMark` color, filled like Find, never underline. `visibleFor` must keep spoken marks even when `showHighlights` is false.
**When to use:** Reader of the speaking article, follow-along on, and follow-along not paused by manual scroll.
**Auto-scroll:** `HighlightJump.awaitStop` + `scrollTarget` already used for highlight focus. Set a `followAlongScrolling` flag around `animateScrollTo` so the existing `snapshotFlow { scrollState.value }` saver does not treat that motion as D-15.

### Pattern 4: Settings write path

**What:** Typed accessors on `UserSettings`. Mutations go through `SettingsViewModel.update`. Language dropdown writes three keys like the webapp:

```ts
// Source: /Users/gigi/Development/vibe/boris/src/components/Settings/TTSSettings.tsx
ttsLanguageMode: value,
ttsUseSystemLanguage: value === 'system',
ttsDetectContentLanguage: value === 'content'
```

Speed cycle writes `ttsDefaultSpeed` only. Live speaking: call `tts.setSpeechRate` then `QUEUE_FLUSH` the current paragraph so the new rate applies (platform `setSpeechRate` does not change the in-flight utterance).

### Anti-Patterns to Avoid

- **TTS in `ReaderViewModel` or a Composable:** Leaves the reader and speech dies (violates D-02).
- **Second Activity for playback:** Share/VIEW assume one `singleTop` `MainActivity`.
- **Media3 / ExoPlayer for speech:** Wrong pipeline; locked to `TextToSpeech`.
- **Speaking raw markdown:** Images, URLs, fences, and tables get read aloud.
- **Using FindMark color for spoken paragraphs:** Collides with in-article find (`FindMark = Color(0xFF93C5FD)` in `ui/theme/Color.kt`).
- **Painting a NIP-84 highlight for follow-along:** Would publish/sign and mix with `HIGH-01`.
- **Requesting audio focus from a stopped Activity on target 35:** Returns `AUDIOFOCUS_REQUEST_FAILED`. Request from the FGS after `startForeground`.
- **Logging `speak()` text:** Article content in logcat. No `Log` in this codebase.
- **Hiding the listen control when TTS is missing:** Violates D-11 and TTS-01.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| On-device speech | Cloud HTTP TTS, custom vocoder | `android.speech.tts.TextToSpeech` | Locked; offline; no keys. |
| Background survival | `startService` without FGS, WorkManager, wakelock-only | `mediaPlayback` foreground service | Android 8+ kills background; 14+ requires the type. |
| Lock-screen buttons | Custom broadcast notification actions only | `MediaSession.Callback` + `PlaybackState` actions | Android 13+ compact controls come from `PlaybackState`, not `Notification.Action`. |
| Pause/resume speech | Fake pause via volume=0 loops | Stop utterance, keep index, speak again | Platform TTS has `stop()`, not `pause()`. |
| Markdown flatten | New markdown parser | `Footnotes.expand` then block split then `MarkdownInline.plain` | Already tested. |
| Settings sync | New DataStore / prefs file for speed | `UserSettings` + `SettingsViewModel.update` | NIP-78 identifier is already `com.dergigi.boris.user-settings`. |
| Language ID library | ML Kit, tinyld JNI | `TextClassifier.detectLanguage` (API 29+) + script heuristic | No new deps; JVM-test the heuristic. |
| Audio mixing with calls | Ignore focus | `AudioFocusRequest` `AUDIOFOCUS_GAIN` + `CONTENT_TYPE_SPEECH` | Official media guidance; pause not duck for speech. |

**Key insight:** The hard parts are process lifetime and utterance boundaries, not synthesis quality. A custom audio pipeline would reimplement what the system TTS engine already does, and would still need the same FGS + session.

## Common Pitfalls

### Pitfall 1: Android TTS has no pause

**What goes wrong:** Calling a non-existent `pause()` or expecting `stop()` to resume.
**Why it happens:** Web Speech API has `pause()`/`resume()`. Android `TextToSpeech` does not.
**How to avoid:** `paused` is session state. Pause = `tts.stop()` + keep `index` (and optional `onRangeStart` char offset). Resume = `speak` current (or remaining) paragraph with `QUEUE_FLUSH`.
**Warning signs:** Speech restarts the whole article after pause.

### Pitfall 2: Skip buttons do nothing on the lock screen

**What goes wrong:** Notification shows next/prev but taps are ignored.
**Why it happens:** `PlaybackState` `setActions` omitted `ACTION_SKIP_TO_NEXT` / `ACTION_SKIP_TO_PREVIOUS`. Android 13+ derives compact slots from `PlaybackState`, not from notification actions.
**How to avoid:** Always set `ACTION_PLAY | ACTION_PAUSE | ACTION_STOP | ACTION_PLAY_PAUSE | ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS`. Map skip to paragraph index, not "next track".
**Warning signs:** Play/pause works, skip does not.

### Pitfall 3: `startForeground` without type on API 34+

**What goes wrong:** `MissingForegroundServiceTypeException` or `SecurityException` for `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
**Why it happens:** targetSdk 35. Types are required. `mediaPlayback` has no extra runtime permission beyond the install-time FGS permission.
**How to avoid:** Manifest `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `android:foregroundServiceType="mediaPlayback"`, `exported="false"`. Call `ServiceCompat.startForeground(this, id, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)` within the `startForegroundService` timeout.
**Warning signs:** Crash only on Android 14/15 devices.

### Pitfall 4: Utterance longer than the engine cap

**What goes wrong:** `speak` returns `ERROR` or `onError` on a long paragraph.
**Why it happens:** Input must be no longer than `TextToSpeech.getMaxSpeechInputLength()`.
**How to avoid:** After building a paragraph, if `text.length` exceeds the cap, split on sentence boundaries (`.?!`) then on spaces. Keep one logical paragraph for follow-along (highlight the whole block; speak sub-chunks sequentially with ids `p{i}.{j}`).
**Warning signs:** Playback stops mid-article on legal pages.

### Pitfall 5: Package visibility hides the TTS engine

**What goes wrong:** Init fails or `setLanguage` always `LANG_MISSING_DATA` on Android 11+.
**Why it happens:** Target 30+ package visibility. Official `TextToSpeech.Engine` docs require a `<queries>` intent for `android.intent.action.TTS_SERVICE`.
**How to avoid:** Add that query next to the existing `nostrsigner` query. Open `Settings.ACTION_TEXT_TO_SPEECH_SETTINGS` (`com.android.settings.TTS_SETTINGS`) from the D-11 message. Optional extra: `ACTION_INSTALL_TTS_DATA` when result is `LANG_MISSING_DATA`.
**Warning signs:** Listen works on emulator with Pico, fails on a locked-down OEM.

### Pitfall 6: Volume keys still page the article

**What goes wrong:** While speaking, volume down jumps the reader.
**Why it happens:** `VolumeKeys.Handle` is registered from `ReaderScreen` and `MainActivity.dispatchKeyEvent` consumes volume keys first.
**How to avoid:** `VolumeKeys.Handle(enabled = volumeScroll && settings.volumeButtonScroll && !speaking)`. When disabled, keys reach the system stream.
**Warning signs:** D-19 fails only on the reader, works on Home.

### Pitfall 7: Follow-along fights the user

**What goes wrong:** User scrolls to reread; the next utterance yanks them back.
**Why it happens:** D-15. Auto-scroll must pause on user-initiated scroll, not on our `animateScrollTo`.
**How to avoid:** Ignore scroll deltas while `followAlongScrolling == true`. Any other `scrollState` change sets `followAlongPaused = true` until play or skip.
**Warning signs:** Cannot check a footnote while listening.

### Pitfall 8: Speed cycle loses the preset

**What goes wrong:** `indexOf` misses and jumps to 0.8.
**Why it happens:** Webapp uses exact `SPEED_OPTIONS.indexOf(currentSpeed)`. `UserSettings.withDouble(1.0)` already writes `"1"` (integer-looking). Keep the preset list as doubles and snap unknown values to the nearest preset, then cycle.
**Warning signs:** First tap after sync from webapp skips 2.1.

### Pitfall 9: Logged-out settings do not persist

**What goes wrong:** User changes speed logged out, kills the process, it is 2.1 again.
**Why it happens:** `SettingsViewModel.requestSave` returns if `SessionStore.load` is null. `SettingsSync` is in-memory. Same as today's font size. Do not add a new store just for TTS.
**How to avoid:** Accept process-lifetime settings while logged out. Defaults (`2.1`, `content`, follow-along on) still make TTS-01 true.

## Code Examples

Verified patterns from official sources and this repo.

### Speak one paragraph

```kotlin
// Source: https://developer.android.com/reference/android/speech/tts/TextToSpeech
// speak(CharSequence, int, Bundle, String) added in API 21.
// text must be no longer than TextToSpeech.getMaxSpeechInputLength()
tts.setSpeechRate(rate) // 1.0 = normal; 2.0 = twice; pass ttsDefaultSpeed (e.g. 2.1f)
tts.setAudioAttributes(
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build(),
)
val params = Bundle()
tts.speak(paragraph, TextToSpeech.QUEUE_FLUSH, params, "p$index")
```

### Init and language

```kotlin
// Source: https://developer.android.com/reference/android/speech/tts/TextToSpeech
tts = TextToSpeech(applicationContext) { status ->
    if (status != TextToSpeech.SUCCESS) {
        // D-11: keep listen visible; show message + Settings.ACTION_TEXT_TO_SPEECH_SETTINGS
        return@TextToSpeech
    }
    val result = tts.setLanguage(locale)
    if (result == TextToSpeech.LANG_MISSING_DATA ||
        result == TextToSpeech.LANG_NOT_SUPPORTED
    ) {
        // same D-11 path; optional ACTION_INSTALL_TTS_DATA
    }
}
tts.setOnUtteranceProgressListener(listener) // not OnUtteranceCompletedListener (deprecated API 18)
```

### Foreground service (target 34+)

```kotlin
// Source: https://developer.android.com/about/versions/14/changes/fgs-types-required
// mediaPlayback runtime prerequisites: None
ServiceCompat.startForeground(
    this,
    NOTIFICATION_ID,
    notification,
    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
)
```

Manifest additions (in addition to existing `INTERNET`):

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
<service
    android:name=".tts.TtsPlaybackService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

Media-session notifications are exempt from the Android 13 `POST_NOTIFICATIONS` behavior change. Still declare the permission. Do not block reading or speaking if the user denies it; in-app controls remain.

### MediaSession transport

```kotlin
// Source: https://developer.android.com/media/implement/surfaces/mobile
// Android 13+ compact: slot1 play/pause, slot2 previous, slot3 next
session.setCallback(object : MediaSession.Callback() {
    override fun onPlay() { TtsPlayback.resume() }
    override fun onPause() { TtsPlayback.pause() }
    override fun onStop() { TtsPlayback.stop() }
    override fun onSkipToNext() { TtsPlayback.skip(1) }
    override fun onSkipToPrevious() { TtsPlayback.skip(-1) }
})
val state = PlaybackState.Builder()
    .setActions(
        PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS,
    )
    .setState(
        if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
        PlaybackState.PLAYBACK_POSITION_UNKNOWN,
        rate,
    )
    .build()
session.setPlaybackState(state)
session.setMetadata(
    MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, title ?: url)
        .putString(MediaMetadata.METADATA_KEY_ARTIST, author.orEmpty())
        .build(),
)
```

Use `Notification.MediaStyle().setMediaSession(session.sessionToken)`. Compact lock-screen will show play/pause + paragraph skip. Stop is `ACTION_STOP` (expanded / headset). That satisfies D-04 without a fifth compact slot.

### Audio focus (request from the FGS)

```kotlin
// Source: https://developer.android.com/media/optimize/audio-focus
// Target 35: request only as top app or from a foreground service.
val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build(),
    )
    .setOnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> TtsPlayback.stop() // no auto-resume
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> TtsPlayback.pause() // speech: pause, do not duck
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocus) TtsPlayback.resume()
        }
    }
    .build()
```

Official speech note: describe the stream as `CONTENT_TYPE_SPEECH`. Pause on transient loss rather than ducking.

### Language resolution (match webapp, do not invent)

```kotlin
// Source: /Users/gigi/Development/vibe/boris/src/components/TTSControls.tsx
// Priority: specific language > content detection > system language
fun resolveLanguage(
    mode: String,           // ttsLanguageMode
    text: String,
    systemLang: String,     // Locale.getDefault().language
    detect: (String) -> String?,
): String? {
    if (mode != "system" && mode != "content") return mode
    if (mode == "content") detect(text)?.let { return it }
    if (mode == "system") return systemLang
    return detect(text) ?: systemLang
}
```

Fallback when `ttsLanguageMode` is missing: treat `ttsUseSystemLanguage == true` as `"system"`, else `ttsDetectContentLanguage != false` as `"content"` (webapp defaults).

Content detect:

- API 29+: `TextClassificationManager.textClassifier.detectLanguage` on a worker thread. Take `getLocale(0).language` if confidence is usable.
- API 26–28, or classifier failure: Unicode script heuristic (Han -> `zh`, Hiragana/Katakana -> `ja`, Arabic -> `ar`, Devanagari -> `hi`, Cyrillic -> `ru`) plus compact stopword scores for `en`/`es`/`fr`/`pt`/`de`. JVM-test the heuristic. Do not add ML Kit.

`setLanguage` locales: `Locale.forLanguageTag("en-US")`, `en-GB`, and `Locale(language)` for `zh`,`es`,`hi`,`ar`,`fr`,`pt`,`de`,`ja`,`ru`.

### Speed presets

```kotlin
// Source: /Users/gigi/Development/vibe/boris/src/components/Settings/TTSSettings.tsx
val SPEED_OPTIONS = doubleArrayOf(0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.1, 2.4, 2.8, 3.0)
const val DEFAULT_SPEED = 2.1
```

**Mapping 3x onto `setSpeechRate`:** pass the webapp value through. Official docs: `1.0` is normal, `2.0` is twice. Same units as Web Speech `utterance.rate`. Do not scale 3.0 down to 1.5. If `setSpeechRate` returns `ERROR` or speech is unchanged, keep the stored setting (so NIP-78 stays aligned) and still speak; do not invent a second Android-only speed key.

### Spoken text cleanup

```kotlin
// Build speakable blocks from Footnotes.expand(content.body), then MarkdownInline.plain.
// Include title then summary then body blocks (webapp ContentPanel joins title, summary, body).
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

Block split rules (prescriptive):

1. Run `Footnotes.expand` first (reader already does this before markdown render).
2. Drop fenced code (` ``` ` / `~~~`), image-only lines (`![...](...)`), and table rows (`| ... |`).
3. Treat ATX headings, list items, and blank-line-separated paragraphs as separate blocks (this is the skip unit).
4. Flatten remaining inline markdown with `MarkdownInline.plain` (strips `[text](url)` to `text`, images to alt).
5. Do not speak raw URLs that were the only content of a link with empty label.

Start index (D-14):

```kotlin
fun startIndex(fraction: Float, count: Int): Int {
    if (count <= 0) return 0
    if (fraction < 0.01f) return 0 // same noise floor as ReadingProgress.MIN_RESTORE_FRACTION
    return (fraction * count).toInt().coerceIn(0, count - 1)
}
```

Use `ReadingPositionStore.fraction(url)` at play time. If the reader is open, using the live `scrollState` fraction is equivalent after restore.

### Follow-along paint

```kotlin
// Reuse ArticleFind's transient pattern. Distinct id and color.
// ArticleFind.HIGHLIGHT_ID = "find"
const val SPOKEN_ID = "spoken"

fun spokenMark(paragraph: String): PaintedHighlight =
    PaintedHighlight(id = SPOKEN_ID, quote = paragraph, mine = false, spoken = true)
```

Recommend `SpokenMark = Color(0xFF2DD4BF)` (teal) at about Find's alpha (`FindMarkAlpha = 0.38f`). Must stay distinct from `HighlightMine` `#FDE047`, `HighlightFriends` `#F97316`, `HighlightOther` `#9333EA`, and `FindMark` `#93C5FD`.

### Top bar and mini player

- Top bar: `Icons.Filled.PlayArrow` / `Icons.Filled.Pause` `IconButton` in `actions` before overflow. `contentDescription` "Listen" / "Pause". Visible in `ReaderUiState.Ready` even logged out. Do not hide when TTS init failed (D-11).
- Mini player: slim bar above `BorisBottomBar` when a tab is showing, and overlay the bottom of the reader when the current `ReaderViewModel.url` is not `TtsPlayback.url`. Title tap -> `Routes.reader(url)`. Include play/pause, skip prev/next, speed cycle (`{rate}x`).
- Settings: add `TtsSection` inside `SettingsCategory.Reading` (webapp keeps Text-to-Speech as a section, not a top-level category). Controls: speed cycle, language dropdown (values below), preview paragraph + play, follow-along checkbox.

Dropdown values (verbatim from `TTSSettings.tsx`): `system`, `content`, `en-US`, `en-GB`, `zh`, `es`, `hi`, `ar`, `fr`, `pt`, `de`, `ja`, `ru`.

Preview sentence (verbatim from `TTSSettings.tsx` `EXAMPLE_TEXT`):

`Boris aims to be a calm reader app with clean typography, beautiful design, and a focus on readability. Boris does not and will never have ads, trackers, paywalls, subscriptions, or any other distractions.`

### Follow-along setting key

Add `"ttsFollowAlong":true` to `DEFAULT_JSON` and `val ttsFollowAlong: Boolean get() = bool("ttsFollowAlong", true)`. Webapp `saveSettings` stringifies the loaded object after `{ ...loadedSettings }`, so an extra key round-trips today. If a later webapp save starts picking known keys only, the key could be dropped; that is acceptable (default stays on). Do not invent `ttsUseSystemLanguage`-style duplicates.

### Typed accessors to add on `UserSettings`

Existing `DEFAULT_JSON` already contains:

```
"ttsUseSystemLanguage":false,
"ttsDetectContentLanguage":true,
"ttsLanguageMode":"content",
"ttsDefaultSpeed":2.1,
```

Add:

- `ttsDefaultSpeed: Double` default `2.1`
- `ttsLanguageMode: String` default `"content"`
- `ttsUseSystemLanguage: Boolean` default `false`
- `ttsDetectContentLanguage: Boolean` default `true`
- `ttsFollowAlong: Boolean` default `true`

Extend `UserSettingsTest.defaultsMatchWebappReadingDisplay` with the TTS defaults.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Untyped FGS | Required `foregroundServiceType` | Android 14 (API 34) | Must declare `mediaPlayback` and pass the type to `startForeground`. |
| Notification actions drive media buttons | `PlaybackState` drives compact controls | Android 13 (API 33) | Skip/play must be session actions. |
| Optional audio focus | System enforces focus; target 35 needs FGS or top app | Android 12 / 15 | Request focus from the service. |
| Google media samples on `androidx.media` | Media3 `MediaSessionService` | Media3 era | Do not migrate this phase; platform session is enough for TTS. |

**Deprecated/outdated:**

- `TextToSpeech.OnUtteranceCompletedListener`: deprecated API 18. Use `UtteranceProgressListener`.
- `speak(String, int, HashMap)`: older overload. Use `speak(CharSequence, int, Bundle, String)` (API 21, below minSdk risk).
- `KEY_FEATURE_NETWORK_SYNTHESIS`: do not enable. D-05 is on-device only. Prefer a voice with `Voice.isNetworkConnectionRequired == false` when `getVoices()` is available.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | AOSP `getMaxSpeechInputLength()` is commonly 4000 characters; always call the method rather than hard-coding. | Pitfall 4 | Hard-coding 4000 could be short or long on some engines. |
| A2 | Some OEM engines clamp `setSpeechRate` near 2.0 even though 3.0 is in the webapp list. | Speed mapping | User hears ~2x at the 3x label; still pass 3.0 through so settings match the webapp. |
| A3 | Webapp will keep unknown JSON keys (including `ttsFollowAlong`) when it next saves. | Follow-along key | If the webapp later picks known keys, follow-along resets to default true after a web save. |
| A4 | `Settings.ACTION_TEXT_TO_SPEECH_SETTINGS` is the right D-11 deep link on current OEMs. | D-11 | Some skins ignore it; fall back to `ACTION_INSTALL_TTS_DATA` then generic Settings. |

## Open Questions (RESOLVED)

1. **Resume mid-paragraph vs restart the paragraph** — RESOLVED
   - What we know: `UtteranceProgressListener.onRangeStart(utteranceId, start, end, frame)` exists. Pause has no platform API.
   - What's unclear: engine support for `onRangeStart` is uneven.
   - Recommendation: v1 resume restarts the current paragraph. Store range offsets if the callback fires, use them if present. Do not block the phase on perfect resume.

2. **API 26–28 language detect quality** — RESOLVED
   - What we know: `detectLanguage` is API 29. Webapp uses `tinyld`.
   - What's unclear: stopword lists will mis-label short English/Spanish mix.
   - Recommendation: heuristic is good enough; if detect returns null, fall through to system language (same as webapp).

3. **Notification permission prompt** — RESOLVED
   - What we know: media-session notifications are exempt on Android 13; FGS still needs a notification object.
   - What's unclear: some OEMs still hide the shade entry without `POST_NOTIFICATIONS`.
   - Recommendation: declare the permission; request it once on first Play if `Build.VERSION.SDK_INT >= 33`. Never gate reading or in-app play on the result.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | compile | ✓ | OpenJDK 17.0.20 | — |
| Gradle wrapper | `./gradlew :app:test` | ✓ | 8.13 | — |
| Android SDK 35 | compileSdk / targetSdk | ✓ | `sdk.dir=/Users/gigi/Android/Sdk` | — |
| `adb` on PATH | device UAT | ✗ | — | Android Studio / full SDK platform-tools; not needed for JVM tests |
| System TTS engine | speech | device-only | — | D-11 message; reading still works |
| Knowledge graph `.planning/graphs/graph.json` | cross-doc query | ✗ | — | Research used source reads instead |

**Missing dependencies with no fallback:** none for planning/JVM tests.

**Missing dependencies with fallback:** `adb` not on PATH (device UAT later); no `graph.json`.

Step 2.6: external tools identified (JDK, Gradle, SDK, system TTS). Graph skipped (absent).

## Security Domain

`workflow.security_enforcement` is enabled (ASVS level 1).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Listen works logged out. Do not call `SessionStore` for play. |
| V3 Session Management | no | No new auth cookies/tokens. |
| V4 Access Control | no | No privileged article content beyond what the reader already fetched. |
| V5 Input Validation | yes | Treat `ReadableContent.body` as untrusted. Strip markdown/HTML before `speak`. Notification metadata = title/author only, never body. |
| V6 Cryptography | no | Do not add crypto. Never touch `nsec` / `SecretBox`. |

### Known Threat Patterns for Android TTS / FGS

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Exported FGS / media intent hijack | Elevation | `android:exported="false"`; no intent-filter; no MediaBrowser. |
| Article body in logcat or crash reports | Information disclosure | No `Log`. Do not put paragraph text in `Toast` or notification body. |
| Network TTS / `ERROR_NETWORK` path | Information disclosure | Do not set network-required voices. On `ERROR_NETWORK`, skip to D-11, do not retry on a cloud engine. |
| Notification leaking full article on lock screen | Information disclosure | `METADATA_KEY_TITLE` + author only. |
| Overlay of existing session (Amber/bunker) | Tampering | TTS code must not read `SessionStore` except that settings save already does when logged in. |
| Backup of unrelated secrets | Information disclosure | Do not add TTS state to SharedPreferences that currently exclude `boris_session.xml`. Session stays in the speaking process; stop on `onDestroy`. |

## Sources

### Primary (HIGH confidence)

- `https://developer.android.com/reference/android/speech/tts/TextToSpeech` — `setSpeechRate`, `speak`, `getMaxSpeechInputLength`, `LANG_*`, `QUEUE_FLUSH`
- `https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener` — `onStart` / `onDone` / `onError` / `onRangeStart` / `onStop`
- `https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine` — `TTS_SERVICE` queries, `ACTION_INSTALL_TTS_DATA`, `ACTION_CHECK_TTS_DATA`
- `https://developer.android.com/about/versions/14/changes/fgs-types-required` — `mediaPlayback`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, runtime prerequisites none
- `https://developer.android.com/media/optimize/audio-focus` — `AudioFocusRequest`, `CONTENT_TYPE_SPEECH`, target 35 FGS rule
- `https://developer.android.com/media/implement/surfaces/mobile` — Android 13 compact slots from `PlaybackState`
- `https://developer.android.com/reference/android/media/session/MediaSession.Callback` — `onPlay` / `onPause` / `onStop` / `onSkipToNext` / `onSkipToPrevious`
- `https://developer.android.com/about/versions/13/behavior-changes-all` — media session notifications exempt from `POST_NOTIFICATIONS`
- `https://developer.android.com/reference/android/view/textclassifier/TextClassifier` — `detectLanguage` API 29, worker thread
- In-repo: `UserSettings.kt` `DEFAULT_JSON`, `Nip78.kt` `SETTINGS_D`, webapp `TTSSettings.tsx` / `TTSControls.tsx` / `settingsService.ts`, reader `HighlightMarks` / `ArticleFind` / `VolumeKeys` / `ReadingPositionStore`

### Secondary (MEDIUM confidence)

- Android 13 notification permission docs (media exemption vs FGS shade visibility)
- OEM `setSpeechRate` clamp reports (not in official API)

### Tertiary (LOW confidence)

- Exact AOSP `getMaxSpeechInputLength` numeric default
- OEM TTS settings activity names

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - platform APIs read from developer.android.com; no new packages
- Architecture: HIGH - locked FGS + MediaSession + existing reader/settings seams, verified in source
- Pitfalls: HIGH for pause/FGS/PlaybackState/volume keys; MEDIUM for OEM rate clamp and language detect on API 26–28

**Research date:** 2026-08-18
**Valid until:** 2026-09-17 (stable platform APIs)
