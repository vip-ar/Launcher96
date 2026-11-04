package com.tv96.launcher

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** درجة الحرارة الحالية في عمّان من Open-Meteo (بدون مفتاح). */
object Weather {
    fun fetch(onResult: (Int?) -> Unit) {
        Thread {
            var temp: Int? = null
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=31.95&longitude=35.93&current=temperature_2m"
                )
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                val body = c.inputStream.bufferedReader().use { it.readText() }
                temp = JSONObject(body).getJSONObject("current").getDouble("temperature_2m").roundToInt()
            } catch (e: Exception) {
                // بدون إنترنت أو خطأ: نترك القيمة فارغة
            }
            Handler(Looper.getMainLooper()).post { onResult(temp) }
        }.start()
    }
}
