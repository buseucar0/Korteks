package com.buse.korteks.game

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Oyuncunun kalıcı ilerlemesi: toplam XP, günlük seri (streak) ve rekorlar.
 * Seviye saklanmaz, her zaman totalXp'den hesaplanır (tek doğruluk kaynağı).
 * bestScores: görev + zorluk başına en yüksek puan, anahtar GameRecord.bestKeyOf ile ("STROOP_ZOR").
 */
data class PlayerProgress(
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val longestStreak: Int = 0,
    val lastPlayedDate: LocalDate? = null,
    val bestScores: Map<String, Int> = emptyMap(),
)

/** Seviye bilgisi: hangi seviyedeyiz, bu seviyede ne kadar XP birikti, sonrakine ne kadar lazım. */
data class LevelInfo(val level: Int, val xpIntoLevel: Int, val xpForNextLevel: Int) {
    /** Sonraki seviyeye ilerleme oranı (0..1), XP çubuğu için. */
    val fraction: Float get() = xpIntoLevel.toFloat() / xpForNextLevel
}

/**
 * Bir oyunun ilerlemeye etkisi. Sonuç ekranında "+X XP", "Seviye N!" ve "Yeni rekor!" göstermek için.
 * previousBest: bu görev + zorluktaki önceki rekor (ilk oyunsa null).
 */
data class GameReward(
    val xpGained: Int,
    val before: PlayerProgress,
    val after: PlayerProgress,
    val isNewBest: Boolean = false,
    val previousBest: Int? = null,
) {
    val levelBefore: Int get() = Progression.levelInfo(before.totalXp).level
    val levelAfter: Int get() = Progression.levelInfo(after.totalXp).level
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

/**
 * XP, seviye ve seri kuralları. Saf fonksiyonlar: bugünün tarihi dışarıdan gelir,
 * böylece "dün oynadıysa seri artar" gibi kurallar takvim beklemeden test edilir.
 */
object Progression {

    /** Oyun başına XP: 10 taban + puanın onda biri (puan 1000'de kırpılır) → 10..110 XP. */
    fun xpFor(score: Int): Int = 10 + score.coerceIn(0, 1000) / 10

    /** Seviye n'den n+1'e geçmek için gereken XP: 100, 150, 200, ... */
    fun xpToNextLevel(level: Int): Int = 100 + 50 * (level - 1)

    fun levelInfo(totalXp: Int): LevelInfo {
        var level = 1
        var remaining = totalXp.coerceAtLeast(0)
        while (remaining >= xpToNextLevel(level)) {
            remaining -= xpToNextLevel(level)
            level++
        }
        return LevelInfo(level, remaining, xpToNextLevel(level))
    }

    /** Bir oyun bitti: XP ekle, seriyi ve rekoru güncelle. Asıl kayıt fonksiyonu budur. */
    fun recordGame(progress: PlayerProgress, record: GameRecord, today: LocalDate): GameReward {
        val score = record.result.score
        val xp = xpFor(score)
        var after = withStreakUpdated(progress, today).copy(totalXp = progress.totalXp + xp)

        // Rekor: 0 puan rekor sayılmaz; eşit puan da yeni rekor değildir
        val previousBest = progress.bestScores[record.bestKey]
        val isNewBest = score > (previousBest ?: 0)
        if (isNewBest) after = after.copy(bestScores = after.bestScores + (record.bestKey to score))

        return GameReward(xpGained = xp, before = progress, after = after, isNewBest = isNewBest, previousBest = previousBest)
    }

    /**
     * Ekranda gösterilecek seri. Son oyun bugün ya da dünse seri sürüyor;
     * daha eskiyse seri kırılmıştır → 0 (kayıttaki değer bir sonraki oyunda 1'e döner).
     */
    fun currentStreak(progress: PlayerProgress, today: LocalDate): Int {
        val last = progress.lastPlayedDate ?: return 0
        return if (ChronoUnit.DAYS.between(last, today) <= 1) progress.streakDays else 0
    }

    private fun withStreakUpdated(progress: PlayerProgress, today: LocalDate): PlayerProgress {
        val last = progress.lastPlayedDate
        val daysSinceLast = last?.let { ChronoUnit.DAYS.between(it, today) }
        val newStreak = when {
            daysSinceLast == null -> 1                      // ilk oyun
            daysSinceLast <= 0L -> progress.streakDays      // aynı gün (ya da saat geri alınmış): değişmez
            daysSinceLast == 1L -> progress.streakDays + 1  // dün oynamış: seri sürüyor
            else -> 1                                       // en az bir gün atlanmış: baştan
        }
        return progress.copy(
            streakDays = newStreak,
            longestStreak = maxOf(progress.longestStreak, newStreak),
            // Saat geri alınmışsa son oyun günü geriye gitmesin
            lastPlayedDate = if (last != null && last > today) last else today,
        )
    }
}
