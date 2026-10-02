package com.buse.korteks.game

import kotlin.math.roundToInt

/**
 * Profil kartı için her görevde 0..100 arası "en iyi performans" puanı.
 *
 * Hesap: rekor / o zorluktaki teorik en yüksek puan, zorluk ağırlığıyla çarpılır
 * (Kolay ×0.6, Orta ×0.8, Zor ×1.0) ve görevin bütün zorluklarındaki en yüksek değer alınır.
 * Bu, oyuncunun kendi görevleri arasında karşılaştırma içindir; bilimsel/klinik bir ölçüm DEĞİLDİR.
 */
object CognitiveProfile {

    private val DIFFICULTY_WEIGHT = listOf(0.6, 0.8, 1.0) // zorluk sırasına (ordinal) göre

    /** Görev başına puan; hiç oynanmamış görev null. */
    fun of(bestScores: Map<String, Int>): Map<GameType, Int?> =
        GameType.entries.associateWith { game -> scoreFor(game, bestScores) }

    fun scoreFor(game: GameType, bestScores: Map<String, Int>): Int? =
        difficultiesOf(game)
            .mapNotNull { (name, ordinal) ->
                val best = bestScores[GameRecord.bestKeyOf(game, name)] ?: return@mapNotNull null
                val ratio = (best.toDouble() / maxScore(game, name)).coerceIn(0.0, 1.0)
                (100 * DIFFICULTY_WEIGHT[ordinal] * ratio).roundToInt()
            }
            .maxOrNull()

    /** Görevin zorlukları: (ad, sıra). */
    private fun difficultiesOf(game: GameType): List<Pair<String, Int>> = when (game) {
        GameType.STROOP -> StroopDifficulty.entries
        GameType.MATRIX -> MatrixDifficulty.entries
        GameType.NBACK -> NBackDifficulty.entries
        GameType.SPEED -> SymbolDigitDifficulty.entries
        GameType.CORSI -> CorsiDifficulty.entries
        GameType.HANOI -> HanoiDifficulty.entries
    }.map { it.name to it.ordinal }

    /**
     * Bir zorlukta alınabilecek en yüksek puan (her görevin puan formülünden).
     * Hız görevinde puanın üst sınırı yok: saniyede 1 doğru cevap referans kabul edilir.
     */
    fun maxScore(game: GameType, difficulty: String): Int = when (game) {
        GameType.STROOP -> StroopDifficulty.valueOf(difficulty).trialCount * 200
        GameType.MATRIX -> MatrixDifficulty.valueOf(difficulty).puzzleCount * 200
        GameType.NBACK -> NBackDifficulty.valueOf(difficulty).let { d ->
            val hits = 2 * (d.scoredSteps * MATCH_PERCENT / 100)   // iki kanal, bütün eşleşmeler yakalanmış
            val correctRejections = 2 * d.scoredSteps - hits
            (hits * 100 + correctRejections * 20) * d.n
        }
        GameType.SPEED -> (SymbolDigitDifficulty.valueOf(difficulty).durationMs / 1000 * 100).toInt()
        GameType.CORSI -> CorsiDifficulty.valueOf(difficulty).let { d ->
            val span = CORSI_MAX_LENGTH
            val correct = CORSI_MAX_LENGTH - d.startLength + 1     // başlangıçtan 9'a kadar hiç hatasız
            span * 100 + correct * 20 + if (d.backward) span * 50 else 0
        }
        GameType.HANOI -> 100 * HanoiDifficulty.valueOf(difficulty).disks * 10
    }
}
