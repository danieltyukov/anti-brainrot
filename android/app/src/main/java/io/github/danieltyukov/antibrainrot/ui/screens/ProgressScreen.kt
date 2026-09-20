package io.github.danieltyukov.antibrainrot.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.danieltyukov.antibrainrot.core.Keys
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Progress
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.Appear
import io.github.danieltyukov.antibrainrot.ui.BarChart
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.StatTile
import io.github.danieltyukov.antibrainrot.ui.animatedInt
import io.github.danieltyukov.antibrainrot.ui.count
import io.github.danieltyukov.antibrainrot.ui.theme.Amber
import io.github.danieltyukov.antibrainrot.ui.theme.Leaf
import io.github.danieltyukov.antibrainrot.ui.theme.Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

private val RANGES = listOf(7, 30, 90)

// Progress over time: the day history as tiles, bars, a streak and the apps
// that needed the most block screens.
@Composable
fun ProgressScreen(s: Settings, l: LocalState?) {
    var range by rememberSaveable { mutableIntStateOf(7) }
    val today = remember { LocalDate.now() }
    val history = l?.history ?: emptyMap()
    val days = remember(history, range) { Progress.days(history, range, today) }
    val summary = remember(days) { Progress.summary(days) }
    val streak = remember(history) { Progress.streak(history, today) }
    val best = remember(history) { Progress.bestStreak(history) }
    val top = remember(days) { Progress.topApps(days) }
    val topUsage = remember(days) { Progress.topUsage(days) }
    val labels = remember(days) { labelsFor(days) }
    val empty = remember(history) { history.values.all { it.focusSeconds == 0 && it.blocks == 0 && it.usage.isEmpty() && it.passes == 0 } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp)) {
        Appear(0) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)) {
                RANGES.forEachIndexed { i, r ->
                    SegmentedButton(selected = range == r, onClick = { range = r }, shape = SegmentedButtonDefaults.itemShape(i, RANGES.size)) { Text("$r days") }
                }
            }
        }
        if (empty) Appear(1) {
            SectionCard("Nothing yet", "The counters fill in as the filter runs: how long it was on, block screens, time in timed apps, sessions. Check back tomorrow.") {}
        }
        Appear(1) {
            SectionCard("Last $range days") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Progress.focusText(animatedInt(summary.focusSeconds)), "Filter on", Modifier.weight(1f))
                    StatTile(animatedInt(summary.blocks).toString(), "Block screens", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Progress.focusText(animatedInt(summary.usageSeconds)), "In timed apps", Modifier.weight(1f), accent = Amber)
                    StatTile(animatedInt(summary.passes).toString(), "Sessions, ${summary.passMinutes} min", Modifier.weight(1f))
                }
            }
        }
        Appear(2) {
            SectionCard("Filter on", "Hours per day the filter was on while the service ran.") {
                BarChart(days.map { it.record.focusSeconds / 3600f }, labels)
                Spacer(Modifier.height(6.dp))
                Text("Best day: ${Progress.focusText(days.maxOf { it.record.focusSeconds })}. ${count(summary.activeDays, "day")} with an hour or more.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Appear(3) {
            SectionCard("Time in timed apps and sites", "Hours per day spent in apps and sites with a daily timer, counted only while they are in front.") {
                BarChart(days.map { it.record.usage.values.sum() / 3600f }, labels, primaryColor = Amber)
                Spacer(Modifier.height(6.dp))
                Text("Most in one day: ${Progress.focusText(days.maxOf { it.record.usage.values.sum() })}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Appear(4) {
            SectionCard("Block screens", "Times a blocked or out-of-time app was stopped, per day.") {
                BarChart(days.map { it.record.blocks.toFloat() }, labels)
            }
        }
        Appear(5) { StreakCard(streak, best, days.takeLast(7)) }
        if (topUsage.isNotEmpty()) Appear(6) {
            SectionCard("Most used timed apps and sites", "Time in front in the last $range days.") { TopApps(topUsage) { Progress.focusText(it) } }
        }
        if (top.isNotEmpty()) Appear(7) {
            SectionCard("Most blocked", "Block screens in the last $range days.") { TopApps(top) { it.toString() } }
        }
    }
}

private fun labelsFor(days: List<Progress.Day>): List<String> {
    val n = days.size
    val fmt = DateTimeFormatter.ofPattern("d MMM")
    return days.mapIndexed { i, d ->
        val fromEnd = n - 1 - i
        when {
            n <= 7 -> d.date.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault())
            n <= 31 -> if (fromEnd % 7 == 0) fmt.format(d.date) else ""
            else -> if (fromEnd % 30 == 0) fmt.format(d.date) else ""
        }
    }
}

@Composable
private fun StreakCard(streak: Int, best: Int, lastWeek: List<Progress.Day>) {
    SectionCard("Streak") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(animatedInt(streak).toString(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(if (streak == 1) "day in a row" else "days in a row", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text("Days with the filter on for at least an hour. Best: ${count(best, "day")}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            lastWeek.forEach { d ->
                val hit = d.record.focusSeconds >= Progress.STREAK_MIN_SECONDS
                val color by animateColorAsState(if (hit) Leaf else Theme.extra.track, tween(400), label = "dot")
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.height(4.dp))
                    Text(d.date.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TopApps(top: List<Pair<String, Int>>, format: (Int) -> String) {
    val pm = LocalContext.current.packageManager
    val max = top.first().second.toFloat().coerceAtLeast(1f)
    top.forEachIndexed { i, (key, n) ->
        val site = Keys.isSite(key)
        val label = remember(key) {
            if (site) Keys.label(key) else try { pm.getApplicationLabel(pm.getApplicationInfo(key, 0)).toString() } catch (e: Exception) { key }
        }
        val icon = remember(key) { if (site) null else try { pm.getApplicationIcon(key).toBitmap(96, 96).asImageBitmap() } catch (e: Exception) { null } }
        val fraction by animateFloatAsState(n / max, tween(700, delayMillis = i * 60, easing = FastOutSlowInEasing), label = "bar")
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                icon != null -> Image(icon, contentDescription = null, Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                site -> Icon(Icons.Rounded.Language, contentDescription = null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Spacer(Modifier.size(32.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(format(n), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Theme.extra.track)) {
                    Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}
