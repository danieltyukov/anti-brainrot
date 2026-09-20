package io.github.danieltyukov.antibrainrot.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Drops notifications from blocked apps while the filter is on and no pass
// is active for that app.
class NotificationBlockerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile private var settings = Settings()
    @Volatile private var local = LocalState()

    override fun onListenerConnected() {
        val app = App.instance
        scope.launch { app.settings.flow.collect { settings = it } }
        scope.launch { app.local.flow.collect { local = it } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (!settings.apps.blockNotifications) return
        if (Enforcer.isBlockedApp(settings, local, n.packageName)) cancelNotification(n.key)
    }
}
