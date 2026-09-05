package org.dergigi.boris.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

internal object PublicHttpDestination {
    fun toHttpUrl(raw: String): HttpUrl? {
        val url = raw.toHttpUrlOrNull() ?: return null
        if (url.scheme != "http" && url.scheme != "https") return null
        return url.takeIf { hostResolvesPublicly(it.host) }
    }

    fun hostResolvesPublicly(host: String): Boolean {
        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull() ?: return false
        return addresses.isNotEmpty() && addresses.all(::isPublicAddress)
    }

    fun isPublicAddress(address: InetAddress): Boolean {
        if (
            address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }
        return when (address) {
            is Inet4Address -> !isReservedIpv4(address.address)
            is Inet6Address -> !isReservedIpv6(address.address)
            else -> false
        }
    }

    private fun isReservedIpv4(bytes: ByteArray): Boolean {
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        val third = bytes[2].toInt() and 0xff
        return when {
            first == 0 -> true
            first == 10 -> true
            first == 100 && second in 64..127 -> true
            first == 127 -> true
            first == 169 && second == 254 -> true
            first == 172 && second in 16..31 -> true
            first == 192 && second == 0 -> true
            first == 192 && second == 88 && third == 99 -> true
            first == 192 && second == 168 -> true
            first == 198 && second in 18..19 -> true
            first == 198 && second == 51 && third == 100 -> true
            first == 203 && second == 0 && third == 113 -> true
            first >= 224 -> true
            else -> false
        }
    }

    private fun isReservedIpv6(bytes: ByteArray): Boolean {
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        val third = bytes[2].toInt() and 0xff
        val fourth = bytes[3].toInt() and 0xff
        return when {
            (first and 0xfe) == 0xfc -> true
            first == 0x20 && second == 0x01 && third == 0x0d && fourth == 0xb8 -> true
            else -> false
        }
    }
}
