package org.dergigi.boris.tts

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.SettingsSync

data class TtsSession(
    val url: String,
    val title: String,
    val author: String?,
    val imageUrl: String?,
    val paragraphs: List<String>,
    val index: Int,
    val playing: Boolean,
    val paused: Boolean,
    /** True once the engine actually started speaking; false while initializing. */
    val started: Boolean = false,
    val sentenceIndex: Int = 0,
    val spokenText: String? = null,
    val followAlongEnabled: Boolean = true,
    val followAlongPaused: Boolean = false,
    val rate: Double = TtsSpeed.DEFAULT,
    val errorMessage: String? = null,
)

/**
 * Process-wide playback state, mirroring the SettingsSync StateFlow pattern.
 * Speech itself lives in TtsPlaybackService so it survives the reader (D-02).
 */
object TtsPlayback {
    const val ERROR_ENGINE = "engine"
    const val ERROR_LANGUAGE = "language"

    private val _session = MutableStateFlow<TtsSession?>(null)
    val session: StateFlow<TtsSession?> = _session.asStateFlow()

    /** True while the one-shot settings preview is speaking (session stays null, D-10). */
    private val _previewing = MutableStateFlow(false)
    val previewing: StateFlow<Boolean> = _previewing.asStateFlow()

    /** Engine/language failure from a preview, which has no session to carry it (D-11). */
    private val _previewError = MutableStateFlow<String?>(null)
    val previewError: StateFlow<String?> = _previewError.asStateFlow()

    @Volatile
    internal var engine: Engine? = null

    @Volatile
    internal var pendingPreview: String? = null

    internal interface Engine {
        fun play()
        fun pause()
        fun applyRate()
        fun applyLanguage()
        fun preview(text: String)
        fun shutdownSelf()
    }

    /** One article at a time (D-03): starting a different url replaces the session. */
    fun start(context: Context, content: ReadableContent, startIndex: Int, author: String?) {
        start(context, content, startIndex, author, restartActive = false)
    }

    fun playFrom(
        context: Context,
        content: ReadableContent,
        startIndex: Int,
        author: String?,
        selectedText: String = "",
        ownerText: String = "",
        ownerOffset: Int = 0,
    ) {
        start(
            context,
            content,
            startIndex,
            author,
            restartActive = true,
            selectedText = selectedText,
            ownerText = ownerText,
            ownerOffset = ownerOffset,
        )
    }

    private fun start(
        context: Context,
        content: ReadableContent,
        startIndex: Int,
        author: String?,
        restartActive: Boolean,
        selectedText: String = "",
        ownerText: String = "",
        ownerOffset: Int = 0,
    ) {
        val current = _session.value
        val restartingCurrentArticle = restartActive && current?.url == content.url
        if (current?.url == content.url && current.playing && !restartActive) return
        val paragraphs = TtsText.paragraphs(content).let { all ->
            if (selectedText.isBlank() && ownerText.isBlank()) {
                all
            } else {
                TtsText.applySentenceStart(all, startIndex, selectedText, ownerText, ownerOffset)
            }
        }
        if (paragraphs.isEmpty()) return
        val settings = SettingsSync.settings.value
        pendingPreview = null
        _previewing.value = false
        _previewError.value = null
        _session.value = TtsSession(
            url = content.url,
            title = content.title?.takeIf { it.isNotBlank() } ?: content.url,
            author = author?.takeIf { it.isNotBlank() },
            imageUrl = content.imageUrl?.takeIf { it.isNotBlank() },
            paragraphs = paragraphs,
            index = startIndex.coerceIn(0, paragraphs.size - 1),
            playing = true,
            paused = false,
            followAlongEnabled = if (restartingCurrentArticle) {
                current?.followAlongEnabled ?: settings.ttsFollowAlong
            } else {
                settings.ttsFollowAlong
            },
            rate = if (restartingCurrentArticle) {
                current?.rate ?: TtsSpeed.snap(settings.ttsDefaultSpeed)
            } else {
                TtsSpeed.snap(settings.ttsDefaultSpeed)
            },
            followAlongPaused = if (restartingCurrentArticle) {
                current?.followAlongPaused ?: false
            } else {
                false
            },
        )
        engine?.play() ?: startService(context)
    }

    fun pause() {
        val current = _session.value ?: return
        if (!current.playing) return
        _session.value = current.copy(playing = false, paused = true, started = false)
        engine?.pause()
    }

    fun resume() {
        val current = _session.value ?: return
        if (!current.paused) return
        // D-15: tapping play resumes follow-along auto-scroll after a user scroll.
        _session.value = current.copy(playing = true, paused = false, followAlongPaused = false)
        engine?.play()
    }

