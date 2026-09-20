package io.github.danieltyukov.antibrainrot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Rules
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val Context.localDataStore: DataStore<Preferences> by preferencesDataStore(name = "local")

// One settings object, serialised as JSON, changed only under a mutex so two
// screens cannot overwrite each other. `update` applies the user guards
// (tighten any time, loosen only while off; locks). `patch` is for the
// app's own enforcement writes and skips them.
class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("settings")
    private val mutex = Mutex()

    val flow: Flow<Settings> = context.settingsDataStore.data.map { Settings.decode(it[key]) }

    suspend fun get(): Settings = flow.first()

    suspend fun update(change: (Settings) -> Settings): Settings = mutex.withLock {
        val current = get()
        val next = Settings.normalize(change(current))
        Rules.guard(current, next)
        write(next)
        next
    }

    suspend fun patch(change: (Settings) -> Settings): Settings = mutex.withLock {
        val next = Settings.normalize(change(get()))
        write(next)
        next
    }

    private suspend fun write(s: Settings) {
        context.settingsDataStore.edit { it[key] = s.encode() }
    }
}

class LocalStore(private val context: Context) {
    private val key = stringPreferencesKey("local")
    private val mutex = Mutex()

    val flow: Flow<LocalState> = context.localDataStore.data.map { LocalState.decode(it[key]) }

    suspend fun get(): LocalState = flow.first()

    suspend fun update(change: (LocalState) -> LocalState): LocalState = mutex.withLock {
        val next = change(get())
        context.localDataStore.edit { it[key] = next.encode() }
        next
    }
}
