package org.dergigi.boris.nostr

object Nip19 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val charsetRev = IntArray(128) { -1 }.also { table ->
        CHARSET.forEachIndexed { i, c -> table[c.code] = i }
    }

    fun npubEncode(pubkeyHex: String): String =
        bech32Encode("npub", pubkeyHex.hexToByteArray())

    fun npubDecode(npub: String): String {
        val (hrp, data) = bech32Decode(npub)
        require(hrp == "npub") { "Expected npub, got $hrp" }
        require(data.size == 32) { "Invalid npub length" }
        return data.toHex()
    }

    fun normalizePubkey(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return try {
            when {
                trimmed.startsWith("npub1", ignoreCase = true) -> npubDecode(trimmed)
                else -> {
                    val hex = trimmed.lowercase()
                    if (hex.length == 64 && hex.all { it in '0'..'9' || it in 'a'..'f' }) hex else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun bech32Encode(hrp: String, data: ByteArray): String {
        val values = convertBits(data, 8, 5, pad = true)
        val checksum = bech32Checksum(hrp, values)
        return buildString(hrp.length + 1 + values.size + 6) {
            append(hrp)
            append('1')
            for (v in values) append(CHARSET[v])
            for (v in checksum) append(CHARSET[v])
        }
    }

    private fun bech32Decode(str: String): Pair<String, ByteArray> {
        val lower = str.lowercase()
        val pos = lower.lastIndexOf('1')
        require(pos >= 1) { "Invalid bech32 string" }
        val hrp = lower.substring(0, pos)
        val dataStr = lower.substring(pos + 1)
        require(dataStr.length >= 6) { "Bech32 data too short" }
        val values = IntArray(dataStr.length) { i ->
            val v = charsetRev[dataStr[i].code]
            require(v != -1) { "Invalid bech32 character" }
            v
        }
        require(verifyChecksum(hrp, values)) { "Invalid bech32 checksum" }
        val data = values.copyOfRange(0, values.size - 6)
        return hrp to convertBits(data, 5, 8, pad = false)
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): IntArray {
        var acc = 0
        var bits = 0
        val ret = mutableListOf<Int>()
        val maxv = (1 shl toBits) - 1
        for (b in data) {
            acc = (acc shl fromBits) or (b.toInt() and 0xFF)
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                ret.add((acc shr bits) and maxv)
            }
        }
        if (pad && bits > 0) ret.add((acc shl (toBits - bits)) and maxv)
        return ret.toIntArray()
    }

    private fun convertBits(data: IntArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val ret = mutableListOf<Byte>()
        val maxv = (1 shl toBits) - 1
        for (v in data) {
            acc = (acc shl fromBits) or v
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                ret.add(((acc shr bits) and maxv).toByte())
            }
        }
        if (pad && bits > 0) ret.add(((acc shl (toBits - bits)) and maxv).toByte())
        return ret.toByteArray()
    }

    private fun bech32Polymod(values: IntArray): Int {
        val gen = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        var chk = 1
        for (v in values) {
            val b = chk shr 25
            chk = ((chk and 0x1ffffff) shl 5) xor v
            for (i in 0..4) if ((b shr i) and 1 == 1) chk = chk xor gen[i]
        }
        return chk
    }

    private fun hrpExpand(hrp: String): IntArray {
        val ret = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) {
            ret[i] = hrp[i].code shr 5
            ret[i + hrp.length + 1] = hrp[i].code and 31
        }
        return ret
    }

    private fun verifyChecksum(hrp: String, values: IntArray): Boolean {
        val exp = hrpExpand(hrp)
        val combined = IntArray(exp.size + values.size)
        exp.copyInto(combined)
        values.copyInto(combined, exp.size)
        return bech32Polymod(combined) == 1
    }

    private fun bech32Checksum(hrp: String, values: IntArray): IntArray {
        val exp = hrpExpand(hrp)
        val combined = IntArray(exp.size + values.size + 6)
        exp.copyInto(combined)
        values.copyInto(combined, exp.size)
        val polymod = bech32Polymod(combined) xor 1
        return IntArray(6) { i -> (polymod shr (5 * (5 - i))) and 31 }
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

internal fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Odd hex length" }
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
