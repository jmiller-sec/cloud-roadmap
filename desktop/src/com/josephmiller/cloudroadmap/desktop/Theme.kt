package com.josephmiller.cloudroadmap.desktop

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Same neon palette as the phone app. */
object Neon {
    val Bg = Color(0xFF060B12)
    val Panel = Color(0xFF0C1622)
    val Panel2 = Color(0xFF112131)
    val Line = Color(0xFF1C3146)
    val Cyan = Color(0xFF00E5FF)
    val Green = Color(0xFF39FF88)
    val Amber = Color(0xFFFFC14D)
    val Red = Color(0xFFFF5C7A)
    val Violet = Color(0xFFB388FF)
    val Teal = Color(0xFF2BD9C9)
    val Text = Color(0xFFE6F1FF)
    val Muted = Color(0xFF7A8DA3)
}

val Mono = FontFamily.Monospace

fun sectionColor(id: String) = when (id) {
    "sophia" -> Neon.Amber; "thm" -> Neon.Green; "pt" -> Neon.Teal; "warm" -> Neon.Violet; else -> Neon.Cyan
}

@Composable
fun Panel(modifier: Modifier = Modifier, glow: Color = Neon.Line, content: @Composable () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Neon.Panel2.copy(alpha = 0.92f), Neon.Panel.copy(alpha = 0.92f))))
            .border(1.dp, glow, RoundedCornerShape(18.dp))
    ) { content() }
}

@Composable
fun Label(text: String, color: Color = Neon.Cyan) =
    Text(text.uppercase(), color = color, fontSize = 11.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)

@Composable
fun Pill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, fontSize = 11.sp, fontFamily = Mono, fontWeight = FontWeight.SemiBold)
    }
}

/** Progress bar whose fill glides to its new value. */
@Composable
fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing))
    Box(modifier.height(6.dp).clip(RoundedCornerShape(50)).background(Neon.Line)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color))))
    }
}

/** Slow drifting aurora behind everything: subtle, never distracting. */
@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition()
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse))
    Canvas(modifier.fillMaxSize()) {
        drawRect(Neon.Bg)
        drawCircle(
            Brush.radialGradient(listOf(Neon.Cyan.copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width * (0.15f + 0.25f * a), size.height * 0.2f), radius = size.minDimension * 0.7f),
            radius = size.minDimension * 0.7f, center = Offset(size.width * (0.15f + 0.25f * a), size.height * 0.2f),
        )
        drawCircle(
            Brush.radialGradient(listOf(Neon.Violet.copy(alpha = 0.08f), Color.Transparent), center = Offset(size.width * (0.85f - 0.2f * a), size.height * 0.85f), radius = size.minDimension * 0.6f),
            radius = size.minDimension * 0.6f, center = Offset(size.width * (0.85f - 0.2f * a), size.height * 0.85f),
        )
    }
}

/** Big timer ring: sweeps smoothly, pulses its glow while running. */
@Composable
fun ProgressRing(fraction: Float, color: Color, running: Boolean, size: Dp = 168.dp, content: @Composable () -> Unit) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    val t = rememberInfiniteTransition()
    val pulse by t.animateFloat(0.25f, 0.7f, infiniteRepeatable(tween(1400), RepeatMode.Reverse))
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 12.dp.toPx()
            if (running) drawCircle(color.copy(alpha = pulse * 0.25f), radius = this.size.minDimension / 2)
            drawArc(Neon.Line, -90f, 360f, false, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(color.copy(alpha = 0.4f), color, color.copy(alpha = 0.4f))), -90f, 360f * f, false, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Round check button that pops when you tick it. */
@Composable
fun CheckBubble(done: Boolean, locked: Boolean, color: Color, size: Dp = 40.dp, onClick: () -> Unit) {
    val s by animateFloatAsState(if (done) 1f else 0.92f, spring(dampingRatio = 0.35f, stiffness = 500f))
    Box(
        Modifier.size(size).scale(s).clip(CircleShape)
            .background(if (done) color else color.copy(alpha = 0.08f))
            .border(1.5.dp, if (locked) Neon.Line else color, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (locked && !done) Icons.Filled.Lock else Icons.Filled.Check, "Done",
            tint = if (done) Neon.Bg else if (locked) Neon.Muted else color.copy(alpha = 0.6f), modifier = Modifier.size(size * 0.5f))
    }
}

/** One-shot confetti burst when a whole bundle is finished. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val anim = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { anim.snapTo(0f); anim.animateTo(1f, tween(1600, easing = LinearOutSlowInEasing)) }
    val parts = remember(trigger) {
        val rnd = Random(trigger)
        List(90) { Triple(rnd.nextFloat() * 360f, 0.35f + rnd.nextFloat() * 0.65f, listOf(Neon.Cyan, Neon.Green, Neon.Amber, Neon.Violet, Neon.Teal)[rnd.nextInt(5)]) }
    }
    val p = anim.value
    if (p >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val c = Offset(size.width / 2, size.height * 0.4f)
        parts.forEach { (ang, speed, col) ->
            val r = speed * p * size.minDimension * 0.55f
            val rad = Math.toRadians(ang.toDouble())
            val pos = Offset(c.x + (cos(rad) * r).toFloat(), c.y + (sin(rad) * r).toFloat() + p * p * 220f)
            drawCircle(col.copy(alpha = 1f - p), radius = 5f * (1f - p * 0.5f), center = pos)
        }
    }
}
