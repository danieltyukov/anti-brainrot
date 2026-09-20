package io.github.danieltyukov.antibrainrot.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.core.Rule
import io.github.danieltyukov.antibrainrot.core.Settings

fun shortMinutes(m: Int) = if (m % 60 == 0) "${m / 60} h" else "$m min"

fun ruleText(rule: Rule?): String = when (rule?.mode) {
    "block" -> "Blocked"
    "timer" -> "${minutesLabel(rule.limitMinutes)} a day"
    else -> "No rule"
}

// The small tag at the end of an app or site row.
@Composable
fun RulePill(rule: Rule?) {
    val scheme = MaterialTheme.colorScheme
    val (text, bg, fg) = when (rule?.mode) {
        "block" -> Triple("Block", scheme.onSurface, scheme.surface)
        "timer" -> Triple(shortMinutes(rule.limitMinutes), scheme.primaryContainer, scheme.onPrimaryContainer)
        else -> Triple("Add", Color.Transparent, scheme.primary)
    }
    val outline = if (rule == null) Modifier.border(1.dp, scheme.outline, CircleShape) else Modifier
    Box(Modifier.clip(CircleShape).background(bg).then(outline).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

// The rule editor for one app or site: changes apply as they are tapped;
// refusals show as a message and the sheet keeps showing the stored rule.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleSheet(title: String, rule: Rule?, icon: @Composable () -> Unit, onDismiss: () -> Unit, onChange: (Rule?) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mode = rule?.mode ?: "off"
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(ruleText(rule), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(20.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("off" to "No rule", "block" to "Block", "timer" to "Timer").forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = {
                            onChange(when (m) {
                                "off" -> null
                                "block" -> Rule("block", rule?.limitMinutes ?: 30)
                                else -> Rule("timer", rule?.limitMinutes ?: 30)
                            })
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(label) }
                }
            }
            Spacer(Modifier.height(16.dp))
            AnimatedContent(targetState = mode, label = "ruleHelp") { m ->
                when (m) {
                    "block" -> Text("Blocked outright while the filter is on. Only turning the filter off, after its delay, opens it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    "timer" -> Column {
                        Text("Minutes a day", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Settings.LIMIT_CHOICES.forEach { minutes ->
                                FilterChip(selected = rule?.limitMinutes == minutes, onClick = { onChange(Rule("timer", minutes)) }, label = { Text(shortMinutes(minutes)) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Opens after the pause for one session at a time. Only time in front counts. When the minutes are gone it stays blocked until midnight.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> Text("Not blocked and not timed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Adding a rule or tightening one applies now. Removing or loosening one waits until the filter is off.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
