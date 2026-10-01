package com.buse.korteks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.buse.korteks.data.MatrixPuzzleLoader
import com.buse.korteks.game.CorsiDifficulty
import com.buse.korteks.game.CorsiTask
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.HanoiMove
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.HanoiTask
import com.buse.korteks.game.LevelInfo
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.MatrixTask
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.NBackTask
import com.buse.korteks.game.PlayerProgress
import com.buse.korteks.game.StroopDifficulty
import com.buse.korteks.game.SymbolDigitDifficulty
import com.buse.korteks.game.SymbolDigitTask
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test

/**
 * Ekran önizlemeleri: emülatör olmadan, JVM üzerinde PNG olarak çizilir.
 *   ./onizle.sh  →  app/src/test/snapshots/images/ altına resimler
 * Yeni bir ekran ya da durum görmek istersen buraya bir @Test eklemen yeterli.
 * Random(1): sabit tohum, her seferinde aynı sorular → resimler karşılaştırılabilir.
 */
class ScreenPreviewTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    private fun snap(content: @Composable () -> Unit) = paparazzi.snapshot { AppTheme { content() } }

    @Test
    fun anaMenu() = snap { HomeScreen(onOpen = {}) }

    @Test
    fun anaMenuIlerleme() = snap {
        HomeScreen(onOpen = {}, levelInfo = LevelInfo(level = 3, xpIntoLevel = 120, xpForNextLevel = 200), streak = 4, longestStreak = 9)
    }

    @Test
    fun sonucSeviyeAtlama() = snap {
        ResultScreen(
            header = "🧠 BELLEK · 2-BACK",
            score = 1640,
            stats = listOf("Doğruluk" to "%88", "İsabet" to "11/12", "Yanlış alarm" to "1"),
            onReplay = {},
            onMenu = {},
            reward = GameReward(xpGained = 110, before = PlayerProgress(totalXp = 200), after = PlayerProgress(totalXp = 310)),
        )
    }

    @Test
    fun stroopGiris() = snap { StroopScreen(onBack = {}, vm = StroopViewModel()) }

    @Test
    fun stroopOyun() = snap { StroopScreen(onBack = {}, vm = StroopViewModel(Random(1)).apply { start(StroopDifficulty.ORTA) }) }

    @Test
    fun stroopOyunZor() = snap { StroopScreen(onBack = {}, vm = StroopViewModel(Random(1)).apply { start(StroopDifficulty.ZOR) }) }

    @Test
    fun matrisGiris() = snap { MatrixScreen(onBack = {}) }

    @Test
    fun matrisOyunKolay() = snap {
        val puzzles = MatrixPuzzleLoader.load(LocalContext.current)
        MatrixPlaying(MatrixTask(puzzles, MatrixDifficulty.KOLAY, Random(1)), onFinished = {})
    }

    @Test
    fun matrisOyunZor() = snap {
        val puzzles = MatrixPuzzleLoader.load(LocalContext.current)
        MatrixPlaying(MatrixTask(puzzles, MatrixDifficulty.ZOR, Random(1)), onFinished = {})
    }

    @Test
    fun sonucEkrani() = snap {
        ResultScreen(
            header = "🎯 STROOP · ORTA",
            score = 2140,
            stats = listOf("Doğruluk" to "%94", "Doğru" to "15/16", "Ort. tepki" to "812 ms"),
            onReplay = {},
            onMenu = {},
        )
    }

    @Test
    fun hizGiris() = snap { SymbolDigitScreen(onBack = {}) }

    @Test
    fun hizOyun() = snap { SymbolDigitPlaying(SymbolDigitTask(SymbolDigitDifficulty.ORTA, Random(1)), onFinished = {}) }

    @Test
    fun bellekOyun() = snap { NBackPlaying(NBackTask(NBackDifficulty.ORTA, Random(1)), onFinished = {}) }

    @Test
    fun corsiOyun() = snap { CorsiPlaying(CorsiTask(CorsiDifficulty.ORTA, Random(1)), onFinished = {}) }

    @Test
    fun hanoiOyun() = snap {
        // Birkaç hamle yapılmış hâli göster
        val task = HanoiTask(HanoiDifficulty.ORTA).apply {
            answer(HanoiMove(0, 1), 0)
            answer(HanoiMove(0, 2), 0)
            answer(HanoiMove(1, 2), 0)
        }
        HanoiPlaying(task, onFinished = {})
    }
}
