// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.tkiedrowicz.radiowilma.ui.Background
import pl.tkiedrowicz.radiowilma.ui.LocalPalette
import pl.tkiedrowicz.radiowilma.ui.PlayerScreen
import pl.tkiedrowicz.radiowilma.ui.SearchScreen
import pl.tkiedrowicz.radiowilma.ui.SettingsPage
import pl.tkiedrowicz.radiowilma.ui.SettingsScreen
import pl.tkiedrowicz.radiowilma.ui.StationsScreen
import pl.tkiedrowicz.radiowilma.ui.paletteByName

private enum class Tab(val label: String, val icon: ImageVector) {
    PLAYER("Gra teraz", Icons.Filled.PlayArrow),
    STATIONS("Stacje", Icons.Filled.Menu),
    SEARCH("Szukaj", Icons.Filled.Search),
    SETTINGS("Ustawienia", Icons.Filled.Settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForNotifications()
        setContent { WilmaApp() }
    }

    /** From Android 13 the notification with the playback controls needs a permission. */
    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}

@Composable
private fun WilmaApp(vm: MainViewModel = viewModel()) {
    val themeName by vm.themeName.collectAsState()
    val palette = paletteByName(themeName)
    var tab by rememberSaveable { mutableStateOf(Tab.PLAYER) }
    var settingsPage by rememberSaveable { mutableStateOf<SettingsPage?>(null) }

    BackHandler(enabled = tab == Tab.SETTINGS && settingsPage != null) { settingsPage = null }
    BackHandler(enabled = tab != Tab.PLAYER && !(tab == Tab.SETTINGS && settingsPage != null)) { tab = Tab.PLAYER }

    val activity = LocalContext.current as? android.app.Activity
    val exitApp = {
        vm.exitApp()
        activity?.finishAndRemoveTask()
        Unit
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = darkColorScheme(primary = palette.accent, background = Background, surface = Background)) {
            val onSelect: (Tab) -> Unit = {
                if (it == Tab.SETTINGS && tab == Tab.SETTINGS) settingsPage = null
                tab = it
            }
            val screen: @Composable () -> Unit = {
                when (tab) {
                    Tab.PLAYER -> PlayerScreen(vm, onExit = exitApp)
                    Tab.STATIONS -> StationsScreen(vm, onSearch = { tab = Tab.SEARCH }, onPlayed = { tab = Tab.PLAYER })
                    Tab.SEARCH -> SearchScreen(vm, onPlayed = { tab = Tab.PLAYER })
                    Tab.SETTINGS -> SettingsScreen(vm, settingsPage, onExit = exitApp) { settingsPage = it }
                }
            }
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape: the navigation sits on the left side, the screen gets the whole height.
                Row(Modifier.fillMaxSize().background(Background)) {
                    NavRail(selected = tab, onSelect = onSelect)
                    Box(Modifier.weight(1f).fillMaxHeight()) { screen() }
                }
            } else {
                Column(Modifier.fillMaxSize().background(Background)) {
                    Box(Modifier.weight(1f)) { screen() }
                    NavBar(selected = tab, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun NavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().background(Background)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.frame))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
            for (t in Tab.values()) {
                val active = t == selected
                val color = if (active) p.accent else p.label
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(role = Role.Tab) { onSelect(t) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(t.icon, contentDescription = null, tint = color)
                    Text(t.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun NavRail(selected: Tab, onSelect: (Tab) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxHeight().background(Background)) {
        Column(
            Modifier.width(76.dp).fillMaxHeight().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (t in Tab.values()) {
                val active = t == selected
                val color = if (active) p.accent else p.label
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(role = Role.Tab) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, contentDescription = null, tint = color)
                    Text(t.label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(p.frame))
    }
}
