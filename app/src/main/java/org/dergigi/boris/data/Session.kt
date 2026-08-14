package org.dergigi.boris.data

data class Session(
    val pubkeyHex: String,
    val signerPackage: String,
) {
    companion object {
        fun fromStored(hex: String?, pkg: String?): Session? {
            val h = hex?.trim()?.lowercase().orEmpty()
            val p = pkg?.trim().orEmpty()
            if (h.length != 64 || h.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
            if (p.isEmpty()) return null
            return Session(h, p)
        }
    }
}
