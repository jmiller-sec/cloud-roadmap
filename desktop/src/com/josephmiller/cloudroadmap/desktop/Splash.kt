package com.josephmiller.cloudroadmap.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Opening cartoon: a fox SOC analyst types fast at a laptop, logs in, "ACCESS GRANTED", then "Welcome, Cyber Fox".
 * Everything is drawn from [tMs] (0..SPLASH_MS) on a 400x470 design grid, so the same file runs on phone, Windows and Mac.
 */
/** Story speed: 1.4x slower than the first cut (5 seconds total). The paws still type at full speed. */
private const val SLOW = 1.4f
const val SPLASH_MS = 5000f
const val SPLASH_SHORT_FROM = 2050f * SLOW

private val Bg = Color(0xFF04070A)
private val Green = Color(0xFF39FF88)
private val Cyan = Color(0xFF35E0FF)
private val Fox = Color(0xFFFF7A1A)
private val FoxDark = Color(0xFFD95F0E)
private val Cream = Color(0xFFFFF1E0)
private val Ink = Color(0xFF0E1218)
private val Hood = Color(0xFF151D2C)
private val HoodLine = Color(0xFF26344D)

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = x * x * (3 - 2 * x)

/** Plays the opening over the app. [short] = already seen today, so only the "ACCESS GRANTED → Welcome" part. Tap to skip. */
@Composable
fun SplashGate(short: Boolean, onDone: () -> Unit) {
    val from = if (short) SPLASH_SHORT_FROM else 0f
    var t by remember { mutableFloatStateOf(from) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (t < SPLASH_MS) { val now = withFrameNanos { it }; t = from + (now - start) / 1_000_000f }
        onDone()
    }
    CyberFoxSplash(t, Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { onDone() })
}

@Composable
fun CyberFoxSplash(tMs: Float, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        drawRect(Bg)
        val s = min(size.width / 400f, size.height / 470f)
        val ox = (size.width - 400f * s) / 2f
        val oy = (size.height - 470f * s) / 2f
        codeRain(tMs * 0.6f)
        withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) { scene(tMs / SLOW, tMs, tm, s) }
        // fade in from black, fade out into the app
        val veil = seg(tMs, SPLASH_MS - 260f, SPLASH_MS) + (1f - seg(tMs, 0f, 220f))
        if (veil > 0f) drawRect(Bg.copy(alpha = veil.coerceIn(0f, 1f)))
    }
}

/**
 * The app icon: the Cyber Fox's face, eyes lit, in his hood. [background] false = transparent with extra margin
 * (Android adaptive foreground, which the launcher crops to its own shape).
 */
@Composable
fun FoxIconArt(background: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        if (background) {
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0B141B), Bg)), size = size, cornerRadius = CornerRadius(w * 0.22f))
            drawCircle(Brush.radialGradient(listOf(Green.copy(alpha = 0.38f), Color.Transparent), center, w * 0.55f), w * 0.55f, center)
            drawRoundRect(Green.copy(alpha = 0.55f), Offset(w * 0.03f, w * 0.03f), Size(w * 0.94f, w * 0.94f), CornerRadius(w * 0.19f), style = Stroke(w * 0.022f))
        } else drawCircle(Brush.radialGradient(listOf(Green.copy(alpha = 0.34f), Color.Transparent), center, w * 0.36f), w * 0.36f, center)
        // Frame the head: design-grid box x 88..312, y 96..320 (above the laptop).
        val fill = if (background) 0.86f else 0.56f
        val k = w * fill / 224f
        clipRect(0f, 0f, size.width, if (background) w * 0.93f else w * 0.5f + 112f * k) {
            withTransform({ translate(w / 2f - 200f * k, w / 2f - 208f * k); scale(k, k, Offset.Zero) }) {
                fox(2600f, 2600f, typing = false, granted = 1f, welcome = 1f, head = true)
            }
        }
    }
}

