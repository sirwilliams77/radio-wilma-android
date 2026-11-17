// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import android.app.Application
import android.content.Context
import android.net.Uri
import android.media.AudioManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchUi(
    val query: String = "",
    val country: String = "PL",
    val tag: String = "",
    val minBitrate: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val results: List<Station> = emptyList(),
    val searched: Boolean = false,
)

data class WeatherUi(
    val info: WeatherInfo? = null,
    val message: String = "",
    val busy: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val player = PlayerController(app)
    private val logoStore = LogoStore(app)
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val playerState: StateFlow<PlayerState> = player.state

    private val _favorites = MutableStateFlow(prefs.loadFavorites())
    val favorites: StateFlow<List<Station>> = _favorites.asStateFlow()

    /** Logos chosen by the user, by station id (only the favourites are loaded). */
    private val _logos = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val logos: StateFlow<Map<String, ImageBitmap>> = _logos.asStateFlow()

    private val _themeName = MutableStateFlow(prefs.themeName)
    val themeName: StateFlow<String> = _themeName.asStateFlow()

    private val _search = MutableStateFlow(SearchUi())
    val search: StateFlow<SearchUi> = _search.asStateFlow()

    private val _weather = MutableStateFlow(WeatherUi())
    val weather: StateFlow<WeatherUi> = _weather.asStateFlow()

    /** Seconds left to the sleep timer, null = timer off. */
    private val _sleepLeft = MutableStateFlow<Int?>(null)
    val sleepLeft: StateFlow<Int?> = _sleepLeft.asStateFlow()
    private var sleepJob: Job? = null

    private var geo: GeoPoint? = prefs.loadGeoPoint()

    init {
        // The last played station is shown (not started) after the app is opened.
        player.onStationChanged = { prefs.lastStationId = it.id }
        loadLogos()
        val last = prefs.lastStationId
        val station = _favorites.value.firstOrNull { it.id == last } ?: _favorites.value.firstOrNull()
        if (station != null) player.prepareSelection(station)
        viewModelScope.launch {
            while (true) {
                refreshWeather()
                delay(20 * 60 * 1000L)
            }
        }
    }

    // ----- playback -----

    fun play(station: Station) {
        prefs.lastStationId = station.id
        player.play(station, _favorites.value)
    }

    fun togglePause() {
        val st = playerState.value.station
        if (st == null) {
            _favorites.value.firstOrNull()?.let { play(it) }
        } else {
            player.togglePause()
        }
    }

    /** Next (+1) or previous (-1) favourite station. */
    fun step(delta: Int) {
        val list = _favorites.value
        if (list.isEmpty()) return
        val current = playerState.value.station
        val index = list.indexOfFirst { it.id == current?.id }
        val next = if (index < 0) 0 else ((index + delta) % list.size + list.size) % list.size
        play(list[next])
    }

    /** Full exit: stops the sound, the timer and the background service. */
    fun exitApp() {
        sleepJob?.cancel()
        _sleepLeft.value = null
        player.exit()
    }

    // ----- volume (system music volume, same as the phone's buttons) -----

    fun maxVolume(): Int = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    fun volume(): Int = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    fun setVolume(v: Int) {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceIn(0, maxVolume()), 0)
    }

    // ----- favourites -----

    fun isFavorite(s: Station): Boolean = _favorites.value.any { it.id == s.id }

    fun addFavorite(s: Station) {
        if (isFavorite(s)) return
        updateFavorites(_favorites.value + s)
    }

    fun removeFavorite(s: Station) {
        updateFavorites(_favorites.value.filterNot { it.id == s.id })
    }

    fun moveFavorite(s: Station, delta: Int) {
        val list = _favorites.value.toMutableList()
        val from = list.indexOfFirst { it.id == s.id }
        val to = from + delta
        if (from < 0 || to < 0 || to >= list.size) return
        list.removeAt(from)
        list.add(to, s)
        updateFavorites(list)
    }

    private fun updateFavorites(list: List<Station>) {
        _favorites.value = list
        prefs.saveFavorites(list)
        player.syncPlaylist(list)
        loadLogos()
    }

    // ----- station logos -----

    private fun readLogos(): Map<String, ImageBitmap> {
        val map = HashMap<String, ImageBitmap>()
        for (st in _favorites.value) logoStore.bitmap(st.id)?.let { map[st.id] = it.asImageBitmap() }
        return map
    }

    private fun loadLogos() {
        viewModelScope.launch(Dispatchers.IO) { _logos.value = readLogos() }
    }

    /** Stores the picked image as the logo of [s]; the notification playlist is refreshed afterwards. */
    fun setLogo(s: Station, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = logoStore.save(s.id, uri)
            if (ok) {
                _logos.value = readLogos()
                withContext(Dispatchers.Main) { player.syncPlaylist(_favorites.value) }
            }
        }
    }

    /** Downloads the catalogue logo of one station. Returns true when a logo was stored. */
    private suspend fun fetchLogoNow(s: Station): Boolean {
        val addr = RadioBrowserApi.faviconUrl(s.id) ?: return false
        val bytes = RadioBrowserApi.downloadImage(addr) ?: return false
        return logoStore.saveBytes(s.id, bytes)
    }

    /** Button in the logo dialog: [onDone] gets true on success. */
    fun fetchLogo(s: Station, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = fetchLogoNow(s)
            if (ok) {
                withContext(Dispatchers.IO) { _logos.value = readLogos() }
                player.syncPlaylist(_favorites.value)
            }
            onDone(ok)
        }
    }

    /** Fetches logos for all favourites without a logo; [onDone] gets (found, tried). */
    fun fetchMissingLogos(onDone: (Int, Int) -> Unit) {
        viewModelScope.launch {
            val todo = _favorites.value.filter { !logoStore.has(it.id) }
            var found = 0
            for (st in todo) if (fetchLogoNow(st)) found++
            if (found > 0) {
                withContext(Dispatchers.IO) { _logos.value = readLogos() }
                player.syncPlaylist(_favorites.value)
            }
            onDone(found, todo.size)
        }
    }

    fun removeLogo(s: Station) {
        viewModelScope.launch(Dispatchers.IO) {
            logoStore.remove(s.id)
            _logos.value = readLogos()
            withContext(Dispatchers.Main) { player.syncPlaylist(_favorites.value) }
        }
    }

    // ----- search -----

    fun setQuery(q: String) = _search.update { it.copy(query = q) }

    fun cycleCountry() = _search.update { it.copy(country = if (it.country == "PL") "" else "PL") }

    fun cycleTag() = _search.update {
        val i = TAGS.indexOf(it.tag)
        it.copy(tag = TAGS[(i + 1) % TAGS.size])
    }

    fun cycleBitrate() = _search.update {
        val i = BITRATES.indexOf(it.minBitrate)
        it.copy(minBitrate = BITRATES[(i + 1) % BITRATES.size])
    }

    fun runSearch() {
        val s = _search.value
        _search.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val found = RadioBrowserApi.search(s.query, s.country.ifBlank { null }, s.tag.ifBlank { null }, s.minBitrate)
                _search.update { it.copy(loading = false, results = found, searched = true) }
            } catch (e: Exception) {
                _search.update { it.copy(loading = false, error = "Nie udało się pobrać listy. Sprawdź połączenie z internetem.") }
            }
        }
    }

    // ----- look -----

    fun setTheme(name: String) {
        prefs.themeName = name
        _themeName.value = name
    }

    // ----- weather -----

    fun weatherQuery(): String = prefs.weatherQuery

    fun setWeatherPlace(query: String) {
        val q = query.trim()
        prefs.weatherQuery = q
        if (q.isEmpty()) {
            geo = null
            prefs.saveGeoPoint(null)
            _weather.value = WeatherUi(message = "Pogoda wyłączona")
            return
        }
        _weather.update { it.copy(busy = true, message = "Szukam miejscowości...") }
        viewModelScope.launch {
            try {
                val point = WeatherApi.geocode(q)
                if (point == null) {
                    _weather.update { it.copy(busy = false, message = "Nie znaleziono miejscowości") }
                } else {
                    geo = point
                    prefs.saveGeoPoint(point)
                    refreshWeather()
                }
            } catch (e: Exception) {
                _weather.update { it.copy(busy = false, message = "Brak połączenia") }
            }
        }
    }

    private suspend fun refreshWeather() {
        val point = geo ?: return
        try {
            val info = WeatherApi.current(point)
            _weather.value = WeatherUi(info = info, message = "Pogoda: " + info.city)
        } catch (e: Exception) {
            _weather.update { it.copy(busy = false, message = "Nie udało się pobrać pogody") }
        }
    }

    // ----- sleep timer -----

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        _sleepLeft.value = minutes * 60
        sleepJob = viewModelScope.launch {
            while ((_sleepLeft.value ?: 0) > 0) {
                delay(1000)
                _sleepLeft.update { (it ?: 1) - 1 }
            }
            player.pause()
            _sleepLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _sleepLeft.value = null
    }

    companion object {
        val TAGS = listOf("", "pop", "rock", "jazz", "classical", "news", "dance", "hits")
        val BITRATES = listOf(0, 64, 128)
    }
}
