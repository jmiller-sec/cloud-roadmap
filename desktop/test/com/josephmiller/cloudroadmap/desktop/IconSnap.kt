package com.josephmiller.cloudroadmap.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Renders the app icon: full icon (desktop) and transparent foreground (Android adaptive). */
fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/icon" }).apply { mkdirs() }
    listOf("icon" to true, "fg" to false).forEach { (name, bg) ->
        val scene = ImageComposeScene(1024, 1024, Density(1f)) { FoxIconArt(bg, Modifier.fillMaxSize()) }
        File(out, "$name.png").writeBytes(scene.render(0).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }
    System.exit(0)
}
