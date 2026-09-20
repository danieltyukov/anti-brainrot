package io.github.danieltyukov.antibrainrot.block

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.core.Keys
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Progress
import io.github.danieltyukov.antibrainrot.service.Enforcer
import io.github.danieltyukov.antibrainrot.ui.minutesLabel
import io.github.danieltyukov.antibrainrot.ui.theme.Cream
import io.github.danieltyukov.antibrainrot.ui.theme.Ink
import io.github.danieltyukov.antibrainrot.ui.theme.Muted
import io.github.danieltyukov.antibrainrot.ui.theme.SkyGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import io.github.danieltyukov.antibrainrot.R
import io.github.danieltyukov.antibrainrot.ui.Ring
import io.github.danieltyukov.antibrainrot.ui.theme.AntiBrainrotTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// The block screen, shown over a blocked app. Block mode only offers a way
// out. Pause mode runs a countdown that pauses whenever this screen is not
// in front, takes an intention, and grants a timed pass from the budget.
class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val key = intent.getStringExtra(EXTRA_KEY) ?: pkg
        val label = if (Keys.isSite(key)) Keys.label(key) else appLabel(pkg)
        val icon = appIcon(pkg)
        setContent { BlockScreen(key, label, icon, onHome = { goHome() }, onOpen = { openApp(pkg) }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun openApp(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    private fun appIcon(pkg: String): Drawable? = try {
        packageManager.getApplicationIcon(pkg)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        // The app to return to: the blocked app, or the browser for a site.
        const val EXTRA_PACKAGE = "package"
        // The rule key: the package, or "site:" plus the rule host.
        const val EXTRA_KEY = "key"
    }
}

@Composable
private fun BlockScreen(key: String, label: String, icon: Drawable?, onHome: () -> Unit, onOpen: () -> Unit) {
    val app = App.instance
    val settings by app.settings.flow.collectAsState(initial = null)
    val local by app.local.flow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val s = settings
    val l = local
    var remaining by remember { mutableIntStateOf(-1) }
    var intention by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val rule = s?.let { Keys.ruleFor(it, key) }
    val mode = rule?.mode ?: "block"
    val left = if (l != null && rule != null) Passes.secondsLeft(l, rule, key) else 0
    val exhausted = mode == "timer" && left <= 0
    val canPass = s != null && l != null && Passes.canPass(l, s, key)
    val cooldown = l?.let { Passes.cooldownUntil(it, key) }
    val session = if (s != null && l != null) Passes.sessionSeconds(l, s, key) else 0
    val total = s?.apps?.pauseSeconds ?: 10

    // Countdown only advances while this screen is resumed; leaving resets it.
    LaunchedEffect(lifecycle, total, mode, canPass) {
        if (mode != "timer" || !canPass) return@LaunchedEffect
        if (lifecycle != Lifecycle.State.RESUMED) {
            remaining = -1
            return@LaunchedEffect
        }
        remaining = total
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    // A session is running and there is time left: let the app through.
    LaunchedEffect(l, s) {
        if (s != null && l != null && !Enforcer.isBlocked(s, l, key)) onOpen()
    }

    // The card slides up and the app icon pops once it is there.
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    var pop by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); pop = true }
    val iconScale by animateFloatAsState(if (pop) 1f else 0.5f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "icon")

    AntiBrainrotTheme("light") {
        Box(Modifier.fillMaxSize().background(SkyGradient), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visibleState = shown,
                enter = fadeIn(tween(320)) + slideInVertically(tween(440, easing = FastOutSlowInEasing)) { it / 6 } + scaleIn(tween(440, easing = FastOutSlowInEasing), initialScale = 0.94f),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xC4FFFFFF)).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_mark), contentDescription = null, Modifier.size(18.dp), tint = Ink)
                        Spacer(Modifier.size(6.dp))
                        Text("AntiBrainrot", color = Muted, style = MaterialTheme.typography.labelLarge)
                    }
                    if (!s?.focus?.reason.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(s!!.focus.reason, color = Muted, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(18.dp))
                    icon?.let {
                        Image(
                            BitmapPainter(it.toBitmap(128, 128).asImageBitmap()), contentDescription = null,
                            Modifier.size(60.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    val title = when {
                        exhausted -> "Time's up"
                        mode == "timer" && canPass -> "Take a breath"
                        else -> "Not now"
                    }
                    Text(title, color = Ink, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    val line = when {
                        exhausted -> "You have used your ${minutesLabel(rule!!.limitMinutes).lowercase()} in $label for today."
                        mode == "timer" -> "$label has a limit of ${minutesLabel(rule!!.limitMinutes).lowercase()} a day."
                        else -> "$label is blocked."
                    }
                    Text(line, color = Ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    when {
                        mode == "block" -> Text("To open it, turn the filter off in AntiBrainrot and wait out your unlock delay.", color = Muted, textAlign = TextAlign.Center)
                        exhausted -> Text("It resets at midnight.", color = Muted, textAlign = TextAlign.Center)
                        cooldown != null -> Text("Cooling down. The next session opens at ${timeText(cooldown)}.", color = Muted, textAlign = TextAlign.Center)
                        !canPass -> Text("No session is possible right now.", color = Muted, textAlign = TextAlign.Center)
                        else -> {
                            val progress = if (remaining < 0 || total == 0) 0f else 1f - remaining.toFloat() / total
                            Ring(progress, Modifier.size(124.dp), stroke = 8.dp, color = Ink, track = Ink.copy(alpha = 0.1f)) {
                                AnimatedContent(
                                    targetState = remaining,
                                    transitionSpec = { (fadeIn(tween(180)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(120)) + slideOutVertically { -it / 2 }) },
                                    label = "seconds",
                                ) { r ->
                                    when {
                                        r > 0 -> Text("$r", color = Ink, style = MaterialTheme.typography.displayMedium)
                                        r == 0 -> Text("Ready", color = Ink, style = MaterialTheme.typography.titleLarge)
                                        else -> Text("Paused", color = Muted, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                            if (remaining < 0) {
                                Spacer(Modifier.height(6.dp))
                                Text("Come back to this screen to continue the countdown.", color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                            if (s?.apps?.intention == true) {
                                Spacer(Modifier.height(14.dp))
                                OutlinedTextField(intention, { intention = it }, Modifier.fillMaxWidth(), label = { Text("What do you need there?") }, singleLine = true, shape = MaterialTheme.shapes.medium)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("${Progress.focusText(left)} left today.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            val ready = remaining == 0 && (s?.apps?.intention != true || intention.trim().length >= 3)
                            Button(
                                onClick = {
                                    scope.launch {
                                        val current = app.local.get()
                                        val st = app.settings.get()
                                        if (Passes.canPass(current, st, key)) {
                                            app.local.update { Passes.grant(it, st, key) }
                                            onOpen()
                                        } else {
                                            message = "Could not start a pass."
                                        }
                                    }
                                },
                                enabled = ready,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Cream, disabledContainerColor = Ink.copy(alpha = 0.12f), disabledContentColor = Muted),
                            ) { Text("Continue for ${Progress.focusText(session)}") }
                        }
                    }
                    if (message.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(message, color = Muted) }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(onClick = onHome, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text("Keep it blocked") }
                }
            }
        }
    }
}

private fun timeText(ms: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
