package com.buse.korteks.game

import kotlin.random.Random

/** OK (ok işareti) asimetrik tek şekil: döndürülünce fark görünür. Diğerleri simetrik. */
enum class Shape { DAIRE, KARE, UCGEN, ELMAS, OK }

/** Bir hücredeki çizim: [count] tane aynı şekil, aynı renk ve açıyla. */
data class Figure(val shape: Shape, val color: InkColor, val count: Int, val rotation: Int)

const val MAX_FIGURE_COUNT = 4
private val ROTATIONS = listOf(0, 45, 90, 135, 180, 225, 270, 315)

/**
 * Bir özelliğin 3x3 tabloda nasıl değiştiği:
 *  SABIT: her hücrede values[0]
 *  SATIR: satır numarasına göre (her satır kendi değerinde)
 *  SUTUN: sütun numarasına göre
 *  LATIN: her satır ve sütunda her değer bir kez (sudoku gibi)
 */
enum class RuleType { SABIT, SATIR, SUTUN, LATIN }

data class AttributeRule<T>(val type: RuleType, val values: List<T>) {
    fun valueAt(row: Int, col: Int): T = values[
        when (type) {
            RuleType.SABIT -> 0
            RuleType.SATIR -> row
            RuleType.SUTUN -> col
            RuleType.LATIN -> (row + col) % 3
        }
    ]
}

/** JSON'daki bir bulmacanın tarifi. Hücreler ve cevap bu kurallardan hesaplanır. */
data class MatrixPuzzleSpec(
    val id: Int,
    val level: Int,
    val shape: AttributeRule<Shape>,
    val color: AttributeRule<InkColor>,
    val count: AttributeRule<Int>,
    val rotation: AttributeRule<Int>,
) {
    fun figureAt(row: Int, col: Int) = Figure(
        shape = shape.valueAt(row, col),
        color = color.valueAt(row, col),
        count = count.valueAt(row, col),
        rotation = rotation.valueAt(row, col),
    )

    /** Eksik hücre her zaman sağ alt köşe (2, 2). */
    val answer: Figure get() = figureAt(2, 2)

    private val rotationVisible: Boolean
        get() = shape.type == RuleType.SABIT && shape.values[0] == Shape.OK

    /** Veri kurallara uymuyorsa açıklayıcı mesajla hata fırlatır (bozuk JSON'u erken yakalamak için). */
    fun validate() {
        for ((name, rule) in listOf("sekil" to shape, "renk" to color, "adet" to count, "aci" to rotation)) {
            val needed = if (rule.type == RuleType.SABIT) 1 else 3
            require(rule.values.size == needed) { "Bulmaca $id: '$name' ${rule.type} kuralı $needed değer ister" }
            if (rule.type != RuleType.SABIT) {
                require(rule.values.distinct().size == 3) { "Bulmaca $id: '$name' değerleri birbirinden farklı olmalı" }
            }
        }
        require(count.values.all { it in 1..MAX_FIGURE_COUNT }) { "Bulmaca $id: adet 1..$MAX_FIGURE_COUNT olmalı" }
        require(rotation.values.all { it in ROTATIONS }) { "Bulmaca $id: açı 45'in katı olmalı" }
        // Daire 90° döndürülse de aynı görünür → cevap belirsizleşir. Açı sadece OK şeklinde değişebilir.
        if (rotation.type != RuleType.SABIT || rotation.values[0] != 0) {
            require(rotationVisible) { "Bulmaca $id: açı değişiyorsa şekil SABIT ve OK olmalı" }
        }
    }

    /** Renk kuralı var mı? (renk körü modunda bu kural kaldırılır) */
    val usesColor: Boolean get() = color.type != RuleType.SABIT

    /**
     * Renk körü modu için: renk sabitlenir (bütün hücreler ilk renkte). Başka değişen özellik
     * kalmıyorsa (bulmaca sadece renkten ibaretse) null döner → bu bulmaca bu modda kullanılmaz.
     */
    fun withoutColorRule(): MatrixPuzzleSpec? {
        if (!usesColor) return this
        val stripped = copy(color = AttributeRule(RuleType.SABIT, listOf(color.values[0])))
        val othersVary = listOf(shape.type, count.type, rotation.type).any { it != RuleType.SABIT }
        return if (othersVary) stripped else null
    }

    /**
     * Soruyu oluşturur: 8 hücre + eksik hücre (null) + seçenekler.
     * Her çeldirici, doğru cevaptan TEK bir özellikte farklıdır → tam olarak bir doğru seçenek olur.
     */
    fun buildQuestion(optionCount: Int, random: Random, colorDistractors: Boolean = true): MatrixQuestion {
        val answer = answer
        // "İnandırıcı" çeldiriciler: bulmacada zaten geçen değerleri kullananlar. Önce bunlar seçilir.
        val plausible = mutableListOf<Figure>()
        val others = mutableListOf<Figure>()

        fun <T> addVariants(rule: AttributeRule<T>, all: List<T>, current: T, make: (T) -> Figure) {
            for (v in all) {
                if (v == current) continue
                if (v in rule.values) plausible += make(v) else others += make(v)
            }
        }
        addVariants(shape, Shape.entries, answer.shape) { answer.copy(shape = it) }
        // Renk körü modunda sadece renkte farklı seçenek üretilmez (iki seçenek aynı görünebilir)
        if (colorDistractors) addVariants(color, InkColor.entries, answer.color) { answer.copy(color = it) }
        addVariants(count, (1..MAX_FIGURE_COUNT).toList(), answer.count) { answer.copy(count = it) }
        if (rotationVisible) addVariants(rotation, ROTATIONS, answer.rotation) { answer.copy(rotation = it) }

        val distractors = (plausible.shuffled(random) + others.shuffled(random)).distinct().take(optionCount - 1)
        val options = (distractors + answer).shuffled(random)

        val cells = (0 until 9).map { i -> if (i == 8) null else figureAt(i / 3, i % 3) }
        return MatrixQuestion(puzzleId = id, cells = cells, options = options, correctIndex = options.indexOf(answer))
    }
}

