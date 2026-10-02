package com.buse.korteks.game

import java.time.LocalDate
import kotlin.random.Random

/**
 * Günlük antrenman: her gün 6 görevden 3'ü önerilir.
 * Seçim tarihten türetilen sabit tohumla (seed) yapılır: aynı gün uygulama kaç kez açılırsa açılsın
 * aynı 3 görev çıkar, ertesi gün farklı bir üçlü gelir. Sunucu ya da kayıt gerekmez.
 */
object DailyPlan {
    const val GAMES_PER_DAY = 3

    /** Üçü de bitirilince bir kez verilen bonus XP. */
    const val BONUS_XP = 50

    fun gamesFor(date: LocalDate): List<GameType> =
        GameType.entries.shuffled(Random(date.toEpochDay())).take(GAMES_PER_DAY)
}
