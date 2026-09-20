package io.github.danieltyukov.antibrainrot.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.SwitchRow

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// The weekly schedule, shown on Home under the filter card.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LockedHoursCard(vm: AppViewModel, s: Settings) {
    val context = LocalContext.current
    fun pick(current: String, onPicked: (String) -> Unit) {
        val (h, m) = current.split(":").map { it.toInt() }
        TimePickerDialog(context, { _, hh, mm -> onPicked("%02d:%02d".format(hh, mm)) }, h, m, true).show()
    }
    SectionCard("Locked hours", "During these hours the filter turns itself on and cannot be turned off, not even with the countdown.") {
        SwitchRow("Enable locked hours", s.schedule.enabled) { v -> vm.update { it.copy(schedule = it.schedule.copy(enabled = v)) } }
        AnimatedVisibility(s.schedule.enabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(Modifier.height(8.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        val on = day in s.schedule.days
                        FilterChip(selected = on, onClick = {
                            val next = if (on) s.schedule.days - day else s.schedule.days + day
                            vm.update { it.copy(schedule = it.schedule.copy(days = next)) }
                        }, label = { Text(DAY_NAMES[day - 1]) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { pick(s.schedule.start) { v -> vm.update { it.copy(schedule = it.schedule.copy(start = v)) } } }) { Text("From ${s.schedule.start}") }
                    OutlinedButton(onClick = { pick(s.schedule.end) { v -> vm.update { it.copy(schedule = it.schedule.copy(end = v)) } } }) { Text("Until ${s.schedule.end}") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Widening the window is immediate. Narrowing it, or removing a day, waits until the filter is off.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
