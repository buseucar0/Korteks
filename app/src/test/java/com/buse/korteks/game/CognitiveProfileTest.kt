package com.buse.korteks.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CognitiveProfileTest {

    private fun key(game: GameType, d: String) = GameRecord.bestKeyOf(game, d)

    @Test
    fun `kusursuz zor 100, kusursuz kolay 60 puan`() {
        val max = CognitiveProfile.maxScore(GameType.STROOP, "ZOR")
        assertEquals(100, CognitiveProfile.scoreFor(GameType.STROOP, mapOf(key(GameType.STROOP, "ZOR") to max)))
        val kolay = CognitiveProfile.maxScore(GameType.STROOP, "KOLAY")
        assertEquals(60, CognitiveProfile.scoreFor(GameType.STROOP, mapOf(key(GameType.STROOP, "KOLAY") to kolay)))
    }

    @Test
    fun `birden cok zorlukta en yuksek deger alinir`() {
        val best = mapOf(
            key(GameType.HANOI, "KOLAY") to 3000,  // kusursuz kolay → 60
            key(GameType.HANOI, "ZOR") to 2500,    // %50 zor → 50
        )
        assertEquals(60, CognitiveProfile.scoreFor(GameType.HANOI, best))
    }

    @Test
    fun `oynanmamis gorev bos, hiz puani 100'de kirpilir`() {
        val profile = CognitiveProfile.of(mapOf(key(GameType.SPEED, "ZOR") to 99_999))
        assertEquals(100, profile[GameType.SPEED])
        assertNull(profile[GameType.MATRIX])
        assertEquals(GameType.entries.toSet(), profile.keys)
    }

    @Test
    fun `en yuksek puanlar gorevlerin puan formulleriyle uyumlu`() {
        assertEquals(20 * 200, CognitiveProfile.maxScore(GameType.STROOP, "ZOR"))
        assertEquals(1000 * 5, CognitiveProfile.maxScore(GameType.HANOI, "ZOR"))
        // Corsi ORTA: 3'ten 9'a hatasız (7 doğru), aralık 9
        assertEquals(9 * 100 + 7 * 20, CognitiveProfile.maxScore(GameType.CORSI, "ORTA"))
        // N-Back 2-back: kanal başına 6 eşleşme → 12 isabet, 28 doğru ret
        assertEquals((12 * 100 + 28 * 20) * 2, CognitiveProfile.maxScore(GameType.NBACK, "ORTA"))
    }
}
