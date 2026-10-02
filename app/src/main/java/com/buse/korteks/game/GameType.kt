package com.buse.korteks.game

/** Uygulamadaki görevler. Rekor, günlük antrenman ve profil kartı bu listeyi kullanır. */
enum class GameType(val title: String, val emoji: String) {
    STROOP("Dikkat", "🎯"),
    MATRIX("Mantık", "🔷"),
    NBACK("Bellek", "🧠"),
    SPEED("Hız", "⚡"),
    CORSI("Uzamsal", "🧊"),
    HANOI("Planlama", "🗼"),
}

/** Biten bir oyunun kaydı: hangi görev, hangi zorluk, sonuç. */
data class GameRecord(val game: GameType, val difficulty: String, val result: TaskResult) {
    val bestKey: String get() = bestKeyOf(game, difficulty)

    companion object {
        /** Rekorlar görev + zorluk başına tutulur: ör. "STROOP_ZOR". */
        fun bestKeyOf(game: GameType, difficulty: String) = "${game.name}_$difficulty"
    }
}
