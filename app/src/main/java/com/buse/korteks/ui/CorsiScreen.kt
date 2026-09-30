package com.buse.korteks.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buse.korteks.game.CorsiDifficulty
import com.buse.korteks.game.CorsiTask
import com.buse.korteks.game.TaskResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 9 bloğun alandaki yerleri (0..1 oranı, sol üst köşe). Klasik Corsi gibi düzensiz dağılım. */
private val BLOCK_POSITIONS = listOf(
    Offset(0.10f, 0.08f), Offset(0.55f, 0.03f), Offset(0.78f, 0.25f),
    Offset(0.32f, 0.26f), Offset(0.05f, 0.50f), Offset(0.52f, 0.50f),
    Offset(0.80f, 0.62f), Offset(0.28f, 0.76f), Offset(0.60f, 0.80f),
)
private const val BLOCK_SIZE = 0.17f
private val BLOCK_COLOR = Color(0xFF3949AB)
private val BLOCK_LIT = Color(0xFFFFEB3B)

private sealed interface CorsiPhase {
    data object Intro : CorsiPhase
    data class Playing(val task: CorsiTask) : CorsiPhase
    data class Finished(val task: CorsiTask, val result: TaskResult) : CorsiPhase
}

@Composable
fun CorsiScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf<CorsiPhase>(CorsiPhase.Intro) }
    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (phase is CorsiPhase.Intro) onBack() else phase = CorsiPhase.Intro }

    when (val p = phase) {
        CorsiPhase.Intro -> TaskIntro(
            emoji = "🧊",
            title = "Uzamsal",
            info = listOf(
                "Corsi blok testi (P. Corsi, 1972), görsel-uzamsal kısa süreli belleği ölçmek için " +
                    "nöropsikolojide yaygın kullanılan bir görevdir.",
                "Doğru hatırlanan en uzun dizi \"Corsi aralığı\" (Corsi span) olarak adlandırılır. " +
                    "Tersten versiyonu ayrıca bilgiyi zihinde işleme becerisini de içerir.",
            ),
            howTo = "Bloklar sırayla yanacak. Bitince aynı sırayla bloklara dokun (\"Tersten\" seviyesinde ters " +
                "sırayla). Her doğru cevapta dizi bir uzar. Aynı uzunlukta iki kez yanılırsan oyun biter.",
            choices = CorsiDifficulty.entries.map { d ->
                "${d.title}  ·  ${d.startLength} blokla başla" + (if (d.backward) "  ·  ters sıra" else "") to
                    { phase = CorsiPhase.Playing(CorsiTask(d)) }
            },
            onBack = onBack,
        )
        is CorsiPhase.Playing -> key(p.task) {
            CorsiPlaying(p.task, onFinished = { phase = CorsiPhase.Finished(p.task, it) })
        }
        is CorsiPhase.Finished -> ResultScreen(
            header = "🧊 CORSI · ${p.task.difficulty.title.uppercase()}",
            score = p.result.score,
            stats = listOf(
                "Aralık (span)" to "${p.task.span}",
                "Doğru" to "${p.result.correct}/${p.result.total}",
                "Ort. süre" to if (p.result.averageReactionMs > 0) "${formatSeconds(p.result.averageReactionMs)} sn" else "—",
            ),
            onReplay = { phase = CorsiPhase.Playing(CorsiTask(p.task.difficulty)) },
            onMenu = { phase = CorsiPhase.Intro },
        )
    }
}

@Composable
internal fun CorsiPlaying(task: CorsiTask, onFinished: (TaskResult) -> Unit) {
    var trialNo by remember { mutableIntStateOf(0) }
    var litBlock by remember { mutableStateOf<Int?>(null) }
    var showing by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<Boolean?>(null) } // null = yok, true = doğru, false = yanlış
    var shownLength by remember { mutableIntStateOf(task.currentLength) }
    var inputStartMs by remember { mutableLongStateOf(0L) }
    val taps = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()

    // Gösterim aşaması: blokları sırayla yak. delay() burada bir RTOS'taki vTaskDelay gibi:
    // bu korutin (coroutine) bekler ama arayüz donmaz.
    LaunchedEffect(trialNo) {
        if (task.isFinished) return@LaunchedEffect
        taps.clear()
        feedback = null
        showing = true
        shownLength = task.currentLength
        delay(800)
        for (block in task.currentQuestion()) {
            litBlock = block
            delay(700)
            litBlock = null
            delay(250)
        }
        showing = false
        inputStartMs = System.currentTimeMillis()
    }

    fun onTap(block: Int) {
        if (showing || feedback != null || task.isFinished) return
        taps += block
        scope.launch {
            litBlock = block
            delay(200)
            if (litBlock == block) litBlock = null
        }
        if (taps.size == shownLength) {
            feedback = task.answer(taps.toList(), System.currentTimeMillis() - inputStartMs)
            scope.launch {
                delay(900)
                if (task.isFinished) onFinished(task.result()) else trialNo++
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("Dizi uzunluğu: $shownLength  ·  En iyi: ${task.span}", null)
        Spacer(Modifier.height(24.dp))
        Text(
            when (feedback) {
                true -> "✓ Doğru!"
                false -> "✗ Yanlış"
                null -> if (showing) {
                    "İzle…"
                } else {
                    (if (task.difficulty.backward) "Tersten dokun" else "Aynı sırayla dokun") + "  (${taps.size}/$shownLength)"
                }
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = when (feedback) {
                true -> Color(0xFF66BB6A)
                false -> Color(0xFFF44336)
                null -> MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(Modifier.weight(1f))

        // BoxWithConstraints: alanın gerçek boyutunu (maxWidth) verir, bloklar buna göre konumlanır
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val side = maxWidth * BLOCK_SIZE
            BLOCK_POSITIONS.forEachIndexed { index, pos ->
                Box(
                    Modifier
                        .offset(x = maxWidth * pos.x, y = maxHeight * pos.y)
                        .size(side)
                        .background(if (litBlock == index) BLOCK_LIT else BLOCK_COLOR, RoundedCornerShape(10.dp))
                        .clickable { onTap(index) }
                        .testTag("blok_$index"),
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
