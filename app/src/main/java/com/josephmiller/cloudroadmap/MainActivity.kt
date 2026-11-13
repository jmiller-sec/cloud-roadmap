package com.josephmiller.cloudroadmap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.scale
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import java.time.format.DateTimeFormatter

// ---------- palette ----------
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
    val Text = Color(0xFFE6F1FF)
    val Muted = Color(0xFF7A8DA3)
}

private val Mono = FontFamily.Monospace

class MainActivity : ComponentActivity() {
    private val version = mutableIntStateOf(0)
    private val startTab = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Sync.ensureChannel(this)
        Sync.schedule(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        startTab.intValue = intent?.getIntExtra("tab", 0) ?: 0
        setContent {
            RoadmapTheme {
                App(version.intValue, startTab.intValue) { version.intValue++ }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        startTab.intValue = intent.getIntExtra("tab", 0)
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch { WtdWidget.refresh(this@MainActivity) }
    }

    override fun onResume() {
        super.onResume()
        version.intValue++
        // Pull anything done on the Windows app.
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (Cloud.token(this@MainActivity).isNotBlank()) { Cloud.syncNow(this@MainActivity); version.intValue++ }
        }
    }
}

@Composable
fun RoadmapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon.Cyan, onPrimary = Color(0xFF00121A),
            secondary = Neon.Green, background = Neon.Bg, surface = Neon.Panel,
            onSurface = Neon.Text, onBackground = Neon.Text,
        ),
        content = content,
    )
}

private val shortFmt = DateTimeFormatter.ofPattern("MMM d")
private val longFmt = DateTimeFormatter.ofPattern("MMM d ''yy")
private fun fmt(d: LocalDate) = if (d.year == LocalDate.now().year) d.format(shortFmt) else d.format(longFmt)
private fun range(a: LocalDate, b: LocalDate) = if (a == b) fmt(a) else "${fmt(a)} → ${fmt(b)}"

// ---------- small building blocks ----------

@Composable
private fun Pill(text: String, color: Color, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = Mono)
    }
}

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    glow: Color = Neon.Line,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Neon.Panel2, Neon.Panel)))
            .border(1.dp, glow, RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
private fun Label(text: String, color: Color = Neon.Cyan) {
    Text(text.uppercase(), color = color, fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProgressRing(fraction: Float, size: Dp = 92.dp) {
    val anim by animateFloatAsState(fraction, tween(900), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            drawArc(Neon.Line, 0f, 360f, false, style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(Neon.Cyan, Neon.Green, Neon.Cyan)),
                -90f, 360f * anim, false, style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(fraction * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Neon.Text)
            Text("DONE", fontSize = 9.sp, fontFamily = Mono, color = Neon.Muted, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700))
    Box(modifier.height(5.dp).clip(RoundedCornerShape(50)).background(Neon.Line)) {
        Box(
            Modifier.fillMaxWidth(f).fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)))
        )
    }
}

@Composable
private fun SmallButton(text: String, filled: Boolean, color: Color = Neon.Cyan, icon: ImageVector? = null, onClick: () -> Unit) {
    val pad = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    val content: @Composable () -> Unit = {
        if (icon != null) { Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (filled) {
        Button(onClick, contentPadding = pad, modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF00121A))) { content() }
    } else {
        OutlinedButton(onClick, contentPadding = pad, modifier = Modifier.height(36.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)) { content() }
    }
}

// ---------- app shell ----------

@Composable
fun App(version: Int, startTab: Int, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val r = remember(version) { Store.load(ctx) }
    AppContent(r, startTab, refresh) { block -> Store.edit(ctx) { block(it) }; refresh() }
}

@Composable
fun AppContent(r: Roadmap, startTab: Int, refresh: () -> Unit, change: ((Roadmap) -> Unit) -> Unit) {
    // startTab 99 = open the Updates screen (from the Sunday notification).
    var tab by remember(startTab) { mutableIntStateOf(if (startTab == 99) 0 else startTab) }
    var showSettings by remember { mutableStateOf(false) }
    var showUpdates by remember(startTab) { mutableStateOf(startTab == 99) }
    var showBadges by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    fun newIn(t: String) = r.findings.count { it.state == "new" && r.tabOf(it) == t }
    val updatesCount = r.proposals.size + r.findings.count { it.state == "new" && (r.tabOf(it) == "other" || it.type == "change") }
    val openSettings = { showSettings = true }
    BackHandler(showSettings || showUpdates || showBadges) { showSettings = false; showUpdates = false; showBadges = false }
    val wtdLeft = StudyPlan.tasksFor(today, r).count { it.stream != "rest" && !r.isDone(it) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    val doSync: () -> Unit = {
        if (Cloud.token(ctx).isBlank()) showSettings = true
        else if (!syncing) {
            syncing = true
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val msg = Cloud.syncNow(ctx)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    syncing = false; refresh(); android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var apkUpdate by remember { mutableStateOf<AppUpdate.Apk?>(null) }
    var updating by remember { mutableStateOf(false) }
    LaunchedEffect(r) {
        apkUpdate = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { Updater.check(ctx) }.getOrNull() }
    }
    val doUpdate: () -> Unit = {
        if (!updating) {
            updating = true
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val msg = Updater.install(ctx)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    updating = false; android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    CompositionLocalProvider(LocalHeaderActions provides HeaderActions(updatesCount, doSync, syncing, apkUpdate?.versionName, doUpdate, updating) { showUpdates = true }) {
    Scaffold(
        containerColor = Neon.Bg,
        bottomBar = {
            if (!showSettings && !showUpdates && !showBadges) NavigationBar(containerColor = Neon.Panel, tonalElevation = 0.dp) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Neon.Cyan, selectedTextColor = Neon.Cyan,
                    indicatorColor = Neon.Cyan.copy(alpha = 0.12f),
                    unselectedIconColor = Neon.Muted, unselectedTextColor = Neon.Muted,
                )
                listOf(
                    Triple("WTD", Icons.Filled.CheckCircle, wtdLeft),
                    Triple("Roadmap", Icons.Filled.Home, 0),
                    Triple("Projects", Icons.Filled.List, 0),
                    Triple("Videos", Icons.Filled.PlayArrow, newIn("videos")),
                    Triple("Jobs", Icons.Filled.Build, newIn("jobs")),
                    Triple("WGU", Icons.Filled.DateRange, newIn("wgu")),
                    Triple("Gear", Icons.Filled.ShoppingCart, 0),
                ).forEachIndexed { i, (label, icon, count) ->
                    NavigationBarItem(tab == i, { tab = i }, {
                        BadgedBox(badge = { if (count > 0) Badge(containerColor = Neon.Amber, contentColor = Color.Black) { Text("$count") } }) {
                            Icon(icon, null)
                        }
                    }, label = { Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false) }, colors = colors)
                }
            }
        },
    ) { inner ->
        Box(
            Modifier.fillMaxSize().padding(inner)
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1A2A), Neon.Bg, Neon.Bg)))
        ) {
            when {
                showSettings -> SettingsTab(r, today, refresh, change) { showSettings = false }
                showUpdates -> UpdatesTab(r, today, change, openSettings) { showUpdates = false }
                showBadges -> BadgesTab(r, today, openSettings) { showBadges = false }
                else -> androidx.compose.animation.Crossfade(tab, animationSpec = tween(220), label = "tab") { tb -> when (tb) {
                    0 -> WtdTab(r, today, change, openSettings)
                    1 -> RoadmapTab(r, today, change, openSettings) { showBadges = true }
                    2 -> ProjectsTab(r, today, change, openSettings)
                    3 -> VideosTab(r, today, change, openSettings)
                    4 -> JobsTab(r, today, change, openSettings)
                    5 -> WguTab(r, today, change, openSettings)
                    else -> GearTab(r, change, openSettings)
                } }
            }
        }
    }
    }
}

class HeaderActions(
    val updatesCount: Int, val sync: () -> Unit = {}, val syncing: Boolean = false,
    val appUpdate: String? = null, val installUpdate: () -> Unit = {}, val installing: Boolean = false,
    val openUpdates: () -> Unit,
)
val LocalHeaderActions = staticCompositionLocalOf { HeaderActions(0) {} }

@Composable
private fun ScreenHeader(title: String, subtitle: String, onSettings: () -> Unit) {
    val h = LocalHeaderActions.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Neon.Text, fontWeight = FontWeight.Black, fontSize = 26.sp)
            Text(subtitle, color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // One tap: pull from + push to the Windows app.
        val spin by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)))
        IconButton(h.sync) { Icon(Icons.Filled.Refresh, "Sync", tint = if (h.syncing) Neon.Cyan else Neon.Muted, modifier = if (h.syncing) Modifier.rotate(spin) else Modifier) }
        IconButton(h.openUpdates) {
            BadgedBox(badge = { if (h.updatesCount > 0) Badge(containerColor = Neon.Amber, contentColor = Color.Black) { Text("${h.updatesCount}") } }) {
                Icon(Icons.Filled.Notifications, "Updates", tint = if (h.updatesCount > 0) Neon.Amber else Neon.Muted)
            }
        }
        IconButton(onSettings) { Icon(Icons.Filled.Settings, "Settings", tint = Neon.Muted) }
    }
    androidx.compose.animation.AnimatedVisibility(h.appUpdate != null) {
        val pulse by rememberInfiniteTransition().animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(1100), androidx.compose.animation.core.RepeatMode.Reverse))
        Panel(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(enabled = !h.installing) { h.installUpdate() }, glow = Neon.Green.copy(alpha = pulse)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (h.installing) "Downloading v${h.appUpdate}..." else "✨ v${h.appUpdate} is ready. Tap to update.",
                    color = Neon.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Pill(if (h.installing) "…" else "UPDATE", Neon.Green)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = Neon.Cyan) {
    Spacer(Modifier.height(16.dp)); Label(text, color); Spacer(Modifier.height(8.dp))
}

