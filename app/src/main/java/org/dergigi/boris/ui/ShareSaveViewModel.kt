package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dergigi.boris.R
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.SessionStore

class ShareSaveViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _signIntent = MutableStateFlow<Intent?>(null)
    val signIntent: StateFlow<Intent?> = _signIntent.asStateFlow()

    private val action = LibrarySaveAction(
        app = application,
        scope = viewModelScope,
        onMessage = { _message.value = it },
        onSignIntent = { _signIntent.value = it },
    )

    fun loggedIn(): Boolean = SessionStore.load(getApplication()) != null

    fun save(url: String, title: String?): Intent? {
        val content = LibrarySave.contentFromShare(url, title)
        if (content == null) {
            _message.value = getApplication<Application>().getString(R.string.share_save_failed)
            return null
        }
        return action.request(content)
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        action.onSignerResult(resultCode, data)
    }

    fun consumeSignIntent() {
        _signIntent.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }
}
