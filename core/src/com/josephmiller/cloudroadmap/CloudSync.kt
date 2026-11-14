package com.josephmiller.cloudroadmap

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlin.math.abs

/** A set of study keys that remembers when each key was checked (+ms) or unchecked (-ms). */
class LoggedSet(initial: Collection<String>, val log: MutableMap<String, Long>, private val clock: () -> Long = { System.currentTimeMillis() }) : AbstractMutableSet<String>() {
    private val backing = LinkedHashSet(initial)
    override val size get() = backing.size
    override fun contains(element: String) = backing.contains(element)
    override fun add(element: String): Boolean = backing.add(element).also { if (it) log[element] = clock() }
    override fun remove(element: String): Boolean = backing.remove(element).also { if (it) log[element] = -clock() }
    override fun iterator(): MutableIterator<String> = object : MutableIterator<String> {
        val inner = backing.iterator(); var last: String? = null
        override fun hasNext() = inner.hasNext()
        override fun next() = inner.next().also { last = it }
        override fun remove() { inner.remove(); last?.let { log[it] = -clock() } }
    }
    /** Replace contents without logging (used when merging two devices). */
    fun setSilently(keys: Collection<String>) { backing.clear(); backing.addAll(keys) }
}

/** Merges two devices' saves so neither loses work. */
object Merge {
    fun merge(a: Roadmap, b: Roadmap): Roadmap {
        val (newer, older) = if (a.updatedAt >= b.updatedAt) a to b else b to a
        val r = Roadmap.fromJson(newer.toJson())
        // Checkmarks: union, then each key's latest check/uncheck wins.
        val log = HashMap(older.doneLog)
        newer.doneLog.forEach { (k, v) -> val o = log[k]; if (o == null || abs(v) >= abs(o)) log[k] = v }
        val keys = LinkedHashSet(a.studyDone + b.studyDone)
        log.forEach { (k, v) -> if (v > 0) keys.add(k) else keys.remove(k) }
        r.doneLog.clear(); r.doneLog.putAll(log)
        r.studyDone.setSilently(keys)
        // Per-day / per-bundle values: keep both; the newer save wins a clash. Minutes only ever grow.
        older.energy.forEach { (k, v) -> r.energy.putIfAbsent(k, v) }
        older.sleep.forEach { (k, v) -> r.sleep[k] = maxOf(v, r.sleep[k] ?: 0) }
        if (r.sleepStatus.isBlank()) r.sleepStatus = older.sleepStatus
        older.checkin.forEach { (k, v) -> r.checkin.putIfAbsent(k, v) }
        older.portfolio.forEach { (k, v) -> r.portfolio.putIfAbsent(k, v) }
        older.studyMin.forEach { (k, v) -> r.studyMin[k] = maxOf(v, r.studyMin[k] ?: 0) }
        older.alertLog.forEach { (k, v) -> r.alertLog[k] = maxOf(v, r.alertLog[k] ?: 0) }
        r.updatedAt = maxOf(a.updatedAt, b.updatedAt)
        return r
    }
}

/** Syncs the plan through one JSON file in a private GitHub repo, using a fine-grained token. */
object CloudSync {
    const val DEFAULT_REPO = "jmiller-sec/cloud-roadmap"
    const val PATH = "sync/state.json"

    class Remote(val json: String?, val sha: String?)

