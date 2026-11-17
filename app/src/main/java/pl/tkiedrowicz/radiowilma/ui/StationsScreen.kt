// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import pl.tkiedrowicz.radiowilma.Station
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.tkiedrowicz.radiowilma.MainViewModel

@Composable
fun StationsScreen(vm: MainViewModel, onSearch: () -> Unit, onPlayed: () -> Unit) {
    val p = LocalPalette.current
    val favorites by vm.favorites.collectAsState()
    val state by vm.playerState.collectAsState()
    val current = state.station
    val logos by vm.logos.collectAsState()
    var dialogId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickId by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val st = favorites.firstOrNull { it.id == pickId }
        pickId = null
        if (uri != null && st != null) vm.setLogo(st, uri)
    }

    var addOpen by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("") }
    if (addOpen) {
        AddStationDialog(
            onAdd = { name, url ->
                vm.addFavorite(Station(id = "url:" + url, name = name, url = url))
                addOpen = false
            },
            onDismiss = { addOpen = false },
        )
    }

    val dialogStation = favorites.firstOrNull { it.id == dialogId }
    if (dialogStation != null) {
        LogoDialog(
            station = dialogStation,
            hasLogo = logos.containsKey(dialogStation.id),
            busy = busy,
            info = info,
            onFetch = {
                busy = true
                info = ""
                vm.fetchLogo(dialogStation) { ok ->
                    busy = false
                    if (ok) dialogId = null else info = "Nie znaleziono logo tej stacji w katalogu."
                }
            },
            onPick = {
                pickId = dialogStation.id
                dialogId = null
                info = ""
                picker.launch("image/*")
            },
            onRemove = {
                vm.removeLogo(dialogStation)
                dialogId = null
            },
            onDismiss = { dialogId = null; info = "" },
        )
    }

    Column(Modifier.fillMaxSize().background(Background).padding(horizontal = 16.dp)) {
        ScreenTitle("STACJE", "Twoje ulubione · " + favorites.size, Modifier.padding(top = 14.dp, bottom = if (favorites.isEmpty()) 12.dp else 4.dp))
        ActionButton("Dodaj stację z adresu URL", { addOpen = true }, modifier = Modifier.fillMaxWidth())
        if (favorites.isNotEmpty()) {
            Text("Dotknij kółka przy stacji, aby ustawić jej logo.", color = p.label, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            ActionButton(
                if (busy) "Pobieram logo…" else "Pobierz brakujące logo z internetu",
                {
                    if (!busy) {
                        busy = true
                        info = ""
                        vm.fetchMissingLogos { found, tried ->
                            busy = false
                            info = if (tried == 0) "Wszystkie stacje mają już logo." else "Pobrano logo: " + found + " z " + tried + "."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (info.isNotEmpty() && dialogId == null) Text(info, color = p.label, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (favorites.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nie masz jeszcze ulubionych stacji", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                Text("Wyszukaj stację w katalogu i dodaj ją do listy.", color = p.label, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
                ActionButton("Szukaj stacji", onSearch, filled = true)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favorites, key = { it.id }) { s ->
                    val active = s.id == current?.id
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(if (active) p.accent.copy(alpha = 0.10f) else Background)
                            .border(1.5.dp, if (active) p.frame else p.divider, shape)
                            .clickable(role = Role.Button) {
                                vm.play(s)
                                onPlayed()
                            }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StationAvatar(s.initials, active, logo = logos[s.id], modifier = Modifier.clickable(role = Role.Button) { dialogId = s.id })
                        Column(Modifier.weight(1f)) {
                            Text(s.name, color = if (active) p.station else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.meta, color = p.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        RoundButton("Przesuń wyżej", { vm.moveFavorite(s, -1) }, size = 40.dp) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = p.label)
                        }
                        RoundButton("Przesuń niżej", { vm.moveFavorite(s, 1) }, size = 40.dp) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = p.label)
                        }
                        RoundButton("Usuń z ulubionych", { vm.removeFavorite(s) }, size = 40.dp) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = p.label)
                        }
                    }
                }
            }
        }
    }
}

/** Pop-up for one station: choose a logo from the phone or remove the current one. */
@Composable
private fun LogoDialog(station: Station, hasLogo: Boolean, busy: Boolean, info: String, onFetch: () -> Unit, onPick: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Background)
                .border(1.5.dp, p.frame, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("LOGO STACJI", color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text(station.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Logo jest zmniejszane i zapisywane tylko w aplikacji. Logo stacji należy do jej nadawcy.", color = p.label, fontSize = 12.sp)
            ActionButton(if (busy) "Pobieram…" else "Pobierz logo z internetu", { if (!busy) onFetch() }, filled = true, modifier = Modifier.fillMaxWidth())
            if (info.isNotEmpty()) Text(info, color = p.accent, fontSize = 12.sp)
            ActionButton("Wybierz obraz z telefonu", onPick, modifier = Modifier.fillMaxWidth())
            if (hasLogo) ActionButton("Usuń logo (wróć do inicjałów)", onRemove, modifier = Modifier.fillMaxWidth())
            ActionButton("Anuluj", onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Pop-up for adding a station by hand: a name and the stream address (e.g. the one used in Radio Wilma 4.0). */
@Composable
private fun AddStationDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        cursorColor = p.accent,
        focusedBorderColor = p.accent,
        unfocusedBorderColor = p.frame,
    )
    val address = url.trim()
    val valid = name.isNotBlank() && (address.startsWith("http://") || address.startsWith("https://"))
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Background)
                .border(1.5.dp, p.frame, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("NOWA STACJA", color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Nazwa, np. RMF FM", color = p.label) },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Adres strumienia (http://...)", color = p.label) },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors,
            )
            ActionButton("Dodaj do ulubionych", { if (valid) onAdd(name.trim(), address) }, filled = true, modifier = Modifier.fillMaxWidth(), enabled = valid)
            ActionButton("Anuluj", onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
