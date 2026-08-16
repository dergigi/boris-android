package org.dergigi.boris.nostr

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Observed per-relay reliability from real traffic. Conservative like Wisp:
 * a relay is only skipped (cooldown) after repeated consecutive failures, and
 * cooldowns expire so every relay gets second chances.
 */
object RelayHealth {
    data class Stats(
        val successes: Int = 0,
        val failures: Int = 0,
        val consecutiveFailures: Int = 0,
        val lastOkAt: Long = 0,
        val lastFailAt: Long = 0,
        val lastLatencyMs: Long = -1,
        val eventsReceived: Long = 0,
    )

    const val COOLDOWN_FAILURES = 3
    const val COOLDOWN_MS = 30L * 60_000L

    /** Traffic newer than this counts as a live connection signal for the UI. */
    const val FRESH_MS = 2L * 60_000L

    private val stats = ConcurrentHashMap<String, Stats>()
    private val _version = MutableStateFlow(0)

    /** Bumped on every update so composables can observe changes. */
    val version: StateFlow<Int> = _version

    @Volatile
    private var file: File? = null

    private val diskExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "relay-health").apply { isDaemon = true }
    }

    @Volatile
    private var saveScheduled = false

    fun init(target: File) {
        file = target
        if (!target.exists()) return
        runCatching {
            val obj = JSONObject(target.readText())
            for (url in obj.keys()) {
                val s = obj.getJSONObject(url)
                stats[url] = Stats(
                    successes = s.optInt("ok"),
                    failures = s.optInt("fail"),
                    consecutiveFailures = s.optInt("streak"),
                    lastOkAt = s.optLong("okAt"),
                    lastFailAt = s.optLong("failAt"),
                    lastLatencyMs = s.optLong("latency", -1),
                    eventsReceived = s.optLong("events"),
                )
            }
        }
    }

    fun onConnectOk(url: String, latencyMs: Long, now: Long = System.currentTimeMillis()) {
        stats.compute(url) { _, old ->
            (old ?: Stats()).let {
                it.copy(
                    successes = it.successes + 1,
                    consecutiveFailures = 0,
                    lastOkAt = now,
                    lastLatencyMs = latencyMs,
                )
            }
        }
        changed()
    }

    fun onConnectFail(url: String, now: Long = System.currentTimeMillis()) {
        stats.compute(url) { _, old ->
            (old ?: Stats()).let {
                it.copy(
                    failures = it.failures + 1,
                    consecutiveFailures = it.consecutiveFailures + 1,
                    lastFailAt = now,
                )
            }
        }
        changed()
    }

    fun onEvents(url: String, count: Int) {
        if (count <= 0) return
        stats.compute(url) { _, old ->
            (old ?: Stats()).let { it.copy(eventsReceived = it.eventsReceived + count) }
        }
        changed()
    }

    /**
     * True while a relay should be skipped: repeated consecutive failures and
     * the most recent failure is younger than the cooldown window. Local relays
     * never cool down.
     */
    fun inCooldown(url: String, now: Long = System.currentTimeMillis()): Boolean {
        if (LocalRelays.isLocal(url)) return false
        val s = stats[url] ?: return false
        return s.consecutiveFailures >= COOLDOWN_FAILURES && now - s.lastFailAt < COOLDOWN_MS
    }

    fun stats(url: String): Stats? = stats[url]

    /** Recent successful traffic and no failures since: treat as connected. */
    fun isFresh(url: String, now: Long = System.currentTimeMillis()): Boolean {
        val s = stats[url] ?: return false
        return s.consecutiveFailures == 0 && s.lastOkAt > 0 && now - s.lastOkAt < FRESH_MS
    }

    private fun changed() {
        _version.value++
        scheduleSave()
    }

    private fun scheduleSave() {
        if (file == null || saveScheduled) return
        saveScheduled = true
        diskExecutor.execute {
            try {
                Thread.sleep(SAVE_DEBOUNCE_MS)
            } catch (_: InterruptedException) {
            }
            saveScheduled = false
            val target = file ?: return@execute
            runCatching {
                val obj = JSONObject()
                for ((url, s) in stats) {
                    obj.put(
                        url,
                        JSONObject()
                            .put("ok", s.successes)
                            .put("fail", s.failures)
                            .put("streak", s.consecutiveFailures)
                            .put("okAt", s.lastOkAt)
                            .put("failAt", s.lastFailAt)
                            .put("latency", s.lastLatencyMs)
                            .put("events", s.eventsReceived),
                    )
                }
                target.writeText(obj.toString())
            }
        }
    }

    private const val SAVE_DEBOUNCE_MS = 2_000L
}
