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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.game.InkColor
import com.buse.korteks.game.StroopDifficulty

/**
 * Stroop ekranı. Durumu StroopViewModel tutar; bu fonksiyon sadece durumu çizer ve
 * kullanıcı olaylarını (tıklama, süre dolması) ViewModel'e iletir.
 * viewModel(): Activity'ye bağlı ViewModel'i getirir, yoksa oluşturur. Testte dışarıdan verilebilir.
 */
@Composable
fun StroopScreen(onBack: () -> Unit, vm: StroopViewModel = viewModel()) {
    val state = vm.state

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (state is StroopUiState.Intro) onBack() else vm.backToIntro() }

    when (state) {
        StroopUiState.Intro -> TaskIntro(
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
                    { vm.start(d) }
            },
            onBack = onBack,
        )
        is StroopUiState.Playing -> StroopPlaying(state, onAnswer = vm::answer, onTimeout = vm::timeout)
        is StroopUiState.Finished -> ResultScreen(
            header = "🎯 STROOP · ${state.difficulty.title.uppercase()}",
            score = state.result.score,
            stats = listOf(
                "Doğruluk" to "%${state.result.accuracyPercent}",
                "Doğru" to "${state.result.correct}/${state.result.total}",
                "Ort. tepki" to reactionText(state.result.averageReactionMs),
            ),
            onReplay = { vm.start(state.difficulty) },
            onMenu = vm::backToIntro,
        )
    }
}

/**
 * Oyun ekranı: tamamen durumsuz (stateless). Ne göstereceğini [state]'ten alır,
 * olayları yukarı bildirir. Tek kendi durumu, ekran karesine bağlı süre sayacı.
 */
@Composable
internal fun StroopPlaying(
    state: StroopUiState.Playing,
    onAnswer: (color: InkColor, reactionMs: Long, trialNumber: Int) -> Unit,
    onTimeout: (trialNumber: Int) -> Unit,
) {
    // Anahtar soru numarası: yeni soru gelince sayaç sıfırdan başlar
    val elapsedMs by rememberTrialClock(state.number, state.timeLimitMs) { onTimeout(state.number) }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("${state.number} / ${state.total}", 1f - elapsedMs.toFloat() / state.timeLimitMs)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(state.trial.word.label, color = state.trial.ink.toColor(), fontSize = 64.sp, fontWeight = FontWeight.Black)
        }

        // Butonlar nötr renkte ve üzerinde renk ADI yazıyor: oyuncu yine okumak zorunda kalıyor
        state.trial.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { color ->
                    Button(
                        onClick = { onAnswer(color, elapsedMs, state.number) },
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
