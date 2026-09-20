package io.github.danieltyukov.antibrainrot.core

import java.time.LocalDate

// Reads the day history for the Progress screen. Pure functions over
// LocalState.history so they can be unit tested without Android.
object Progress {
    const val HISTORY_DAYS = 90
    // A day counts towards the streak when the filter was on this long.
    const val STREAK_MIN_SECONDS = 3600

    data class Day(val date: LocalDate, val record: DayRecord)

    data class Summary(
        val focusSeconds: Int,
        val blocks: Int,
        val feedsClosed: Int,
        val passes: Int,
        val passMinutes: Int,
        val activeDays: Int,
    )

    fun prune(history: Map<String, DayRecord>, today: String): Map<String, DayRecord> {
        val cutoff = LocalDate.parse(today).minusDays(HISTORY_DAYS - 1L).toString()
        return history.filterKeys { it >= cutoff }
    }

    // The last `count` days ending today, oldest first, missing days empty.
    fun days(history: Map<String, DayRecord>, count: Int, today: LocalDate = LocalDate.now()): List<Day> =
        (count - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            Day(date, history[date.toString()] ?: DayRecord())
        }

    fun summary(days: List<Day>): Summary = Summary(
        focusSeconds = days.sumOf { it.record.focusSeconds },
        blocks = days.sumOf { it.record.blocks },
        feedsClosed = days.sumOf { it.record.feedsClosed },
        passes = days.sumOf { it.record.passes },
        passMinutes = days.sumOf { it.record.passMinutes },
        activeDays = days.count { it.record.focusSeconds >= STREAK_MIN_SECONDS },
    )

    // Consecutive qualifying days ending today, or ending yesterday while
    // today is still short of the threshold.
    fun streak(history: Map<String, DayRecord>, today: LocalDate = LocalDate.now(), min: Int = STREAK_MIN_SECONDS): Int {
        fun qualifies(d: LocalDate) = (history[d.toString()]?.focusSeconds ?: 0) >= min
        var day = if (qualifies(today)) today else today.minusDays(1)
        var n = 0
        while (qualifies(day)) {
            n += 1
            day = day.minusDays(1)
        }
        return n
    }

    fun bestStreak(history: Map<String, DayRecord>, min: Int = STREAK_MIN_SECONDS): Int {
        val dates = history.filterValues { it.focusSeconds >= min }.keys.map { LocalDate.parse(it) }.sorted()
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (d in dates) {
            run = if (previous != null && previous.plusDays(1) == d) run + 1 else 1
            if (run > best) best = run
            previous = d
        }
        return best
    }

    // Packages by block screens shown, most first.
    fun topApps(days: List<Day>, limit: Int = 5): List<Pair<String, Int>> {
        val totals = HashMap<String, Int>()
        for (d in days) for ((pkg, n) in d.record.byApp) totals[pkg] = (totals[pkg] ?: 0) + n
        return totals.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }
    }

    fun focusText(seconds: Int): String {
        val minutes = seconds / 60
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}
