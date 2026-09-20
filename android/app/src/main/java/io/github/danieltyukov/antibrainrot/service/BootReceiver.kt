package io.github.danieltyukov.antibrainrot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.danieltyukov.antibrainrot.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// After a reboot the accessibility service comes back by itself; the DNS
// filter needs a nudge (consent was given earlier, so prepare() is null).
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val s = App.instance.settings.get()
                Enforcer.syncSiteFilter(context.applicationContext, s)
            } finally {
                pending.finish()
            }
        }
    }
}
