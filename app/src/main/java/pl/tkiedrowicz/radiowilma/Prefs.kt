// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Settings and favourites kept in SharedPreferences. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wilma", Context.MODE_PRIVATE)

    fun loadFavorites(): List<Station> {
        val raw = sp.getString("favorites", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Station(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    url = o.getString("url"),
                    country = o.optString("country"),
                    tags = o.optString("tags"),
                    bitrate = o.optInt("bitrate"),
                    codec = o.optString("codec"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveFavorites(list: List<Station>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("url", s.url)
                    .put("country", s.country)
                    .put("tags", s.tags)
                    .put("bitrate", s.bitrate)
                    .put("codec", s.codec)
            )
        }
        sp.edit().putString("favorites", arr.toString()).apply()
    }

    var themeName: String
        get() = sp.getString("theme", "Niebieski") ?: "Niebieski"
        set(value) = sp.edit().putString("theme", value).apply()

    var lastStationId: String
        get() = sp.getString("lastStation", "") ?: ""
        set(value) = sp.edit().putString("lastStation", value).apply()

    /** City typed by the user for the weather tile (empty = weather off). */
    var weatherQuery: String
        get() = sp.getString("weatherQuery", "") ?: ""
        set(value) = sp.edit().putString("weatherQuery", value).apply()

    fun loadGeoPoint(): GeoPoint? {
        val name = sp.getString("geoName", null) ?: return null
        val lat = sp.getString("geoLat", null)?.toDoubleOrNull() ?: return null
        val lon = sp.getString("geoLon", null)?.toDoubleOrNull() ?: return null
        return GeoPoint(name, lat, lon)
    }

    fun saveGeoPoint(p: GeoPoint?) {
        val e = sp.edit()
        if (p == null) {
            e.remove("geoName").remove("geoLat").remove("geoLon")
        } else {
            e.putString("geoName", p.name).putString("geoLat", p.lat.toString()).putString("geoLon", p.lon.toString())
        }
        e.apply()
    }
}
