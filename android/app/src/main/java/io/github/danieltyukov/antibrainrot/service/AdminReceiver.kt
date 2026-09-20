package io.github.danieltyukov.antibrainrot.service

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context

// A device admin with no policies. Being active is the point: Android
// refuses to uninstall an active admin until it is deactivated in Settings,
// and strict mode leaves that Settings page as soon as it opens.
class AdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun component(context: Context) = ComponentName(context, AdminReceiver::class.java)
    }
}
