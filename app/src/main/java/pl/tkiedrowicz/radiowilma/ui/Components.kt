// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Rounded frame like the tiles on the Radio Wilma screen. */
@Composable
fun Tile(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, p.frame, shape),
        content = content,
    )
}

/** Round button with a drawn icon. */
@Composable
fun RoundButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fill: Color? = null,
    border: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var m = modifier
        .size(size)
        .clip(CircleShape)
    if (fill != null) m = m.background(fill)
    if (border != null) m = m.border(1.5.dp, border, CircleShape)
    Box(
        modifier = m
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// ---------- drawn icons (no icon library needed) ----------

@Composable
fun PowerIcon(color: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.17f),
            size = Size(w * 0.70f, h * 0.70f),
            style = stroke,
        )
        drawLine(color, Offset(w * 0.5f, h * 0.06f), Offset(w * 0.5f, h * 0.52f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
    }
}

@Composable
fun PauseIcon(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRect(color, Offset(w * 0.20f, h * 0.12f), Size(w * 0.22f, h * 0.76f))
        drawRect(color, Offset(w * 0.58f, h * 0.12f), Size(w * 0.22f, h * 0.76f))
    }
}

@Composable
fun PlayIcon(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.26f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.5f)
            lineTo(w * 0.26f, h * 0.88f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
fun SkipIcon(color: Color, forward: Boolean, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val tri = Path()
        if (forward) {
            tri.moveTo(w * 0.12f, h * 0.18f); tri.lineTo(w * 0.70f, h * 0.5f); tri.lineTo(w * 0.12f, h * 0.82f)
            drawRect(color, Offset(w * 0.74f, h * 0.18f), Size(w * 0.14f, h * 0.64f))
        } else {
            tri.moveTo(w * 0.88f, h * 0.18f); tri.lineTo(w * 0.30f, h * 0.5f); tri.lineTo(w * 0.88f, h * 0.82f)
            drawRect(color, Offset(w * 0.12f, h * 0.18f), Size(w * 0.14f, h * 0.64f))
        }
        tri.close()
        drawPath(tri, color)
    }
}

@Composable
fun SpeakerIcon(color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val body = Path().apply {
            moveTo(w * 0.10f, h * 0.38f); lineTo(w * 0.32f, h * 0.38f); lineTo(w * 0.55f, h * 0.18f)
            lineTo(w * 0.55f, h * 0.82f); lineTo(w * 0.32f, h * 0.62f); lineTo(w * 0.10f, h * 0.62f); close()
        }
        drawPath(body, color)
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        drawArc(color, -40f, 80f, false, Offset(w * 0.52f, h * 0.30f), Size(w * 0.30f, h * 0.40f), style = stroke)
    }
}

// ---------- vinyl and equalizer ----------

@Composable
fun Vinyl(playing: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "vinyl")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Canvas(modifier.rotate(if (playing) angle else 0f)) {
        val r = size.minDimension / 2f
        val c = center
        drawCircle(Color(0xFF07070A), r, c)
        drawCircle(Color(0xFF2A2A1A), r, c, style = Stroke(1.5f))
        for (k in listOf(0.88f, 0.76f, 0.64f)) {
            drawCircle(Color(0xFF16161C), r * k, c, style = Stroke(1.5f))
        }
        // two lighter sectors give a visible spin
        drawArc(Color(0x33FFFFFF), -150f, 40f, false, Offset(c.x - r * 0.92f, c.y - r * 0.92f), Size(r * 1.84f, r * 1.84f), style = Stroke(r * 0.12f))
        drawArc(Color(0x33FFFFFF), 30f, 40f, false, Offset(c.x - r * 0.92f, c.y - r * 0.92f), Size(r * 1.84f, r * 1.84f), style = Stroke(r * 0.12f))
        drawCircle(p.accent, r * 0.38f, c)
        drawCircle(Color.Black, r * 0.07f, c)
    }
}

/** Decorative spectrum: Android gives no spectrum of a stream without microphone permission, so the bars are animated. */
@Composable
fun Equalizer(playing: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val bars = remember { mutableStateListOf<Float>().apply { repeat(16) { add(0.08f) } } }
    LaunchedEffect(playing) {
        while (true) {
            for (i in bars.indices) {
                val target = if (playing) 0.10f + Random.nextFloat() * 0.90f else 0.05f
                bars[i] = bars[i] * 0.45f + target * 0.55f
            }
            delay(110)
        }
    }
    Canvas(modifier) {
        val count = bars.size
        val gap = 3.dp.toPx()
        val barW = (size.width - gap * (count - 1)) / count
        val seg = 5.dp.toPx()
        val segGap = 2.dp.toPx()
        val maxSegs = (size.height / (seg + segGap)).toInt().coerceAtLeast(1)
        for (i in 0 until count) {
            val on = (bars[i] * maxSegs).toInt().coerceIn(0, maxSegs)
            for (s in 0 until on) {
                val f = (s + 1f) / maxSegs
                val color = when {
                    f < 0.5f -> p.eqLow
                    f < 0.78f -> p.eqMid
                    else -> p.eqHigh
                }
                val y = size.height - (s + 1) * (seg + segGap) + segGap
                drawRect(color, Offset(i * (barW + gap), y), Size(barW, seg))
            }
        }
    }
}