// ---------- roadmap ----------

@Composable
private fun Proposals(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit) {
    r.proposals.forEach { p ->
        Panel(Modifier.fillMaxWidth().padding(vertical = 5.dp), glow = Neon.Amber.copy(alpha = 0.6f)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = Neon.Amber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Label("Needs your OK", Neon.Amber)
                }
                Text(p.text, color = Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton("Move dates", true, Neon.Amber) { change { it.approve(p.id, today) } }
                    SmallButton("Keep dates", false, Neon.Muted) { change { it.keep(p.id, today) } }
                }
            }
        }
    }
}

@Composable
private fun Hero(r: Roadmap, today: LocalDate) {
    val drift = r.drift(today)
    val (label, color) = when {
        drift > 0 -> "$drift DAYS BEHIND" to Neon.Amber
        drift < 0 -> "${-drift} DAYS AHEAD" to Neon.Green
        else -> "ON TRACK" to Neon.Green
    }
    val done = r.items.count { it.status == Status.DONE }
    val frac = if (r.items.isEmpty()) 0f else done.toFloat() / r.items.size
    val nextLocked = r.items.filter { it.locked && it.status != Status.DONE && !it.end.isBefore(today) }.minByOrNull { it.end }
    val working = r.items.count { it.status == Status.WORKING }

    Panel(Modifier.fillMaxWidth(), glow = Neon.Cyan.copy(alpha = 0.35f)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(frac)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Help desk → SOC → Cloud", color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Pill(label, color)
                    Text("$done of ${r.items.size} complete · $working active", color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono)
                }
            }
            nextLocked?.let {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.Bg.copy(alpha = 0.6f))
                        .border(1.dp, Neon.Line, RoundedCornerShape(10.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Neon.Violet, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NEXT LOCKED DATE", color = Neon.Violet, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(it.title.substringBefore(":").substringBefore(" ("), color = Neon.Text, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${Roadmap.days(today, it.end)}", color = Neon.Violet, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("DAYS", color = Neon.Muted, fontFamily = Mono, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

private fun stageColor(i: Int) = listOf(Neon.Cyan, Neon.Green, Neon.Amber, Neon.Violet)[i % 4]

@Composable
private fun RoadmapTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit, onBadges: () -> Unit) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            ScreenHeader("Roadmap", "Mission // Cloud security", onSettings)
            Hero(r, today)
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth().clickable { onBadges() }, glow = Neon.Violet.copy(alpha = 0.4f)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Neon.Violet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("${r.badges.size} Credly badges", color = Neon.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("View", color = Neon.Violet, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ArrowForward, null, tint = Neon.Violet, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Proposals(r, today, change)
        }
        Plan.stages.forEachIndexed { idx, stage ->
            val stageItems = r.items.filter { it.stage == stage.id }.sortedBy { it.start }
            if (stageItems.isEmpty()) return@forEachIndexed
            val color = stageColor(idx)
            val doneN = stageItems.count { it.status == Status.DONE }
            val auto = stageItems.any { it.status == Status.WORKING } ||
                stageItems.any { it.status != Status.DONE && !it.start.isAfter(today.plusDays(45)) }
            val open = expanded[stage.id] ?: auto
            item(key = "h-${stage.id}") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded[stage.id] = !open }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.15f))
                            .border(1.5.dp, color, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (doneN == stageItems.size) Icon(Icons.Filled.CheckCircle, null, tint = color, modifier = Modifier.size(18.dp))
                        else Text("${idx + 1}", color = color, fontWeight = FontWeight.Black, fontFamily = Mono)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stage.title.substringAfter(". "), color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Bar(doneN.toFloat() / stageItems.size, color, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            Text("$doneN/${stageItems.size}", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                        }
                    }
                    Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Neon.Muted)
                }
            }
            if (open) {
                items(stageItems, key = { it.id }) { TimelineRow(it, r, today, change, color, last = it == stageItems.last()) }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

private fun statusColor(it: Item, over: Int) = when {
    it.status == Status.DONE -> Neon.Green
    over > 0 -> Neon.Red
    it.status == Status.WORKING -> Neon.Cyan
    it.locked -> Neon.Violet
    else -> Neon.Muted
}

@Composable
private fun TimelineRow(it: Item, r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, stage: Color, last: Boolean) {
    val over = r.overdueDays(it, today)
    val c = statusColor(it, over)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(30.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2
                val y = 30.dp.toPx()
                drawLine(stage.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, if (last) y else size.height), 2.dp.toPx())
                if (it.status == Status.WORKING) drawCircle(c.copy(alpha = 0.25f), 11.dp.toPx(), Offset(x, y))
                drawCircle(Neon.Bg, 7.dp.toPx(), Offset(x, y))
                if (it.status == Status.DONE) drawCircle(c, 6.dp.toPx(), Offset(x, y))
                else drawCircle(c, 6.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f).padding(bottom = 8.dp)) { ItemCard(it, r, today, change, stage) }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, color: Color, filled: Boolean, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.08f))
            .border(1.5.dp, color.copy(alpha = if (filled) 1f else 0.7f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = if (filled) Neon.Bg else color, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun ItemCard(it: Item, r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, stage: Color, compact: Boolean = false) {
    val over = r.overdueDays(it, today)
    val c = statusColor(it, over)
    val shifted = Roadmap.days(it.originalEnd, it.end)
    val glow = when (it.status) {
        Status.DONE -> Neon.Green.copy(alpha = 0.22f)
        Status.WORKING -> c.copy(alpha = 0.55f)
        else -> Neon.Line
    }
    Panel(Modifier.fillMaxWidth(), glow = glow) {
        Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (it.locked) {
                        Icon(Icons.Filled.Lock, null, tint = Neon.Violet, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        it.title, color = if (it.status == Status.DONE) Neon.Muted else Neon.Text,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(3.dp))
                val sub = when (it.status) {
                    Status.DONE -> "Done ${it.doneOn?.let(::fmt) ?: ""}" + if (it.doneByBadge) " · Credly" else ""
                    Status.WORKING -> if (over > 0) "$over days over · due ${fmt(it.end)}" else "In progress · due ${fmt(it.end)}"
                    Status.TODO -> if (it.locked) "Locked · ${fmt(it.end)}" else range(it.start, it.end)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sub, fontSize = 11.sp, fontFamily = Mono,
                        color = when { it.status == Status.DONE -> Neon.Green.copy(alpha = 0.8f); over > 0 -> Neon.Red; it.status == Status.WORKING -> Neon.Cyan; else -> Neon.Muted },
                    )
                    if (it.status != Status.DONE && shifted != 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(if (shifted > 0) "+${shifted}d" else "${shifted}d", color = if (shifted > 0) Neon.Amber else Neon.Green, fontSize = 11.sp, fontFamily = Mono, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (Projects.byId.containsKey(it.id) && it.status != Status.DONE) {
                val cur = Projects.current(r)?.id == it.id
                Pill(if (cur) "PROJECTS TAB" else "LOCKED", if (cur) Neon.Violet else Neon.Muted, if (cur) null else Icons.Filled.Lock)
            } else when (it.status) {
                Status.TODO -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!it.locked) RoundAction(Icons.Filled.PlayArrow, stage, false, "Working on it") { change { r2 -> r2.markWorking(it.id, today) } }
                    RoundAction(Icons.Filled.Check, Neon.Green, false, "Done") { change { r2 -> r2.markDone(it.id, today, today) } }
                }
                Status.WORKING -> RoundAction(Icons.Filled.Check, Neon.Green, false, "Done") { change { r2 -> r2.markDone(it.id, today, today) } }
                Status.DONE -> RoundAction(Icons.Filled.Check, Neon.Green, true, "Undo") { change { r2 -> r2.undo(it.id, today) } }
            }
        }
    }
}

// ---------- updates ----------

@Composable
private fun FindingCard(f: Finding, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit) {
    val ctx = LocalContext.current
    val isChange = f.type == "change"
    val accent = if (isChange) Neon.Amber else Neon.Cyan
    Panel(Modifier.fillMaxWidth().padding(vertical = 5.dp), glow = if (f.state == "new") accent.copy(alpha = 0.55f) else Neon.Line) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(if (isChange) "PLAN CHANGE" else (f.category.ifBlank { "info" }).uppercase(), accent)
                if (f.state == "new") Pill("NEW", Neon.Green)
                Spacer(Modifier.weight(1f))
                Text(f.week.take(10), color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
            }
            Spacer(Modifier.height(8.dp))
            Text(f.title, color = Neon.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (f.detail.isNotBlank()) Text(f.detail, color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            if (f.link.isNotBlank()) {
                Text(
                    "Open source ↗", color = Neon.Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp).clickable {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(f.link))) }
                    },
                )
            }
            if (f.state == "new") {
                Spacer(Modifier.height(10.dp))
                if (isChange) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Approve", true, Neon.Amber) { change { it.approveFinding(f.id, today) } }
                        SmallButton("Dismiss", false, Neon.Muted) { change { it.dismissFinding(f.id) } }
                    }
                } else {
                    SmallButton("Got it", false, Neon.Cyan) { change { it.findings.firstOrNull { x -> x.id == f.id }?.state = "seen" } }
                }
            } else if (f.state != "seen") {
                Text(f.state.uppercase(), color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun UpdatesTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit, onBack: () -> Unit) {
    val list = r.findings.reversed().filter { r.tabOf(it) == "other" || (it.type == "change" && it.state == "new") }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Neon.Text) }
                Column {
                    Text("Updates", color = Neon.Text, fontWeight = FontWeight.Black, fontSize = 26.sp)
                    Text("Sunday check · approvals · activity", color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono)
                }
            }
            Proposals(r, today, change)
            SectionLabel("Certs · deals")
            if (list.isEmpty()) {
                Text(
                    if (r.feedUrl.isBlank()) "Weekly feed not connected. Add the feed link in Settings."
                    else "Nothing yet. The next check runs Sunday morning.",
                    color = Neon.Muted, fontSize = 14.sp,
                )
            }
        }
        items(list, key = { it.id }) { FindingCard(it, today, change) }
        item {
            SectionLabel("Activity log", Neon.Violet)
            if (r.notes.isEmpty()) Text("Nothing yet.", color = Neon.Muted, fontSize = 14.sp)
        }
        items(r.notes.reversed().take(40)) { n ->
            Row(Modifier.padding(vertical = 5.dp)) {
                Text(fmt(n.date), color = Neon.Violet, fontSize = 11.sp, fontFamily = Mono, modifier = Modifier.width(64.dp))
                Text(n.text, color = Neon.Text.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------- WTD: what to do ----------

private val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d")

private fun kindColor(kind: String) = when (kind) {
    "tcm", "soc" -> Neon.Cyan
    "ld" -> Neon.Green
    "lab" -> Neon.Violet
    "video" -> Color(0xFF4DA3FF)
    "thm" -> Neon.Green
    "pt" -> Color(0xFF2BD9C9)
    "bundle" -> Color(0xFF7AA2FF)
    "job" -> Color(0xFFFF8A65)
    "course" -> Neon.Amber
    "jobs" -> Color(0xFFFF7AC6)
    "alerts" -> Neon.Red
    "todo" -> Color(0xFFFF7AC6)
    else -> Neon.Muted
}

private fun kindLabel(kind: String) = when (kind) {
    "tcm" -> "TCM"; "soc" -> "SOC"; "ld" -> "PRACTICE"; "lab" -> "LAB"; "video" -> "VIDEO"; "thm" -> "TRYHACKME"; "pt" -> "PACKET TRACER"; "bundle" -> "BUNDLE"; "job" -> "JOB HUNT"
    "course" -> "COURSE"; "jobs" -> "JOBS"; "alerts" -> "ALERTS"; "todo" -> "TO-DO"; else -> "REST"
}

@Composable
private fun TaskRow(t: DayTask, done: Boolean, onToggle: () -> Unit) {
    val c = kindColor(t.stream)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(kindLabel(t.stream), c)
                Spacer(Modifier.width(8.dp))
                Text(t.where.substringBefore(" ·"), color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                t.task, color = if (done) Neon.Muted else Neon.Text, fontSize = 15.sp,
                fontWeight = if (done) FontWeight.Normal else FontWeight.SemiBold, lineHeight = 20.sp,
            )
            t.url?.let { u ->
                val ctx = LocalContext.current
                Text(when (t.stream) { "video" -> "▶ Watch"; "thm" -> "▶ Open TryHackMe"; "pt" -> "▶ Packet Tracer course"; "lab" -> "▶ Open"; else -> "▶ Open link" }, color = kindColor(t.stream), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } })
            }
        }
        if (t.stream != "rest") {
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(if (done) c else c.copy(alpha = 0.08f))
                    .border(1.5.dp, c, CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, "Done", tint = if (done) Neon.Bg else c.copy(alpha = 0.6f), modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
private fun WtdTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); nowMs = System.currentTimeMillis() } }
    var breakUntil by remember { mutableLongStateOf(0L) }
    // A 50-minute block or the daily cap ends the running session automatically.
    LaunchedEffect(nowMs, r.timerStart) {
        if (r.timerStart > 0) {
            val d = runCatching { LocalDate.parse(r.timerDay) }.getOrDefault(today)
            if (d != today || Burnout.runningSessionMin(r, d, nowMs) >= Burnout.WORK_MIN || Burnout.capReached(r, today, nowMs)) {
                val hitBlock = Burnout.runningSessionMin(r, d, nowMs) >= Burnout.WORK_MIN
                change { Burnout.stopTimer(it, nowMs) }
                if (hitBlock && !Burnout.capReached(r, today, nowMs)) breakUntil = nowMs + Burnout.BREAK_MIN * 60_000L
            }
        }
    }

    val tasks = StudyPlan.tasksFor(today, r)
    val bundles = StudyPlan.bundlesToday(today, r)
    val others = tasks.filter { it.stream != "bundle" }
    val real = tasks.filter { it.stream != "rest" }
    val doneN = real.count { r.isDone(it) }
    val allDone = real.isNotEmpty() && doneN == real.size
    val mode = Burnout.mode(r, today)
    val capped = Burnout.capReached(r, today, nowMs)
    val lateStart = today.dayOfWeek in listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY) && today == LocalDate.now() &&
        LocalTime.now().hour >= Burnout.LATE_HOUR && bundles.none { "$today|b:${it.n}" in r.studyDone }
    val running = r.timerStart > 0
    var showPlan by remember { mutableStateOf(false) }
    var showPortfolio by remember { mutableStateOf(false) }
    var quiz by remember { mutableStateOf<Pair<String, List<Q>>?>(null) }
    var noteFor by remember { mutableStateOf<Pair<StudyBundle, DayTask>?>(null) }
    var pendingWarm by remember { mutableStateOf<DayTask?>(null) }
    val sat = ThmPlan.saturdayOf(today)
    val weekStart = sat.minusDays(4) // Tue
    val monthDays = r.studyDaysThisMonth(today)

    fun toast(s: String) = android.widget.Toast.makeText(ctx, s, android.widget.Toast.LENGTH_SHORT).show()
    // Confetti when a whole bundle gets finished.
    val bundlesDone = Bundles.done(r)
    var lastDone by remember { mutableIntStateOf(bundlesDone) }
    var burst by remember { mutableIntStateOf(0) }
    LaunchedEffect(bundlesDone) { if (bundlesDone > lastDone) burst++; lastDone = bundlesDone }
    fun startTimer() {
        if (capped) return toast("Done for today. The cap is the point.")
        change { Burnout.startTimer(it, today, System.currentTimeMillis()) }
        val left = Burnout.capMin(today, r) - Burnout.minutesOn(r, today, System.currentTimeMillis())
        if (left <= Burnout.WORK_MIN) TimerAlarm.schedule(ctx, left, "Done for today", "You hit today's study cap. Close the laptop. Nothing piles up.")
        else TimerAlarm.schedule(ctx, Burnout.WORK_MIN, "Break time: 10 minutes", "50 minutes done. Stand up, water, walk. Then hit Start again.")
    }
    fun stopTimer() { change { Burnout.stopTimer(it, System.currentTimeMillis()) }; TimerAlarm.cancel(ctx) }
    fun tapSection(b: StudyBundle, t: DayTask) {
        val done = t.key in r.studyDone
        if (done) return change { it.toggleTask(t, today) }
        if (capped) return toast("Today's cap is reached. This waits for next time.")
        if (lateStart) return toast("It's after 8 pm. This bundle waits for next time.")
        if (!running) startTimer()
        when (t.key.substringAfterLast(":")) {
            "warm" -> { pendingWarm = t; quiz = "warm" to Quiz.pick((Bundles.all.getOrNull(b.n - 2) ?: b).unit, 3, today, "warm${b.n}") }
            "note" -> noteFor = b to t
            else -> change { it.toggleTask(t, today) }
        }
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            ScreenHeader("WTD", "What to do · ${today.format(dayFmt)}", onSettings)
            com.josephmiller.cloudroadmap.sleep.SleepCard()
            Spacer(Modifier.height(12.dp))

            Panel(Modifier.fillMaxWidth(), glow = (if (allDone) Neon.Green else Neon.Cyan).copy(alpha = 0.5f)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label(
                            when {
                                ThmPlan.isOffWeek(today) -> "Week off"
                                Bundles.next(r).isNotEmpty() -> "CS1015 bundles ${Bundles.done(r)}/${Bundles.all.size}"
                                ThmPlan.next(r) != null -> "TryHackMe week ${ThmPlan.next(r)!!.n}/${ThmPlan.weeks.size}"
                                else -> "SOC Level 1 complete"
                            }
                        )
                        Spacer(Modifier.weight(1f))
                        Pill(when (mode) { Mode.C -> "Plan C"; Mode.B -> "Plan B"; Mode.REST -> "Rest week" }, when (mode) { Mode.C -> Neon.Cyan; Mode.B -> Neon.Amber; Mode.REST -> Neon.Violet })
                        if (monthDays > 0) { Spacer(Modifier.width(6.dp)); Pill("$monthDays this month", Neon.Green) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (allDone) "All done for today 💪" else if (real.isEmpty()) "Day off" else "$doneN of ${real.size} steps done today",
                        color = if (allDone) Neon.Green else Neon.Text, fontSize = 22.sp, fontWeight = FontWeight.Black,
                    )
                    if (real.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Bar(doneN.toFloat() / real.size, if (allDone) Neon.Green else Neon.Cyan, Modifier.fillMaxWidth())
                    }
                    if (mode == Mode.B) {
                        Spacer(Modifier.height(10.dp))
                        Text("Your last two energy checks were low, so Saturday is 2 bundles this week instead of 3.", color = Neon.Amber, fontSize = 13.sp)
                        if (!Burnout.overrideUsed(r, today)) Text("Keep me on Plan C (pass a 5/5 quiz) ›", color = Neon.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp).clickable { quiz = "override" to Quiz.pick(Burnout.currentUnit(r), 5, today, "override") })
                    }
                    others.forEach { t -> TaskRow(t, r.isDone(t)) { change { it.toggleTask(t, today) } } }
                }
            }

            // Burnout timer
            if (Burnout.baseCapMin(today) > 0 && real.any { it.stream != "todo" }) {
                Spacer(Modifier.height(12.dp))
                val mins = Burnout.minutesOn(r, today, nowMs)
                val cap = Burnout.capMin(today, r)
                val onBreak = !running && breakUntil > nowMs
                Panel(Modifier.fillMaxWidth(), glow = (if (capped) Neon.Red else if (running) Neon.Green else Neon.Line).copy(alpha = 0.6f)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label("Burnout timer")
                            Spacer(Modifier.weight(1f))
                            Text("${mins / 60}h ${mins % 60}m / ${cap / 60}h${if (cap % 60 > 0) " ${cap % 60}m" else ""}", color = Neon.Muted, fontFamily = Mono, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TimerRing(mins.toFloat() / cap, if (capped) Neon.Red else Neon.Green, running) {
                                Text("${mins / 60}:%02d".format(mins % 60), color = Neon.Text, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = Mono)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                capped -> "Done for today. Close the laptop: whatever's left waits, it never piles up."
                                running -> "Studying · ${Burnout.runningSessionMin(r, today, nowMs)}/${Burnout.WORK_MIN} min this block"
                                onBreak -> "Break until ${java.time.Instant.ofEpochMilli(breakUntil).atZone(java.time.ZoneId.systemDefault()).toLocalTime().withNano(0).withSecond(0)}. Stand up, water, walk."
                                lateStart -> "It's after 8 pm. Today's bundle waits for next time. Get some sleep."
                                else -> "50 min on, 10 off. Starts by itself when you check your first step."
                            },
                            color = if (capped) Neon.Red else Neon.Text, fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            if (!capped) {
                                if (running) OutlinedButton(onClick = { stopTimer() }) { Text("Pause") }
                                else Button(onClick = { startTimer() }, enabled = !lateStart) { Text(if (onBreak) "Back from break" else "Start") }
                            } else if (!Burnout.overrideUsed(r, today)) {
                                OutlinedButton(onClick = { quiz = "override" to Quiz.pick(Burnout.currentUnit(r), 5, today, "override") }) { Text("Let me keep going (5/5 quiz)") }
                            }
                        }
                    }
                }
            }

            // Bundles
            bundles.forEach { b ->
                Spacer(Modifier.height(12.dp))
                val bDone = Bundles.isDone(r, b)
                Panel(Modifier.fillMaxWidth(), glow = (if (bDone) Neon.Green else kindColor("bundle")).copy(alpha = 0.5f)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label("Bundle ${b.n}/${Bundles.all.size} · Unit ${b.unit}")
                            Spacer(Modifier.weight(1f))
                            Text("~${b.minutes / 60}h ${b.minutes % 60}m", color = Neon.Muted, fontFamily = Mono, fontSize = 11.sp)
                        }
                        Text(b.title, color = if (bDone) Neon.Green else Neon.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        Text("📁 Screenshots go in ${b.folder}", color = Neon.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        b.sections.forEach { s ->
                            val t = StudyPlan.sectionTask(b, s)
                            SectionRow(s, t.key in r.studyDone, locked = (capped || lateStart) && t.key !in r.studyDone) { tapSection(b, t) }
                        }
                    }
                }
            }

            // Energy check
            if (Burnout.isStudyDay(today) && real.any { it.stream != "todo" } && (r.dayDone(today) || capped)) {
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Label("Energy check")
                        Text("How do you feel after today?", color = Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        val cur = Burnout.rating(r, today)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1 to "😩", 2 to "😕", 3 to "😐", 4 to "🙂", 5 to "💪").forEach { (n, e) ->
                                val sel = cur == n
                                Box(
                                    Modifier.size(48.dp).clip(CircleShape).background(if (sel) Neon.Cyan.copy(alpha = 0.3f) else Color.Transparent)
                                        .border(1.dp, if (sel) Neon.Cyan else Neon.Line, CircleShape)
                                        .clickable { change { it.energy[today.toString()] = n } },
                                    contentAlignment = Alignment.Center,
                                ) { Text(e, fontSize = 20.sp) }
                            }
                        }
                        if (cur != null && cur <= 2) Text("Noted. Two low days in a row and the app lightens your load automatically.", color = Neon.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            // Study days this week
            Spacer(Modifier.height(12.dp))
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0..6).forEach { k ->
                        val day = weekStart.plusDays(k.toLong())
                        val study = StudyPlan.tasksFor(day, r).any { it.stream != "rest" }
                        val done = r.dayDone(day)
                        val isToday = day == today
                        val c = when { done -> Neon.Green; !study -> Neon.Line; else -> Neon.Muted }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.dayOfWeek.name.take(3), color = if (isToday) Neon.Text else Neon.Muted, fontSize = 10.sp, fontFamily = Mono)
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier.size(30.dp).clip(CircleShape)
                                    .background(if (done) Neon.Green else Color.Transparent)
                                    .border(if (isToday) 2.dp else 1.dp, if (isToday) Neon.Text else c, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (done) Icon(Icons.Filled.Check, null, tint = Neon.Bg, modifier = Modifier.size(16.dp))
                                else Text(if (study) "${day.dayOfMonth}" else "off", color = c, fontSize = if (study) 10.sp else 8.sp, fontFamily = Mono)
                            }
                        }
                    }
                }
            }

            // Portfolio
            SectionLabel("Portfolio · ${r.portfolio.size} entries", Neon.Green)
            Text(
                if (showPortfolio) "Hide ▲" else "Show your 2-line notes ▼", color = Neon.Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showPortfolio = !showPortfolio }.padding(vertical = 4.dp),
            )
            if (showPortfolio) Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (r.portfolio.isEmpty()) Text("Finish a bundle's portfolio note and it shows up here. Copy these into GitHub + your resume.", color = Neon.Muted, fontSize = 13.sp)
                    r.portfolio.entries.sortedBy { it.key.toIntOrNull() ?: 0 }.forEach { (k, v) ->
                        Text("Bundle $k · ${Bundles.all.firstOrNull { it.n.toString() == k }?.title ?: ""}", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                        Text(v, color = Neon.Text, fontSize = 14.sp)
                    }
                }
            }

            // Projects
            val proj = Projects.current(r)
            SectionLabel("Projects", Neon.Violet)
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (StudyPlan.projectsOpen(today)) Icons.Filled.Check else Icons.Filled.Lock, null, tint = Neon.Violet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (!StudyPlan.projectsOpen(today)) "Locked until Sat May 1. First up: ${proj?.title ?: "Active Directory"}."
                        else proj?.let { "Now: ${it.title}. Steps are on Saturdays and in the Projects tab." } ?: "All projects done.",
                        color = Neon.Text, fontSize = 13.sp,
                    )
                }
            }

            ProgressSection(r, today)

            SectionLabel("Full plan", Neon.Amber)
            Text(
                if (showPlan) "Hide plan ▲" else "Show all 26 bundles + SOC weeks ▼", color = Neon.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showPlan = !showPlan }.padding(vertical = 4.dp),
            )
        }
        if (showPlan) {
            items(Bundles.all.size) { i ->
                val b = Bundles.all[i]
                val d = Bundles.isDone(r, b)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (d) "✓" else "${b.n}", color = if (d) Neon.Green else Neon.Muted, fontFamily = Mono, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("Unit ${b.unit} · ${b.title}", color = if (d) Neon.Muted else Neon.Text, fontSize = 13.sp)
                }
            }
            items(ThmPlan.weeks.size) { i ->
                val w = ThmPlan.weeks[i]
                val past = w.key in r.studyDone
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (past) "✓" else "S${w.n}", color = if (past) Neon.Green else Neon.Muted, fontFamily = Mono, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text(w.module, color = if (past) Neon.Muted else Neon.Text, fontSize = 13.sp)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    ConfettiBurst(burst)
    }

    quiz?.let { (purpose, qs) ->
        QuizDialog(if (purpose == "override") "Prove you're sharp: 5/5 to keep going" else "Warm-up: last time's topic", qs,
            onDone = { score ->
                if (purpose == "override") {
                    change { Burnout.recordOverride(it, today, score, qs.size) }
                    toast(if (score == qs.size) "5/5. You get 30 more minutes and Plan C this week." else "$score/${qs.size}. Your brain's done for today. That's the signal, not a failure.")
                } else pendingWarm?.let { t -> change { it.toggleTask(t, today) } }
                pendingWarm = null; quiz = null
            },
            onCancel = { pendingWarm = null; quiz = null })
    }
    noteFor?.let { (b, t) ->
        NoteDialog(b.noteHint, r.portfolio[b.n.toString()] ?: "",
            onSave = { text -> change { it.portfolio[b.n.toString()] = text.trim(); it.toggleTask(t, today) }; noteFor = null },
            onCancel = { noteFor = null })
    }
}

