package com.buse.korteks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.SymbolDigitDifficulty
import com.buse.korteks.game.SymbolDigitTask
import com.buse.korteks.game.TaskResult
import kotlin.random.Random

sealed interface SpeedUiState {
    data object Intro : SpeedUiState

    /**
     * key: anahtar (sembol, rakam), rakama göre sıralı. answerNo: 1'den başlayan cevap sırası (çift tıklama kontrolü).
     * gameNo: her yeni oyunda artar; oturum sayacının anahtarı.
     */
    data class Playing(
        val symbol: String,
        val key: List<Pair<String, Int>>,
        val correct: Int,
        val answerNo: Int,
        val durationMs: Long,
        val gameNo: Int,
    ) : SpeedUiState

    data class Finished(
        val difficulty: SymbolDigitDifficulty,
        val result: TaskResult,
        val reward: GameReward? = null,
    ) : SpeedUiState
}

/** Hız (sembol-sayı) ekranının ViewModel'i. Süre bütün oturum için sayılır. */
class SymbolDigitViewModel(private val random: Random = Random.Default) : ViewModel() {

    private var task: SymbolDigitTask? = null
    private var gameNo = 0
    private var lastAnswerAtMs = 0L

    /**
     * Oturumda geçen süre. Ekrandaki sayaç her karede buraya yazar; ekran yeniden kurulunca
     * sayaç buradan devam eder. Compose state DEĞİL: her karede değişiyor, yeniden çizim tetiklemesin.
     */
    var sessionElapsedMs = 0L

    var state: SpeedUiState by mutableStateOf(SpeedUiState.Intro)
        private set

    fun start(difficulty: SymbolDigitDifficulty) {
        task = SymbolDigitTask(difficulty, random)
        gameNo++
        sessionElapsedMs = 0
        lastAnswerAtMs = 0
        publish()
    }

    /** Tepki süresi = son cevaptan bu yana geçen oturum süresi. */
    fun answer(digit: Int, answerNo: Int) {
        val t = task ?: return
        if (t.isFinished || t.correct + t.wrong + 1 != answerNo) return
        t.answer(digit, sessionElapsedMs - lastAnswerAtMs)
        lastAnswerAtMs = sessionElapsedMs
        publish()
    }

    /** Oturum süresi doldu. gameNo: eski oyunun sayacı yeni oyunu bitirmesin. */
    fun timeout(gameNo: Int) {
        val t = task ?: return
        if (t.isFinished || gameNo != this.gameNo) return
        t.timeout()
        publish()
    }

    fun attachReward(reward: GameReward?) {
        val s = state
        if (s is SpeedUiState.Finished) state = s.copy(reward = reward)
    }

    fun backToIntro() {
        task = null
        state = SpeedUiState.Intro
    }

    private fun publish() {
        val t = task ?: return
        state = if (t.isFinished) {
            SpeedUiState.Finished(t.difficulty, t.result())
        } else {
            SpeedUiState.Playing(
                symbol = t.currentQuestion(),
                key = t.key.entries.sortedBy { it.value }.map { it.key to it.value },
                correct = t.correct,
                answerNo = t.correct + t.wrong + 1,
                durationMs = t.difficulty.durationMs,
                gameNo = gameNo,
            )
        }
    }
}
