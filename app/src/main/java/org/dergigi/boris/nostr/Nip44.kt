package org.dergigi.boris.nostr

import fr.acinq.secp256k1.Secp256k1
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Nip44 {
    private val salt = "nip44-v2".toByteArray(Charsets.UTF_8)
    private const val MIN_PLAINTEXT = 1
    private const val MAX_PLAINTEXT = 0xFFFFFFFFL
    private const val EXTENDED_PREFIX_THRESHOLD = 65536

    fun encrypt(plaintext: String, priv32: ByteArray, pubHex: String): String {
        val nonce = ByteArray(32)
        SecureRandom().nextBytes(nonce)
        return encrypt(plaintext, conversationKey(priv32, pubHex), nonce)
    }

    fun decrypt(payload: String, priv32: ByteArray, pubHex: String): String {
        if (payload.contains("?iv=")) {
            return decryptLegacy(payload, priv32, pubHex)
        }
        return decrypt(payload, conversationKey(priv32, pubHex))
    }

    internal fun conversationKey(priv32: ByteArray, pubHex: String): ByteArray {
        val pub = pubHex.hexToByteArray()
        val encodings = when (pub.size) {
            32 -> listOf(byteArrayOf(0x02) + pub, byteArrayOf(0x03) + pub)
            33, 65 -> listOf(pub)
            else -> error("bad pub")
        }
        var last: Exception? = null
        for (enc in encodings) {
            try {
                val uncompressed = Secp256k1.pubKeyTweakMul(enc, priv32)
                val sharedX = uncompressed.copyOfRange(1, 33)
                return hkdfExtract(sharedX, salt)
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("conversation key failed")
    }

    internal fun encrypt(plaintext: String, conversationKey: ByteArray, nonce: ByteArray): String {
        val (chachaKey, chachaNonce, hmacKey) = messageKeys(conversationKey, nonce)
        val padded = pad(plaintext)
        val ciphertext = chacha20(chachaKey, chachaNonce, padded)
        val mac = hmacAad(hmacKey, ciphertext, nonce)
        val packed = ByteArray(1 + nonce.size + ciphertext.size + mac.size)
        packed[0] = 2
        nonce.copyInto(packed, 1)
        ciphertext.copyInto(packed, 1 + nonce.size)
        mac.copyInto(packed, 1 + nonce.size + ciphertext.size)
        return Base64.getEncoder().encodeToString(packed)
    }

    internal fun decrypt(payload: String, conversationKey: ByteArray): String {
        if (payload.isEmpty() || payload[0] == '#') error("unknown version")
        if (payload.length < 132) error("invalid payload size")
        val data = Base64.getDecoder().decode(payload)
        if (data.size < 99) error("invalid data size")
        if (data[0] != 2.toByte()) error("unknown version")
        val nonce = data.copyOfRange(1, 33)
        val ciphertext = data.copyOfRange(33, data.size - 32)
        val mac = data.copyOfRange(data.size - 32, data.size)
        val (chachaKey, chachaNonce, hmacKey) = messageKeys(conversationKey, nonce)
        val calculated = hmacAad(hmacKey, ciphertext, nonce)
        if (!isEqualCt(calculated, mac)) error("invalid MAC")
        return unpad(chacha20(chachaKey, chachaNonce, ciphertext))
    }

    private fun messageKeys(conversationKey: ByteArray, nonce: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        require(conversationKey.size == 32) { "invalid conversation_key length" }
        require(nonce.size == 32) { "invalid nonce length" }
        val keys = hkdfExpand(conversationKey, nonce, 76)
        return Triple(
            keys.copyOfRange(0, 32),
            keys.copyOfRange(32, 44),
            keys.copyOfRange(44, 76),
        )
    }

    private fun pad(plaintext: String): ByteArray {
        val unpadded = plaintext.toByteArray(Charsets.UTF_8)
        val unpaddedLen = unpadded.size
        if (unpaddedLen < MIN_PLAINTEXT || unpaddedLen.toLong() > MAX_PLAINTEXT) {
            error("invalid plaintext length")
        }
        val prefix = if (unpaddedLen >= EXTENDED_PREFIX_THRESHOLD) {
            ByteBuffer.allocate(6).order(ByteOrder.BIG_ENDIAN).putShort(0).putInt(unpaddedLen).array()
        } else {
            ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(unpaddedLen.toShort()).array()
        }
        val paddedLen = calcPaddedLen(unpaddedLen)
        val out = ByteArray(prefix.size + paddedLen)
        prefix.copyInto(out)
        unpadded.copyInto(out, prefix.size)
        return out
    }

    private fun unpad(padded: ByteArray): String {
        if (padded.size < 2) error("invalid padding")
        val firstTwo = ((padded[0].toInt() and 0xFF) shl 8) or (padded[1].toInt() and 0xFF)
        val unpaddedLen: Int
        val prefixLen: Int
        if (firstTwo == 0) {
            if (padded.size < 6) error("invalid padding")
            unpaddedLen = ByteBuffer.wrap(padded, 2, 4).order(ByteOrder.BIG_ENDIAN).int
            if (unpaddedLen < EXTENDED_PREFIX_THRESHOLD) error("invalid padding")
            prefixLen = 6
        } else {
            unpaddedLen = firstTwo
            prefixLen = 2
        }
        if (unpaddedLen == 0) error("invalid padding")
        val end = prefixLen + unpaddedLen
        if (end > padded.size) error("invalid padding")
        val unpadded = padded.copyOfRange(prefixLen, end)
        if (unpadded.size != unpaddedLen) error("invalid padding")
        if (padded.size != prefixLen + calcPaddedLen(unpaddedLen)) error("invalid padding")
        return String(unpadded, Charsets.UTF_8)
    }

    internal fun calcPaddedLen(unpaddedLen: Int): Int {
        if (unpaddedLen <= 32) return 32
        val log = 31 - Integer.numberOfLeadingZeros(unpaddedLen - 1)
        val nextPower = 1 shl (log + 1)
        val chunk = if (nextPower <= 256) 32 else nextPower / 8
        return chunk * (((unpaddedLen - 1) / chunk) + 1)
    }

    private fun hkdfExtract(ikm: ByteArray, salt: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val out = ByteArray(length)
        var prev = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(prev)
            mac.update(info)
            mac.update(counter.toByte())
            prev = mac.doFinal()
            val copy = minOf(prev.size, length - offset)
            prev.copyInto(out, offset, 0, copy)
            offset += copy
            counter++
        }
        return out
    }

    private fun hmacAad(key: ByteArray, message: ByteArray, aad: ByteArray): ByteArray {
        require(aad.size == 32) { "AAD associated data must be 32 bytes" }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        mac.update(aad)
        mac.update(message)
        return mac.doFinal()
    }

    private fun chacha20(key: ByteArray, nonce: ByteArray, data: ByteArray): ByteArray {
        val engine = ChaCha7539Engine()
        engine.init(true, ParametersWithIV(KeyParameter(key), nonce))
        val out = ByteArray(data.size)
        engine.processBytes(data, 0, data.size, out, 0)
        return out
    }

    private fun isEqualCt(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }

    private fun decryptLegacy(payload: String, priv32: ByteArray, pubHex: String): String {
        val idx = payload.indexOf("?iv=")
        val cipherB64 = payload.substring(0, idx)
        val ivB64 = payload.substring(idx + 4)
        val cipherBytes = Base64.getDecoder().decode(cipherB64)
        val iv = Base64.getDecoder().decode(ivB64)
        val pub = pubHex.hexToByteArray()
        val pubEnc = when (pub.size) {
            32 -> byteArrayOf(0x02) + pub
            33, 65 -> pub
            else -> error("bad pub")
        }
        val key = Secp256k1.ecdh(priv32, pubEnc)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    }
}
