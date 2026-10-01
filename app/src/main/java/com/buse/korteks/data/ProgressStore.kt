package com.buse.korteks.data

import android.content.Context
import com.buse.korteks.game.PlayerProgress
import java.time.LocalDate

/**
 * İlerlemeyi cihazda kalıcı saklar. SharedPreferences: Android'in hazır anahtar-değer deposu,
 * ek kütüphane gerektirmez. Gömülüdeki EEPROM'a birkaç değer yazmak gibi düşün.
 */
class ProgressStore(context: Context) {

    private val prefs = context.getSharedPreferences("korteks_ilerleme", Context.MODE_PRIVATE)

    fun load() = PlayerProgress(
        totalXp = prefs.getInt(KEY_XP, 0),
        streakDays = prefs.getInt(KEY_STREAK, 0),
        longestStreak = prefs.getInt(KEY_LONGEST, 0),
        lastPlayedDate = prefs.getString(KEY_LAST_DAY, null)?.let(LocalDate::parse),
    )

    /** apply(): diske arka planda yazar, arayüzü bekletmez. */
    fun save(progress: PlayerProgress) {
        prefs.edit()
            .putInt(KEY_XP, progress.totalXp)
            .putInt(KEY_STREAK, progress.streakDays)
            .putInt(KEY_LONGEST, progress.longestStreak)
            .putString(KEY_LAST_DAY, progress.lastPlayedDate?.toString()) // ISO biçimi: 2026-10-01
            .apply()
    }

    private companion object {
        const val KEY_XP = "toplam_xp"
        const val KEY_STREAK = "seri_gun"
        const val KEY_LONGEST = "en_uzun_seri"
        const val KEY_LAST_DAY = "son_oyun_gunu"
    }
}
