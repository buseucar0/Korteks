package com.buse.korteks.ui

import com.buse.korteks.game.StroopDifficulty
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ViewModel testleri: ekran yok, Robolectric yok, düz JVM.
 * Ekrandan taşınan "yarış durumu" kontrolleri artık burada doğrudan test edilebiliyor.
 */
class StroopViewModelTest {

    private fun started(d: StroopDifficulty = StroopDifficulty.ORTA) =
        StroopViewModel(Random(1)).apply { start(d) }

    private val StroopViewModel.playing get() = state as StroopUiState.Playing

    @Test
    fun `baslangicta giris ekrani, start ile ilk soru`() {
        val vm = StroopViewModel(Random(1))
        assertEquals(StroopUiState.Intro, vm.state)
        vm.start(StroopDifficulty.KOLAY)
        assertEquals(1, vm.playing.number)
        assertEquals(12, vm.playing.total)
    }

    @Test
    fun `ayni soruya ikinci cevap yok sayilir (cift tiklama)`() {
        val vm = started()
        val first = vm.playing
        vm.answer(first.trial.ink, 500, trialNumber = 1)
        vm.answer(first.trial.ink, 510, trialNumber = 1) // aynı karede ikinci tıklama
        assertEquals(2, vm.playing.number)
    }

    @Test
    fun `eski sorunun sayaci yeni soruyu atlatmaz`() {
        val vm = started()
        vm.answer(vm.playing.trial.ink, 500, trialNumber = 1)
        vm.timeout(trialNumber = 1) // 1. sorunun geç kalmış sayacı
        assertEquals(2, vm.playing.number)
        vm.timeout(trialNumber = 2)
        assertEquals(3, vm.playing.number)
    }

    @Test
    fun `son sorudan sonra sonuc, tekrar oyna yeni oyun baslatir`() {
        val vm = started(StroopDifficulty.KOLAY)
        repeat(12) { vm.timeout(trialNumber = it + 1) }
        val finished = vm.state as StroopUiState.Finished
        assertEquals(0, finished.result.correct)
        vm.start(finished.difficulty)
        assertEquals(1, vm.playing.number)
    }

    @Test
    fun `giris ekranina donunce oyun birakilir`() {
        val vm = started()
        vm.backToIntro()
        assertEquals(StroopUiState.Intro, vm.state)
        vm.timeout(trialNumber = 1) // bırakılmış oyunun sayacı: etkisiz
        assertTrue(vm.state is StroopUiState.Intro)
    }
}
