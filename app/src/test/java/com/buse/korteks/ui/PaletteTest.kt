package com.buse.korteks.ui

import androidx.compose.ui.graphics.Color
import com.buse.korteks.game.COLOR_BLIND_SAFE_COLORS
import com.buse.korteks.game.InkColor
import kotlin.math.pow
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Renk paletinin bekçisi: renkler değiştirilirse ve renk körleri için birbirine fazla yaklaşırsa bu test kırılır.
 *
 * Yöntem: renk körlüğü simülasyonu (Machado, Oliveira & Fernandes 2009, şiddet 1.0, doğrusal RGB'de 3x3 matris)
 * + iki renk arasındaki algısal fark ΔE (CIELAB, CIE76). Kabaca: ΔE < 10 neredeyse aynı, ΔE > 20 rahat ayrılır.
 * Eşikler, palet seçilirken ölçülen değerlerin biraz altında tutuldu (eski palette en kötü çift ΔE ≈ 16 idi).
 */
class PaletteTest {

    private val simulations = mapOf(
        "kırmızı körlüğü" to arrayOf(
            doubleArrayOf(0.152286, 1.052583, -0.204868),
            doubleArrayOf(0.114503, 0.786281, 0.099216),
            doubleArrayOf(-0.003882, -0.048116, 1.051998),
        ),
        "yeşil körlüğü" to arrayOf(
            doubleArrayOf(0.367322, 0.860646, -0.227968),
            doubleArrayOf(0.280085, 0.672501, 0.047413),
            doubleArrayOf(-0.011820, 0.042940, 0.968881),
        ),
        "mavi-sarı körlüğü" to arrayOf(
            doubleArrayOf(1.255528, -0.076749, -0.178779),
            doubleArrayOf(-0.078411, 0.930809, 0.147602),
            doubleArrayOf(0.004733, 0.691367, 0.303900),
        ),
    )

    @Test
    fun `tum renkler normal gorusde rahat ayrilir`() {
        assertMinDistance(InkColor.entries, simulation = null, atLeast = 40.0)
    }

    @Test
    fun `alti renk renk korlugunde eski paletten belirgin iyi ayrilir`() {
        simulations.forEach { (name, m) -> assertMinDistance(InkColor.entries, m, atLeast = 25.0, label = name) }
    }

    @Test
    fun `renk koru modundaki 3 ve 4 renk her renk korlugu turunde ayrilir`() {
        simulations.forEach { (name, m) ->
            assertMinDistance(COLOR_BLIND_SAFE_COLORS.take(3), m, atLeast = 45.0, label = "$name, 3 renk")
            assertMinDistance(COLOR_BLIND_SAFE_COLORS, m, atLeast = 30.0, label = "$name, 4 renk")
        }
    }

    @Test
    fun `her renk koyu arka planda okunur (WCAG kontrast en az 4,5)`() {
        val background = Color(0xFF141218)
        InkColor.entries.forEach { ink ->
            val c = contrast(ink.toColor(), background)
            assertTrue("${ink.label} kontrastı $c", c >= 4.5)
        }
    }

    // ---------------------------------------------------------------- renk matematiği

    private fun assertMinDistance(colors: List<InkColor>, simulation: Array<DoubleArray>?, atLeast: Double, label: String = "normal") {
        for (i in colors.indices) for (j in i + 1 until colors.size) {
            val a = lab(simulate(linear(colors[i].toColor()), simulation))
            val b = lab(simulate(linear(colors[j].toColor()), simulation))
            val d = sqrt((0..2).sumOf { (a[it] - b[it]).pow(2) })
            assertTrue("$label: ${colors[i].label}–${colors[j].label} ΔE=${"%.1f".format(d)} < $atLeast", d >= atLeast)
        }
    }

    /** sRGB (0..1) → doğrusal RGB: ekran gamasını geri al. */
    private fun linear(c: Color) = doubleArrayOf(c.red.toDouble(), c.green.toDouble(), c.blue.toDouble())
        .map { if (it <= 0.04045) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }.toDoubleArray()

    private fun simulate(rgb: DoubleArray, m: Array<DoubleArray>?): DoubleArray =
        m?.let { DoubleArray(3) { i -> (0..2).sumOf { j -> m[i][j] * rgb[j] }.coerceIn(0.0, 1.0) } } ?: rgb

    /** Doğrusal RGB → CIELAB (D65). */
    private fun lab(rgb: DoubleArray): DoubleArray {
        val x = 0.4124 * rgb[0] + 0.3576 * rgb[1] + 0.1805 * rgb[2]
        val y = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]
        val z = 0.0193 * rgb[0] + 0.1192 * rgb[1] + 0.9505 * rgb[2]
        fun f(t: Double) = if (t > 0.008856) Math.cbrt(t) else 7.787 * t + 16.0 / 116
        val fx = f(x / 0.95047)
        val fy = f(y)
        val fz = f(z / 1.08883)
        return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    private fun contrast(a: Color, b: Color): Double {
        fun lum(c: Color) = linear(c).let { 0.2126 * it[0] + 0.7152 * it[1] + 0.0722 * it[2] }
        val (hi, lo) = listOf(lum(a), lum(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
}
