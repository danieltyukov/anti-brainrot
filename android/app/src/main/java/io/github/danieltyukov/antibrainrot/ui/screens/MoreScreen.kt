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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.BuildConfig
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.ChoiceRow
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow

@Composable
fun MoreScreen(vm: AppViewModel, s: Settings, l: LocalState?, onSetup: () -> Unit) {
    val context = LocalContext.current
    var reason by remember(s.focus.reason) { mutableStateOf(s.focus.reason) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        SectionCard("Today") {
            val stats = l?.stats
            val today = stats != null && stats.day == Passes.dayKey()
            Text("Blocked apps ${if (today) stats!!.blocks else 0} times, ${if (today) stats!!.passes else 0} passes used, Shorts and Reels closed ${if (today) stats!!.feedsClosed else 0} times, ${l?.let { Passes.budgetLeft(it, s.apps) } ?: s.apps.dailyBudgetMinutes} minutes of pass budget left.")
        }
        SectionCard("Your reason", "One line shown on every block screen. Written by you, for you.") {
            OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Thesis due in March.") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.update { it.copy(focus = it.focus.copy(reason = reason)) } }, Modifier.fillMaxWidth()) { Text("Save") }
        }
        SectionCard("Strict mode", "While the filter is on, the Settings pages that could disable Anti-Brainrot (its app info page, the accessibility page) are closed as soon as they open. Turning strict mode off waits until the filter is off.") {
            SwitchRow("Strict mode", s.strictMode) { v -> vm.update { it.copy(strictMode = v) } }
        }
        SectionCard("Appearance") {
            ChoiceRow("Theme", s.theme, Settings.THEMES, text = { it.replaceFirstChar { c -> c.uppercase() } }) { v -> vm.update { it.copy(theme = v) } }
        }
        SectionCard("Setup and permissions") {
            TextButton(onClick = onSetup) { Text("Check permissions") }
        }
        SectionCard("About") {
            Text("Anti-Brainrot ${BuildConfig.VERSION_NAME}. Open source, MIT. No accounts, no analytics, no network calls of its own.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/danieltyukov/anti-brainrot"))) }) { Text("Source on GitHub") }
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://danieltyukov.github.io/anti-brainrot/"))) }) { Text("Website") }
        }
    }
}
