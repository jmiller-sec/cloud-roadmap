package com.josephmiller.cloudroadmap.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.josephmiller.cloudroadmap.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

enum class Tab(val label: String, val icon: ImageVector) {
    WTD("WTD", Icons.Filled.TaskAlt), ROADMAP("Roadmap", Icons.Filled.Route), PROJECTS("Projects", Icons.Filled.Build),
    VIDEOS("Videos", Icons.Filled.PlayCircle), JOBS("Jobs", Icons.Filled.Work), WGU("WGU", Icons.Filled.School),
    GEAR("Gear", Icons.Filled.ShoppingCart),
    UPDATES("Updates", Icons.Filled.Notifications), SETTINGS("Settings", Icons.Filled.Settings),
}

fun main() = application {
    val app = remember { AppState().also { it.startBackground() } }
    val win = rememberWindowState(size = DpSize(1360.dp, 880.dp), position = WindowPosition(Alignment.Center))
    Window(onCloseRequest = ::exitApplication, state = win, title = "Cloud Roadmap") {
        MaterialTheme(colorScheme = darkColorScheme(primary = Neon.Cyan, background = Neon.Bg, surface = Neon.Panel)) {
            Shell(app, { hide -> win.isMinimized = hide })
        }
    }
}

@Composable
fun SnapShell(app: AppState, start: Tab) = Shell(app, {}, start)

@Composable
fun Shell(app: AppState, hide: (Boolean) -> Unit, start: Tab = Tab.WTD) {
    var tab by remember { mutableStateOf(start) }
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    Box(Modifier.fillMaxSize()) {
        AnimatedBackground()
        Row(Modifier.fillMaxSize()) {
            NavRail(tab, app) { tab = it }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Crossfade(tab, animationSpec = tween(250)) { tb ->
                    when (tb) {
                        Tab.WTD -> WtdWide(app, hide)
                        Tab.ROADMAP -> RoadmapScreen(app)
                        Tab.PROJECTS -> ProjectsScreen(app)
                        Tab.VIDEOS -> VideosScreen(app)
                        Tab.JOBS -> FindingsScreen(app, "jobs", "Jobs", "Help desk and IT openings the weekly check found.")
                        Tab.WGU -> WguScreen(app)
                        Tab.GEAR -> GearScreen(app)
                        Tab.UPDATES -> FindingsScreen(app, "other", "Updates", "Cert, price and resource news from the weekly checks.")
                        Tab.SETTINGS -> SettingsScreen(app)
                    }
                }
                ConfettiBurst(app.confetti)
            }
        }
    }
}

