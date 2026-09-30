package com.buse.korteks.game

import kotlin.random.Random

/** Kullanılabilecek semboller. Her oyunda bunlardan bazıları rastgele rakamlarla eşleştirilir. */
val SYMBOLS = listOf("◆", "●", "▲", "■", "★", "✚", "◐", "☾", "⊗")

enum class SymbolDigitDifficulty(val title: String, val symbolCount: Int, val durationMs: Long) {
    KOLAY("Kolay", symbolCount = 6, durationMs = 60_000),
    ORTA("Orta", symbolCount = 9, durationMs = 60_000),
    ZOR("Zor", symbolCount = 9, durationMs = 45_000),
}

/**
 * Sembol-sayı eşleştirme. Soru = gösterilen sembol, cevap = seçilen rakam.
 * Diğer görevlerden farkı: süre soru başına değil, TÜM oturum için. Süre bitene kadar
 * olabildiğince çok doğru cevap verilir. Bu yüzden soru listesi sonsuz gibi davranır.
 */
class SymbolDigitTask(
    val difficulty: SymbolDigitDifficulty,
    private val random: Random = Random.Default,
) : CognitiveTask<String, Int> {

    /** Anahtar (key): sembol → rakam (1'den başlar). Her oyunda yeniden karıştırılır, ezberlenmesin. */
    val key: Map<String, Int> = SYMBOLS.shuffled(random)
        .take(difficulty.symbolCount)
        .withIndex()
        .associate { (index, symbol) -> symbol to index + 1 }

    private var current: String = nextSymbol(previous = null)
    private var sessionOver = false
    private val correctReactionTimes = mutableListOf<Long>()

    var correct = 0
        private set
    var wrong = 0
        private set

    override val isFinished: Boolean get() = sessionOver

    override fun currentQuestion(): String = current

    override fun answer(answer: Int, reactionMs: Long): Boolean {
        check(!isFinished) { "Süre bitti, cevap alınamaz" }
        val isCorrect = key.getValue(current) == answer
        if (isCorrect) {
            correct++
            correctReactionTimes += reactionMs
        } else {
            wrong++
        }
        current = nextSymbol(previous = current)
        return isCorrect
    }

    /** Bu görevde timeout = oturumun süresi doldu → görev biter. */
    override fun timeout() {
        sessionOver = true
    }

    /** Doğru başına 100 puan, yanlış başına 50 puan ceza (rastgele basmak işe yaramasın). */
    override fun result() = TaskResult(
        correct = correct,
        total = correct + wrong,
        averageReactionMs = if (correctReactionTimes.isEmpty()) 0 else correctReactionTimes.average().toLong(),
        score = (correct * 100 - wrong * 50).coerceAtLeast(0),
    )

    /** Aynı sembol arka arkaya gelmesin: oyuncu yeni bir sembol geldiğini fark etsin. */
    private fun nextSymbol(previous: String?): String = key.keys.filter { it != previous }.random(random)
}
