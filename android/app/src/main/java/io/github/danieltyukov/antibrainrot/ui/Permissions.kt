package io.github.danieltyukov.antibrainrot.ui

import android.content.Context
import android.app.admin.DevicePolicyManager
import android.content.Intent
import io.github.danieltyukov.antibrainrot.service.AdminReceiver
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.danieltyukov.antibrainrot.service.BlockerAccessibilityService

object Permissions {
    fun accessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = "${context.packageName}/${BlockerAccessibilityService::class.java.name}"
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) || it.endsWith("/.service.BlockerAccessibilityService") && it.startsWith(context.packageName) }
    }

    // Android keeps an updated app's service listed as enabled but does not
    // bind it again until the switch is cycled or the phone restarts.
    fun accessibilityRunning(): Boolean = BlockerAccessibilityService.instance != null

    fun overlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun notificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun batteryUnrestricted(context: Context): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)

    fun accessibilityIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun notificationAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun batteryIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun adminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(AdminReceiver.component(context))
    }

    fun adminIntent(context: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, AdminReceiver.component(context))
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Android will refuse to uninstall AntiBrainrot while this is active. No device policies are used.")

    fun removeAdmin(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val c = AdminReceiver.component(context)
        if (dpm.isAdminActive(c)) dpm.removeActiveAdmin(c)
    }

    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // Private DNS in hostname mode makes Android talk DNS over TLS to that
    // host directly, which a local DNS filter never sees.
    fun privateDnsHostname(context: Context): String? = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork
        val props = network?.let { cm.getLinkProperties(it) }
        if (props != null && props.isPrivateDnsActive) props.privateDnsServerName else null
    } catch (e: Exception) {
        null
    }

    // Android 13 and later hide the accessibility switch for sideloaded apps
    // behind "Allow restricted settings" on the app's info page.
    val needsRestrictedSettingsHint: Boolean get() = Build.VERSION.SDK_INT >= 33
}
