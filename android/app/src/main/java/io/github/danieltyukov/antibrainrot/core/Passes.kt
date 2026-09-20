package io.github.danieltyukov.antibrainrot.core

import java.time.LocalDate
import kotlin.math.min

// Sessions, cooldowns, usage metering and the day counters.
object Passes {
    fun dayKey(date: LocalDate = LocalDate.now()): String = date.toString()

    fun usedSeconds(state: LocalState, pkg: String, today: String = dayKey()): Int =
        if (state.usage.day == today) state.usage.seconds[pkg] ?: 0 else 0

    fun secondsLeft(state: LocalState, rule: Rule, pkg: String, today: String = dayKey()): Int =
        (rule.limitMinutes * 60 - usedSeconds(state, pkg, today)).coerceAtLeast(0)

    fun activePass(state: LocalState, key: String, now: Long = System.currentTimeMillis()): Long? =
        state.passes[key]?.takeIf { it > now }

    fun cooldownUntil(state: LocalState, key: String, now: Long = System.currentTimeMillis()): Long? =
        state.cooldowns[key]?.takeIf { it > now }

    // A timed app or site with time left today and no cooldown running.
    fun canPass(state: LocalState, s: Settings, key: String, now: Long = System.currentTimeMillis(), today: String = dayKey()): Boolean {
        val rule = Keys.ruleFor(s, key) ?: return false
        if (rule.mode != "timer") return false
        if (cooldownUntil(state, key, now) != null) return false
        return s.apps.passMinutes > 0 && secondsLeft(state, rule, key, today) > 0
    }

    // One session: the configured length, or what is left of the day.
    fun sessionSeconds(state: LocalState, s: Settings, key: String, today: String = dayKey()): Int {
        val rule = Keys.ruleFor(s, key) ?: return 0
        return min(s.apps.passMinutes * 60, secondsLeft(state, rule, key, today))
    }

    // Every counter goes through here: today's record in the history is
    // changed and the history is trimmed.
    private fun record(state: LocalState, today: String, change: (DayRecord) -> DayRecord): LocalState {
        val rec = change(state.history[today] ?: DayRecord())
        return state.copy(history = Progress.prune(state.history + (today to rec), today))
    }

    // A granted session: the pass and the counters.
    fun grant(state: LocalState, s: Settings, key: String, now: Long = System.currentTimeMillis(), today: String = dayKey()): LocalState {
        val seconds = sessionSeconds(state, s, key, today)
        val next = state.copy(passes = state.passes + (key to now + seconds * 1000L))
        return record(next, today) { it.copy(passes = it.passes + 1, passMinutes = it.passMinutes + (seconds + 59) / 60) }
    }

    // Time spent in front of a timed app or site.
    fun recordUsage(state: LocalState, pkg: String, seconds: Int, today: String = dayKey()): LocalState {
        if (seconds <= 0) return state
        val current = if (state.usage.day == today) state.usage.seconds else emptyMap()
        val usage = Usage(today, current + (pkg to (current[pkg] ?: 0) + seconds))
        return record(state.copy(usage = usage), today) { it.copy(usage = it.usage + (pkg to (it.usage[pkg] ?: 0) + seconds)) }
    }

    fun recordBlock(state: LocalState, today: String = dayKey(), pkg: String? = null): LocalState =
        record(state, today) {
            val byApp = if (pkg == null) it.byApp else it.byApp + (pkg to (it.byApp[pkg] ?: 0) + 1)
            it.copy(blocks = it.blocks + 1, byApp = byApp)
        }

    fun recordFocus(state: LocalState, seconds: Int, today: String = dayKey()): LocalState =
        if (seconds <= 0) state else record(state, today) { it.copy(focusSeconds = it.focusSeconds + seconds) }

    // Drops expired sessions and starts cooldowns for them.
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
