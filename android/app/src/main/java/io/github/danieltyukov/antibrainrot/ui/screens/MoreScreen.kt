package io.github.danieltyukov.antibrainrot.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import io.github.danieltyukov.antibrainrot.ui.Permissions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.BuildConfig
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.Appear
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.ChoiceRow
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow

@Composable
fun MoreScreen(vm: AppViewModel, s: Settings, onSetup: () -> Unit) {
    val context = LocalContext.current
    var reason by remember(s.focus.reason) { mutableStateOf(s.focus.reason) }
    var refreshAdmin by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp)) {
        Appear(0) {
            SectionCard("Your reason", "One line shown on every block screen. Written by you, for you.") {
                OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Thesis due in March.") }, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.update { it.copy(focus = it.focus.copy(reason = reason)) } }, Modifier.fillMaxWidth(), enabled = reason != s.focus.reason) { Text("Save") }
            }
        }
        Appear(1) {
            SectionCard("Strict mode", "While the filter is on, the Settings pages that could disable AntiBrainrot (its App info page, the accessibility page) are closed as soon as they open. Turning strict mode off waits until the filter is off.") {
                SwitchRow("Strict mode", s.strictMode) { v -> vm.update { it.copy(strictMode = v) } }
            }
        }
        Appear(2) {
            val adminActive = remember(refreshAdmin) { Permissions.adminActive(context) }
            val askAdmin = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                refreshAdmin += 1
                if (Permissions.adminActive(context)) vm.update { it.copy(preventUninstall = true) }
                else vm.message.value = "Uninstall protection needs the device admin confirmation."
            }
            SectionCard("Prevent uninstall", "Makes AntiBrainrot a device admin with no policies. Android then refuses to uninstall it until the admin is turned off in Settings, and strict mode leaves that page as soon as it opens. Turning this off waits until the filter is off.") {
                SwitchRow("Prevent uninstall", s.preventUninstall && adminActive) { v ->
                    if (v) askAdmin.launch(Permissions.adminIntent(context))
                    else vm.update { it.copy(preventUninstall = false) }.also { if (!s.focus.enabled) { Permissions.removeAdmin(context); refreshAdmin += 1 } }
                }
                if (s.preventUninstall && !adminActive) Text("The admin was turned off in Settings. Switch it on again here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Appear(3) {
            SectionCard("Appearance") {
                ChoiceRow("Theme", s.theme, Settings.THEMES, text = { it.replaceFirstChar { c -> c.uppercase() } }) { v -> vm.update { it.copy(theme = v) } }
            }
        }
        Appear(4) {
            SectionCard("Setup and permissions", "The accessibility service and the overlay are required; the rest make the app better.") {
                OutlinedButton(onClick = onSetup) { Text("Check permissions") }
            }
        }
        Appear(5) {
            SectionCard("About") {
                Text("AntiBrainrot ${BuildConfig.VERSION_NAME}. Open source, MIT. No accounts, no analytics, no network calls of its own.", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/danieltyukov/anti-brainrot"))) }) { Text("Source on GitHub") }
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://danieltyukov.github.io/anti-brainrot/"))) }) { Text("Website") }
            }
        }
        if (BuildConfig.DEBUG) Appear(6) {
            SectionCard("Debug build", "Not in the release. Fills sixty days of made-up history so the Progress screen can be checked.") {
                OutlinedButton(onClick = { vm.seedHistory() }) { Text("Seed sample history") }
            }
        }
    }
}
