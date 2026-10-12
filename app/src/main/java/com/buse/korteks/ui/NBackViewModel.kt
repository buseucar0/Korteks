package com.buse.korteks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.NBackResponse
import com.buse.korteks.game.NBackStimulus
import com.buse.korteks.game.NBackTask
import com.buse.korteks.game.TaskResult
import kotlin.random.Random

sealed interface NBackUiState {
    data object Intro : NBackUiState

    /**
     * practice: alıştırma turu (puanlanmaz, kaydedilmez).
     * lastFeedback: alıştırmada bir önceki adımın açıklaması ("✓ Doğru", "Konum aynıydı, ..."), yoksa null.
     */
    data class Playing(
        val stimulus: NBackStimulus,
        val number: Int,
        val total: Int,
        val n: Int,
        val stepMs: Long,
        val practice: Boolean = false,
        val lastFeedback: String? = null,
    ) : NBackUiState {
        /** İlk N adım puanlanmaz (karşılaştıracak geçmiş yok): butonlar kapalı. */
        val scored: Boolean get() = number - 1 >= n
    }

    data class Finished(
        val difficulty: NBackDifficulty,
        val result: TaskResult,
        val hits: Int,
        val misses: Int,
        val falseAlarms: Int,
        val practice: Boolean = false,
        val reward: GameReward? = null,
    ) : NBackUiState
}

/** Bellek (çift N-Back) ekranının ViewModel'i. */
class NBackViewModel(private val random: Random = Random.Default) : ViewModel() {

    /** internal: testler eşleşmeleri (doğru cevabı) buradan okur. */
    internal var task: NBackTask? = null
        private set

    var state: NBackUiState by mutableStateOf(NBackUiState.Intro)
        private set

    private var practice = false
    private var lastFeedback: String? = null

    fun start(difficulty: NBackDifficulty) {
        task = NBackTask(difficulty, random)
        practice = false
        lastFeedback = null
        publish()
    }

    /** Alıştırma turu: 1-back, kısa, her adımdan sonra açıklama; puanlanmaz ve kaydedilmez. */
    fun startPractice() {
        task = NBackTask(NBackDifficulty.KOLAY, random, scoredSteps = PRACTICE_STEPS)
        practice = true
        lastFeedback = null
        publish()
    }

    /** Adımın süresi doldu: o adımda basılan butonlar değerlendirilir. */
    fun stepEnded(response: NBackResponse, firstPressMs: Long, stepNumber: Int) {
        val t = task ?: return
        if (t.isFinished || t.progress + 1 != stepNumber) return
        val step = t.progress
        t.answer(response, firstPressMs)
        if (practice) lastFeedback = explain(t, step, response)
        publish()
    }

    fun attachReward(reward: GameReward?) {
        val s = state
        if (s is NBackUiState.Finished) state = s.copy(reward = reward)
    }

    fun backToIntro() {
        task = null
        state = NBackUiState.Intro
    }

    private fun publish() {
        val t = task ?: return
        state = if (t.isFinished) {
            NBackUiState.Finished(t.difficulty, t.result(), t.hits, t.misses, t.falseAlarms, practice)
        } else {
            NBackUiState.Playing(
                t.currentQuestion(), t.progress + 1, t.stimuli.size, t.difficulty.n, t.difficulty.stepMs,
                practice = practice,
                lastFeedback = lastFeedback,
            )
        }
    }

    companion object {
        const val PRACTICE_STEPS = 10

        /** Alıştırmada bir adımın açıklaması: neye basılmalıydı, neye basılmamalıydı. */
        internal fun explain(task: NBackTask, step: Int, response: NBackResponse): String? {
            if (step < task.difficulty.n) return null // ilk N adım: karşılaştıracak geçmiş yok
            val notes = buildList {
                val pos = task.isPositionMatch(step)
                val letter = task.isLetterMatch(step)
                if (pos && !response.position) add("Konum aynıydı, KONUM'a basmalıydın")
                if (!pos && response.position) add("Konum farklıydı, KONUM'a basmamalıydın")
                if (letter && !response.letter) add("Harf aynıydı, HARF'e basmalıydın")
                if (!letter && response.letter) add("Harf farklıydı, HARF'e basmamalıydın")
            }
            return if (notes.isEmpty()) "✓ Doğru" else notes.joinToString("\n")
        }
    }
}