    fun stop() {
        _session.value = null
        engine?.shutdownSelf()
    }

    fun skip(delta: Int) {
        val current = _session.value ?: return
        val next = current.index + delta
        if (next < 0) return
        if (next > current.paragraphs.lastIndex) {
            stop()
            return
        }
        _session.value = current.copy(
            index = next,
            playing = true,
            paused = false,
            started = false,
            sentenceIndex = 0,
            spokenText = null,
            followAlongPaused = false,
        )
        engine?.play()
    }

    fun setRate(rate: Double) {
        val snapped = TtsSpeed.snap(rate)
        val current = _session.value ?: return
        if (current.rate == snapped) return
        _session.value = current.copy(rate = snapped)
        engine?.applyRate()
    }

    /** Re-resolve the speaking locale after a language-setting change (D-09). */
    fun applyLanguage() {
        if (_session.value == null) return
        engine?.applyLanguage()
    }

    /**
     * One-shot preview (D-10): no session, no mini player. Stops any speaking
     * article first because there is only one engine.
     */
    fun preview(context: Context, text: String = TtsPreview.EXAMPLE_TEXT) {
        _session.value = null
        _previewing.value = true
        _previewError.value = null
        val current = engine
        if (current != null) {
            current.preview(text)
        } else {
            pendingPreview = text
            startService(context)
        }
    }

    /** Stops the one-shot preview utterance; sessions never exist while previewing. */
    fun stopPreview() {
        pendingPreview = null
        if (!_previewing.value) return
        _previewing.value = false
        engine?.shutdownSelf()
    }

    fun setFollowAlongPaused(paused: Boolean) {
        val current = _session.value ?: return
        if (current.followAlongPaused == paused) return
        _session.value = current.copy(followAlongPaused = paused)
    }

    fun setFollowAlongEnabled(enabled: Boolean) {
        val current = _session.value ?: return
        if (current.followAlongEnabled == enabled) return
        _session.value = current.copy(
            followAlongEnabled = enabled,
            followAlongPaused = if (enabled) false else current.followAlongPaused,
        )
    }

    internal fun onSpeechStarted(index: Int, sentenceIndex: Int, spokenText: String) {
        val current = _session.value ?: return
        if (!current.playing) return
        if (index !in current.paragraphs.indices) return
        val nextSentence = sentenceIndex.coerceAtLeast(0)
        val nextSpoken = spokenText.takeIf { it.isNotBlank() }
        if (current.index != index || !current.started || current.sentenceIndex != nextSentence ||
            current.spokenText != nextSpoken || current.errorMessage != null
        ) {
            _session.value = current.copy(
                index = index,
                started = true,
                sentenceIndex = nextSentence,
                spokenText = nextSpoken,
                errorMessage = null,
            )
        }
    }

    internal fun onParagraphFinished(index: Int) {
        val current = _session.value ?: return
        if (!current.playing || index != current.index) return
        // Listening counts as reading (issue #86): every paragraph spoken to
        // the end advances the saved reading position. Skips do not save until
        // the next paragraph is actually heard through.
        saveListenedPosition(current, index)
        if (index >= current.paragraphs.lastIndex) {
            stop()
            return
        }
        _session.value = current.copy(
            index = index + 1,
            started = false,
            sentenceIndex = 0,
            spokenText = null,
        )
        engine?.play()
    }

    internal fun onQueuedParagraphFinished(index: Int) {
        val current = _session.value ?: return
        if (!current.playing) return
        if (index !in current.paragraphs.indices) return
        if (index > current.index) return
        saveListenedPosition(current, index)
    }

    private fun saveListenedPosition(session: TtsSession, index: Int) {
        val count = session.paragraphs.size
        if (count <= 0) return
        val fraction = (index + 1).toFloat() / count
        if (fraction > ReadingPositionStore.fraction(session.url)) {
            ReadingPositionStore.save(session.url, fraction)
        }
    }

    internal fun onPreviewFinished() {
        _previewing.value = false
    }

    internal fun onError(message: String) {
        _previewing.value = false
        val current = _session.value
        if (current != null) {
            _session.value = current.copy(
                playing = false,
                paused = false,
                started = false,
                errorMessage = message,
            )
        } else {
            _previewError.value = message
        }
        engine?.shutdownSelf()
    }

    private fun startService(context: Context) {
        val app = context.applicationContext
        ContextCompat.startForegroundService(app, Intent(app, TtsPlaybackService::class.java))
    }
}
