package com.buse.korteks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.Screen
import com.buse.korteks.game.LevelInfo

/** target = null ise sekme henüz yapılmadı ("Yakında"). */
data class TabInfo(val emoji: String, val title: String, val taskName: String, val target: Screen?)

val allTabs = listOf(
    TabInfo("🎯", "Dikkat", "Stroop testi", Screen.STROOP),
    TabInfo("🔷", "Mantık", "Matris bulmacaları", Screen.MATRIX),
    TabInfo("🧠", "Bellek", "Dual N-Back", Screen.NBACK),
    TabInfo("⚡", "Hız", "Sembol-sayı eşleştirme", Screen.SPEED),
    TabInfo("🧊", "Uzamsal", "Corsi blokları", Screen.CORSI),
    TabInfo("🗼", "Planlama", "Hanoi Kulesi", Screen.HANOI),
)

/**
 * Ana ekran. Seviye ve seri değerlerini parametre olarak alır (ViewModel'i MainActivity okur):
 * böylece önizleme ve testlerde ViewModel olmadan çizilebilir.
 */
@Composable
fun HomeScreen(
    onOpen: (Screen) -> Unit,
    levelInfo: LevelInfo = LevelInfo(level = 1, xpIntoLevel = 0, xpForNextLevel = 100),
    streak: Int = 0,
    longestStreak: Int = 0,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Korteks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "IQ testlerinde ve bilişsel bilim araştırmalarında kullanılan görevlerle beynini antrenman yap",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        ProgressCard(levelInfo, streak, longestStreak)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(allTabs) { tab -> TabCard(tab, onOpen) }
        }
    }
}

/** Seviye, XP çubuğu ve günlük seri. Seviye/seri motivasyon içindir, bilişsel bir ölçüm değildir. */
@Composable
private fun ProgressCard(levelInfo: LevelInfo, streak: Int, longestStreak: Int) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Seviye ${levelInfo.level}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { levelInfo.fraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${levelInfo.xpIntoLevel} / ${levelInfo.xpForNextLevel} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 $streak", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(
                    if (streak == 0) "Bugün oyna,\nseriyi başlat" else "gün seri",
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
                if (longestStreak > 0) {
                    Text(
                        "En uzun: $longestStreak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCard(tab: TabInfo, onOpen: (Screen) -> Unit) {
    Card(
        onClick = { tab.target?.let(onOpen) },
        enabled = tab.target != null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(tab.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tab.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tab.taskName, style = MaterialTheme.typography.bodyMedium)
            }
            if (tab.target == null) {
                Text("Yakında", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
