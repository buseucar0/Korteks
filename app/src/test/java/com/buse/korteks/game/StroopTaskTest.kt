package com.buse.korteks.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StroopTaskTest {

    // Sabit tohum (seed): her test çalıştırmasında aynı "rastgele" sorular üretilir → tekrarlanabilir test
    private fun task(d: StroopDifficulty, seed: Int = 42) = StroopTask(d, Random(seed))

    @Test
    fun `dogru cevap her zaman murekkep rengidir`() {
        for (d in StroopDifficulty.entries) {
            for (seed in 0 until 100) {
                for (trial in task(d, seed).trials) {
                    assertEquals(trial.ink, trial.correctAnswer)
                    assertTrue(trial.ink in trial.options)
                }
            }
        }
    }

    @Test
    fun `soru sayisi ve renk sayisi zorluga uyar`() {
        for (d in StroopDifficulty.entries) {
            val t = task(d)
            assertEquals(d.trialCount, t.trials.size)
            t.trials.forEach { assertEquals(d.colorCount, it.options.size) }
        }
    }

    @Test
    fun `uyumlu soru orani tam tutar`() {
        for (d in StroopDifficulty.entries) {
            val expected = d.trialCount * d.congruentPercent / 100
            assertEquals(expected, task(d).trials.count { it.isCongruent })
        }
    }

    @Test
    fun `zor seviyede hic uyumlu soru yoktur`() {
        for (seed in 0 until 100) {
            assertTrue(task(StroopDifficulty.ZOR, seed).trials.none { it.isCongruent })
        }
    }

    @Test
    fun `murekkep rengi dogru, kelime anlami yanlis sayilir`() {
        val t = task(StroopDifficulty.ZOR)
        val first = t.currentQuestion()
        assertFalse(t.answer(first.word, 500)) // uyumsuz: kelimeyi seçmek yanlış
        val second = t.currentQuestion()
        assertTrue(t.answer(second.ink, 500))
    }

    @Test
    fun `sure dolunca yanlis sayilir ve sonraki soruya gecilir`() {
        val t = task(StroopDifficulty.KOLAY)
        t.timeout()
        assertEquals(1, t.progress)
        assertEquals(0, t.result().correct)
    }

    @Test
    fun `hizli cevap daha cok puan alir`() {
        val fast = task(StroopDifficulty.ORTA).apply { answer(currentQuestion().ink, 300) }
        val slow = task(StroopDifficulty.ORTA).apply { answer(currentQuestion().ink, 1800) }
        assertTrue(fast.result().score > slow.result().score)
    }

    @Test
    fun `hepsi dogru cevaplaninca sonuc yuzde yuz olur`() {
        val t = task(StroopDifficulty.ORTA)
        while (!t.isFinished) t.answer(t.currentQuestion().ink, 1000)
        val r = t.result()
        assertEquals(100, r.accuracyPercent)
        assertEquals(1000, r.averageReactionMs)
    }

    @Test(expected = IllegalStateException::class)
    fun `bitmis goreve cevap verilemez`() {
        val t = task(StroopDifficulty.KOLAY)
        repeat(t.trials.size) { t.timeout() }
        t.answer(InkColor.KIRMIZI, 100)
    }

    @Test
    fun `renk koru modunda sadece guvenli renkler, en fazla 4 renk`() {
        for (d in StroopDifficulty.entries) {
            val t = StroopTask(d, Random(1), colorBlindSafe = true)
            val used = t.trials.flatMap { listOf(it.word, it.ink) }.toSet()
            assertTrue(used.all { it in COLOR_BLIND_SAFE_COLORS })
            assertEquals(minOf(d.colorCount, 4), t.trials.first().options.size)
        }
        // Kolay: 3 renk = güvenli listenin ilk 3'ü
        assertEquals(COLOR_BLIND_SAFE_COLORS.take(3), StroopTask(StroopDifficulty.KOLAY, Random(1), colorBlindSafe = true).trials.first().options)
    }
}
