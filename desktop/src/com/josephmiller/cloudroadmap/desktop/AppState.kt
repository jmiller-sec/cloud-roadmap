package com.josephmiller.cloudroadmap.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.josephmiller.cloudroadmap.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import javax.imageio.ImageIO

/** Everything the Windows app keeps: the plan (synced), the token (local only), sync + timer jobs. */
class AppState {
    val version = System.getProperty("app.version") ?: "dev"
    private val dir = File(System.getProperty("user.home"), ".cloudroadmap").apply { mkdirs() }
    private val stateFile = File(dir, "state.json")
    private val tokenFile = File(dir, "token.txt")
    val shotsRoot = File(System.getProperty("user.home"), "Documents/CloudRoadmap").apply { mkdirs() }

    var r: Roadmap = load()
        private set
    /** Bumps on every change so the UI redraws. */
    var tick by mutableIntStateOf(0)
    var syncStatus by mutableStateOf(if (token().isBlank()) "Not connected" else "Ready")
    var update by mutableStateOf<Pair<String, String>?>(null) // (tag, url)
    var confetti by mutableIntStateOf(0)
    /** For design previews only. */
    var dateOverride: LocalDate? = null
    fun today(): LocalDate = dateOverride ?: LocalDate.now()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    private fun load(): Roadmap = runCatching { Roadmap.fromJson(stateFile.readText()) }
        .getOrElse { Roadmap().also { it.applyOneTimeFixes(LocalDate.now()) } }

    @Synchronized private fun save(touch: Boolean) {
        if (touch) r.updatedAt = System.currentTimeMillis()
        stateFile.writeText(r.toJson())
    }

    fun token() = runCatching { tokenFile.readText().trim() }.getOrDefault("")
    fun setToken(t: String) { tokenFile.writeText(t.trim()) }

    /** A real change by Joseph: save, redraw, push to the phone soon. */
    fun change(block: (Roadmap) -> Unit) {
        val before = Bundles.done(r)
        block(r)
        save(touch = true)
        if (Bundles.done(r) > before) confetti++
        tick++
        syncSoon()
    }

    fun syncSoon(delayMs: Long = 8000) {
        if (token().isBlank()) return
        syncJob?.cancel()
        syncJob = scope.launch { delay(delayMs); syncNow() }
    }

    suspend fun syncNow() {
        val t = token()
        if (t.isBlank()) { syncStatus = "Not connected"; return }
        syncStatus = "Syncing…"
        syncStatus = runCatching {
            val merged = CloudSync.sync(r, t)
            synchronized(this) { r = merged; save(touch = false) }
            tick++
            "Synced " + LocalTime.now().withNano(0).withSecond(0)
        }.getOrElse { "Sync failed: ${it.message}" }
    }

    /** Pull from the phone every few minutes, check for app updates once an hour. */
    fun startBackground() {
        scope.launch { while (isActive) { syncNow(); delay(4 * 60_000L) } }
        scope.launch { while (isActive) { checkUpdate(); delay(60 * 60_000L) } }
        scope.launch { timerWatch() }
    }

    // ---------- timer pop-ups ----------
    private suspend fun timerWatch() {
        var warnedBlock = 0L; var warnedCap = ""
        while (true) {
            delay(15_000)
            val now = System.currentTimeMillis(); val today = LocalDate.now()
            if (r.timerStart > 0 && r.timerDay == today.toString()) {
                if (Burnout.runningSessionMin(r, today, now) >= Burnout.WORK_MIN && warnedBlock != r.timerStart) {
                    warnedBlock = r.timerStart
                    change { Burnout.stopTimer(it, now) }
                    if (Burnout.capReached(r, today, now)) notify("Done for today", "You hit today's study cap. Close the laptop. Nothing piles up.")
                    else notify("Break time: 10 minutes", "50 minutes done. Stand up, water, walk. Then hit Start again.")
                }
                if (Burnout.capReached(r, today, now) && warnedCap != today.toString()) {
                    warnedCap = today.toString()
                    change { Burnout.stopTimer(it, now) }
                    notify("Done for today", "You hit today's study cap. Whatever's left waits for next time.")
                }
            }
        }
    }

