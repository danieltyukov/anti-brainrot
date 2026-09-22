package io.github.danieltyukov.antibrainrot.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danieltyukov.antibrainrot.R
import io.github.danieltyukov.antibrainrot.core.Rules
import io.github.danieltyukov.antibrainrot.core.Settings
import io.github.danieltyukov.antibrainrot.ui.screens.AppsScreen
import io.github.danieltyukov.antibrainrot.ui.screens.HomeScreen
import io.github.danieltyukov.antibrainrot.ui.screens.MoreScreen
import io.github.danieltyukov.antibrainrot.ui.screens.ProgressScreen
import io.github.danieltyukov.antibrainrot.ui.screens.SetupScreen
import io.github.danieltyukov.antibrainrot.ui.screens.SitesScreen
import io.github.danieltyukov.antibrainrot.ui.theme.AntiBrainrotTheme
import io.github.danieltyukov.antibrainrot.ui.theme.Theme

enum class Tab(val label: String) { Home("Home"), Progress("Progress"), Apps("Apps"), Sites("Sites"), More("More") }

@Composable
fun AppRoot() {
    val vm: AppViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val local by vm.local.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val s = settings ?: return
    var tab by remember { mutableStateOf(Tab.Home) }
    var showSetup by remember { mutableStateOf(!s.onboarded || !Permissions.accessibilityEnabled(context) || !Permissions.overlayGranted(context)) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        vm.clearMessage()
    }

    AntiBrainrotTheme(s.theme) {
        Box(Modifier.fillMaxSize().background(Theme.extra.pageGradient)) {
            // The container is transparent so the gradient shows through; the
            // content colour has to be set by hand or text outside cards
            // falls back to black, invisible in the dark theme.
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                topBar = { Header(s, showSetup) },
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    AnimatedVisibility(!showSetup, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                            Tab.entries.forEach { t ->
                                val selected = tab == t
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { tab = t },
                                    icon = { Icon(tabIcon(t, selected), contentDescription = t.label) },
                                    label = { Text(t.label) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    AnimatedContent(
                        targetState = if (showSetup) null else tab,
                        transitionSpec = {
                            val forward = (targetState?.ordinal ?: -1) > (initialState?.ordinal ?: -1)
                            val enter = fadeIn(tween(240, delayMillis = 30)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { if (forward) it / 14 else -it / 14 }
                            val exit = fadeOut(tween(150)) + slideOutHorizontally(tween(340, easing = FastOutSlowInEasing)) { if (forward) -it / 14 else it / 14 }
                            enter.togetherWith(exit).using(SizeTransform(clip = false))
                        },
                        contentAlignment = Alignment.TopStart,
                        label = "screen",
                    ) { t ->
                        when (t) {
                            null -> SetupScreen(vm) { showSetup = false }
                            Tab.Home -> HomeScreen(vm, s, local) { tab = Tab.Progress }
                            Tab.Progress -> ProgressScreen(s, local)
                            Tab.Apps -> AppsScreen(vm, s)
                            Tab.Sites -> SitesScreen(vm, s)
                            Tab.More -> MoreScreen(vm, s) { showSetup = true }
                        }
                    }
                }
            }
        }
    }
}

private fun tabIcon(t: Tab, selected: Boolean) = when (t) {
    Tab.Home -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    Tab.Progress -> if (selected) Icons.Rounded.Insights else Icons.Outlined.Insights
    Tab.Apps -> if (selected) Icons.Rounded.GridView else Icons.Outlined.GridView
    Tab.Sites -> if (selected) Icons.Rounded.Language else Icons.Outlined.Language
    Tab.More -> if (selected) Icons.Rounded.MoreHoriz else Icons.Outlined.MoreHoriz
}

// The wordmark on the left, the filter's state on the right.
@Composable
private fun Header(s: Settings, showSetup: Boolean) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_mark), contentDescription = null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Anti Brainrot", style = MaterialTheme.typography.titleLarge)
            AnimatedVisibility(showSetup) {
                Text("Setup", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(!showSetup, enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 0.8f)) {
            StatusPill(s)
        }
    }
}

@Composable
private fun StatusPill(s: Settings) {
    val on = s.focus.enabled
    val locked = on && Rules.isLockedNow(s)
    val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, tween(350), label = "pill")
    val fg by animateColorAsState(if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(350), label = "pillText")
    Row(Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (locked) Icons.Rounded.Lock else Icons.Rounded.PowerSettingsNew, contentDescription = null, Modifier.size(14.dp), tint = fg)
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = when { locked -> "Locked"; on -> "On"; else -> "Off" },
            transitionSpec = { (fadeIn(tween(200)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(150)) + slideOutVertically { -it / 2 }) },
            label = "pillLabel",
        ) { Text(it, style = MaterialTheme.typography.labelLarge, color = fg) }
    }
}
