package io.github.danieltyukov.antibrainrot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.core.DayRecord
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.LockedException
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Random

class AppViewModel : ViewModel() {
    private val app = App.instance
    val settings: StateFlow<Settings?> = app.settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val local: StateFlow<LocalState?> = app.local.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val message = MutableStateFlow<String?>(null)

    // A user change: guarded. Refusals surface as a message.
    fun update(change: (Settings) -> Settings) {
        viewModelScope.launch {
            try {
                app.settings.update(change)
            } catch (e: LockedException) {
                message.value = e.message
            } catch (e: Exception) {
                message.value = "Could not save: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    // Debug builds only (the button lives behind BuildConfig.DEBUG): fills
    // sixty days of plausible history so the Progress screen can be looked at.
    fun seedHistory() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val rnd = Random(7)
            val sample = (1 until 60).associate { back ->
                val d = today.minusDays(back.toLong())
                val weekend = d.dayOfWeek.value >= 6
                val focus = if (rnd.nextInt(9) == 0) 0 else (if (weekend) 2 else 6) * 3600 + rnd.nextInt(3 * 3600)
                val blocks = if (focus == 0) 0 else 2 + rnd.nextInt(9)
                val passes = blocks / 3
                d.toString() to DayRecord(
                    blocks = blocks, passes = passes, passMinutes = passes * 5, focusSeconds = focus,
                    byApp = mapOf("com.android.chrome" to blocks / 2, "com.google.android.youtube" to blocks - blocks / 2).filterValues { it > 0 },
                    usage = mapOf("com.android.chrome" to rnd.nextInt(30 * 60), "com.google.android.youtube" to rnd.nextInt(60 * 60), "site:reddit.com" to rnd.nextInt(40 * 60)).filterValues { it > 0 },
                )
            }
            app.local.update { it.copy(history = sample + it.history.filterKeys { k -> k == today.toString() }) }
        }
    }
}
