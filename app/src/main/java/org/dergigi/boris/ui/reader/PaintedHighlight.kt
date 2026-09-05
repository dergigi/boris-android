package org.dergigi.boris.ui.reader

data class PaintedHighlight(
    val id: String,
    val quote: String,
    val mine: Boolean,
    val friend: Boolean = false,
    val foaf: Boolean = false,
    val pubkey: String = "",
    val createdAt: Long = 0L,
    val context: String? = null,
    val comment: String? = null,
    val authorName: String = "",
    val authorPicture: String? = null,
    val find: Boolean = false,
    val ignoreCase: Boolean = false,
    val spoken: Boolean = false,
    val outline: Boolean = false,
)
