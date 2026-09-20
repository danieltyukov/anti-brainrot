package io.github.danieltyukov.antibrainrot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.danieltyukov.antibrainrot.ui.theme.Theme
import kotlin.math.max
import kotlin.math.min

// A card with the app's corner radius, no shadow, that animates its size
// when content appears or disappears inside it.
@Composable
fun SectionCard(title: String? = null, subtitle: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp).animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (title != null || subtitle != null) Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

// Entrance for a block of content: fades and drifts up, staggered by index.
@Composable
fun Appear(index: Int = 0, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val delay = min(index * 55, 330)
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(300, delayMillis = delay)) + slideInVertically(tween(380, delayMillis = delay, easing = FastOutSlowInEasing)) { it / 8 },
        exit = fadeOut(),
    ) { content() }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, hint: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = 7.dp),
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
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        InlineChoice(value, choices, enabled, text = text, onChange = onChange)
    }
}

// A dropdown that only takes the width of its label, for use inside rows.
@Composable
fun <T> InlineChoice(
    value: T,
    choices: List<T>,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    text: (T) -> String,
    onChange: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = true }, enabled = enabled, colors = ButtonDefaults.textButtonColors(contentColor = contentColor)) { Text(text(value)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = MaterialTheme.shapes.medium) {
            choices.forEach { c -> DropdownMenuItem(text = { Text(text(c)) }, onClick = { open = false; onChange(c) }) }
        }
    }
}

// A number that counts up or down to its new value.
@Composable
fun animatedInt(target: Int): Int {
    val v by animateIntAsState(target, tween(650, easing = FastOutSlowInEasing), label = "count")
    return v
}

// A value with a small caption, on a tinted tile.
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Column(modifier.clip(MaterialTheme.shapes.medium).background(Theme.extra.tileBackground).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// A progress ring with content in the middle. Progress changes animate.
@Composable
fun Ring(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = Theme.extra.track,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arc = Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
            if (p > 0f) drawArc(color, -90f, 360f * p, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

// Bars that grow in when the data changes. `secondary` stacks on top of
// `primary`. Empty labels are skipped, so callers thin them out.
@Composable
fun BarChart(
    primary: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier.fillMaxWidth().height(170.dp),
    secondary: List<Float>? = null,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = Theme.extra.chartSecondary,
) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(primary, secondary) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val track = Theme.extra.track
    val maxV = max(primary.indices.maxOfOrNull { primary[it] + (secondary?.getOrNull(it) ?: 0f) } ?: 0f, 0.0001f)
    Canvas(modifier) {
        val n = primary.size
        if (n == 0) return@Canvas
        val labelH = 20.dp.toPx()
        val chartH = size.height - labelH
        val slot = size.width / n
        val barW = min(slot * 0.64f, 26.dp.toPx())
        val radius = CornerRadius(barW / 2.5f)
        for (i in 0 until n) {
            val x = i * slot + (slot - barW) / 2
            drawRoundRect(track, Offset(x, 0f), Size(barW, chartH), radius)
            val p = primary[i]
            val s = secondary?.getOrNull(i) ?: 0f
            val total = (p + s) / maxV * chartH * grow.value
            if (total > 0f) {
                if (s > 0f) drawRoundRect(secondaryColor, Offset(x, chartH - total), Size(barW, total), radius)
                val ph = if (s > 0f) total * (p / (p + s)) else total
                if (ph > 0f) drawRoundRect(primaryColor, Offset(x, chartH - ph), Size(barW, ph), radius)
            }
            val label = labels.getOrNull(i).orEmpty()
            if (label.isNotEmpty()) {
                val t = measurer.measure(label, labelStyle)
                drawText(t, topLeft = Offset(x + barW / 2 - t.size.width / 2, chartH + (labelH - t.size.height) / 2 + 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// "1 pass", "2 passes", "3 times".
fun count(n: Int, one: String, many: String = one + "s"): String = "$n ${if (n == 1) one else many}"

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
