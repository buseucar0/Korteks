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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.game.GameRecord
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.GameType
import com.buse.korteks.ui.AppTheme
import com.buse.korteks.ui.CorsiScreen
import com.buse.korteks.ui.HanoiScreen
import com.buse.korteks.ui.HomeScreen
import com.buse.korteks.ui.MatrixScreen
import com.buse.korteks.ui.NBackScreen
import com.buse.korteks.ui.ProgressViewModel
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

                // Seviye/seri: tüm ekranların paylaştığı tek ViewModel (Activity'ye bağlı).
                // Her oyun bittiği an record bir kez çağrılır, dönen ödül sonuç ekranında gösterilir.
                val progressVm: ProgressViewModel = viewModel()
                val record: (GameRecord) -> GameReward = { progressVm.recordGame(it) }
                fun best(game: GameType): (String) -> Int? = { difficulty -> progressVm.bestScore(game, difficulty) }

                // Geri tuşunu her oyun ekranı kendisi yönetir (oyun → giriş → ana menü).
                // Ana menüdeyken dinleyici yok: sistem uygulamayı kapatır.

                // safeDrawingPadding: içerik durum çubuğunun (saat, pil) altına girmesin
                Box(Modifier.safeDrawingPadding()) {
                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            onOpen = { screen = it },
                            levelInfo = progressVm.levelInfo,
                            streak = progressVm.currentStreak(),
                            longestStreak = progressVm.progress.longestStreak,
                        )
                        Screen.STROOP -> StroopScreen(onBack = goHome, onGameFinished = record, bestScore = best(GameType.STROOP))
                        Screen.MATRIX -> MatrixScreen(onBack = goHome, onGameFinished = record, bestScore = best(GameType.MATRIX))
                        Screen.NBACK -> NBackScreen(onBack = goHome, onGameFinished = record, bestScore = best(GameType.NBACK))
                        Screen.SPEED -> SymbolDigitScreen(onBack = goHome, onGameFinished = record, bestScore = best(GameType.SPEED))
                        Screen.CORSI -> CorsiScreen(onBack = goHome, onGameFinished = record, bestScore = best(GameType.CORSI))
                        Screen.HANOI -> HanoiScreen(onBack = goHome, onGameFinished = record, bestScore = best(GameType.HANOI))
                    }
                }
            }
        }
    }
}