data class MatrixQuestion(
    val puzzleId: Int,
    /** 9 hücre, satır satır. Son hücre (index 8) null = eksik olan. */
    val cells: List<Figure?>,
    val options: List<Figure>,
    val correctIndex: Int,
)

enum class MatrixDifficulty(
    val title: String,
    val levels: IntRange,
    val puzzleCount: Int,
    val optionCount: Int,
    val timeLimitMs: Long,
) {
    KOLAY("Kolay", levels = 1..2, puzzleCount = 6, optionCount = 4, timeLimitMs = 40_000),
    ORTA("Orta", levels = 1..3, puzzleCount = 8, optionCount = 6, timeLimitMs = 30_000),
    ZOR("Zor", levels = 2..3, puzzleCount = 8, optionCount = 6, timeLimitMs = 20_000),
}

/** Cevap = seçilen seçeneğin sırası (index). */
class MatrixTask(
    allPuzzles: List<MatrixPuzzleSpec>,
    val difficulty: MatrixDifficulty,
    random: Random = Random.Default,
    colorBlindSafe: Boolean = false,
) : CognitiveTask<MatrixQuestion, Int> {

    // Zorluğa uyan bulmacalardan rastgele seç, sonra kolaydan zora sırala.
    // Renk körü modunda renk kuralları kaldırılır, sadece renkten ibaret bulmacalar çıkar.
    val questions: List<MatrixQuestion> = allPuzzles
        .mapNotNull { if (colorBlindSafe) it.withoutColorRule() else it }
        .filter { it.level in difficulty.levels }
        .shuffled(random)
        .take(difficulty.puzzleCount)
        .sortedBy { it.level }
        .map { it.buildQuestion(difficulty.optionCount, random, colorDistractors = !colorBlindSafe) }

    var progress = 0
        private set

    private var correct = 0
    private var score = 0
    private val correctReactionTimes = mutableListOf<Long>()

    override val isFinished: Boolean get() = progress >= questions.size

    override fun currentQuestion(): MatrixQuestion = questions[progress]

    override fun answer(answer: Int, reactionMs: Long): Boolean {
        check(!isFinished) { "Görev bitti, cevap alınamaz" }
        val isCorrect = answer == questions[progress].correctIndex
        if (isCorrect) {
            correct++
            correctReactionTimes += reactionMs
            val remainingMs = (difficulty.timeLimitMs - reactionMs).coerceAtLeast(0)
            score += 100 + (remainingMs * 100 / difficulty.timeLimitMs).toInt()
        }
        progress++
        return isCorrect
    }

    override fun timeout() {
        check(!isFinished) { "Görev bitti" }
        progress++
    }

    override fun result() = TaskResult(
        correct = correct,
        total = questions.size,
        averageReactionMs = if (correctReactionTimes.isEmpty()) 0 else correctReactionTimes.average().toLong(),
        score = score,
    )
}
