package com.josephmiller.cloudroadmap

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Store {
    private const val PREFS = "roadmap"
    private const val KEY = "state"

    @Synchronized
    fun load(ctx: Context): Roadmap {
        val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return try {
            if (s == null) Roadmap().also { it.applyOneTimeFixes(LocalDate.now()) } else Roadmap.fromJson(s)
        } catch (e: Exception) {
            Roadmap()
        }
    }

    /** [touch] = a real change by Joseph: stamp it and push it to the cloud soon. */
    @Synchronized
    fun save(ctx: Context, r: Roadmap, touch: Boolean = true) {
        if (touch) r.updatedAt = System.currentTimeMillis()
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, r.toJson()).commit()
        if (touch) Cloud.syncSoon(ctx)
    }

    /** Load, change, save in one locked step. */
    @Synchronized
    fun <T> edit(ctx: Context, touch: Boolean = true, block: (Roadmap) -> T): T {
        val r = load(ctx)
        val out = block(r)
        save(ctx, r, touch)
        return out
    }
}

/** Phone <-> Windows sync through the private repo jmiller-sec/cloud-roadmap. Token stays on this device only. */
object Cloud {
    private const val P = "cloud"
    fun token(ctx: Context) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString("token", "") ?: ""
    fun setToken(ctx: Context, t: String) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("token", t.trim()).apply()
    fun status(ctx: Context) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString("status", "Not connected") ?: ""
    private fun setStatus(ctx: Context, s: String) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("status", s).apply()

    fun syncSoon(ctx: Context, delaySec: Long = 15) {
        if (token(ctx).isBlank()) return
        val req = androidx.work.OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setInitialDelay(delaySec, TimeUnit.SECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("cloudsync", androidx.work.ExistingWorkPolicy.REPLACE, req)
    }

    /** Blocking: pull, merge, push. Saves the merged plan without re-stamping it (no ping-pong between devices). */
    fun syncNow(ctx: Context): String {
        val t = token(ctx)
        if (t.isBlank()) return "Not connected"
        val msg = runCatching {
            val merged = CloudSync.sync(Store.load(ctx), t)
            Store.save(ctx, merged, touch = false)
            "Synced " + java.time.LocalTime.now().withNano(0).withSecond(0)
        }.getOrElse { "Sync failed: ${it.message}" }
        setStatus(ctx, msg)
        return msg
    }
}

/** One-tap phone updates: downloads apk/CloudRoadmap.apk from the private repo and opens Android's installer. */
object Updater {
    fun installedCode(ctx: Context): Long = runCatching {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else @Suppress("DEPRECATION") pi.versionCode.toLong()
    }.getOrDefault(0L)

    /** Blocking. Returns the newer version, or null. */
    fun check(ctx: Context): AppUpdate.Apk? {
        val t = Cloud.token(ctx); if (t.isBlank()) return null
        val a = AppUpdate.latestApk(t) ?: return null
        return if (a.versionCode > installedCode(ctx)) a else null
    }

    /** Blocking download, then hands the file to Android's installer (one "Update" tap). */
    fun install(ctx: Context): String {
        val pm = ctx.packageManager
        if (Build.VERSION.SDK_INT >= 26 && !pm.canRequestPackageInstalls()) {
            ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return "Turn on \"Allow from this source\", then come back and tap Update again"
        }
        return runCatching {
            val f = java.io.File(ctx.cacheDir, "updates/CloudRoadmap.apk")
            AppUpdate.downloadApk(Cloud.token(ctx), f)
            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", f)
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
            "Tap Update on the next screen"
        }.getOrElse { "Update failed: ${it.message}" }
    }
}

/** Progress backup to a file Joseph picks (Drive, Downloads...). Android also backs up the app to his Google account. */
object Backup {
    private const val P = "backup"

    fun uri(ctx: Context): Uri? = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString("uri", null)?.let(Uri::parse)

    fun remember(ctx: Context, uri: Uri) {
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString("uri", uri.toString()).apply()
    }

    fun write(ctx: Context, uri: Uri? = uri(ctx)): Boolean {
        uri ?: return false
        return runCatching {
            Store.edit(ctx) { it.lastBackup = LocalDate.now().toString() }
            val json = Store.load(ctx).toJson()
            ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            true
        }.getOrDefault(false)
    }

