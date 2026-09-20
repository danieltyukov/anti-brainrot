package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassesTest {
    private val apps = Apps(mode = "pause", passMinutes = 5, dailyBudgetMinutes = 30, cooldownMinutes = 15)

    @Test fun budgetAndGrant() {
        val today = "2026-09-20"
        var state = LocalState()
        assertEquals(30, Passes.budgetLeft(state, apps, today))
        assertTrue(Passes.canPass(state, apps, "a", 1000, today))
        state = Passes.grant(state, apps, "a", 1000, today)
        assertEquals(25, Passes.budgetLeft(state, apps, today))
        assertEquals(1000 + 5 * 60_000L, state.passes["a"])
        assertEquals(1, state.stats.passes)
        assertNotNull(Passes.activePass(state, "a", 2000))
        assertNull(Passes.activePass(state, "a", 1000 + 5 * 60_000L + 1))
        val yesterday = state.copy(budget = state.budget.copy(day = "2026-09-19"))
        assertEquals(30, Passes.budgetLeft(yesterday, apps, today))
        assertFalse(Passes.canPass(state, apps.copy(mode = "block"), "a", 2000, today))
        assertFalse(Passes.canPass(state, apps.copy(dailyBudgetMinutes = 0), "b", 2000, today))
    }

    @Test fun sweepStartsCooldown() {
        val expires = 10_000L
        val state = LocalState(passes = mapOf("a" to expires, "b" to 50_000L))
        val swept = Passes.sweep(state, apps, now = 20_000L)
        assertEquals(mapOf("b" to 50_000L), swept.passes)
        assertEquals(expires + 15 * 60_000L, swept.cooldowns["a"])
        assertFalse(Passes.canPass(swept, apps, "a", 20_000L, "2026-09-20"))
        val later = Passes.sweep(swept, apps, now = expires + 15 * 60_000L + 1)
        assertNull(later.cooldowns["a"])
    }

    @Test fun recordBlockRollsOverDays() {
        val s1 = Passes.recordBlock(LocalState(), "2026-09-20")
        val s2 = Passes.recordBlock(s1, "2026-09-20")
        assertEquals(2, s2.stats.blocks)
        val s3 = Passes.recordBlock(s2, "2026-09-21")
        assertEquals(1, s3.stats.blocks)
        val s4 = Passes.recordFeedClosed(s3, "2026-09-21")
        assertEquals(1, s4.stats.feedsClosed)
        assertEquals(1, s4.stats.blocks)
    }
}
