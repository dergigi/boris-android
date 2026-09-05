package org.dergigi.boris.data

enum class ReaderMargin(val id: String) {
    Compact("compact"),
    Default("default"),
    Comfortable("comfortable"),
    ;

    companion object {
        fun fromId(value: String?): ReaderMargin =
            entries.firstOrNull { it.id == value } ?: Default
    }
}
