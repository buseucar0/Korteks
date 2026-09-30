package com.buse.korteks.data

import android.content.Context
import com.buse.korteks.game.AttributeRule
import com.buse.korteks.game.InkColor
import com.buse.korteks.game.MatrixPuzzleSpec
import com.buse.korteks.game.RuleType
import com.buse.korteks.game.Shape
import org.json.JSONObject

/**
 * assets/matrix_puzzles.json dosyasını okuyup game/ katmanının modellerine çevirir.
 * Android'e bağlı olduğu için (Context, org.json) game/ paketinde değil burada duruyor.
 * org.json Android'in içinde hazır gelir, ek kütüphane gerektirmez.
 */
object MatrixPuzzleLoader {

    fun load(context: Context): List<MatrixPuzzleSpec> {
        val text = context.assets.open("matrix_puzzles.json").bufferedReader().use { it.readText() }
        val array = JSONObject(text).getJSONArray("puzzles")
        return (0 until array.length())
            .map { parse(array.getJSONObject(it)) }
            .onEach { it.validate() } // bozuk bulmaca varsa uygulama açılırken hemen anlaşılsın
    }

    private fun parse(o: JSONObject) = MatrixPuzzleSpec(
        id = o.getInt("id"),
        level = o.getInt("seviye"),
        shape = rule(o, "sekil", default = null) { Shape.valueOf(it as String) },
        color = rule(o, "renk", default = null) { InkColor.valueOf(it as String) },
        count = rule(o, "adet", default = 1) { it as Int },
        rotation = rule(o, "aci", default = 0) { it as Int },
    )

    /** Özellik JSON'da yoksa [default] ile SABIT kural kullanılır (default null ise zorunlu alan). */
    private fun <T> rule(o: JSONObject, key: String, default: T?, convert: (Any) -> T): AttributeRule<T> {
        if (!o.has(key)) {
            requireNotNull(default) { "Bulmaca ${o.optInt("id")}: '$key' alanı zorunlu" }
            return AttributeRule(RuleType.SABIT, listOf(default))
        }
        val r = o.getJSONObject(key)
        val values = r.getJSONArray("degerler")
        return AttributeRule(
            type = RuleType.valueOf(r.getString("kural")),
            values = (0 until values.length()).map { convert(values.get(it)) },
        )
    }
}
