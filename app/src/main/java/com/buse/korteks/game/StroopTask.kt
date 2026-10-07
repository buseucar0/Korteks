package com.buse.korteks.game

import kotlin.random.Random

/** Oyundaki renkler. Burada Android'in Color sınıfı yok, ekrandaki karşılığı ui/ katmanında. */
enum class InkColor(val label: String) {
    KIRMIZI("KIRMIZI"),
    MAVI("MAVİ"),
    YESIL("YEŞİL"),
    SARI("SARI"),
    MOR("MOR"),
    TURUNCU("TURUNCU"),
}

/**
 * Renk körü dostu modda kullanılan renkler, sırası önemli: ilk 3'ü (Kolay) ve 4'ü birlikte,
 * kırmızı-yeşil ve mavi-sarı renk körlüğü simülasyonunda en iyi ayrılan alt kümeler.
 * (Machado ve ark. 2009 simülasyonu + CIELAB ΔE ölçümü; ayrıntı: PaletteTest)
 */
val COLOR_BLIND_SAFE_COLORS = listOf(InkColor.KIRMIZI, InkColor.SARI, InkColor.MAVI, InkColor.MOR)

/** Oyunda kullanılacak renkler. Renk körü modunda en fazla 4 renk (güvenli alt küme). */
fun stroopPalette(difficulty: StroopDifficulty, colorBlindSafe: Boolean): List<InkColor> =
    if (colorBlindSafe) {
        COLOR_BLIND_SAFE_COLORS.take(minOf(difficulty.colorCount, COLOR_BLIND_SAFE_COLORS.size))
    } else {
        InkColor.entries.take(difficulty.colorCount)
    }

/**
 * congruentPercent: kelime ile rengin AYNI olduğu (uyumlu, congruent) soruların yüzdesi.
 * Uyumsuz sorular (ör. KIRMIZI yazısı mavi renkte) asıl zorluğu yaratır.
 */
enum class StroopDifficulty(
    val title: String,
    val colorCount: Int,
    val trialCount: Int,
    val congruentPercent: Int,
    val timeLimitMs: Long,
) {
    KOLAY("Kolay", colorCount = 3, trialCount = 12, congruentPercent = 50, timeLimitMs = 3000),
    ORTA("Orta", colorCount = 4, trialCount = 16, congruentPercent = 25, timeLimitMs = 2000),
    ZOR("Zor", colorCount = 6, trialCount = 20, congruentPercent = 0, timeLimitMs = 1500),
}

/** Tek bir soru (trial): yazılan kelime, yazının rengi (mürekkep, ink) ve seçenekler. */
data class StroopTrial(
    val word: InkColor,
    val ink: InkColor,
    val options: List<InkColor>,
) {
    /** Stroop'un temel kuralı: doğru cevap her zaman yazının RENGİDİR, anlamı değil. */
    val correctAnswer: InkColor get() = ink
    val isCongruent: Boolean get() = word == ink
}

class StroopTask(
    val difficulty: StroopDifficulty,
    random: Random = Random.Default,
    colorBlindSafe: Boolean = false,
) : CognitiveTask<StroopTrial, InkColor> {

    val trials: List<StroopTrial> = generateTrials(difficulty, random, stroopPalette(difficulty, colorBlindSafe))

    /** Kaçıncı sorudayız (0'dan başlar). */
    var progress = 0
        private set

    private var correct = 0
    private var score = 0
    private val correctReactionTimes = mutableListOf<Long>()

    override val isFinished: Boolean get() = progress >= trials.size

    override fun currentQuestion(): StroopTrial = trials[progress]

    override fun answer(answer: InkColor, reactionMs: Long): Boolean {
        check(!isFinished) { "Görev bitti, cevap alınamaz" }
        val isCorrect = answer == trials[progress].correctAnswer
        if (isCorrect) {
            correct++
            correctReactionTimes += reactionMs
            score += pointsFor(reactionMs)
        }
        progress++
        return isCorrect
    }

    override fun timeout() {
        check(!isFinished) { "Görev bitti" }
        progress++
    }

    override fun result() = TaskResult(
        correct = correct,
        total = trials.size,
        averageReactionMs = if (correctReactionTimes.isEmpty()) 0 else correctReactionTimes.average().toLong(),
        score = score,
    )

    /** Doğru cevap = 100 taban puan + 0..100 hız bonusu (süre sınırından ne kadar önce, o kadar çok). */
    private fun pointsFor(reactionMs: Long): Int {
        val remainingMs = (difficulty.timeLimitMs - reactionMs).coerceAtLeast(0)
        return 100 + (remainingMs * 100 / difficulty.timeLimitMs).toInt()
    }
}

internal fun generateTrials(
    difficulty: StroopDifficulty,
    random: Random,
    palette: List<InkColor> = InkColor.entries.take(difficulty.colorCount),
): List<StroopTrial> {
    val congruentCount = difficulty.trialCount * difficulty.congruentPercent / 100

    // Önce uyumlu/uyumsuz bayraklarını tam oranla hazırla, sonra karıştır.
    // Böylece oran şansa bağlı değil, her oyunda aynı olur.
    val congruentFlags =
        (List(congruentCount) { true } + List(difficulty.trialCount - congruentCount) { false })
            .shuffled(random)

    return congruentFlags.map { congruent ->
        val ink = palette.random(random)
        val word = if (congruent) ink else (palette - ink).random(random)
        // Seçenekler hep aynı sırada: butonların yeri oyun boyunca değişmesin
        StroopTrial(word = word, ink = ink, options = palette)
    }
}
