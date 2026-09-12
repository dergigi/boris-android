package org.dergigi.boris.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ImportedArticles
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.PocketCsv
import org.dergigi.boris.data.ReadwiseCsv
import org.dergigi.boris.data.ArticleExport
import org.dergigi.boris.data.ArticleImport
import org.dergigi.boris.data.ArticleImportProgress
import org.dergigi.boris.data.SessionStore

enum class ArticleImportSource { Readwise, Pocket }

data class ArticleImportUiState(
    val source: ArticleImportSource? = null,
    val busy: Boolean = false,
    val preview: ArticleExport? = null,
    val progress: ArticleImportProgress? = null,
    val error: String? = null,
)

class ArticleImportViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ArticleImportUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun choose(uri: Uri, source: ArticleImportSource) {
        if (_state.value.busy) return
        _state.value = ArticleImportUiState(source = source, busy = true)
        job = viewModelScope.launch {
            try {
                val export = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.reader(Charsets.UTF_8).use { reader ->
                            when (source) {
                                ArticleImportSource.Readwise -> ReadwiseCsv.parse(reader)
                                ArticleImportSource.Pocket -> PocketCsv.parse(reader)
                            }
                        }
                    } ?: error("Could not open the selected file.")
                }
                _state.value = ArticleImportUiState(source = source, preview = export)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.value = ArticleImportUiState(source = source, error = e.message ?: "Could not read the CSV.")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun start() {
        val export = _state.value.preview ?: return
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null, progress = null)
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val pubkey = SessionStore.load(getApplication())?.pubkeyHex
                    val known = ArticleImport.knownUrls(pubkey)
                    val repository = ReaderRepository()
                    ArticleImport.run(export, known, fetch = { repository.fetch(it) }, save = ImportedArticles::add) {
                        _state.value = _state.value.copy(progress = it)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Import failed.")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun stop() { job?.cancel() }

    fun clearImported() {
        if (_state.value.busy) return
        _state.value = ArticleImportUiState(busy = true)
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { ImportedArticles.clear() }
                _state.value = ArticleImportUiState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.value = ArticleImportUiState(error = e.message ?: "Could not remove imported articles.")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }
}
