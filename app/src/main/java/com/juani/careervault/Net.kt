package com.juani.careervault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

fun trimNum(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString()
    else String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

fun fmtMoney(v: Long): String {
    val a = kotlin.math.abs(v)
    val s = if (v < 0) "-" else ""
    return when {
        a >= 1_000_000 -> s + "€" + trimNum(a / 1_000_000.0) + "M"
        a >= 1_000 -> s + "€" + (a / 1_000) + "K"
        else -> s + "€" + a
    }
}

fun String.n(): Int = toIntOrNull() ?: 0

object Crests {
    private fun dir(ctx: Context): File = File(ctx.filesDir, "crests").apply { mkdirs() }

    private fun save(ctx: Context, bmp: Bitmap): String {
        val s = 256f / maxOf(bmp.width, bmp.height)
        val out = if (s < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt().coerceAtLeast(1), (bmp.height * s).toInt().coerceAtLeast(1), true)
        } else bmp
        val f = File(dir(ctx), "c_${System.nanoTime()}.png")
        f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f.absolutePath
    }

    /** Escudo elegido a mano desde la galería. */
    suspend fun fromUri(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bmp == null) null else save(ctx, bmp)
        } catch (e: Exception) {
            null
        }
    }

    /** Busca el escudo en Wikipedia (imagen principal de la página del club o selección). */
    suspend fun search(ctx: Context, name: String, national: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            val term = URLEncoder.encode(name.trim() + if (national) " national football team" else " football club", "UTF-8")
            val api = "https://en.wikipedia.org/w/api.php?action=query&format=json&generator=search" +
                "&gsrsearch=$term&gsrlimit=4&prop=pageimages&piprop=thumbnail&pithumbsize=300&redirects=1"
            val root = JSONObject(String(httpGet(api), Charsets.UTF_8))
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return@withContext null
            var bestIdx = Int.MAX_VALUE
            var bestUrl: String? = null
            val keys = pages.keys()
            while (keys.hasNext()) {
                val p = pages.getJSONObject(keys.next())
                val th = p.optJSONObject("thumbnail")?.optString("source") ?: continue
                val idx = p.optInt("index", 99)
                if (th.isNotBlank() && idx < bestIdx) { bestIdx = idx; bestUrl = th }
            }
            val url = bestUrl ?: return@withContext null
            val bytes = httpGet(url)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            save(ctx, bmp)
        } catch (e: Exception) {
            null
        }
    }

    private fun httpGet(u: String): ByteArray {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "CareerVault/1.0 (Android; personal career tracker)")
        return c.inputStream.use { it.readBytes() }
    }
}
