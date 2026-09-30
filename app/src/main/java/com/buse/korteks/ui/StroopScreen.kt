package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.game.InkColor
import com.buse.korteks.game.StroopDifficulty
import com.buse.korteks.game.StroopTask
import com.buse.korteks.game.TaskResult

/** Ekranın durum makinesi (state machine): GİRİŞ → OYUN → SONUÇ */
private sealed interface StroopPhase {
    data object Intro : StroopPhase
    data class Playing(val task: StroopTask) : StroopPhase
    data class Finished(val difficulty: StroopDifficulty, val result: TaskResult) : StroopPhase
}

@Composable
fun StroopScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf<StroopPhase>(StroopPhase.Intro) }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön

    BackHandler { if (phase is StroopPhase.Intro) onBack() else phase = StroopPhase.Intro }

    when (val p = phase) {
        StroopPhase.Intro -> TaskIntro(
            emoji = "🎯",
            title = "Dikkat",
            info = listOf(
                "Stroop görevi (J. R. Stroop, 1935) bilişsel psikolojinin en bilinen deneylerinden biridir. " +
                    "Okumak otomatik bir alışkanlık olduğu için kelimenin anlamı, rengini söylemeyi zorlaştırır.",
                "Araştırmalarda bu otomatik tepkiyi bastırıp dikkati doğru bilgiye yöneltme becerisini " +
                    "(ketleme, inhibition) ölçmek için kullanılır.",
            ),
            howTo = "Kelimeyi okuma, yazının RENGİNİ seç. Örneğin:",
            example = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("MAVİ", color = InkColor.KIRMIZI.toColor(), fontSize = 40.sp, fontWeight = FontWeight.Black)
                    Text("  →  doğru cevap: KIRMIZI", style = MaterialTheme.typography.bodyLarge)
                }
            },
            choices = StroopDifficulty.entries.map { d ->
                "${d.title}  ·  ${d.colorCount} renk  ·  ${d.trialCount} soru  ·  ${formatSeconds(d.timeLimitMs)} sn" to
                    { phase = StroopPhase.Playing(StroopTask(d)) }
            },
            onBack = onBack,
        )
        is StroopPhase.Playing -> key(p.task) {
            StroopPlaying(p.task, onFinished = { result -> phase = StroopPhase.Finished(p.task.difficulty, result) })
        }
        is StroopPhase.Finished -> ResultScreen(
            header = "🎯 STROOP · ${p.difficulty.title.uppercase()}",
            score = p.result.score,
            stats = listOf(
                "Doğruluk" to "%${p.result.accuracyPercent}",
                "Doğru" to "${p.result.correct}/${p.result.total}",
                "Ort. tepki" to reactionText(p.result.averageReactionMs),
            ),
            onReplay = { phase = StroopPhase.Playing(StroopTask(p.difficulty)) },
            onMenu = { phase = StroopPhase.Intro },
        )
    }
}

@Composable
internal fun StroopPlaying(task: StroopTask, onFinished: (TaskResult) -> Unit) {
    // Compose sadece "state" değişince ekranı yeniden çizer. task nesnesi state değil,
    // o yüzden soru değiştiğinde bu sayacı güncelleyerek yeniden çizimi tetikliyoruz.
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
    val trial = task.currentQuestion()

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("${task.progress + 1} / ${task.trials.size}", 1f - elapsedMs.toFloat() / limitMs)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(trial.word.label, color = trial.ink.toColor(), fontSize = 64.sp, fontWeight = FontWeight.Black)
        }

        // Butonlar nötr renkte ve üzerinde renk ADI yazıyor: oyuncu yine okumak zorunda kalıyor
        trial.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { color ->
                    Button(
                        onClick = {
                            // Aynı karede iki kez tıklanırsa ikinciyi yok say
                            if (task.progress == shownTrial) {
                                task.answer(color, elapsedMs)
                                goNext()
                            }
                        },
                        modifier = Modifier.weight(1f).height(72.dp),
                        colors = neutralButtonColors(),
                    ) {
                        Text(color.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
