package com.buse.korteks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import com.buse.korteks.data.MatrixPuzzleLoader
import com.buse.korteks.game.CorsiDifficulty
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.StroopDifficulty
import com.buse.korteks.game.SymbolDigitDifficulty
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.PlayerProgress
import com.buse.korteks.game.TaskResult
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Oynanış testleri: ekranlar emülatörsüz (Robolectric ile JVM'de) çalışır, test butonlara basar.
 *
 * Saat elle ilerletilir (mainClock.autoAdvance = false). Böylece "2 sn sonra süre doluyor mu?"
 * gibi şeyler gerçekten beklemeden, her seferinde aynı şekilde test edilir.
 * Sabit tohumlu (Random(x)) görev nesnesini test de tuttuğu için doğru cevabı biliyoruz.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class GameplayTest {

    @get:Rule
    val rule = createComposeRule()

    private fun start(content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { AppTheme { content() } }
        frame()
    }

    /** Bir ekran karesi ilerlet (tıklamadan sonra ekranın güncellenmesi için). */
    private fun frame() = rule.mainClock.advanceTimeByFrame()

    private fun advance(ms: Long) = rule.mainClock.advanceTimeBy(ms)

    private fun tap(matcher: SemanticsMatcher) {
        rule.onNode(matcher).performClick()
        frame()
    }

    private fun button(text: String) = hasText(text) and hasClickAction()

    private fun assertText(text: String) = rule.onNodeWithText(text).assertExists()

    // ---------------------------------------------------------------- Stroop

    /** Stroop artık ViewModel üzerinden: test ViewModel'i sabit tohumla kurup ekrana verir. */
    private fun stroop(difficulty: StroopDifficulty, seed: Int): StroopViewModel {
        val vm = StroopViewModel(Random(seed))
        vm.start(difficulty)
        start { StroopScreen(onBack = {}, vm = vm) }
        return vm
    }

    private val StroopViewModel.playing get() = state as StroopUiState.Playing

    @Test
    fun `stroop - hep murekkep rengine basan oyuncu yuzde yuz alir`() {
        val vm = stroop(StroopDifficulty.KOLAY, seed = 1)
        while (vm.state is StroopUiState.Playing) {
            assertText("${vm.playing.number} / ${vm.playing.total}")
            tap(button(vm.playing.trial.ink.label))
        }
        val result = (vm.state as StroopUiState.Finished).result
        assertEquals(100, result.accuracyPercent)
        assertEquals(StroopDifficulty.KOLAY.trialCount, result.correct)
        assertText("Tekrar oyna") // sonuç ekranı çizildi
    }

    @Test
    fun `stroop - oyun bitince ilerleme tam bir kez kaydedilir`() {
        var calls = 0
        val fakeReward = GameReward(xpGained = 60, before = PlayerProgress(totalXp = 50), after = PlayerProgress(totalXp = 110))
        val vm = StroopViewModel(Random(1))
        vm.start(StroopDifficulty.KOLAY)
        start { StroopScreen(onBack = {}, onGameFinished = { calls++; fakeReward }, vm = vm) }

        while (vm.state is StroopUiState.Playing) tap(button(vm.playing.trial.ink.label))
        advance(3_000) // sonuç ekranı birkaç saniye çizilsin: yeniden çizim tekrar kayıt yapmamalı
        assertEquals(1, calls)
        assertText("+60 XP")
        assertText("🎉 Seviye 2!") // 50 → 110 XP: seviye 1 → 2
    }

    @Test
    fun `stroop - kelimenin anlamina basmak yanlis sayilir`() {
        val vm = stroop(StroopDifficulty.ZOR, seed = 2) // ZOR: hepsi uyumsuz
        tap(button(vm.playing.trial.word.label))
        assertText("2 / 20")
    }

    @Test
    fun `stroop - sure dolunca sonraki soruya gecilir`() {
        stroop(StroopDifficulty.ORTA, seed = 3) // soru başına 2 sn
        assertText("1 / 16")
        advance(1_900)
        assertText("1 / 16")
        advance(200)
        assertText("2 / 16")
    }

    // ---------------------------------------------------------------- Matris

    @Test
    fun `matris - JSON'daki her bulmaca gecerli ve tek dogru cevapli`() {
        val puzzles = MatrixPuzzleLoader.load(RuntimeEnvironment.getApplication())
        assertEquals(18, puzzles.size)
        for (p in puzzles) {
            for (seed in 0 until 50) {
                val q = p.buildQuestion(optionCount = 6, random = Random(seed))
                assertEquals("Bulmaca ${p.id}", 1, q.options.count { it == p.answer })
            }
        }
    }

    @Test
    fun `matris - dogru secenege basan oyuncu yuzde yuz alir`() {
        val puzzles = MatrixPuzzleLoader.load(RuntimeEnvironment.getApplication())
        val vm = MatrixViewModel(Random(4))
        vm.start(puzzles, MatrixDifficulty.ORTA)
        start { MatrixScreen(onBack = {}, vm = vm) }

        while (vm.state is MatrixUiState.Playing) {
            tap(hasTestTag("secenek_${(vm.state as MatrixUiState.Playing).question.correctIndex}"))
        }
        assertEquals(100, (vm.state as MatrixUiState.Finished).result.accuracyPercent)
    }

    // ---------------------------------------------------------------- Hız

    @Test
    fun `hiz - dogru ve yanlis cevaplar sayilir, sure dolunca biter`() {
        val vm = SymbolDigitViewModel(Random(5))
        vm.start(SymbolDigitDifficulty.ORTA)
        start { SymbolDigitScreen(onBack = {}, vm = vm) }
        fun playing() = vm.state as SpeedUiState.Playing
        fun digitOf(p: SpeedUiState.Playing) = p.key.first { it.first == p.symbol }.second

        repeat(10) {
            advance(300)
            tap(button("${digitOf(playing())}"))
        }
        tap(button("${digitOf(playing()) % 9 + 1}")) // yanlış rakam
        assertTrue(vm.state is SpeedUiState.Playing)

        advance(60_000)
        val result = (vm.state as SpeedUiState.Finished).result
        assertEquals(10, result.correct)
        assertEquals(11, result.total)
        assertEquals(950, result.score)
        assertTrue("tepki süresi ölçülmeli", result.averageReactionMs in 250..400)
    }

    // ---------------------------------------------------------------- N-Back

    @Test
    fun `nback - uyaran 1 sn sonra kaybolur, ilk N adimda butonlar kapali`() {
        val vm = NBackViewModel(Random(6))
        vm.start(NBackDifficulty.ORTA)
        start { NBackScreen(onBack = {}, vm = vm) }
        val letter = (vm.state as NBackUiState.Playing).stimulus.letter.toString()
        assertText(letter)
        rule.onNode(button("📍 KONUM")).assertIsNotEnabled()
        advance(1_100)
        rule.onNodeWithText(letter).assertDoesNotExist()
    }

    @Test
    fun `nback - eslesmelere dogru basan oyuncu yuzde yuz alir`() {
        val vm = NBackViewModel(Random(7))
        vm.start(NBackDifficulty.ORTA)
        start { NBackScreen(onBack = {}, vm = vm) }
        val task = vm.task!!

        while (vm.state is NBackUiState.Playing) {
            val step = task.progress
            if (task.isPositionMatch(step)) tap(button("📍 KONUM"))
            if (task.isLetterMatch(step)) tap(button("🔤 HARF"))
            // adım süresi dolana kadar saati ilerlet
            while (task.progress == step) advance(100)
            frame()
        }
        val finished = vm.state as NBackUiState.Finished
        assertEquals(100, finished.result.accuracyPercent)
        assertEquals(0, finished.falseAlarms)
    }

    // ---------------------------------------------------------------- Corsi

    /** Dizinin gösterimi bitene kadar bekle: 800 ms başlangıç + blok başına 700 + 250 ms. */
    private fun waitForShow(length: Int) = advance(800L + length * 950L + 100)

    @Test
    fun `corsi - gosterim sirasinda dokunma sayilmaz, dogru sira diziyi uzatir`() {
        val vm = CorsiViewModel(Random(8))
        vm.start(CorsiDifficulty.ORTA)
        start { CorsiScreen(onBack = {}, vm = vm) }

        assertText("İzle…")
        tap(hasTestTag("blok_0")) // gösterim sırasında: yok sayılmalı
        waitForShow(3)
        assertText("Aynı sırayla dokun  (0/3)")

        vm.task!!.expectedAnswer().forEach { tap(hasTestTag("blok_$it")) }
        advance(100) // son dokunuşta iki durum birden değişiyor, ekrana bir kare sonra yansır
        assertText("✓ Doğru!")
        advance(1_000)
        assertText("Dizi uzunluğu: 4  ·  En iyi: 3")
    }

    @Test
    fun `corsi - ayni uzunlukta iki yanlis oyunu bitirir`() {
        val vm = CorsiViewModel(Random(9))
        vm.start(CorsiDifficulty.ORTA)
        start { CorsiScreen(onBack = {}, vm = vm) }

        repeat(2) {
            waitForShow(3)
            vm.task!!.expectedAnswer().reversed().forEach { tap(hasTestTag("blok_$it")) } // ters sıra = yanlış
            advance(100)
            assertText("✗ Yanlış")
            advance(1_000)
        }
        assertEquals(0, (vm.state as CorsiUiState.Finished).result.correct)
    }

    // ---------------------------------------------------------------- Hanoi

    private fun tapPeg(peg: Int) {
        rule.onNodeWithTag("hanoi_alan").performTouchInput {
            click(Offset(width * (peg + 0.5f) / 3f, height / 2f))
        }
        frame()
    }

    private fun hanoi(): HanoiViewModel {
        val vm = HanoiViewModel()
        vm.start(HanoiDifficulty.KOLAY)
        start { HanoiScreen(onBack = {}, vm = vm) }
        return vm
    }

    @Test
    fun `hanoi - kural disi hamle uygulanmaz`() {
        val vm = hanoi()
        tapPeg(0)
        assertText("Nereye? Hedef çubuğa dokun")
        tapPeg(1) // küçük disk → orta: geçerli
        tapPeg(0)
        tapPeg(1) // orta boy disk küçüğün üstüne: kural dışı
        val playing = vm.state as HanoiUiState.Playing
        assertEquals(1, playing.moves)
        assertEquals(listOf(listOf(3, 2), listOf(1), emptyList()), playing.pegs)
    }

    @Test
    fun `hanoi - dokunarak optimal cozum tam puan verir`() {
        val vm = hanoi()
        val optimal = listOf(0 to 2, 0 to 1, 2 to 1, 0 to 2, 1 to 0, 1 to 2, 0 to 2)
        for ((from, to) in optimal) {
            advance(1_000)
            tapPeg(from)
            tapPeg(to)
        }
        val finished = vm.state as HanoiUiState.Finished
        assertTrue(finished.solved)
        assertEquals(7, finished.moves)
        assertEquals(3000, finished.result.score)
    }

    @Test
    fun `hanoi - sure dolarsa oyun biter`() {
        val vm = hanoi()
        advance(91_000)
        val finished = vm.state as HanoiUiState.Finished
        assertEquals(false, finished.solved)
        assertEquals(0, finished.result.score)
    }
}
