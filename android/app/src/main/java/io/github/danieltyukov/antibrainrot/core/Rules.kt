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
        // An app rule loosens when it goes away, when block becomes timer,
        // or when a timer gets more minutes.
        for ((pkg, ra) in a.apps.rules) {
            val rb = b.apps.rules[pkg] ?: return true
            if (ra.mode == "block" && rb.mode == "timer") return true
            if (ra.mode == "timer" && rb.mode == "timer" && rb.limitMinutes > ra.limitMinutes) return true
        }
        if (a.apps.pauseEnabled && !b.apps.pauseEnabled) return true
        if (b.apps.pauseEnabled && b.apps.pauseSeconds < a.apps.pauseSeconds) return true
        if (b.apps.pauseEnabled && a.apps.intention && !b.apps.intention) return true
        if (a.apps.blockNotifications && !b.apps.blockNotifications) return true
        if (a.apps.blockInstalls && !b.apps.blockInstalls) return true
        if (a.sites.adult && !b.sites.adult) return true
        // Safe search and Restricted Mode follow the adult filter; they only
        // count while it is on.
        if (b.sites.adult) {
            if (a.sites.safeSearch && !b.sites.safeSearch) return true
            if (a.sites.restrictYouTube && !b.sites.restrictYouTube) return true
        }
        if (hasNew(b.sites.keywords, a.sites.keywords)) return true
        for ((host, ra) in a.sites.rules) {
            val rb = b.sites.rules[host] ?: return true
            if (ra.mode == "block" && rb.mode == "timer") return true
            if (ra.mode == "timer" && rb.mode == "timer" && rb.limitMinutes > ra.limitMinutes) return true
        }
        if (b.sites.adult || b.sites.rules.isNotEmpty()) {
            if (hasNew(a.sites.allowed, b.sites.allowed)) return true
        }
        if (a.strictMode && !b.strictMode) return true
        if (a.preventUninstall && !b.preventUninstall) return true
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