    fun restore(ctx: Context, uri: Uri): Boolean = runCatching {
        val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val r = Roadmap.fromJson(text)
        require(r.items.isNotEmpty())
        Store.save(ctx, r)
        true
    }.getOrDefault(false)
}

object Sync {
    private const val CHANNEL = "weekly"
    private const val WORK = "roadmap-sync"

    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", "CloudRoadmap/1.0 (Android)")
        c.useCaches = false
        try {
            if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun fetchBadges(user: String): List<Badge> {
        val o = JSONObject(get("https://www.credly.com/users/$user/badges.json"))
        val arr = o.getJSONArray("data")
        return (0 until arr.length()).mapNotNull { k ->
            val b = arr.getJSONObject(k)
            val name = b.optJSONObject("badge_template")?.optString("name") ?: return@mapNotNull null
            val date = b.optString("issued_at_date").takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: return@mapNotNull null
            val issuer = b.optJSONObject("issuer")?.optString("summary")?.removePrefix("issued by ") ?: ""
            Badge(name, issuer, date)
        }
    }

    /**
     * One full check: Credly badges, weekly feed, and (on Sunday from 9am, or when [manual]) the running-late check.
     * Returns lines describing what happened.
     */
    fun run(ctx: Context, manual: Boolean): List<String> {
        val today = LocalDate.now()
        val snapshot = Store.load(ctx)
        val lines = mutableListOf<String>()
        var feedSummary: String? = null

        val badges = try { fetchBadges(snapshot.credlyUser) } catch (e: Exception) { lines += "Credly check failed: ${e.message}"; null }
        val feed = if (snapshot.feedUrl.isBlank()) null else try {
            get(snapshot.feedUrl + (if (snapshot.feedUrl.contains("?")) "&" else "?") + "t=" + System.currentTimeMillis())
        } catch (e: Exception) { lines += "Weekly feed failed: ${e.message}"; null }

        val isSundayCheck = today.dayOfWeek == DayOfWeek.SUNDAY && LocalTime.now().hour >= 9
        var sendSunday = false
        var sundayFeedSummary: String? = null
        val sundayMorning = today.dayOfWeek == DayOfWeek.SUNDAY && LocalTime.now().hour < 9

        Store.edit(ctx, touch = false) { r ->
            if (badges != null) r.updateBadges(badges, today).forEach { lines += "Badge found, marked done: $it" }
            if (feed != null) {
                try {
                    feedSummary = r.applyFeed(feed, today)
                } catch (e: Exception) {
                    lines += "Weekly feed unreadable: ${e.message}"
                }
            }
            if ((isSundayCheck || manual) && r.lastLateCheck != today.toString()) {
                lines += r.lateCheck(today)
            }
            if (isSundayCheck && r.lastSundayNotice != today.toString()) {
                r.lastSundayNotice = today.toString()
                sendSunday = true
                if (feedSummary == null && r.lastFeedDate == today.toString()) sundayFeedSummary = r.lastFeedSummary
            }
        }

        // Weekly automatic backup on Sunday to the file Joseph picked.
        if ((sendSunday || manual) && Backup.uri(ctx) != null && Store.load(ctx).lastBackup != today.toString()) {
            if (Backup.write(ctx)) lines += "Progress backed up" else lines += "Backup failed: pick the backup file again in Settings"
        }

        if (!manual) {
            val body = buildList {
                (feedSummary ?: sundayFeedSummary)?.let { add(it) }
                addAll(lines.filterNot { it.startsWith("Credly check failed") })
            }
            when {
                sendSunday -> notify(ctx, "Sunday roadmap check", body.ifEmpty { listOf("No changes this week. Keep going.") })
                sundayMorning -> Unit // held for the 9am Sunday notification
                body.isNotEmpty() -> notify(ctx, if (feedSummary != null) "New roadmap update" else "Cloud Roadmap update", body)
            }
        }
        feedSummary?.let { lines.add(0, "Weekly check: $it") }
        return lines
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Weekly roadmap check", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    @SuppressLint("MissingPermission")
    fun notify(ctx: Context, title: String, lines: List<String>, tab: Int = 99) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, tab, Intent(ctx, MainActivity::class.java).putExtra("tab", tab),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val style = NotificationCompat.InboxStyle()
        lines.take(6).forEach { style.addLine(it) }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(title.hashCode(), n)
    }
}

class CloudSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Cloud.syncNow(applicationContext)
        WtdWidget.refresh(applicationContext)
        return Result.success()
    }
}

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Cloud.syncNow(applicationContext)
        Sync.run(applicationContext, manual = false)
        WtdWidget.refresh(applicationContext)
        return Result.success()
    }
}