// ---------- weather icons ----------

enum class WeatherKind { CLEAR, PARTLY, CLOUDY, FOG, RAIN, SNOW, STORM }

fun weatherKind(code: Int): WeatherKind = when (code) {
    0 -> WeatherKind.CLEAR
    1, 2 -> WeatherKind.PARTLY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    in 51..67, in 80..82 -> WeatherKind.RAIN
    in 71..77, 85, 86 -> WeatherKind.SNOW
    in 95..99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

private val CloudColor = Color(0xFFB7C6FF)
private val SunColor = Color(0xFFF6D36B)

private fun DrawScope.drawCloud(left: Float, top: Float, w: Float, color: Color) {
    val h = w * 0.55f
    drawCircle(color, h * 0.42f, Offset(left + w * 0.28f, top + h * 0.62f))
    drawCircle(color, h * 0.55f, Offset(left + w * 0.52f, top + h * 0.42f))
    drawCircle(color, h * 0.40f, Offset(left + w * 0.76f, top + h * 0.64f))
    drawRect(color, Offset(left + w * 0.28f, top + h * 0.62f), Size(w * 0.48f, h * 0.42f))
}

@Composable
fun WeatherIcon(code: Int, isDay: Boolean, modifier: Modifier = Modifier) {
    val kind = weatherKind(code)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sunCenter = Offset(w * 0.38f, h * 0.36f)
        fun celestial(cx: Float, cy: Float, r: Float) {
            if (isDay) {
                drawCircle(SunColor, r, Offset(cx, cy))
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    val s = Offset(cx + (r * 1.35f * Math.cos(a)).toFloat(), cy + (r * 1.35f * Math.sin(a)).toFloat())
                    val e = Offset(cx + (r * 1.75f * Math.cos(a)).toFloat(), cy + (r * 1.75f * Math.sin(a)).toFloat())
                    drawLine(SunColor, s, e, strokeWidth = r * 0.22f, cap = StrokeCap.Round)
                }
            } else {
                drawCircle(SunColor, r, Offset(cx, cy))
                drawCircle(Color.Black, r * 0.85f, Offset(cx + r * 0.55f, cy - r * 0.25f))
            }
        }
        when (kind) {
            WeatherKind.CLEAR -> celestial(w * 0.5f, h * 0.5f, h * 0.22f)
            WeatherKind.PARTLY -> {
                celestial(sunCenter.x, sunCenter.y, h * 0.17f)
                drawCloud(w * 0.20f, h * 0.38f, w * 0.64f, CloudColor)
            }
            WeatherKind.CLOUDY -> drawCloud(w * 0.15f, h * 0.25f, w * 0.70f, CloudColor)
            WeatherKind.FOG -> {
                for (k in 0 until 4) {
                    val y = h * (0.28f + k * 0.15f)
                    drawLine(CloudColor, Offset(w * 0.2f, y), Offset(w * 0.8f, y), strokeWidth = h * 0.07f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.RAIN -> {
                drawCloud(w * 0.15f, h * 0.10f, w * 0.70f, CloudColor)
                for (k in 0 until 3) {
                    val x = w * (0.34f + k * 0.16f)
                    drawLine(Color(0xFF59B4FF), Offset(x, h * 0.66f), Offset(x - w * 0.04f, h * 0.84f), strokeWidth = h * 0.06f, cap = StrokeCap.Round)
                }
            }
            WeatherKind.SNOW -> {
                drawCloud(w * 0.15f, h * 0.10f, w * 0.70f, CloudColor)
                for (k in 0 until 3) {
                    drawCircle(Color.White, h * 0.04f, Offset(w * (0.34f + k * 0.16f), h * (0.72f + (k % 2) * 0.1f)))
                }
            }
            WeatherKind.STORM -> {
                drawCloud(w * 0.15f, h * 0.08f, w * 0.70f, Color(0xFF9AA3C8))
                val bolt = Path().apply {
                    moveTo(w * 0.52f, h * 0.58f); lineTo(w * 0.42f, h * 0.76f); lineTo(w * 0.50f, h * 0.76f)
                    lineTo(w * 0.44f, h * 0.92f); lineTo(w * 0.62f, h * 0.70f); lineTo(w * 0.53f, h * 0.70f); close()
                }
                drawPath(bolt, SunColor)
            }
        }
    }
}