    private val tray: TrayIcon? by lazy {
        runCatching {
            if (!SystemTray.isSupported()) return@runCatching null
            val img = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().apply {
                    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    color = java.awt.Color(0, 229, 255); fillOval(2, 2, 28, 28)
                    color = java.awt.Color(6, 11, 18); fillOval(9, 9, 14, 14); dispose()
                }
            }
            TrayIcon(img, "Cloud Roadmap").also { it.isImageAutoSize = true; SystemTray.getSystemTray().add(it) }
        }.getOrNull()
    }

    fun notify(title: String, text: String) { tray?.displayMessage(title, text, TrayIcon.MessageType.INFO) }

    // ---------- links, folders, screenshots ----------
    fun open(url: String) = runCatching { Desktop.getDesktop().browse(URI(url)) }
    fun folder(b: StudyBundle) = File(shotsRoot, "b%02d".format(b.n)).apply { mkdirs() }
    fun openFolder(f: File) = runCatching { Desktop.getDesktop().open(f) }

    fun shotKey(b: StudyBundle, s: Section, i: Int) = "shot:${b.n}:${s.id}:$i"

    /** Hide the app, capture the whole screen, save it into the bundle's folder with a clear name, bring the app back. */
    fun captureScreen(b: StudyBundle, s: Section, i: Int, hide: (Boolean) -> Unit) {
        scope.launch {
            withContext(Dispatchers.Main) { hide(true) }
            delay(650)
            val file = runCatching {
                val bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
                val img = Robot().createScreenCapture(bounds)
                val slug = s.shots[i].lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40)
                File(folder(b), "b%02d-%s-%d-%s.png".format(b.n, s.id, i + 1, slug)).also { ImageIO.write(img, "png", it) }
            }
            withContext(Dispatchers.Main) {
                hide(false)
                file.onSuccess { change { r -> r.studyDone.add(shotKey(b, s, i)) }; notify("Screenshot saved", it.name) }
                    .onFailure { notify("Screenshot failed", it.message ?: "") }
            }
        }
    }

    // ---------- updates ----------
    var updating by mutableStateOf<String?>(null)
    private var msi: AppUpdate.Msi? = null

    fun checkUpdate() {
        val t = token(); if (t.isBlank()) return
        val m = AppUpdate.latestMsi(t) ?: return
        if (AppUpdate.newer(m.version, version)) { msi = m; update = m.version to "" }
    }

    /** One click: download the new installer, close, install over this version, reopen. Data in ~/.cloudroadmap stays. */
    fun installUpdate(exit: () -> Unit) {
        val m = msi ?: return
        if (updating != null) return
        updating = "Downloading ${m.version}..."
        scope.launch(Dispatchers.IO) {
            runCatching {
                val dir = File(System.getProperty("java.io.tmpdir"), "CloudRoadmapUpdate")
                val file = File(dir, m.name)
                AppUpdate.downloadMsi(token(), m, file)
                val exe = ProcessHandle.current().info().command().orElse("")
                val bat = File(dir, "update.bat")
                bat.writeText(buildString {
                    appendLine("@echo off")
                    appendLine("timeout /t 2 /nobreak >nul")
                    appendLine("msiexec /i \"${file.absolutePath}\" /passive /norestart")
                    if (exe.endsWith(".exe", true) && !exe.endsWith("java.exe", true) && !exe.endsWith("javaw.exe", true))
                        appendLine("start \"\" \"$exe\"")
                })
                ProcessBuilder("cmd", "/c", "start", "\"\"", "/min", bat.absolutePath).start()
            }.onSuccess {
                withContext(Dispatchers.Main) { exit() }
            }.onFailure {
                withContext(Dispatchers.Main) { updating = null; notify("Update failed", it.message ?: "") }
            }
        }
    }
}
