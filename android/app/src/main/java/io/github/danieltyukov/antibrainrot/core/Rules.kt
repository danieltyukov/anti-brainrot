package io.github.danieltyukov.antibrainrot.core

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LockedException(message: String) : Exception(message)

object Rules {
    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun hasNew(before: List<String>, after: List<String>): Boolean {
        val b = before.map { it.trim().lowercase() }.toSet()
        return after.any { it.trim().lowercase() !in b }
    }

    private fun minutesOf(hhmm: String): Int {
        val (h, m) = hhmm.split(":").map { it.toInt() }
        return h * 60 + m
    }

    // True when `next` restricts less than `current` in any way. The single
    // definition of "loosening" for the rule "tighten any time, loosen only
    // while the filter is off".
    fun isLoosening(current: Settings, next: Settings): Boolean {
        val a = Settings.normalize(current)
        val b = Settings.normalize(next)
        if (b.focus.unlockDelaySec < a.focus.unlockDelaySec) return true
        if (b.focus.lockUntil < a.focus.lockUntil) return true
        if (a.schedule.enabled && !b.schedule.enabled) return true
        if (b.schedule.enabled) {
            if (hasNew(b.schedule.days.map { it.toString() }, a.schedule.days.map { it.toString() })) return true
            if (b.schedule.start > a.schedule.start) return true
            if (b.schedule.end < a.schedule.end) return true
        }
        if (hasNew(b.apps.blocked, a.apps.blocked)) return true
        if (a.apps.mode == "block" && b.apps.mode == "pause") return true
        if (b.apps.passMinutes > a.apps.passMinutes) return true
        if (b.apps.pauseSeconds < a.apps.pauseSeconds) return true
        if (b.apps.dailyBudgetMinutes > a.apps.dailyBudgetMinutes) return true
        if (b.apps.cooldownMinutes < a.apps.cooldownMinutes) return true
        if (a.apps.intention && !b.apps.intention) return true
        if (a.apps.blockNotifications && !b.apps.blockNotifications) return true
        if (a.reels.instagram && !b.reels.instagram) return true
        if (a.reels.facebook && !b.reels.facebook) return true
        if (a.reels.snapchatSpotlight && !b.reels.snapchatSpotlight) return true
        if (a.sites.adult && !b.sites.adult) return true
        if (a.sites.distracting && !b.sites.distracting) return true
        if (b.sites.distracting) {
            if (hasNew(b.sites.presets, a.sites.presets)) return true
            if (hasNew(b.sites.custom, a.sites.custom)) return true
        }
        if (b.sites.adult || b.sites.distracting) {
            if (hasNew(a.sites.allowed, b.sites.allowed)) return true
        }
        if (a.strictMode && !b.strictMode) return true
        return false
    }

    fun isLockedNow(s: Settings, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (s.focus.lockUntil > nowMs) return true
        if (!s.schedule.enabled) return false
        if (now.dayOfWeek.value !in s.schedule.days) return false
        val minutes = now.hour * 60 + now.minute
        return minutes >= minutesOf(s.schedule.start) && minutes < minutesOf(s.schedule.end)
    }

    // "17:00" or "tomorrow 09:30", empty when nothing locks.
    fun lockedUntilText(s: Settings, now: LocalDateTime = LocalDateTime.now()): String {
        var end: LocalDateTime? = null
        val nowMs = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (s.focus.lockUntil > nowMs) {
            end = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(s.focus.lockUntil), ZoneId.systemDefault())
        }
        if (s.schedule.enabled && now.dayOfWeek.value in s.schedule.days) {
            val minutes = now.hour * 60 + now.minute
            if (minutes >= minutesOf(s.schedule.start) && minutes < minutesOf(s.schedule.end)) {
                val (h, m) = s.schedule.end.split(":").map { it.toInt() }
                val scheduleEnd = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
                if (end == null || scheduleEnd.isAfter(end)) end = scheduleEnd
            }
        }
        val e = end ?: return ""
        val time = e.format(HHMM)
        return if (e.toLocalDate() == now.toLocalDate()) time else "tomorrow $time"
    }

    // Applies the guards a user change must pass. Throws LockedException.
    fun guard(current: Settings, next: Settings, now: LocalDateTime = LocalDateTime.now()) {
        if (current.focus.enabled && next.focus.enabled && isLoosening(current, next)) {
            throw LockedException("The filter is on. Loosening it needs the filter off first.")
        }
        if (current.focus.enabled && !next.focus.enabled && isLockedNow(current, now)) {
            throw LockedException("Locked until ${lockedUntilText(current, now)}. The filter stays on.")
        }
    }
}
