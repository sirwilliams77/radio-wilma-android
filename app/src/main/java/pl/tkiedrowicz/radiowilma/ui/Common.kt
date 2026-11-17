// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(subtitle.uppercase(), color = p.label, fontSize = 10.sp, letterSpacing = 3.sp)
    }
}

/** Pill-shaped filter / theme chip. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(if (selected) p.accent.copy(alpha = 0.14f) else Background)
            .border(1.dp, if (selected) p.accent else p.divider, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) p.accent else p.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Wide rectangular action button. */
@Composable
fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false, enabled: Boolean = true) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    val base = modifier
        .defaultMinSize(minHeight = 44.dp)
        .clip(shape)
    val styled = if (filled) base.background(if (enabled) p.accent else p.divider) else base.border(1.dp, if (enabled) p.frame else p.divider, shape)
    Box(
        modifier = (if (enabled) styled.clickable(role = Role.Button, onClick = onClick) else styled).padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) Color.Black else if (enabled) TextPrimary else p.label,
            fontSize = 13.sp,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** Circle with the station's logo (set by the user) or, when there is none, its initials. */
@Composable
fun StationAvatar(initials: String, active: Boolean, modifier: Modifier = Modifier, logo: ImageBitmap? = null) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PanelDark)
            .border(1.dp, p.divider, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            Image(bitmap = logo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(initials, color = if (active) p.accent else p.label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
