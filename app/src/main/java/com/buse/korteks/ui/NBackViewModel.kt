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

    data class Playing(
        val stimulus: NBackStimulus,
        val number: Int,
        val total: Int,
        val n: Int,
        val stepMs: Long,
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

    fun start(difficulty: NBackDifficulty) {
        task = NBackTask(difficulty, random)
        publish()
    }

    /** Adımın süresi doldu: o adımda basılan butonlar değerlendirilir. */
    fun stepEnded(response: NBackResponse, firstPressMs: Long, stepNumber: Int) {
        val t = task ?: return
        if (t.isFinished || t.progress + 1 != stepNumber) return
        t.answer(response, firstPressMs)
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
            NBackUiState.Finished(t.difficulty, t.result(), t.hits, t.misses, t.falseAlarms)
        } else {
            NBackUiState.Playing(t.currentQuestion(), t.progress + 1, t.stimuli.size, t.difficulty.n, t.difficulty.stepMs)
        }
    }
}
