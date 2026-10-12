package com.buse.korteks.ui

import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.NBackResponse
import com.buse.korteks.game.NBackTask
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** N-Back alıştırma turunun açıklamaları ve akışı (düz JVM). */
class NBackPracticeTest {

    private val task = NBackTask(NBackDifficulty.KOLAY, Random(3), scoredSteps = 10)

    /** Alıştırma görevinde koşulu sağlayan ilk adım. */
    private fun firstStep(condition: (Int) -> Boolean) = (1 until task.stimuli.size).first(condition)

    @Test
    fun `kisa gorev istenen adim sayisinda ve dogru oranda eslesmeli`() {
        assertEquals(1 + 10, task.stimuli.size)
        assertEquals(3, task.stimuli.indices.count { task.isPositionMatch(it) })
    }

    @Test
    fun `dogru basis dogru, eksik ya da fazla basis aciklanir`() {
        val step = firstStep { task.isPositionMatch(it) && !task.isLetterMatch(it) }
        assertEquals("✓ Doğru", NBackViewModel.explain(task, step, NBackResponse(position = true, letter = false)))
        assertEquals("Konum aynıydı, KONUM'a basmalıydın", NBackViewModel.explain(task, step, NBackResponse(false, false)))
        assertEquals(
            "Harf farklıydı, HARF'e basmamalıydın",
            NBackViewModel.explain(task, step, NBackResponse(position = true, letter = true)),
        )
    }

    @Test
    fun `ilk N adimda aciklama yok`() {
        assertNull(NBackViewModel.explain(task, 0, NBackResponse(false, false)))
    }

    @Test
    fun `alistirma bitince sonuc alistirma olarak isaretlenir`() {
        val vm = NBackViewModel(Random(4))
        vm.startPractice()
        val total = (vm.state as NBackUiState.Playing).total
        assertTrue((vm.state as NBackUiState.Playing).practice)
        for (n in 1..total) vm.stepEnded(NBackResponse(false, false), 0, n)
        assertTrue((vm.state as NBackUiState.Finished).practice)
    }
}
