package org.dergigi.boris.tts

import kotlinx.coroutines.flow.MutableStateFlow
import org.dergigi.boris.data.ReadingPositionStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TtsPlaybackTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun url(name: String) = "https://example.com/$name-${System.nanoTime()}"

    @After
    fun tearDown() {
        sessionFlow().value = null
    }

    @Test
    fun queuedParagraphCompletionAdvancesReadingPosition() {
        val article = url("tts-progress")
        resetStore("queued")
        sessionFlow().value = session(article, index = 0)

        TtsPlayback.onQueuedParagraphFinished(0)
        assertEquals(0.25f, ReadingPositionStore.fraction(article), 0.0001f)

        sessionFlow().value = sessionFlow().value!!.copy(index = 2)
        TtsPlayback.onQueuedParagraphFinished(2)
        assertEquals(0.75f, ReadingPositionStore.fraction(article), 0.0001f)
    }

    @Test
    fun queuedParagraphCompletionDoesNotMoveProgressBackwards() {
        val article = url("tts-backwards")
        resetStore("backwards")
        ReadingPositionStore.save(article, 0.75f)
        sessionFlow().value = session(article, index = 1)

        TtsPlayback.onQueuedParagraphFinished(1)

        assertEquals(0.75f, ReadingPositionStore.fraction(article), 0.0001f)
    }

    private fun resetStore(name: String) {
        ReadingPositionStore.init(folder.newFile("reading-position-$name.json"))
    }

    private fun session(url: String, index: Int) = TtsSession(
        url = url,
        title = "Article",
        author = null,
        imageUrl = null,
        paragraphs = listOf("One.", "Two.", "Three.", "Four."),
        index = index,
        playing = true,
        paused = false,
    )

    @Suppress("UNCHECKED_CAST")
    private fun sessionFlow(): MutableStateFlow<TtsSession?> {
        val field = TtsPlayback::class.java.getDeclaredField("_session")
        field.isAccessible = true
        return field.get(TtsPlayback) as MutableStateFlow<TtsSession?>
    }
}
