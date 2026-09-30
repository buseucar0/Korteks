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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.Screen

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

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Korteks", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "IQ testlerinde ve bilişsel bilim araştırmalarında kullanılan görevlerle beynini antrenman yap",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(allTabs) { tab -> TabCard(tab, onOpen) }
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
