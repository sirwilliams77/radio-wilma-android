// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Station search in the community catalogue https://www.radio-browser.info (public API, no key). */
object RadioBrowserApi {
    const val USER_AGENT = "RadioWilmaAndroid/1.0"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Several mirrors: the next one is tried when a server does not answer.
    private val servers = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info",
    )

    /**
     * @param name part of the station name (may be empty)
     * @param countryCode e.g. "PL", empty/null = all countries
     * @param tag genre, e.g. "jazz", empty/null = any
     * @param minBitrate 0 = any
     */
    suspend fun search(name: String, countryCode: String?, tag: String?, minBitrate: Int): List<Station> =
        withContext(Dispatchers.IO) {
            var last: Exception? = null
            for (base in servers) {
                try {
                    val url = base.toHttpUrl().newBuilder()
                        .addPathSegments("json/stations/search")
                        .addQueryParameter("limit", "40")
                        .addQueryParameter("hidebroken", "true")
                        .addQueryParameter("lastcheckok", "1")
                        .addQueryParameter("order", "clickcount")
                        .addQueryParameter("reverse", "true")
                    if (name.isNotBlank()) url.addQueryParameter("name", name.trim())
                    if (!countryCode.isNullOrBlank()) url.addQueryParameter("countrycode", countryCode)
                    if (!tag.isNullOrBlank()) url.addQueryParameter("tag", tag)
                    if (minBitrate > 0) url.addQueryParameter("bitrateMin", minBitrate.toString())
                    val request = Request.Builder().url(url.build()).header("User-Agent", USER_AGENT).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("HTTP " + response.code)
                        val body = response.body?.string() ?: "[]"
                        return@withContext parse(body)
                    }
                } catch (e: Exception) {
                    last = e
                }
            }
            throw last ?: IOException("Brak połączenia z katalogiem stacji")
        }

    /** Logo address of a station from the catalogue (by station UUID), or null when there is none. */
    suspend fun faviconUrl(uuid: String): String? = withContext(Dispatchers.IO) {
        for (base in servers) {
            try {
                val url = base.toHttpUrl().newBuilder()
                    .addPathSegments("json/stations/byuuid")
                    .addQueryParameter("uuids", uuid)
                    .build()
                val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP " + response.code)
                    val arr = JSONArray(response.body?.string() ?: "[]")
                    if (arr.length() == 0) return@withContext null
                    val f = arr.getJSONObject(0).optString("favicon").trim()
                    return@withContext if (f.startsWith("http")) f else null
                }
            } catch (e: Exception) {
                // try the next mirror
            }
        }
        null
    }

    /** Downloads a small image (max 2 MB); http addresses are retried over https when they fail. */
    suspend fun downloadImage(address: String): ByteArray? = withContext(Dispatchers.IO) {
        val candidates = if (address.startsWith("http://")) listOf(address, "https://" + address.removePrefix("http://")) else listOf(address)
        for (u in candidates) {
            try {
                val request = Request.Builder().url(u).header("User-Agent", USER_AGENT).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body ?: return@use
                    if (body.contentLength() > 2_000_000) return@use
                    val bytes = body.bytes()
                    if (bytes.size in 1..2_000_000) return@withContext bytes
                }
            } catch (e: Exception) {
                // next candidate
            }
        }
        null
    }

    private fun parse(json: String): List<Station> {
        val arr = JSONArray(json)
        val out = ArrayList<Station>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val resolved = o.optString("url_resolved").ifBlank { o.optString("url") }
            val name = o.optString("name").trim()
            if (resolved.isBlank() || name.isBlank()) continue
            // Same filter as Radio Wilma 4.0: HLS and playlists are skipped (they carry no song titles).
            val low = resolved.lowercase()
            if (o.optInt("hls") != 0 || low.contains(".m3u") || low.contains(".pls") || low.contains(".asx")) continue
            out += Station(
                id = o.optString("stationuuid").ifBlank { resolved },
                name = name,
                url = resolved,
                country = o.optString("countrycode"),
                tags = o.optString("tags"),
                bitrate = o.optInt("bitrate"),
                codec = o.optString("codec"),
            )
        }
        return out
    }
}
