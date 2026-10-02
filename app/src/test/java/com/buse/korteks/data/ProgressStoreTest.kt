package com.buse.korteks.data

import com.buse.korteks.game.PlayerProgress
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ProgressStoreTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `hic kayit yokken bos ilerleme doner`() {
        assertEquals(PlayerProgress(), ProgressStore(context).load())
    }

    @Test
    fun `kaydedilen ilerleme yeni bir nesneyle aynen geri okunur`() {
        val p = PlayerProgress(
            totalXp = 1234,
            streakDays = 5,
            longestStreak = 9,
            lastPlayedDate = LocalDate.of(2026, 10, 1),
            bestScores = mapOf("STROOP_ZOR" to 2140, "HANOI_KOLAY" to 3000),
        )
        ProgressStore(context).save(p)
        assertEquals(p, ProgressStore(context).load()) // uygulama yeniden açılmış gibi
    }
}
