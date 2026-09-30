package com.buse.korteks.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolDigitTaskTest {

    @Test
    fun `anahtar her sembole farkli bir rakam verir`() {
        for (d in SymbolDigitDifficulty.entries) {
            val key = SymbolDigitTask(d, Random(1)).key
            assertEquals(d.symbolCount, key.size)
            assertEquals((1..d.symbolCount).toSet(), key.values.toSet())
        }
    }

    @Test
    fun `dogru rakam dogru sayilir ve ayni sembol arka arkaya gelmez`() {
        val t = SymbolDigitTask(SymbolDigitDifficulty.ORTA, Random(2))
        repeat(200) {
            val symbol = t.currentQuestion()
            assertTrue(t.answer(t.key.getValue(symbol), 500))
            assertNotEquals(symbol, t.currentQuestion())
        }
        assertEquals(200, t.correct)
    }

    @Test
    fun `yanlis cevap puan dusurur, sure dolunca gorev biter`() {
        val t = SymbolDigitTask(SymbolDigitDifficulty.KOLAY, Random(3))
        t.answer(t.key.getValue(t.currentQuestion()), 400)          // +100
        val wrongDigit = t.key.getValue(t.currentQuestion()) % t.key.size + 1
        assertFalse(t.answer(wrongDigit, 400))                      // -50
        assertEquals(50, t.result().score)
        t.timeout()
        assertTrue(t.isFinished)
    }
}

class NBackTaskTest {

    @Test
    fun `eslesme sayisi her kanalda tam tutar`() {
        for (d in NBackDifficulty.entries) {
            for (seed in 0 until 50) {
                val t = NBackTask(d, Random(seed))
                val expected = d.scoredSteps * 30 / 100
                assertEquals(d.n + d.scoredSteps, t.stimuli.size)
                assertEquals(expected, t.stimuli.indices.count { t.isPositionMatch(it) })
                assertEquals(expected, t.stimuli.indices.count { t.isLetterMatch(it) })
            }
        }
    }

    @Test
    fun `hep dogru basan oyuncu yuzde yuz alir`() {
        val t = NBackTask(NBackDifficulty.ORTA, Random(7))
        while (!t.isFinished) {
            val i = t.progress
            t.answer(NBackResponse(t.isPositionMatch(i), t.isLetterMatch(i)), 600)
        }
        assertEquals(100, t.result().accuracyPercent)
        assertEquals(0, t.falseAlarms)
    }

    @Test
    fun `hic basmayan oyuncu eslesmeleri kacirir`() {
        val t = NBackTask(NBackDifficulty.KOLAY, Random(8))
        while (!t.isFinished) t.timeout()
        assertEquals(0, t.hits)
        assertEquals(2 * 6, t.misses) // 20 adımın %30'u = 6, iki kanal
    }

    @Test
    fun `ilk N adim puanlanmaz`() {
        val t = NBackTask(NBackDifficulty.ZOR, Random(9))
        repeat(3) { t.answer(NBackResponse(position = true, letter = true), 100) }
        assertEquals(0, t.falseAlarms + t.hits)
    }
}

class CorsiTaskTest {

    @Test
    fun `dizi tekrarsizdir ve dogru uzunluktadir`() {
        val t = CorsiTask(CorsiDifficulty.ORTA, Random(1))
        val seq = t.currentQuestion()
        assertEquals(3, seq.size)
        assertEquals(seq.size, seq.distinct().size)
        assertTrue(seq.all { it in 0 until CORSI_BLOCKS })
    }

    @Test
    fun `dogru cevap diziyi uzatir ve araligi gunceller`() {
        val t = CorsiTask(CorsiDifficulty.KOLAY, Random(2))
        assertTrue(t.answer(t.expectedAnswer(), 1000))
        assertEquals(3, t.currentLength)
        assertEquals(2, t.span)
    }

    @Test
    fun `ayni uzunlukta iki yanlis gorevi bitirir`() {
        val t = CorsiTask(CorsiDifficulty.ORTA, Random(3))
        t.timeout()
        assertFalse(t.isFinished)
        t.timeout()
        assertTrue(t.isFinished)
    }

    @Test
    fun `tersten modda ters sira beklenir`() {
        val t = CorsiTask(CorsiDifficulty.ZOR, Random(4))
        val shown = t.currentQuestion()
        assertEquals(shown.reversed(), t.expectedAnswer())
        assertFalse(t.answer(shown, 1000)) // düz sıra yanlış
    }

    @Test
    fun `en uzun diziye ulasinca gorev biter`() {
        val t = CorsiTask(CorsiDifficulty.ORTA, Random(5))
        while (!t.isFinished) t.answer(t.expectedAnswer(), 1000)
        assertEquals(CORSI_MAX_LENGTH, t.span)
    }
}

class HanoiTaskTest {

    /** Klasik özyinelemeli (recursive) çözüm: n diski from→to taşı, via yardımcı çubuk. */
    private fun solve(t: HanoiTask, n: Int, from: Int, to: Int, via: Int) {
        if (n == 0) return
        solve(t, n - 1, from, via, to)
        assertTrue(t.answer(HanoiMove(from, to), 1000L * t.moves))
        solve(t, n - 1, via, to, from)
    }

    @Test
    fun `en az hamle sayisi 2 uzeri n eksi 1`() {
        assertEquals(7, HanoiTask(HanoiDifficulty.KOLAY).minimumMoves)
        assertEquals(15, HanoiTask(HanoiDifficulty.ORTA).minimumMoves)
        assertEquals(31, HanoiTask(HanoiDifficulty.ZOR).minimumMoves)
    }

    @Test
    fun `buyuk disk kucugun ustune konamaz`() {
        val t = HanoiTask(HanoiDifficulty.KOLAY)
        assertTrue(t.answer(HanoiMove(0, 1), 0))   // disk 1 → orta
        assertFalse(t.answer(HanoiMove(0, 1), 0))  // disk 2, disk 1'in üstüne: kural dışı
        assertFalse(t.answer(HanoiMove(2, 0), 0))  // boş çubuktan hamle: kural dışı
        assertEquals(1, t.moves)
    }

    @Test
    fun `optimal cozum yuzde yuz verimlilik verir`() {
        for (d in HanoiDifficulty.entries) {
            val t = HanoiTask(d)
            solve(t, d.disks, 0, 2, 1)
            assertTrue(t.isSolved)
            assertEquals(t.minimumMoves, t.moves)
            assertEquals(100, t.result().accuracyPercent)
            assertEquals(100 * d.disks * 10, t.result().score)
        }
    }

    @Test
    fun `sure dolarsa cozulmemis sayilir`() {
        val t = HanoiTask(HanoiDifficulty.KOLAY)
        t.answer(HanoiMove(0, 2), 0)
        t.timeout()
        assertTrue(t.isFinished)
        assertFalse(t.isSolved)
        assertEquals(0, t.result().score)
    }
}
