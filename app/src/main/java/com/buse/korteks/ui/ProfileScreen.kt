package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.game.GameType
import com.buse.korteks.game.LevelInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val CARD_BACKGROUND = Color(0xFF141218)

/**
 * Bilişsel profil ekranı: radar grafikli kart + "Paylaş" butonu.
 * Kart, ekrana çizilirken aynı anda bir GraphicsLayer'a da kaydedilir; paylaşırken o katman
 * bitmap'e çevrilir (ekran görüntüsü almak gibi, ama sadece kartın kendisi).
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    profile: Map<GameType, Int?>,
    levelInfo: LevelInfo,
    streak: Int,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cardLayer = rememberGraphicsLayer()

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawWithContent {
                    cardLayer.record { this@drawWithContent.drawContent() } // katmana kaydet
                    drawLayer(cardLayer) // ve ekrana çiz
                },
        ) {
            ProfileCard(profile, levelInfo, streak)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("← Ana menü")
            }
            Button(
                onClick = { scope.launch { shareImage(context, cardLayer.toImageBitmap().asAndroidBitmap()) } },
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text("📤 Paylaş", fontSize = 18.sp)
            }
        }
    }
}

/** Paylaşılan görselin kendisi. Kendi arka planını çizer (PNG'de saydam kalmasın). */
@Composable
internal fun ProfileCard(profile: Map<GameType, Int?>, levelInfo: LevelInfo, streak: Int) {
    Column(
        Modifier.fillMaxSize().background(CARD_BACKGROUND).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("KORTEKS", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.primary)
        Text("Bilişsel profilim", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(
            "Seviye ${levelInfo.level}  ·  🔥 $streak gün seri",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RadarChart(
            values = GameType.entries.map { it to profile[it] },
            modifier = Modifier.weight(1f).fillMaxWidth().aspectRatio(1f, matchHeightConstraintsFirst = true),
        )

        // Görev başına değerler, iki sütun
        GameType.entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                pair.forEach { game ->
                    Text(
                        "${game.emoji} ${game.title}: ${profile[game]?.toString() ?: "—"}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Görevlerdeki en iyi sonuçlarına göre 0–100. Kişisel takip içindir, bilimsel bir ölçüm değildir.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Radar (örümcek ağı) grafik: her görev bir eksen, merkez 0, dış halka 100.
 * Açı = eksen sırası × (360° / eksen sayısı), en üstten (-90°) başlayarak saat yönünde.
 */
@Composable
private fun RadarChart(values: List<Pair<GameType, Int?>>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)

    Canvas(modifier.padding(8.dp)) {
        // Etiketlere yer kalsın diye grafik alanın yarısından biraz küçük
        val radius = size.minDimension / 2 * 0.58f
        val n = values.size

        /** i. eksen üzerinde, merkezden [fraction] oranında uzaklıktaki nokta. */
        fun point(i: Int, fraction: Float): Offset {
            val angle = -PI / 2 + 2 * PI * i / n
            return Offset(
                center.x + (cos(angle) * radius * fraction).toFloat(),
                center.y + (sin(angle) * radius * fraction).toFloat(),
            )
        }

        fun polygon(fractions: List<Float>) = Path().apply {
            fractions.forEachIndexed { i, f -> if (i == 0) moveTo(point(i, f).x, point(i, f).y) else lineTo(point(i, f).x, point(i, f).y) }
            close()
        }

        // Halkalar (25, 50, 75, 100) ve eksenler
        for (ring in 1..4) drawPath(polygon(List(n) { ring / 4f }), Color(0xFF4A4458), style = Stroke(1.dp.toPx()))
        for (i in 0 until n) drawLine(Color(0xFF4A4458), center, point(i, 1f), strokeWidth = 1.dp.toPx())

        // Veri alanı
        val data = polygon(values.map { (it.second ?: 0) / 100f })
        drawPath(data, accent.copy(alpha = 0.35f))
        drawPath(data, accent, style = Stroke(3.dp.toPx()))
        values.forEachIndexed { i, (_, v) -> drawCircle(accent, 4.dp.toPx(), point(i, (v ?: 0) / 100f)) }

        // Eksen etiketleri: halkanın biraz dışında, ortalanmış
        values.forEachIndexed { i, (game, _) ->
            val layout = textMeasurer.measure("${game.emoji}\n${game.title}", labelStyle)
            val p = point(i, 1.3f)
            // Etiket hiçbir zaman çizim alanının dışına taşmasın (dar ekranlarda yandaki etiketler)
            val x = (p.x - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
            val y = (p.y - layout.size.height / 2f).coerceIn(0f, size.height - layout.size.height)
            drawText(layout, topLeft = Offset(x, y))
        }
    }
}