@Composable
fun NavRail(current: Tab, app: AppState, onPick: (Tab) -> Unit) {
    Column(
        Modifier.width(96.dp).fillMaxHeight().background(Neon.Panel.copy(alpha = 0.85f)).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Neon.Cyan, Neon.Violet))), contentAlignment = Alignment.Center) {
            Text("CR", color = Neon.Bg, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.height(14.dp))
        Tab.entries.forEach { t ->
            val sel = t == current
            val bg by animateColorAsState(if (sel) Neon.Cyan.copy(alpha = 0.14f) else Color.Transparent, tween(200))
            val fg by animateColorAsState(if (sel) Neon.Cyan else Neon.Muted, tween(200))
            Column(
                Modifier.width(80.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable { onPick(t) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BadgedBox(badge = { if (t == Tab.UPDATES && app.r.findings.any { it.state == "new" }) Badge(containerColor = Neon.Amber) }) {
                    Icon(t.icon, t.label, tint = fg, modifier = Modifier.size(24.dp))
                }
                Text(t.label, color = fg, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        // One click: pull from + push to the phone. (It also syncs by itself every few minutes.)
        val ok = app.syncStatus.startsWith("Synced")
        val busy = app.syncStatus == "Syncing…"
        val spin by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)))
        val scope = rememberCoroutineScope()
        Column(
            Modifier.width(80.dp).clip(RoundedCornerShape(14.dp)).clickable {
                if (app.token().isBlank()) onPick(Tab.SETTINGS) else scope.launch(kotlinx.coroutines.Dispatchers.IO) { app.syncNow() }
            }.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Sync, "Sync", tint = if (ok) Neon.Green else if (busy) Neon.Cyan else Neon.Amber,
                modifier = Modifier.size(26.dp).then(if (busy) Modifier.rotate(spin) else Modifier))
            Text(if (busy) "syncing" else if (ok) "synced" else if (app.token().isBlank()) "connect" else "sync", color = Neon.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun Header(title: String, sub: String, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Neon.Text, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(sub, color = Neon.Muted, fontSize = 13.sp, fontFamily = Mono)
        }
        right()
    }
}

// ===================== WTD (wide Saturday mode) =====================

@Composable
fun WtdWide(app: AppState, hide: (Boolean) -> Unit) {
    val r = app.r
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(15_000); nowMs = System.currentTimeMillis() } }
    val today = app.today()
    val tasks = StudyPlan.tasksFor(today, r)
    val bundles = StudyPlan.bundlesToday(today, r)
    val others = tasks.filter { it.stream != "bundle" }
    val real = tasks.filter { it.stream != "rest" }
    val doneN = real.count { r.isDone(it) }
    val mode = Burnout.mode(r, today)
    val capped = Burnout.capReached(r, today, nowMs)
    val late = today.dayOfWeek in listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY) && LocalTime.now().hour >= Burnout.LATE_HOUR &&
        bundles.none { "$today|b:${it.n}" in r.studyDone }
    var quiz by remember { mutableStateOf<Pair<String, List<Q>>?>(null) }
    var pendingWarm by remember { mutableStateOf<DayTask?>(null) }
    var noteFor by remember { mutableStateOf<Pair<StudyBundle, DayTask>?>(null) }

    var pendingGate by remember { mutableStateOf<StudyBundle?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    val needsCheckin = Gate.needsCheckin(r, today) && today == LocalDate.now()
    fun startTimer() {
        if (needsCheckin) { hint = "Check in first (right side): how do you feel right now?"; return }
        if (!capped) app.change { Burnout.startTimer(it, today, System.currentTimeMillis()) }
    }
    fun tap(b: StudyBundle, task: DayTask) {
        if (task.key in r.studyDone) return app.change { it.toggleTask(task, today) }
        if (capped || late) return
        if (needsCheckin) { hint = "Check in first (right side): how do you feel right now?"; return }
        val sec = task.key.substringAfterLast(":")
        if (sec != "warm" && !Gate.passed(r, b.n)) { hint = "Locked. Pass the recall check at the top of bundle ${b.n} first."; return }
        if (sec == "warm" && Gate.needsReview(r, b.n)) {
            val v = Gate.reviewVideo(b.n)
            app.change { Gate.markReviewed(it, b.n) }
            v?.let { app.open(it.url) }
            hint = "Two misses: rewatch \"${v?.title ?: "the last lesson"}\", then click the recall check again."; return
        }
        hint = null
        if (r.timerStart <= 0) startTimer()
        when (sec) {
            "warm" -> { pendingWarm = task; pendingGate = b; quiz = "gate" to Gate.questions(b.n, today, r) }
            "note" -> noteFor = b to task
            else -> app.change { it.toggleTask(task, today) }
        }
    }

    Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        // ---- left: the work ----
        Column(Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Header("What to do", today.format(dayFmt)) {
                Pill(when (mode) { Mode.C -> "Plan C"; Mode.B -> "Plan B"; Mode.REST -> "Rest week" }, when (mode) { Mode.C -> Neon.Cyan; Mode.B -> Neon.Amber; Mode.REST -> Neon.Violet })
                Spacer(Modifier.width(8.dp))
                Pill("CS1015 ${Bundles.done(r)}/${Bundles.all.size}", Neon.Green)
            }
            AnimatedVisibility(app.update != null) {
                app.update?.let { (tag, _) ->
                    Panel(Modifier.fillMaxWidth().padding(bottom = 14.dp).clickable { app.installUpdate { kotlin.system.exitProcess(0) } }, glow = Neon.Green) {
                        Text(app.updating ?: "✨ Update $tag is ready. Click to update (the app closes, updates, and reopens).", color = Neon.Green, modifier = Modifier.padding(14.dp))
                    }
                }
            }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(if (real.isEmpty()) "Day off. Rest up." else if (doneN == real.size) "All done for today 💪" else "$doneN of ${real.size} steps done",
                        color = if (real.isNotEmpty() && doneN == real.size) Neon.Green else Neon.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    if (real.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Bar(doneN.toFloat() / real.size, Neon.Cyan, Modifier.fillMaxWidth()) }
                    if (mode == Mode.B) Text("Your last two energy checks were low, so Saturday is 2 bundles this week.", color = Neon.Amber, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    others.forEach { o -> OtherTaskRow(app, o, today) { if (needsCheckin) "Check in first (right side): how do you feel right now?" else null } }
                    hint?.let { Text(it, color = Neon.Amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp)) }
                }
            }
            bundles.forEach { b ->
                Spacer(Modifier.height(16.dp))
                BundleCard(app, b, locked = capped || late || needsCheckin, onTap = { task -> tap(b, task) }, hide = hide)
            }
            Spacer(Modifier.height(24.dp))
        }
        // ---- right: timer, energy, week, portfolio ----
        Column(Modifier.weight(0.38f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(64.dp))
            if (Gate.isPlanned(today) && today == LocalDate.now()) CheckInCard(app, today)
            TimerCard(app, today, nowMs, capped, late, onStart = { startTimer() }, onOverride = { quiz = "override" to Quiz.pick(Burnout.currentUnit(r), 5, today, "override") })
            if (Burnout.isStudyDay(today) && real.any { it.stream != "todo" }) EnergyCard(app, today)
            WeekStrip(app, today)
            PortfolioCard(app)
        }
    }

    quiz?.let { (purpose, qs) ->
        QuizDialog(if (purpose == "override") "Prove you're sharp: 5/5 to keep going" else "Recall check: 2 of 3 to unlock", qs,
            onDone = { score ->
                if (purpose == "gate") {
                    pendingGate?.let { b ->
                        app.change { Gate.record(it, b, score, today) }
                        hint = if (score >= Gate.PASS) "$score/3. Bundle ${b.n} unlocked, go." else if (Gate.needsReview(app.r, b.n)) "$score/3. Locked. Rewatch the last lesson's video, then try again." else "$score/3. Locked. Try again with new questions."
                    }
                    pendingGate = null; pendingWarm = null; quiz = null; return@QuizDialog
                }
                if (purpose == "override") app.change { Burnout.recordOverride(it, today, score, qs.size) }
                else pendingWarm?.let { task -> app.change { it.toggleTask(task, today) } }
                pendingWarm = null; quiz = null
            }, onCancel = { pendingWarm = null; pendingGate = null; quiz = null })
    }
    noteFor?.let { (b, task) ->
        NoteDialog(b.noteHint, r.portfolio[b.n.toString()] ?: "",
            onSave = { text -> app.change { it.portfolio[b.n.toString()] = text.trim(); it.toggleTask(task, today) }; noteFor = null },
            onCancel = { noteFor = null })
    }
}

