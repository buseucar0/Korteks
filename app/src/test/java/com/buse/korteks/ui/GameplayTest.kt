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
import com.buse.korteks.game.CorsiTask
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.HanoiTask
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.MatrixTask
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.NBackTask
import com.buse.korteks.game.StroopDifficulty
import com.buse.korteks.game.StroopTask
import com.buse.korteks.game.SymbolDigitDifficulty
import com.buse.korteks.game.SymbolDigitTask
import com.buse.korteks.game.TaskResult
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `stroop - hep murekkep rengine basan oyuncu yuzde yuz alir`() {
        val task = StroopTask(StroopDifficulty.KOLAY, Random(1))
        var result: TaskResult? = null
        start { StroopPlaying(task) { result = it } }

        while (result == null) {
            assertText("${task.progress + 1} / ${task.trials.size}")
            tap(button(task.currentQuestion().ink.label))
        }
        assertEquals(100, result!!.accuracyPercent)
        assertEquals(task.trials.size, result!!.correct)
    }

    @Test
    fun `stroop - kelimenin anlamina basmak yanlis sayilir`() {
        val task = StroopTask(StroopDifficulty.ZOR, Random(2)) // ZOR: hepsi uyumsuz
        start { StroopPlaying(task) {} }
        tap(button(task.currentQuestion().word.label))
        assertEquals(0, task.result().correct)
        assertText("2 / 20")
    }

    @Test
    fun `stroop - sure dolunca sonraki soruya gecilir`() {
        val task = StroopTask(StroopDifficulty.ORTA, Random(3)) // soru başına 2 sn
        start { StroopPlaying(task) {} }
        assertText("1 / 16")
        advance(1_900)
        assertText("1 / 16")
        advance(200)
        assertText("2 / 16")
        assertEquals(0, task.result().correct)
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
        val task = MatrixTask(puzzles, MatrixDifficulty.ORTA, Random(4))
        var result: TaskResult? = null
        start { MatrixPlaying(task) { result = it } }

        while (result == null) {
            tap(hasTestTag("secenek_${task.currentQuestion().correctIndex}"))
        }
        assertEquals(100, result!!.accuracyPercent)
    }

    // ---------------------------------------------------------------- Hız

    @Test
    fun `hiz - dogru ve yanlis cevaplar sayilir, sure dolunca biter`() {
        val task = SymbolDigitTask(SymbolDigitDifficulty.ORTA, Random(5))
        var result: TaskResult? = null
        start { SymbolDigitPlaying(task) { result = it } }

        repeat(10) {
            advance(300)
            tap(button("${task.key.getValue(task.currentQuestion())}"))
        }
        val wrong = task.key.getValue(task.currentQuestion()) % 9 + 1
        tap(button("$wrong"))
        assertNull(result)

        advance(60_000)
        assertNotNull(result)
        assertEquals(10, result!!.correct)
        assertEquals(11, result!!.total)
        assertEquals(950, result!!.score)
        assertTrue("tepki süresi ölçülmeli", result!!.averageReactionMs in 250..400)
    }

    // ---------------------------------------------------------------- N-Back

    @Test
    fun `nback - uyaran 1 sn sonra kaybolur, ilk N adimda butonlar kapali`() {
        val task = NBackTask(NBackDifficulty.ORTA, Random(6))
        start { NBackPlaying(task) {} }
        val letter = task.currentQuestion().letter.toString()
        assertText(letter)
        rule.onNode(button("📍 KONUM")).assertIsNotEnabled()
        advance(1_100)
        rule.onNodeWithText(letter).assertDoesNotExist()
    }

    @Test
    fun `nback - eslesmelere dogru basan oyuncu yuzde yuz alir`() {
        val task = NBackTask(NBackDifficulty.ORTA, Random(7))
        var result: TaskResult? = null
        start { NBackPlaying(task) { result = it } }

        while (result == null) {
            val step = task.progress
            if (task.isPositionMatch(step)) tap(button("📍 KONUM"))
            if (task.isLetterMatch(step)) tap(button("🔤 HARF"))
            // adım süresi dolana kadar saati ilerlet
            while (task.progress == step) advance(100)
            frame()
        }
        assertEquals(100, result!!.accuracyPercent)
        assertEquals(0, task.falseAlarms)
    }

    // ---------------------------------------------------------------- Corsi

    /** Dizinin gösterimi bitene kadar bekle: 800 ms başlangıç + blok başına 700 + 250 ms. */
    private fun waitForShow(length: Int) = advance(800L + length * 950L + 100)

    @Test
    fun `corsi - gosterim sirasinda dokunma sayilmaz, dogru sira diziyi uzatir`() {
        val task = CorsiTask(CorsiDifficulty.ORTA, Random(8))
        start { CorsiPlaying(task) {} }

        assertText("İzle…")
        tap(hasTestTag("blok_0")) // gösterim sırasında: yok sayılmalı
        waitForShow(3)
        assertText("Aynı sırayla dokun  (0/3)")

        task.expectedAnswer().forEach { tap(hasTestTag("blok_$it")) }
        assertText("✓ Doğru!")
        advance(1_000)
        assertText("Dizi uzunluğu: 4  ·  En iyi: 3")
    }

    @Test
    fun `corsi - ayni uzunlukta iki yanlis oyunu bitirir`() {
        val task = CorsiTask(CorsiDifficulty.ORTA, Random(9))
        var result: TaskResult? = null
        start { CorsiPlaying(task) { result = it } }

        repeat(2) {
            waitForShow(3)
            task.expectedAnswer().reversed().forEach { tap(hasTestTag("blok_$it")) } // ters sıra = yanlış
            assertText("✗ Yanlış")
            advance(1_000)
        }
        assertNotNull(result)
        assertEquals(0, result!!.correct)
    }

    // ---------------------------------------------------------------- Hanoi

    private fun tapPeg(peg: Int) {
        rule.onNodeWithTag("hanoi_alan").performTouchInput {
            click(Offset(width * (peg + 0.5f) / 3f, height / 2f))
        }
        frame()
    }

    @Test
    fun `hanoi - kural disi hamle uygulanmaz`() {
        val task = HanoiTask(HanoiDifficulty.KOLAY)
        start { HanoiPlaying(task) {} }

        tapPeg(0)
        assertText("Nereye? Hedef çubuğa dokun")
        tapPeg(1) // küçük disk → orta: geçerli
        tapPeg(0)
        tapPeg(1) // orta boy disk küçüğün üstüne: kural dışı
        assertEquals(1, task.moves)
        assertEquals(listOf(listOf(3, 2), listOf(1), emptyList()), task.currentQuestion())
    }

    @Test
    fun `hanoi - dokunarak optimal cozum tam puan verir`() {
        val task = HanoiTask(HanoiDifficulty.KOLAY)
        var result: TaskResult? = null
        start { HanoiPlaying(task) { result = it } }

        val optimal = listOf(0 to 2, 0 to 1, 2 to 1, 0 to 2, 1 to 0, 1 to 2, 0 to 2)
        for ((from, to) in optimal) {
            advance(1_000)
            tapPeg(from)
            tapPeg(to)
        }
        assertNotNull(result)
        assertTrue(task.isSolved)
        assertEquals(7, task.moves)
        assertEquals(3000, result!!.score)
    }

    @Test
    fun `hanoi - sure dolarsa oyun biter`() {
        val task = HanoiTask(HanoiDifficulty.KOLAY)
        var result: TaskResult? = null
        start { HanoiPlaying(task) { result = it } }
        advance(91_000)
        assertNotNull(result)
        assertEquals(0, result!!.score)
    }
}