@Composable
private fun SectionRow(s: Section, done: Boolean, locked: Boolean, onTap: () -> Unit) {
    val c = when (s.id) { "sophia" -> Neon.Amber; "thm" -> Neon.Green; "pt" -> Color(0xFF2BD9C9); "warm" -> Neon.Violet; else -> Neon.Cyan }
    var open by remember { mutableStateOf(!done) }
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).clickable { open = !open }) {
            Text("${s.title} · ${s.minutes} min", color = if (done) Neon.Muted else c, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (open && !done) {
                s.steps.forEachIndexed { i, st -> Text("${i + 1}. $st", color = Neon.Text, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp)) }
                s.shots.forEach { Text("📸 $it", color = Neon.Amber, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                s.url?.let { u ->
                    Text("▶ Open", color = c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } })
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        val pop by animateFloatAsState(if (done) 1f else 0.9f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 500f))
        Box(
            Modifier.size(38.dp).scale(pop).clip(CircleShape)
                .background(if (done) c else c.copy(alpha = 0.08f))
                .border(1.5.dp, if (locked) Neon.Line else c, CircleShape)
                .clickable { onTap() },
            contentAlignment = Alignment.Center,
        ) { Icon(if (locked) Icons.Filled.Lock else Icons.Filled.Check, "Done", tint = if (done) Neon.Bg else if (locked) Neon.Muted else c.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun QuizDialog(title: String, qs: List<Q>, onDone: (Int) -> Unit, onCancel: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    val finished = i >= qs.size
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Neon.Panel,
        title = { Text(if (finished) "Score: $score/${qs.size}" else "$title (${i + 1}/${qs.size})", color = Neon.Text, fontSize = 16.sp) },
        text = {
            if (!finished) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val q = qs[i]
                Text(q.q, color = Neon.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                q.options.forEachIndexed { k, o ->
                    val col = when { picked == null -> Neon.Line; k == q.answer -> Neon.Green; k == picked -> Neon.Red; else -> Neon.Line }
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, col, RoundedCornerShape(10.dp))
                        .clickable(enabled = picked == null) { picked = k; if (k == q.answer) score++ }.padding(12.dp)) {
                        Text(o, color = Neon.Text, fontSize = 14.sp)
                    }
                }
            } else Text(if (score == qs.size) "Perfect." else "Review what you missed next time.", color = Neon.Muted)
        },
        confirmButton = {
            TextButton(onClick = {
                if (finished) onDone(score) else if (picked != null) { i++; picked = null }
            }, enabled = finished || picked != null) { Text(if (finished) "Done" else "Next", color = Neon.Cyan) }
        },
        dismissButton = { if (!finished) TextButton(onClick = onCancel) { Text("Cancel", color = Neon.Muted) } },
    )
}

@Composable
private fun NoteDialog(hint: String, existing: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(existing.ifBlank { hint }) }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Neon.Panel,
        title = { Text("Portfolio note (2 lines)", color = Neon.Text, fontSize = 16.sp) },
        text = {
            Column {
                Text("Edit it to say what YOU did. This goes on GitHub + your resume.", color = Neon.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Neon.Text, unfocusedTextColor = Neon.Text))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save", color = Neon.Cyan) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Neon.Muted) } },
    )
}

