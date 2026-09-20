package io.github.danieltyukov.antibrainrot.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.danieltyukov.antibrainrot.core.Rule
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.Appear
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.ChoiceRow
import io.github.danieltyukov.antibrainrot.ui.RulePill
import io.github.danieltyukov.antibrainrot.ui.RuleSheet
import io.github.danieltyukov.antibrainrot.ui.ruleText
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow
import io.github.danieltyukov.antibrainrot.ui.minutesLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val pkg: String, val label: String, val icon: ImageBitmap?)

// Every launcher app with its rule: none, blocked, or a daily timer.
@Composable
fun AppsScreen(vm: AppViewModel, s: Settings) {
    val context = LocalContext.current
    // Icons for every launcher app take a moment; load them off the main thread.
    val apps by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadApps(context.packageManager, context.packageName) }
    }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<InstalledApp?>(null) }
    val rules = s.apps.rules
    val shown = (apps ?: emptyList())
        .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true) }
        .sortedWith(compareBy<InstalledApp> { rank(rules[it.pkg]) }.thenBy { it.label.lowercase() })

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)) {
        item {
            Appear(0) {
                SectionCard("Timed and blocked apps", "Give each app its own rule: blocked outright, or a daily timer. A timed app opens after a short pause for one session, and only the time it is in front counts against its limit.") {
                    ChoiceRow("Pause before continuing", s.apps.pauseSeconds, Settings.PAUSE_CHOICES, text = { "$it seconds" }) { v -> vm.update { it.copy(apps = it.apps.copy(pauseSeconds = v)) } }
                    ChoiceRow("Length of one session", s.apps.passMinutes, Settings.PASS_CHOICES, text = { minutesLabel(it) }) { v -> vm.update { it.copy(apps = it.apps.copy(passMinutes = v)) } }
                    ChoiceRow("Cooldown after a session", s.apps.cooldownMinutes, Settings.COOLDOWN_CHOICES, text = { minutesLabel(it) }) { v -> vm.update { it.copy(apps = it.apps.copy(cooldownMinutes = v)) } }
                    SwitchRow("Ask what you need there", s.apps.intention) { v -> vm.update { it.copy(apps = it.apps.copy(intention = v)) } }
                    SwitchRow("Hide their notifications", s.apps.blockNotifications, hint = "Needs notification access under Setup") { v -> vm.update { it.copy(apps = it.apps.copy(blockNotifications = v)) } }
                }
            }
        }
        item {
            Appear(1) {
                Column {
                    Text("Your apps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, top = 10.dp, end = 20.dp))
                    Text("Tap an app to set its rule.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search apps") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                    if (apps == null) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }
        }
        items(shown, key = { it.pkg }) { app ->
            AppRow(app, rules[app.pkg], Modifier.animateItem()) { editing = app }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    editing?.let { app ->
        RuleSheet(app.label, rules[app.pkg], icon = { AppIcon(app.icon, 44.dp) }, onDismiss = { editing = null }) { rule ->
            vm.update { it.copy(apps = it.apps.copy(rules = if (rule == null) it.apps.rules - app.pkg else it.apps.rules + (app.pkg to rule))) }
        }
    }
}

private fun rank(rule: Rule?) = when (rule?.mode) {
    "block" -> 0
    "timer" -> 1
    else -> 2
}

@Composable
private fun AppRow(app: InstalledApp, rule: Rule?, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ruleText(rule), style = MaterialTheme.typography.bodySmall, color = if (rule == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(8.dp))
        RulePill(rule)
    }
}

@Composable
private fun AppIcon(icon: ImageBitmap?, size: androidx.compose.ui.unit.Dp) {
    if (icon != null) Image(icon, contentDescription = null, Modifier.size(size).clip(RoundedCornerShape(10.dp)))
    else Spacer(Modifier.size(size))
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
