package io.github.danieltyukov.antibrainrot.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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

// One app's or site's rule: blocked outright, or a daily timer of limitMinutes.
@Serializable
data class Rule(val mode: String = "block", val limitMinutes: Int = 30)

@Serializable
data class Apps(
    // Package name to rule.
    val rules: Map<String, Rule> = emptyMap(),
    // A timed app opens for one session of this length, capped by what is
    // left of its daily limit.
    val passMinutes: Int = 5,
    val pauseSeconds: Int = 10,
    val cooldownMinutes: Int = 15,
    val intention: Boolean = true,
    val blockNotifications: Boolean = true,
)

@Serializable
data class Sites(
    val adult: Boolean = false,
    // Host to rule; subdomains follow the rule of their parent.
    val rules: Map<String, Rule> = emptyMap(),
    // Never blocked, whatever the adult list says.
    val allowed: List<String> = emptyList(),
)

@Serializable
data class Settings(
    val version: Int = 2,
    val focus: Focus = Focus(),
    val schedule: Schedule = Schedule(),
    val apps: Apps = Apps(),
    val sites: Sites = Sites(),
    val strictMode: Boolean = false,
    val theme: String = "system",
    val onboarded: Boolean = false,
) {
    companion object {
        val DELAY_CHOICES = listOf(0, 30, 60, 300, 600, 1800, 3600)
        val PASS_CHOICES = listOf(1, 2, 5, 10, 15, 30, 60)
        val PAUSE_CHOICES = listOf(5, 10, 20, 30, 60)
        val COOLDOWN_CHOICES = listOf(0, 5, 15, 30, 60)
        val LOCK_HOURS = listOf(1, 2, 4, 8, 24)
        val RULE_MODES = listOf("block", "timer")
        val LIMIT_CHOICES = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180)
        val THEMES = listOf("system", "light", "dark")
        private val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

        fun decode(text: String?): Settings {
            if (text.isNullOrBlank()) return Settings()
            return try {
                normalize(json.decodeFromJsonElement(serializer(), migrate(json.parseToJsonElement(text).jsonObject)))
            } catch (e: Exception) {
                Settings()
            }
        }

        // Up to 1.3 the apps section had one mode, one daily budget and a
        // list of packages. They become one rule per package.
        fun migrate(root: JsonObject): JsonObject = migrateSites(migrateApps(root))

        private fun migrateApps(root: JsonObject): JsonObject {
            val apps = root["apps"]?.jsonObject ?: return root
            if ("rules" in apps || "blocked" !in apps) return root
            val mode = if (apps["mode"]?.jsonPrimitive?.contentOrNull == "block") "block" else "timer"
            val budget = apps["dailyBudgetMinutes"]?.jsonPrimitive?.intOrNull ?: 30
            val limit = LIMIT_CHOICES.firstOrNull { it >= budget } ?: LIMIT_CHOICES.last()
            val rules = buildJsonObject {
                apps["blocked"]?.jsonArray?.forEach { pkg ->
                    put(pkg.jsonPrimitive.content, buildJsonObject { put("mode", mode); put("limitMinutes", limit) })
                }
            }
            return JsonObject(root + ("apps" to JsonObject(apps + ("rules" to rules))))
        }

        // Up to 1.3 distracting sites were a switch over presets and custom
        // hosts. They become block rules, one per host, when the switch was on.
        private fun migrateSites(root: JsonObject): JsonObject {
            val sites = root["sites"]?.jsonObject ?: return root
            if ("rules" in sites || ("presets" !in sites && "custom" !in sites)) return root
            val on = sites["distracting"]?.jsonPrimitive?.contentOrNull == "true"
            val hosts = if (!on) emptyList() else {
                val presets = sites["presets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val custom = sites["custom"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                Presets.ALL.filter { it.id in presets }.map { it.hosts.first() } + custom
            }
            val rules = buildJsonObject {
                hosts.distinct().forEach { host -> put(host, buildJsonObject { put("mode", "block"); put("limitMinutes", 30) }) }
            }
            return JsonObject(root + ("sites" to JsonObject(sites + ("rules" to rules))))
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
            val rules = s.apps.rules.entries
                .filter { it.key.isNotBlank() }
                .associate { (pkg, r) ->
                    pkg.trim() to Rule(
                        mode = if (r.mode in RULE_MODES) r.mode else "block",
                        limitMinutes = if (r.limitMinutes in LIMIT_CHOICES) r.limitMinutes else 30,
                    )
                }
            val apps = s.apps.copy(
                rules = rules,
                passMinutes = if (s.apps.passMinutes in PASS_CHOICES) s.apps.passMinutes else 5,
                pauseSeconds = if (s.apps.pauseSeconds in PAUSE_CHOICES) s.apps.pauseSeconds else 10,
                cooldownMinutes = if (s.apps.cooldownMinutes in COOLDOWN_CHOICES) s.apps.cooldownMinutes else 15,
            )
            val siteRules = s.sites.rules.entries
                .mapNotNull { (host, r) -> Domains.normalize(host)?.let { it to r } }
                .associate { (host, r) ->
                    host to Rule(
                        mode = if (r.mode in RULE_MODES) r.mode else "block",
                        limitMinutes = if (r.limitMinutes in LIMIT_CHOICES) r.limitMinutes else 30,
                    )
                }
            val sites = s.sites.copy(
                rules = siteRules,
                allowed = Domains.parseList(s.sites.allowed.joinToString("\n")),
            )
            return s.copy(
                version = 2,
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

// Seconds spent today in each timed app.
@Serializable
data class Usage(val day: String = "", val seconds: Map<String, Int> = emptyMap())

// Today's counters as 1.2 stored them; only read to migrate them.
@Serializable
data class Stats(val day: String = "", val blocks: Int = 0, val passes: Int = 0)

// One day of the progress history.
@Serializable
data class DayRecord(
    val blocks: Int = 0,
    val passes: Int = 0,
    val passMinutes: Int = 0,
    // Seconds the filter was on while the service ran.
    val focusSeconds: Int = 0,
    // Block screens per package.
    val byApp: Map<String, Int> = emptyMap(),
    // Seconds in front per timed package.
    val usage: Map<String, Int> = emptyMap(),
)

@Serializable
data class LocalState(
    // Package to session end, epoch millis.
    val passes: Map<String, Long> = emptyMap(),
    val cooldowns: Map<String, Long> = emptyMap(),
    val usage: Usage = Usage(),
    val stats: Stats = Stats(),
    // ISO date to that day's counters, the last Progress.HISTORY_DAYS days.
    val history: Map<String, DayRecord> = emptyMap(),
) {
    companion object {
        fun decode(text: String?): LocalState =
            if (text.isNullOrBlank()) LocalState() else try {
                migrate(Settings.json.decodeFromString(serializer(), text))
            } catch (e: Exception) {
                LocalState()
            }

        // State written before the history existed only had today's Stats;
        // carry them into the history so the first day is not lost.
        fun migrate(state: LocalState): LocalState {
            val st = state.stats
            if (st.day.isBlank() || st.day in state.history) return state
            if (st.blocks == 0 && st.passes == 0) return state
            return state.copy(history = state.history + (st.day to DayRecord(blocks = st.blocks, passes = st.passes)))
        }
    }

    fun encode(): String = Settings.json.encodeToString(serializer(), this)
}
