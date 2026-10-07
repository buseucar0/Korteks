package com.buse.korteks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.MatrixPuzzleSpec
import com.buse.korteks.game.MatrixQuestion
import com.buse.korteks.game.MatrixTask
import com.buse.korteks.game.TaskResult
import kotlin.random.Random

sealed interface MatrixUiState {
    data object Intro : MatrixUiState

    data class Playing(
        val question: MatrixQuestion,
        val number: Int,
        val total: Int,
        val timeLimitMs: Long,
    ) : MatrixUiState

    data class Finished(
        val difficulty: MatrixDifficulty,
        val result: TaskResult,
        val reward: GameReward? = null,
    ) : MatrixUiState
}

/** Matris ekranının ViewModel'i. Yapısı StroopViewModel ile aynı (açıklamalar orada). */
class MatrixViewModel(private val random: Random = Random.Default) : ViewModel() {

    private var task: MatrixTask? = null

    var state: MatrixUiState by mutableStateOf(MatrixUiState.Intro)
        private set

    /** Bulmacalar dışarıdan gelir: JSON'u ekran okur (Context gerekir), ViewModel Android'den bağımsız kalır. */
    fun start(puzzles: List<MatrixPuzzleSpec>, difficulty: MatrixDifficulty, colorBlindSafe: Boolean = false) {
        task = MatrixTask(puzzles, difficulty, random, colorBlindSafe)
        publish()
    }

    fun answer(optionIndex: Int, reactionMs: Long, trialNumber: Int) {
        val t = activeTask(trialNumber) ?: return
        t.answer(optionIndex, reactionMs)
        publish()
    }

    fun timeout(trialNumber: Int) {
        val t = activeTask(trialNumber) ?: return
        t.timeout()
        publish()
    }

    fun attachReward(reward: GameReward?) {
        val s = state
        if (s is MatrixUiState.Finished) state = s.copy(reward = reward)
    }

    fun backToIntro() {
        task = null
        state = MatrixUiState.Intro
    }

    private fun activeTask(trialNumber: Int): MatrixTask? {
        val t = task ?: return null
        if (t.isFinished || t.progress + 1 != trialNumber) return null
        return t
    }

    private fun publish() {
        val t = task ?: return
        state = if (t.isFinished) {
            MatrixUiState.Finished(t.difficulty, t.result())
        } else {
            MatrixUiState.Playing(t.currentQuestion(), t.progress + 1, t.questions.size, t.difficulty.timeLimitMs)
        }
    }
}
