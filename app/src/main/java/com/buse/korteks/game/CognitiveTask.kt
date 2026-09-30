package com.buse.korteks.game

/**
 * Tüm sekmelerin (görevlerin) uyduğu ortak arayüz (interface).
 * Gömülüdeki bir sürücü arayüzü gibi düşün: her görev kendi içini farklı yazar,
 * ama ekran katmanı hepsiyle aynı fonksiyonlar üzerinden konuşur.
 *
 * Q = soru tipi (question), A = cevap tipi (answer).
 * "Başlatma" işi constructor'da yapılır: nesne oluştuğunda sorular hazırdır.
 *
 * Bu paket Android'e hiç bağımlı değil, zamanı da kendisi ölçmez
 * (tepki süresini dışarıdan alır). Böylece JVM'de unit test edilebilir.
 */
interface CognitiveTask<Q, A> {
    val isFinished: Boolean

    /** Şu an cevaplanması gereken soru. Görev bittiyse çağırma. */
    fun currentQuestion(): Q

    /** Cevabı işler, bir sonraki soruya geçer. Doğruysa true döner. */
    fun answer(answer: A, reactionMs: Long): Boolean

    /** Süre doldu: soru yanlış sayılır, bir sonrakine geçilir. */
    fun timeout()

    fun result(): TaskResult
}

data class TaskResult(
    val correct: Int,
    val total: Int,
    /** Sadece doğru cevapların ortalama tepki süresi. Hiç doğru yoksa 0. */
    val averageReactionMs: Long,
    val score: Int,
) {
    val accuracyPercent: Int get() = if (total == 0) 0 else correct * 100 / total
}
