package org.dergigi.boris.ui.reader

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerDecryptResult
import org.dergigi.boris.nostr.BunkerEncryptResult
import org.dergigi.boris.nostr.BunkerSignResult
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class PaintedHighlight(
    val id: String,
    val quote: String,
    val mine: Boolean,
    val friend: Boolean = false,
    val pubkey: String = "",
    val createdAt: Long = 0L,
    val context: String? = null,
    val authorName: String = "",
    val authorPicture: String? = null,
)

class ReaderViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = ReaderRepository()
    val url: String = decodeUrl(savedStateHandle.get<String>(URL_ARG).orEmpty())
    val focusHighlightId: String =
        decodeUrl(savedStateHandle.get<String>(HIGHLIGHT_ARG).orEmpty()).trim().lowercase()

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _gallery = MutableStateFlow<ImageGalleryState?>(null)
    val gallery: StateFlow<ImageGalleryState?> = _gallery.asStateFlow()

    private val _highlights = MutableStateFlow<List<PaintedHighlight>>(emptyList())
    val highlights: StateFlow<List<PaintedHighlight>> = _highlights.asStateFlow()

    private val _highlightCount = MutableStateFlow(0)
    val highlightCount: StateFlow<Int> = _highlightCount.asStateFlow()

    private val _highlightsLoaded = MutableStateFlow(false)
    val highlightsLoaded: StateFlow<Boolean> = _highlightsLoaded.asStateFlow()

    private val _loggedIn = MutableStateFlow(SessionStore.load(application) != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _canSave = MutableStateFlow(false)
    val canSave: StateFlow<Boolean> = _canSave.asStateFlow()

    private val _archived = MutableStateFlow(false)
    val archived: StateFlow<Boolean> = _archived.asStateFlow()

    private val _author = MutableStateFlow<Profile?>(null)
    val author: StateFlow<Profile?> = _author.asStateFlow()

    private val _signIntent = MutableStateFlow<Intent?>(null)
    val signIntent: StateFlow<Intent?> = _signIntent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var highlightJob: Job? = null
    private var membershipJob: Job? = null
    private var archiveJob: Job? = null
    private var authorJob: Job? = null
    private var pendingUnsigned: PendingUnsignedEvent? = null
    private var pendingLibrary: PendingLibrary? = null
    private var inLibrary = false
    private var saving = false
    private var archiving = false
    private var pendingArchive = false
    private var archiveIds = emptyList<String>()

    init {
        load()
    }

    fun openGallery(urls: List<String>, index: Int) {
        if (urls.isEmpty()) return
        _gallery.value = ImageGalleryState(
            urls = urls,
            initialIndex = index.coerceIn(0, urls.lastIndex),
        )
    }

    fun closeGallery() {
        _gallery.value = null
    }

    fun setGalleryIndex(index: Int) {
        val current = _gallery.value ?: return
        val next = index.coerceIn(0, current.urls.lastIndex)
        if (next == current.initialIndex) return
        _gallery.value = current.copy(initialIndex = next)
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun consumeSignIntent() {
        _signIntent.value = null
    }

    fun load() {
        if (url.isBlank()) {
            _state.value = ReaderUiState.Error("No URL to read.", url)
            _highlights.value = emptyList()
            _highlightCount.value = 0
            _highlightsLoaded.value = true
            inLibrary = false
            resetArchive()
            resetAuthor()
            publishSaveState()
            return
        }
        viewModelScope.launch {
            _state.value = ReaderUiState.Loading
            _highlights.value = emptyList()
            _highlightCount.value = 0
            _highlightsLoaded.value = false
            inLibrary = false
            resetArchive()
            resetAuthor()
            publishSaveState()
            try {
                val content = withContext(Dispatchers.IO) { repository.fetch(url) }
                _state.value = ReaderUiState.Ready(content)
                startHighlightFetch(content)
                startMembershipCheck(content)
                startArchiveCheck(content)
                startAuthorFetch(content)
            } catch (e: Exception) {
                highlightJob?.cancel()
                membershipJob?.cancel()
                archiveJob?.cancel()
                authorJob?.cancel()
                _highlights.value = emptyList()
                _highlightCount.value = 0
                _highlightsLoaded.value = true
                _state.value = ReaderUiState.Error(
                    e.message ?: "Failed to load this article.",
                    url,
                )
                publishSaveState()
            }
        }
    }

    fun highlight(quote: String): Intent? {
        val trimmed = quote.trim()
        val app = getApplication<Application>()
        if (trimmed.isBlank()) {
            _message.value = app.getString(R.string.highlight_cancelled)
            return null
        }
        val session = SessionStore.load(app) ?: return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        val context = Nip84.extractContext(trimmed, content.body)
        when (session) {
            is Session.Amber -> {
                val createdAt = System.currentTimeMillis() / 1000
                val tags = Nip84.tags(
                    content.url,
                    context,
                    content.articleCoordinate,
                    content.eventId,
                    content.authorPubkey,
                )
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_HIGHLIGHT,
                    tags = tags,
                    content = trimmed,
                )
                val unsigned = Nip84.unsignedJson(
                    trimmed,
                    content.url,
                    context,
                    session.pubkeyHex,
                    createdAt,
                    content.articleCoordinate,
                    content.eventId,
                    content.authorPubkey,
                )
                return RemoteSignerBridge.buildSignEventIntent(
                    unsigned,
                    session.signerPackage,
                    session.pubkeyHex,
                )
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = Nip84.unsignedJson(
                    trimmed,
                    content.url,
                    context,
                    pubkeyHex = null,
                    coordinate = content.articleCoordinate,
                    eventId = content.eventId,
                    authorPubkey = content.authorPubkey,
                )
                viewModelScope.launch {
                    signWithBunker(session, unsigned)
                }
                return null
            }
        }
    }

    fun archive(): Intent? {
        val app = getApplication<Application>()
        if (archiving) return null
        val session = SessionStore.load(app) ?: return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        archiving = true
        pendingArchive = true
        return if (_archived.value) {
            requestUnarchive(session)
        } else {
            requestArchive(session, content)
        }
    }

    fun saveToLibrary(privateBookmark: Boolean = true): Intent? {
        val app = getApplication<Application>()
        if (saving || inLibrary) return null
        val session = SessionStore.load(app) ?: return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        saving = true
        publishSaveState()
        return when {
            LibrarySave.isWeb(content) -> requestWebBookmark(session, content)
            privateBookmark -> requestPrivateBookmark(session, content)
            else -> requestPublicBookmark(session, content)
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        when (val step = pendingLibrary) {
            is PendingLibrary.Decrypt -> {
                pendingLibrary = null
                val plaintext = SignerResults.parsePlaintext(resultCode, data)
                if (plaintext == null) {
                    failSave(app.getString(R.string.reader_save_cancelled))
                    return
                }
                continuePrivateAfterDecrypt(session, step.list, step.newTag, plaintext)
            }
            is PendingLibrary.Encrypt -> {
                pendingLibrary = null
                val ciphertext = SignerResults.parsePlaintext(resultCode, data)
                if (ciphertext == null) {
                    failSave(app.getString(R.string.reader_save_cancelled))
                    return
                }
                requestPrivateSign(session, step.list, ciphertext)
            }
            null -> {
                val pending = pendingUnsigned
                pendingUnsigned = null
                val archiveOp = pendingArchive ||
                    pending?.kind == Nip01Event.KIND_DELETION ||
                    (pending != null && Archive.isArchiveKind(pending.kind))
                pendingArchive = false
                val librarySave = pending != null &&
                    !archiveOp &&
                    pending.kind != Nip01Event.KIND_HIGHLIGHT
                when (val result = SignerResults.parseSignedEvent(resultCode, data, session.pubkeyHex, pending)) {
                    is SignerResult.Signed -> onSignedEvent(result.event)
                    SignerResult.Rejected -> {
                        when {
                            archiveOp -> failArchive(app.getString(R.string.reader_archive_rejected))
                            librarySave -> failSave(app.getString(R.string.reader_save_rejected))
                            else -> _message.value = app.getString(R.string.highlight_rejected)
                        }
                    }
                    SignerResult.Cancelled, is SignerResult.Success -> {
                        when {
                            archiveOp -> failArchive(app.getString(R.string.reader_archive_cancelled))
                            librarySave -> failSave(app.getString(R.string.reader_save_cancelled))
                            else -> _message.value = app.getString(R.string.highlight_cancelled)
                        }
                    }
                }
            }
        }
    }

    fun onSignedEvent(event: Nip01Event) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true)) return
        if (!event.verify()) return
        when {
            event.kind == Nip01Event.KIND_HIGHLIGHT -> onSignedHighlight(session, event)
            event.kind == Nip01Event.KIND_WEB_BOOKMARK || event.kind == Nip01Event.KIND_BOOKMARKS -> {
                onSignedLibrary(session, event)
            }
            Archive.isArchive(event) -> onSignedArchive(session, event)
            event.kind == Nip01Event.KIND_DELETION -> onSignedUnarchive(session, event)
        }
    }

    private fun onSignedHighlight(session: Session, event: Nip01Event) {
        val painted = PaintedHighlight(
            id = event.id,
            quote = event.content,
            mine = true,
            pubkey = event.pubkey,
            createdAt = event.createdAt,
            context = event.tagValue("context"),
        )
        if (_highlights.value.none { it.id == event.id }) {
            _highlights.value = _highlights.value + painted
            _highlightCount.value = _highlightCount.value + 1
        }
        viewModelScope.launch(Dispatchers.IO) {
            publish(session, event)
        }
    }

    private fun onSignedLibrary(session: Session, event: Nip01Event) {
        viewModelScope.launch(Dispatchers.IO) {
            publish(session, event)
            inLibrary = true
            saving = false
            _message.value = getApplication<Application>().getString(R.string.reader_saved)
            publishSaveState()
        }
    }

    private fun requestArchive(session: Session, content: ReadableContent): Intent? {
        val createdAt = System.currentTimeMillis() / 1000
        val kind = Archive.kind(content)
        val tags = Archive.tags(content)
        if (kind == null || tags == null) {
            failArchive(getApplication<Application>().getString(R.string.reader_archive_failed))
            return null
        }
        return when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = kind,
                    tags = tags,
                    content = Archive.EMOJI,
                )
                val unsigned = Archive.unsignedJson(content, session.pubkeyHex, createdAt)
                if (unsigned == null) {
                    failArchive(getApplication<Application>().getString(R.string.reader_archive_failed))
                    return null
                }
                RemoteSignerBridge.buildSignEventIntent(unsigned, session.signerPackage, session.pubkeyHex)
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = Archive.unsignedJson(content, pubkeyHex = null, createdAt)
                if (unsigned == null) {
                    failArchive(getApplication<Application>().getString(R.string.reader_archive_failed))
                    return null
                }
                viewModelScope.launch { signWithBunker(session, unsigned, SignOp.Archive) }
                null
            }
        }
    }

    private fun requestUnarchive(session: Session): Intent? {
        val createdAt = System.currentTimeMillis() / 1000
        val unsignedAmber = Archive.deleteUnsignedJson(archiveIds, session.pubkeyHex, createdAt)
        if (unsignedAmber == null) {
            failArchive(getApplication<Application>().getString(R.string.reader_archive_failed))
            return null
        }
        val tags = Archive.deleteTags(archiveIds)
        return when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_DELETION,
                    tags = tags,
                    content = "unarchive",
                )
                RemoteSignerBridge.buildSignEventIntent(unsignedAmber, session.signerPackage, session.pubkeyHex)
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = Archive.deleteUnsignedJson(archiveIds, pubkeyHex = null, createdAt)
                    ?: run {
                        failArchive(getApplication<Application>().getString(R.string.reader_archive_failed))
                        return null
                    }
                viewModelScope.launch { signWithBunker(session, unsigned, SignOp.Archive) }
                null
            }
        }
    }

    private fun onSignedArchive(session: Session, event: Nip01Event) {
        archiveIds = listOf(event.id)
        _archived.value = true
        archiving = false
        pendingArchive = false
        viewModelScope.launch(Dispatchers.IO) {
            publish(session, event)
        }
    }

    private fun onSignedUnarchive(session: Session, event: Nip01Event) {
        archiveIds = emptyList()
        _archived.value = false
        archiving = false
        pendingArchive = false
        viewModelScope.launch(Dispatchers.IO) {
            publish(session, event)
        }
    }

    private fun startArchiveCheck(content: ReadableContent) {
        archiveJob?.cancel()
        val session = SessionStore.load(getApplication())
        _loggedIn.value = session != null
        if (session == null || Archive.kind(content) == null) {
            resetArchive()
            return
        }
        archiveJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val relays = buildList {
                    addAll(RelayList.FALLBACK)
                    addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                }.distinct()
                val events = RelayQuery.fetchArchives(relays, session.pubkeyHex, content)
                archiveIds = events.map { it.id }
                _archived.value = events.isNotEmpty()
            } catch (_: Exception) {
                archiveIds = emptyList()
                _archived.value = false
            }
        }
    }

    private fun startAuthorFetch(content: ReadableContent) {
        authorJob?.cancel()
        val pubkey = content.authorPubkey?.trim()?.takeIf { it.length == 64 }
        if (pubkey == null) {
            resetAuthor()
            return
        }
        authorJob = viewModelScope.launch(Dispatchers.IO) {
            _author.value = try {
                RelayQuery.fetchProfile(pubkey)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun resetAuthor() {
        authorJob?.cancel()
        _author.value = null
    }

    private fun resetArchive() {
        archiveJob?.cancel()
        archiveIds = emptyList()
        _archived.value = false
        archiving = false
        pendingArchive = false
    }

    private fun failArchive(message: String) {
        archiving = false
        pendingArchive = false
        pendingUnsigned = null
        _message.value = message
    }

    private fun requestWebBookmark(session: Session, content: ReadableContent): Intent? {
        val createdAt = System.currentTimeMillis() / 1000
        val tags = NipB0.tags(content.url, content.title, createdAt)
        if (tags.isEmpty()) {
            failSave(getApplication<Application>().getString(R.string.reader_save_failed))
            return null
        }
        return when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_WEB_BOOKMARK,
                    tags = tags,
                    content = "",
                )
                NipB0.unsignedJson(content.url, content.title, session.pubkeyHex, createdAt)?.let { unsigned ->
                    RemoteSignerBridge.buildSignEventIntent(unsigned, session.signerPackage, session.pubkeyHex)
                } ?: run {
                    failSave(getApplication<Application>().getString(R.string.reader_save_failed))
                    null
                }
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = NipB0.unsignedJson(content.url, content.title, pubkeyHex = null, createdAt)
                if (unsigned == null) {
                    failSave(getApplication<Application>().getString(R.string.reader_save_failed))
                    return null
                }
                viewModelScope.launch { signWithBunker(session, unsigned, library = true) }
                null
            }
        }
    }

    private fun requestPrivateBookmark(session: Session, content: ReadableContent): Intent? {
        val newTag = LibrarySave.hiddenTag(content)
        if (newTag == null) {
            failSave(getApplication<Application>().getString(R.string.reader_save_failed))
            return null
        }
        return when (session) {
            is Session.Amber -> {
                viewModelScope.launch {
                    val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
                    beginPrivateAmber(session, list, newTag)
                }
                null
            }
            is Session.Bunker -> {
                viewModelScope.launch { savePrivateWithBunker(session, newTag) }
                null
            }
        }
    }

    private fun requestPublicBookmark(session: Session, content: ReadableContent): Intent? {
        val newTag = LibrarySave.hiddenTag(content)
        if (newTag == null) {
            failSave(getApplication<Application>().getString(R.string.reader_save_failed))
            return null
        }
        return when (session) {
            is Session.Amber -> {
                viewModelScope.launch {
                    val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
                    beginPublicAmber(session, list, newTag)
                }
                null
            }
            is Session.Bunker -> {
                viewModelScope.launch { savePublicWithBunker(session, newTag) }
                null
            }
        }
    }

    private fun beginPrivateAmber(
        session: Session.Amber,
        list: Nip01Event?,
        newTag: List<String>,
    ) {
        val ciphertext = list?.content.orEmpty()
        if (list != null && Nip51.looksEncrypted(ciphertext)) {
            pendingLibrary = PendingLibrary.Decrypt(list, newTag)
            _signIntent.value = RemoteSignerBridge.buildDecryptIntent(
                ciphertext = ciphertext,
                signerPackage = session.signerPackage,
                currentUserHex = session.pubkeyHex,
                peerPubkeyHex = session.pubkeyHex,
                nip44 = !Nip51.isNip04(ciphertext),
            )
            return
        }
        requestPrivateEncrypt(session, list, listOf(newTag))
    }

    private fun beginPublicAmber(
        session: Session.Amber,
        list: Nip01Event?,
        newTag: List<String>,
    ) {
        val tags = list?.tags.orEmpty()
        if (Nip51.containsTag(tags, newTag)) {
            inLibrary = true
            saving = false
            _message.value = getApplication<Application>().getString(R.string.reader_already_saved)
            publishSaveState()
            return
        }
        requestBookmarkListSign(session, tags + listOf(newTag), list?.content.orEmpty())
    }

    private fun continuePrivateAfterDecrypt(
        session: Session,
        list: Nip01Event,
        newTag: List<String>,
        plaintext: String,
    ) {
        val tags = Nip51.parseTagArray(plaintext)
        if (tags == null) {
            failSave(getApplication<Application>().getString(R.string.reader_save_failed))
            return
        }
        if (Nip51.containsTag(tags, newTag)) {
            inLibrary = true
            saving = false
            _message.value = getApplication<Application>().getString(R.string.reader_already_saved)
            publishSaveState()
            return
        }
        when (session) {
            is Session.Amber -> requestPrivateEncrypt(session, list, tags + listOf(newTag))
            is Session.Bunker -> Unit
        }
    }

    private fun requestPrivateEncrypt(
        session: Session.Amber,
        list: Nip01Event?,
        hiddenTags: List<List<String>>,
    ) {
        pendingLibrary = PendingLibrary.Encrypt(list ?: emptyBookmarkList(session.pubkeyHex), hiddenTags)
        _signIntent.value = RemoteSignerBridge.buildEncryptIntent(
            plaintext = Nip51.encodeTagArray(hiddenTags),
            signerPackage = session.signerPackage,
            currentUserHex = session.pubkeyHex,
            peerPubkeyHex = session.pubkeyHex,
        )
    }

    private fun requestPrivateSign(
        session: Session,
        list: Nip01Event,
        ciphertext: String,
    ) {
        requestBookmarkListSign(session, list.tags, ciphertext)
    }

    private fun requestBookmarkListSign(
        session: Session,
        publicTags: List<List<String>>,
        content: String,
    ) {
        val createdAt = System.currentTimeMillis() / 1000
        when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_BOOKMARKS,
                    tags = publicTags,
                    content = content,
                )
                _signIntent.value = RemoteSignerBridge.buildSignEventIntent(
                    Nip51.unsignedJson(publicTags, content, session.pubkeyHex, createdAt),
                    session.signerPackage,
                    session.pubkeyHex,
                )
            }
            is Session.Bunker -> {
                viewModelScope.launch {
                    signWithBunker(
                        session,
                        Nip51.unsignedJson(publicTags, content, pubkeyHex = null, createdAt),
                        library = true,
                    )
                }
            }
        }
    }

    private suspend fun savePrivateWithBunker(session: Session.Bunker, newTag: List<String>) {
        val app = getApplication<Application>()
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            failSave(app.getString(R.string.reader_save_cancelled))
            return
        }
        try {
            val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
            val hidden = if (list != null && Nip51.looksEncrypted(list.content)) {
                val decrypted = withContext(Dispatchers.IO) {
                    BunkerClient(onAuthUrl = ::openAuthUrl).decrypt(
                        session.relays,
                        session.remoteSignerPubkey,
                        privkey,
                        session.pubkeyHex,
                        list.content,
                        nip44 = !Nip51.isNip04(list.content),
                    )
                }
                when (decrypted) {
                    is BunkerDecryptResult.Plaintext -> Nip51.parseTagArray(decrypted.value)
                    else -> null
                }
            } else {
                emptyList()
            }
            if (hidden == null) {
                failSave(app.getString(R.string.reader_save_failed))
                return
            }
            if (Nip51.containsTag(hidden, newTag)) {
                inLibrary = true
                saving = false
                _message.value = app.getString(R.string.reader_already_saved)
                publishSaveState()
                return
            }
            val encrypted = withContext(Dispatchers.IO) {
                BunkerClient(onAuthUrl = ::openAuthUrl).encrypt(
                    session.relays,
                    session.remoteSignerPubkey,
                    privkey,
                    session.pubkeyHex,
                    Nip51.encodeTagArray(hidden + listOf(newTag)),
                )
            }
            val ciphertext = when (encrypted) {
                is BunkerEncryptResult.Ciphertext -> encrypted.value
                else -> null
            }
            if (ciphertext == null) {
                failSave(app.getString(R.string.reader_save_rejected))
                return
            }
            val createdAt = System.currentTimeMillis() / 1000
            val tags = list?.tags.orEmpty()
            signWithBunker(
                session,
                Nip51.unsignedJson(tags, ciphertext, pubkeyHex = null, createdAt),
                library = true,
            )
        } finally {
            privkey.fill(0)
        }
    }

    private suspend fun savePublicWithBunker(session: Session.Bunker, newTag: List<String>) {
        val app = getApplication<Application>()
        val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
        val tags = list?.tags.orEmpty()
        if (Nip51.containsTag(tags, newTag)) {
            inLibrary = true
            saving = false
            _message.value = app.getString(R.string.reader_already_saved)
            publishSaveState()
            return
        }
        val createdAt = System.currentTimeMillis() / 1000
        signWithBunker(
            session,
            Nip51.unsignedJson(tags + listOf(newTag), list?.content.orEmpty(), pubkeyHex = null, createdAt),
            library = true,
        )
    }

    private fun startMembershipCheck(content: ReadableContent) {
        membershipJob?.cancel()
        val session = SessionStore.load(getApplication())
        _loggedIn.value = session != null
        if (session == null) {
            inLibrary = false
            publishSaveState()
            return
        }
        membershipJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val relays = buildList {
                    addAll(RelayList.FALLBACK)
                    addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                }.distinct()
                val list = RelayQuery.fetchBookmarkList(session.pubkeyHex, relays)
                val web = RelayQuery.fetchWebBookmarks(session.pubkeyHex, relays)
                inLibrary = LibrarySave.isSaved(content, list, web)
            } catch (_: Exception) {
                inLibrary = false
            }
            publishSaveState()
        }
    }

    private suspend fun fetchBookmarkList(pubkeyHex: String): Nip01Event? {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(RelayQuery.fetchRelayList(pubkeyHex).read)
        }.distinct()
        return RelayQuery.fetchBookmarkList(pubkeyHex, relays)
    }

    private fun emptyBookmarkList(pubkeyHex: String): Nip01Event =
        Nip01Event(
            id = "0".repeat(64),
            pubkey = pubkeyHex,
            createdAt = 0,
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = emptyList(),
            content = "",
            sig = "0".repeat(128),
        )

    private fun publish(session: Session, event: Nip01Event) {
        EventPublisher.publish(session.pubkeyHex, event)
    }

    private fun failSave(message: String) {
        saving = false
        pendingLibrary = null
        pendingUnsigned = null
        _message.value = message
        publishSaveState()
    }

    private fun publishSaveState() {
        _canSave.value = _loggedIn.value &&
            !inLibrary &&
            !saving &&
            _state.value is ReaderUiState.Ready
    }

    private suspend fun signWithBunker(session: Session.Bunker, unsignedJson: String) {
        signWithBunker(session, unsignedJson, SignOp.Highlight)
    }

    private suspend fun signWithBunker(
        session: Session.Bunker,
        unsignedJson: String,
        library: Boolean,
    ) {
        signWithBunker(session, unsignedJson, if (library) SignOp.Library else SignOp.Highlight)
    }

    private suspend fun signWithBunker(
        session: Session.Bunker,
        unsignedJson: String,
        op: SignOp,
    ) {
        val app = getApplication<Application>()
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            bunkerFailure(op, rejected = false)
            return
        }
        try {
            val result = withContext(Dispatchers.IO) {
                BunkerClient(onAuthUrl = ::openAuthUrl).signEvent(
                    session.relays,
                    session.remoteSignerPubkey,
                    privkey,
                    unsignedJson,
                )
            }
            when (result) {
                is BunkerSignResult.Signed -> onSignedEvent(result.event)
                BunkerSignResult.Rejected -> bunkerFailure(op, rejected = true)
                BunkerSignResult.RelayTimeout -> bunkerFailure(op, rejected = false)
            }
        } finally {
            privkey.fill(0)
        }
    }

    private fun bunkerFailure(op: SignOp, rejected: Boolean) {
        val app = getApplication<Application>()
        when (op) {
            SignOp.Archive -> failArchive(
                app.getString(if (rejected) R.string.reader_archive_rejected else R.string.reader_archive_cancelled),
            )
            SignOp.Library -> failSave(
                app.getString(if (rejected) R.string.reader_save_rejected else R.string.reader_save_cancelled),
            )
            SignOp.Highlight -> {
                _message.value = app.getString(
                    if (rejected) R.string.highlight_rejected else R.string.highlight_cancelled,
                )
            }
        }
    }

    private fun cachedHighlightsFor(content: ReadableContent): List<Nip01Event> =
        when {
            !content.articleCoordinate.isNullOrBlank() || !content.eventId.isNullOrBlank() -> {
                RelayQuery.cachedHighlightsForArticle(content.articleCoordinate, content.eventId)
            }
            else -> RelayQuery.cachedHighlights(content.url)
        }

    private fun paintHighlights(
        events: List<Nip01Event>,
        pubkeyHex: String?,
        friends: Set<String>,
    ) {
        val profiles = RelayQuery.cachedProfiles(events.map { it.pubkey })
        _highlightCount.value = events.size
        _highlights.value = events.map { event ->
            val mine = pubkeyHex != null && event.pubkey.equals(pubkeyHex, ignoreCase = true)
            val key = event.pubkey.lowercase()
            PaintedHighlight(
                id = event.id,
                quote = event.content,
                mine = mine,
                friend = !mine && key in friends,
                pubkey = event.pubkey,
                createdAt = event.createdAt,
                context = event.tagValue("context"),
                authorName = Profile.displayName(event.pubkey, profiles[key]),
                authorPicture = profiles[key]?.picture,
            )
        }
    }

    private fun startHighlightFetch(content: ReadableContent) {
        highlightJob?.cancel()
        val session = SessionStore.load(getApplication())
        _loggedIn.value = session != null
        _highlightsLoaded.value = false
        publishSaveState()
        highlightJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val friends = if (session != null) {
                    RelayQuery.cachedContactPubkeys(session.pubkeyHex)
                } else {
                    emptySet()
                }
                val cached = cachedHighlightsFor(content)
                if (cached.isNotEmpty()) paintHighlights(cached, session?.pubkeyHex, friends)
                val relays = buildList {
                    addAll(RelayList.FALLBACK)
                    if (session != null) addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                }.distinct()
                val contacts = if (session != null) {
                    RelayQuery.fetchContactPubkeys(session.pubkeyHex)
                } else {
                    emptySet()
                }
                val events = when {
                    !content.articleCoordinate.isNullOrBlank() || !content.eventId.isNullOrBlank() -> {
                        RelayQuery.fetchHighlightsForArticle(
                            relays,
                            content.articleCoordinate,
                            content.eventId,
                        )
                    }
                    else -> RelayQuery.fetchHighlights(relays, content.url)
                }
                paintHighlights(events, session?.pubkeyHex, contacts)
                val authors = events.map { it.pubkey }.distinct()
                if (authors.isNotEmpty()) {
                    RelayQuery.fetchProfiles(relays, authors)
                    paintHighlights(events, session?.pubkeyHex, contacts)
                }
            } catch (_: Exception) {
            } finally {
                _highlightsLoaded.value = true
            }
        }
    }

    private fun openAuthUrl(url: String) {
        val app = getApplication<Application>()
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    companion object {
        const val URL_ARG = "url"
        const val HIGHLIGHT_ARG = "highlight"

        fun decodeUrl(encoded: String): String =
            URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
    }
}

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Ready(val content: ReadableContent) : ReaderUiState
    data class Error(val message: String, val url: String) : ReaderUiState
}

private sealed class PendingLibrary {
    data class Decrypt(val list: Nip01Event, val newTag: List<String>) : PendingLibrary()
    data class Encrypt(val list: Nip01Event, val hiddenTags: List<List<String>>) : PendingLibrary()
}

private enum class SignOp { Highlight, Library, Archive }
