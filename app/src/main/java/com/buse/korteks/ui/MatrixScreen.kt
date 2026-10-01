package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.data.MatrixPuzzleLoader
import com.buse.korteks.game.Figure
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.MatrixTask
import com.buse.korteks.game.Shape
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.TaskResult

private sealed interface MatrixPhase {
    data object Intro : MatrixPhase
    data class Playing(val task: MatrixTask) : MatrixPhase
    data class Finished(val difficulty: MatrixDifficulty, val result: TaskResult, val reward: GameReward? = null) : MatrixPhase
}

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun MatrixScreen(onBack: () -> Unit, onGameFinished: (TaskResult) -> GameReward? = { null }) {
    val context = LocalContext.current
    // JSON dosyası bir kez okunur, ekran yeniden çizildikçe tekrar okunmaz
    val puzzles = remember { MatrixPuzzleLoader.load(context) }
    var phase by remember { mutableStateOf<MatrixPhase>(MatrixPhase.Intro) }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön

    BackHandler { if (phase is MatrixPhase.Intro) onBack() else phase = MatrixPhase.Intro }

    when (val p = phase) {
        MatrixPhase.Intro -> TaskIntro(
            emoji = "🔷",
            title = "Mantık",
            info = listOf(
                "Bu bulmacalar, J. C. Raven'ın 1930'larda geliştirdiği Progresif Matrisler testinden esinlenir. " +
                    "Raven tarzı matrisler, IQ testlerinde ve araştırmalarda en sık kullanılan görevlerdendir.",
                "Dil ve bilgi gerektirmeden, yeni bir örüntüdeki kuralı bulup uygulama becerisini " +
                    "(akıcı akıl yürütme, fluid reasoning) ölçmek için kullanılır.",
            ),
            howTo = "3x3 tablodaki şekiller satır ve sütunlarda bir kurala göre değişir (şekil, renk, adet, açı). " +
                "Kuralı bul ve sağ alttaki eksik hücreye uyan seçeneği seç.",
            choices = MatrixDifficulty.entries.map { d ->
                "${d.title}  ·  ${d.puzzleCount} bulmaca  ·  ${d.optionCount} seçenek  ·  ${d.timeLimitMs / 1000} sn" to
                    { phase = MatrixPhase.Playing(MatrixTask(puzzles, d)) }
            },
            onBack = onBack,
        )
        is MatrixPhase.Playing -> key(p.task) {
            MatrixPlaying(p.task, onFinished = { phase = MatrixPhase.Finished(p.task.difficulty, it, onGameFinished(it)) })
        }
        is MatrixPhase.Finished -> ResultScreen(
            header = "🔷 MATRİS · ${p.difficulty.title.uppercase()}",
            score = p.result.score,
            stats = listOf(
                "Doğruluk" to "%${p.result.accuracyPercent}",
                "Doğru" to "${p.result.correct}/${p.result.total}",
                "Ort. süre" to if (p.result.averageReactionMs > 0) "${formatSeconds(p.result.averageReactionMs)} sn" else "—",
            ),
            onReplay = { phase = MatrixPhase.Playing(MatrixTask(puzzles, p.difficulty)) },
            onMenu = { phase = MatrixPhase.Intro },
            reward = p.reward,
        )
    }
}

@Composable
internal fun MatrixPlaying(task: MatrixTask, onFinished: (TaskResult) -> Unit) {
    var trialNo by remember { mutableIntStateOf(0) }
    val shownTrial = task.progress
    val limitMs = task.difficulty.timeLimitMs

    fun goNext() {
        trialNo = task.progress
        if (task.isFinished) onFinished(task.result())
    }

    val elapsedMs by rememberTrialClock(trialNo, limitMs) {
        if (!task.isFinished && task.progress == shownTrial) {
            task.timeout()
            goNext()
        }
    }

    if (task.isFinished) return
    val q = task.currentQuestion()

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("${task.progress + 1} / ${task.questions.size}", 1f - elapsedMs.toFloat() / limitMs)
        Spacer(Modifier.height(20.dp))

        // 3x3 tablo
        Column(Modifier.fillMaxWidth(0.9f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            q.cells.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { figure ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(2.dp, Color(0xFF444444), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (figure == null) {
                                Text("?", fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            } else {
                                FigureCanvas(figure)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Seçenekler: 4 tane → 2'li satırlar, 6 tane → 3'lü satırlar
        val perRow = if (q.options.size <= 4) 2 else 3
        q.options.withIndex().chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (index, figure) ->
                    Card(
                        onClick = {
                            if (task.progress == shownTrial) {
                                task.answer(index, elapsedMs)
                                goNext()
                            }
                        },
                        modifier = Modifier.weight(1f).aspectRatio(if (perRow == 2) 1.6f else 1f).testTag("secenek_$index"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                    ) {
                        FigureCanvas(figure)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FigureCanvas(figure: Figure) {
    Canvas(Modifier.fillMaxSize().padding(6.dp)) { drawFigure(figure) }
}

/** Adet'e göre şekillerin hücre içindeki merkezleri (0..1 arası oran olarak). */
private val LAYOUTS = mapOf(
    1 to listOf(Offset(0.5f, 0.5f)),
    2 to listOf(Offset(0.28f, 0.5f), Offset(0.72f, 0.5f)),
    3 to listOf(Offset(0.5f, 0.27f), Offset(0.27f, 0.72f), Offset(0.73f, 0.72f)),
    4 to listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.28f), Offset(0.28f, 0.72f), Offset(0.72f, 0.72f)),
)

/**
 * Canvas çizimi: DrawScope içinde size = çizim alanının piksel boyutu.
 * Gömülüdeki bir framebuffer'a çizmek gibi, ama koordinat sistemi float ve (0,0) sol üst köşe.
 */
private fun DrawScope.drawFigure(figure: Figure) {
    val color = figure.color.toColor()
    val radius = size.minDimension * if (figure.count == 1) 0.32f else 0.19f
    for (rel in LAYOUTS.getValue(figure.count)) {
        val center = Offset(size.width * rel.x, size.height * rel.y)
        rotate(figure.rotation.toFloat(), pivot = center) {
            drawShape(figure.shape, center, radius, color)
        }
    }
}

private fun DrawScope.drawShape(shape: Shape, c: Offset, r: Float, color: Color) {
    when (shape) {
        Shape.DAIRE -> drawCircle(color, r, c)
        Shape.KARE -> drawRect(color, topLeft = Offset(c.x - r * 0.85f, c.y - r * 0.85f), size = Size(r * 1.7f, r * 1.7f))
        Shape.UCGEN -> drawPath(polygon(c, r, 0f to -1f, 0.95f to 0.75f, -0.95f to 0.75f), color)
        Shape.ELMAS -> drawPath(polygon(c, r, 0f to -1f, 0.75f to 0f, 0f to 1f, -0.75f to 0f), color)
        // Yukarı bakan ok: döndürme açısı net görünsün diye asimetrik
        Shape.OK -> drawPath(
            polygon(c, r, 0f to -1f, 0.85f to -0.1f, 0.3f to -0.1f, 0.3f to 1f, -0.3f to 1f, -0.3f to -0.1f, -0.85f to -0.1f),
            color,
        )
    }
}

/** Merkez + yarıçapa göre oranlanmış noktalardan kapalı çokgen (polygon) yolu oluşturur. */
private fun polygon(c: Offset, r: Float, vararg points: Pair<Float, Float>) = Path().apply {
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) moveTo(c.x + x * r, c.y + y * r) else lineTo(c.x + x * r, c.y + y * r)
    }
    close()
}
