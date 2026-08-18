package org.dergigi.boris.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.ServiceCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.SettingsSync

/**
 * mediaPlayback foreground service owning the TextToSpeech engine, MediaSession,
 * audio focus, and the lock-screen notification (D-01, D-04, D-05).
 */
class TtsPlaybackService : Service(), TtsPlayback.Engine {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var pausedByFocusLoss = false
    private var resolvedLanguageUrl: String? = null
    private var lastChunkId: String? = null
    private var previewing = false
    private var foregrounded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createChannel()
        mediaSession = MediaSession(this, SESSION_TAG).apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = TtsPlayback.resume()
                override fun onPause() = TtsPlayback.pause()
                override fun onStop() = TtsPlayback.stop()
                override fun onSkipToNext() = TtsPlayback.skip(1)
                override fun onSkipToPrevious() = TtsPlayback.skip(-1)
            })
        }
        TtsPlayback.engine = this
        tts = TextToSpeech(applicationContext) { status ->
            handler.post { onTtsInit(status) }
        }
        scope.launch {
            TtsPlayback.session.collect { session ->
                if (session == null) {
                    if (!previewing && TtsPlayback.pendingPreview == null) stopPlaybackAndSelf()
                } else {
                    updateMediaSession(session)
                    if (foregrounded) {
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIFICATION_ID, buildNotification(session))
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregrounded) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(TtsPlayback.session.value),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
            foregrounded = true
        }
        when (intent?.action) {
            ACTION_PLAY -> TtsPlayback.resume()
            ACTION_PAUSE -> TtsPlayback.pause()
            ACTION_STOP -> TtsPlayback.stop()
            ACTION_NEXT -> TtsPlayback.skip(1)
            ACTION_PREV -> TtsPlayback.skip(-1)
            else -> if (ttsReady) startPending()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (TtsPlayback.engine === this) TtsPlayback.engine = null
        scope.cancel()
        abandonFocus()
        mediaSession?.release()
        mediaSession = null
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    override fun play() {
        handler.post { speakCurrent() }
    }

    override fun pause() {
        handler.post {
            tts?.stop()
            abandonFocus()
        }
    }

    override fun applyRate() {
        handler.post {
            val session = TtsPlayback.session.value ?: return@post
            tts?.setSpeechRate(session.rate.toFloat())
            if (session.playing) speakCurrent()
        }
    }

    override fun preview(text: String) {
        handler.post { speakPreview(text) }
    }

    override fun shutdownSelf() {
        handler.post { stopPlaybackAndSelf() }
    }

    private fun onTtsInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            TtsPlayback.pendingPreview = null
            TtsPlayback.onError(TtsPlayback.ERROR_ENGINE)
            return
        }
        ttsReady = true
        tts?.setOnUtteranceProgressListener(progressListener)
        tts?.setAudioAttributes(speechAttributes())
        startPending()
    }

    private fun startPending() {
        val preview = TtsPlayback.pendingPreview
        if (preview != null) {
            TtsPlayback.pendingPreview = null
            speakPreview(preview)
            return
        }
        if (TtsPlayback.session.value != null) speakCurrent()
    }

    private fun speakCurrent() {
        val engine = tts ?: return
        if (!ttsReady) return
        val session = TtsPlayback.session.value ?: return
        previewing = false
        if (!applyLanguage(session)) return
        if (!requestFocus()) return
        engine.setSpeechRate(session.rate.toFloat())
        val paragraph = session.paragraphs.getOrNull(session.index) ?: return
        val chunks = TtsText.chunks(paragraph, maxSpeechLength())
        chunks.forEachIndexed { j, chunk ->
            val id = "p${session.index}.$j"
            if (j == chunks.lastIndex) lastChunkId = id
            engine.speak(
                chunk,
                if (j == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                id,
            )
        }
    }

    private fun speakPreview(text: String) {
        val engine = tts ?: return
        if (!ttsReady) {
            TtsPlayback.pendingPreview = text
            return
        }
        previewing = true
        resolvedLanguageUrl = null
        applyLanguageFor(text)
        requestFocus()
        engine.setSpeechRate(TtsSpeed.snap(SettingsSync.settings.value.ttsDefaultSpeed).toFloat())
        lastChunkId = PREVIEW_ID
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, PREVIEW_ID)
    }

    private fun applyLanguage(session: TtsSession): Boolean {
        if (resolvedLanguageUrl == session.url) return true
        val sample = session.paragraphs.joinToString(" ").take(2000)
        if (!applyLanguageFor(sample)) return false
        resolvedLanguageUrl = session.url
        return true
    }

    private fun applyLanguageFor(sample: String): Boolean {
        val engine = tts ?: return false
        val settings = SettingsSync.settings.value
        val mode = TtsLanguage.mode(
            settings.ttsLanguageMode,
            settings.ttsUseSystemLanguage,
            settings.ttsDetectContentLanguage,
        )
        val language = TtsLanguage.resolveLanguage(
            mode,
            sample,
            Locale.getDefault().toLanguageTag(),
            TtsLanguage::detectHeuristic,
        )
        val locale = TtsLanguage.locale(language)
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            TtsPlayback.onError(TtsPlayback.ERROR_LANGUAGE)
            return false
        }
        preferOfflineVoice(locale)
        return true
    }

    /** D-05: prefer a voice that does not stream text to the network. */
    private fun preferOfflineVoice(locale: Locale) {
        val engine = tts ?: return
        val current = runCatching { engine.voice }.getOrNull()
        if (current != null &&
            !current.isNetworkConnectionRequired &&
            current.locale.language == locale.language
        ) {
            return
        }
        val voices = runCatching { engine.voices }.getOrNull() ?: return
        val offline = voices.filter {
            it.locale.language == locale.language && !it.isNetworkConnectionRequired
        }
        val voice = offline.firstOrNull { it.locale.country == locale.country }
            ?: offline.firstOrNull()
            ?: return
        runCatching { engine.voice = voice }
    }

    private fun maxSpeechLength(): Int =
        runCatching { TextToSpeech.getMaxSpeechInputLength() }.getOrDefault(4000)

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            handler.post {
                if (utteranceId == PREVIEW_ID) return@post
                parseIndex(utteranceId)?.let(TtsPlayback::onSpeechStarted)
            }
        }

        override fun onDone(utteranceId: String?) {
            handler.post {
                if (utteranceId == PREVIEW_ID) {
                    previewing = false
                    if (TtsPlayback.session.value == null) stopPlaybackAndSelf()
                    return@post
                }
                if (utteranceId != lastChunkId) return@post
                parseIndex(utteranceId)?.let(TtsPlayback::onParagraphFinished)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            handler.post { TtsPlayback.onError(TtsPlayback.ERROR_ENGINE) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            handler.post { TtsPlayback.onError(TtsPlayback.ERROR_ENGINE) }
        }
    }

    private fun parseIndex(utteranceId: String?): Int? {
        if (utteranceId == null || !utteranceId.startsWith("p")) return null
        return utteranceId.substringAfter("p").substringBefore(".").toIntOrNull()
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        handler.post {
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    pausedByFocusLoss = false
                    TtsPlayback.stop()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                -> {
                    // Speech ducking is unintelligible; pause instead (research pitfall).
                    if (TtsPlayback.session.value?.playing == true) {
                        pausedByFocusLoss = true
                        TtsPlayback.pause()
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (pausedByFocusLoss) {
                        pausedByFocusLoss = false
                        TtsPlayback.resume()
                    }
                }
            }
        }
    }

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return false
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(speechAttributes())
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        val request = focusRequest ?: return
        audioManager?.abandonAudioFocusRequest(request)
    }

    private fun speechAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private fun updateMediaSession(session: TtsSession) {
        val media = mediaSession ?: return
        media.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, session.title)
                .apply {
                    session.author?.let { putString(MediaMetadata.METADATA_KEY_ARTIST, it) }
                }
                .build(),
        )
        val state = if (session.playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        media.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, if (session.playing) 1f else 0f)
                .build(),
        )
        media.isActive = true
    }

    private fun stopPlaybackAndSelf() {
        tts?.stop()
        abandonFocus()
        mediaSession?.isActive = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregrounded = false
        stopSelf()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tts_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    /** Metadata only: title and author, never paragraph text (D-04). */
    private fun buildNotification(session: TtsSession?): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_highlighter)
            .setContentTitle(session?.title ?: getString(R.string.app_name))
            .setOngoing(session?.playing == true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            builder.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        session?.author?.let { builder.setContentText(it) }
        if (session != null) {
            builder.addAction(
                action(android.R.drawable.ic_media_previous, R.string.tts_previous_paragraph, ACTION_PREV),
            )
            if (session.playing) {
                builder.addAction(
                    action(android.R.drawable.ic_media_pause, R.string.tts_pause_playback, ACTION_PAUSE),
                )
            } else {
                builder.addAction(
                    action(android.R.drawable.ic_media_play, R.string.tts_resume_playback, ACTION_PLAY),
                )
            }
            builder.addAction(
                action(android.R.drawable.ic_media_next, R.string.tts_next_paragraph, ACTION_NEXT),
            )
            builder.addAction(
                action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    R.string.tts_stop_playback,
                    ACTION_STOP,
                ),
            )
            mediaSession?.let { media ->
                builder.setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(media.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2),
                )
            }
        }
        return builder.build()
    }

    private fun action(icon: Int, label: Int, intentAction: String): Notification.Action {
        val intent = Intent(this, TtsPlaybackService::class.java).setAction(intentAction)
        val pending = PendingIntent.getService(
            this,
            intentAction.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, icon), getString(label), pending)
            .build()
    }

    private companion object {
        const val SESSION_TAG = "BorisTts"
        const val CHANNEL_ID = "tts_playback"
        const val NOTIFICATION_ID = 41
        const val PREVIEW_ID = "preview"
        const val ACTION_PLAY = "org.dergigi.boris.tts.PLAY"
        const val ACTION_PAUSE = "org.dergigi.boris.tts.PAUSE"
        const val ACTION_STOP = "org.dergigi.boris.tts.STOP"
        const val ACTION_NEXT = "org.dergigi.boris.tts.NEXT"
        const val ACTION_PREV = "org.dergigi.boris.tts.PREV"
    }
}
