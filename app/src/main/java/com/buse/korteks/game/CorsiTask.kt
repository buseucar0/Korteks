package com.buse.korteks.game

import kotlin.random.Random

const val CORSI_BLOCKS = 9
const val CORSI_MAX_LENGTH = 9

/** backward = true: gösterilen sırayı TERSTEN dokunmak gerekir (daha zor, klasik "backward Corsi"). */
enum class CorsiDifficulty(val title: String, val startLength: Int, val backward: Boolean) {
    KOLAY("Kolay", startLength = 2, backward = false),
    ORTA("Orta", startLength = 3, backward = false),
    ZOR("Tersten", startLength = 3, backward = true),
}

/**
 * Corsi blokları. Soru = yanan blokların sırası (0..8), cevap = oyuncunun dokunduğu sıra.
 * Kural: doğru → dizi 1 uzar. Aynı uzunlukta üst üste 2 yanlış → görev biter.
 * "Aralık" (span) = doğru hatırlanan en uzun dizi.
 */
class CorsiTask(
    val difficulty: CorsiDifficulty,
    private val random: Random = Random.Default,
) : CognitiveTask<List<Int>, List<Int>> {

    var currentLength = difficulty.startLength
        private set
    var span = 0
        private set
    var trials = 0
        private set
    var correctCount = 0
        private set

    private var failuresAtLength = 0
    private var finished = false
    private val correctReactionTimes = mutableListOf<Long>()
    private var sequence = newSequence()

    override val isFinished: Boolean get() = finished

    override fun currentQuestion(): List<Int> = sequence

    /** Oyuncunun dokunması gereken sıra. */
    fun expectedAnswer(): List<Int> = if (difficulty.backward) sequence.reversed() else sequence

    override fun answer(answer: List<Int>, reactionMs: Long): Boolean {
        check(!isFinished) { "Görev bitti" }
        val isCorrect = answer == expectedAnswer()
        trials++
        if (isCorrect) {
            correctCount++
            correctReactionTimes += reactionMs
            span = maxOf(span, currentLength)
            failuresAtLength = 0
            if (currentLength == CORSI_MAX_LENGTH) finished = true else currentLength++
        } else {
            failuresAtLength++
            if (failuresAtLength >= 2) finished = true
        }
        if (!finished) sequence = newSequence()
        return isCorrect
    }

    override fun timeout() {
        answer(emptyList(), reactionMs = 0)
    }

    override fun result() = TaskResult(
        correct = correctCount,
        total = trials,
        averageReactionMs = if (correctReactionTimes.isEmpty()) 0 else correctReactionTimes.average().toLong(),
        score = span * 100 + correctCount * 20 + if (difficulty.backward) span * 50 else 0,
    )

    /** Tekrarsız dizi: aynı blok bir dizide iki kez yanmaz. */
    private fun newSequence(): List<Int> = (0 until CORSI_BLOCKS).shuffled(random).take(currentLength)
}
