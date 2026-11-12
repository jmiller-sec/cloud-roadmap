import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin { jvmToolchain(21) }

sourceSets {
    main { kotlin.srcDirs("src", "../core/src") }
    test { kotlin.srcDirs("test") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    testImplementation("junit:junit:4.13.2")
}

val appVersion = (project.findProperty("appVersion") as String?) ?: "5.2.0"

compose.desktop {
    application {
        mainClass = "com.josephmiller.cloudroadmap.desktop.MainKt"
        jvmArgs += listOf("-Dapp.version=$appVersion")
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Cloud Roadmap"
            packageVersion = appVersion
            description = "Joseph's cybersecurity roadmap"
            vendor = "jmiller-sec"
            modules("java.net.http", "java.desktop")
            windows {
                menu = true
                shortcut = true
                perUserInstall = true
                menuGroup = "Cloud Roadmap"
                upgradeUuid = "8f3c2a1e-6b7d-4e9a-9c51-2d7f0b3a6e41"
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}

tasks.register<JavaExec>("snap") {
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.josephmiller.cloudroadmap.desktop.SnapKt")
    args(layout.buildDirectory.dir("snaps").get().asFile.absolutePath)
    systemProperty("user.home", layout.buildDirectory.dir("snaphome").get().asFile.absolutePath)
}
