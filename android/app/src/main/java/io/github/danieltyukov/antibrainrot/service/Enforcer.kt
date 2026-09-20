package io.github.danieltyukov.antibrainrot.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Rules
import io.github.danieltyukov.antibrainrot.core.Settings
import java.time.LocalDateTime

// Shared enforcement helpers used by the services and the UI.
object Enforcer {
    // Packages that are never blocked: our own, the system UI, launchers, the settings app.
    val NEVER_BLOCK = setOf("android", "com.android.systemui", "com.android.settings")

    fun siteFilterWanted(s: Settings): Boolean = s.focus.enabled && (s.sites.adult || s.sites.distracting)

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

    fun isBlockedApp(s: Settings, state: LocalState, pkg: String, now: Long = System.currentTimeMillis()): Boolean {
        if (!s.focus.enabled) return false
        if (pkg !in s.apps.blocked) return false
        return Passes.activePass(state, pkg, now) == null
    }

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