@Composable
private fun ProgressSection(r: Roadmap, today: LocalDate) {
    val weeks = Progress.weekly(r, today)
    val maxDays = 7f
    SectionLabel("Your progress", Neon.Violet)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("${r.streak(today)}", "Streak", Neon.Amber, Modifier.weight(1f))
        Stat("${Progress.bestStreak(r)}", "Best", Neon.Green, Modifier.weight(1f))
        Stat("${Progress.studyDays(r).size}", "Study days", Neon.Cyan, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("STUDY DAYS PER WEEK", color = Neon.Muted, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                Text("${r.items.count { it.status == Status.DONE }} done · THM ${ThmPlan.done(r)}/${ThmPlan.weeks.size}", color = Neon.Muted, fontFamily = Mono, fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                weeks.forEachIndexed { i, (_, n) ->
                    val current = i == weeks.lastIndex
                    Box(
                        Modifier.weight(1f).fillMaxHeight((n / maxDays).coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (n == 0) Neon.Line else if (current) Neon.Cyan else Neon.Violet.copy(alpha = 0.75f))
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(weeks.first().first), color = Neon.Muted, fontSize = 10.sp, fontFamily = Mono, modifier = Modifier.weight(1f))
                Text("this week", color = Neon.Cyan, fontSize = 10.sp, fontFamily = Mono)
            }
        }
    }
}

@Composable
private fun AlertRow(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit) {
    val c = kindColor("alerts")
    val week = r.alertsInWeek(today)
    val left = r.alertsLeftThisMonth(today)
    val goalMet = week >= Roadmap.ALERTS_PER_WEEK
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("ALERTS", c)
            Spacer(Modifier.width(8.dp))
            Text("Work laptop", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (left == 0 && !goalMet) "Free alert cap hit for this month. Resumes on the 1st."
            else "Triage ${Roadmap.ALERTS_PER_WEEK} SOC alerts in the simulator.",
            color = if (goalMet) Neon.Muted else Neon.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            (1..Roadmap.ALERTS_PER_WEEK).forEach { n ->
                Box(Modifier.padding(end = 6.dp).size(14.dp).clip(CircleShape).background(if (week >= n) c else c.copy(alpha = 0.12f)).border(1.dp, c, CircleShape))
            }
            Spacer(Modifier.width(6.dp))
            Text("$week/${Roadmap.ALERTS_PER_WEEK} week · $left left in month", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono, modifier = Modifier.weight(1f))
            Text("−", color = Neon.Muted, fontSize = 22.sp, modifier = Modifier.clickable { change { it.logAlert(today, -1) } }.padding(horizontal = 10.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (left > 0) c else Neon.Line)
                    .clickable(enabled = left > 0) { change { it.logAlert(today, 1) } },
                contentAlignment = Alignment.Center,
            ) { Text("+1", color = Neon.Bg, fontWeight = FontWeight.Black, fontSize = 14.sp) }
        }
    }
}

