package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.HanoiMove
import com.buse.korteks.game.HanoiTask
import com.buse.korteks.game.InkColor
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.TaskResult

private sealed interface HanoiPhase {
    data object Intro : HanoiPhase
    data class Playing(val task: HanoiTask) : HanoiPhase
    data class Finished(val task: HanoiTask, val result: TaskResult, val reward: GameReward? = null) : HanoiPhase
}

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun HanoiScreen(onBack: () -> Unit, onGameFinished: (TaskResult) -> GameReward? = { null }) {
    var phase by remember { mutableStateOf<HanoiPhase>(HanoiPhase.Intro) }
    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (phase is HanoiPhase.Intro) onBack() else phase = HanoiPhase.Intro }

    when (val p = phase) {
        HanoiPhase.Intro -> TaskIntro(
            emoji = "🗼",
            title = "Planlama",
            info = listOf(
                "Hanoi Kulesi, 1883'te matematikçi É. Lucas'ın tanıttığı bir bulmacadır. Nöropsikolojide " +
                    "planlama ve problem çözme (yürütücü işlevler, executive functions) görevi olarak kullanılır.",
                "Birkaç hamle sonrasını zihinde planlamayı gerektirir. n disk için en az hamle sayısı 2ⁿ − 1'dir.",
            ),
            howTo = "Bütün diskleri soldaki çubuktan sağdaki çubuğa taşı. Önce diski almak istediğin çubuğa, sonra " +
                "koymak istediğin çubuğa dokun. Kurallar: tek seferde bir disk, büyük disk küçüğün üstüne konamaz.",
            choices = HanoiDifficulty.entries.map { d ->
                "${d.title}  ·  ${d.disks} disk  ·  en az ${(1 shl d.disks) - 1} hamle  ·  ${d.timeLimitMs / 1000} sn" to
                    { phase = HanoiPhase.Playing(HanoiTask(d)) }
            },
            onBack = onBack,
        )
        is HanoiPhase.Playing -> key(p.task) {
            HanoiPlaying(p.task, onFinished = { phase = HanoiPhase.Finished(p.task, it, onGameFinished(it)) })
        }
        is HanoiPhase.Finished -> ResultScreen(
            header = "🗼 HANOİ · ${p.task.difficulty.title.uppercase()} · " + if (p.task.isSolved) "ÇÖZÜLDÜ" else "SÜRE DOLDU",
            score = p.result.score,
            stats = listOf(
                "Hamle" to "${p.task.moves}",
                "En az" to "${p.task.minimumMoves}",
                "Süre" to if (p.task.isSolved) "${formatSeconds(p.result.averageReactionMs)} sn" else "—",
            ),
            onReplay = { phase = HanoiPhase.Playing(HanoiTask(p.task.difficulty)) },
            onMenu = { phase = HanoiPhase.Intro },
            reward = p.reward,
        )
    }
}

@Composable
internal fun HanoiPlaying(task: HanoiTask, onFinished: (TaskResult) -> Unit) {
    var moveNo by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) } // seçili (disk alınacak) çubuk
    val limitMs = task.difficulty.timeLimitMs

    val elapsedMs by rememberTrialClock(Unit, limitMs) {
        if (!task.isFinished) {
            task.timeout()
            onFinished(task.result())
        }
    }

    fun onPegTap(peg: Int) {
        if (task.isFinished) return
        val from = selected
        if (from == null) {
            if (task.currentQuestion()[peg].isNotEmpty()) selected = peg
        } else {
            if (task.answer(HanoiMove(from, peg), elapsedMs)) moveNo++
            selected = null
            if (task.isFinished) onFinished(task.result())
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader(
            "Hamle: ${task.moves}  ·  En az: ${task.minimumMoves}  ·  ${(limitMs - elapsedMs).coerceAtLeast(0) / 1000} sn",
            1f - elapsedMs.toFloat() / limitMs,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (selected == null) "Taşımak istediğin diskin çubuğuna dokun" else "Nereye? Hedef çubuğa dokun",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        // pointerInput: ham dokunma olaylarını alır. Dokunulan x konumundan hangi çubuk olduğu hesaplanır.
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("hanoi_alan")
                .pointerInput(Unit) {
                    detectTapGestures { pos -> onPegTap((pos.x / (size.width / 3f)).toInt().coerceIn(0, 2)) }
                },
        ) {
            // moveNo ve selected'ı burada okumak, değiştiklerinde Canvas'ın yeniden çizilmesini sağlar
            moveNo
            drawHanoi(task.currentQuestion(), task.difficulty.disks, selected)
        }
    }
}

private fun DrawScope.drawHanoi(pegs: List<List<Int>>, diskCount: Int, selected: Int?) {
    val pegWidth = size.width / 3f
    val baseY = size.height * 0.8f
    val diskHeight = size.height * 0.07f
    val poleTop = baseY - diskHeight * (diskCount + 1.5f)

    // Taban
    drawRoundRect(
        Color(0xFF6D4C41),
        topLeft = Offset(0f, baseY),
        size = Size(size.width, diskHeight * 0.4f),
        cornerRadius = CornerRadius(8f),
    )

    for (p in 0..2) {
        val centerX = pegWidth * (p + 0.5f)
        // Çubuk (seçiliyse vurgulu)
        drawRect(
            if (selected == p) Color(0xFFD1C4E9) else Color(0xFF8D6E63),
            topLeft = Offset(centerX - 6f, poleTop),
            size = Size(12f, baseY - poleTop),
        )
        pegs[p].forEachIndexed { level, disk ->
            // Seçili çubuğun en üstteki diski havaya kaldırılmış çizilir
            val lifted = selected == p && level == pegs[p].lastIndex
            val width = pegWidth * (0.3f + 0.62f * disk / diskCount)
            val top = if (lifted) poleTop - diskHeight * 1.4f else baseY - diskHeight * (level + 1)
            drawRoundRect(
                InkColor.entries[(disk - 1) % InkColor.entries.size].toColor(),
                topLeft = Offset(centerX - width / 2, top),
                size = Size(width, diskHeight * 0.9f),
                cornerRadius = CornerRadius(diskHeight / 2),
            )
        }
    }
}
