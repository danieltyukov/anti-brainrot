package io.github.danieltyukov.antibrainrot.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.core.Keys
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Rules
import io.github.danieltyukov.antibrainrot.core.Settings
import java.time.LocalDateTime

// Shared enforcement helpers used by the services and the UI.
object Enforcer {
    // Packages that are never blocked: our own, the system UI, launchers, the settings app.
    val NEVER_BLOCK = setOf("android", "com.android.systemui", "com.android.settings")

    fun siteFilterWanted(s: Settings): Boolean = s.focus.enabled && (s.sites.adult || Keys.blockedHosts(s).isNotEmpty())

    // Starts or stops the DNS filter to match the settings. Returns the
    // consent intent when the user still has to approve the VPN.
    fun syncSiteFilter(context: Context, s: Settings): Intent? {
        val wanted = siteFilterWanted(s)
        if (wanted) {
            val consent = VpnService.prepare(context)
            if (consent != null) return consent
            val intent = Intent(context, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        } else if (DnsVpnService.running) {
            context.startService(Intent(context, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
        }
        return null
    }

    // Blocked outright, out of time for today, or timed without a running
    // session. The key is a package name or "site:" plus a rule host.
    fun isBlocked(s: Settings, state: LocalState, key: String, now: Long = System.currentTimeMillis(), today: String = Passes.dayKey()): Boolean {
        if (!s.focus.enabled) return false
        val rule = Keys.ruleFor(s, key) ?: return false
        if (rule.mode == "block") return true
        if (Passes.secondsLeft(state, rule, key, today) <= 0) return true
        return Passes.activePass(state, key, now) == null
    }

    fun isBlockedApp(s: Settings, state: LocalState, pkg: String, now: Long = System.currentTimeMillis()): Boolean = isBlocked(s, state, pkg, now)

    fun isTimed(s: Settings, key: String): Boolean = s.focus.enabled && Keys.ruleFor(s, key)?.mode == "timer"

    // Called on a timer and on app switches: expires passes, starts cooldowns,
    // and forces the filter on during locked hours.
    suspend fun tick(app: App) {
        val settings = app.settings.get()
        app.local.update { Passes.sweep(it, settings.apps) }
        if (Rules.isLockedNow(settings, LocalDateTime.now()) && !settings.focus.enabled) {
            app.settings.patch { it.copy(focus = it.focus.copy(enabled = true)) }
        }
    }
}
