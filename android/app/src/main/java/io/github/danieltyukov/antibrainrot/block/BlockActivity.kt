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
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.ui.theme.Cream
import io.github.danieltyukov.antibrainrot.ui.theme.Ink
import io.github.danieltyukov.antibrainrot.ui.theme.Muted
import io.github.danieltyukov.antibrainrot.ui.theme.SkyGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Image
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
        val label = appLabel(pkg)
        val icon = appIcon(pkg)
        setContent { BlockScreen(pkg, label, icon, onHome = { goHome() }, onOpen = { openApp(pkg) }) }
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
        const val EXTRA_PACKAGE = "package"
    }
}

@Composable
private fun BlockScreen(pkg: String, label: String, icon: Drawable?, onHome: () -> Unit, onOpen: () -> Unit) {
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

    val mode = s?.apps?.mode ?: "pause"
    val canPass = s != null && l != null && Passes.canPass(l, s.apps, pkg)
    val cooldown = l?.let { Passes.cooldownUntil(it, pkg) }
    val total = s?.apps?.pauseSeconds ?: 10

    // Countdown only advances while this screen is resumed; leaving resets it.
    LaunchedEffect(lifecycle, total, mode, canPass) {
        if (mode != "pause" || !canPass) return@LaunchedEffect
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
    LaunchedEffect(l, s) {
        if (s != null && l != null && Passes.activePass(l, pkg) != null) onOpen()
    }

    Box(Modifier.fillMaxSize().background(SkyGradient), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xB8FFFFFF)).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ANTI-BRAINROT", color = Muted, fontSize = 12.sp, letterSpacing = 2.sp)
            if (!s?.focus?.reason.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(s!!.focus.reason, color = Muted, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            icon?.let { Image(BitmapPainter(it.toBitmap(128, 128).asImageBitmap()), contentDescription = null, Modifier.size(56.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(if (mode == "pause" && canPass) "Take a breath" else "Not now", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("$label is on your blocked list.", color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            when {
                mode == "block" -> Text("To open it, turn the filter off in Anti-Brainrot and wait out your unlock delay.", color = Muted, textAlign = TextAlign.Center)
                cooldown != null -> Text("Cooling down. The next pass for this app opens at ${timeText(cooldown)}.", color = Muted, textAlign = TextAlign.Center)
                !canPass -> Text("Your daily budget for passes is used up. It resets at midnight.", color = Muted, textAlign = TextAlign.Center)
                else -> {
                    Text(if (remaining > 0) "$remaining" else if (remaining == 0) "Ready" else "Come back to this screen", color = Ink, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
                    if (s?.apps?.intention == true) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(intention, { intention = it }, Modifier.fillMaxWidth(), label = { Text("What do you need there?") }, singleLine = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Budget left today: ${l?.let { Passes.budgetLeft(it, s!!.apps) } ?: 0} minutes.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    val ready = remaining == 0 && (s?.apps?.intention != true || intention.trim().length >= 3)
                    Button(
                        onClick = {
                            scope.launch {
                                val current = app.local.get()
                                val st = app.settings.get()
                                if (Passes.canPass(current, st.apps, pkg)) {
                                    app.local.update { Passes.grant(it, st.apps, pkg) }
                                    onOpen()
                                } else {
                                    message = "Could not start a pass."
                                }
                            }
                        },
                        enabled = ready,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Cream),
                    ) { Text("Continue for ${s?.apps?.passMinutes ?: 5} minutes") }
                }
            }
            if (message.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(message, color = Muted) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = onHome) { Text("Keep it blocked", color = Ink) }
            }
        }
    }
}

private fun timeText(ms: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))
