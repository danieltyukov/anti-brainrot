package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RulesTest {
    private val base = Settings(
        apps = Apps(blocked = listOf("com.zhiliaoapp.musically")),
        schedule = Schedule(enabled = true),
        sites = Sites(adult = true, distracting = true),
    )

    private fun loosens(change: (Settings) -> Settings) = Rules.isLoosening(base, change(base))

    @Test fun unchangedIsNotLoosening() = assertFalse(loosens { it })

    @Test fun featureOffLoosens() {
        assertTrue(loosens { it.copy(sites = it.sites.copy(adult = false)) })
        assertTrue(loosens { it.copy(schedule = it.schedule.copy(enabled = false)) })
        assertTrue(loosens { it.copy(apps = it.apps.copy(blocked = emptyList())) })
        assertFalse(loosens { it.copy(apps = it.apps.copy(blocked = it.apps.blocked + "com.instagram.android")) })
    }

    @Test fun passSettings() {
        assertTrue(loosens { it.copy(apps = it.apps.copy(passMinutes = 10)) })
        assertFalse(loosens { it.copy(apps = it.apps.copy(passMinutes = 2)) })
        assertTrue(loosens { it.copy(apps = it.apps.copy(pauseSeconds = 5)) })
        assertTrue(loosens { it.copy(apps = it.apps.copy(dailyBudgetMinutes = 60)) })
        assertTrue(loosens { it.copy(apps = it.apps.copy(cooldownMinutes = 5)) })
        val blockMode = base.copy(apps = base.apps.copy(mode = "block"))
        assertTrue(Rules.isLoosening(blockMode, base))
        assertFalse(Rules.isLoosening(base, blockMode))
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
        Rules.guard(on, on.copy(apps = on.apps.copy(blocked = on.apps.blocked + "x")))
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
        val n = Settings.normalize(Settings(focus = Focus(unlockDelaySec = 42), apps = Apps(mode = "nuke", passMinutes = 7), schedule = Schedule(start = "18:00", end = "09:00", days = listOf(0, 1, 9))))
        assertEquals(300, n.focus.unlockDelaySec)
        assertEquals("pause", n.apps.mode)
        assertEquals(5, n.apps.passMinutes)
        assertEquals(listOf(1), n.schedule.days)
        assertEquals("09:00", n.schedule.start)
    }

    @Test fun roundTrip() {
        val s = base.copy(focus = Focus(reason = "Thesis"))
        assertEquals(s, Settings.decode(s.encode()))
        assertEquals(Settings(), Settings.decode("garbage"))
    }
}
