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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.NBackResponse
import com.buse.korteks.game.TaskResult

/** Uyaran her adımın başında bu kadar süre görünür, sonra kaybolur (hafızada tutulmalı). */
private const val STIMULUS_VISIBLE_MS = 1000L

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun NBackScreen(
    onBack: () -> Unit,
    onGameFinished: (TaskResult) -> GameReward? = { null },
    vm: NBackViewModel = viewModel(),
) {
    val state = vm.state

    /** Hamleyi uygula; hamle oyunu bitirdiyse (Playing → Finished) ilerlemeyi bir kez kaydet. */
    fun move(action: () -> Unit) {
        val wasPlaying = vm.state is NBackUiState.Playing
        action()
        val now = vm.state
        if (wasPlaying && now is NBackUiState.Finished) vm.attachReward(onGameFinished(now.result))
    }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (state is NBackUiState.Intro) onBack() else vm.backToIntro() }

    when (state) {
        NBackUiState.Intro -> TaskIntro(
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
                "${d.title}  ·  ${d.n + d.scoredSteps} adım  ·  ${formatSeconds(d.stepMs)} sn/adım" to { vm.start(d) }
            },
            onBack = onBack,
        )
        is NBackUiState.Playing -> NBackPlaying(
            state,
            onStepEnded = { response, firstPressMs, number -> move { vm.stepEnded(response, firstPressMs, number) } },
        )
        is NBackUiState.Finished -> ResultScreen(
            header = "🧠 BELLEK · ${state.difficulty.title.uppercase()}",
            score = state.result.score,
            stats = listOf(
                "Doğruluk" to "%${state.result.accuracyPercent}",
                "İsabet" to "${state.hits}/${state.hits + state.misses}",
                "Yanlış alarm" to "${state.falseAlarms}",
            ),
            onReplay = { vm.start(state.difficulty) },
            onMenu = vm::backToIntro,
            reward = state.reward,
        )
    }
}

/**
 * Oyun ekranı. Adım içindeki basışlar (hangi butona basıldı) bu ekranın kendi durumu:
 * adım bitince ViewModel'e tek seferde bildirilir.
 */
@Composable
internal fun NBackPlaying(
    state: NBackUiState.Playing,
    onStepEnded: (response: NBackResponse, firstPressMs: Long, stepNumber: Int) -> Unit,
) {
    // remember(state.number): adım değişince basış bilgileri kendiliğinden sıfırlanır
    var positionPressed by remember(state.number) { mutableStateOf(false) }
    var letterPressed by remember(state.number) { mutableStateOf(false) }
    var firstPressMs by remember(state.number) { mutableLongStateOf(0L) }

    // Adım süresi dolunca o adımdaki basışlar değerlendirilir, sonraki adıma geçilir
    val elapsedMs by rememberTrialClock(state.number, state.stepMs) {
        onStepEnded(NBackResponse(positionPressed, letterPressed), firstPressMs, state.number)
    }

    val visible = elapsedMs < STIMULUS_VISIBLE_MS

    fun press(isPosition: Boolean) {
        if (!state.scored) return
        if (firstPressMs == 0L) firstPressMs = elapsedMs.coerceAtLeast(1)
        if (isPosition) positionPressed = true else letterPressed = true
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("${state.number} / ${state.total}", 1f - elapsedMs.toFloat() / state.stepMs)
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.scored) "${state.n} adım öncekiyle aynı mı?" else "Ezberle… (${state.n - state.number + 1} adım sonra başlıyor)",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        // Izgara: kalan alana sığan en büyük kare (yatay tablette butonların üstüne taşmasın)
        Box(Modifier.weight(1f).fillMaxWidth(0.85f).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0..2) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0..2) {
                            val active = visible && state.stimulus.position == row * 3 + col
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (active) {
                                    Text(
                                        "${state.stimulus.letter}",
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
            AnswerButton("📍 KONUM", pressed = positionPressed, enabled = state.scored) { press(isPosition = true) }
            AnswerButton("🔤 HARF", pressed = letterPressed, enabled = state.scored) { press(isPosition = false) }
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
