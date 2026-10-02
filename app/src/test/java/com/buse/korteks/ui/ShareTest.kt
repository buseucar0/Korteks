package com.buse.korteks.ui

import android.content.Intent
import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShareTest {

    @Test
    fun `gorsel png olarak yazilir ve paylasim menusu acilir`() {
        val app = RuntimeEnvironment.getApplication()
        val bitmap = Bitmap.createBitmap(100, 160, Bitmap.Config.ARGB_8888)

        val file = shareImage(app, bitmap)

        assertTrue(file.exists() && file.length() > 0)
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, started.action)
        @Suppress("DEPRECATION")
        val send = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/png", send.type)
        assertTrue(send.getParcelableExtraUri()!!.toString().startsWith("content://"))
    }

    @Suppress("DEPRECATION")
    private fun Intent.getParcelableExtraUri() = getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
}
