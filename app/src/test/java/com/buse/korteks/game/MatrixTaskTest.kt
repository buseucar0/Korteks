package com.buse.korteks.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixTaskTest {

    private fun <T> sabit(v: T) = AttributeRule(RuleType.SABIT, listOf(v))

    private val shapeRows = MatrixPuzzleSpec(
        id = 1, level = 1,
        shape = AttributeRule(RuleType.SATIR, listOf(Shape.DAIRE, Shape.KARE, Shape.UCGEN)),
        color = sabit(InkColor.MAVI),
        count = AttributeRule(RuleType.SUTUN, listOf(1, 2, 3)),
        rotation = sabit(0),
    )

    private val arrowLatin = MatrixPuzzleSpec(
        id = 2, level = 3,
        shape = sabit(Shape.OK),
        color = AttributeRule(RuleType.LATIN, listOf(InkColor.KIRMIZI, InkColor.MAVI, InkColor.YESIL)),
        count = sabit(1),
        rotation = AttributeRule(RuleType.SUTUN, listOf(0, 90, 180)),
    )

    @Test
    fun `kurallar dogru hesaplanir`() {
        assertEquals(Figure(Shape.UCGEN, InkColor.MAVI, 3, 0), shapeRows.answer)
        // LATIN: (2+2)%3 = 1 → MAVI
        assertEquals(InkColor.MAVI, arrowLatin.answer.color)
        assertEquals(180, arrowLatin.answer.rotation)
    }

    @Test
    fun `her soruda tam olarak bir dogru secenek vardir`() {
        for (spec in listOf(shapeRows, arrowLatin)) {
            for (optionCount in listOf(4, 6)) {
                for (seed in 0 until 200) {
                    val q = spec.buildQuestion(optionCount, Random(seed))
                    assertEquals(optionCount, q.options.size)
                    assertEquals(optionCount, q.options.distinct().size) // aynı seçenek iki kez yok
                    assertEquals(1, q.options.count { it == spec.answer })
                    assertEquals(spec.answer, q.options[q.correctIndex])
                }
            }
        }
    }

    @Test
    fun `eksik hucre sag alt kosedir`() {
        val q = shapeRows.buildQuestion(4, Random(1))
        assertEquals(9, q.cells.size)
        assertNull(q.cells[8])
        assertTrue(q.cells.take(8).all { it != null })
    }

    @Test
    fun `gecerli bulmacalar dogrulamadan gecer`() {
        shapeRows.validate()
        arrowLatin.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `dairede aci degisirse gecersizdir`() {
        shapeRows.copy(rotation = AttributeRule(RuleType.SUTUN, listOf(0, 90, 180))).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `eksik deger sayisi gecersizdir`() {
        shapeRows.copy(color = AttributeRule(RuleType.SATIR, listOf(InkColor.MAVI))).validate()
    }

    @Test
    fun `gorev zorluga gore bulmaca secer ve kolaydan zora siralar`() {
        val puzzles = (1..20).map { shapeRows.copy(id = it, level = 1 + it % 3) }
        val task = MatrixTask(puzzles, MatrixDifficulty.ZOR, Random(3))
        assertEquals(MatrixDifficulty.ZOR.puzzleCount, task.questions.size)
        val levels = task.questions.map { q -> puzzles.first { it.id == q.puzzleId }.level }
        assertTrue(levels.all { it in MatrixDifficulty.ZOR.levels })
        assertEquals(levels.sorted(), levels)
    }

    @Test
    fun `dogru secenek puan kazandirir`() {
        val task = MatrixTask(listOf(shapeRows), MatrixDifficulty.KOLAY, Random(5))
        assertTrue(task.answer(task.currentQuestion().correctIndex, 5_000))
        assertTrue(task.isFinished)
        assertEquals(1, task.result().correct)
        assertTrue(task.result().score > 100)
    }

    @Test
    fun `renk koru modunda renk kurali kalkar, sadece renkten ibaret bulmaca cikar`() {
        val colorOnly = shapeRows.copy(
            shape = sabit(Shape.KARE),
            count = sabit(1),
            color = AttributeRule(RuleType.SATIR, listOf(InkColor.KIRMIZI, InkColor.MAVI, InkColor.YESIL)),
        )
        assertNull(colorOnly.withoutColorRule())
        val stripped = arrowLatin.withoutColorRule()!!
        assertEquals(RuleType.SABIT, stripped.color.type)
        assertEquals(RuleType.SUTUN, stripped.rotation.type) // diğer kurallar aynen kalır
    }

    @Test
    fun `renk koru modunda secenekler renkle ayrilmaz`() {
        for (seed in 0 until 100) {
            val q = shapeRows.buildQuestion(6, Random(seed), colorDistractors = false)
            assertTrue(q.options.all { it.color == shapeRows.answer.color })
            assertEquals(6, q.options.distinct().size)
        }
    }
}
