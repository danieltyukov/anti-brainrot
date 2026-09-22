package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RulesTest {
    private val tiktok = "com.zhiliaoapp.musically"
    private val base = Settings(
        apps = Apps(rules = mapOf(tiktok to Rule("timer", 30), "com.reddit.frontpage" to Rule("block"))),
        schedule = Schedule(enabled = true),
        sites = Sites(adult = true, rules = mapOf("reddit.com" to Rule("block"), "x.com" to Rule("timer", 15))),
    )

    private fun loosens(change: (Settings) -> Settings) = Rules.isLoosening(base, change(base))
    private fun withRule(s: Settings, pkg: String, rule: Rule?) =
        s.copy(apps = s.apps.copy(rules = if (rule == null) s.apps.rules - pkg else s.apps.rules + (pkg to rule)))

    @Test fun unchangedIsNotLoosening() = assertFalse(loosens { it })

    @Test fun featureOffLoosens() {
        assertTrue(loosens { it.copy(sites = it.sites.copy(adult = false)) })
        assertTrue(loosens { it.copy(schedule = it.schedule.copy(enabled = false)) })
        // Safe search and Restricted Mode follow the adult filter.
        assertTrue(loosens { it.copy(sites = it.sites.copy(safeSearch = false)) })
        assertTrue(loosens { it.copy(sites = it.sites.copy(restrictYouTube = false)) })
        val adultOff = base.copy(sites = base.sites.copy(adult = false))
        assertFalse(Rules.isLoosening(adultOff, adultOff.copy(sites = adultOff.sites.copy(safeSearch = false, restrictYouTube = false))))
        assertFalse(Rules.isLoosening(adultOff.copy(sites = adultOff.sites.copy(safeSearch = false)), adultOff))
    }

    @Test fun keywords() {
        val words = base.copy(sites = base.sites.copy(keywords = listOf("feet", "nudes")))
        assertTrue(Rules.isLoosening(words, words.copy(sites = words.sites.copy(keywords = listOf("feet")))))
        assertFalse(Rules.isLoosening(words, words.copy(sites = words.sites.copy(keywords = listOf("feet", "nudes", "more")))))
        assertFalse(Rules.isLoosening(words, words.copy(sites = words.sites.copy(keywords = listOf("Feet", "NUDES")))))
        assertFalse(Rules.isLoosening(base, words))
        assertEquals(listOf("feet", "nudes"), Settings.normalize(Settings(sites = Sites(keywords = listOf(" Feet ", "", "nudes", "feet")))).sites.keywords)
    }

    @Test fun appRules() {
        assertTrue(loosens { withRule(it, tiktok, null) })
        assertTrue(loosens { withRule(it, "com.reddit.frontpage", Rule("timer", 30)) })
        assertTrue(loosens { withRule(it, tiktok, Rule("timer", 60)) })
        assertFalse(loosens { withRule(it, tiktok, Rule("timer", 15)) })
        assertFalse(loosens { withRule(it, tiktok, Rule("block")) })
        assertFalse(loosens { withRule(it, "com.instagram.android", Rule("timer", 180)) })
    }

    @Test fun siteRules() {
        fun withSite(s: Settings, host: String, rule: Rule?) = s.copy(sites = s.sites.copy(rules = if (rule == null) s.sites.rules - host else s.sites.rules + (host to rule)))
        assertTrue(loosens { withSite(it, "reddit.com", null) })
        assertTrue(loosens { withSite(it, "reddit.com", Rule("timer", 30)) })
        assertTrue(loosens { withSite(it, "x.com", Rule("timer", 30)) })
        assertFalse(loosens { withSite(it, "x.com", Rule("block")) })
        assertFalse(loosens { withSite(it, "tiktok.com", Rule("timer", 5)) })
        assertTrue(loosens { it.copy(sites = it.sites.copy(allowed = listOf("reddit.com"))) })
    }

    @Test fun oldDistractingSitesBecomeRules() {
        val old = """{"sites":{"adult":false,"distracting":true,"presets":["reddit","x"],"custom":["example.com"],"allowed":[]}}"""
        val s = Settings.decode(old)
        assertEquals(mapOf("reddit.com" to Rule("block", 30), "x.com" to Rule("block", 30), "example.com" to Rule("block", 30)), s.sites.rules)
        val off = old.replace("\"distracting\":true", "\"distracting\":false")
        assertEquals(emptyMap<String, Rule>(), Settings.decode(off).sites.rules)
    }

    @Test fun pauseSettings() {
        val paused = base.copy(apps = base.apps.copy(pauseEnabled = true))
        assertTrue(Rules.isLoosening(paused, base))
        assertFalse(Rules.isLoosening(base, paused))
        assertTrue(Rules.isLoosening(paused, paused.copy(apps = paused.apps.copy(pauseSeconds = 5))))
        assertFalse(Rules.isLoosening(paused, paused.copy(apps = paused.apps.copy(pauseSeconds = 60))))
        assertTrue(Rules.isLoosening(paused, paused.copy(apps = paused.apps.copy(intention = false))))
        // With the pause off its length and the intention do not matter.
        assertFalse(loosens { it.copy(apps = it.apps.copy(pauseSeconds = 5, intention = false)) })
        val guarded = base.copy(preventUninstall = true, strictMode = true)
        assertTrue(Rules.isLoosening(guarded, guarded.copy(preventUninstall = false)))
        assertFalse(Rules.isLoosening(base, guarded))
        val installsOn = base.copy(apps = base.apps.copy(blockInstalls = true))
        assertTrue(Rules.isLoosening(installsOn, base))
        assertFalse(Rules.isLoosening(base, installsOn))
    }

    @Test fun installersAreBlockedAsOne() {
        val on = Settings(apps = Apps(blockInstalls = true, rules = mapOf("com.android.vending" to Rule("timer", 15))))
        assertEquals(Rule("block"), Keys.ruleFor(on, "com.google.android.packageinstaller"))
        assertEquals(Rule("block"), Keys.ruleFor(on, "org.fdroid.fdroid"))
        // An explicit rule for a store still wins.
        assertEquals(Rule("timer", 15), Keys.ruleFor(on, "com.android.vending"))
        assertEquals(null, Keys.ruleFor(on, "com.example.other"))
        val off = on.copy(apps = on.apps.copy(blockInstalls = false))
        assertEquals(null, Keys.ruleFor(off, "com.google.android.packageinstaller"))
        assertTrue(Keys.isInstaller("com.android.vending"))
    }

    @Test fun scheduleWindow() {
        assertTrue(loosens { it.copy(schedule = it.schedule.copy(days = listOf(1, 2))) })
        assertFalse(loosens { it.copy(schedule = it.schedule.copy(days = (1..7).toList())) })
        assertTrue(loosens { it.copy(schedule = it.schedule.copy(start = "10:00")) })
        assertTrue(loosens { it.copy(schedule = it.schedule.copy(end = "16:00")) })
        assertFalse(loosens { it.copy(schedule = it.schedule.copy(start = "08:00", end = "18:00")) })
    }

    @Test fun lockAndDelay() {
        assertTrue(loosens { it.copy(focus = it.focus.copy(unlockDelaySec = 60)) })
        assertFalse(loosens { it.copy(focus = it.focus.copy(unlockDelaySec = 3600)) })
        val locked = base.copy(focus = base.focus.copy(lockUntil = 5_000_000))
        assertTrue(Rules.isLoosening(locked, locked.copy(focus = locked.focus.copy(lockUntil = 0))))
        assertFalse(Rules.isLoosening(locked, locked.copy(focus = locked.focus.copy(lockUntil = 9_000_000))))
    }

    @Test fun lockedNow() {
        val s = Settings(schedule = Schedule(enabled = true, days = listOf(1, 2, 3, 4, 5), start = "09:00", end = "17:00"))
        val monday10 = LocalDateTime.of(2026, 9, 21, 10, 0)
        val monday18 = LocalDateTime.of(2026, 9, 21, 18, 0)
        val saturday10 = LocalDateTime.of(2026, 9, 19, 10, 0)
        assertTrue(Rules.isLockedNow(s, monday10))
        assertFalse(Rules.isLockedNow(s, monday18))
        assertFalse(Rules.isLockedNow(s, saturday10))
        assertEquals("17:00", Rules.lockedUntilText(s, monday10))
        assertEquals("", Rules.lockedUntilText(s, monday18))
    }

    @Test fun guardRefusesLoosening() {
        val on = base.copy(focus = base.focus.copy(enabled = true))
        assertThrows(LockedException::class.java) {
            Rules.guard(on, on.copy(sites = on.sites.copy(adult = false)))
        }
        Rules.guard(on, withRule(on, "x", Rule("block")))
        val off = on.copy(focus = on.focus.copy(enabled = false))
        Rules.guard(off, off.copy(sites = off.sites.copy(adult = false)))
    }

    @Test fun guardRefusesTurningOffDuringLock() {
        val allDay = base.copy(schedule = Schedule(enabled = true, days = (1..7).toList(), start = "00:00", end = "23:59"))
        val e = assertThrows(LockedException::class.java) {
            Rules.guard(allDay, allDay.copy(focus = allDay.focus.copy(enabled = false)), LocalDateTime.of(2026, 9, 21, 10, 0))
        }
        assertTrue(e.message!!.contains("Locked until 23:59"))
    }

    @Test fun normalizeFallsBack() {
        val n = Settings.normalize(Settings(
            focus = Focus(unlockDelaySec = 42),
            apps = Apps(rules = mapOf("a" to Rule("nuke", 7), " " to Rule()), pauseSeconds = 7),
            schedule = Schedule(start = "18:00", end = "09:00", days = listOf(0, 1, 9)),
        ))
        assertEquals(300, n.focus.unlockDelaySec)
        assertEquals(mapOf("a" to Rule("block", 30)), n.apps.rules)
        assertEquals(10, n.apps.pauseSeconds)
        assertEquals(listOf(1), n.schedule.days)
        assertEquals("09:00", n.schedule.start)
    }

    @Test fun roundTrip() {
        val s = base.copy(focus = Focus(reason = "Thesis"))
        assertEquals(s, Settings.decode(s.encode()))
        assertEquals(Settings(), Settings.decode("garbage"))
    }

    @Test fun oldBlockedListBecomesRules() {
        val old = """{"version":1,"apps":{"mode":"pause","blocked":["a","b"],"passMinutes":5,"pauseSeconds":10,"dailyBudgetMinutes":60,"cooldownMinutes":15,"intention":true,"blockNotifications":true},"reels":{"instagram":true}}"""
        val s = Settings.decode(old)
        assertEquals(mapOf("a" to Rule("timer", 60), "b" to Rule("timer", 60)), s.apps.rules)
        val block = old.replace("\"mode\":\"pause\"", "\"mode\":\"block\"").replace("\"dailyBudgetMinutes\":60", "\"dailyBudgetMinutes\":1440")
        assertEquals(mapOf("a" to Rule("block", 180), "b" to Rule("block", 180)), Settings.decode(block).apps.rules)
        // Already migrated state is left alone.
        assertEquals(s, Settings.decode(s.encode()))
    }
}