/** Faint falling code across the whole window. */
private fun DrawScope.codeRain(t: Float) {
    val cols = (size.width / 26f).toInt().coerceAtLeast(8)
    val cw = size.width / cols
    for (i in 0 until cols) {
        val speed = 0.10f + ((i * 37) % 11) / 60f
        val head = ((t * speed + i * 173f) % (size.height + 260f)) - 130f
        for (k in 0 until 9) {
            val y = head - k * 15f
            if (y < -10f || y > size.height) continue
            val on = ((i * 7 + k * 13 + (t / 90f).toInt()) % 5) != 0
            if (!on) continue
            drawRect(Green.copy(alpha = (0.20f - k * 0.02f).coerceAtLeast(0.02f)), Offset(i * cw + cw * 0.3f, y), Size(cw * 0.34f, 9f))
        }
    }
}

private fun DrawScope.scene(t: Float, raw: Float, tm: TextMeasurer, s: Float) {
    val granted = seg(t, 2050f, 2250f)
    val typing = t in 200f..1950f
    val welcome = ease(seg(t, 2400f, 2800f))

    // ---------- terminal ----------
    val termA = 1f - ease(seg(t, 2350f, 2600f))
    if (termA > 0f) {
        drawRoundRect(Color(0xFF070C11).copy(alpha = 0.94f * termA), Offset(36f, 14f), Size(328f, 122f), CornerRadius(10f))
        drawRoundRect(Green.copy(alpha = (0.35f + 0.45f * granted) * termA), Offset(36f, 14f), Size(328f, 122f), CornerRadius(10f), style = Stroke(1.4f))
        listOf(Color(0xFFFF5F57), Color(0xFFFEBC2E), Color(0xFF28C840)).forEachIndexed { i, c -> drawCircle(c.copy(alpha = termA), 3.2f, Offset(50f + i * 11f, 26f)) }
        val lines = listOf(
            Triple("$ ssh soc@cloud-roadmap", 220f, 760f),
            Triple("user: cyberfox", 820f, 1150f),
            Triple("pass: ************", 1210f, 1560f),
            Triple("verifying badge...", 1620f, 1950f),
        )
        lines.forEachIndexed { i, (txt, a, b) ->
            val n = (txt.length * seg(t, a, b)).toInt()
            if (n > 0) text(tm, txt.take(n) + if (t in a..b && (raw / 90f).toInt() % 2 == 0) "▌" else "", 50f, 38f + i * 17f, 11.5f, Green.copy(alpha = termA), s)
        }
        if (granted > 0f) {
            val flash = 0.65f + 0.35f * sin(t / 45f)
            drawRoundRect(Green.copy(alpha = 0.16f * flash * termA), Offset(46f, 104f), Size(308f, 24f), CornerRadius(5f))
            text(tm, "ACCESS GRANTED", 200f, 108f, 15f, Green.copy(alpha = termA), s, bold = true, center = true)
        }
    }

    // ---------- welcome ----------
    if (welcome > 0f) {
        val lift = (1f - welcome) * 14f
        text(tm, "Welcome,", 200f, 30f + lift, 17f, Cyan.copy(alpha = welcome), s, center = true)
        text(tm, "CYBER FOX", 200f, 52f + lift, 40f, Fox.copy(alpha = welcome), s, bold = true, center = true)
        drawLine(Green.copy(alpha = welcome), Offset(200f - 90f * welcome, 108f), Offset(200f + 90f * welcome, 108f), 2f, StrokeCap.Round)
        text(tm, "SOC analyst in training", 200f, 114f, 11f, Green.copy(alpha = welcome * 0.9f), s, center = true)
    }

    withTransform({ translate(0f, 58f) }) { fox(t, raw, typing, granted, welcome) }
}

