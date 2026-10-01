package com.buse.korteks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.InkColor

// Bütün oyun ekranlarının ortak parçaları.

/** game/ katmanındaki renk → ekrandaki gerçek renk. Koyu arka planda net görünecek tonlar. */
fun InkColor.toColor(): Color = when (this) {
    InkColor.KIRMIZI -> Color(0xFFF44336)
    InkColor.MAVI -> Color(0xFF42A5F5)
    InkColor.YESIL -> Color(0xFF66BB6A)
    InkColor.SARI -> Color(0xFFFFEB3B)
    InkColor.MOR -> Color(0xFFAB47BC)
    InkColor.TURUNCU -> Color(0xFFFF9800)
}

@Composable
fun neutralButtonColors(): ButtonColors =
    ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C), contentColor = Color.White)

fun formatSeconds(ms: Long): String = "%.1f".format(ms / 1000.0)

/**
 * Soru başına süre sayacı (game loop). withFrameMillis her ekran karesinde (~60 Hz, vsync) bir kez döner.
 * Gömülüdeki periyodik timer kesmesi gibi: her tick'te geçen süreyi güncelle, sınır aşılınca onTimeout çağır.
 * trialKey değişince sayaç iptal edilip sıfırdan başlar.
 *
 * DİKKAT: onTimeout içinde "bu hâlâ aynı soru mu?" kontrolü yap. Aynı karede hem tıklama hem timeout
 * gelebilir (yarış durumu, race condition), eski sayaç yeni soruyu yanlışlıkla atlatmasın.
 */
@Composable
fun rememberTrialClock(trialKey: Any, limitMs: Long, onTimeout: () -> Unit): State<Long> {
    val elapsed = remember { mutableLongStateOf(0L) }
    val latestOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(trialKey) {
        elapsed.longValue = 0
        val startMs = withFrameMillis { it }
        while (true) {
            elapsed.longValue = withFrameMillis { it } - startMs
            if (elapsed.longValue >= limitMs) {
                latestOnTimeout()
                break
            }
        }
    }
    return elapsed
}

/** Oyun ekranının üstündeki bilgi yazısı (ör. "5 / 16") ve kalan süre çubuğu (null = çubuk yok). */
@Composable
fun ProgressHeader(label: String, remainingFraction: Float?) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (remainingFraction != null) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { remainingFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
        }
    }
}

/** Her görevin giriş ekranı: "Bu görev nedir?" kartı + nasıl oynanır + zorluk seçimi. */
@Composable
fun TaskIntro(
    emoji: String,
    title: String,
    info: List<String>,
    howTo: String,
    choices: List<Pair<String, () -> Unit>>,
    onBack: () -> Unit,
    example: (@Composable () -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Ana menü") }
        Text("$emoji $title", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bu görev nedir?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                info.forEach { Text(it) }
            }
        }

        Text("Nasıl oynanır?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(howTo)
        example?.invoke()

        Text("Zorluk seç", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        choices.forEach { (label, onClick) ->
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(label, fontSize = 16.sp)
            }
        }
    }
}

/** Sonuç ekranı: ekran kaydında iyi görünsün diye büyük ve kontrastlı. */
@Composable
fun ResultScreen(
    header: String,
    score: Int,
    stats: List<Pair<String, String>>,
    onReplay: () -> Unit,
    onMenu: () -> Unit,
    reward: GameReward? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(header, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("$score", fontSize = 96.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("PUAN", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (reward != null) {
            Spacer(Modifier.height(16.dp))
            Text("+${reward.xpGained} XP", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (reward.leveledUp) {
                Spacer(Modifier.height(8.dp))
                Text("🎉 Seviye ${reward.levelAfter}!", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFEB3B))
            }
        }
        Spacer(Modifier.height(40.dp))

        stats.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) -> StatBox(label, value, Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(28.dp))
        Button(onClick = onReplay, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text("Tekrar oyna", fontSize = 20.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Zorluk seç", fontSize = 18.sp)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Ortalama tepki süresi: hiç ölçüm yoksa "—". */
fun reactionText(ms: Long): String = if (ms <= 0) "—" else "$ms ms"
