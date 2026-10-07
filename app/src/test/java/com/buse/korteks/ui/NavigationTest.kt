package com.buse.korteks.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.buse.korteks.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Uygulamanın tamamı (MainActivity) açılır: menü → sekme → oyun → geri tuşu akışı. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val tabs = listOf("Dikkat", "Mantık", "Bellek", "Hız", "Uzamsal", "Planlama")

    /**
     * Geri tuşuna bas, sonra saati otomatiğe al. Oyun ekranından çıkınca sürekli çalışan sayaç da
     * durduğu için otomatik saat güvenli. (Saat elle yönetilirken bazı ekran geçişleri tamamlanmıyor;
     * bu test ortamının bir özelliği, uygulamanın hatası değil.)
     */
    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    /** Sekme listesi kaydırılabilir: alttaki sekmeler ekranda görünmüyorsa önce oraya kaydır. */
    private fun openTab(tab: String) {
        // Sekme adı günlük antrenman kartında da geçebilir: sekme kartını etiketinden bul
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("sekme_$tab"))
        rule.onNode(hasTestTag("sekme_$tab")).performClick()
    }

    @Test
    fun `her sekme acilir ve ana menu butonuyla donulur`() {
        for (tab in tabs) {
            openTab(tab)
            rule.onNodeWithText("Bu görev nedir?").assertExists()
            rule.onNodeWithText("← Ana menü").performClick()
            rule.onNodeWithText("Korteks").assertExists()
        }
    }

    @Test
    fun `geri tusu once oyundan girise, sonra ana menuye doner`() {
        for (tab in tabs) {
            openTab(tab)
            rule.onNodeWithText("Bu görev nedir?").assertExists("sekme=$tab")

            // İlk zorluk butonunu görünür yap (kaydırma bir animasyon: saat durmadan önce yapılmalı)
            val firstDifficulty = rule.onAllNodes(hasText("·", substring = true) and hasClickAction()).onFirst()
            firstDifficulty.performScrollTo()
            // Oyun ekranlarında sürekli çalışan sayaç var: oyun sırasında saati elle yönet
            rule.mainClock.autoAdvance = false
            firstDifficulty.performClick()
            rule.mainClock.advanceTimeBy(100)
            rule.onNodeWithText("Bu görev nedir?").assertDoesNotExist() // oyundayız

            pressBack()
            rule.onNodeWithText("Bu görev nedir?").assertExists("sekme=$tab: oyundan girişe dönmeli")
            pressBack()
            rule.onNodeWithText("Korteks").assertExists("sekme=$tab: girişten ana menüye dönmeli")
        }
    }

    @Test
    fun `profil karti acilir ve geri donulur`() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("profil_karti"))
        rule.onNode(hasTestTag("profil_karti")).performClick()
        rule.onNodeWithText("Bilişsel profilim").assertExists()
        rule.onNodeWithText("📤 Paylaş").assertExists()
        pressBack()
        rule.onNodeWithText("Korteks").assertExists()
    }

    @Test
    fun `renk koru modu acilinca stroop zor seviyesi 4 renkle oynanir`() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("renk_koru_anahtari"))
        rule.onNode(hasTestTag("renk_koru_anahtari")).performClick()
        openTab("Dikkat")
        rule.onNode(hasText("Zor  ·  4 renk", substring = true)).assertExists()
    }

    /** Uçtan uca: gerçek kayıt (SharedPreferences) ile bir oyun bitir, ödülü ve ana menüyü kontrol et. */
    @Test
    fun `oyun bitince xp ve seri ana menude gorunur`() {
        rule.onNodeWithText("🔥 0").assertExists() // henüz hiç oynanmadı

        openTab("Dikkat")
        val kolay = rule.onNode(hasText("Kolay", substring = true) and hasClickAction())
        kolay.performScrollTo()
        rule.mainClock.autoAdvance = false
        kolay.performClick()
        rule.mainClock.advanceTimeBy(100)
        rule.mainClock.advanceTimeBy(12 * 3_100L) // 12 sorunun hepsinin süresi dolsun → 0 puan

        rule.onNodeWithText("+10 XP").assertExists() // 0 puan da 10 XP verir
        pressBack()
        pressBack()
        rule.onNodeWithText("🔥 1").assertExists()
        rule.onNodeWithText("10 / 100 XP").assertExists()
        rule.onNodeWithText("Seviye 1").assertExists()
    }

    /**
     * Tema/dil değişince veya ekran döndürülünce Android, Activity'yi yıkıp yeniden kurar (recreate).
     * Oyun durumu Compose'un remember'ında tutulursa bu sırada kaybolur; ViewModel'de tutulursa kalır.
     */
    @Test
    fun `ekran yeniden olusturulunca stroop oyunu kaldigi yerden devam eder`() {
        openTab("Dikkat")
        val kolay = rule.onNode(hasText("Kolay", substring = true) and hasClickAction())
        kolay.performScrollTo()
        rule.mainClock.autoAdvance = false
        kolay.performClick()
        rule.mainClock.advanceTimeBy(100)

        // İlk iki sorunun süresi dolsun (Kolay: 3 sn/soru) → 3. sorudayız
        rule.mainClock.advanceTimeBy(6_200)
        rule.onNodeWithText("3 / 12").assertExists()

        rule.activityRule.scenario.recreate()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("3 / 12").assertExists("Activity yeniden kurulunca oyun sıfırlandı")
    }

    /** Hız oyununda süre bütün oturum için: ekran yeniden kurulunca sayaç sıfırlanmamalı (bedava süre olmasın). */
    @Test
    fun `ekran yeniden olusturulunca hiz oyununun suresi kaldigi yerden devam eder`() {
        openTab("Hız")
        val kolay = rule.onNode(hasText("Kolay", substring = true) and hasClickAction())
        kolay.performScrollTo()
        rule.mainClock.autoAdvance = false
        kolay.performClick()
        rule.mainClock.advanceTimeBy(20_100) // 60 sn'nin 20'si geçti
        rule.onNodeWithText("Kalan: 39 sn  ·  Doğru: 0").assertExists()

        rule.activityRule.scenario.recreate()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("Kalan: 39 sn  ·  Doğru: 0").assertExists("süre sıfırlandı")
    }
}
