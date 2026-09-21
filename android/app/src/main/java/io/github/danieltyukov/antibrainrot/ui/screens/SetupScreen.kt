package io.github.danieltyukov.antibrainrot.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.Permissions
import io.github.danieltyukov.antibrainrot.ui.Appear
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.theme.Leaf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon

@Composable
fun SetupScreen(vm: AppViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(lifecycle) { if (lifecycle == Lifecycle.State.RESUMED) refresh += 1 }
    val accessibility = remember(refresh) { Permissions.accessibilityEnabled(context) }
    val accessibilityRunning = remember(refresh) { Permissions.accessibilityRunning() }
    val overlay = remember(refresh) { Permissions.overlayGranted(context) }
    val notifAccess = remember(refresh) { Permissions.notificationAccess(context) }
    val battery = remember(refresh) { Permissions.batteryUnrestricted(context) }
    var notificationsGranted by remember(refresh) {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsGranted = it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp)) {
        Appear(0) { SectionCard("Setup", "Two permissions are required. The others make the app better.") {
            PermissionRow("Accessibility service", "Required. Sees which app is in front, reads the browser address bar, shows the block screen.", accessibility) { context.startActivity(Permissions.accessibilityIntent()) }
            if (accessibility && !accessibilityRunning) {
                Text("Enabled but not running. After an update Android waits for the switch to be turned off and on again, or for a restart, before it starts the service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { context.startActivity(Permissions.accessibilityIntent()) }) { Text("Open accessibility settings") }
                Spacer(Modifier.height(8.dp))
            }
            if (Permissions.needsRestrictedSettingsHint && !accessibility) {
                Text("Sideloaded apps on Android 13 and later: if the switch is greyed out, open App info for AntiBrainrot, tap the three dots, choose Allow restricted settings, then come back.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { context.startActivity(Permissions.appInfoIntent(context)) }) { Text("Open App info") }
                Spacer(Modifier.height(8.dp))
            }
            PermissionRow("Display over other apps", "Required. Lets the block screen appear over a blocked app.", overlay) { context.startActivity(Permissions.overlayIntent(context)) }
            PermissionRow("Notifications", "Shows the site filter's status notification.", notificationsGranted) {
                if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            PermissionRow("Notification access", "Hides notifications from blocked apps while the filter is on.", notifAccess) { context.startActivity(Permissions.notificationAccessIntent()) }
            PermissionRow("Unrestricted battery", "Keeps the services alive on phones that kill background apps.", battery) { context.startActivity(Permissions.batteryIntent(context)) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.update { it.copy(onboarded = true) }; onDone() }, Modifier.fillMaxWidth(), enabled = accessibility && overlay) { Text("Continue") }
        } }
    }
}

@Composable
private fun PermissionRow(title: String, text: String, granted: Boolean, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        AnimatedContent(targetState = granted, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith fadeOut() }, label = "granted") { ok ->
            if (ok) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Leaf, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("On", color = Leaf, fontWeight = FontWeight.SemiBold)
            } else OutlinedButton(onClick = onOpen) { Text("Open") }
        }
    }
}
