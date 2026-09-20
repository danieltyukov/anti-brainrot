package io.github.danieltyukov.antibrainrot.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Settings mirror the browser extension's schema where the concepts match.
// Everything the user can change lives here; passes, budgets and counters
// live in LocalState because they are machine local and short lived.

@Serializable
data class Focus(
    val enabled: Boolean = true,
    val unlockDelaySec: Int = 300,
    val lockUntil: Long = 0,
    val reason: String = "",
)

@Serializable
data class Schedule(
    val enabled: Boolean = false,
    // ISO days: 1 = Monday ... 7 = Sunday
    val days: List<Int> = listOf(1, 2, 3, 4, 5),
    val start: String = "09:00",
    val end: String = "17:00",
)

@Serializable
data class Apps(
    val mode: String = "pause",
    val blocked: List<String> = emptyList(),
    val passMinutes: Int = 5,
    val pauseSeconds: Int = 10,
    val dailyBudgetMinutes: Int = 30,
    val cooldownMinutes: Int = 15,
    val intention: Boolean = true,
    val blockNotifications: Boolean = true,
)

@Serializable
data class Reels(
    // YouTube Shorts are always blocked; this is not a setting, like the extension.
    val instagram: Boolean = true,
    val facebook: Boolean = true,
    val snapchatSpotlight: Boolean = true,
)

@Serializable
data class Sites(
    val adult: Boolean = false,
    val distracting: Boolean = false,
    val presets: List<String> = Presets.DEFAULT_IDS,
    val custom: List<String> = emptyList(),
    val allowed: List<String> = emptyList(),
)

@Serializable
data class Settings(
    val version: Int = 1,
    val focus: Focus = Focus(),
    val schedule: Schedule = Schedule(),
    val apps: Apps = Apps(),
    val reels: Reels = Reels(),
    val sites: Sites = Sites(),
    val strictMode: Boolean = false,
    val theme: String = "system",
    val onboarded: Boolean = false,
) {
    companion object {
        val DELAY_CHOICES = listOf(0, 30, 60, 300, 600, 1800, 3600)
        val PASS_CHOICES = listOf(1, 2, 5, 10, 15, 30)
        val PAUSE_CHOICES = listOf(5, 10, 20, 30, 60)
        val BUDGET_CHOICES = listOf(0, 10, 15, 30, 60, 120, 1440)
        val COOLDOWN_CHOICES = listOf(0, 5, 15, 30, 60)
        val LOCK_HOURS = listOf(1, 2, 4, 8, 24)
        val MODES = listOf("block", "pause")
        val THEMES = listOf("system", "light", "dark")
        private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

        fun decode(text: String?): Settings {
            if (text.isNullOrBlank()) return Settings()
            return try {
                normalize(json.decodeFromString(serializer(), text))
            } catch (e: Exception) {
                Settings()
            }
        }

        // Brings any stored object back into a valid state: unknown choices
        // fall back to defaults, lists are trimmed and deduplicated.
        fun normalize(s: Settings): Settings {
            val focus = s.focus.copy(
                unlockDelaySec = if (s.focus.unlockDelaySec in DELAY_CHOICES) s.focus.unlockDelaySec else 300,
                lockUntil = if (s.focus.lockUntil > 0) s.focus.lockUntil else 0,
                reason = s.focus.reason.trim().take(200),
            )
            val days = s.schedule.days.filter { it in 1..7 }.distinct().sorted()
            val start = if (TIME.matches(s.schedule.start)) s.schedule.start else "09:00"
            val end = if (TIME.matches(s.schedule.end)) s.schedule.end else "17:00"
            val schedule = if (start < end) s.schedule.copy(days = days, start = start, end = end)
            else s.schedule.copy(days = days, start = "09:00", end = "17:00")
            val apps = s.apps.copy(
                mode = if (s.apps.mode in MODES) s.apps.mode else "pause",
                blocked = s.apps.blocked.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                passMinutes = if (s.apps.passMinutes in PASS_CHOICES) s.apps.passMinutes else 5,
                pauseSeconds = if (s.apps.pauseSeconds in PAUSE_CHOICES) s.apps.pauseSeconds else 10,
                dailyBudgetMinutes = if (s.apps.dailyBudgetMinutes in BUDGET_CHOICES) s.apps.dailyBudgetMinutes else 30,
                cooldownMinutes = if (s.apps.cooldownMinutes in COOLDOWN_CHOICES) s.apps.cooldownMinutes else 15,
            )
            val sites = s.sites.copy(
                presets = s.sites.presets.filter { id -> Presets.ALL.any { it.id == id } }.distinct(),
                custom = Domains.parseList(s.sites.custom.joinToString("\n")),
                allowed = Domains.parseList(s.sites.allowed.joinToString("\n")),
            )
            return s.copy(
                version = 1,
                focus = focus,
                schedule = schedule,
                apps = apps,
                sites = sites,
                theme = if (s.theme in THEMES) s.theme else "system",
            )
        }
    }

    fun encode(): String = json.encodeToString(serializer(), this)
}

// Machine local, short lived state.
@Serializable
data class Budget(val day: String = "", val usedMinutes: Int = 0)

@Serializable
data class Stats(val day: String = "", val blocks: Int = 0, val passes: Int = 0, val feedsClosed: Int = 0)

@Serializable
data class LocalState(
    // key: package name for apps, host for sites; value: epoch millis
    val passes: Map<String, Long> = emptyMap(),
    val cooldowns: Map<String, Long> = emptyMap(),
    val budget: Budget = Budget(),
    val stats: Stats = Stats(),
) {
    companion object {
        fun decode(text: String?): LocalState =
            if (text.isNullOrBlank()) LocalState() else try {
                Settings.json.decodeFromString(serializer(), text)
            } catch (e: Exception) {
                LocalState()
            }
    }

    fun encode(): String = Settings.json.encodeToString(serializer(), this)
}
