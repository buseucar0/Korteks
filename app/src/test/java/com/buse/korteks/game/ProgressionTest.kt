package com.buse.korteks.game

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {

    private val day = LocalDate.of(2026, 10, 1)

    private fun rec(score: Int, game: GameType = GameType.STROOP, difficulty: String = "KOLAY") =
        GameRecord(game, difficulty, TaskResult(correct = 0, total = 0, averageReactionMs = 0, score = score))

    private fun play(p: PlayerProgress, date: LocalDate, score: Int = 0) =
        Progression.recordGame(p, rec(score), date).after

    // ---------------------------------------------------------------- XP ve seviye

    @Test
    fun `xp puandan hesaplanir, 10 ile 110 arasinda kalir`() {
        assertEquals(10, Progression.xpFor(0))
        assertEquals(10, Progression.xpFor(-50))
        assertEquals(60, Progression.xpFor(500))
        assertEquals(110, Progression.xpFor(1000))
        assertEquals(110, Progression.xpFor(25_000))
    }

    @Test
    fun `seviye esikleri 100, 150, 200`() {
        assertEquals(LevelInfo(1, 0, 100), Progression.levelInfo(0))
        assertEquals(LevelInfo(1, 99, 100), Progression.levelInfo(99))
        assertEquals(LevelInfo(2, 0, 150), Progression.levelInfo(100))   // tam eşikte seviye atlar
        assertEquals(LevelInfo(2, 149, 150), Progression.levelInfo(249))
        assertEquals(LevelInfo(3, 0, 200), Progression.levelInfo(250))
        assertEquals(0.5f, Progression.levelInfo(175).fraction, 0.001f)
    }

    @Test
    fun `seviye atlama odulde gorunur`() {
        val p = PlayerProgress(totalXp = 95)
        val reward = Progression.recordGame(p, rec(0), day) // +10 XP → 105
        assertEquals(10, reward.xpGained)
        assertTrue(reward.leveledUp)
        assertEquals(2, reward.levelAfter)
        assertFalse(Progression.recordGame(reward.after, rec(0), day).leveledUp)
    }

    // ---------------------------------------------------------------- Seri

    @Test
    fun `ilk oyun seriyi 1 yapar`() {
        val p = play(PlayerProgress(), day)
        assertEquals(1, p.streakDays)
        assertEquals(day, p.lastPlayedDate)
    }

    @Test
    fun `ayni gun birden cok oyun seriyi artirmaz`() {
        val p = play(play(play(PlayerProgress(), day), day), day)
        assertEquals(1, p.streakDays)
        assertEquals(30, p.totalXp)
    }

    @Test
    fun `ardisik gunler seriyi artirir`() {
        var p = PlayerProgress()
        repeat(5) { p = play(p, day.plusDays(it.toLong())) }
        assertEquals(5, p.streakDays)
        assertEquals(5, p.longestStreak)
    }

    @Test
    fun `bir gun atlanirsa seri 1'den baslar, en uzun seri korunur`() {
        var p = PlayerProgress()
        repeat(4) { p = play(p, day.plusDays(it.toLong())) }   // 4 gün seri
        p = play(p, day.plusDays(5))                            // 4. günü atladı
        assertEquals(1, p.streakDays)
        assertEquals(4, p.longestStreak)
    }

    @Test
    fun `ay ve yil donumunde seri bozulmaz`() {
        val p1 = play(play(PlayerProgress(), LocalDate.of(2026, 10, 31)), LocalDate.of(2026, 11, 1))
        assertEquals(2, p1.streakDays)
        val p2 = play(play(PlayerProgress(), LocalDate.of(2026, 12, 31)), LocalDate.of(2027, 1, 1))
        assertEquals(2, p2.streakDays)
        val p3 = play(play(PlayerProgress(), LocalDate.of(2028, 2, 28)), LocalDate.of(2028, 2, 29)) // artık yıl
        assertEquals(2, p3.streakDays)
    }

    @Test
    fun `saat geri alinirsa seri ve son gun bozulmaz`() {
        val p = play(play(PlayerProgress(), day), day.minusDays(3))
        assertEquals(1, p.streakDays)
        assertEquals(day, p.lastPlayedDate)
    }

    @Test
    fun `gorunen seri, oyun kacirilinca 0 olur`() {
        val p = play(play(PlayerProgress(), day), day.plusDays(1)) // 2 gün seri
        assertEquals(2, Progression.currentStreak(p, day.plusDays(1)))  // bugün oynadı
        assertEquals(2, Progression.currentStreak(p, day.plusDays(2)))  // dün oynadı: seri hâlâ kurtarılabilir
        assertEquals(0, Progression.currentStreak(p, day.plusDays(3)))  // bir gün kaçtı
        assertEquals(0, Progression.currentStreak(PlayerProgress(), day))
    }

    // ---------------------------------------------------------------- Rekor

    @Test
    fun `ilk puanli oyun rekor olur, dusuk puan rekoru bozmaz`() {
        val r1 = Progression.recordGame(PlayerProgress(), rec(800), day)
        assertTrue(r1.isNewBest)
        assertEquals(null, r1.previousBest)
        assertEquals(800, r1.after.bestScores["STROOP_KOLAY"])

        val r2 = Progression.recordGame(r1.after, rec(500), day)
        assertFalse(r2.isNewBest)
        assertEquals(800, r2.previousBest)
        assertEquals(800, r2.after.bestScores["STROOP_KOLAY"])

        val r3 = Progression.recordGame(r2.after, rec(950), day)
        assertTrue(r3.isNewBest)
        assertEquals(950, r3.after.bestScores["STROOP_KOLAY"])
    }

    @Test
    fun `sifir puan ve esit puan rekor sayilmaz`() {
        assertFalse(Progression.recordGame(PlayerProgress(), rec(0), day).isNewBest)
        val p = Progression.recordGame(PlayerProgress(), rec(700), day).after
        assertFalse(Progression.recordGame(p, rec(700), day).isNewBest)
    }

    @Test
    fun `rekorlar gorev ve zorluk basina ayri tutulur`() {
        var p = Progression.recordGame(PlayerProgress(), rec(900, GameType.STROOP, "ZOR"), day).after
        val other = Progression.recordGame(p, rec(100, GameType.STROOP, "KOLAY"), day)
        assertTrue(other.isNewBest) // KOLAY'ın rekoru ZOR'dan bağımsız
        p = Progression.recordGame(other.after, rec(50, GameType.HANOI, "ZOR"), day).after
        assertEquals(mapOf("STROOP_ZOR" to 900, "STROOP_KOLAY" to 100, "HANOI_ZOR" to 50), p.bestScores)
    }
}
