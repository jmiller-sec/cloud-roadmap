package com.josephmiller.cloudroadmap.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Renders the Windows app to PNGs (no screen needed) so the design can be checked. */
fun main(args: Array<String>) {
    val app = AppState()
    app.dateOverride = java.time.LocalDate.parse("2026-12-05")
    // Sample progress: bundle 1 done, bundle 2 half done, timer running 70 min in.
    val d = app.today()
    val b1 = com.josephmiller.cloudroadmap.Bundles.byN(1)
    b1.sections.forEach { s -> app.r.toggleTask(com.josephmiller.cloudroadmap.StudyPlan.sectionTask(b1, s), d) }
    val b2 = com.josephmiller.cloudroadmap.Bundles.byN(2)
    b2.sections.take(2).forEach { s -> app.r.toggleTask(com.josephmiller.cloudroadmap.StudyPlan.sectionTask(b2, s), d) }
    app.r.studyMin[d.toString()] = 70
    app.r.portfolio["1"] = b1.noteHint
    app.r.energy["2026-12-03"] = 4
    val out = File(args.getOrElse(0) { "build/snaps" }).apply { mkdirs() }
    Tab.entries.filter { it in listOf(Tab.WTD, Tab.ROADMAP, Tab.PROJECTS) }.forEach { tab ->
        val scene = ImageComposeScene(1360, 880, Density(1f)) {
            MaterialTheme(colorScheme = darkColorScheme(primary = Neon.Cyan, background = Neon.Bg, surface = Neon.Panel)) {
                SnapShell(app, tab)
            }
        }
        repeat(3) { scene.render((it * 400_000_000L)) }
        val img = scene.render(2_000_000_000L)
        File(out, "${tab.name.lowercase()}.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }
    System.exit(0)
}
