package com.buse.korteks.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Bir görseli PNG olarak önbelleğe yazar ve Android'in paylaşım menüsünü açar (Instagram, WhatsApp...).
 * Dosya FileProvider ile content:// adresinden paylaşılır; alan uygulamaya sadece okuma izni verilir.
 * Dönüş: yazılan dosya (test için).
 */
fun shareImage(context: Context, bitmap: Bitmap, fileName: String = "korteks_profil.png"): File {
    val dir = File(context.cacheDir, "paylasim").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.dosyalar", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(send, "Profil kartını paylaş")
    // Activity dışı bir Context'ten (ör. testte Application) açılıyorsa yeni görev gerekir
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
    return file
}
