package com.buse.korteks.game

enum class HanoiDifficulty(val title: String, val disks: Int, val timeLimitMs: Long) {
    KOLAY("Kolay", disks = 3, timeLimitMs = 90_000),
    ORTA("Orta", disks = 4, timeLimitMs = 180_000),
    ZOR("Zor", disks = 5, timeLimitMs = 300_000),
}

/** Bir hamle: [from] çubuğunun en üstündeki diski [to] çubuğuna taşı (çubuklar 0, 1, 2). */
data class HanoiMove(val from: Int, val to: Int)

/**
 * Hanoi Kulesi. Soru = çubukların anlık durumu, cevap = bir hamle.
 * Diğer görevlerden farklı olarak tek bir "soru" vardır ve çok hamlede çözülür.
 * Hedef: bütün diskleri sol çubuktan (0) sağ çubuğa (2) taşımak.
 *
 * TaskResult'ta: correct = en az hamle sayısı (çözüldüyse), total = yapılan hamle,
 * yani accuracyPercent = verimlilik. averageReactionMs = toplam çözüm süresi.
 */
class HanoiTask(val difficulty: HanoiDifficulty) : CognitiveTask<List<List<Int>>, HanoiMove> {

    /** Her çubuk alttan üste disk boyutlarının listesi (büyük sayı = büyük disk). */
    private val pegs: List<MutableList<Int>> = List(3) { mutableListOf<Int>() }.also {
        it[0].addAll(difficulty.disks downTo 1)
    }

    /** En az hamle sayısı: 2^n - 1 */
    val minimumMoves: Int = (1 shl difficulty.disks) - 1

    var moves = 0
        private set
    private var gaveUp = false
    private var solveTimeMs = 0L

    val isSolved: Boolean get() = pegs[2].size == difficulty.disks

    override val isFinished: Boolean get() = gaveUp || isSolved

    override fun currentQuestion(): List<List<Int>> = pegs.map { it.toList() }

    /** Kural: çubuk boş olmamalı, büyük disk küçük diskin üstüne konamaz. */
    fun canMove(from: Int, to: Int): Boolean {
        if (from == to || from !in 0..2 || to !in 0..2 || pegs[from].isEmpty()) return false
        return pegs[to].isEmpty() || pegs[to].last() > pegs[from].last()
    }

    /** Geçerli hamleyse uygular ve true döner. reactionMs = oyun başından beri geçen süre. */
    override fun answer(answer: HanoiMove, reactionMs: Long): Boolean {
        check(!isFinished) { "Görev bitti" }
        if (!canMove(answer.from, answer.to)) return false
        // removeAt(lastIndex): removeLast() eski Android sürümlerinde çökebiliyor
        val disk = pegs[answer.from].removeAt(pegs[answer.from].lastIndex)
        pegs[answer.to].add(disk)
        moves++
        if (isSolved) solveTimeMs = reactionMs
        return true
    }

    /** Süre doldu → görev biter, çözülmemiş sayılır. */
    override fun timeout() {
        gaveUp = true
    }

    /** Puan: verimlilik (en az / yapılan) × disk sayısı. Çözülmediyse 0. */
    override fun result(): TaskResult {
        val efficiency = if (isSolved && moves > 0) minimumMoves * 100 / moves else 0
        return TaskResult(
            correct = if (isSolved) minimumMoves else 0,
            total = moves,
            averageReactionMs = solveTimeMs,
            score = efficiency * difficulty.disks * 10,
        )
    }
}
