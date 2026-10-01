package com.buse.korteks.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.buse.korteks.game.InkColor
import com.buse.korteks.game.StroopDifficulty
import com.buse.korteks.game.StroopTask
import com.buse.korteks.game.StroopTrial
import com.buse.korteks.game.TaskResult
import kotlin.random.Random

/**
 * Ekranın çizmesi gereken her şeyin değişmez (immutable) anlık görüntüsü.
 * Ekran sadece bunu okur; oyunun iç durumunu (StroopTask) hiç görmez.
 */
sealed interface StroopUiState {
    data object Intro : StroopUiState

    /** number: 1'den başlayan soru numarası. Her soru için farklı olduğu için sayaç anahtarı olarak da kullanılır. */
    data class Playing(
        val trial: StroopTrial,
        val number: Int,
        val total: Int,
        val timeLimitMs: Long,
    ) : StroopUiState

    data class Finished(val difficulty: StroopDifficulty, val result: TaskResult) : StroopUiState
}

/**
 * Stroop ekranının ViewModel'i: oyun durumunu ekrandan (Activity'den) ayrı tutar.
 * Activity yıkılıp yeniden kurulsa da (tema/dil değişimi) ViewModel yaşamaya devam eder.
 *
 * Zamanı ölçmez: süre sayacı ekranda kalır (ekran karesine bağlı), buraya sadece sonuç gelir.
 * random dışarıdan verilebilir → testte sabit tohumla aynı sorular üretilir.
 */
class StroopViewModel(private val random: Random = Random.Default) : ViewModel() {

    private var task: StroopTask? = null

    var state: StroopUiState by mutableStateOf(StroopUiState.Intro)
        private set

    fun start(difficulty: StroopDifficulty) {
        task = StroopTask(difficulty, random)
        publish()
    }

    /**
     * trialNumber: cevabın hangi soruya verildiği. Ekrandaki soru numarasıyla aynı değilse
     * (ör. aynı karede iki tıklama) cevap yok sayılır.
     */
    fun answer(color: InkColor, reactionMs: Long, trialNumber: Int) {
        val t = activeTask(trialNumber) ?: return
        t.answer(color, reactionMs)
        publish()
    }

    /** Süre doldu. Eski bir sayaç (önceki sorunun) yeni soruyu atlatmasın diye numara kontrol edilir. */
    fun timeout(trialNumber: Int) {
        val t = activeTask(trialNumber) ?: return
        t.timeout()
        publish()
    }

    fun backToIntro() {
        task = null
        state = StroopUiState.Intro
    }

    /** Görev sürüyorsa ve gelen numara şu anki soruysa görevi döndürür, değilse null. */
    private fun activeTask(trialNumber: Int): StroopTask? {
        val t = task ?: return null
        if (t.isFinished || t.progress + 1 != trialNumber) return null
        return t
    }

    /** Görevin iç durumundan ekran için yeni bir anlık görüntü üretir. */
    private fun publish() {
        val t = task ?: return
        state = if (t.isFinished) {
            StroopUiState.Finished(t.difficulty, t.result())
        } else {
            StroopUiState.Playing(
                trial = t.currentQuestion(),
                number = t.progress + 1,
                total = t.trials.size,
                timeLimitMs = t.difficulty.timeLimitMs,
            )
        }
    }
}
