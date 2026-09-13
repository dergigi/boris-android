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
import org.dergigi.boris.data.ReadwiseCsv
import org.dergigi.boris.data.ReadwiseExport
import org.dergigi.boris.data.ReadwiseImport
import org.dergigi.boris.data.ReadwiseImportProgress
import org.dergigi.boris.data.SessionStore

data class ReadwiseImportUiState(
    val busy: Boolean = false,
    val preview: ReadwiseExport? = null,
    val progress: ReadwiseImportProgress? = null,
    val error: String? = null,
)

class ReadwiseImportViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ReadwiseImportUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun choose(uri: Uri) {
        if (_state.value.busy) return
        _state.value = ReadwiseImportUiState(busy = true)
        job = viewModelScope.launch {
            try {
                val export = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        it.reader(Charsets.UTF_8).use(ReadwiseCsv::parse)
                    } ?: error("Could not open the selected file.")
                }
                _state.value = ReadwiseImportUiState(preview = export)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.value = ReadwiseImportUiState(error = e.message ?: "Could not read the CSV.")
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
                    val known = ReadwiseImport.knownUrls(pubkey)
                    val repository = ReaderRepository()
                    ReadwiseImport.run(export, known, fetch = { repository.fetch(it) }, save = ImportedArticles::add) {
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
        _state.value = ReadwiseImportUiState(busy = true)
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { ImportedArticles.clear() }
                _state.value = ReadwiseImportUiState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.value = ReadwiseImportUiState(error = e.message ?: "Could not remove imported articles.")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }
}
