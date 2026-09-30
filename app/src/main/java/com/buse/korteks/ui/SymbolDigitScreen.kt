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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.game.SymbolDigitDifficulty
import com.buse.korteks.game.SymbolDigitTask
import com.buse.korteks.game.TaskResult

private sealed interface SpeedPhase {
    data object Intro : SpeedPhase
    data class Playing(val task: SymbolDigitTask) : SpeedPhase
    data class Finished(val difficulty: SymbolDigitDifficulty, val result: TaskResult) : SpeedPhase
}

@Composable
fun SymbolDigitScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf<SpeedPhase>(SpeedPhase.Intro) }
    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (phase is SpeedPhase.Intro) onBack() else phase = SpeedPhase.Intro }

    when (val p = phase) {
        SpeedPhase.Intro -> TaskIntro(
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
                "${d.title}  ·  ${d.symbolCount} sembol  ·  ${d.durationMs / 1000} sn" to
                    { phase = SpeedPhase.Playing(SymbolDigitTask(d)) }
            },
            onBack = onBack,
        )
        is SpeedPhase.Playing -> key(p.task) {
            SymbolDigitPlaying(p.task, onFinished = { phase = SpeedPhase.Finished(p.task.difficulty, it) })
        }
        is SpeedPhase.Finished -> ResultScreen(
            header = "⚡ HIZ · ${p.difficulty.title.uppercase()}",
            score = p.result.score,
            stats = listOf(
                "Doğru" to "${p.result.correct}",
                "Doğruluk" to "%${p.result.accuracyPercent}",
                "Ort. tepki" to reactionText(p.result.averageReactionMs),
            ),
            onReplay = { phase = SpeedPhase.Playing(SymbolDigitTask(p.difficulty)) },
            onMenu = { phase = SpeedPhase.Intro },
        )
    }
}

@Composable
internal fun SymbolDigitPlaying(task: SymbolDigitTask, onFinished: (TaskResult) -> Unit) {
    var answerNo by remember { mutableIntStateOf(0) }
    var lastAnswerAtMs by remember { mutableLongStateOf(0L) }
    val shownAnswer = answerNo
    val durationMs = task.difficulty.durationMs

    // Süre tüm oturum için: trialKey sabit (Unit), sayaç bir kez başlar
    val elapsedMs by rememberTrialClock(Unit, durationMs) {
        if (!task.isFinished) {
            task.timeout()
            onFinished(task.result())
        }
    }

    if (task.isFinished) return
    val remainingSec = (durationMs - elapsedMs).coerceAtLeast(0) / 1000

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("Kalan: $remainingSec sn  ·  Doğru: ${task.correct}", 1f - elapsedMs.toFloat() / durationMs)
        Spacer(Modifier.height(20.dp))

        // Anahtar (key): üstte sembol, altta rakam
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            task.key.entries.sortedBy { it.value }.forEach { (symbol, digit) ->
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
            Text(task.currentQuestion(), fontSize = 120.sp)
        }

        // Tuş takımı (keypad)
        (1..task.key.size).chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    Button(
                        onClick = {
                            if (answerNo == shownAnswer && !task.isFinished) {
                                task.answer(digit, elapsedMs - lastAnswerAtMs)
                                lastAnswerAtMs = elapsedMs
                                answerNo++
                            }
                        },
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