@Composable
fun OtherTaskRow(app: AppState, t: DayTask, today: LocalDate, blocked: () -> String? = { null }) {
    val done = app.r.isDone(t)
    var msg by remember(t.key) { mutableStateOf<String?>(null) }
    val c = when (t.stream) { "job" -> Color(0xFFFF8A65); "video" -> Color(0xFF4DA3FF); "thm" -> Neon.Green; "course" -> Neon.Amber; "lab" -> Neon.Violet; "todo" -> Color(0xFFFF7AC6); else -> Neon.Muted }
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Label(t.where, c)
            Text(t.task, color = if (done) Neon.Muted else Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            t.url?.let { u -> Text("▶ Open", color = c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp).clickable { app.open(u) }) }
        }
        if (t.stream != "rest") { Spacer(Modifier.width(12.dp)); CheckBubble(done, false, c) {
            val b = if (done) null else blocked()
            if (b != null) msg = b else { msg = null; app.change { it.toggleTask(t, today) } }
        } }
    }
    msg?.let { Text(it, color = Neon.Amber, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
}

@Composable
fun BundleCard(app: AppState, b: StudyBundle, locked: Boolean, onTap: (DayTask) -> Unit, hide: (Boolean) -> Unit) {
    val r = app.r
    val done = Bundles.isDone(r, b)
    val glow by animateColorAsState(if (done) Neon.Green else Color(0xFF7AA2FF).copy(alpha = 0.6f), tween(500))
    Panel(Modifier.fillMaxWidth(), glow = glow) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("Bundle ${b.n}/${Bundles.all.size} · Unit ${b.unit}", Color(0xFF7AA2FF))
                Spacer(Modifier.weight(1f))
                Text("~${b.minutes / 60}h ${b.minutes % 60}m", color = Neon.Muted, fontFamily = Mono, fontSize = 12.sp)
            }
            Text(b.title, color = if (done) Neon.Green else Neon.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 6.dp).clickable { app.openFolder(app.folder(b)) }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FolderOpen, null, tint = Neon.Muted, modifier = Modifier.size(16.dp))
                Text("  Screenshots: Documents/CloudRoadmap/b%02d (click to open)".format(b.n), color = Neon.Muted, fontSize = 12.sp)
            }
            b.sections.forEach { s ->
                val task = StudyPlan.sectionTask(b, s)
                val sDone = task.key in r.studyDone
                var open by remember(b.n, s.id) { mutableStateOf(!sDone) }
                val c = sectionColor(s.id)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("${s.title} · ${s.minutes} min", color = if (sDone) Neon.Muted else c, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { open = !open })
                        AnimatedVisibility(open && !sDone, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column {
                                s.steps.forEachIndexed { i, st -> Text("${i + 1}. $st", color = Neon.Text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp)) }
                                s.shots.forEachIndexed { i, shot ->
                                    val key = app.shotKey(b, s, i)
                                    val got = key in r.studyDone
                                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (got) "✅ $shot" else "📸 $shot", color = if (got) Neon.Green else Neon.Amber, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        OutlinedButton(onClick = { app.captureScreen(b, s, i, hide) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                                            Icon(Icons.Filled.PhotoCamera, null, Modifier.size(16.dp)); Text(if (got) "  Retake" else "  Capture", fontSize = 12.sp)
                                        }
                                    }
                                }
                                s.url?.let { u ->
                                    Text("▶ Open", color = c, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp).clickable { app.open(u) })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    val gated = s.id != "warm" && !Gate.passed(r, b.n)
                    CheckBubble(sDone, (locked || gated) && !sDone, c) { onTap(task) }
                }
            }
        }
    }
}

