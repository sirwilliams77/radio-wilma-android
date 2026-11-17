// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import pl.tkiedrowicz.radiowilma.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.tkiedrowicz.radiowilma.BuildConfig
import pl.tkiedrowicz.radiowilma.MainViewModel

enum class SettingsPage { LOOK, WEATHER, SLEEP, ABOUT }

@Composable
fun SettingsScreen(vm: MainViewModel, page: SettingsPage?, onExit: () -> Unit, onPage: (SettingsPage?) -> Unit) {
    when (page) {
        null -> SettingsHome(onPage, onExit)
        SettingsPage.LOOK -> SubScreen("WYGLĄD", "Motywy kolorów", onBack = { onPage(null) }) { LookContent(vm) }
        SettingsPage.WEATHER -> SubScreen("POGODA", "Miejscowość", onBack = { onPage(null) }) { WeatherContent(vm) }
        SettingsPage.SLEEP -> SubScreen("WYŁĄCZNIK CZASOWY", "Zasypianie przy radiu", onBack = { onPage(null) }) { SleepContent(vm) }
        SettingsPage.ABOUT -> SubScreen("O APLIKACJI", "Radio Wilma", onBack = { onPage(null) }) { AboutContent() }
    }
}

@Composable
private fun SettingsHome(onPage: (SettingsPage?) -> Unit, onExit: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(Background).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        ScreenTitle("USTAWIENIA", "Radio Wilma", Modifier.padding(top = 14.dp, bottom = 12.dp))
        val tiles = listOf(
            Triple(SettingsPage.LOOK, "Wygląd", "Motywy kolorów"),
            Triple(SettingsPage.WEATHER, "Pogoda", "Miejscowość na ekranie"),
            Triple(SettingsPage.SLEEP, "Wyłącznik czasowy", "Zasypianie przy radiu"),
            Triple(SettingsPage.ABOUT, "O aplikacji", "Wersja i licencje"),
        )
        for (t in tiles) {
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.5.dp, p.frame, shape)
                    .clickable(role = Role.Button) { onPage(t.first) }
                    .padding(16.dp),
            ) {
                Text(t.second, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(t.third, color = p.label, fontSize = 13.sp)
            }
        }
        ActionButton("Wyjdź z aplikacji (zatrzymaj radio)", onExit, filled = true, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Text(
            "Planowane w kolejnych wersjach: sterowanie radiem Wilma przez sieć, budzik, import i eksport listy stacji.",
            color = p.label, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun SubScreen(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(Background).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("Wstecz", onBack, size = 44.dp) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
            }
            ScreenTitle(title, subtitle, Modifier.padding(start = 6.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Box(Modifier.padding(top = 14.dp)) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookContent(vm: MainViewModel) {
    val p = LocalPalette.current
    val selected by vm.themeName.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // preview of the selected theme
        Tile(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("12:47", color = p.clock, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("STACJA RADIOWA", color = p.label, fontSize = 10.sp, letterSpacing = 3.sp)
                Text("Nazwa stacji", color = p.station, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Wykonawca", color = p.artist, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Tytuł utworu", color = p.title, fontSize = 14.sp)
                Row(Modifier.padding(top = 8.dp).height(26.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    val hs = listOf(10, 18, 26, 14, 24, 16, 8, 20)
                    for (h in hs) {
                        val color = when {
                            h < 14 -> p.eqLow
                            h < 22 -> p.eqMid
                            else -> p.eqHigh
                        }
                        Box(Modifier.width(18.dp).height(h.dp).background(color))
                    }
                }
            }
        }
        Text("GOTOWE MOTYWY", color = p.label, fontSize = 11.sp, letterSpacing = 3.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (t in Themes) {
                Chip(t.name, t.name == selected, { vm.setTheme(t.name) })
            }
        }
        Text(
            "Motyw zmienia kolory całej aplikacji. To te same motywy, które są w panelu WWW radia Wilma.",
            color = p.label, fontSize = 12.sp,
        )
    }
}

@Composable
private fun WeatherContent(vm: MainViewModel) {
    val p = LocalPalette.current
    val ui by vm.weather.collectAsState()
    var city by remember { mutableStateOf(vm.weatherQuery()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Miejscowość", color = p.label) },
            placeholder = { Text("np. Gdańsk", color = p.label) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.setWeatherPlace(city) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = p.accent,
                focusedBorderColor = p.accent,
                unfocusedBorderColor = p.frame,
                focusedLabelColor = p.accent,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Zapisz", { vm.setWeatherPlace(city) }, filled = true, enabled = !ui.busy)
            ActionButton("Wyłącz pogodę", {
                city = ""
                vm.setWeatherPlace("")
            })
        }
        val info = ui.info
        if (info != null) {
            Tile(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WeatherIcon(info.code, info.isDay, Modifier.size(64.dp))
                    Column {
                        Text(info.temperature.toString() + "°", color = p.clock, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text(info.city, color = TextCity, fontSize = 15.sp)
                    }
                }
            }
        }
        if (ui.message.isNotEmpty()) Text(ui.message, color = p.label, fontSize = 13.sp)
        Text(
            "Dane pogodowe: Open-Meteo.com (licencja CC BY 4.0). Pogoda odświeża się co 20 minut.",
            color = p.label, fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepContent(vm: MainViewModel) {
    val p = LocalPalette.current
    val left by vm.sleepLeft.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val remaining = left
        if (remaining != null) {
            val m = remaining / 60
            val s = remaining % 60
            Text("Radio wyłączy się za " + m + ":" + s.toString().padStart(2, '0'), color = p.accent, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            ActionButton("Anuluj wyłącznik", { vm.cancelSleepTimer() })
        } else {
            Text("Po wybranym czasie odtwarzanie zostanie wstrzymane.", color = p.label, fontSize = 13.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (min in listOf(15, 30, 45, 60, 90, 120)) {
                Chip(min.toString() + " min", false, { vm.startSleepTimer(min) })
            }
        }
    }
}

@Composable
private fun AboutContent() {
    val p = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(
            painter = painterResource(R.drawable.logo_radio_wilma),
            contentDescription = "Radio Wilma",
            modifier = Modifier.size(110.dp).clip(RoundedCornerShape(20.dp)),
        )
        Text("Radio Wilma na Androida", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Wersja " + BuildConfig.VERSION_NAME, color = p.label, fontSize = 13.sp)
        Text(
            "Autor i właściciel projektu: Sir Williams, © 2026. Program jest wolnym oprogramowaniem na licencji GNU GPL v3 (pełny tekst w pliku LICENSE w kodzie źródłowym). Oprogramowanie jest dostarczane bez gwarancji.",
            color = TextPrimary, fontSize = 14.sp,
        )
        Text(
            "Kod, dokumentacja i makieta powstały z pomocą asystenta AI (Claude, Anthropic); całość została zaprojektowana, przetestowana i jest wydawana przez autora.",
            color = TextPrimary, fontSize = 14.sp,
        )
        Text("ELEMENTY ZEWNĘTRZNE", color = p.label, fontSize = 11.sp, letterSpacing = 3.sp, modifier = Modifier.padding(top = 6.dp))
        for (line in listOf(
            "AndroidX Media3, Jetpack Compose, Kotlin: Apache-2.0",
            "OkHttp: Apache-2.0",
            "Katalog stacji: radio-browser.info (dane społecznościowe, bez gwarancji; logo i nazwy stacji należą do nadawców)",
            "Dane pogodowe: Open-Meteo.com, CC BY 4.0",
        )) {
            Text("• " + line, color = p.label, fontSize = 13.sp)
        }
    }
}
