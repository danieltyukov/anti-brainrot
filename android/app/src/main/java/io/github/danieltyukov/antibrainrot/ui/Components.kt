package io.github.danieltyukov.antibrainrot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionCard(title: String? = null, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (title != null || subtitle != null) Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, hint: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = { onChange(it) }, enabled = enabled)
    }
}

@Composable
fun <T> ChoiceRow(label: String, value: T, choices: List<T>, enabled: Boolean = true, text: (T) -> String, onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Column {
            TextButton(onClick = { open = true }, enabled = enabled) { Text(text(value)) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                choices.forEach { c ->
                    DropdownMenuItem(text = { Text(text(c)) }, onClick = { open = false; onChange(c) })
                }
            }
        }
    }
}

// A dropdown that only takes the width of its label, for use inside rows.
@Composable
fun <T> InlineChoice(value: T, choices: List<T>, enabled: Boolean = true, text: (T) -> String, onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = true }, enabled = enabled) { Text(text(value)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { c -> DropdownMenuItem(text = { Text(text(c)) }, onClick = { open = false; onChange(c) }) }
        }
    }
}

fun minutesLabel(minutes: Int): String = when {
    minutes == 0 -> "None"
    minutes >= 1440 -> "Unlimited"
    minutes % 60 == 0 -> if (minutes == 60) "1 hour" else "${minutes / 60} hours"
    minutes == 1 -> "1 minute"
    else -> "$minutes minutes"
}

fun delayLabel(seconds: Int): String = when {
    seconds == 0 -> "Instant"
    seconds % 3600 == 0 -> if (seconds == 3600) "1 hour" else "${seconds / 3600} hours"
    seconds % 60 == 0 -> if (seconds == 60) "1 minute" else "${seconds / 60} minutes"
    else -> "$seconds seconds"
}
