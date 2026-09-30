plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
}

android {
    // Paket adı (applicationId): Play Store'da yayınlandıktan sonra DEĞİŞTİRİLEMEZ
    namespace = "com.buse.korteks"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.buse.korteks"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric testleri assets/ (bulmaca JSON'u) ve kaynak dosyalarını görebilsin
        unitTests.isIncludeAndroidResources = true
        // JDK 17+ modül kuralı: Robolectric'in Android 17 taklidi Java'nın iç sınıflarına erişmek zorunda
        unitTests.all {
            it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
            // Her test sınıfı ayrı JVM'de: Paparazzi ve Robolectric aynı JVM'de çakışıyor
            it.forkEvery = 1
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose BOM (Bill of Materials): tüm Compose kütüphanelerinin uyumlu sürümlerini tek yerden sabitler
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Unit test (saf Kotlin oyun mantığı için, JVM üzerinde çalışır)
    testImplementation("junit:junit:4.13.2")

    // Oynanış (UI) testleri: Robolectric uygulamayı emülatörsüz JVM'de çalıştırır,
    // ui-test-junit4 butonlara basıp saati ileri sarmayı sağlar
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
