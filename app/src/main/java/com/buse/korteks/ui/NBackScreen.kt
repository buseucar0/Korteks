package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.NBackResponse
import com.buse.korteks.game.NBackTask
import com.buse.korteks.game.TaskResult

/** Uyaran her adımın başında bu kadar süre görünür, sonra kaybolur (hafızada tutulmalı). */
private const val STIMULUS_VISIBLE_MS = 1000L

private sealed interface NBackPhase {
    data object Intro : NBackPhase
    data class Playing(val task: NBackTask) : NBackPhase
    data class Finished(val task: NBackTask, val result: TaskResult) : NBackPhase
}

@Composable
fun NBackScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf<NBackPhase>(NBackPhase.Intro) }
    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (phase is NBackPhase.Intro) onBack() else phase = NBackPhase.Intro }

    when (val p = phase) {
        NBackPhase.Intro -> TaskIntro(
            emoji = "🧠",
            title = "Bellek",
            info = listOf(
                "N-Back görevi (W. Kirchner, 1958) ve çift (dual) versiyonu, çalışan bellek (working memory) " +
                    "araştırmalarında en yaygın kullanılan görevlerdendir.",
                "Aynı anda iki bilgi akışını akılda tutup güncelleme becerisini ölçmek için kullanılır. " +
                    "Orijinal çift N-Back'te harfler sesli okunur; bu sürümde ikisi de ekranda gösterilir.",
            ),
            howTo = "Her adımda ızgarada bir kare, içinde bir harfle yanar. Konum N adım öncekiyle aynıysa " +
                "KONUM'a, harf N adım öncekiyle aynıysa HARF'e bas. İkisi de aynıysa ikisine de bas, " +
                "hiçbiri değilse hiçbir şeye basma.",
            choices = NBackDifficulty.entries.map { d ->
                "${d.title}  ·  ${d.n + d.scoredSteps} adım  ·  ${formatSeconds(d.stepMs)} sn/adım" to
                    { phase = NBackPhase.Playing(NBackTask(d)) }
            },
            onBack = onBack,
        )
        is NBackPhase.Playing -> key(p.task) {
            NBackPlaying(p.task, onFinished = { phase = NBackPhase.Finished(p.task, it) })
        }
        is NBackPhase.Finished -> ResultScreen(
            header = "🧠 BELLEK · ${p.task.difficulty.title.uppercase()}",
            score = p.result.score,
            stats = listOf(
                "Doğruluk" to "%${p.result.accuracyPercent}",
                "İsabet" to "${p.task.hits}/${p.task.hits + p.task.misses}",
                "Yanlış alarm" to "${p.task.falseAlarms}",
            ),
            onReplay = { phase = NBackPhase.Playing(NBackTask(p.task.difficulty)) },
            onMenu = { phase = NBackPhase.Intro },
        )
    }
}

@Composable
internal fun NBackPlaying(task: NBackTask, onFinished: (TaskResult) -> Unit) {
    var stepNo by remember { mutableIntStateOf(0) }
    val shownStep = task.progress
    val n = task.difficulty.n
    val stepMs = task.difficulty.stepMs

    // remember(stepNo): adım değişince basış bilgileri kendiliğinden sıfırlanır
    var positionPressed by remember(stepNo) { mutableStateOf(false) }
    var letterPressed by remember(stepNo) { mutableStateOf(false) }
    var firstPressMs by remember(stepNo) { mutableLongStateOf(0L) }

    // Adım süresi dolunca o adımdaki basışlar değerlendirilir, sonraki adıma geçilir
    val elapsedMs by rememberTrialClock(stepNo, stepMs) {
        if (!task.isFinished && task.progress == shownStep) {
            task.answer(NBackResponse(positionPressed, letterPressed), firstPressMs)
            stepNo = task.progress
            if (task.isFinished) onFinished(task.result())
        }
    }

    if (task.isFinished) return
    val stimulus = task.currentQuestion()
    val visible = elapsedMs < STIMULUS_VISIBLE_MS
    val scored = task.progress >= n

    fun press(isPosition: Boolean) {
        if (!scored) return
        if (firstPressMs == 0L) firstPressMs = elapsedMs.coerceAtLeast(1)
        if (isPosition) positionPressed = true else letterPressed = true
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("${task.progress + 1} / ${task.stimuli.size}", 1f - elapsedMs.toFloat() / stepMs)
        Spacer(Modifier.height(12.dp))
        Text(
            if (scored) "$n adım öncekiyle aynı mı?" else "Ezberle… (${n - task.progress} adım sonra başlıyor)",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(0.85f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..2) {
                            val active = visible && stimulus.position == row * 3 + col
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (active) {
                                    Text(
                                        "${stimulus.letter}",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnswerButton("📍 KONUM", pressed = positionPressed, enabled = scored) { press(isPosition = true) }
            AnswerButton("🔤 HARF", pressed = letterPressed, enabled = scored) { press(isPosition = false) }
        }
    }
}

/** Basılınca rengi değişen buton: oyuncu o adımda neye bastığını görsün. */
@Composable
private fun RowScope.AnswerButton(label: String, pressed: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f).height(80.dp),
        colors = if (pressed) ButtonDefaults.buttonColors() else neutralButtonColors(),
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