@Composable
private fun WeekLine(label: String, text: String, color: Color) {
    Row {
        Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label.uppercase(), color = color, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)
            Text(text, color = Neon.Text, fontSize = 14.sp)
        }
    }
}

// ---------- videos ----------

private val creatorColors = listOf(Neon.Cyan, Neon.Amber, Color(0xFF4DA3FF), Neon.Red)

@Composable
private fun VideosTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val vids = r.findings.reversed().filter { r.tabOf(it) == "videos" }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item { ScreenHeader("Videos", "A pair for every lesson · watch on the commute", onSettings) }
        item {
            Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp), glow = Neon.Cyan.copy(alpha = 0.4f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("When to watch", color = Neon.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("① Before: the learn-it video on the commute to work, the morning of that lesson.", color = Neon.Text, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Text("② After: NetworkChuck on the ride home or your next day off, to lock it in.", color = Neon.Text, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Not during: it eats your 2-hour study cap.", color = Neon.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Check a video off and you get a 3-question quick check.", color = Neon.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item { SectionLabel("CS1015 LESSONS · LEARN IT, THEN MAKE IT STICK") }
        item(key = "lessons") { LessonVideos(r, change) }
        item { SectionLabel("LATER: SOC + A+", Neon.Violet) }
        Videos.creators.forEachIndexed { i, c ->
            val color = creatorColors[i % creatorColors.size]
            val mine = vids.filter { Videos.creatorOf(it) == c }
            val items = c.itemIds.mapNotNull { r.item(it) }
            item(key = "cr-${c.id}") {
                Panel(Modifier.fillMaxWidth().padding(vertical = 6.dp), glow = if (mine.any { it.state == "new" }) color.copy(alpha = 0.6f) else Neon.Line) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)).border(1.5.dp, color, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text(c.name.filter { it.isUpperCase() }.let { if (it.length >= 2) it.first().toString() + it.last() else it }, color = color, fontWeight = FontWeight.Black, fontFamily = Mono) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(c.focus, color = Neon.Muted, fontSize = 12.sp)
                            }
                            val n = mine.count { it.state == "new" }
                            if (n > 0) Pill("$n NEW", Neon.Green)
                        }
                        if (items.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            items.forEach { CheckRow(it) }
                        }
                        Spacer(Modifier.height(8.dp))
                        c.links.chunked(2).forEach { row ->
                            Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (label, url) ->
                                    SmallButton(label, false, color, Icons.Filled.PlayArrow) {
                                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    }
                                }
                            }
                        }
                        if (mine.isNotEmpty()) {
                            Text("FROM THE SUNDAY CHECK", color = Neon.Muted, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
                            mine.take(5).forEach { f ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                        if (f.link.isNotBlank()) runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(f.link))) }
                                        if (f.state == "new") change { it.findings.firstOrNull { x -> x.id == f.id }?.state = "seen" }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (f.state == "new") Neon.Green else Neon.Line))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(f.title, color = Neon.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (f.detail.isNotBlank()) Text(f.detail, color = Neon.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(f.week.take(10).drop(5), color = Neon.Muted, fontSize = 10.sp, fontFamily = Mono)
                                }
                            }
                        }
                    }
                }
            }
        }
        val other = vids.filter { Videos.creatorOf(it) == null }
        if (other.isNotEmpty()) {
            item { SectionLabel("Other creators") }
            items(other, key = { it.id }) { FindingCard(it, today, change) }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------- projects ----------

private fun levelColor(level: String) = when (level) { "Beginner" -> Neon.Green; "Medium" -> Neon.Amber; else -> Neon.Red }

@Composable
private fun ProjectsTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit) {
    val current = Projects.current(r)
    val open = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            ScreenHeader("Projects", "Do #1 first · beginner → expert", onSettings)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Beginner", "Medium", "Expert").forEach { lv ->
                    val ps = Projects.all.filter { it.level == lv }
                    Stat("${ps.count { Projects.isDone(r, it) }}/${ps.size}", lv, levelColor(lv), Modifier.weight(1f))
                }
            }
            if (!StudyPlan.projectsOpen(today)) {
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth(), glow = Neon.Violet.copy(alpha = 0.5f)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, tint = Neon.Violet, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("All projects locked until Sat May 1, after TryHackMe SOC Level 1. You can read them, but not start.", color = Neon.Text, fontSize = 13.sp)
                    }
                }
            }
        }
        listOf("Beginner" to "Help desk · from May 1", "Medium" to "SOC analyst · 2027", "Expert" to "Cloud security · 2028+").forEach { (lv, sub) ->
            item(key = "lv-$lv") {
                Row(Modifier.padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(lv.uppercase(), levelColor(lv))
                    Spacer(Modifier.width(8.dp))
                    Text(sub, color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono)
                }
            }
            items(Projects.all.filter { it.level == lv }, key = { it.id }) { p ->
                val idx = Projects.all.indexOf(p) + 1
                val done = Projects.isDone(r, p)
                val stepsDone = Projects.doneSteps(r, p)
                val isNext = p == current
                val locked = !done && !isNext
                val c = if (locked) Neon.Muted else levelColor(p.level)
                val expanded = open[p.id] ?: isNext
                Panel(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    glow = when { isNext -> c.copy(alpha = 0.7f); done -> Neon.Green.copy(alpha = 0.25f); else -> Neon.Line },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { open[p.id] = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(if (done) Neon.Green else c.copy(alpha = 0.12f)).border(1.5.dp, if (done) Neon.Green else c, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (done) Icon(Icons.Filled.Check, null, tint = Neon.Bg, modifier = Modifier.size(16.dp))
                                else if (locked) Icon(Icons.Filled.Lock, "Locked", tint = Neon.Muted, modifier = Modifier.size(15.dp))
                                else Text("$idx", color = c, fontWeight = FontWeight.Black, fontFamily = Mono, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.title, color = if (done) Neon.Muted else Neon.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f, fill = false))
                                    if (isNext) { Spacer(Modifier.width(6.dp)); Pill("NEXT", c) }
                                }
                                Text(
                                    when { done -> "Done"; locked -> "#$idx · locked · unlocks after #${Projects.all.indexOf(current) + 1}"; else -> "$stepsDone/${p.steps.size} steps · target ${p.target}" },
                                    color = if (done) Neon.Green else Neon.Muted, fontSize = 11.sp, fontFamily = Mono,
                                )
                            }
                            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Neon.Muted)
                        }
                        if (!done && !locked) {
                            Spacer(Modifier.height(8.dp))
                            Bar(stepsDone.toFloat() / p.steps.size, c, Modifier.fillMaxWidth())
                        }
                        if (expanded && locked) {
                            Text(p.why, color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                            Text("Steps unlock when you finish ${current?.title}.", color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono, modifier = Modifier.padding(top = 6.dp))
                            PartsList(r, p, c, change)
                        } else if (expanded) {
                            Text(p.why, color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                            p.steps.forEachIndexed { n, step ->
                                val key = "lab:${p.id}:$n"
                                val sd = key in r.studyDone || (done && r.item(p.id)?.status == Status.DONE)
                                val task = DayTask(key, "lab", "", step, p.id)
                                val can = r.projectLock(task, today) == null
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(22.dp).clip(CircleShape).background(if (sd) c else Color.Transparent)
                                            .border(1.5.dp, if (can || sd) c else Neon.Line, CircleShape)
                                            .clickable(enabled = can) { change { it.toggleTask(task, today) } },
                                        contentAlignment = Alignment.Center,
                                    ) { if (sd) Icon(Icons.Filled.Check, null, tint = Neon.Bg, modifier = Modifier.size(14.dp)) }
                                    Spacer(Modifier.width(10.dp))
                                    Text("${n + 1}. $step", color = if (sd) Neon.Muted else Neon.Text, fontSize = 13.sp)
                                }
                            }
                            PartsList(r, p, c, change)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun LessonVideos(r: Roadmap, change: ((Roadmap) -> Unit) -> Unit) {
    val ctx = LocalContext.current
    val nextN = Bundles.next(r).firstOrNull()?.n
    val open = remember { mutableStateMapOf<Int, Boolean>() }
    var quiz by remember { mutableStateOf<Triple<StudyBundle, LessonPair, String>?>(null) }
    quiz?.let { (b, pair, k) ->
        val v = if (k == "pn") pair.pn else pair.chuck
        QuizDialog("Quick check: ${v.title}", Quiz.afterVideo(b, k, LocalDate.now()),
            onDone = { score ->
                change { Videos.toggle(it, pair.key(k)); it.studyDone.add("quiz:${pair.key(k)}:$score") }
                quiz = null
            },
            onCancel = { quiz = null })
    }
    Column {
        Bundles.all.forEach { b ->
            val pair = Videos.lessonByBundle[b.n] ?: return@forEach
            val watched = listOf("pn", "nc").count { pair.key(it) in r.studyDone }
            val isNext = b.n == nextN
            val expanded = open[b.n] ?: isNext
            val c = if (watched == 2) Neon.Green else if (isNext) Neon.Cyan else Neon.Muted
            Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp).animateContentSize(), glow = if (isNext) Neon.Cyan.copy(alpha = 0.6f) else Neon.Line) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { open[b.n] = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                        Text("%02d".format(b.n), color = c, fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(b.title, color = if (watched == 2) Neon.Muted else Neon.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (isNext) { Pill("NEXT", Neon.Cyan); Spacer(Modifier.width(6.dp)) }
                        Text("$watched/2", color = c, fontFamily = Mono, fontSize = 11.sp)
                    }
                    if (expanded) listOf("pn" to pair.pn, "nc" to pair.chuck).forEach { (k, v) ->
                        val seen = pair.key(k) in r.studyDone
                        val vc = if (k == "pn") Neon.Cyan else Neon.Amber
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape).background(if (seen) vc else Color.Transparent).border(1.5.dp, vc, CircleShape)
                                    .clickable { if (seen) change { Videos.toggle(it, pair.key(k)) } else quiz = Triple(b, pair, k) },
                                contentAlignment = Alignment.Center,
                            ) { if (seen) Icon(Icons.Filled.Check, null, tint = Neon.Bg, modifier = Modifier.size(14.dp)) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(v.url))) } }) {
                                Text(v.title, color = if (seen) Neon.Muted else Neon.Text, fontSize = 13.sp)
                                Text(if (k == "pn") "${v.channel} · learn it" else "NetworkChuck · make it stick", color = vc, fontSize = 11.sp, fontFamily = Mono)
                            }
                            Icon(Icons.Filled.PlayArrow, "Play", tint = vc, modifier = Modifier.size(20.dp).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(v.url))) } })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GearTab(r: Roadmap, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit) {
    val withParts = Projects.all.filter { it.parts.isNotEmpty() }
    val left = withParts.sumOf { Projects.partsLeftCost(r, it) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item { ScreenHeader("Gear", "Shopping list · ~$$left left to buy", onSettings) }
        item {
            Panel(Modifier.fillMaxWidth().padding(vertical = 4.dp), glow = Neon.Amber.copy(alpha = 0.5f)) {
                Text("Plan: one Prime Day order in July 2027, a couple of weeks before the build. Don't buy before the project unlocks.",
                    color = Neon.Text, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            }
        }
        withParts.forEach { p ->
            item(key = p.id) {
                val c = levelColor(p.level)
                Panel(Modifier.fillMaxWidth().padding(vertical = 6.dp), glow = c.copy(alpha = 0.4f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(p.title, color = Neon.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Build target ${p.target}", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                        PartsList(r, p, c, change)
                    }
                }
            }
        }
    }
}

@Composable
private fun PartsList(r: Roadmap, p: Project, c: Color, change: ((Roadmap) -> Unit) -> Unit) {
    if (p.parts.isEmpty()) return
    val ctx = LocalContext.current
    val got = p.parts.indices.count { Projects.partKey(p, it) in r.studyDone }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("GEAR LIST · $got/${p.parts.size}", color = c, fontSize = 11.sp, fontFamily = Mono, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("~$${Projects.partsLeftCost(r, p)} left", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
    }
    p.parts.forEachIndexed { n, part ->
        val have = Projects.partKey(p, n) in r.studyDone
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (have) c else Color.Transparent)
                    .border(1.5.dp, c, RoundedCornerShape(6.dp))
                    .clickable { change { Projects.togglePart(it, p, n) } },
                contentAlignment = Alignment.Center,
            ) { if (have) Icon(Icons.Filled.Check, null, tint = Neon.Bg, modifier = Modifier.size(14.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(part.url))) } }) {
                Text(part.name, color = if (have) Neon.Muted else Neon.Text, fontSize = 13.sp)
                Text(part.why, color = Neon.Muted, fontSize = 11.sp)
                if (part.buy.isNotBlank() && !have) Text("When to buy: ${part.buy}", color = c.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = Mono)
            }
            Text("$${part.price}", color = if (have) Neon.Muted else c, fontSize = 12.sp, fontFamily = Mono)
        }
    }
    Text("Tap an item to look it up. Prices are estimates. Don't buy before the project unlocks.", color = Neon.Muted, fontSize = 10.sp, fontFamily = Mono, modifier = Modifier.padding(top = 2.dp))
}

