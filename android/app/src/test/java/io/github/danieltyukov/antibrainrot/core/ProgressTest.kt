package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    private val today = LocalDate.parse("2026-09-20")

    private fun history(vararg focusHoursByBack: Pair<Int, Int>): Map<String, DayRecord> =
        focusHoursByBack.associate { (back, hours) -> today.minusDays(back.toLong()).toString() to DayRecord(focusSeconds = hours * 3600) }

    @Test fun countersLandInHistory() {
        val apps = Settings(apps = Apps(rules = mapOf("com.android.chrome" to Rule("timer", 30)), passMinutes = 5))
        var s = Passes.recordBlock(LocalState(), "2026-09-20", "com.android.chrome")
        s = Passes.recordBlock(s, "2026-09-20", "com.android.chrome")
        s = Passes.recordUsage(s, "com.android.chrome", 90, "2026-09-20")
        s = Passes.recordFocus(s, 900, "2026-09-20")
        s = Passes.grant(s, apps, "com.android.chrome", 1000, "2026-09-20")
        val rec = s.history.getValue("2026-09-20")
        assertEquals(2, rec.blocks)
        assertEquals(90, rec.usage["com.android.chrome"])
        assertEquals(900, rec.focusSeconds)
        assertEquals(1, rec.passes)
        assertEquals(5, rec.passMinutes)
        assertEquals(2, rec.byApp["com.android.chrome"])
        assertEquals(90, Progress.summary(Progress.days(s.history, 7, today)).usageSeconds)
    }

    @Test fun historyIsPrunedToNinetyDays() {
        val old = today.minusDays(90).toString()
        val kept = today.minusDays(89).toString()
        var s = LocalState(history = mapOf(old to DayRecord(blocks = 1), kept to DayRecord(blocks = 1)))
        s = Passes.recordBlock(s, "2026-09-20")
        assertNull(s.history[old])
        assertEquals(1, s.history.getValue(kept).blocks)
        assertEquals(2, s.history.size)
    }

    @Test fun daysFillGapsOldestFirst() {
        val h = mapOf("2026-09-20" to DayRecord(blocks = 3), "2026-09-18" to DayRecord(blocks = 1))
        val days = Progress.days(h, 3, today)
        assertEquals(listOf("2026-09-18", "2026-09-19", "2026-09-20"), days.map { it.date.toString() })
        assertEquals(listOf(1, 0, 3), days.map { it.record.blocks })
        val sum = Progress.summary(days)
        assertEquals(4, sum.blocks)
        assertEquals(0, sum.activeDays)
    }

    @Test fun streakCountsBackFromTodayOrYesterday() {
        // Today has only 10 minutes so far; the three days before qualify.
        val h = history(0 to 0, 1 to 2, 2 to 1, 3 to 5, 5 to 8) + ("2026-09-20" to DayRecord(focusSeconds = 600))
        assertEquals(3, Progress.streak(h, today))
        assertEquals(3, Progress.bestStreak(h))
        val withToday = h + ("2026-09-20" to DayRecord(focusSeconds = 3600))
        assertEquals(4, Progress.streak(withToday, today))
        assertEquals(0, Progress.streak(emptyMap(), today))
    }

    @Test fun topAppsAndFocusText() {
        val h = mapOf(
            "2026-09-20" to DayRecord(byApp = mapOf("a" to 2, "b" to 5), usage = mapOf("a" to 600)),
            "2026-09-19" to DayRecord(byApp = mapOf("a" to 4), usage = mapOf("b" to 900, "c" to 0)),
        )
        assertEquals(listOf("a" to 6, "b" to 5), Progress.topApps(Progress.days(h, 7, today)))
        assertEquals(listOf("b" to 900, "a" to 600), Progress.topUsage(Progress.days(h, 7, today)))
        assertEquals("0m", Progress.focusText(0))
        assertEquals("45m", Progress.focusText(45 * 60))
        assertEquals("2h", Progress.focusText(7200))
        assertEquals("2h 15m", Progress.focusText(8100))
    }
}

class MigrationTest {
    @Test fun oldStatsBecomeHistory() {
        val old = """{"passes":{},"cooldowns":{},"budget":{"day":"2026-09-20","usedMinutes":5},"stats":{"day":"2026-09-20","blocks":2,"passes":1,"feedsClosed":7}}"""
        val migrated = LocalState.decode(old)
        assertEquals(2, migrated.history.getValue("2026-09-20").blocks)
        assertEquals(1, migrated.history.getValue("2026-09-20").passes)
        // Already migrated state is left alone.
        val again = LocalState.decode(migrated.encode())
        assertEquals(migrated, again)
    }
}
