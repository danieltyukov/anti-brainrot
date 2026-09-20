package io.github.danieltyukov.antibrainrot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.danieltyukov.antibrainrot.MainActivity
import io.github.danieltyukov.antibrainrot.R
import io.github.danieltyukov.antibrainrot.core.Keys
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Progress
import io.github.danieltyukov.antibrainrot.core.Settings

// While a timed app or site is in front: a quiet notification with the time
// left today, and one heads-up warning a minute before it runs out, so the
// block screen never comes out of the blue.
class SessionNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var warnedFor = ""
    private var showing = false

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(SESSIONS, context.getString(R.string.sessions_channel), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
            manager.createNotificationChannel(NotificationChannel(WARNINGS, context.getString(R.string.warnings_channel), NotificationManager.IMPORTANCE_HIGH))
        }
    }

    // Seconds of the day's limit left for a timed key, or null.
    fun secondsLeft(s: Settings, state: LocalState, key: String, now: Long = System.currentTimeMillis()): Int? {
        val rule = Keys.ruleFor(s, key) ?: return null
        if (rule.mode != "timer") return null
        return Passes.secondsLeft(state, rule, key)
    }

    fun update(s: Settings, state: LocalState, keys: Set<String>, label: (String) -> String) {
        val key = keys.mapNotNull { k -> secondsLeft(s, state, k)?.let { k to it } }.minByOrNull { it.second }
        if (key == null) {
            clear()
            return
        }
        val (k, left) = key
        val name = label(k)
        val text = if (left >= 60) "${Progress.focusText(left)} left today." else "Less than a minute left today."
        manager.notify(ID_SESSION, build(SESSIONS, name, text, ongoing = true))
        showing = true
        if (left in 1..75 && warnedFor != k + ":" + (System.currentTimeMillis() / 3_600_000)) {
            warnedFor = k + ":" + (System.currentTimeMillis() / 3_600_000)
            manager.notify(ID_WARNING, build(WARNINGS, "$name: one minute left", "Wrap up. It is blocked for the rest of the day after that."))
        }
    }

    fun clear() {
        if (!showing) return
        showing = false
        manager.cancel(ID_SESSION)
    }

    private fun build(channel: String, title: String, text: String, ongoing: Boolean = false): Notification {
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channel) else @Suppress("DEPRECATION") Notification.Builder(context)
        return builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(ongoing)
            .build()
    }

    companion object {
        const val SESSIONS = "sessions"
        const val WARNINGS = "warnings"
        const val ID_SESSION = 20
        const val ID_WARNING = 21
    }
}
