package io.github.danieltyukov.antibrainrot

import android.app.Application
import io.github.danieltyukov.antibrainrot.data.LocalStore
import io.github.danieltyukov.antibrainrot.data.SettingsStore

class App : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var local: LocalStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        local = LocalStore(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
