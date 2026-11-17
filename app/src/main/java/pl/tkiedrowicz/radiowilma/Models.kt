// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

/** A radio station: [id] is the radio-browser.info station UUID (or any unique text). */
data class Station(
    val id: String,
    val name: String,
    val url: String,
    val country: String = "",
    val tags: String = "",
    val bitrate: Int = 0,
    val codec: String = "",
) {
    /** Up to two letters shown in a circle instead of the station logo. */
    val initials: String
        get() {
            val words = name.trim().split(" ", "-", ".").filter { it.isNotEmpty() }
            return when {
                words.isEmpty() -> "FM"
                words.size == 1 -> words[0].take(2).uppercase()
                else -> (words[0].take(1) + words[1].take(1)).uppercase()
            }
        }

    /** First genre tag, upper case, or an empty text. */
    val firstTag: String
        get() = tags.split(",").firstOrNull { it.isNotBlank() }?.trim()?.uppercase().orEmpty()

    /** Short description for lists, e.g. "PL - jazz - MP3 128 kbit/s". */
    val meta: String
        get() {
            val parts = mutableListOf<String>()
            if (country.isNotBlank()) parts += country
            if (firstTag.isNotBlank()) parts += firstTag.lowercase()
            val q = buildString {
                if (codec.isNotBlank()) append(codec)
                if (bitrate > 0) {
                    if (isNotEmpty()) append(' ')
                    append(bitrate).append(" kbit/s")
                }
            }
            if (q.isNotBlank()) parts += q
            return parts.joinToString(" · ")
        }
}

data class WeatherInfo(
    val city: String,
    val temperature: Int,
    val code: Int,
    val isDay: Boolean,
)

data class GeoPoint(val name: String, val lat: Double, val lon: Double)

data class PlayerState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val artist: String = "",
    val title: String = "",
    val error: String? = null,
    /** Short diagnostic text: what the stream metadata looked like (shown when no song title is known). */
    val debug: String = "",
)
