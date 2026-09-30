// Kök build dosyası: eklentilerin (plugin) sürümleri burada tek yerde tanımlı
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    // Paparazzi: ekranları emülatörsüz, JVM üzerinde PNG olarak çizer (sadece test tarafında kullanılır)
    id("app.cash.paparazzi") version "2.0.0-alpha05" apply false
}
