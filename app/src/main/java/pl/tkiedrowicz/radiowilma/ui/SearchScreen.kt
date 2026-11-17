// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement.spacedBy
import pl.tkiedrowicz.radiowilma.MainViewModel

@Composable
fun SearchScreen(vm: MainViewModel, onPlayed: () -> Unit) {
    val p = LocalPalette.current
    val ui by vm.search.collectAsState()
    val favorites by vm.favorites.collectAsState()

    // The first time the tab is opened it shows the most popular stations of the chosen country.
    LaunchedEffect(Unit) {
        if (!ui.searched && !ui.loading) vm.runSearch()
    }

    Column(Modifier.fillMaxSize().background(Background).padding(horizontal = 16.dp)) {
        ScreenTitle("SZUKAJ STACJI", "Katalog radio-browser.info", Modifier.padding(top = 14.dp, bottom = 10.dp))

        OutlinedTextField(
            value = ui.query,
            onValueChange = { vm.setQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Nazwa stacji, np. jazz", color = p.label) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = p.accent) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.runSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = p.accent,
                focusedBorderColor = p.accent,
                unfocusedBorderColor = p.frame,
            ),
        )

        // Filters: tapping a chip switches it to the next value.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), horizontalArrangement = spacedBy(8.dp)) {
            Chip(if (ui.country == "PL") "Polska" else "Cały świat", true, { vm.cycleCountry() })
            Chip(if (ui.tag.isEmpty()) "Gatunek" else ui.tag, ui.tag.isNotEmpty(), { vm.cycleTag() })
            Chip(if (ui.minBitrate == 0) "Jakość" else "Min. " + ui.minBitrate + " kbit/s", ui.minBitrate != 0, { vm.cycleBitrate() })
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = spacedBy(8.dp)) {
            ActionButton("Szukaj", { vm.runSearch() }, filled = true, modifier = Modifier.weight(1f), enabled = !ui.loading)
        }

        Text(
            text = if (ui.loading) "SZUKAM..." else "WYNIKI · " + ui.results.size,
            color = p.label, fontSize = 11.sp, letterSpacing = 3.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (ui.error != null) {
            Text(ui.error ?: "", color = Color(0xFFEF4444), fontSize = 14.sp, textAlign = TextAlign.Start)
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.results, key = { it.id }) { s ->
                val shape = RoundedCornerShape(14.dp)
                val inFavorites = favorites.any { it.id == s.id }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(1.5.dp, p.frame, shape)
                        .padding(12.dp),
                ) {
                    Row(horizontalArrangement = spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        StationAvatar(s.initials, false)
                        Column(Modifier.weight(1f)) {
                            Text(s.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(s.meta, color = p.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = spacedBy(8.dp)) {
                        ActionButton("Graj", {
                            vm.play(s)
                            onPlayed()
                        }, filled = true, modifier = Modifier.weight(1f))
                        ActionButton(
                            if (inFavorites) "W ulubionych" else "Dodaj do ulubionych",
                            { vm.addFavorite(s) },
                            modifier = Modifier.weight(1f),
                            enabled = !inFavorites,
                        )
                    }
                }
            }
        }
    }
}
