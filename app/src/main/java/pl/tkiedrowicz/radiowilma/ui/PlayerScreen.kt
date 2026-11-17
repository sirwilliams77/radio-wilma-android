// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import pl.tkiedrowicz.radiowilma.R
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.tkiedrowicz.radiowilma.Station
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.tkiedrowicz.radiowilma.MainViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PL: Locale = Locale.forLanguageTag("pl-PL")

private fun utcLabel(): String {
    val seconds = ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds
    val hours = seconds / 3600
    val minutes = kotlin.math.abs(seconds % 3600) / 60
    val sign = if (hours >= 0) "+" else "-"
    return "UTC" + sign + kotlin.math.abs(hours) + (if (minutes != 0) ":" + minutes.toString().padStart(2, '0') else "")
}

@Composable
fun PlayerScreen(vm: MainViewModel, onExit: () -> Unit) {
    val p = LocalPalette.current
    val state by vm.playerState.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val weather by vm.weather.collectAsState()
    val logos by vm.logos.collectAsState()
    val station = state.station

    var showPicker by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var volume by remember { mutableFloatStateOf(vm.volume().toFloat()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            volume = vm.volume().toFloat()
            delay(1000)
        }
    }

    val dateText: AnnotatedString = buildAnnotatedString {
        val weekday = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, PL)
            .replaceFirstChar { it.uppercase() }
        append("$weekday, ")
        withStyle(SpanStyle(color = p.accent)) { append(now.format(DateTimeFormatter.ofPattern("d MMMM", PL))) }
        append(" " + now.year)
    }

    val statusText = when {
        state.error != null -> "BŁĄD"
        state.buffering -> "BUFOR"
        state.isPlaying -> "GRA"
        else -> "PAUZA"
    }
    val index = favorites.indexOfFirst { it.id == station?.id }

    if (showPicker) {
        StationPicker(
            stations = favorites,
            logos = logos,
            currentId = station?.id,
            onPick = { vm.play(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ----- pieces shared by the portrait and the landscape layout -----

    val topBar: @Composable () -> Unit = {
        Row(
            Modifier
                .fillMaxWidth()
                .height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_radio_wilma),
                contentDescription = "Radio Wilma",
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)),
            )
            Text(dateText, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(statusText, color = if (state.error != null) Color(0xFFEF4444) else p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                RoundButton("Wyjdź z aplikacji", onExit, size = 30.dp, border = p.frame) { PowerIcon(p.accent, 16.dp) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }

    val clockTile: @Composable (Modifier) -> Unit = { m ->
        Tile(m) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    color = p.clock,
                    fontSize = minOf(maxWidth.value * 0.27f, maxHeight.value * 0.8f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }

    val weatherRow: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(Modifier.weight(1f).fillMaxSize()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val info = weather.info
                    Text(if (info != null) info.temperature.toString() + "°" else "--°", color = p.clock, fontSize = if (landscape) 26.sp else 32.sp, fontWeight = FontWeight.Bold)
                    Text(info?.city ?: "Ustaw pogodę", color = TextCity, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Tile(Modifier.weight(1f).fillMaxSize()) {
                val info = weather.info
                if (info != null) {
                    WeatherIcon(info.code, info.isDay, Modifier.align(Alignment.Center).size(if (landscape) 40.dp else 64.dp))
                }
            }
        }
    }

    val stationRow: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(Modifier.width(if (landscape) 80.dp else 96.dp).fillMaxSize()) {
                val logo = station?.let { logos[it.id] }
                if (logo != null) {
                    Image(
                        bitmap = logo,
                        contentDescription = station?.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.align(Alignment.Center).size(if (landscape) 56.dp else 66.dp).clip(RoundedCornerShape(8.dp)),
                    )
                } else Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(width = if (landscape) 56.dp else 72.dp, height = if (landscape) 46.dp else 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(p.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = station?.initials ?: "FM",
                        color = Color.Black,
                        fontSize = if (landscape) 18.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
            Tile(Modifier.weight(1f).fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().clickable(role = Role.Button) { showPicker = true }.padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("STACJA RADIOWA  ▾", color = p.label, fontSize = 11.sp, letterSpacing = 3.sp)
                    Text(
                        text = station?.name ?: "Wybierz stację",
                        color = p.station,
                        fontSize = if (landscape) 21.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(station?.meta ?: "", color = p.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    val nowPlayingTile: @Composable (Modifier) -> Unit = { m ->
        Tile(m) {
            Column(Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("AKTUALNIE GRA:", color = p.label, fontSize = 11.sp, letterSpacing = 3.sp)
                when {
                    state.error != null -> Text(state.error ?: "", color = Color(0xFFEF4444), fontSize = 16.sp, textAlign = TextAlign.Center)
                    state.artist.isNotEmpty() || state.title.isNotEmpty() -> {
                        if (state.artist.isNotEmpty()) {
                            Text(state.artist, color = p.artist, fontSize = if (landscape) 18.sp else 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (state.title.isNotEmpty()) {
                            Text(state.title, color = p.title, fontSize = if (landscape) 16.sp else 20.sp, fontWeight = FontWeight.Medium, maxLines = if (landscape) 1 else 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                    else -> {
                        Text(if (station == null) "Wybierz stację z listy" else "Brak informacji o utworze", color = p.label, fontSize = 15.sp)
                        if (station != null && state.isPlaying) Text(state.debug, color = p.label.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }
    }

    val vinylRow: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile(Modifier.width(if (landscape) 80.dp else 100.dp).fillMaxSize()) {
                Vinyl(state.isPlaying, Modifier.align(Alignment.Center).size(if (landscape) 56.dp else 76.dp))
            }
            Tile(Modifier.weight(1f).fillMaxSize()) {
                Equalizer(state.isPlaying, Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }
    }

    val controlsRow: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().height(if (landscape) 52.dp else 60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpeakerIcon(p.label)
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    vm.setVolume(it.toInt())
                },
                valueRange = 0f..vm.maxVolume().toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = TextPrimary,
                    activeTrackColor = p.accent,
                    inactiveTrackColor = p.divider,
                ),
                modifier = Modifier.weight(1f),
            )
            RoundButton("Poprzednia stacja", { vm.step(-1) }, size = 44.dp, border = p.frame) { SkipIcon(TextPrimary, forward = false, size = 18.dp) }
            RoundButton("Odtwarzaj lub pauza", { vm.togglePause() }, size = if (landscape) 48.dp else 54.dp, fill = p.accent) {
                if (state.isPlaying || state.buffering) PauseIcon(Color.Black, 22.dp) else PlayIcon(Color.Black, 22.dp)
            }
            RoundButton("Następna stacja", { vm.step(1) }, size = 44.dp, border = p.frame) { SkipIcon(TextPrimary, forward = true, size = 18.dp) }
        }
    }

    val infoStrip: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val favText = if (index >= 0) "♥ " + (index + 1) + "/" + favorites.size else "♥ " + favorites.size
            val kbps = if (station != null && station.bitrate > 0) station.bitrate.toString() + " kbps" else "— kbps"
            val tag = station?.firstTag?.ifBlank { "—" } ?: "—"
            val codec = station?.codec?.ifBlank { "—" } ?: "—"
            for (t in listOf(favText, kbps, codec, tag, utcLabel())) {
                Text(t, color = TextCity, fontSize = 12.sp, maxLines = 1)
            }
        }
    }

    // ----- layouts -----

    if (landscape) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Background)
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            topBar()
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    clockTile(Modifier.fillMaxWidth().weight(1.2f))
                    weatherRow(Modifier.fillMaxWidth().weight(0.8f))
                    stationRow(Modifier.fillMaxWidth().weight(1f))
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    nowPlayingTile(Modifier.fillMaxWidth().weight(1f))
                    vinylRow(Modifier.fillMaxWidth().weight(1f))
                    controlsRow()
                    infoStrip()
                }
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(Background)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            topBar()
            clockTile(Modifier.fillMaxWidth().weight(1.35f))
            weatherRow(Modifier.fillMaxWidth().weight(0.75f))
            stationRow(Modifier.fillMaxWidth().weight(0.95f))
            nowPlayingTile(Modifier.fillMaxWidth().weight(0.85f))
            vinylRow(Modifier.fillMaxWidth().weight(1f))
            controlsRow()
            infoStrip()
        }
    }
}

/** Pop-up with the favourite stations; tapping one starts it. */
@Composable
private fun StationPicker(stations: List<Station>, logos: Map<String, ImageBitmap>, currentId: String?, onPick: (Station) -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(shape)
                .background(Background)
                .border(1.5.dp, p.frame, shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("WYBIERZ STACJĘ", color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            if (stations.isEmpty()) {
                Text("Nie masz jeszcze ulubionych stacji. Dodaj je w zakładce Szukaj.", color = p.label, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stations, key = { it.id }) { s ->
                        val active = s.id == currentId
                        val rowShape = RoundedCornerShape(14.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .background(if (active) p.accent.copy(alpha = 0.10f) else Background)
                                .border(1.5.dp, if (active) p.frame else p.divider, rowShape)
                                .clickable(role = Role.Button) { onPick(s) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StationAvatar(s.initials, active, logo = logos[s.id])
                            Column(Modifier.weight(1f)) {
                                Text(s.name, color = if (active) p.station else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(s.meta, color = p.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            ActionButton("Zamknij", onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
