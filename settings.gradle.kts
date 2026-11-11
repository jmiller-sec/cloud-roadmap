pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
    }
}
rootProject.name = "CloudRoadmap"
// CI builds only the Windows app (no Android SDK there).
if (System.getenv("DESKTOP_ONLY") == null) include(":app")
include(":desktop")
