package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassesTest {
    private val today = "2026-09-20"
    private val apps = Apps(rules = mapOf("a" to Rule("timer", 30), "b" to Rule("block")))
    private val s = Settings(apps = apps, sites = Sites(rules = mapOf("reddit.com" to Rule("timer", 10))))

    @Test fun limitsAndUsage() {
        var state = LocalState()
        assertEquals(1800, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", today))
        assertTrue(Passes.canPass(state, s, "a", today))
        assertFalse(Passes.canPass(state, s, "b", today))
        assertFalse(Passes.canPass(state, s, "c", today))
        // Usage counts only what was in front.
        state = Passes.recordUsage(state, "a", 28 * 60, 1000, today)
        assertEquals(120, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", today))
        assertEquals(28 * 60, state.history.getValue(today).usage["a"])
        state = Passes.recordUsage(state, "a", 200, 2000, today)
        assertEquals(0, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", today))
        assertFalse(Passes.canPass(state, s, "a", today))
        // A new day starts fresh.
        assertEquals(1800, Passes.secondsLeft(state, apps.rules.getValue("a"), "a", "2026-09-21"))
        assertEquals(0, Passes.usedSeconds(state, "a", "2026-09-21"))
    }

    @Test fun pauseIsKeptAliveWhileInFront() {
        var state = Passes.grant(LocalState(), "a", 1000, today)
        assertEquals(1, state.history.getValue(today).passes)
        assertNotNull(Passes.activePass(state, "a", 1000 + Passes.KEEP_MS - 1))
        assertNull(Passes.activePass(state, "a", 1000 + Passes.KEEP_MS + 1))
        // Time in front pushes the pause further away.
        state = Passes.recordUsage(state, "a", 15, 50_000, today)
        assertNotNull(Passes.activePass(state, "a", 100_000))
        assertNull(Passes.activePass(state, "a", 50_000 + Passes.KEEP_MS + 1))
        // Usage for a key without a pass does not create one.
        val other = Passes.recordUsage(LocalState(), "a", 15, 1000, today)
        assertNull(Passes.activePass(other, "a", 1001))
        val swept = Passes.sweep(state, now = 200_000)
        assertTrue(swept.passes.isEmpty())
    }

    @Test fun siteKeysWorkLikeApps() {
        val key = Keys.site("reddit.com")
        assertTrue(Passes.canPass(LocalState(), s, key, today))
        val used = Passes.recordUsage(LocalState(), key, 9 * 60 + 30, 1000, today)
        assertEquals(30, Passes.secondsLeft(used, Rule("timer", 10), key, today))
        assertEquals("reddit.com", Keys.siteRuleHost(s, "old.reddit.com"))
        assertEquals("reddit.com", Keys.siteRuleHost(s, "reddit.com"))
        assertNull(Keys.siteRuleHost(s, "redditstatic.com"))
        assertEquals(listOf<String>(), Keys.blockedHosts(s))
        assertEquals(Rule("timer", 10), Keys.ruleFor(s, key))
        assertNull(Keys.ruleFor(s, "reddit.com"))
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
