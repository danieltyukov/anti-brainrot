package io.github.danieltyukov.antibrainrot.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.core.Domains
import io.github.danieltyukov.antibrainrot.core.Presets
import io.github.danieltyukov.antibrainrot.core.Rule
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.service.DnsVpnService
import io.github.danieltyukov.antibrainrot.service.Enforcer
import io.github.danieltyukov.antibrainrot.ui.Appear
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.Permissions
import io.github.danieltyukov.antibrainrot.ui.RulePill
import io.github.danieltyukov.antibrainrot.ui.RuleSheet
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow
import io.github.danieltyukov.antibrainrot.ui.ruleText

// Sites get the same rules as apps. Blocked ones are also answered at the
// DNS level; timed ones are watched in the browser's address bar.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SitesScreen(vm: AppViewModel, s: Settings) {
    val context = LocalContext.current
    var newHost by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<String?>(null) }
    var allowed by remember(s.sites.allowed) { mutableStateOf(s.sites.allowed.joinToString("\n")) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) Enforcer.syncSiteFilter(context, s)
        else vm.message.value = "The site filter needs the VPN permission to work."
    }

    // Changes that may need the DNS filter running ask for VPN consent.
    fun change(mutation: (Settings) -> Settings) {
        vm.update(mutation)
        val next = Settings.normalize(mutation(s))
        val intent = Enforcer.syncSiteFilter(context, next)
        if (intent != null) consent.launch(intent)
    }
    fun setRule(host: String, rule: Rule?) = change { it.copy(sites = it.sites.copy(rules = if (rule == null) it.sites.rules - host else it.sites.rules + (host to rule))) }

    val rules = s.sites.rules
    val sorted = rules.entries.sortedWith(compareBy<Map.Entry<String, Rule>> { if (it.value.mode == "block") 0 else 1 }.thenBy { it.key })
    val suggestions = Presets.ALL.filter { p -> p.hosts.none { h -> rules.keys.any { Domains.matches(it, h) || Domains.matches(h, it) } } }
    val privateDns = Permissions.privateDnsHostname(context)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp)) {
        if (privateDns != null) Appear(0) {
            SectionCard("Private DNS is on", "Android is set to use $privateDns for name lookups, which bypasses the DNS part of the filter. Set Private DNS to Off or Automatic under Network settings.") {}
        }
        Appear(0) {
            SectionCard("Your sites", "Each site gets its own rule, blocked outright or a daily timer, like an app. Subdomains follow the rule. Sites are caught in the address bar of Chrome, Firefox, Samsung Internet, Brave, Edge, Opera, Vivaldi and DuckDuckGo; blocked sites are also stopped at the DNS level in every app.") {
                sorted.forEach { (host, rule) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = host }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Language, contentDescription = null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Presets.byHost(host)?.label ?: host, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (Presets.byHost(host) != null) "$host, ${ruleText(rule).lowercase()}" else ruleText(rule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        RulePill(rule)
                    }
                }
                if (sorted.isEmpty()) Text("No sites yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newHost, { newHost = it }, Modifier.weight(1f), placeholder = { Text("reddit.com") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                    Button(
                        onClick = {
                            val host = Domains.normalize(newHost)
                            // Opens the editor without a rule, so Block or
                            // Timer is the first choice, not a loosening.
                            if (host == null) vm.message.value = "That does not look like a site. Try something like reddit.com."
                            else {
                                newHost = ""
                                editing = host
                            }
                        },
                        enabled = newHost.isNotBlank(),
                    ) { Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add") }
                }
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { p ->
                            AssistChip(onClick = { editing = p.hosts.first() }, label = { Text(p.label) })
                        }
                    }
                }
            }
        }
        Appear(1) {
            SectionCard("Adult sites", "A bundled list of 15,000 domains plus keyword rules, answered at the DNS level. Status: ${if (DnsVpnService.running) "filter running" else "filter off"}.") {
                SwitchRow("Block adult sites", s.sites.adult) { v -> change { it.copy(sites = it.sites.copy(adult = v)) } }
            }
        }
        Appear(2) {
            SectionCard("Never block", "One host per line. Wins over the adult list and over blocked sites at the DNS level.") {
                OutlinedTextField(allowed, { allowed = it }, Modifier.fillMaxWidth(), minLines = 2, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.update { it.copy(sites = it.sites.copy(allowed = Domains.parseList(allowed))) } }, Modifier.fillMaxWidth(), enabled = Domains.parseList(allowed) != s.sites.allowed) { Text("Save") }
                Spacer(Modifier.height(8.dp))
                Text("A DNS filter sees hosts, not paths, and browsers with their own DNS over HTTPS setting bypass it. The address bar check does not depend on DNS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    editing?.let { host ->
        RuleSheet(Presets.byHost(host)?.label ?: host, rules[host], icon = { Icon(Icons.Rounded.Language, contentDescription = null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }, onDismiss = { editing = null }) { rule ->
            setRule(host, rule)
            if (rule == null) editing = null
        }
    }
}
