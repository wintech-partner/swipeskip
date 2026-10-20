package io.github.wintech_partner.swipeskip.diagnostics

import io.github.wintech_partner.swipeskip.core.model.SessionState

/** Keeps the last few state changes in memory for the diagnostics screen. Never written to disk. */
class TransitionLog(private val capacity: Int = 20) {
    data class Entry(val atMillis: Long, val state: SessionState)

    private val entries = ArrayDeque<Entry>()

    @Synchronized
    fun record(state: SessionState, atMillis: Long = System.currentTimeMillis()) {
        if (entries.lastOrNull()?.state == state) return
        entries.addLast(Entry(atMillis, state))
        while (entries.size > capacity) entries.removeFirst()
    }

    @Synchronized
    fun last(): Entry? = entries.lastOrNull()

    @Synchronized
    fun all(): List<Entry> = entries.toList()
}