// ---------- badges ----------

private fun badgeColor(issuer: String) = when {
    issuer.contains("Sophia", true) -> Neon.Cyan
    issuer.contains("Amazon", true) || issuer.contains("AWS", true) -> Neon.Amber
    issuer.contains("Microsoft", true) -> Color(0xFF4DA3FF)
    issuer.contains("CompTIA", true) -> Neon.Red
    issuer.contains("Study.com", true) -> Neon.Green
    else -> Neon.Violet
}

private fun badgeCode(name: String): String {
    Regex("\\(([A-Z]{2,5}\\d{3,4})\\)").find(name)?.let { return it.groupValues[1] }
    return name.split(" ", ":", "-").filter { it.isNotBlank() && it[0].isUpperCase() }.take(3).joinToString("") { it.take(1) }
}

private fun upcomingCode(it: Item): String {
    Regex("[A-Z]{2,3}-\\d{3}").find(it.title)?.let { m -> return m.value }
    Regex("[A-Z]{2,5}\\d{3,4}").find(it.title)?.let { m -> return m.value }
    if ("A+" in it.title) return "A+"
    return badgeCode(it.title)
}

@Composable
private fun Hexagon(color: Color, text: String, size: Dp = 64.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            val path = Path().apply {
                moveTo(w / 2, 0f); lineTo(w, h * 0.25f); lineTo(w, h * 0.75f)
                lineTo(w / 2, h); lineTo(0f, h * 0.75f); lineTo(0f, h * 0.25f); close()
            }
            drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.08f))))
            drawPath(path, color, style = Stroke(2.dp.toPx()))
        }
        Text(text, color = color, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = when { size < 50.dp -> if (text.length > 4) 8.sp else 10.sp; text.length > 6 -> 9.sp; else -> 11.sp })
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier) {
    Panel(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text(label.uppercase(), color = Neon.Muted, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun BadgesTab(r: Roadmap, today: LocalDate, onSettings: () -> Unit, onBack: () -> Unit) {
    val upcoming = r.items.filter { it.badgeKey != null && it.status != Status.DONE }.sortedBy { it.end }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Neon.Text) }
                Column {
                    Text("Badges", color = Neon.Text, fontWeight = FontWeight.Black, fontSize = 26.sp)
                    Text("Synced from Credly · @${r.credlyUser.substringBefore(".")}", color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${r.badges.size}", "Earned", Neon.Cyan, Modifier.weight(1f))
                Stat("${r.badges.count { it.issuer.contains("Sophia", true) }}", "Sophia", Neon.Green, Modifier.weight(1f))
                Stat("${r.badges.count { !it.date.isBefore(today.minusDays(30)) }}", "Last 30d", Neon.Violet, Modifier.weight(1f))
            }
            if (upcoming.isNotEmpty()) {
                SectionLabel("Up next · auto-tracked", Neon.Amber)
                upcoming.take(5).forEach { u ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Hexagon(Neon.Muted, upcomingCode(u), 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.title, color = Neon.Text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Target ${fmt(u.end)}", color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono)
                        }
                    }
                }
            }
            SectionLabel("Earned")
        }
        items(r.badges.chunked(2)) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b ->
                    val c = badgeColor(b.issuer)
                    Panel(Modifier.weight(1f).height(170.dp)) {
                        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Hexagon(c, badgeCode(b.name))
                            Spacer(Modifier.height(8.dp))
                            Text(b.name.substringBefore(" ("), color = Neon.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.weight(1f))
                            Text(fmt(b.date), color = c, fontSize = 11.sp, fontFamily = Mono)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------- jobs ----------

@Composable
private fun CheckRow(it: Item) {
    val done = it.status == Status.DONE
    val c = when (it.status) { Status.DONE -> Neon.Green; Status.WORKING -> Neon.Cyan; else -> Neon.Muted }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(if (done) c else Color.Transparent).border(1.5.dp, c, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done) Text("✓", color = Neon.Bg, fontSize = 12.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(10.dp))
        Text(it.title.substringBefore(" ("), color = if (done) Neon.Muted else Neon.Text, fontSize = 14.sp,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(if (done) "DONE" else fmt(it.end), color = c, fontSize = 11.sp, fontFamily = Mono)
    }
}

@Composable
private fun Countdown(title: String, days: Int, color: Color, sub: String) {
    Panel(Modifier.fillMaxWidth(), glow = color.copy(alpha = 0.45f)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label(title, color)
                Spacer(Modifier.height(4.dp))
                Text(sub, color = Neon.Muted, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${maxOf(days, 0)}", color = color, fontWeight = FontWeight.Black, fontSize = 40.sp)
                Text("DAYS", color = Neon.Muted, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun JobsTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit) {
    val ready = listOf("resume", "lab_ad", "lab_net", "lab_ticket", "lab_ps", "k401_check").mapNotNull { r.item(it) }
    val path = listOf("apply", "helpdesk_job", "soc_apply", "soc_job", "cloudsec_job").mapNotNull { r.item(it) }
    val jobs = r.findings.reversed().filter { r.tabOf(it) == "jobs" }
    val apply = r.item("apply")
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            ScreenHeader("Jobs", "Help desk → SOC → Cloud security", onSettings)
            if (apply != null && apply.status != Status.DONE) {
                Countdown("Start applying", Roadmap.days(today, apply.end), Neon.Green, "Mid-January · Southwest, airport IT, Dallas help desk")
            }
            SectionLabel("Ready to apply · ${ready.count { it.status == Status.DONE }}/${ready.size}", Neon.Green)
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Bar(ready.count { it.status == Status.DONE }.toFloat() / maxOf(ready.size, 1), Neon.Green, Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    ready.forEach { CheckRow(it) }
                }
            }
            SectionLabel("Career path", Neon.Violet)
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { path.forEach { CheckRow(it) } }
            }
            SectionLabel("Openings & market")
            if (jobs.isEmpty()) {
                Text("Southwest and Love Field IT openings show up here after each Sunday check. Dallas help desk pay snapshots start in January.",
                    color = Neon.Muted, fontSize = 14.sp)
            }
        }
        items(jobs, key = { it.id }) { FindingCard(it, today, change) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ---------- WGU ----------

@Composable
private fun WguTab(r: Roadmap, today: LocalDate, change: ((Roadmap) -> Unit) -> Unit, onSettings: () -> Unit) {
    val start = r.item("wgu_start")
    val transfer = r.items.filter { it.stage == "transfer" && it.id != "tcm_ld" }.sortedBy { it.start }
    val courses = r.items.filter { it.stage == "wgu" && it.id != "wgu_start" }.sortedBy { it.start }
    val sophia = r.badges.count { it.issuer.contains("Sophia", true) }
    val news = r.findings.reversed().filter { r.tabOf(it) == "wgu" }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp)) {
        item {
            ScreenHeader("WGU", "B.S. Cybersecurity · starts Aug 2027", onSettings)
            if (start != null) {
                Countdown(if (start.status == Status.DONE) "Enrolled" else "Term starts", Roadmap.days(today, start.end), Neon.Cyan, "Start date ${fmt(start.end)}")
            }
            SectionLabel("Transfer credits", Neon.Green)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("$sophia", "Sophia badges", Neon.Cyan, Modifier.weight(1f))
                Stat("${transfer.count { it.status == Status.DONE }}/${transfer.size}", "Done", Neon.Green, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { transfer.forEach { CheckRow(it) } }
            }
            SectionLabel("First WGU courses", Neon.Violet)
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { courses.forEach { CheckRow(it) } }
            }
            WguDegreePlan(r)

            SectionLabel("Deadlines & tuition")
            if (news.isEmpty()) {
                Text("Application deadlines, tuition changes and transfer-rule updates show up here after each Sunday check.",
                    color = Neon.Muted, fontSize = 14.sp)
            }
        }
        items(news, key = { it.id }) { FindingCard(it, today, change) }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun WguDegreePlan(r: Roadmap) {
    var open by remember { mutableStateOf(false) }
    val active = r.wguCourses.filter { it.flag != "removed" }
    val removed = r.wguCourses.filter { it.flag == "removed" }
    val added = active.count { it.flag == "new" }
    SectionLabel("Degree plan · ${active.size} courses · ${active.sumOf { it.cu }} CUs", Neon.Cyan)
    Panel(Modifier.fillMaxWidth(), glow = if (added + removed.size > 0) Neon.Amber.copy(alpha = 0.6f) else Neon.Line) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Catalog ${r.wguCatalog}", color = Neon.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        if (added + removed.size > 0) "$added added · ${removed.size} removed since you started tracking"
                        else "Checked every Sunday. No changes so far.",
                        color = if (added + removed.size > 0) Neon.Amber else Neon.Muted, fontSize = 12.sp,
                    )
                }
                Text(if (open) "Hide ▲" else "Show ▼", color = Neon.Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { open = !open }.padding(6.dp))
            }
            if (open) {
                Spacer(Modifier.height(8.dp))
                (active + removed).groupBy { if (it.flag == "removed") 99 else it.term }.toSortedMap().forEach { (term, list) ->
                    Text(if (term == 99) "REMOVED" else "TERM $term", color = if (term == 99) Neon.Red else Neon.Muted, fontFamily = Mono, fontSize = 10.sp,
                        letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    list.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                c.name, color = if (c.flag == "removed") Neon.Muted else Neon.Text, fontSize = 13.sp, modifier = Modifier.weight(1f),
                                textDecoration = if (c.flag == "removed") androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            )
                            if (c.flag == "new") { Pill("NEW", Neon.Amber); Spacer(Modifier.width(6.dp)) }
                            WguPlan.studyComTransfers[c.name]?.let { Pill(it.uppercase(), Neon.Green); Spacer(Modifier.width(6.dp)) }
                            Text("${c.cu} CU", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                        }
                    }
                }
                Text("Study.com = planned transfer. Confirm every transfer with a WGU enrollment counselor.",
                    color = Neon.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

// ---------- settings ----------

@Composable
private fun SettingsTab(r: Roadmap, today: LocalDate, refresh: () -> Unit, change: ((Roadmap) -> Unit) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var credly by remember(r.credlyUser) { mutableStateOf(r.credlyUser) }
    var feed by remember(r.feedUrl) { mutableStateOf(r.feedUrl) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            Backup.remember(ctx, uri)
            status = if (Backup.write(ctx, uri)) "Backed up. It will update itself every Sunday." else "Backup failed."
            refresh()
        }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { status = if (Backup.restore(ctx, uri)) "Progress restored." else "That file isn't a Cloud Roadmap backup."; refresh() }
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Neon.Cyan, unfocusedBorderColor = Neon.Line,
        focusedTextColor = Neon.Text, unfocusedTextColor = Neon.Text,
    )

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Neon.Text) }
                Text("Settings", color = Neon.Text, fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), glow = Neon.Cyan.copy(alpha = 0.35f)) {
                Column(Modifier.padding(16.dp)) {
                    Label("Sync")
                    Text("Credly badges, weekly feed and running-late check, right now.", color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                    SmallButton(if (busy) "Checking…" else "Check now", true, Neon.Cyan, Icons.Filled.Refresh) {
                        if (busy) return@SmallButton
                        busy = true; status = "Checking…"
                        scope.launch {
                            val lines = withContext(Dispatchers.IO) {
                                runCatching { Sync.run(ctx, manual = true) }.getOrElse { listOf("Error: ${it.message}") }
                            }
                            status = lines.ifEmpty { listOf("Everything is up to date.") }.joinToString("\n")
                            busy = false
                            refresh()
                        }
                    }
                    if (status.isNotBlank()) Text(status, color = Neon.Text, fontSize = 13.sp, fontFamily = Mono, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth(), glow = Neon.Green.copy(alpha = 0.35f)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("Backup", Neon.Green)
                    Text(
                        "Android backs up your progress to your Google account automatically. For extra safety, save a backup file " +
                            "(Google Drive works). The app updates that file every Sunday.",
                        color = Neon.Muted, fontSize = 13.sp,
                    )
                    Text(
                        if (r.lastBackup.isBlank()) "No backup file yet" else "Last backup: ${fmt(LocalDate.parse(r.lastBackup))}" + if (Backup.uri(ctx) != null) " · weekly auto-backup on" else "",
                        color = if (r.lastBackup.isBlank()) Neon.Amber else Neon.Green, fontSize = 12.sp, fontFamily = Mono,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallButton("Back up now", true, Neon.Green) {
                            if (Backup.uri(ctx) != null && Backup.write(ctx)) { status = "Backed up."; refresh() }
                            else saveFile.launch("cloud-roadmap-backup.json")
                        }
                        SmallButton("Restore", false, Neon.Muted) { openFile.launch(arrayOf("application/json", "*/*")) }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("Connections", Neon.Violet)
                    OutlinedTextField(credly, { credly = it.trim() }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Credly username") }, colors = fieldColors)
                    OutlinedTextField(feed, { feed = it.trim() }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Weekly feed link") }, colors = fieldColors)
                    SmallButton("Save", true, Neon.Violet) { change { it.credlyUser = credly; it.feedUrl = feed }; status = "Saved." }
                }
            }
        }
        item {
            var token by remember { mutableStateOf(Cloud.token(ctx)) }
            var cloudStatus by remember { mutableStateOf(Cloud.status(ctx)) }
            val scope = rememberCoroutineScope()
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("Sync with Windows", Neon.Cyan)
                    Text("Paste your GitHub token for the private repo jmiller-sec/cloud-roadmap. It stays on this phone.", color = Neon.Muted, fontSize = 12.sp)
                    OutlinedTextField(token, { token = it.trim() }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("GitHub token") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), colors = fieldColors)
                    Text(cloudStatus, color = if (cloudStatus.startsWith("Synced")) Neon.Green else Neon.Amber, fontSize = 12.sp, fontFamily = Mono)
                    SmallButton("Save + sync now", true, Neon.Cyan) {
                        Cloud.setToken(ctx, token); cloudStatus = "Syncing…"
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) { cloudStatus = Cloud.syncNow(ctx); refresh() }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Label("Rules", Neon.Green)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        Neon.Green to "Finish ≤ 7 days early or late → rest of the stage shifts automatically",
                        Neon.Amber to "More than 7 days → the app asks first",
                        Neon.Red to "Running late → extended once by up to 7 days, then it asks",
                        Neon.Violet to "Locked dates never move. You get a warning instead",
                        Neon.Cyan to "Sunday 9am → weekly check + notification",
                    ).forEach { (c, t) ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                            Spacer(Modifier.width(10.dp))
                            Text(t, color = Neon.Text.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            SmallButton(if (confirmReset) "Tap again to erase all progress" else "Reset plan", false, if (confirmReset) Neon.Red else Neon.Muted) {
                if (!confirmReset) confirmReset = true else {
                    confirmReset = false
                    Store.save(ctx, Roadmap(credlyUser = r.credlyUser, feedUrl = r.feedUrl)); refresh(); status = "Plan reset."
                }
            }
            Text("CLOUD ROADMAP v5.5 · ${fmt(today).uppercase()}", color = Neon.Muted, fontSize = 10.sp, fontFamily = Mono,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 18.dp))
        }
    }
}


@Composable
private fun TimerRing(fraction: Float, color: Color, running: Boolean, content: @Composable () -> Unit) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900))
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(1400), androidx.compose.animation.core.RepeatMode.Reverse), label = "p")
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = 10.dp.toPx()
            if (running) drawCircle(color.copy(alpha = pulse * 0.25f), radius = size.minDimension / 2)
            drawArc(Neon.Line, -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(color.copy(alpha = 0.4f), color, color.copy(alpha = 0.4f))), -90f, 360f * f, false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        content()
    }
}

@Composable
private fun ConfettiBurst(trigger: Int) {
    if (trigger == 0) return
    val anim = remember(trigger) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(trigger) { anim.animateTo(1f, tween(1600)) }
    val parts = remember(trigger) {
        val rnd = kotlin.random.Random(trigger)
        List(80) { Triple(rnd.nextFloat() * 360f, 0.35f + rnd.nextFloat() * 0.65f, listOf(Neon.Cyan, Neon.Green, Neon.Amber, Neon.Violet)[rnd.nextInt(4)]) }
    }
    val p = anim.value
    if (p >= 1f) return
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val c = androidx.compose.ui.geometry.Offset(size.width / 2, size.height * 0.35f)
        parts.forEach { (ang, speed, col) ->
            val r = speed * p * size.minDimension * 0.6f
            val rad = Math.toRadians(ang.toDouble())
            drawCircle(col.copy(alpha = 1f - p), radius = 7f * (1f - p * 0.5f),
                center = androidx.compose.ui.geometry.Offset(c.x + (kotlin.math.cos(rad) * r).toFloat(), c.y + (kotlin.math.sin(rad) * r).toFloat() + p * p * 260f))
        }
    }
}
