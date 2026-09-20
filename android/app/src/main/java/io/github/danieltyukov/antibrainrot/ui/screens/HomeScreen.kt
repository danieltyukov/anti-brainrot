package io.github.danieltyukov.antibrainrot.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Rules
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.ChoiceRow
import io.github.danieltyukov.antibrainrot.ui.InlineChoice
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.count
import io.github.danieltyukov.antibrainrot.ui.delayLabel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: AppViewModel, s: Settings, l: LocalState?) {
    val locked = s.focus.enabled && Rules.isLockedNow(s)
    var countingDown by remember { mutableStateOf(false) }
    var lockHours by remember { mutableIntStateOf(2) }
    var delay by remember(s.focus.unlockDelaySec) { mutableIntStateOf(s.focus.unlockDelaySec) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        if (!io.github.danieltyukov.antibrainrot.ui.Permissions.accessibilityRunning()) {
            SectionCard("Service not running", "Nothing is being blocked right now. Open Setup under More and switch the accessibility service off and on, or restart the phone.") {}
        }
        if (!s.focus.enabled) {
            SectionCard("Filter is off", "YouTube Shorts stay blocked either way.") {
                ChoiceRow("Unlock delay", delay, Settings.DELAY_CHOICES, text = { delayLabel(it) }) { delay = it }
                Text(
                    "Turning the filter off again takes this long, and only while this screen stays open. Change any setting now; it applies once the filter is on.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { vm.update { it.copy(focus = it.focus.copy(enabled = true, unlockDelaySec = delay)) } }, Modifier.fillMaxWidth()) { Text("Turn on") }
            }
        } else if (countingDown) {
            TurnOffCountdown(seconds = s.focus.unlockDelaySec, onDone = {
                countingDown = false
                vm.update { it.copy(focus = it.focus.copy(enabled = false)) }
            }, onCancel = { countingDown = false })
        } else {
            SectionCard("Filter is on", if (locked) "Locked until ${Rules.lockedUntilText(s)}. It cannot be turned off before then. Adding restrictions is still fine." else "Unlock delay: ${delayLabel(s.focus.unlockDelaySec).lowercase()}. Add restrictions any time. Removing one needs the filter off.") {
                if (s.focus.reason.isNotBlank()) {
                    Text(s.focus.reason, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lock for", modifier = Modifier.weight(1f))
                    InlineChoice(lockHours, Settings.LOCK_HOURS, text = { if (it == 1) "1 hour" else "$it hours" }) { lockHours = it }
                    OutlinedButton(onClick = {
                        val until = maxOf(s.focus.lockUntil, System.currentTimeMillis() + lockHours * 3600_000L)
                        vm.update { it.copy(focus = it.focus.copy(lockUntil = until)) }
                    }) { Text("Lock") }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { if (s.focus.unlockDelaySec == 0) vm.update { it.copy(focus = it.focus.copy(enabled = false)) } else countingDown = true },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (locked) "Locked" else "Turn off") }
            }
        }
        val stats = l?.stats
        if (stats != null && stats.day == Passes.dayKey() && (stats.blocks > 0 || stats.passes > 0 || stats.feedsClosed > 0)) {
            SectionCard("Today") {
                Text("Blocked apps ${count(stats.blocks, "time")}, ${count(stats.passes, "pass", "passes")} used, Shorts and Reels closed ${count(stats.feedsClosed, "time")}.")
            }
        }
        SectionCard("Always on") {
            Text("YouTube Shorts are closed the moment they open, inside the YouTube app, whether the filter is on or off. Instagram Reels, Facebook Reels and Snapchat Spotlight follow the switches under Apps.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// The friction timer: counts only while this screen is resumed. Leaving the
// app, switching screens or turning the display off cancels it.
@Composable
private fun TurnOffCountdown(seconds: Int, onDone: () -> Unit, onCancel: () -> Unit) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var remaining by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(lifecycle) {
        if (lifecycle != Lifecycle.State.RESUMED) {
            onCancel()
            return@LaunchedEffect
        }
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        onDone()
    }
    SectionCard("Turning off in") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(format(remaining), fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Keep this screen open. Leaving cancels.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onCancel, Modifier.fillMaxWidth()) { Text("Keep it on") }
        }
    }
}

private fun format(total: Int): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
