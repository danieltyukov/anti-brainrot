package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassesTest {
    private val today = "2026-09-20"
    private val apps = Apps(rules = mapOf("a" to Rule("timer", 30), "b" to Rule("block")), passMinutes = 5, cooldownMinutes = 15)
    private val s = Settings(apps = apps, sites = Sites(rules = mapOf("reddit.com" to Rule("timer", 10))))

    @Test fun sessionsAndLimit() {
        var state = LocalState()
        assertEquals(1800, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", today))
        assertTrue(Passes.canPass(state, s, "a", 1000, today))
        assertFalse(Passes.canPass(state, s, "b", 1000, today))
        assertFalse(Passes.canPass(state, s, "c", 1000, today))
        assertEquals(300, Passes.sessionSeconds(state, s, "a", today))
        state = Passes.grant(state, s, "a", 1000, today)
        assertEquals(1000 + 5 * 60_000L, state.passes["a"])
        assertEquals(1, state.history.getValue(today).passes)
        assertEquals(5, state.history.getValue(today).passMinutes)
        assertNotNull(Passes.activePass(state, "a", 2000))
        assertNull(Passes.activePass(state, "a", 1000 + 5 * 60_000L + 1))
        // Usage counts only what was in front; 28 minutes used leaves a short session.
        state = Passes.recordUsage(state, "a", 28 * 60, today)
        assertEquals(120, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", today))
        assertEquals(120, Passes.sessionSeconds(state, s, "a", today))
        assertEquals(28 * 60, state.history.getValue(today).usage["a"])
        state = Passes.recordUsage(state, "a", 200, today)
        assertEquals(0, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", today))
        assertFalse(Passes.canPass(state, s, "a", 2000, today))
        // A new day starts fresh.
        assertEquals(1800, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", "2026-09-21"))
        assertEquals(0, Passes.usedSeconds(state, "a", "2026-09-21"))
    }

    @Test fun siteKeysWorkLikeApps() {
        val key = Keys.site("reddit.com")
        assertTrue(Passes.canPass(LocalState(), s, key, 1000, today))
        assertEquals(300, Passes.sessionSeconds(LocalState(), s, key, today))
        val used = Passes.recordUsage(LocalState(), key, 9 * 60 + 30, today)
        assertEquals(30, Passes.sessionSeconds(used, s, key, today))
        assertEquals("reddit.com", Keys.siteRuleHost(s, "old.reddit.com"))
        assertEquals("reddit.com", Keys.siteRuleHost(s, "reddit.com"))
        assertNull(Keys.siteRuleHost(s, "redditstatic.com"))
        assertEquals(listOf<String>(), Keys.blockedHosts(s))
        assertEquals(Rule("timer", 10), Keys.ruleFor(s, key))
        assertNull(Keys.ruleFor(s, "reddit.com"))
    }

    @Test fun sweepStartsCooldown() {
        val expires = 10_000L
        val state = LocalState(passes = mapOf("a" to expires, "b" to 50_000L))
        val swept = Passes.sweep(state, apps, now = 20_000L)
        assertEquals(mapOf("b" to 50_000L), swept.passes)
        assertEquals(expires + 15 * 60_000L, swept.cooldowns["a"])
        assertFalse(Passes.canPass(swept, s, "a", 20_000L, today))
        val later = Passes.sweep(swept, apps, now = expires + 15 * 60_000L + 1)
        assertNull(later.cooldowns["a"])
    }

    @Test fun recordBlockRollsOverDays() {
        val s1 = Passes.recordBlock(LocalState(), today, "a")
        val s2 = Passes.recordBlock(s1, today, "a")
        assertEquals(2, s2.history.getValue(today).blocks)
        assertEquals(2, s2.history.getValue(today).byApp["a"])
        val s3 = Passes.recordBlock(s2, "2026-09-21")
        assertEquals(1, s3.history.getValue("2026-09-21").blocks)
        assertEquals(2, s3.history.getValue(today).blocks)
    }
}
