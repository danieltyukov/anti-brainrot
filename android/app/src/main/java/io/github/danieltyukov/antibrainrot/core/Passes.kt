package io.github.danieltyukov.antibrainrot.core

import java.time.LocalDate

// Daily limits, the optional pause, usage metering and the day counters.
object Passes {
    // How long a timed app or site may be out of front before its pause
    // shows again.
    const val KEEP_MS = 60_000L

    fun dayKey(date: LocalDate = LocalDate.now()): String = date.toString()

    fun usedSeconds(state: LocalState, pkg: String, today: String = dayKey()): Int =
        if (state.usage.day == today) state.usage.seconds[pkg] ?: 0 else 0

    fun secondsLeft(state: LocalState, rule: Rule, pkg: String, today: String = dayKey()): Int =
        (rule.limitMinutes * 60 - usedSeconds(state, pkg, today)).coerceAtLeast(0)

    fun activePass(state: LocalState, key: String, now: Long = System.currentTimeMillis()): Long? =
        state.passes[key]?.takeIf { it > now }

    // A timed app or site with time left today.
    fun canPass(state: LocalState, s: Settings, key: String, today: String = dayKey()): Boolean {
        val rule = Keys.ruleFor(s, key) ?: return false
        return rule.mode == "timer" && secondsLeft(state, rule, key, today) > 0
    }

    // Every counter goes through here: today's record in the history is
    // changed and the history is trimmed.
    private fun record(state: LocalState, today: String, change: (DayRecord) -> DayRecord): LocalState {
        val rec = change(state.history[today] ?: DayRecord())
        return state.copy(history = Progress.prune(state.history + (today to rec), today))
    }

    // The pause was waited out: the app or site opens, and stays open while
    // it is in front.
    fun grant(state: LocalState, key: String, now: Long = System.currentTimeMillis(), today: String = dayKey()): LocalState {
        val next = state.copy(passes = state.passes + (key to now + KEEP_MS))
        return record(next, today) { it.copy(passes = it.passes + 1) }
    }

    // Time spent in front of a timed app or site; a running pass is kept alive.
    fun recordUsage(state: LocalState, key: String, seconds: Int, now: Long = System.currentTimeMillis(), today: String = dayKey()): LocalState {
        if (seconds <= 0) return state
        val current = if (state.usage.day == today) state.usage.seconds else emptyMap()
        val usage = Usage(today, current + (key to (current[key] ?: 0) + seconds))
        val passes = if (activePass(state, key, now) != null) state.passes + (key to now + KEEP_MS) else state.passes
        return record(state.copy(usage = usage, passes = passes), today) { it.copy(usage = it.usage + (key to (it.usage[key] ?: 0) + seconds)) }
    }

    fun recordBlock(state: LocalState, today: String = dayKey(), pkg: String? = null): LocalState =
        record(state, today) {
            val byApp = if (pkg == null) it.byApp else it.byApp + (pkg to (it.byApp[pkg] ?: 0) + 1)
            it.copy(blocks = it.blocks + 1, byApp = byApp)
        }

    fun recordFocus(state: LocalState, seconds: Int, today: String = dayKey()): LocalState =
        if (seconds <= 0) state else record(state, today) { it.copy(focusSeconds = it.focusSeconds + seconds) }

    // Drops passes whose minute away has run out.
    fun sweep(state: LocalState, now: Long = System.currentTimeMillis()): LocalState =
        state.copy(passes = state.passes.filterValues { it > now })
}
