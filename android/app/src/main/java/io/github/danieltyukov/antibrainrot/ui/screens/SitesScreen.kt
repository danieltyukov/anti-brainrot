package io.github.danieltyukov.antibrainrot.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.core.Domains
import io.github.danieltyukov.antibrainrot.core.Presets
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.service.DnsVpnService
import io.github.danieltyukov.antibrainrot.service.Enforcer
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow

@Composable
fun SitesScreen(vm: AppViewModel, s: Settings) {
    val context = LocalContext.current
    var custom by remember(s.sites.custom) { mutableStateOf(s.sites.custom.joinToString("\n")) }
    var allowed by remember(s.sites.allowed) { mutableStateOf(s.sites.allowed.joinToString("\n")) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) Enforcer.syncSiteFilter(context, s)
        else vm.message.value = "The site filter needs the VPN permission to work."
    }

    fun turnOn(change: (Settings) -> Settings) {
        vm.update(change)
        val next = Settings.normalize(change(s))
        val intent = Enforcer.syncSiteFilter(context, next)
        if (intent != null) consent.launch(intent)
    }

    val privateDns = io.github.danieltyukov.antibrainrot.ui.Permissions.privateDnsHostname(context)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        if (privateDns != null) {
            SectionCard("Private DNS is on", "Android is set to use $privateDns for name lookups, which bypasses this filter. Set Private DNS to Off or Automatic under Network settings.") {}
        }
        SectionCard("Site filter", "A local DNS filter on this device. Only name lookups pass through it; nothing leaves the phone through this app. Status: ${if (DnsVpnService.running) "running" else "off"}.") {
            SwitchRow("Block adult sites", s.sites.adult, hint = "15,000 domains plus keyword rules, shipped inside the app") { v -> turnOn { it.copy(sites = it.sites.copy(adult = v)) } }
            SwitchRow("Block distracting sites", s.sites.distracting, hint = "The presets below plus your own") { v -> turnOn { it.copy(sites = it.sites.copy(distracting = v)) } }
        }
        SectionCard("Distracting sites", "Whole hosts, because a DNS filter cannot see paths. Block the apps themselves under Apps.") {
            Presets.ALL.forEach { preset ->
                val on = preset.id in s.sites.presets
                SwitchRow(preset.label, on) { v ->
                    val next = if (v) s.sites.presets + preset.id else s.sites.presets - preset.id
                    vm.update { it.copy(sites = it.sites.copy(presets = next)) }
                }
            }
        }
        SectionCard("Your own", "One host per line. Subdomains are included.") {
            OutlinedTextField(custom, { custom = it }, Modifier.fillMaxWidth(), label = { Text("Also block") }, minLines = 3)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(allowed, { allowed = it }, Modifier.fillMaxWidth(), label = { Text("Never block") }, minLines = 2)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.update { it.copy(sites = it.sites.copy(custom = Domains.parseList(custom), allowed = Domains.parseList(allowed))) } }, Modifier.fillMaxWidth()) { Text("Save sites") }
            Spacer(Modifier.height(8.dp))
            Text("Browsers with their own DNS over HTTPS setting bypass any local filter. Chrome follows the device DNS by default.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
