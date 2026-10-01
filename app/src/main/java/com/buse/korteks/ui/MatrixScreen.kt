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
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.data.MatrixPuzzleLoader
import com.buse.korteks.game.Figure
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.Shape
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.TaskResult

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun MatrixScreen(
    onBack: () -> Unit,
    onGameFinished: (TaskResult) -> GameReward? = { null },
    vm: MatrixViewModel = viewModel(),
) {
    val context = LocalContext.current
    // JSON dosyası bir kez okunur, ekran yeniden çizildikçe tekrar okunmaz
    val puzzles = remember { MatrixPuzzleLoader.load(context) }
    val state = vm.state

    /** Hamleyi uygula; hamle oyunu bitirdiyse (Playing → Finished) ilerlemeyi bir kez kaydet. */
    fun move(action: () -> Unit) {
        val wasPlaying = vm.state is MatrixUiState.Playing
        action()
        val now = vm.state
        if (wasPlaying && now is MatrixUiState.Finished) vm.attachReward(onGameFinished(now.result))
    }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (state is MatrixUiState.Intro) onBack() else vm.backToIntro() }

    when (state) {
        MatrixUiState.Intro -> TaskIntro(
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
                    { vm.start(puzzles, d) }
            },
            onBack = onBack,
        )
        is MatrixUiState.Playing -> MatrixPlaying(
            state,
            onAnswer = { index, reactionMs, number -> move { vm.answer(index, reactionMs, number) } },
            onTimeout = { number -> move { vm.timeout(number) } },
        )
        is MatrixUiState.Finished -> ResultScreen(
            header = "🔷 MATRİS · ${state.difficulty.title.uppercase()}",
            score = state.result.score,
            stats = listOf(
                "Doğruluk" to "%${state.result.accuracyPercent}",
                "Doğru" to "${state.result.correct}/${state.result.total}",
                "Ort. süre" to if (state.result.averageReactionMs > 0) "${formatSeconds(state.result.averageReactionMs)} sn" else "—",
            ),
            onReplay = { vm.start(puzzles, state.difficulty) },
            onMenu = vm::backToIntro,
            reward = state.reward,
        )
    }
}

/** Oyun ekranı: durumsuz. Ne göstereceğini [state]'ten alır, olayları yukarı bildirir. */
@Composable
internal fun MatrixPlaying(
    state: MatrixUiState.Playing,
    onAnswer: (optionIndex: Int, reactionMs: Long, trialNumber: Int) -> Unit,
    onTimeout: (trialNumber: Int) -> Unit,
) {
    val elapsedMs by rememberTrialClock(state.number, state.timeLimitMs) { onTimeout(state.number) }
    val q = state.question

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("${state.number} / ${state.total}", 1f - elapsedMs.toFloat() / state.timeLimitMs)
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
                        onClick = { onAnswer(index, elapsedMs, state.number) },
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
