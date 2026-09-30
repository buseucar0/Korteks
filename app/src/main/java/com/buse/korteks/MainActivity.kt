package com.buse.korteks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.buse.korteks.ui.AppTheme
import com.buse.korteks.ui.CorsiScreen
import com.buse.korteks.ui.HanoiScreen
import com.buse.korteks.ui.HomeScreen
import com.buse.korteks.ui.MatrixScreen
import com.buse.korteks.ui.NBackScreen
import com.buse.korteks.ui.StroopScreen
import com.buse.korteks.ui.SymbolDigitScreen

/** Uygulamadaki ekranlar. Yeni sekme eklenince buraya bir satır eklenir. */
enum class Screen { HOME, STROOP, MATRIX, NBACK, SPEED, CORSI, HANOI }

// Uygulamanın giriş noktası (entry point), gömülüdeki main() gibi düşün.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

                val goHome = { screen = Screen.HOME }

                // Geri tuşunu her oyun ekranı kendisi yönetir (oyun → giriş → ana menü).
                // Ana menüdeyken dinleyici yok: sistem uygulamayı kapatır.

                // safeDrawingPadding: içerik durum çubuğunun (saat, pil) altına girmesin
                Box(Modifier.safeDrawingPadding()) {
                    when (screen) {
                        Screen.HOME -> HomeScreen(onOpen = { screen = it })
                        Screen.STROOP -> StroopScreen(onBack = goHome)
                        Screen.MATRIX -> MatrixScreen(onBack = goHome)
                        Screen.NBACK -> NBackScreen(onBack = goHome)
                        Screen.SPEED -> SymbolDigitScreen(onBack = goHome)
                        Screen.CORSI -> CorsiScreen(onBack = goHome)
                        Screen.HANOI -> HanoiScreen(onBack = goHome)
                    }
                }
            }
        }
    }
}
