package com.buse.korteks.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.buse.korteks.data.SettingsStore

/** Ayarların ekrandaki kaynağı. Değişiklik hem Compose state'e hem kalıcı kayda yazılır. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)

    /** Renk körü dostu mod: Stroop'ta en iyi ayrılan 4 renk, Matris'te renk kuralı yok. */
    var colorBlindMode: Boolean by mutableStateOf(store.colorBlindMode)
        private set

    fun updateColorBlindMode(enabled: Boolean) {
        colorBlindMode = enabled
        store.colorBlindMode = enabled
    }
}
