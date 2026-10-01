package com.buse.korteks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.buse.korteks.game.CorsiDifficulty
import com.buse.korteks.game.CorsiTask
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.TaskResult
import kotlin.random.Random

sealed interface CorsiUiState {
    data object Intro : CorsiUiState

    /**
     * trialNo: 1'den başlayan deneme sırası. feedback: null = cevap bekleniyor,
     * true/false = cevap verildi, kısa bir süre "Doğru/Yanlış" gösterilip next() ile sonraki denemeye geçilir.
     */
    data class Playing(
        val trialNo: Int,
        val sequence: List<Int>,
        val span: Int,
        val backward: Boolean,
        val feedback: Boolean? = null,
    ) : CorsiUiState {
        val length: Int get() = sequence.size
    }

    data class Finished(
        val difficulty: CorsiDifficulty,
        val result: TaskResult,
        val span: Int,
        val reward: GameReward? = null,
    ) : CorsiUiState
}

/** Uzamsal (Corsi) ekranının ViewModel'i. */
class CorsiViewModel(private val random: Random = Random.Default) : ViewModel() {

    /** internal: testler beklenen cevabı (expectedAnswer) buradan okur. */
    internal var task: CorsiTask? = null
        private set
    private var trialNo = 0

    var state: CorsiUiState by mutableStateOf(CorsiUiState.Intro)
        private set

    fun start(difficulty: CorsiDifficulty) {
        task = CorsiTask(difficulty, random)
        trialNo = 1
        publishTrial()
    }

    /** Dokunuşlar tamamlandı. Sonuç hemen değil, "Doğru/Yanlış" geri bildirimi olarak gösterilir. */
    fun answer(taps: List<Int>, reactionMs: Long, trialNo: Int) {
        val s = state as? CorsiUiState.Playing ?: return
        val t = task ?: return
        if (s.trialNo != trialNo || s.feedback != null || t.isFinished) return
        val ok = t.answer(taps, reactionMs)
        state = s.copy(feedback = ok, span = t.span)
    }

    /** Geri bildirim gösterildi: sonraki deneme ya da sonuç ekranı. */
    fun next(trialNo: Int) {
        val s = state as? CorsiUiState.Playing ?: return
        val t = task ?: return
        if (s.trialNo != trialNo || s.feedback == null) return
        if (t.isFinished) {
            state = CorsiUiState.Finished(t.difficulty, t.result(), t.span)
        } else {
            this.trialNo++
            publishTrial()
        }
    }

    fun attachReward(reward: GameReward?) {
        val s = state
        if (s is CorsiUiState.Finished) state = s.copy(reward = reward)
    }

    fun backToIntro() {
        task = null
        state = CorsiUiState.Intro
    }

    private fun publishTrial() {
        val t = task ?: return
        state = CorsiUiState.Playing(trialNo, t.currentQuestion(), t.span, t.difficulty.backward)
    }
}
