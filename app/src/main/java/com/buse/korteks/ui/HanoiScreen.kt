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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.game.GameRecord
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.GameType
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.InkColor

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * bestScore: zorluk başına rekor (giriş ekranındaki zorluk butonlarında gösterilir).
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun HanoiScreen(
    onBack: () -> Unit,
    onGameFinished: (GameRecord) -> GameReward? = { null },
    bestScore: (difficulty: String) -> Int? = { null },
    vm: HanoiViewModel = viewModel(),
) {
    val state = vm.state

    /** Hamleyi uygula; hamle oyunu bitirdiyse (Playing → Finished) ilerlemeyi bir kez kaydet. */
    fun move(action: () -> Unit) {
        val wasPlaying = vm.state is HanoiUiState.Playing
        action()
        val now = vm.state
        if (wasPlaying && now is HanoiUiState.Finished) {
            vm.attachReward(onGameFinished(GameRecord(GameType.HANOI, now.difficulty.name, now.result)))
        }
    }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (state is HanoiUiState.Intro) onBack() else vm.backToIntro() }

    when (state) {
        HanoiUiState.Intro -> TaskIntro(
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
                withBest("${d.title}  ·  ${d.disks} disk  ·  en az ${(1 shl d.disks) - 1} hamle  ·  ${d.timeLimitMs / 1000} sn", bestScore(d.name)) to
                    { vm.start(d) }
            },
            onBack = onBack,
        )
        is HanoiUiState.Playing -> HanoiPlaying(
            state,
            initialElapsedMs = vm.sessionElapsedMs,
            onTick = { vm.sessionElapsedMs = it },
            onMove = { from, to -> move { vm.move(from, to) } },
            onTimeout = { gameNo -> move { vm.timeout(gameNo) } },
        )
        is HanoiUiState.Finished -> ResultScreen(
            header = "🗼 HANOİ · ${state.difficulty.title.uppercase()} · " + if (state.solved) "ÇÖZÜLDÜ" else "SÜRE DOLDU",
            score = state.result.score,
            stats = listOf(
                "Hamle" to "${state.moves}",
                "En az" to "${state.minimumMoves}",
                "Süre" to if (state.solved) "${formatSeconds(state.result.averageReactionMs)} sn" else "—",
            ),
            onReplay = { vm.start(state.difficulty) },
            onMenu = vm::backToIntro,
            reward = state.reward,
        )
    }
}

/** Oyun ekranı. Seçili çubuk (disk alınacak yer) bu ekranın geçici durumu. */
@Composable
internal fun HanoiPlaying(
    state: HanoiUiState.Playing,
    initialElapsedMs: Long,
    onTick: (Long) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onTimeout: (gameNo: Int) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    val elapsedMs by rememberTrialClock(state.gameNo, state.timeLimitMs, initialElapsedMs, onTick) { onTimeout(state.gameNo) }

    // pointerInput bir kez kurulur; içinden her zaman en güncel durumu okumak için rememberUpdatedState
    val latestState by rememberUpdatedState(state)

    fun onPegTap(peg: Int) {
        val from = selected
        if (from == null) {
            if (latestState.pegs[peg].isNotEmpty()) selected = peg
        } else {
            onMove(from, peg) // kural dışıysa ViewModel yok sayar
            selected = null
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader(
            "Hamle: ${state.moves}  ·  En az: ${state.minimumMoves}  ·  ${(state.timeLimitMs - elapsedMs).coerceAtLeast(0) / 1000} sn",
            1f - elapsedMs.toFloat() / state.timeLimitMs,
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
            drawHanoi(state.pegs, state.disks, selected)
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
