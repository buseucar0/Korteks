package com.buse.korteks.data

import android.content.Context

/** Kullanıcı ayarları (ilerlemeden ayrı dosyada). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("korteks_ayarlar", Context.MODE_PRIVATE)

    var colorBlindMode: Boolean
        get() = prefs.getBoolean(KEY_COLOR_BLIND, false)
        set(value) = prefs.edit().putBoolean(KEY_COLOR_BLIND, value).apply()

    var vibration: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    private companion object {
        const val KEY_COLOR_BLIND = "renk_koru_modu"
        const val KEY_VIBRATION = "titresim"
    }
}
