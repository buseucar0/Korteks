package com.buse.korteks.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Uygulamanın teması. Hem MainActivity hem önizleme (screenshot) testleri bunu kullanır. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // Koyu tema: renkler daha canlı görünür, sonuç ekranı ekran kaydında kontrastlı olur
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) { content() }
    }
}
