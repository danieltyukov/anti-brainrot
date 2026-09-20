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

    // A granted pass: new state with the pass, the budget debit and the counter.
    fun grant(state: LocalState, apps: Apps, key: String, now: Long = System.currentTimeMillis(), today: String = dayKey()): LocalState {
        val used = if (state.budget.day == today) state.budget.usedMinutes else 0
        val stats = if (state.stats.day == today) state.stats else Stats(day = today)
        return state.copy(
            passes = state.passes + (key to now + apps.passMinutes * 60_000L),
            budget = Budget(day = today, usedMinutes = used + apps.passMinutes),
            stats = stats.copy(passes = stats.passes + 1),
        )
    }

    fun recordBlock(state: LocalState, today: String = dayKey()): LocalState {
        val stats = if (state.stats.day == today) state.stats else Stats(day = today)
        return state.copy(stats = stats.copy(blocks = stats.blocks + 1))
    }

    fun recordFeedClosed(state: LocalState, today: String = dayKey()): LocalState {
        val stats = if (state.stats.day == today) state.stats else Stats(day = today)
        return state.copy(stats = stats.copy(feedsClosed = stats.feedsClosed + 1))
    }

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
