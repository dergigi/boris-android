package org.dergigi.boris.tts

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.nostr.Profile

/** Loads an article and starts TTS from saved progress, if any. */
suspend fun startListening(context: Context, url: String): String? {
    val app = context.applicationContext
    val content = try {
        withContext(Dispatchers.IO) {
            val repo = ReaderRepository()
            repo.peekCached(url) ?: repo.fetch(url)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        return error.message?.takeIf { it.isNotBlank() }
            ?: app.getString(R.string.tts_empty_heading)
    }
    val paragraphs = TtsText.paragraphs(content)
    if (paragraphs.isEmpty()) return app.getString(R.string.tts_empty_heading)
    val startIndex = TtsText.listenStartIndex(
        ReadingPositionStore.fraction(content.url),
        paragraphs.size,
    )
    val author = content.authorPubkey?.trim()
        ?.takeIf { it.length == 64 }
        ?.let { Profile.displayName(it, null) }
    TtsPlayback.start(app, content, startIndex, author)
    return null
}
