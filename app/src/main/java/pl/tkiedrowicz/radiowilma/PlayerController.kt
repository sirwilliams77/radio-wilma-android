// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Title shown on the notification / lock screen tile until the stream sends the name of the song. */
const val IDLE_TITLE = "Radio Wilma"

/** Thin wrapper over a MediaController connected to [PlaybackService]; exposes the player state as a flow. */
@OptIn(UnstableApi::class)
class PlayerController(private val context: Context) {
    private var controller: MediaController? = null
    private var connecting = false
    private var pending: Station? = null
    private val prefs = Prefs(context)
    private val logoStore = LogoStore(context)

    /** Stations in the playlist handed to the player (the favourites), used by next / previous on the notification. */
    private var stations: List<Station> = emptyList()
    /** The Radio Wilma logo (res/raw) shown on the notification and the lock screen tile. */
    private val defaultArtworkUri: Uri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.logo_notification)

    /** Song title read straight from the stream (ICY / ID3) by the player in the service; empty = none yet. */
    private var streamTitle = ""
    private var metaCount = 0
    private var metaKinds = ""
    private var directPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    private val directListener = object : Player.Listener {
        override fun onMetadata(metadata: Metadata) {
            var artist = ""
            var title = ""
            for (i in 0 until metadata.length()) {
                val e = metadata[i]
                metaCount++
                if (!metaKinds.contains(e.javaClass.simpleName)) metaKinds += e.javaClass.simpleName + " "
                when {
                    e is IcyInfo -> if (!e.title.isNullOrBlank()) title = e.title!!.trim()
                    e is TextInformationFrame && e.id == "TIT2" -> title = e.values.firstOrNull().orEmpty()
                    e is TextInformationFrame && e.id == "TPE1" -> artist = e.values.firstOrNull().orEmpty()
                }
            }
            if (title.isNotBlank()) {
                streamTitle = if (artist.isNotBlank()) artist + " - " + title else title
            }
            refresh()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val t = mediaMetadata.title?.toString().orEmpty().trim()
            if (t.isNotEmpty() && t != IDLE_TITLE && t != _state.value.station?.name) streamTitle = t
            refresh()
        }
    }

    private fun attachDirect() {
        val pl = PlaybackService.sharedPlayer
        if (pl === directPlayer) return
        directPlayer?.removeListener(directListener)
        directPlayer = pl
        pl?.addListener(directListener)
    }

    /** Called when the station changes from outside the app screen (notification buttons). */
    var onStationChanged: ((Station) -> Unit)? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = refresh()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = syncStation(mediaItem?.mediaId)
        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(isPlaying = false, buffering = false, error = "Nie udało się odtworzyć strumienia") }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            connecting = false
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                attachDirect()
                val waiting = pending
                pending = null
                if (waiting != null) {
                    play(waiting, stations)
                } else {
                    syncStation(c.currentMediaItem?.mediaId)
                    refresh()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Brak połączenia z usługą odtwarzania") }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** The station was changed by the notification buttons: follow it on the app screen. */
    private fun syncStation(id: String?) {
        if (id == null || id == _state.value.station?.id) return
        val st = stations.firstOrNull { it.id == id } ?: prefs.loadFavorites().firstOrNull { it.id == id } ?: return
        streamTitle = ""
        _state.update { it.copy(station = st, artist = "", title = "", error = null) }
        onStationChanged?.invoke(st)
    }

    private fun refresh() {
        val c = controller ?: return
        val md = c.mediaMetadata
        attachDirect()
        val rawTitle = streamTitle.ifEmpty { md.title?.toString().orEmpty().trim() }
        var artist = ""
        var title = ""
        // ICY stream title usually looks like "Artist - Title"; until it arrives the title is only our IDLE_TITLE.
        if (rawTitle.isNotEmpty() && rawTitle != IDLE_TITLE && rawTitle != _state.value.station?.name) {
            val cut = rawTitle.indexOf(" - ")
            if (cut > 0) {
                artist = rawTitle.substring(0, cut).trim()
                title = rawTitle.substring(cut + 3).trim()
            } else {
                title = rawTitle
            }
        }
        _state.update {
            it.copy(
                isPlaying = c.isPlaying,
                buffering = c.playbackState == Player.STATE_BUFFERING,
                artist = artist,
                title = title,
                error = if (c.isPlaying) null else it.error,
                debug = "dane ze strumienia: " + metaCount + (if (metaKinds.isNotEmpty()) " (" + metaKinds.trim() + ")" else ""),
            )
        }
    }

    /** Shows a station as selected without starting it (the first press of play starts it). */
    fun prepareSelection(station: Station) {
        _state.update { if (it.station == null) it.copy(station = station) else it }
    }

    /** Starts [station]; [playlist] (the favourites) lets the notification buttons switch to the neighbouring stations. */
    fun play(station: Station, playlist: List<Station> = emptyList()) {
        streamTitle = ""
        metaCount = 0
        metaKinds = ""
        _state.update { it.copy(station = station, artist = "", title = "", error = null, buffering = true) }
        val list = if (playlist.any { it.id == station.id }) playlist else listOf(station)
        stations = list
        val c = controller
        if (c == null || !c.isConnected) {
            // First start, or the service was shut down by the notification close button: connect again.
            controller = null
            pending = station
            connect()
            return
        }
        c.repeatMode = Player.REPEAT_MODE_ALL
        c.setMediaItems(list.map { mediaItem(it) }, list.indexOfFirst { it.id == station.id }, C.TIME_UNSET)
        c.prepare()
        c.play()
    }

    /** Keeps the player's playlist in step with the favourites without interrupting the sound. */
    fun syncPlaylist(list: List<Station>) {
        val c = controller ?: return
        val current = c.currentMediaItem ?: return
        val idx = list.indexOfFirst { it.id == current.mediaId }
        if (idx < 0) return
        val ci = c.currentMediaItemIndex
        if (ci + 1 < c.mediaItemCount) c.removeMediaItems(ci + 1, c.mediaItemCount)
        if (ci > 0) c.removeMediaItems(0, ci)
        c.addMediaItems(0, list.take(idx).map { mediaItem(it) })
        c.addMediaItems(list.drop(idx + 1).map { mediaItem(it) })
        stations = list
    }

    fun togglePause() {
        val c = controller
        val station = _state.value.station
        when {
            c == null || !c.isConnected -> if (station != null) play(station, stations)
            c.isPlaying -> c.pause()
            station != null && (c.mediaItemCount == 0 || c.playbackState == Player.STATE_IDLE) -> play(station, stations)
            else -> c.play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    /** Stops the sound, releases the connection and shuts the playback service down (the app exit button). */
    fun exit() {
        controller?.run {
            stop()
            clearMediaItems()
            removeListener(listener)
            release()
        }
        directPlayer?.removeListener(directListener)
        directPlayer = null
        controller = null
        context.stopService(Intent(context, PlaybackService::class.java))
    }

    // ----- notification / lock screen content -----

    /** The station's own logo when the user set one, otherwise the Radio Wilma logo. */
    private fun artworkFor(s: Station): Uri = logoStore.file(s.id)?.let { Uri.fromFile(it) } ?: defaultArtworkUri

    private fun mediaItem(s: Station): MediaItem =
        MediaItem.Builder()
            .setMediaId(s.id)
            .setUri(s.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(IDLE_TITLE)
                    .setArtist(s.name)
                    .setArtworkUri(artworkFor(s))
                    .build()
            )
            .build()
}
