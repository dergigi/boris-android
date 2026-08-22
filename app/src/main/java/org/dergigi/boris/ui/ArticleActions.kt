package org.dergigi.boris.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.tts.requestTtsNotificationPermissionOnce
import org.dergigi.boris.tts.startListening as startArticleListening

fun shareArticleLink(context: Context, title: String?, url: String) {
    val shareUrl = NostrLink.parse(url)?.publicUrl ?: url
    val text = if (title.isNullOrBlank()) shareUrl else "$title\n$shareUrl"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (!title.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.reader_share_article)),
    )
}

fun copyArticleLink(context: Context, clipboard: ClipboardManager, url: String) {
    copyPlainLink(context, clipboard, NostrLink.copyText(url))
}

fun copyPlainLink(context: Context, clipboard: ClipboardManager, text: String) {
    clipboard.setText(AnnotatedString(text))
    Toast.makeText(context, context.getString(R.string.reader_copied), Toast.LENGTH_SHORT).show()
}

fun openOriginalArticle(context: Context, url: String) {
    val target = NostrLink.parse(url)?.publicUrl ?: url
    openExternalUri(context, target)
}

data class ArticleActionHandlers(
    val loggedIn: Boolean,
    val archivedKeys: Set<String>,
    val onListen: (String) -> Unit,
    val onMarkAsRead: (url: String, title: String?, imageUrl: String?) -> Unit,
) {
    fun archived(url: String?): Boolean =
        url != null && ArchivedArticles.isArchived(url, archivedKeys)
}

class ArticleActionsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _loggedIn = MutableStateFlow(SessionStore.load(application) != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _archivedKeys = MutableStateFlow(loadArchivedKeys())
    val archivedKeys: StateFlow<Set<String>> = _archivedKeys.asStateFlow()

    private var listenJob: Job? = null
    private val markAsReadAction = MarkAsReadAction(
        app = application,
        scope = viewModelScope,
        onMessage = { _message.value = it },
        onArchived = { key, _ ->
            if (key != null) _archivedKeys.value += key
        },
    )

    fun refreshSession() {
        _loggedIn.value = SessionStore.load(getApplication()) != null
        _archivedKeys.value = loadArchivedKeys() + _archivedKeys.value
    }

    fun startListening(url: String) {
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            startArticleListening(getApplication(), url)?.let { _message.value = it }
        }
    }

    fun markAsRead(url: String, title: String?, imageUrl: String?): Intent? =
        markAsReadAction.request(url, title, imageUrl)

    fun onArchiveSignerResult(resultCode: Int, data: Intent?) {
        markAsReadAction.onSignerResult(resultCode, data)
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun loadArchivedKeys(): Set<String> {
        val hex = SessionStore.load(getApplication())?.pubkeyHex ?: return emptySet()
        return ArchivedArticles.keys(RelayQuery.cachedArchiveReactions(hex))
    }
}

@Composable
fun rememberArticleActions(
    viewModel: ArticleActionsViewModel = viewModel(),
): ArticleActionHandlers {
    val context = LocalContext.current
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val archivedKeys by viewModel.archivedKeys.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val archiveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onArchiveSignerResult(result.resultCode, result.data)
    }
    val ttsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSession()
    }
    return ArticleActionHandlers(
        loggedIn = loggedIn,
        archivedKeys = archivedKeys,
        onListen = { url ->
            requestTtsNotificationPermissionOnce(context) {
                ttsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            viewModel.startListening(url)
        },
        onMarkAsRead = { url, title, imageUrl ->
            viewModel.markAsRead(url, title, imageUrl)?.let(archiveLauncher::launch)
        },
    )
}
