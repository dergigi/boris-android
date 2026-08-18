package org.dergigi.boris.nostr

data class NaddrPointer(
    val identifier: String,
    val pubkey: String,
    val kind: Int,
    val relays: List<String> = emptyList(),
) {
    val coordinate: String get() = "$kind:${pubkey.lowercase()}:$identifier"
}

data class NeventPointer(
    val eventId: String,
    val relays: List<String> = emptyList(),
    val author: String? = null,
    val kind: Int? = null,
)

data class NprofilePointer(
    val pubkey: String,
    val relays: List<String> = emptyList(),
)

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

    fun naddrEncode(pointer: NaddrPointer): String {
        val payload = buildList {
            add(tlv(0, pointer.identifier.toByteArray(Charsets.UTF_8)))
            for (relay in pointer.relays) {
                add(tlv(1, relay.toByteArray(Charsets.UTF_8)))
            }
            add(tlv(2, pointer.pubkey.hexToByteArray()))
            add(tlv(3, uint32be(pointer.kind)))
        }.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
        return bech32Encode("naddr", payload)
    }

    fun noteEncode(eventIdHex: String): String =
        bech32Encode("note", eventIdHex.hexToByteArray())

    fun noteDecode(note: String): String {
        val (hrp, data) = bech32Decode(note)
        require(hrp == "note") { "Expected note, got $hrp" }
        require(data.size == 32) { "Invalid note length" }
        return data.toHex()
    }

    fun neventEncode(pointer: NeventPointer): String {
        val payload = buildList {
            add(tlv(0, pointer.eventId.hexToByteArray()))
            for (relay in pointer.relays) {
                add(tlv(1, relay.toByteArray(Charsets.UTF_8)))
            }
            pointer.author?.let { add(tlv(2, it.hexToByteArray())) }
            pointer.kind?.let { add(tlv(3, uint32be(it))) }
        }.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
        return bech32Encode("nevent", payload)
    }

    fun nprofileEncode(pointer: NprofilePointer): String {
        val pubkey = pointer.pubkey.hexToByteArray()
        require(pubkey.size == 32) { "Invalid nprofile pubkey length" }
        val payload = buildList {
            add(tlv(0, pubkey))
            for (relay in pointer.relays) {
                add(tlv(1, relay.toByteArray(Charsets.UTF_8)))
            }
        }.fold(ByteArray(0)) { acc, chunk -> acc + chunk }
        return bech32Encode("nprofile", payload)
    }

    fun nprofileDecode(nprofile: String): NprofilePointer {
        val (hrp, data) = bech32Decode(nprofile)
        require(hrp == "nprofile") { "Expected nprofile, got $hrp" }
        val fields = parseTlv(data)
        val pubkey = fields[0]?.firstOrNull()?.toHex()
        require(pubkey != null && pubkey.length == 64) { "nprofile missing pubkey" }
        val relays = fields[1].orEmpty().map { it.toString(Charsets.UTF_8) }
        return NprofilePointer(pubkey, relays)
    }

    fun neventDecode(nevent: String): NeventPointer {
        val (hrp, data) = bech32Decode(nevent)
        require(hrp == "nevent") { "Expected nevent, got $hrp" }
        val fields = parseTlv(data)
        val eventId = fields[0]?.firstOrNull()?.toHex()
        require(eventId != null && eventId.length == 64) { "nevent missing id" }
        val relays = fields[1].orEmpty().map { it.toString(Charsets.UTF_8) }
        val author = fields[2]?.firstOrNull()?.toHex()?.takeIf { it.length == 64 }
        val kind = fields[3]?.firstOrNull()?.let { be32(it) }
        return NeventPointer(eventId, relays, author, kind)
    }

    fun naddrDecode(naddr: String): NaddrPointer {
        val (hrp, data) = bech32Decode(naddr)
        require(hrp == "naddr") { "Expected naddr, got $hrp" }
        val fields = parseTlv(data)
        val identifier = fields[0]?.firstOrNull()?.toString(Charsets.UTF_8)
        require(!identifier.isNullOrEmpty()) { "naddr missing identifier" }
        val pubkey = fields[2]?.firstOrNull()?.toHex()
        require(pubkey != null && pubkey.length == 64) { "naddr missing author" }
        val kindBytes = fields[3]?.firstOrNull()
        require(kindBytes != null && kindBytes.isNotEmpty()) { "naddr missing kind" }
        val relays = fields[1].orEmpty().map { it.toString(Charsets.UTF_8) }
        return NaddrPointer(
            identifier = identifier,
            pubkey = pubkey,
            kind = be32(kindBytes),
            relays = relays,
        )
    }

    private fun tlv(type: Int, value: ByteArray): ByteArray {
        require(value.size <= 255) { "TLV value too long" }
        return byteArrayOf(type.toByte(), value.size.toByte()) + value
    }

    private fun parseTlv(data: ByteArray): Map<Int, List<ByteArray>> {
        val out = mutableMapOf<Int, MutableList<ByteArray>>()
        var i = 0
        while (i + 2 <= data.size) {
            val type = data[i].toInt() and 0xFF
            val len = data[i + 1].toInt() and 0xFF
            i += 2
            if (i + len > data.size) break
            out.getOrPut(type) { mutableListOf() }.add(data.copyOfRange(i, i + len))
            i += len
        }
        return out
    }

    private fun uint32be(value: Int): ByteArray = byteArrayOf(
        (value ushr 24).toByte(),
        (value ushr 16).toByte(),
        (value ushr 8).toByte(),
        value.toByte(),
    )

    private fun be32(bytes: ByteArray): Int {
        var n = 0
        for (b in bytes) n = (n shl 8) or (b.toInt() and 0xFF)
        return n
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