private fun DrawScope.fox(t: Float, raw: Float, typing: Boolean, granted: Float, welcome: Float, head: Boolean = false) {
    val bob = sin(t / 260f) * 1.5f
    // tail
    val swing = sin(t / 300f) * 7f + granted * 10f
    if (!head) rotate(swing, Offset(282f, 388f)) {
        val tail = Path().apply {
            moveTo(272f, 392f); cubicTo(318f, 388f, 352f, 352f, 346f, 300f); cubicTo(372f, 340f, 362f, 396f, 300f, 404f); close()
        }
        drawPath(tail, Fox)
        val tip = Path().apply { moveTo(346f, 300f); cubicTo(352f, 316f, 354f, 330f, 350f, 342f); cubicTo(360f, 330f, 360f, 314f, 346f, 300f); close() }
        drawPath(tip, Cream)
    }
    // hoodie body
    val body = Path().apply {
        moveTo(98f, 404f); cubicTo(100f, 330f, 140f, 296f, 200f, 296f); cubicTo(260f, 296f, 300f, 330f, 302f, 404f); close()
    }
    drawPath(body, Hood); drawPath(body, HoodLine, style = Stroke(2f))

    withTransform({ translate(0f, bob) }) {
        // hood behind the head
        drawOval(Hood, Offset(104f, 138f), Size(192f, 176f)); drawOval(HoodLine, Offset(104f, 138f), Size(192f, 176f), style = Stroke(2f))
        // ears
        for (m in listOf(-1f, 1f)) {
            val ear = Path().apply { moveTo(200f + m * 72f, 178f); lineTo(200f + m * 58f, 104f); lineTo(200f + m * 18f, 160f); close() }
            drawPath(ear, Fox)
            val inner = Path().apply { moveTo(200f + m * 62f, 168f); lineTo(200f + m * 54f, 124f); lineTo(200f + m * 32f, 160f); close() }
            drawPath(inner, Color(0xFF2B1608))
        }
        // head
        val head = Path().apply {
            moveTo(200f, 296f); quadraticBezierTo(138f, 286f, 112f, 222f); quadraticBezierTo(122f, 160f, 200f, 152f)
            quadraticBezierTo(278f, 160f, 288f, 222f); quadraticBezierTo(262f, 286f, 200f, 296f); close()
        }
        drawPath(head, Fox)
        // forehead shade
        val brow = Path().apply { moveTo(150f, 170f); quadraticBezierTo(200f, 150f, 250f, 170f); quadraticBezierTo(200f, 186f, 150f, 170f); close() }
        drawPath(brow, FoxDark.copy(alpha = 0.55f))
        // cream cheeks
        for (m in listOf(-1f, 1f)) {
            val cheek = Path().apply {
                moveTo(200f, 296f); quadraticBezierTo(200f + m * 60f, 286f, 200f + m * 87f, 224f)
                quadraticBezierTo(200f + m * 44f, 238f, 200f, 246f); close()
            }
            drawPath(cheek, Cream)
        }
        // screen light on the face
        val glow = if (typing) 0.20f + 0.08f * sin(t / 50f) else 0.16f + 0.2f * granted
        drawOval(Brush.radialGradient(listOf(Green.copy(alpha = glow), Color.Transparent), Offset(200f, 300f), 120f), Offset(90f, 200f), Size(220f, 130f))
        // eyes
        val blink = if (abs((t % 1700f) - 900f) < 55f && granted == 0f) 0.15f else 1f
        for (m in listOf(-1f, 1f)) {
            val c = Offset(200f + m * 36f, 214f)
            val ry = 11f * blink * (if (typing) 0.78f else 1f)
            drawOval(Ink, Offset(c.x - 13f, c.y - ry), Size(26f, ry * 2))
            if (blink > 0.5f) {
                if (granted > 0f) {
                    drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.85f * granted), Color.Transparent), c, 26f), 26f, c)
                    drawCircle(Cyan.copy(alpha = granted), 5.5f, c)
                } else {
                    // code reflected in the eyes, scanning left to right
                    val sx = ((t / 3.2f) % 16f) - 8f
                    drawRect(Green.copy(alpha = 0.9f), Offset(c.x + sx - 3f, c.y - 3.5f), Size(6f, 2f))
                    drawRect(Green.copy(alpha = 0.6f), Offset(c.x - sx * 0.6f - 2f, c.y + 1.5f), Size(4f, 2f))
                }
                drawCircle(Color.White.copy(alpha = 0.9f), 2.2f, Offset(c.x + 5f, c.y - 4.5f))
            }
            // focused brow while typing, relaxed after
            val tilt = if (typing) 6f else -2f * welcome
            drawLine(Ink, Offset(c.x - m * 15f, c.y - 19f - tilt * 0.3f), Offset(c.x + m * 11f, c.y - 17f + tilt), 3.2f, StrokeCap.Round)
        }
        // nose + mouth
        val nose = Path().apply { moveTo(190f, 258f); lineTo(210f, 258f); quadraticBezierTo(200f, 273f, 190f, 258f); close() }
        drawPath(nose, Ink)
        val smile = 2f + 7f * welcome
        val mouth = Path().apply { moveTo(184f, 276f); quadraticBezierTo(200f, 276f + smile, 216f, 276f) }
        drawPath(mouth, Ink, style = Stroke(2.6f, cap = StrokeCap.Round))
        // hoodie strings
        drawLine(Fox, Offset(184f, 300f), Offset(182f, 322f), 2.6f, StrokeCap.Round)
        drawLine(Fox, Offset(216f, 300f), Offset(218f, 322f), 2.6f, StrokeCap.Round)
    }

    if (head) return
    // laptop (lid facing us)
    drawRoundRect(Color(0xFF1B2433), Offset(98f, 322f), Size(204f, 84f), CornerRadius(9f))
    drawRoundRect(Color(0xFF34455F), Offset(98f, 322f), Size(204f, 84f), CornerRadius(9f), style = Stroke(2f))
    val pulse = 0.55f + 0.45f * sin(t / 180f)
    val logo = if (granted > 0f) Green else Cyan
    drawCircle(Brush.radialGradient(listOf(logo.copy(alpha = 0.5f * pulse), Color.Transparent), Offset(200f, 364f), 34f), 34f, Offset(200f, 364f))
    // shield logo
    val shield = Path().apply {
        moveTo(200f, 348f); lineTo(214f, 353f); lineTo(214f, 365f); quadraticBezierTo(213f, 376f, 200f, 381f)
        quadraticBezierTo(187f, 376f, 186f, 365f); lineTo(186f, 353f); close()
    }
    drawPath(shield, logo.copy(alpha = 0.9f), style = Stroke(2.4f))
    if (granted > 0f) {
        val ck = Path().apply { moveTo(193f, 364f); lineTo(198.5f, 370f); lineTo(208f, 358f) }
        drawPath(ck, Green.copy(alpha = granted), style = Stroke(2.6f, cap = StrokeCap.Round))
    }
    // screen light spilling over the top edge
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Green.copy(alpha = 0.22f)), 300f, 322f), Offset(104f, 300f), Size(192f, 22f))

    // paws: a blur of typing, then one fist pump
    for ((i, m) in listOf(-1f, 1f).withIndex()) {
        val jitter = if (typing) sin(raw / (17f + i * 6f) + i * 2f) * 5.5f else 0f
        val pump = if (m > 0f) ease(seg(t, 2250f, 2550f)) * 46f else 0f
        val px = 200f + m * 78f
        val py = 320f + jitter - pump
        if (typing) for (g in 1..2) drawOval(Fox.copy(alpha = 0.22f), Offset(px - 15f, py - 10f - g * 4f * sin(raw / 23f + g)), Size(30f, 19f))
        drawOval(Fox, Offset(px - 15f, py - 10f), Size(30f, 20f))
        drawOval(Cream, Offset(px - 9f, py - 8f), Size(18f, 9f))
        if (typing && (raw / 70f).toInt() % 2 == i) {
            // key sparks
            for (k in 0..2) drawCircle(Green.copy(alpha = 0.8f), 1.6f, Offset(px - 12f + k * 12f + sin(raw / 30f + k) * 4f, py - 16f - k * 3f))
        }
    }
}

private fun DrawScope.text(tm: TextMeasurer, s: String, x: Float, y: Float, sizeU: Float, color: Color, scale: Float, bold: Boolean = false, center: Boolean = false) {
    // sizes are in design units; the canvas is already scaled, so undo the device font scale only
    val style = TextStyle(color = color, fontSize = sizeU.toSp(), fontFamily = FontFamily.Monospace, fontWeight = if (bold) FontWeight.Black else FontWeight.Medium)
    val l = tm.measure(s, style, softWrap = false)
    drawText(l, topLeft = Offset(if (center) x - l.size.width / 2f else x, y))
}
