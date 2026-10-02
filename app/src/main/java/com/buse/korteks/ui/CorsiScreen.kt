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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buse.korteks.game.CorsiDifficulty
import com.buse.korteks.game.GameReward
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

/** "Doğru/Yanlış" geri bildiriminin ekranda kalma süresi. */
private const val FEEDBACK_MS = 900L

/**
 * onGameFinished: oyun bittiği AN bir kez çağrılır (ilerleme kaydı). Dönen ödül sonuç ekranında gösterilir.
 * Varsayılan { null }: önizleme ve testlerde kayıt yapılmaz.
 */
@Composable
fun CorsiScreen(
    onBack: () -> Unit,
    onGameFinished: (TaskResult) -> GameReward? = { null },
    vm: CorsiViewModel = viewModel(),
) {
    val state = vm.state

    /** Hamleyi uygula; hamle oyunu bitirdiyse (Playing → Finished) ilerlemeyi bir kez kaydet. */
    fun move(action: () -> Unit) {
        val wasPlaying = vm.state is CorsiUiState.Playing
        action()
        val now = vm.state
        if (wasPlaying && now is CorsiUiState.Finished) vm.attachReward(onGameFinished(now.result))
    }

    // Tek geri tuşu dinleyicisi: oyun/sonuç ekranındaysa girişe, girişteyse ana menüye dön
    BackHandler { if (state is CorsiUiState.Intro) onBack() else vm.backToIntro() }

    when (state) {
        CorsiUiState.Intro -> TaskIntro(
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
                "${d.title}  ·  ${d.startLength} blokla başla" + (if (d.backward) "  ·  ters sıra" else "") to { vm.start(d) }
            },
            onBack = onBack,
        )
        is CorsiUiState.Playing -> CorsiPlaying(
            state,
            onAnswer = { taps, reactionMs, trialNo -> vm.answer(taps, reactionMs, trialNo) },
            onNext = { trialNo -> move { vm.next(trialNo) } },
        )
        is CorsiUiState.Finished -> ResultScreen(
            header = "🧊 CORSI · ${state.difficulty.title.uppercase()}",
            score = state.result.score,
            stats = listOf(
                "Aralık (span)" to "${state.span}",
                "Doğru" to "${state.result.correct}/${state.result.total}",
                "Ort. süre" to if (state.result.averageReactionMs > 0) "${formatSeconds(state.result.averageReactionMs)} sn" else "—",
            ),
            onReplay = { vm.start(state.difficulty) },
            onMenu = vm::backToIntro,
            reward = state.reward,
        )
    }
}

/**
 * Oyun ekranı. Gösterim aşaması (hangi blok yanıyor) ve dokunulan bloklar bu ekranın geçici durumu;
 * deneme numarası değişince sıfırlanır. Cevabın sonucu (feedback) ViewModel'de tutulur.
 */
@Composable
internal fun CorsiPlaying(
    state: CorsiUiState.Playing,
    onAnswer: (taps: List<Int>, reactionMs: Long, trialNo: Int) -> Unit,
    onNext: (trialNo: Int) -> Unit,
) {
    var litBlock by remember(state.trialNo) { mutableStateOf<Int?>(null) }
    var showing by remember(state.trialNo) { mutableStateOf(state.feedback == null) }
    var inputStartMs by remember(state.trialNo) { mutableLongStateOf(0L) }
    val taps = remember(state.trialNo) { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()

    // Gösterim aşaması: blokları sırayla yak. delay() burada bir RTOS'taki vTaskDelay gibi:
    // bu korutin (coroutine) bekler ama arayüz donmaz.
    LaunchedEffect(state.trialNo) {
        if (state.feedback != null) return@LaunchedEffect
        delay(800)
        for (block in state.sequence) {
            litBlock = block
            delay(700)
            litBlock = null
            delay(250)
        }
        showing = false
        inputStartMs = System.currentTimeMillis()
    }

    // Cevap verildi: geri bildirimi kısa süre göster, sonra sonraki denemeye geç
    LaunchedEffect(state.trialNo, state.feedback) {
        if (state.feedback == null) return@LaunchedEffect
        delay(FEEDBACK_MS)
        onNext(state.trialNo)
    }

    fun onTap(block: Int) {
        if (showing || state.feedback != null) return
        taps += block
        scope.launch {
            litBlock = block
            delay(200)
            if (litBlock == block) litBlock = null
        }
        if (taps.size == state.length) {
            onAnswer(taps.toList(), System.currentTimeMillis() - inputStartMs, state.trialNo)
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressHeader("Dizi uzunluğu: ${state.length}  ·  En iyi: ${state.span}", null)
        Spacer(Modifier.height(24.dp))
        Text(
            when (state.feedback) {
                true -> "✓ Doğru!"
                false -> "✗ Yanlış"
                null -> if (showing) {
                    "İzle…"
                } else {
                    (if (state.backward) "Tersten dokun" else "Aynı sırayla dokun") + "  (${taps.size}/${state.length})"
                }
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = when (state.feedback) {
                true -> Color(0xFF66BB6A)
                false -> Color(0xFFF44336)
                null -> MaterialTheme.colorScheme.onSurface
            },
        )
        // Blok alanı: kalan alana sığan en büyük kare (yatay tablette ekrandan taşmasın).
        // BoxWithConstraints: alanın gerçek boyutunu (maxWidth) verir, bloklar buna göre konumlanır.
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true)) {
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
        }
    }
}
