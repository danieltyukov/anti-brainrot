package io.github.danieltyukov.antibrainrot.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.danieltyukov.antibrainrot.core.DayRecord
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Progress
import io.github.danieltyukov.antibrainrot.core.Rules
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.Appear
import io.github.danieltyukov.antibrainrot.ui.AppViewModel
import io.github.danieltyukov.antibrainrot.ui.ChoiceRow
import io.github.danieltyukov.antibrainrot.ui.InlineChoice
import io.github.danieltyukov.antibrainrot.ui.Permissions
import io.github.danieltyukov.antibrainrot.ui.Ring
import io.github.danieltyukov.antibrainrot.ui.SectionCard
import io.github.danieltyukov.antibrainrot.ui.animatedInt
import io.github.danieltyukov.antibrainrot.ui.delayLabel
import io.github.danieltyukov.antibrainrot.ui.theme.DeepAccent
import io.github.danieltyukov.antibrainrot.ui.theme.OnGradient
import io.github.danieltyukov.antibrainrot.ui.theme.Theme
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: AppViewModel, s: Settings, l: LocalState?, onProgress: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp)) {
        if (!Permissions.accessibilityRunning()) Appear(0) {
            SectionCard("Service not running", "Nothing is being blocked right now. Open Setup under More and switch the accessibility service off and on, or restart the phone.") {}
        }
        Appear(0) { FilterCard(vm, s) }
        Appear(1) { TodayStrip(l, onProgress) }
        Appear(2) { LockedHoursCard(vm, s) }
        Appear(3) {
            SectionCard("Always on") {
                Text("YouTube Shorts are closed the moment they open, inside the YouTube app, whether the filter is on or off. Instagram Reels, Facebook Reels and Snapchat Spotlight follow the switches under Apps.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private enum class Mode { Off, On, Countdown }

// The hero card: the filter's state, the way to change it, and the friction
// timer. Colours cross-fade between the off (surface) and on (gradient) looks.
@Composable
private fun FilterCard(vm: AppViewModel, s: Settings) {
    val locked = s.focus.enabled && Rules.isLockedNow(s)
    var countingDown by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(0) }
    var lockHours by remember { mutableIntStateOf(2) }
    var delay by remember(s.focus.unlockDelaySec) { mutableIntStateOf(s.focus.unlockDelaySec) }
    if (!s.focus.enabled && countingDown) countingDown = false
    val mode = when {
        !s.focus.enabled -> Mode.Off
        countingDown -> Mode.Countdown
        else -> Mode.On
    }
    val on = mode != Mode.Off

    // The friction timer: counts only while this screen is resumed. Leaving
    // the app, switching screens or turning the display off cancels it.
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(countingDown, lifecycle) {
        if (!countingDown) return@LaunchedEffect
        if (lifecycle != Lifecycle.State.RESUMED) {
            countingDown = false
            return@LaunchedEffect
        }
        remaining = s.focus.unlockDelaySec
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        countingDown = false
        vm.update { it.copy(focus = it.focus.copy(enabled = false)) }
    }

    val container by animateColorAsState(if (on) DeepAccent else MaterialTheme.colorScheme.surface, tween(450), label = "card")
    val fg by animateColorAsState(if (on) Color.White else MaterialTheme.colorScheme.onSurface, tween(450), label = "fg")
    val gradientAlpha by animateFloatAsState(if (on) 1f else 0f, tween(450), label = "gradient")
    val fgMuted = fg.copy(alpha = 0.8f)
    val ringTrack = if (on) Color.White.copy(alpha = 0.25f) else Theme.extra.track
    val ringProgress = when (mode) {
        Mode.Off -> 0f
        Mode.On -> 1f
        Mode.Countdown -> if (s.focus.unlockDelaySec == 0) 0f else remaining.toFloat() / s.focus.unlockDelaySec
    }
    val title = when (mode) {
        Mode.Off -> "Filter is off"
        Mode.On -> "Filter is on"
        Mode.Countdown -> "Turning off"
    }
    val subtitle = when {
        mode == Mode.Countdown -> "Keep this screen open. Leaving cancels."
        mode == Mode.Off -> "YouTube Shorts stay blocked either way."
        locked -> "Locked until ${Rules.lockedUntilText(s)}. Adding restrictions is still fine."
        else -> "Unlock delay ${delayLabel(s.focus.unlockDelaySec).lowercase()}. Add restrictions any time; removing one needs the filter off."
    }

    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .drawBehind { drawRect(OnGradient, alpha = gradientAlpha) }
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(ringProgress, Modifier.size(68.dp), stroke = 5.dp, color = if (on) Color.White else MaterialTheme.colorScheme.primary, track = ringTrack) {
                    AnimatedContent(targetState = mode == Mode.Countdown, label = "ringContent") { counting ->
                        if (counting) Text(remaining.toString(), style = MaterialTheme.typography.titleMedium, color = fg)
                        else Icon(if (locked) Icons.Rounded.Lock else Icons.Rounded.PowerSettingsNew, contentDescription = null, Modifier.size(28.dp), tint = fg)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    AnimatedContent(targetState = title, transitionSpec = { (fadeIn(tween(220)) + slideInVertically { it / 3 }) togetherWith (fadeOut(tween(120)) + slideOutVertically { -it / 3 }) }, label = "title") {
                        Text(it, style = MaterialTheme.typography.headlineSmall, color = fg)
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = fgMuted)
                }
            }
            if (on && s.focus.reason.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(s.focus.reason, fontStyle = FontStyle.Italic, color = fgMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = mode,
                transitionSpec = { (fadeIn(tween(260, delayMillis = 60)) + slideInVertically { it / 8 }) togetherWith fadeOut(tween(140)) },
                label = "mode",
            ) { m ->
                when (m) {
                    Mode.Off -> Column {
                        ChoiceRow("Unlock delay", delay, Settings.DELAY_CHOICES, text = { delayLabel(it) }) { delay = it }
                        Text(
                            "Turning the filter off again takes this long, and only while this screen stays open. Change any setting now; it applies once the filter is on.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.update { it.copy(focus = it.focus.copy(enabled = true, unlockDelaySec = delay)) } }, Modifier.fillMaxWidth()) { Text("Turn on") }
                    }
                    Mode.On -> Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Lock for", color = Color.White, modifier = Modifier.weight(1f))
                            InlineChoice(lockHours, Settings.LOCK_HOURS, contentColor = Color.White, text = { if (it == 1) "1 hour" else "$it hours" }) { lockHours = it }
                            OutlinedButton(
                                onClick = {
                                    val until = maxOf(s.focus.lockUntil, System.currentTimeMillis() + lockHours * 3600_000L)
                                    vm.update { it.copy(focus = it.focus.copy(lockUntil = until)) }
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            ) { Text("Lock") }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { if (s.focus.unlockDelaySec == 0) vm.update { it.copy(focus = it.focus.copy(enabled = false)) } else countingDown = true },
                            enabled = !locked,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White, contentColor = DeepAccent,
                                disabledContainerColor = Color.White.copy(alpha = 0.22f), disabledContentColor = Color.White.copy(alpha = 0.8f),
                            ),
                        ) { Text(if (locked) "Locked" else "Turn off") }
                    }
                    Mode.Countdown -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = remaining,
                            transitionSpec = { (fadeIn(tween(180)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(120)) + slideOutVertically { -it / 2 }) },
                            label = "seconds",
                        ) { Text(format(it), style = MaterialTheme.typography.displayMedium, color = Color.White) }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { countingDown = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepAccent),
                        ) { Text("Keep it on") }
                    }
                }
            }
        }
    }
}

// Three numbers for today; tapping opens Progress.
@Composable
private fun TodayStrip(l: LocalState?, onProgress: () -> Unit) {
    val rec = l?.history?.get(Passes.dayKey()) ?: DayRecord()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onProgress)
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat(Progress.focusText(animatedInt(rec.focusSeconds)), "Filter on today", Modifier.weight(1.2f))
        Stat(animatedInt(rec.blocks).toString(), "Blocks", Modifier.weight(0.8f))
        Stat(animatedInt(rec.feedsClosed).toString(), "Feeds closed", Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = "Progress", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private fun format(total: Int): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
