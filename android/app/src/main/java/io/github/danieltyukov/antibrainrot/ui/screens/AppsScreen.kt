package io.github.danieltyukov.antibrainrot.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.ChoiceRow
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow
import io.github.danieltyukov.antibrainrot.ui.minutesLabel

data class InstalledApp(val pkg: String, val label: String, val icon: androidx.compose.ui.graphics.ImageBitmap?)

@Composable
fun AppsScreen(vm: AppViewModel, s: Settings) {
    val context = LocalContext.current
    val apps = remember { loadApps(context.packageManager, context.packageName) }
    var query by remember { mutableStateOf("") }
    val blocked = s.apps.blocked.toSet()
    val shown = apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true) }
        .sortedWith(compareByDescending<InstalledApp> { it.pkg in blocked }.thenBy { it.label.lowercase() })

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            SectionCard("Short video feeds", "Closed the moment they open. YouTube Shorts is not a setting.") {
                SwitchRow("YouTube Shorts", checked = true, enabled = false, hint = "always on") {}
                SwitchRow("Instagram Reels", s.reels.instagram) { v -> vm.update { it.copy(reels = it.reels.copy(instagram = v)) } }
                SwitchRow("Facebook Reels", s.reels.facebook) { v -> vm.update { it.copy(reels = it.reels.copy(facebook = v)) } }
                SwitchRow("Snapchat Spotlight", s.reels.snapchatSpotlight) { v -> vm.update { it.copy(reels = it.reels.copy(snapchatSpotlight = v)) } }
            }
        }
        item {
            SectionCard("Blocked apps", "In pause mode a countdown and an intention stand between you and the app, then a timed pass from a daily budget. In block mode the app is simply off.") {
                ChoiceRow("Mode", s.apps.mode, Settings.MODES, text = { if (it == "pause") "Pause, then a timed pass" else "Block outright" }) { v -> vm.update { it.copy(apps = it.apps.copy(mode = v)) } }
                ChoiceRow("Pause before continuing", s.apps.pauseSeconds, Settings.PAUSE_CHOICES, text = { "$it seconds" }) { v -> vm.update { it.copy(apps = it.apps.copy(pauseSeconds = v)) } }
                ChoiceRow("Length of one pass", s.apps.passMinutes, Settings.PASS_CHOICES, text = { minutesLabel(it) }) { v -> vm.update { it.copy(apps = it.apps.copy(passMinutes = v)) } }
                ChoiceRow("Daily budget for passes", s.apps.dailyBudgetMinutes, Settings.BUDGET_CHOICES, text = { if (it == 0) "No passes" else minutesLabel(it) }) { v -> vm.update { it.copy(apps = it.apps.copy(dailyBudgetMinutes = v)) } }
                ChoiceRow("Cooldown after a pass", s.apps.cooldownMinutes, Settings.COOLDOWN_CHOICES, text = { minutesLabel(it) }) { v -> vm.update { it.copy(apps = it.apps.copy(cooldownMinutes = v)) } }
                SwitchRow("Ask what you need there", s.apps.intention) { v -> vm.update { it.copy(apps = it.apps.copy(intention = v)) } }
                SwitchRow("Hide their notifications", s.apps.blockNotifications, hint = "Needs notification access under Setup") { v -> vm.update { it.copy(apps = it.apps.copy(blockNotifications = v)) } }
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search apps") }, singleLine = true)
        }
        items(shown, key = { it.pkg }) { app ->
            val checked = app.pkg in blocked
            Row(
                Modifier.fillMaxWidth().clickable {
                    val next = if (checked) s.apps.blocked - app.pkg else s.apps.blocked + app.pkg
                    vm.update { it.copy(apps = it.apps.copy(blocked = next)) }
                }.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (app.icon != null) Image(app.icon, contentDescription = null, Modifier.size(36.dp)) else Spacer(Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Checkbox(checked = checked, onCheckedChange = null)
            }
        }
    }
}

private fun loadApps(pm: PackageManager, self: String): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = pm.queryIntentActivities(intent, 0)
    return resolved.map { it.activityInfo.packageName }.distinct().filter { it != self }.map { pkg ->
        val label = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }
        val icon = try { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() } catch (e: Exception) { null }
        InstalledApp(pkg, label, icon)
    }
}
