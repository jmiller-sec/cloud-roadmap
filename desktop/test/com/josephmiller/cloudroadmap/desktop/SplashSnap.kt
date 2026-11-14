package com.josephmiller.cloudroadmap.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Renders the opening cartoon frame by frame (phone and desktop shapes) so it can be previewed as a video. */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/splash" })
    listOf(Triple("phone", 540, 1170), Triple("desktop", 1280, 800)).forEach { (name, w, h) ->
        val dir = File(out, name).apply { mkdirs() }
        val t = mutableFloatStateOf(0f)
        val scene = ImageComposeScene(w, h, Density(1f)) { CyberFoxSplash(t.floatValue, Modifier.fillMaxSize()) }
        var i = 0
        var ms = 0f
        while (ms <= SPLASH_MS) {
            t.floatValue = ms
            val img = scene.render((ms * 1_000_000L).toLong())
            File(dir, "f%04d.png".format(i++)).writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            ms += 1000f / 30f
        }
        scene.close()
    }
    System.exit(0)
}
