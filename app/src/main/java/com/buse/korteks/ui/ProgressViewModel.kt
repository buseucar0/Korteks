package com.buse.korteks.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.buse.korteks.data.ProgressStore
import com.buse.korteks.game.GameRecord
import com.buse.korteks.game.GameReward
import com.buse.korteks.game.GameType
import com.buse.korteks.game.LevelInfo
import com.buse.korteks.game.PlayerProgress
import com.buse.korteks.game.Progression
import java.time.LocalDate

/**
 * Seviye ve seri için ekranların tek giriş noktası. Activity'ye bağlıdır (tüm ekranlar aynı örneği görür).
 * AndroidViewModel: Application nesnesine erişir (SharedPreferences için Context lazım).
 *
 * Kullanım: bir oyun bittiğinde, bittiği AN bir kez recordGame(kayıt) çağrılır
 * (composable gövdesinde değil! recomposition ile iki kez kaydedilir).
 */
class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ProgressStore(application)

    var progress: PlayerProgress by mutableStateOf(store.load())
        private set

    /** Son oyunun ödülü: sonuç ekranında "+X XP" / "Seviye N!" göstermek için. Yeni oyun başlayınca eskir. */
    var lastReward: GameReward? by mutableStateOf(null)
        private set

    val levelInfo: LevelInfo get() = Progression.levelInfo(progress.totalXp)

    fun currentStreak(today: LocalDate = LocalDate.now()): Int = Progression.currentStreak(progress, today)

    /** Görev + zorluktaki rekor (hiç oynanmadıysa null). Giriş ekranındaki zorluk butonlarında gösterilir. */
    fun bestScore(game: GameType, difficulty: String): Int? = progress.bestScores[GameRecord.bestKeyOf(game, difficulty)]

    fun recordGame(record: GameRecord, today: LocalDate = LocalDate.now()): GameReward {
        val reward = Progression.recordGame(progress, record, today)
        progress = reward.after
        store.save(progress)
        lastReward = reward
        return reward
    }
}
