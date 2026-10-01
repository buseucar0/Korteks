package com.buse.korteks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.HanoiMove
import com.buse.korteks.game.HanoiTask
import com.buse.korteks.game.TaskResult

sealed interface HanoiUiState {
    data object Intro : HanoiUiState

    data class Playing(
        val pegs: List<List<Int>>,
        val moves: Int,
        val minimumMoves: Int,
        val disks: Int,
        val timeLimitMs: Long,
        val gameNo: Int,
    ) : HanoiUiState

    data class Finished(
        val difficulty: HanoiDifficulty,
        val result: TaskResult,
        val moves: Int,
        val minimumMoves: Int,
        val solved: Boolean,
        val reward: GameReward? = null,
    ) : HanoiUiState
}

/** Planlama (Hanoi) ekranının ViewModel'i. Süre bütün oyun için sayılır (Hız ile aynı yöntem). */
class HanoiViewModel : ViewModel() {

    private var task: HanoiTask? = null
    private var gameNo = 0

    /** Oyunda geçen süre: ekran yeniden kurulunca sayaç buradan devam eder (Compose state değil). */
    var sessionElapsedMs = 0L

    var state: HanoiUiState by mutableStateOf(HanoiUiState.Intro)
        private set

    fun start(difficulty: HanoiDifficulty) {
        task = HanoiTask(difficulty)
        gameNo++
        sessionElapsedMs = 0
        publish()
    }

    /** Hamle kurala uygunsa uygular. Çözüm süresi = oyunda geçen süre. */
    fun move(from: Int, to: Int) {
        val t = task ?: return
        if (t.isFinished) return
        if (t.answer(HanoiMove(from, to), sessionElapsedMs)) publish()
    }

    fun timeout(gameNo: Int) {
        val t = task ?: return
        if (t.isFinished || gameNo != this.gameNo) return
        t.timeout()
        publish()
    }

    fun attachReward(reward: GameReward?) {
        val s = state
        if (s is HanoiUiState.Finished) state = s.copy(reward = reward)
    }

    fun backToIntro() {
        task = null
        state = HanoiUiState.Intro
    }

    private fun publish() {
        val t = task ?: return
        state = if (t.isFinished) {
            HanoiUiState.Finished(t.difficulty, t.result(), t.moves, t.minimumMoves, t.isSolved)
        } else {
            HanoiUiState.Playing(t.currentQuestion(), t.moves, t.minimumMoves, t.difficulty.disks, t.difficulty.timeLimitMs, gameNo)
        }
    }
}