    private fun open(url: String, token: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 20000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

    fun pull(token: String, repo: String = DEFAULT_REPO): Remote {
        val c = open("https://api.github.com/repos/$repo/contents/$PATH", token, "GET")
        return when (c.responseCode) {
            200 -> {
                val o = JSONObject(c.inputStream.bufferedReader().readText())
                Remote(String(Base64.getMimeDecoder().decode(o.getString("content"))), o.getString("sha"))
            }
            404 -> Remote(null, null)
            else -> error("GitHub said ${c.responseCode} (check the token)")
        }
    }

    fun push(token: String, json: String, sha: String?, repo: String = DEFAULT_REPO): Int {
        val c = open("https://api.github.com/repos/$repo/contents/$PATH", token, "PUT")
        c.doOutput = true
        val body = JSONObject().put("message", "sync").put("content", Base64.getEncoder().encodeToString(json.toByteArray()))
        if (sha != null) body.put("sha", sha)
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        return c.responseCode
    }

    /** Pull, merge with [local], push if anything changed. Returns the merged plan. */
    fun sync(local: Roadmap, token: String, repo: String = DEFAULT_REPO): Roadmap {
        repeat(2) {
            val remote = pull(token, repo)
            val merged = remote.json?.let { Merge.merge(local, Roadmap.fromJson(it)) } ?: local
            val out = merged.toJson()
            val same = remote.json != null && Roadmap.fromJson(remote.json).toJson() == out
            if (same) return merged
            val code = push(token, out, remote.sha, repo)
            if (code == 200 || code == 201) return merged
            if (code != 409 && code != 422) error("GitHub said $code while saving")
        }
        error("Sync conflict, try again")
    }
}


/** In-app updates from the private repo: phone APK in apk/, Windows MSI in Releases. */
object AppUpdate {
    class Apk(val versionCode: Int, val versionName: String)
    class Msi(val version: String, val assetApi: String, val name: String)

    private fun api(url: String, token: String, accept: String = "application/vnd.github+json"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 60000; instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", accept)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }

    /** apk/version.json: {"versionCode":33,"versionName":"4.9"} */
    fun latestApk(token: String, repo: String = CloudSync.DEFAULT_REPO): Apk? = runCatching {
        val c = api("https://api.github.com/repos/$repo/contents/apk/version.json", token, "application/vnd.github.raw")
        if (c.responseCode != 200) return null
        val o = JSONObject(c.inputStream.bufferedReader().readText())
        Apk(o.getInt("versionCode"), o.getString("versionName"))
    }.getOrNull()

    /** [suffix] picks the installer for this computer: ".msi" on Windows, ".dmg" on a Mac. */
    fun latestMsi(token: String, repo: String = CloudSync.DEFAULT_REPO, suffix: String = ".msi"): Msi? = runCatching {
        val c = api("https://api.github.com/repos/$repo/releases/latest", token)
        if (c.responseCode != 200) return null
        val o = JSONObject(c.inputStream.bufferedReader().readText())
        val assets = o.getJSONArray("assets")
        val a = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(suffix) } ?: return null
        Msi(o.getString("tag_name").removePrefix("v"), a.getString("url"), a.getString("name"))
    }.getOrNull()

    fun downloadApk(token: String, dest: java.io.File, repo: String = CloudSync.DEFAULT_REPO) =
        save(api("https://api.github.com/repos/$repo/contents/apk/CloudRoadmap.apk", token, "application/vnd.github.raw"), dest)

    fun downloadMsi(token: String, m: Msi, dest: java.io.File) =
        save(api(m.assetApi, token, "application/octet-stream"), dest)

    /** Follows GitHub's redirect to its file storage without sending the token there. */
    private fun save(first: HttpURLConnection, dest: java.io.File) {
        var c = first
        repeat(5) {
            val code = c.responseCode
            if (code in 300..399) {
                val loc = c.getHeaderField("Location") ?: error("Bad redirect")
                c = (URL(loc).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 60000 }
                return@repeat
            }
            if (code != 200) error("GitHub said $code")
            dest.parentFile?.mkdirs()
            val tmp = java.io.File(dest.path + ".part")
            c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            if (!tmp.renameTo(dest)) { dest.delete(); tmp.renameTo(dest) }
            return
        }
        error("Too many redirects")
    }

    fun newer(a: String, b: String): Boolean {
        val x = a.split(".").map { it.toIntOrNull() ?: 0 }; val y = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) { val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }; if (d != 0) return d > 0 }
        return false
    }
}
