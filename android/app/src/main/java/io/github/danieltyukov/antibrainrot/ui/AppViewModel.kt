package io.github.danieltyukov.antibrainrot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.LockedException
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
}
