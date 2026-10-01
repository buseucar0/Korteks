package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.SymbolDigitDifficulty
import com.buse.korteks.game.TaskResult

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun SymbolDigitScreen(
    onBack: () -> Unit,
    onGameFinished: (TaskResult) -> GameReward? = { null },
    vm: SymbolDigitViewModel = viewModel(),
) {
    val state = vm.state

    /** Hamleyi uygula; hamle oyunu bitirdiyse (Playing → Finished) ilerlemeyi bir kez kaydet. */
    fun move(action: () -> Unit) {
        val wasPlaying = vm.state is SpeedUiState.Playing
        action()
        val now = vm.state
        if (wasPlaying && now is SpeedUiState.Finished) vm.attachReward(onGameFinished(now.result))
    }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (state is SpeedUiState.Intro) onBack() else vm.backToIntro() }

    when (state) {
        SpeedUiState.Intro -> TaskIntro(
            emoji = "⚡",
            title = "Hız",
            info = listOf(
                "Sembol-sayı eşleştirme; Wechsler zeka ölçeklerindeki Kodlama (Coding) alt testine ve " +
                    "Sembol Rakam Modaliteleri Testi'ne (SDMT, A. Smith, 1973) dayanır.",
                "Görsel tarama ve işlem hızını (processing speed) ölçmek için kullanılır.",
            ),
            howTo = "Üstteki anahtarda her sembolün bir rakamı var. Ortada çıkan sembolün rakamına olabildiğince " +
                "hızlı bas. Süre bitene kadar devam et. Yanlış cevap puan kaybettirir.",
            choices = SymbolDigitDifficulty.entries.map { d ->
                "${d.title}  ·  ${d.symbolCount} sembol  ·  ${d.durationMs / 1000} sn" to { vm.start(d) }
            },
            onBack = onBack,
        )
        is SpeedUiState.Playing -> SymbolDigitPlaying(
            state,
            initialElapsedMs = vm.sessionElapsedMs,
            onTick = { vm.sessionElapsedMs = it },
            onAnswer = { digit, answerNo -> move { vm.answer(digit, answerNo) } },
            onTimeout = { gameNo -> move { vm.timeout(gameNo) } },
        )
        is SpeedUiState.Finished -> ResultScreen(
            header = "⚡ HIZ · ${state.difficulty.title.uppercase()}",
            score = state.result.score,
            stats = listOf(
                "Doğru" to "${state.result.correct}",
                "Doğruluk" to "%${state.result.accuracyPercent}",
                "Ort. tepki" to reactionText(state.result.averageReactionMs),
            ),
            onReplay = { vm.start(state.difficulty) },
            onMenu = vm::backToIntro,
            reward = state.reward,
        )
    }
}

/** Oyun ekranı: durumsuz. Oturum sayacı kaldığı yerden başlar (initialElapsedMs) ve her karede onTick'e yazar. */
@Composable
internal fun SymbolDigitPlaying(
    state: SpeedUiState.Playing,
    initialElapsedMs: Long,
    onTick: (Long) -> Unit,
    onAnswer: (digit: Int, answerNo: Int) -> Unit,
    onTimeout: (gameNo: Int) -> Unit,
) {
    // Süre tüm oturum için: anahtar oyun numarası, sayaç oyun boyunca bir kez çalışır
    val elapsedMs by rememberTrialClock(state.gameNo, state.durationMs, initialElapsedMs, onTick) { onTimeout(state.gameNo) }
    val remainingSec = (state.durationMs - elapsedMs).coerceAtLeast(0) / 1000

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("Kalan: $remainingSec sn  ·  Doğru: ${state.correct}", 1f - elapsedMs.toFloat() / state.durationMs)
        Spacer(Modifier.height(20.dp))

        // Anahtar (key): üstte sembol, altta rakam
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.key.forEach { (symbol, digit) ->
                Column(
                    Modifier.weight(1f).border(1.dp, Color(0xFF555555), RoundedCornerShape(6.dp)).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(symbol, fontSize = 24.sp)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("$digit", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(state.symbol, fontSize = 120.sp)
        }

        // Tuş takımı (keypad)
        (1..state.key.size).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    Button(
                        onClick = { onAnswer(digit, state.answerNo) },
                        modifier = Modifier.weight(1f).height(68.dp),
                        colors = neutralButtonColors(),
                    ) {
                        Text("$digit", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
