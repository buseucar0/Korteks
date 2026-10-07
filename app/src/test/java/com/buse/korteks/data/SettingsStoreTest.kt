package com.buse.korteks.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {
    @Test
    fun `renk koru modu kapali baslar ve kaydedilir`() {
        val app = RuntimeEnvironment.getApplication()
        assertFalse(SettingsStore(app).colorBlindMode)
        SettingsStore(app).colorBlindMode = true
        assertTrue(SettingsStore(app).colorBlindMode) // yeniden açılmış gibi
    }
}
