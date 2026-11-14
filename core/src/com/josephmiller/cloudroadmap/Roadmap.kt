package com.josephmiller.cloudroadmap

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** All roadmap state and rules. Pure Kotlin + org.json so it can be unit tested. */
class Roadmap(
    val items: MutableList<Item> = Plan.defaults(),
    val proposals: MutableList<Proposal> = mutableListOf(),
    val findings: MutableList<Finding> = mutableListOf(),
    val notes: MutableList<Note> = mutableListOf(),
    val badges: MutableList<Badge> = Plan.startingBadges(),
    var seenWeek: String = "",
    var lastLateCheck: String = "",
    var lastSundayNotice: String = "",
    var feedUrl: String = DEFAULT_FEED,
    var lastFeedSummary: String = "",
    var lastFeedDate: String = "",
    var credlyUser: String = "joseph-miller.6b56c94d",
    /** When each study key was last checked (+ms) or unchecked (-ms). Lets two devices merge without losing work. */
    val doneLog: MutableMap<String, Long> = mutableMapOf(),
    val studyDone: LoggedSet = LoggedSet(emptyList(), doneLog),
    var studyShift: Int = 0,
    /** LetsDefend alerts triaged per day (yyyy-mm-dd -> count). */
    val alertLog: MutableMap<String, Int> = mutableMapOf(),
    val wguCourses: MutableList<WguCourse> = WguPlan.baseline(),
    var wguCatalog: String = WguPlan.BASE_CATALOG,
    var lastBackup: String = "",
    /** Energy check after each study day (yyyy-mm-dd -> 1..5). */
    val energy: MutableMap<String, Int> = mutableMapOf(),
    /** Minutes slept, keyed by the date you woke up (from Health Connect: SnoozeFox or Samsung Health). */
    val sleep: MutableMap<String, Int> = mutableMapOf(),
    /** One-line health of the sleep link, e.g. "ready 3 nights, newest 2026-11-13 from com.sec.android.app.shealth" (no health values). */
    var sleepStatus: String = "",
    /** Pre-study burnout check-in (1-5), keyed by date. */
    val checkin: MutableMap<String, Int> = mutableMapOf(),
    /** Minutes studied per day, banked from the burnout timer. */
    val studyMin: MutableMap<String, Int> = mutableMapOf(),
    var timerStart: Long = 0,
    var timerDay: String = "",
    /** 2-line portfolio note per bundle number. */
    val portfolio: MutableMap<String, String> = mutableMapOf(),
    /** Last time this state was saved on any device (ms). */
    var updatedAt: Long = 0,
) {
    companion object {
        const val AUTO_LIMIT = 7
        const val ALERTS_PER_MONTH = 15
        const val ALERTS_PER_WEEK = 4
        val LD_FREE = listOf("ld_socfund", "ld_phish", "ld_linux", "ld_web", "ld_malab")
        const val DEFAULT_FEED = "https://raw.githubusercontent.com/jmiller-sec/cloud-roadmap-feed/main/updates.json"

        fun days(from: LocalDate, to: LocalDate) = ChronoUnit.DAYS.between(from, to).toInt()

        fun fromJson(s: String): Roadmap {
            val log: MutableMap<String, Long> = JSONObject(s).optJSONObject("doneLog")?.let { a -> a.keys().asSequence().associateWith { k -> a.getLong(k) }.toMutableMap() } ?: mutableMapOf()
            val o = JSONObject(s)
            val r = Roadmap(
                items = o.getJSONArray("items").let { a -> MutableList(a.length()) { Item.fromJson(a.getJSONObject(it)) } },
                proposals = o.optJSONArray("proposals")?.let { a -> MutableList(a.length()) { Proposal.fromJson(a.getJSONObject(it)) } } ?: mutableListOf(),
                findings = o.optJSONArray("findings")?.let { a -> MutableList(a.length()) { Finding.fromJson(a.getJSONObject(it)) } } ?: mutableListOf(),
                notes = o.optJSONArray("notes")?.let { a ->
                    MutableList(a.length()) { a.getJSONObject(it).let { n -> Note(LocalDate.parse(n.getString("d")), n.getString("t")) } }
                } ?: mutableListOf(),
                badges = o.optJSONArray("badges")?.let { a -> MutableList(a.length()) { Badge.fromJson(a.getJSONObject(it)) } } ?: Plan.startingBadges(),
                seenWeek = o.optString("seenWeek"),
                lastLateCheck = o.optString("lastLateCheck"),
                lastSundayNotice = o.optString("lastSundayNotice"),
                feedUrl = o.optString("feedUrl").ifBlank { DEFAULT_FEED }.replace("/jos492/", "/jmiller-sec/"), // GitHub account renamed Nov 2026
                lastFeedSummary = o.optString("lastFeedSummary"),
                lastFeedDate = o.optString("lastFeedDate"),
                credlyUser = o.optString("credlyUser", "joseph-miller.6b56c94d"),
                doneLog = log,
                studyDone = LoggedSet(o.optJSONArray("studyDone")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(), log),
                studyShift = o.optInt("studyShift"),
                wguCourses = o.optJSONArray("wguCourses")?.let { WguPlan.listFromJson(it) } ?: WguPlan.baseline(),
                wguCatalog = o.optString("wguCatalog", WguPlan.BASE_CATALOG),
                lastBackup = o.optString("lastBackup"),
                alertLog = o.optJSONObject("alertLog")?.let { a -> a.keys().asSequence().associateWith { k -> a.getInt(k) }.toMutableMap() } ?: mutableMapOf(),
                energy = o.optJSONObject("energy")?.let { a -> a.keys().asSequence().associateWith { k -> a.getInt(k) }.toMutableMap() } ?: mutableMapOf(),
                sleepStatus = o.optString("sleepStatus", ""),
                checkin = o.optJSONObject("checkin")?.let { a -> a.keys().asSequence().associateWith { k -> a.getInt(k) }.toMutableMap() } ?: mutableMapOf(),
                sleep = o.optJSONObject("sleep")?.let { a -> a.keys().asSequence().associateWith { k -> a.getInt(k) }.toMutableMap() } ?: mutableMapOf(),
                studyMin = o.optJSONObject("studyMin")?.let { a -> a.keys().asSequence().associateWith { k -> a.getInt(k) }.toMutableMap() } ?: mutableMapOf(),
                timerStart = o.optLong("timerStart"),
                timerDay = o.optString("timerDay"),
                updatedAt = o.optLong("updatedAt"),
                portfolio = o.optJSONObject("portfolio")?.let { a -> a.keys().asSequence().associateWith { k -> a.getString(k) }.toMutableMap() } ?: mutableMapOf(),
            )
            // Fill in auto-tracking keys added in newer versions.
            val defaults = Plan.defaults().associateBy { it.id }
            r.items.forEach { if (it.badgeKey == null) it.badgeKey = defaults[it.id]?.badgeKey }
            // Add items introduced in newer versions, and move TCM+LD into the SOC training stage.
            val have = r.items.map { it.id }.toSet()
            Plan.defaults().filter { it.id !in have }.forEach { r.items.add(it) }
            r.applyOneTimeFixes(LocalDate.now())
            return r
        }
    }

    fun toJson(): String = JSONObject().apply {
        put("items", Plan.itemsToJson(items))
        put("proposals", JSONArray().apply { proposals.forEach { put(it.toJson()) } })
        put("findings", JSONArray().apply { findings.forEach { put(it.toJson()) } })
        put("notes", JSONArray().apply { notes.takeLast(60).forEach { put(JSONObject().put("d", it.date.toString()).put("t", it.text)) } })
        put("badges", JSONArray().apply { badges.forEach { put(it.toJson()) } })
        put("seenWeek", seenWeek); put("lastLateCheck", lastLateCheck)
        put("lastSundayNotice", lastSundayNotice)
        put("feedUrl", feedUrl); put("credlyUser", credlyUser)
        put("lastFeedSummary", lastFeedSummary); put("lastFeedDate", lastFeedDate)
        put("studyDone", JSONArray().apply { studyDone.sorted().forEach { put(it) } }); put("studyShift", studyShift)
        put("energy", JSONObject().apply { energy.forEach { (k, v) -> put(k, v) } })
        put("sleepStatus", sleepStatus)
        put("checkin", JSONObject().apply { checkin.entries.sortedBy { it.key }.takeLast(120).forEach { (k, v) -> put(k, v) } })
        put("sleep", JSONObject().apply { sleep.entries.sortedBy { it.key }.takeLast(120).forEach { (k, v) -> put(k, v) } })
        put("studyMin", JSONObject().apply { studyMin.forEach { (k, v) -> put(k, v) } })
        put("timerStart", timerStart); put("timerDay", timerDay); put("updatedAt", updatedAt)
        put("doneLog", JSONObject().apply { doneLog.forEach { (k, v) -> put(k, v) } })
        put("portfolio", JSONObject().apply { portfolio.forEach { (k, v) -> put(k, v) } })
        put("alertLog", JSONObject().apply { alertLog.forEach { (k, v) -> put(k, v) } })
        put("wguCourses", WguPlan.listToJson(wguCourses)); put("wguCatalog", wguCatalog); put("lastBackup", lastBackup)
    }.toString()

    fun item(id: String) = items.firstOrNull { it.id == id }

    private fun note(today: LocalDate, text: String) { notes.add(Note(today, text)) }

    // ---------- shifting ----------

    /**
     * Move every unfinished, unlocked item in the same stage that starts after [anchorEnd]
     * (with a 2-day overlap allowance) by [delta] days. Returns warnings about locked dates.
     */
    fun shiftAfter(source: Item, anchorEnd: LocalDate, delta: Int, today: LocalDate): List<String> {
        if (delta == 0) return emptyList()
        val warnings = mutableListOf<String>()
        val lockedDates = items.filter { it.locked && it.status != Status.DONE }
        for (other in items) {
            if (other === source || other.stage != source.stage) continue
            if (other.status == Status.DONE || other.locked) continue
            if (!other.start.isAfter(anchorEnd.minusDays(3))) continue
            val oldEnd = other.end
            other.start = other.start.plusDays(delta.toLong())
            other.end = other.end.plusDays(delta.toLong())
            for (l in lockedDates) {
                if (!oldEnd.isAfter(l.end) && other.end.isAfter(l.end)) {
                    warnings += "\"${other.title}\" now ends after the locked date \"${l.title}\" (${l.end})."
                }
            }
        }
        warnings.forEach { note(today, "Warning: $it") }
        return warnings
    }

    // ---------- buttons ----------

    fun markWorking(id: String, today: LocalDate) {
        val it = item(id) ?: return
        if (it.status == Status.DONE) return
        it.status = Status.WORKING
        if (it.startedOn == null) it.startedOn = today
        note(today, "Started: ${it.title}")
    }

    /** Done button or Credly badge. [on] is the finish date. */
    private val NO_SHIFT = setOf("cs1015", "wgu_confirm")

    fun markDone(id: String, on: LocalDate, today: LocalDate, viaBadge: Boolean = false, shift: Boolean = true) {
        val it = item(id) ?: return
        if (it.status == Status.DONE) return
        it.status = Status.DONE
        it.doneOn = on
        it.doneByBadge = viaBadge
        if (it.startedOn == null) it.startedOn = minOf(on, it.start)
        val oldEnd = it.end
        val delta = days(oldEnd, on) // + late, - early
        it.end = on
        note(today, "Done${if (viaBadge) " (Credly badge)" else ""}: ${it.title}")
        // CS1015 has a planned gap before Study.com (SOC Level 1 comes first), so finishing it never moves other dates.
        if (!shift || it.locked || id in NO_SHIFT || delta == 0 || on.isBefore(today.minusYears(1))) return
        val label = if (delta > 0) "$delta days late" else "${-delta} days early"
        if (abs(delta) <= AUTO_LIMIT) {
            shiftAfter(it, oldEnd, delta, today)
            note(today, "Auto-shifted the rest of \"${stageTitle(it.stage)}\" by ${signed(delta)} (${it.title} finished $label).")
        } else {
            proposals.removeAll { p -> p.itemId == id }
            proposals.add(
                Proposal(
                    "p-$id-${today}", "shift",
                    "${it.title} finished $label. Move the rest of \"${stageTitle(it.stage)}\" ${signed(delta)}?",
                    id, delta, oldEnd,
                )
            )
        }
    }

    fun undo(id: String, today: LocalDate) {
        val it = item(id) ?: return
        it.status = if (it.startedOn != null) Status.WORKING else Status.TODO
        it.doneOn = null
        it.doneByBadge = false
        note(today, "Undone: ${it.title} (dates were not moved back)")
    }

    // ---------- running late ----------

    fun overdueDays(it: Item, today: LocalDate) =
        if (it.status == Status.WORKING && !paused(it.id) && today.isAfter(it.end)) days(it.end, today) else 0

    private fun unpauseTransfers(today: LocalDate) {
        val d = Plan.defaults().associateBy { it.id }
        listOf("cs1015", "sdc_ethics", "sdc_infosec", "sdc_forensics").forEach { id ->
            studyDone.remove("paused:$id")
            val def = d[id] ?: return@forEach
            item(id)?.let { it.title = def.title; if (id != "cs1015" && it.status == Status.TODO) { it.start = def.start; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 } }
        }
        // No-burnout plan: SOC L1 shifted a week (networking rooms first, matched to CS1015), projects from Mar 27.
        listOf("thm_free", "thm_soc1", "lab_ad", "lab_net", "lab_ticket", "lab_ps", "messer_core1", "messer_core2").forEach { id ->
            val def = d[id] ?: return@forEach
            item(id)?.takeIf { it.status != Status.DONE }?.let { it.title = def.title; it.start = def.start; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 }
        }
        item("wgu_confirm")?.let { it.title = d["wgu_confirm"]!!.title; if (it.status != Status.DONE) markDone("wgu_confirm", today, today, shift = false) }
        studyDone.add("fix:unpause29")
    }

    /** Paused items (e.g. CS1015 while waiting on WGU) never count as late. */
    fun paused(id: String) = "paused:$id" in studyDone

    /** Weekly check for Working-on-it items past their end date. Returns summary lines. */
    fun lateCheck(today: LocalDate): List<String> {
        val lines = mutableListOf<String>()
        for (it in items.toList()) {
            val over = overdueDays(it, today)
            if (over <= 0) continue
            if (it.autoExtended < AUTO_LIMIT) {
                val add = AUTO_LIMIT - it.autoExtended
                val oldEnd = it.end
                it.end = it.end.plusDays(add.toLong())
                it.autoExtended += add
                shiftAfter(it, oldEnd, add, today)
                note(today, "Auto-extended ${it.title} by $add days (running $over days over).")
                lines += "${it.title}: running $over days over, extended $add days"
            } else if (proposals.none { p -> p.itemId == it.id && p.kind == "late" }) {
                proposals.add(
                    Proposal(
                        "late-${it.id}-$today", "late",
                        "${it.title} is running $over days long. Push it and the rest of \"${stageTitle(it.stage)}\" back a week?",
                        it.id, 7, it.end,
                    )
                )
                lines += "${it.title}: running $over days over, needs your OK"
            }
        }
        lastLateCheck = today.toString()
        return lines
    }

    // ---------- proposals ----------

    fun approve(pid: String, today: LocalDate) {
        val p = proposals.firstOrNull { it.id == pid } ?: return
        proposals.remove(p)
        val it = item(p.itemId) ?: return
        when (p.kind) {
            "shift" -> {
                shiftAfter(it, p.anchorEnd, p.days, today)
                note(today, "You approved: moved \"${stageTitle(it.stage)}\" ${signed(p.days)}.")
            }
            "late" -> {
                val oldEnd = it.end
                it.end = it.end.plusDays(p.days.toLong())
                shiftAfter(it, oldEnd, p.days, today)
                note(today, "You approved: pushed ${it.title} back ${p.days} days.")
            }
        }
    }

    fun keep(pid: String, today: LocalDate) {
        val p = proposals.firstOrNull { it.id == pid } ?: return
        proposals.remove(p)
        note(today, "Kept dates: ${p.text}")
    }

    // ---------- Credly ----------

    /** badges = list of (name, issuedDate). Returns names of items marked done. */
    fun applyBadges(badges: List<Pair<String, LocalDate>>, today: LocalDate): List<String> {
        val done = mutableListOf<String>()
        for (it in items) {
            val key = it.badgeKey ?: continue
            if (it.status == Status.DONE) continue
            val hit = badges.firstOrNull { b -> b.first.contains(key, ignoreCase = true) } ?: continue
            markDone(it.id, hit.second, today, viaBadge = true)
            done += it.title
        }
        return done
    }

    /** Replace the stored badge list with what Credly returned and mark matching items done. */
    fun updateBadges(list: List<Badge>, today: LocalDate): List<String> {
        if (list.isNotEmpty()) { badges.clear(); badges.addAll(list.sortedByDescending { it.date }) }
        return applyBadges(list.map { it.name to it.date }, today)
    }

    // ---------- weekly feed ----------

    /** Returns the summary text if this is a new week, else null. */
    fun applyFeed(json: String, today: LocalDate): String? {
        val o = JSONObject(json)
        val week = o.optString("weekId")
        if (week.isBlank() || week == seenWeek) return null
        seenWeek = week
        val arr = o.optJSONArray("findings") ?: JSONArray()
        for (k in 0 until arr.length()) {
            val f = arr.getJSONObject(k)
            findings.add(
                Finding(
                    "$week-$k", week, f.optString("type", "info"), f.optString("title"),
                    f.optString("detail"), f.optString("link"), f.optJSONObject("action"), "new",
                    f.optString("category"),
                )
            )
        }
        // "done" actions (e.g. a Study.com completion email) apply on their own, no approval needed.
        findings.filter { it.week == week && it.action?.optString("op") == "done" }.forEach { f ->
            val a = f.action!!
            val id = a.optString("itemId")
            val on = a.optString("date").takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: today
            if (item(id)?.status != Status.DONE) {
                markDone(id, on, today)
                note(today, "Auto-done from email: ${item(id)?.title ?: id}")
            }
            f.state = "seen"
        }
        while (findings.size > 150) findings.removeAt(0)
        val changes = (0 until arr.length()).count { arr.getJSONObject(it).optString("type") == "change" }
        note(today, "Weekly check $week: ${arr.length()} findings, $changes need your OK.")
        // WGU degree plan: the Sunday check sends the full current course list.
        o.optJSONArray("wguCourses")?.let { a ->
            val incoming = (0 until a.length()).map { k -> a.getJSONObject(k).let { c -> WguCourse(c.getString("name"), c.optInt("cu"), c.optInt("term")) } }
            val changes = WguPlan.merge(wguCourses, incoming)
            o.optString("wguCatalog").takeIf { it.isNotBlank() }?.let { wguCatalog = it }
            changes.forEach { note(today, "WGU degree plan: $it") }
        }
        lastFeedSummary = o.optString("summary").ifBlank { "${arr.length()} updates this week" }
        lastFeedDate = today.toString()
        return lastFeedSummary
    }

    fun approveFinding(fid: String, today: LocalDate) {
        val f = findings.firstOrNull { it.id == fid } ?: return
        f.state = "approved"
        val a = f.action ?: return
        when (a.optString("op")) {
            "rename" -> item(a.optString("itemId"))?.let { it.title = a.optString("title", it.title) }
            "remove" -> items.removeAll { it.id == a.optString("itemId") }
            "dates" -> item(a.optString("itemId"))?.let {
                a.optString("start").takeIf { s -> s.isNotBlank() }?.let { s -> it.start = LocalDate.parse(s) }
                a.optString("end").takeIf { s -> s.isNotBlank() }?.let { s -> it.end = LocalDate.parse(s) }
            }
            "add" -> {
                val id = a.optString("itemId").ifBlank { "feed-$fid" }
                if (item(id) == null) {
                    val s = LocalDate.parse(a.getString("start"))
                    val e = LocalDate.parse(a.getString("end"))
                    items.add(Item(id, a.optString("stage", "cloudbasics"), a.optString("title"), s, e, e, badgeKey = a.optString("badgeKey").ifBlank { null }))
                }
            }
        }
        note(today, "You approved: ${f.title}")
    }

    /** jobs | wgu | other. Uses the feed's category, falling back to keywords. */
    fun tabOf(f: Finding): String {
        val c = f.category.lowercase()
        if (c == "jobs" || c == "wgu" || c == "videos") return c
        if (c.isNotBlank()) return "other"
        val t = (f.title + " " + f.detail).lowercase()
        return when {
            listOf("job", "opening", "hiring", "position", "help desk", "southwest").any { it in t } -> "jobs"
            listOf("wgu", "tuition", "enroll", "transfer").any { it in t } -> "wgu"
            else -> "other"
        }
    }

    fun dismissFinding(fid: String) {
        findings.firstOrNull { it.id == fid }?.state = "dismissed"
    }

    // ---------- status ----------

    /** + behind, - ahead, based on the next unfinished, unlocked item that started already or is next. */
    fun drift(today: LocalDate): Int {
        val active = items.filter { it.status != Status.DONE && !it.locked && !paused(it.id) && !it.start.isAfter(today.plusDays(30)) }
        if (active.isEmpty()) return 0
        return active.maxOf { days(it.originalEnd, it.end) + overdueDays(it, today) }
    }

    // ---------- daily study (WTD) ----------

    fun dayDone(day: LocalDate): Boolean {
        val k = day.toString()
        return studyDone.any { it == k || it.startsWith("$k|") }
    }

    fun isDone(t: DayTask) = if (t.stream == "alerts") alertsInWeek(LocalDate.parse(t.key.substringBefore("|"))) >= ALERTS_PER_WEEK || alertsLeftThisMonth(LocalDate.parse(t.key.substringBefore("|"))) == 0 else t.key in studyDone

    /** Check or uncheck a WTD task. Checking the last step of a lab marks that roadmap item done. */
    /** Projects unlock one at a time, and steps go in order. Returns why a step can't be toggled, or null if it can. */
    fun projectLock(t: DayTask, today: LocalDate = LocalDate.now()): String? {
        if (t.stream != "lab") return null
        if (today.isBefore(ThmPlan.PROJECTS_UNLOCK)) return "Locked until Sat May 1 (after TryHackMe SOC Level 1)"
        val p = Projects.byId[t.itemId] ?: return null
        val n = t.key.substringAfterLast(":").toIntOrNull() ?: return null
        val cur = Projects.current(this)
        if (p != cur) return if (Projects.isDone(this, p)) "Project already finished" else "Locked: finish #${Projects.all.indexOf(cur) + 1} ${cur?.title} first"
        val nextStep = p.steps.indices.firstOrNull { "lab:${p.id}:$it" !in studyDone } ?: return null
        val lastDone = p.steps.indices.lastOrNull { "lab:${p.id}:$it" in studyDone }
        return if (n == nextStep || n == lastDone) null else "Do step ${nextStep + 1} first"
    }

    fun toggleTask(t: DayTask, today: LocalDate) {
        if (projectLock(t, today) != null) return
        if (studyDone.remove(t.key)) {
            if (t.stream == "bundle") { if (item("cs1015")?.status == Status.DONE && item("cs1015")?.doneByBadge == false) undo("cs1015", today); return }
            Regex("thm:(\\d+)").matchEntire(t.key)?.let { m -> studyDone.remove("$today|thmwk:${m.groupValues[1]}") }
            if (t.stream == "lab") studyDone.remove("$today|lab")
            if (t.stream == "todo") t.itemId?.let { id -> undo(id, today) }
            if (t.stream == "thm" || t.stream == "video" || t.stream == "job") studyDone.remove("$today|${t.stream}")
            if (t.stream == "thm" && item("thm_soc1")?.status == Status.DONE) undo("thm_soc1", today)
            return
        }
        if (t.stream == "todo") {
            // One-off to-dos finish their roadmap item without moving any other dates.
            t.itemId?.let { id -> markDone(id, today, today, shift = false) }
            studyDone.add(t.key)
            return
        }
        if (t.stream == "bundle") {
            // One section of a CS1015 bundle. Finishing all 26 bundles finishes CS1015 (no other dates move).
            val n = t.key.split(":")[1]
            // Recall gate: nothing in a bundle counts until its warm-up quiz is passed (Gate.record ticks "warm").
            if (!t.key.endsWith(":warm") && !Gate.passed(this, n.toInt())) return
            if (t.key.endsWith(":warm") && !Gate.passed(this, n.toInt())) return
            studyDone.add(t.key); studyDone.add("$today|b:$n")
            if (item("cs1015")?.status == Status.TODO) markWorking("cs1015", today)
            if (Bundles.next(this).isEmpty()) markDone("cs1015", today, today, shift = false)
            return
        }
        if (t.stream == "job") {
            studyDone.add(t.key); studyDone.add("$today|job")
            if (item("helpdesk_job")?.status == Status.TODO) markWorking("helpdesk_job", today)
            return
        }
        if (t.stream == "thm" || t.stream == "video") {
            // TryHackMe Saturday or its paired commute video. The free trial item finishes after week 2;
            // finishing the last capstone finishes SOC Level 1. Neither moves other dates.
            studyDone.add(t.key); studyDone.add("$today|${t.stream}")
            if (t.stream == "thm") studyDone.add("$today|thmwk:${t.key.removePrefix("thm:")}") // keeps today's week on screen
            if (t.itemId == "thm_soc1" && item("thm_soc1")?.status == Status.TODO) markWorking("thm_soc1", today)
            if (t.stream == "thm") {
                if (ThmPlan.next(this) == null) markDone("thm_soc1", today, today, shift = false)
            }
            return
        }
        studyDone.add(t.key)
        if (t.stream == "lab") {
            studyDone.add("$today|lab")
            val id = t.itemId ?: return
            val steps = StudyPlan.labSteps[id] ?: return
            val it = item(id) ?: return
            if (it.status == Status.TODO) markWorking(id, today)
            if (steps.indices.all { n -> "lab:$id:$n" in studyDone }) markDone(id, today, today)
        } else if (t.itemId != null && item(t.itemId)?.status == Status.TODO) {
            markWorking(t.itemId, today)
        }
    }

    /** Consecutive done days ending today (or yesterday if today isn't done yet). Mondays (off day) don't break it. */
    /** Study days so far this month. Missed days never count against you. */
    fun studyDaysThisMonth(today: LocalDate): Int =
        (1..today.dayOfMonth).count { dayDone(today.withDayOfMonth(it)) }

    fun streak(today: LocalDate): Int {
        var d = if (dayDone(today)) today else today.minusDays(1)
        var n = 0
        while (true) {
            if (dayDone(d)) { n++ } else if (d.dayOfWeek != java.time.DayOfWeek.MONDAY) break
            d = d.minusDays(1)
            if (n > 1000 || d.isBefore(today.minusYears(2))) break
        }
        return n
    }

    // ---------- LetsDefend alert triage ----------

    fun alertsInMonth(day: LocalDate): Int =
        alertLog.filterKeys { it.startsWith(day.toString().take(7)) }.values.sum()

    /** Alerts logged in the Tue–Mon study week containing [day]. */
    fun alertsInWeek(day: LocalDate): Int {
        val back = (day.dayOfWeek.value - java.time.DayOfWeek.TUESDAY.value + 7) % 7
        val start = day.minusDays(back.toLong())
        return (0..6L).sumOf { alertLog[start.plusDays(it).toString()] ?: 0 }
    }

    fun alertsLeftThisMonth(day: LocalDate) = (ALERTS_PER_MONTH - alertsInMonth(day)).coerceAtLeast(0)

    /** +1 / -1 alert on [day]. Respects the 15/month free cap. Returns false if capped. */
    fun logAlert(day: LocalDate, delta: Int): Boolean {
        val k = day.toString()
        val cur = alertLog[k] ?: 0
        if (delta > 0 && alertsLeftThisMonth(day) <= 0) return false
        val next = (cur + delta).coerceAtLeast(0)
        if (next == 0) alertLog.remove(k) else alertLog[k] = next
        if (delta > 0) {
            if (item("ld_alerts")?.status == Status.TODO) markWorking("ld_alerts", day)
            studyDone.add("$k|alerts")
        } else if (next == 0) studyDone.remove("$k|alerts")
        return true
    }

    /** One-time data fixes, each runs once per install. */
    fun applyOneTimeFixes(today: LocalDate) {
        // v2.8: TryHackMe replaces TCM structured sessions, TCM Help Desk (paywalled) and LetsDefend alerts.
        // CS1015 + Study.com paused until Klaira (WGU) replies. Projects wait until Mar 20, 2027.
        if ("fix:thm28" !in studyDone) {
            items.removeAll { it.id in listOf("tcm_helpdesk", "ld_alerts") || (it.id == "tcm_ld" && it.status != Status.DONE) }
            proposals.removeAll { it.itemId in listOf("tcm_ld", "tcm_helpdesk", "ld_alerts", "cs1015", "sdc_ethics", "sdc_infosec", "sdc_forensics") }
            val d = Plan.defaults().associateBy { it.id }
            listOf("cs1015", "sdc_ethics", "sdc_infosec", "sdc_forensics").forEach { id ->
                studyDone.add("paused:$id")
                d[id]?.let { def -> item(id)?.title = def.title }
            }
            listOf("wgu_confirm", "lab_ad", "lab_net", "lab_ticket", "lab_ps", "messer_core1", "messer_core2").forEach { id ->
                val def = d[id] ?: return@forEach
                item(id)?.takeIf { it.status != Status.DONE }?.let { it.title = def.title; it.start = def.start; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 }
            }
            item("wgu_confirm")?.takeIf { it.status == Status.WORKING }?.let { it.status = Status.TODO }
            studyDone.removeAll { it.startsWith("tcm:") }
            studyShift = 0
            studyDone.add("fix:thm28")
        }
        // Nov 10 2026: Klaira confirmed by phone that CS1015 + the 3 Study.com courses transfer. Un-pause them.
        if ("fix:unpause29" !in studyDone) unpauseTransfers(today)
        // v3.0: Packet Tracer + a 3rd networking Saturday (Dec 5). SOC L1 Dec 12-Mar 27, projects from Apr 3.
        if ("fix:pt30" !in studyDone) {
            val d = Plan.defaults().associateBy { it.id }
            listOf("thm_free", "thm_soc1", "lab_ad", "lab_net", "lab_ticket", "lab_ps").forEach { id ->
                val def = d[id] ?: return@forEach
                item(id)?.takeIf { it.status != Status.DONE }?.let { it.title = def.title; it.start = def.start; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 }
            }
            studyDone.add("fix:pt30")
        }
        // v4.0: CS1015 as 26 bundles (Sophia + TryHackMe + Packet Tracer), burnout guard, job nights, Study.com on Thursdays.
        if ("fix:v40" !in studyDone) {
            items.removeAll { it.id == "thm_free" }
            val d = Plan.defaults().associateBy { it.id }
            listOf("cs1015", "thm_soc1", "sdc_ethics", "sdc_infosec", "sdc_forensics", "lab_ad", "lab_net", "lab_ticket", "lab_ps").forEach { id ->
                val def = d[id] ?: return@forEach
                item(id)?.takeIf { it.status != Status.DONE }?.let { it.title = def.title; if (id != "cs1015") it.start = def.start; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 }
            }
            proposals.removeAll { it.itemId in listOf("cs1015", "thm_free", "thm_soc1") }
            studyDone.removeAll { it.startsWith("cs:") || it.startsWith("pt:") || Regex("thm:\\d+:\\d+").matches(it) || it.startsWith("thm:") || it.startsWith("vid:") || it.contains("|thmwk:") }
            studyDone.add("fix:v40")
        }
        // v4.1: Sophia reactivated Nov 22, bundles start Dec 1 -> CS1015 ~Jan 12, SOC L1 Jan 16-Apr 24, projects May 1.
        if ("fix:v41" !in studyDone) {
            val d = Plan.defaults().associateBy { it.id }
            listOf("cs1015", "thm_soc1", "lab_ad", "lab_net", "lab_ticket", "lab_ps").forEach { id ->
                val def = d[id] ?: return@forEach
                item(id)?.takeIf { it.status != Status.DONE }?.let { if (id != "cs1015") it.start = def.start; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 }
            }
            studyDone.add("fix:v41")
        }
        // v3.2: real CS1015 syllabus (26 graded pieces) runs to Jan 7 at 2 per commute day.
        if ("fix:cs32" !in studyDone) {
            val def = Plan.defaults().first { it.id == "cs1015" }
            item("cs1015")?.takeIf { it.status != Status.DONE }?.let { it.title = def.title; it.end = def.end; it.originalEnd = def.end; it.autoExtended = 0 }
            proposals.removeAll { it.itemId == "cs1015" }
            studyDone.add("fix:cs32")
        }
        // Joseph finished all 5 free LetsDefend courses before starting the app (told Claude Nov 7, 2026).
        // v2.1: shorter, cleaner item titles.
        // v2.6: TCM SOC 101 is the free YouTube version (~12 hrs), so it ends Dec 20.
        if ("fix:tcmfree26" !in studyDone) {
            item("tcm_ld")?.let { t ->
                t.title = "TCM SOC 101 (free YouTube)"
                if (t.status != Status.DONE) { t.end = LocalDate.parse("2026-12-20"); t.originalEnd = t.end; t.autoExtended = 0 }
            }
            studyDone.add("fix:tcmfree26")
        }
        // v2.4: Study.com course codes + CS1015 name.
        if ("fix:titles24" !in studyDone) {
            val d = Plan.defaults().associateBy { it.id }
            listOf("cs1015", "sdc_ethics", "sdc_infosec", "sdc_forensics").forEach { id -> d[id]?.let { def -> item(id)?.title = def.title } }
            studyDone.add("fix:titles24")
        }
        if ("fix:titles21" !in studyDone) {
            val d = Plan.defaults().associateBy { it.id }
            items.forEach { d[it.id]?.let { def -> it.title = def.title } }
            studyDone.add("fix:titles21")
        }
        if ("fix:ld5done" !in studyDone) {
            LD_FREE.forEach { id -> if (item(id)?.status != Status.DONE) markDone(id, today, today, shift = false) }
            studyDone.add("fix:ld5done")
        }
    }

    fun toggleLdCourse(id: String, today: LocalDate) {
        val it = item(id) ?: return
        // Finishing a side course early or late shouldn't move the rest of the plan.
        if (it.status == Status.DONE) undo(id, today) else markDone(id, today, today, shift = false)
    }

    fun shiftStudy(days: Int, today: LocalDate) {
        studyShift = (studyShift + days).coerceAtLeast(0)
        note(today, if (days > 0) "Study plan pushed back ${days / 7} week" else "Study plan pulled forward ${-days / 7} week")
    }

    fun stageTitle(id: String) = Plan.stages.firstOrNull { it.id == id }?.title?.substringAfter(". ") ?: id

    private fun signed(d: Int) = if (d > 0) "$d days later" else "${-d} days earlier"
}