@Composable
fun TimerCard(app: AppState, today: LocalDate, nowMs: Long, capped: Boolean, late: Boolean, onStart: () -> Unit, onOverride: () -> Unit) {
    val r = app.r
    val cap = Burnout.capMin(today, r)
    val mins = Burnout.minutesOn(r, today, nowMs)
    val running = r.timerStart > 0
    val block = Burnout.runningSessionMin(r, today, nowMs)
    Panel(Modifier.fillMaxWidth(), glow = (if (capped) Neon.Red else if (running) Neon.Green else Neon.Line).copy(alpha = 0.7f)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Label("Burnout timer")
            Spacer(Modifier.height(14.dp))
            if (cap == 0) Text("No study today.", color = Neon.Muted)
            else {
                ProgressRing(mins.toFloat() / cap, if (capped) Neon.Red else Neon.Green, running) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${mins / 60}:%02d".format(mins % 60), color = Neon.Text, fontSize = 34.sp, fontWeight = FontWeight.Black, fontFamily = Mono)
                        Text("of ${cap / 60}h${if (cap % 60 > 0) " ${cap % 60}m" else ""}", color = Neon.Muted, fontSize = 12.sp, fontFamily = Mono)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    when {
                        capped -> "Done for today. Close the laptop: whatever's left waits."
                        running -> "Block: $block / ${Burnout.WORK_MIN} min"
                        late -> "It's after 8 pm. Today's bundle waits. Get some sleep."
                        else -> "50 on, 10 off. Starts when you check your first step."
                    }, color = if (capped) Neon.Red else Neon.Text, fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                when {
                    capped && !Burnout.overrideUsed(r, today) -> OutlinedButton(onClick = onOverride) { Text("Let me keep going (5/5 quiz)") }
                    !capped && running -> OutlinedButton(onClick = { app.change { Burnout.stopTimer(it, System.currentTimeMillis()) } }) { Text("Pause") }
                    !capped && !late -> Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.Bg)) { Text("Start") }
                }
            }
        }
    }
}

@Composable
fun CheckInCard(app: AppState, today: LocalDate) {
    val r = app.r
    val cur = Gate.checkin(r, today)
    val c = if (cur == null) Neon.Amber else when (Gate.score(r, today)) { 3 -> Neon.Green; 2 -> Neon.Cyan; 1 -> Neon.Amber; else -> Neon.Violet }
    Panel(Modifier.fillMaxWidth(), glow = c) {
        Column(Modifier.padding(20.dp)) {
            Label(if (cur == null) "Check in before you start" else "Today's check-in", c)
            Text(if (cur == null) "How do you feel right now? Nothing unlocks until you answer." else "Locked in for today.",
                color = Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1 to "😵", 2 to "😩", 3 to "😐", 4 to "🙂", 5 to "💪").forEach { (n, e) ->
                    val sel = cur == n
                    Box(Modifier.size(48.dp).clip(CircleShape).background(if (sel) c.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (sel) c else Neon.Line, CircleShape)
                        .clickable(enabled = cur == null) { app.change { Gate.setCheckin(it, today, n) } },
                        contentAlignment = Alignment.Center) { Text(e, fontSize = 22.sp) }
                }
            }
            Gate.why(r, today)?.let { Text(it, color = c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp)) }
        }
    }
}

