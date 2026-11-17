// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Weather from Open-Meteo (https://open-meteo.com, data licensed CC BY 4.0, free for non-commercial use). */
object WeatherApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", RadioBrowserApi.USER_AGENT).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP " + response.code)
            return response.body?.string() ?: throw IOException("Pusta odpowiedź")
        }
    }

    /** Finds a place by name, null when nothing matches. */
    suspend fun geocode(query: String): GeoPoint? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query.trim())
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "pl")
            .addQueryParameter("format", "json")
            .build()
        val root = JSONObject(get(url.toString()))
        val results = root.optJSONArray("results") ?: return@withContext null
        if (results.length() == 0) return@withContext null
        val r = results.getJSONObject(0)
        GeoPoint(r.optString("name", query.trim()), r.getDouble("latitude"), r.getDouble("longitude"))
    }

    suspend fun current(point: GeoPoint): WeatherInfo = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", point.lat.toString())
            .addQueryParameter("longitude", point.lon.toString())
            .addQueryParameter("current", "temperature_2m,weather_code,is_day")
            .addQueryParameter("timezone", "auto")
            .build()
        val current = JSONObject(get(url.toString())).getJSONObject("current")
        WeatherInfo(
            city = point.name,
            temperature = current.getDouble("temperature_2m").roundToInt(),
            code = current.getInt("weather_code"),
            isDay = current.optInt("is_day", 1) == 1,
        )
    }
}
