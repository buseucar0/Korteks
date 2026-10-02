package com.buse.korteks.game

import kotlin.random.Random

const val NBACK_LETTERS = "CHKLQRST"

/** Her kanalda (konum, harf) puanlanan adımların yaklaşık bu yüzdesi "eşleşme" olur. */
internal const val MATCH_PERCENT = 30

enum class NBackDifficulty(val title: String, val n: Int, val scoredSteps: Int, val stepMs: Long) {
    KOLAY("1-Back", n = 1, scoredSteps = 20, stepMs = 3000),
    ORTA("2-Back", n = 2, scoredSteps = 20, stepMs = 3000),
    ZOR("3-Back", n = 3, scoredSteps = 20, stepMs = 2500),
}

/** Bir adımda gösterilen uyaran (stimulus): 3x3 ızgarada bir konum (0..8) ve bir harf. */
data class NBackStimulus(val position: Int, val letter: Char)

/** Oyuncunun o adımda bastığı butonlar. */
data class NBackResponse(val position: Boolean, val letter: Boolean)

/**
 * Çift N-Back (dual N-back): her adımda oyuncu iki soruya cevap verir:
 * "Konum N adım öncekiyle aynı mı?" ve "Harf N adım öncekiyle aynı mı?"
 * Aynıysa ilgili butona basar, değilse basmaz. İlk N adım puanlanmaz (karşılaştıracak geçmiş yok).
 */
class NBackTask(
    val difficulty: NBackDifficulty,
    random: Random = Random.Default,
) : CognitiveTask<NBackStimulus, NBackResponse> {

    val stimuli: List<NBackStimulus> = generateStimuli(difficulty, random)

    var progress = 0
        private set

    // Sinyal algılama (signal detection) terimleri: isabet, kaçırma, yanlış alarm, doğru ret
    var hits = 0
        private set
    var misses = 0
        private set
    var falseAlarms = 0
        private set
    var correctRejections = 0
        private set
    private val hitReactionTimes = mutableListOf<Long>()

    private val n get() = difficulty.n

    override val isFinished: Boolean get() = progress >= stimuli.size

    override fun currentQuestion(): NBackStimulus = stimuli[progress]

    fun isPositionMatch(i: Int) = i >= n && stimuli[i].position == stimuli[i - n].position
    fun isLetterMatch(i: Int) = i >= n && stimuli[i].letter == stimuli[i - n].letter

    /** Adımın sonunda çağrılır. reactionMs = adımdaki ilk basışın zamanı. İki kanal da doğruysa true. */
    override fun answer(answer: NBackResponse, reactionMs: Long): Boolean {
        check(!isFinished) { "Görev bitti" }
        var allCorrect = true
        if (progress >= n) {
            val positionOk = judge(isPositionMatch(progress), answer.position)
            val letterOk = judge(isLetterMatch(progress), answer.letter)
            allCorrect = positionOk && letterOk
            val anyHit = (answer.position && isPositionMatch(progress)) || (answer.letter && isLetterMatch(progress))
            if (anyHit) hitReactionTimes += reactionMs
        }
        progress++
        return allCorrect
    }

    /** Adım süresi doldu ve hiç basılmadı. */
    override fun timeout() {
        answer(NBackResponse(position = false, letter = false), reactionMs = 0)
    }

    private fun judge(isMatch: Boolean, pressed: Boolean): Boolean {
        when {
            isMatch && pressed -> hits++
            isMatch -> misses++
            pressed -> falseAlarms++
            else -> correctRejections++
        }
        return isMatch == pressed
    }

    /** Doğru = isabet + doğru ret (iki kanal ayrı sayılır). N büyüdükçe puan çarpanı artar. */
    override fun result() = TaskResult(
        correct = hits + correctRejections,
        total = 2 * difficulty.scoredSteps,
        averageReactionMs = if (hitReactionTimes.isEmpty()) 0 else hitReactionTimes.average().toLong(),
        score = ((hits * 100 + correctRejections * 20 - falseAlarms * 50).coerceAtLeast(0)) * n,
    )
}

internal fun generateStimuli(difficulty: NBackDifficulty, random: Random): List<NBackStimulus> {
    val n = difficulty.n
    val matchCount = difficulty.scoredSteps * MATCH_PERCENT / 100

    // Stroop'taki gibi: eşleşme bayraklarını tam sayıda hazırla, karıştır → oran her oyunda aynı
    fun flags() = (List(matchCount) { true } + List(difficulty.scoredSteps - matchCount) { false }).shuffled(random)
    val positionFlags = flags()
    val letterFlags = flags()

    val positions = mutableListOf<Int>()
    val letters = mutableListOf<Char>()
    for (i in 0 until n + difficulty.scoredSteps) {
        if (i < n) {
            positions += random.nextInt(9)
            letters += NBACK_LETTERS.random(random)
        } else {
            val back = i - n
            // Eşleşme değilse, N adım öncekinden FARKLI bir değer seç (kazara eşleşme olmasın)
            positions += if (positionFlags[back]) positions[back] else (0 until 9).filter { it != positions[back] }.random(random)
            letters += if (letterFlags[back]) letters[back] else NBACK_LETTERS.filter { it != letters[back] }.random(random)
        }
    }
    return positions.zip(letters) { p, l -> NBackStimulus(p, l) }
}