@Composable
fun EnergyCard(app: AppState, today: LocalDate) {
    val cur = Burnout.rating(app.r, today)
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Burnout.sleepNote(app.r, today)?.let { note ->
                Label("Last night · Galaxy Watch", Neon.Violet)
                Text(note, color = Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            }
            Label("Energy check")
            Text("How do you feel after today?", color = Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1 to "😩", 2 to "😕", 3 to "😐", 4 to "🙂", 5 to "💪").forEach { (n, e) ->
                    val sel = cur == n
                    val sc by animateFloatAsState(if (sel) 1.15f else 1f, spring(dampingRatio = 0.4f))
                    Box(Modifier.size(48.dp).scale(sc).clip(CircleShape).background(if (sel) Neon.Cyan.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (sel) Neon.Cyan else Neon.Line, CircleShape).clickable { app.change { it.energy[today.toString()] = n } },
                        contentAlignment = Alignment.Center) { Text(e, fontSize = 22.sp) }
                }
            }
        }
    }
}


@Composable
fun WeekStrip(app: AppState, today: LocalDate) {
    val sat = ThmPlan.saturdayOf(today); val start = sat.minusDays(4)
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { k ->
                val day = start.plusDays(k.toLong())
                val study = StudyPlan.tasksFor(day, app.r).any { it.stream != "rest" }
                val done = app.r.dayDone(day)
                val c = when { done -> Neon.Green; !study -> Neon.Line; else -> Neon.Muted }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.dayOfWeek.name.take(3), color = if (day == today) Neon.Text else Neon.Muted, fontSize = 10.sp, fontFamily = Mono)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(32.dp).clip(CircleShape).background(if (done) Neon.Green else Color.Transparent)
                        .border(if (day == today) 2.dp else 1.dp, if (day == today) Neon.Text else c, CircleShape), contentAlignment = Alignment.Center) {
                        if (done) Icon(Icons.Filled.Check, null, tint = Neon.Bg, modifier = Modifier.size(16.dp))
                        else Text(if (study) "${day.dayOfMonth}" else "off", color = c, fontSize = 10.sp, fontFamily = Mono)
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioCard(app: AppState) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("Portfolio · ${app.r.portfolio.size} entries", Neon.Green)
            if (app.r.portfolio.isEmpty()) Text("Each bundle's 2-line note lands here. Copy them into GitHub + your resume.", color = Neon.Muted, fontSize = 13.sp)
            app.r.portfolio.entries.sortedByDescending { it.key.toIntOrNull() ?: 0 }.take(6).forEach { (k, v) ->
                Text("Bundle $k", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                Text(v, color = Neon.Text, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun QuizDialog(title: String, qs: List<Q>, onDone: (Int) -> Unit, onCancel: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    val finished = i >= qs.size
    AlertDialog(onDismissRequest = onCancel, containerColor = Neon.Panel,
        title = { Text(if (finished) "Score: $score/${qs.size}" else "$title (${i + 1}/${qs.size})", color = Neon.Text) },
        text = {
            if (!finished) Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(520.dp)) {
                val q = qs[i]
                Text(q.q, color = Neon.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                q.options.forEachIndexed { k, o ->
                    val col = when { picked == null -> Neon.Line; k == q.answer -> Neon.Green; k == picked -> Neon.Red; else -> Neon.Line }
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, col, RoundedCornerShape(10.dp))
                        .clickable(enabled = picked == null) { picked = k; if (k == q.answer) score++ }.padding(12.dp)) { Text(o, color = Neon.Text) }
                }
            } else Text(if (score == qs.size) "Perfect." else "Review what you missed next time.", color = Neon.Muted)
        },
        confirmButton = { TextButton(onClick = { if (finished) onDone(score) else if (picked != null) { i++; picked = null } }, enabled = finished || picked != null) { Text(if (finished) "Done" else "Next") } },
        dismissButton = { if (!finished) TextButton(onClick = onCancel) { Text("Cancel", color = Neon.Muted) } })
}

@Composable
fun NoteDialog(hint: String, existing: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(existing.ifBlank { hint }) }
    AlertDialog(onDismissRequest = onCancel, containerColor = Neon.Panel,
        title = { Text("Portfolio note (2 lines)", color = Neon.Text) },
        text = {
            Column(Modifier.width(520.dp)) {
                Text("Edit it to say what YOU did. This goes on GitHub + your resume.", color = Neon.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = Neon.Muted) } })
}
