package io.github.danieltyukov.antibrainrot.core

import java.time.LocalDate

object Passes {
    fun dayKey(date: LocalDate = LocalDate.now()): String = date.toString()

    fun budgetLeft(state: LocalState, apps: Apps, today: String = dayKey()): Int {
        val used = if (state.budget.day == today) state.budget.usedMinutes else 0
        return (apps.dailyBudgetMinutes - used).coerceAtLeast(0)
    }

    fun activePass(state: LocalState, key: String, now: Long = System.currentTimeMillis()): Long? =
        state.passes[key]?.takeIf { it > now }

    fun cooldownUntil(state: LocalState, key: String, now: Long = System.currentTimeMillis()): Long? =
        state.cooldowns[key]?.takeIf { it > now }

    fun canPass(state: LocalState, apps: Apps, key: String, now: Long = System.currentTimeMillis(), today: String = dayKey()): Boolean {
        if (apps.mode != "pause") return false
        if (cooldownUntil(state, key, now) != null) return false
        return apps.passMinutes > 0 && budgetLeft(state, apps, today) >= apps.passMinutes
    }

    // Every counter goes through here: today's record in the history is
    // changed, the history is trimmed, and the Stats mirror is refreshed.
    private fun record(state: LocalState, today: String, change: (DayRecord) -> DayRecord): LocalState {
        val rec = change(state.history[today] ?: DayRecord())
        val history = Progress.prune(state.history + (today to rec), today)
        return state.copy(
            history = history,
            stats = Stats(day = today, blocks = rec.blocks, passes = rec.passes, feedsClosed = rec.feedsClosed),
        )
    }

    // A granted pass: new state with the pass, the budget debit and the counters.
    fun grant(state: LocalState, apps: Apps, key: String, now: Long = System.currentTimeMillis(), today: String = dayKey()): LocalState {
        val used = if (state.budget.day == today) state.budget.usedMinutes else 0
        val next = state.copy(
            passes = state.passes + (key to now + apps.passMinutes * 60_000L),
            budget = Budget(day = today, usedMinutes = used + apps.passMinutes),
        )
        return record(next, today) { it.copy(passes = it.passes + 1, passMinutes = it.passMinutes + apps.passMinutes) }
    }

    fun recordBlock(state: LocalState, today: String = dayKey(), pkg: String? = null): LocalState =
        record(state, today) {
            val byApp = if (pkg == null) it.byApp else it.byApp + (pkg to (it.byApp[pkg] ?: 0) + 1)
            it.copy(blocks = it.blocks + 1, byApp = byApp)
        }

    fun recordFeedClosed(state: LocalState, today: String = dayKey()): LocalState =
        record(state, today) { it.copy(feedsClosed = it.feedsClosed + 1) }

    fun recordFocus(state: LocalState, seconds: Int, today: String = dayKey()): LocalState =
        if (seconds <= 0) state else record(state, today) { it.copy(focusSeconds = it.focusSeconds + seconds) }

    // Drops expired passes and starts cooldowns for them.
    fun sweep(state: LocalState, apps: Apps, now: Long = System.currentTimeMillis()): LocalState {
        val kept = state.passes.filterValues { it > now }
        val cool = state.cooldowns.filterValues { it > now }.toMutableMap()
        val cooldownMs = apps.cooldownMinutes * 60_000L
        for ((key, expiresAt) in state.passes) {
            if (expiresAt <= now && cooldownMs > 0 && expiresAt + cooldownMs > now) {
                cool[key] = maxOf(cool[key] ?: 0L, expiresAt + cooldownMs)
            }
        }
        return state.copy(passes = kept, cooldowns = cool)
    }
}
