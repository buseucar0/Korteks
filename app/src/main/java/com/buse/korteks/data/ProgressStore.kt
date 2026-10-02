package com.buse.korteks.data

import android.content.Context
import com.buse.korteks.game.GameType
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
        // Rekorlar "rekor_" ön ekli ayrı anahtarlarda: rekor_STROOP_ZOR = 2140
        bestScores = prefs.all
            .filterKeys { it.startsWith(BEST_PREFIX) }
            .mapNotNull { (key, value) -> (value as? Int)?.let { key.removePrefix(BEST_PREFIX) to it } }
            .toMap(),
        dailyDate = prefs.getString(KEY_DAILY_DAY, null)?.let(LocalDate::parse),
        // Bitirilen görevler virgülle ayrılmış adlar olarak: "STROOP,HANOI"
        dailyDone = prefs.getString(KEY_DAILY_DONE, "").orEmpty()
            .split(',')
            .mapNotNull { name -> GameType.entries.firstOrNull { it.name == name } }
            .toSet(),
    )

    /** apply(): diske arka planda yazar, arayüzü bekletmez. */
    fun save(progress: PlayerProgress) {
        val editor = prefs.edit()
            .putInt(KEY_XP, progress.totalXp)
            .putInt(KEY_STREAK, progress.streakDays)
            .putInt(KEY_LONGEST, progress.longestStreak)
            .putString(KEY_LAST_DAY, progress.lastPlayedDate?.toString()) // ISO biçimi: 2026-10-01
            .putString(KEY_DAILY_DAY, progress.dailyDate?.toString())
            .putString(KEY_DAILY_DONE, progress.dailyDone.joinToString(",") { it.name })
        progress.bestScores.forEach { (key, score) -> editor.putInt(BEST_PREFIX + key, score) }
        editor.apply()
    }

    private companion object {
        const val KEY_XP = "toplam_xp"
        const val KEY_STREAK = "seri_gun"
        const val KEY_LONGEST = "en_uzun_seri"
        const val KEY_LAST_DAY = "son_oyun_gunu"
        const val BEST_PREFIX = "rekor_"
        const val KEY_DAILY_DAY = "gunluk_gun"
        const val KEY_DAILY_DONE = "gunluk_bitenler"
    }
}
