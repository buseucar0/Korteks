package com.buse.korteks.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.buse.korteks.R
import org.junit.Rule
import org.junit.Test

/**
 * Uygulama ikonunun önizlemesi: farklı telefon üreticilerinin kestiği şekillerde ve boyutlarda.
 * Adaptive icon'da katman 108 birim, görünen kısım ortadaki 72 birim: o yüzden katman 1.5 kat büyük çizilip kesilir.
 *   ./onizle.sh ikon
 */
class IconPreviewTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Composable
    private fun LauncherIcon(size: Dp, shape: Shape, monochrome: Boolean = false) {
        Box(Modifier.size(size).clip(shape).background(if (monochrome) Color(0xFF303030) else Color.Transparent), contentAlignment = Alignment.Center) {
            if (!monochrome) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(size * 1.5f))
            }
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                null,
                Modifier.size(size * 1.5f),
                colorFilter = if (monochrome) ColorFilter.tint(Color(0xFFD0E4FF)) else null,
            )
        }
    }

    @Test
    fun ikon() = paparazzi.snapshot {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF6B7A8F)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            for ((name, shape) in listOf("Daire" to CircleShape, "Yuvarlak kare" to RoundedCornerShape(28), "Kare" to RectangleShape)) {
                Text(name, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    LauncherIcon(120.dp, shape)
                    LauncherIcon(64.dp, shape)
                    LauncherIcon(40.dp, shape)
                }
            }
            Text("Android 13+ temalı (tek renk)", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LauncherIcon(64.dp, CircleShape, monochrome = true)
            }
        }
    }
}
