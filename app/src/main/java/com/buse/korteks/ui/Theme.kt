package com.buse.korteks.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * İçeriğin en fazla genişliği. Telefonlarda (~360-430 dp) etkisi yok; tablette içerik ortada
 * telefon benzeri bir sütun olarak durur, kartlar ekran boyunca uzamaz.
 */
private val MAX_CONTENT_WIDTH = 600.dp

/** Uygulamanın teması. Hem MainActivity hem önizleme (screenshot) testleri bunu kullanır. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // Koyu tema: renkler daha canlı görünür, sonuç ekranı ekran kaydında kontrastlı olur
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = MAX_CONTENT_WIDTH).fillMaxHeight()) { content() }
            }
        }
    }
}
