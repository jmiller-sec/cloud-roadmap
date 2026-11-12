package com.josephmiller.cloudroadmap.desktop

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.josephmiller.cloudroadmap.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val shortFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun Page(title: String, sub: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentPadding = PaddingValues(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header(title, sub) }
        content()
    }
}

@Composable
private fun statusColor(s: Status) = when (s) { Status.DONE -> Neon.Green; Status.WORKING -> Neon.Amber; Status.TODO -> Neon.Muted }

// ===================== Roadmap =====================
@Composable
fun RoadmapScreen(app: AppState) {
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    val r = app.r; val today = LocalDate.now()
    Page("Roadmap", "Help desk → SOC → cloud security") {
        if (r.proposals.isNotEmpty()) item {
            Panel(Modifier.fillMaxWidth(), glow = Neon.Amber) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Needs your OK", Neon.Amber)
                    r.proposals.toList().forEach { p ->
                        Text(p.text, color = Neon.Text, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { app.change { it.approve(p.id, today) } }) { Text("Move dates") }
                            OutlinedButton(onClick = { app.change { it.keep(p.id, today) } }) { Text("Keep dates") }
                        }
                    }
                }
            }
        }
        Plan.stages.forEach { st ->
            val its = r.items.filter { it.stage == st.id }
            if (its.isEmpty()) return@forEach
            item {
                val done = its.count { it.status == Status.DONE }
                Panel(Modifier.fillMaxWidth().animateContentSize()) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(st.title, color = Neon.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("$done/${its.size}", color = Neon.Muted, fontFamily = Mono, fontSize = 12.sp)
                        }
                        Bar(done.toFloat() / its.size, Neon.Cyan, Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        its.forEach { i ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(i.title, color = if (i.status == Status.DONE) Neon.Muted else Neon.Text, fontSize = 14.sp)
                                    Text("${i.start.format(shortFmt)} → ${i.end.format(shortFmt)}" + if (i.locked) "  🔒" else "", color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                                }
                                Pill(i.status.name.lowercase(), statusColor(i.status))
                                Spacer(Modifier.width(8.dp))
                                when (i.status) {
                                    Status.TODO -> TextButton(onClick = { app.change { it.markWorking(i.id, today) } }) { Text("Working on it") }
                                    Status.WORKING -> TextButton(onClick = { app.change { it.markDone(i.id, today, today) } }) { Text("Done", color = Neon.Green) }
                                    Status.DONE -> TextButton(onClick = { app.change { it.undo(i.id, today) } }) { Text("Undo", color = Neon.Muted) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================== Projects =====================
@Composable
fun ProjectsScreen(app: AppState) {
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    val r = app.r; val today = LocalDate.now()
    val current = Projects.current(r)
    val open = StudyPlan.projectsOpen(today)
    Page("Projects", if (open) "One at a time, Saturdays" else "Locked until Sat May 1, after TryHackMe SOC Level 1") {
        listOf("Beginner", "Medium", "Expert").forEach { lv ->
            item { Label(lv, when (lv) { "Beginner" -> Neon.Green; "Medium" -> Neon.Amber; else -> Neon.Red }) }
            Projects.all.filter { it.level == lv }.forEach { p ->
                item {
                    val isDone = Projects.isDone(r, p)
                    val isNow = p == current
                    var expanded by remember(p.id) { mutableStateOf(isNow) }
                    Panel(Modifier.fillMaxWidth().animateContentSize(), glow = if (isNow) Neon.Violet else if (isDone) Neon.Green else Neon.Line) {
                        Column(Modifier.padding(18.dp)) {
                            Row(Modifier.clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                                Text(p.title, color = if (isDone) Neon.Green else Neon.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (!isDone && !isNow) Icon(Icons.Filled.Lock, null, tint = Neon.Muted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp)); Pill(if (isDone) "done" else if (isNow) "next" else p.target, if (isDone) Neon.Green else if (isNow) Neon.Violet else Neon.Muted)
                            }
                            if (expanded) {
                                Text(p.why, color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                                p.steps.forEachIndexed { n, st ->
                                    val task = DayTask("lab:${p.id}:$n", "lab", "", "", p.id)
                                    val sd = task.key in r.studyDone
                                    val lock = r.projectLock(task, today)
                                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${n + 1}. $st", color = if (sd) Neon.Muted else Neon.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Spacer(Modifier.width(10.dp))
                                        CheckBubble(sd, lock != null, Neon.Violet, 32.dp) { if (lock == null || sd) app.change { it.toggleTask(task, today) } }
                                    }
                                }
                                if (p.parts.isNotEmpty()) {
                                    val got = p.parts.indices.count { Projects.partKey(p, it) in r.studyDone }
                                    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("GEAR LIST · $got/${p.parts.size}", color = Neon.Teal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("~$${Projects.partsLeftCost(r, p)} left", color = Neon.Muted, fontSize = 11.sp)
                                    }
                                    p.parts.forEachIndexed { n, part ->
                                        val have = Projects.partKey(p, n) in r.studyDone
                                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f).clickable { app.open(part.url) }) {
                                                Text(part.name, color = if (have) Neon.Muted else Neon.Text, fontSize = 13.sp)
                                                Text(part.why, color = Neon.Muted, fontSize = 11.sp)
                                                if (part.buy.isNotBlank() && !have) Text("When to buy: ${part.buy}", color = Neon.Teal, fontSize = 11.sp)
                                            }
                                            Text("$${part.price}", color = if (have) Neon.Muted else Neon.Teal, fontSize = 13.sp)
                                            Spacer(Modifier.width(10.dp))
                                            CheckBubble(have, false, Neon.Teal, 28.dp) { app.change { Projects.togglePart(it, p, n) } }
                                        }
                                    }
                                    Text("Click an item to look it up. Prices are estimates. Don't buy before the project unlocks.", color = Neon.Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================== Gear =====================
@Composable
fun GearScreen(app: AppState) {
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    val r = app.r
    val withParts = Projects.all.filter { it.parts.isNotEmpty() }
    val left = withParts.sumOf { Projects.partsLeftCost(r, it) }
    Page("Gear", "Shopping list · ~$$left left to buy") {
        item {
            Panel(Modifier.fillMaxWidth(), glow = Neon.Amber) {
                Text("Plan: one Prime Day order in July 2027, a couple of weeks before the build. Don't buy before the project unlocks.",
                    color = Neon.Text, fontSize = 14.sp, modifier = Modifier.padding(18.dp))
            }
        }
        withParts.forEach { p ->
            item {
                Panel(Modifier.fillMaxWidth(), glow = Neon.Teal) {
                    Column(Modifier.padding(18.dp)) {
                        val got = p.parts.indices.count { Projects.partKey(p, it) in r.studyDone }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.title, color = Neon.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Pill("$got/${p.parts.size} · ~$${Projects.partsLeftCost(r, p)} left", Neon.Teal)
                        }
                        Text("Build target ${p.target}", color = Neon.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        p.parts.forEachIndexed { n, part ->
                            val have = Projects.partKey(p, n) in r.studyDone
                            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).clickable { app.open(part.url) }) {
                                    Text(part.name, color = if (have) Neon.Muted else Neon.Text, fontSize = 14.sp)
                                    Text(part.why, color = Neon.Muted, fontSize = 12.sp)
                                    if (part.buy.isNotBlank() && !have) Text("When to buy: ${part.buy}", color = Neon.Teal, fontSize = 12.sp)
                                }
                                Text("$${part.price}", color = if (have) Neon.Muted else Neon.Teal, fontSize = 14.sp)
                                Spacer(Modifier.width(12.dp))
                                CheckBubble(have, false, Neon.Teal, 30.dp) { app.change { Projects.togglePart(it, p, n) } }
                            }
                        }
                        Text("Click an item to look it up. Prices are estimates.", color = Neon.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}

// ===================== Videos =====================
@Composable
fun VideosScreen(app: AppState) {
    Page("Videos", "Free creators, sorted by who made them") {
        items(Videos.creators) { c ->
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(c.name, color = Neon.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(c.focus, color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        c.links.forEach { (label, url) -> OutlinedButton(onClick = { app.open(url) }) { Text(label) } }
                    }
                    app.r.findings.filter { app.r.tabOf(it) == "videos" && c.keywords.any { k -> k.lowercase() in it.title.lowercase() } }.takeLast(5).forEach { f ->
                        Text("▶ ${f.title}", color = Color(0xFF4DA3FF), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp).clickable { app.open(f.link) })
                    }
                }
            }
        }
    }
}

// ===================== Jobs / Updates =====================
@Composable
fun FindingsScreen(app: AppState, tab: String, title: String, sub: String) {
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    val r = app.r; val today = LocalDate.now()
    val list = r.findings.filter { r.tabOf(it) == tab && it.state != "dismissed" }.reversed()
    Page(title, if (r.lastFeedSummary.isNotBlank()) "Latest: ${r.lastFeedSummary}" else sub) {
        if (list.isEmpty()) item { Text("Nothing yet. The Sunday and midweek checks fill this in.", color = Neon.Muted) }
        items(list, key = { it.id }) { f ->
            val c = if (f.type == "change") Neon.Amber else Neon.Cyan
            Panel(Modifier.fillMaxWidth(), glow = if (f.state == "new") c else Neon.Line) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label(if (f.type == "change") "Change" else f.category.ifBlank { "Info" }, c)
                        Spacer(Modifier.weight(1f)); Text(f.week, color = Neon.Muted, fontSize = 11.sp, fontFamily = Mono)
                    }
                    Text(f.title, color = Neon.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    Text(f.detail, color = Neon.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (f.link.isNotBlank()) OutlinedButton(onClick = { app.open(f.link) }) { Text("Open") }
                        if (f.type == "change" && f.action != null && f.state != "approved") Button(onClick = { app.change { it.approveFinding(f.id, today) } }) { Text("Apply") }
                        TextButton(onClick = { app.change { it.dismissFinding(f.id) } }) { Text("Dismiss", color = Neon.Muted) }
                    }
                }
            }
        }
    }
}

// ===================== WGU =====================
@Composable
fun WguScreen(app: AppState) {
    @Suppress("UNUSED_VARIABLE") val t = app.tick
    val r = app.r
    Page("WGU", "B.S. Cybersecurity & Information Assurance · catalog ${r.wguCatalog} · starts Aug 1, 2027") {
        r.wguCourses.groupBy { it.term }.toSortedMap().forEach { (term, cs) ->
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Label("Term $term · ${cs.sumOf { it.cu }} CUs")
                        cs.forEach { c ->
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(c.name, color = Neon.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (c.flag.isNotBlank()) { Pill(c.flag, if (c.flag == "new") Neon.Amber else Neon.Red); Spacer(Modifier.width(8.dp)) }
                                Text("${c.cu} CU", color = Neon.Muted, fontFamily = Mono, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        val wf = r.findings.filter { r.tabOf(it) == "wgu" && it.state != "dismissed" }.takeLast(6).reversed()
        if (wf.isNotEmpty()) item { Label("WGU news", Neon.Amber) }
        items(wf, key = { it.id }) { f ->
            Panel(Modifier.fillMaxWidth().clickable { if (f.link.isNotBlank()) app.open(f.link) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(f.title, color = Neon.Text, fontWeight = FontWeight.SemiBold)
                    Text(f.detail, color = Neon.Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

// ===================== Settings =====================
@Composable
fun SettingsScreen(app: AppState) {
    var token by remember { mutableStateOf(app.token()) }
    val scope = rememberCoroutineScope()
    Page("Settings", "Cloud Roadmap for Windows · v${app.version}") {
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Sync with your phone")
                    Text("Paste your GitHub token for the private repo ${CloudSync.DEFAULT_REPO}. It's saved only on this PC.", color = Neon.Muted, fontSize = 13.sp)
                    OutlinedTextField(token, { token = it.trim() }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("GitHub token") }, visualTransformation = PasswordVisualTransformation())
                    Text(app.syncStatus, color = if (app.syncStatus.startsWith("Synced")) Neon.Green else Neon.Amber, fontFamily = Mono, fontSize = 12.sp)
                    Button(onClick = { app.setToken(token); scope.launch(kotlinx.coroutines.Dispatchers.IO) { app.syncNow() } }) { Text("Save + sync now") }
                }
            }
        }
        item {
            var checking by remember { mutableStateOf(false) }
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("App updates", Neon.Green)
                    val up = app.update
                    Text(
                        app.updating ?: if (up != null) "Version ${up.first} is ready." else if (checking) "Checking..." else "You're on the latest version (v${app.version}). It checks every hour on its own.",
                        color = if (up != null) Neon.Green else Neon.Muted, fontSize = 13.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (up != null) Button(onClick = { app.installUpdate { kotlin.system.exitProcess(0) } }, enabled = app.updating == null) { Text("Update now") }
                        OutlinedButton(onClick = { checking = true; scope.launch(kotlinx.coroutines.Dispatchers.IO) { app.checkUpdate(); checking = false } }) { Text("Check for updates") }
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Label("Screenshots", Neon.Amber)
                    Text("Capture buttons save into Documents\\CloudRoadmap\\b01, b02… Upload each week's folder to jmiller-sec-labs/packet-tracer-labs.", color = Neon.Muted, fontSize = 13.sp)
                    OutlinedButton(onClick = { app.openFolder(app.shotsRoot) }) { Text("Open screenshots folder") }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label("Rules", Neon.Green)
                    listOf(
                        "Study Tue + Thu evenings and Saturday. Sun, Mon, Wed, Fri are off.",
                        "Timer: 50 on / 10 off. Cap 2 h Tue/Thu, 6 h Sat. Override = 5/5 quiz, once a day.",
                        "Two low energy checks in a row → Saturday drops to 2 bundles. Three low weeks → rest week.",
                        "Missed days never stack. Everything just slides.",
                    ).forEach { Text("• $it", color = Neon.Text, fontSize = 13.sp) }
                }
            }
        }
    }
}
