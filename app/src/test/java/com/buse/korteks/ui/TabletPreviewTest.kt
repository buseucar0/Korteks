package com.buse.korteks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.ScreenOrientation
import com.buse.korteks.data.MatrixPuzzleLoader
import com.buse.korteks.game.CorsiDifficulty
import com.buse.korteks.game.HanoiDifficulty
import com.buse.korteks.game.MatrixDifficulty
import com.buse.korteks.game.NBackDifficulty
import com.buse.korteks.game.StroopDifficulty
import com.buse.korteks.game.SymbolDigitDifficulty
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test

/**
 * Yatay tablet önizlemeleri. Android 16+ büyük ekranlarda "portrait" kilidini yok saydığı için
 * oyun tablette yatay da açılır; bu önizlemeler o durumda ekranların taşmadığını gösterir.
 *   ./onizle.sh tablet
 */
class TabletPreviewTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_C.copy(orientation = ScreenOrientation.LANDSCAPE, screenWidth = 2560, screenHeight = 1800))

    private fun snap(content: @Composable () -> Unit) = paparazzi.snapshot { AppTheme { content() } }

    @Test
    fun tabletMenu() = snap { HomeScreen(onOpen = {}, streak = 3, longestStreak = 5) }

    @Test
    fun tabletStroop() = snap { StroopScreen(onBack = {}, vm = StroopViewModel(Random(1)).apply { start(StroopDifficulty.ZOR) }) }

    @Test
    fun tabletMatris() = snap {
        val puzzles = MatrixPuzzleLoader.load(LocalContext.current)
        MatrixScreen(onBack = {}, vm = MatrixViewModel(Random(1)).apply { start(puzzles, MatrixDifficulty.ORTA) })
    }

    @Test
    fun tabletBellek() = snap { NBackScreen(onBack = {}, vm = NBackViewModel(Random(1)).apply { start(NBackDifficulty.ORTA) }) }

    @Test
    fun tabletHiz() = snap { SymbolDigitScreen(onBack = {}, vm = SymbolDigitViewModel(Random(1)).apply { start(SymbolDigitDifficulty.ORTA) }) }

    @Test
    fun tabletCorsi() = snap { CorsiScreen(onBack = {}, vm = CorsiViewModel(Random(1)).apply { start(CorsiDifficulty.ORTA) }) }

    @Test
    fun tabletHanoi() = snap { HanoiScreen(onBack = {}, vm = HanoiViewModel().apply { start(HanoiDifficulty.ZOR) }) }
}
