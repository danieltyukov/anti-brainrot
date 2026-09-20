package io.github.danieltyukov.antibrainrot.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.danieltyukov.antibrainrot.ui.screens.AppsScreen
import io.github.danieltyukov.antibrainrot.ui.screens.HomeScreen
import io.github.danieltyukov.antibrainrot.ui.screens.MoreScreen
import io.github.danieltyukov.antibrainrot.ui.screens.ScheduleScreen
import io.github.danieltyukov.antibrainrot.ui.screens.SetupScreen
import io.github.danieltyukov.antibrainrot.ui.screens.SitesScreen
import io.github.danieltyukov.antibrainrot.ui.theme.AntiBrainrotTheme

enum class Tab(val label: String) { Home("Home"), Apps("Apps"), Sites("Sites"), Schedule("Schedule"), More("More") }

@OptIn(ExperimentalMaterial3Api::class)
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
        Scaffold(
            topBar = { TopAppBar(title = { Text(if (showSetup) "anti-brainrot setup" else "anti-brainrot") }) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (!showSetup) NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = {
                                Icon(
                                    when (t) {
                                        Tab.Home -> Icons.Filled.Home
                                        Tab.Apps -> Icons.Filled.Apps
                                        Tab.Sites -> Icons.Filled.Language
                                        Tab.Schedule -> Icons.Filled.Schedule
                                        Tab.More -> Icons.Filled.MoreHoriz
                                    },
                                    contentDescription = t.label,
                                )
                            },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { padding ->
            androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                if (showSetup) SetupScreen(vm) { showSetup = false }
                else when (tab) {
                    Tab.Home -> HomeScreen(vm, s, local)
                    Tab.Apps -> AppsScreen(vm, s)
                    Tab.Sites -> SitesScreen(vm, s)
                    Tab.Schedule -> ScheduleScreen(vm, s)
                    Tab.More -> MoreScreen(vm, s, local) { showSetup = true }
                }
            }
        }
    }
}
