package org.dergigi.boris.nostr

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.dergigi.boris.data.SessionStore
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object OfflineSync {
    @Volatile
    private var app: Context? = null

    private val bound = AtomicBoolean(false)
    private val flushing = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "offline-sync").apply { isDaemon = true }
    }

    fun bind(context: Context) {
        val appCtx = context.applicationContext
        app = appCtx
        if (bound.compareAndSet(false, true)) {
            val cm = appCtx.getSystemService(ConnectivityManager::class.java)
            cm.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        flush()
                    }
                },
            )
        }
        flush()
    }

    fun hasNetwork(): Boolean {
        val ctx = app ?: return true
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun flush() {
        val ctx = app ?: return
        executor.execute { flushNow(ctx) }
    }

    private fun flushNow(ctx: Context) {
        if (!flushing.compareAndSet(false, true)) return
        try {
            if (!hasNetwork()) return
            val pending = OfflineOutbox.pending()
            if (pending.isEmpty()) return
            val remotes = remoteWriteRelays(ctx)
            if (remotes.isEmpty()) return
            for (event in pending) {
                val result = try {
                    RelayQuery.publish(remotes, event)
                } catch (_: Exception) {
                    continue
                }
                if (result.remoteOk) OfflineOutbox.remove(event.id)
            }
        } finally {
            flushing.set(false)
        }
    }

    private fun remoteWriteRelays(ctx: Context): List<String> {
        val session = SessionStore.load(ctx)
        val write = if (session != null) {
            try {
                RelayQuery.fetchRelayList(session.pubkeyHex).write
            } catch (_: Exception) {
                RelayList.FALLBACK
            }
        } else {
            RelayList.FALLBACK
        }
        return LocalRelays.remoteOnly(write).ifEmpty { RelayList.FALLBACK }
    }
}
