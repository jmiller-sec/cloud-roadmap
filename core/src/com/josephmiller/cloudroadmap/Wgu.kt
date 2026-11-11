package com.josephmiller.cloudroadmap

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** One course in WGU's B.S. Cybersecurity and Information Assurance standard path. flag = "" | "new" | "removed" */
class WguCourse(val name: String, val cu: Int, val term: Int, var flag: String = "") {
    fun toJson(): JSONObject = JSONObject().put("name", name).put("cu", cu).put("term", term).put("flag", flag)

    companion object {
        fun fromJson(o: JSONObject) = WguCourse(o.getString("name"), o.optInt("cu"), o.optInt("term"), o.optString("flag"))
    }
}

object WguPlan {
    /** Baseline: WGU program guide, Catalog Version 202610 (published 6/2/2026). */
    const val BASE_CATALOG = "202610"

    /** Courses Joseph plans to cover with Study.com transfer credit. Confirm with a WGU enrollment counselor. */
    val studyComTransfers = mapOf(
        "Ethics in Technology" to "Study.com",
        "Fundamentals of Information Security" to "Study.com",
        "Digital Forensics in Cybersecurity" to "Study.com",
    )

    fun baseline(): MutableList<WguCourse> = listOf(
        Triple("Health, Fitness, and Wellness", 4, 1), Triple("Practical Applications of Prompt", 2, 1),
        Triple("Composition: Successful Self-Expression", 3, 1), Triple("Introduction to IT", 3, 1),
        Triple("Ethics in Technology", 3, 2), Triple("IT Foundations", 4, 2),
        Triple("Applied Probability and Statistics", 3, 2), Triple("Natural Science Lab", 2, 2),
        Triple("IT Applications", 4, 3), Triple("Applied Algebra", 3, 3),
        Triple("Fundamentals of Information Security", 3, 3), Triple("Network and Security - Foundations", 3, 3),
        Triple("Critical Thinking: Reason and Evidence", 3, 4), Triple("Networks", 4, 4),
        Triple("Introduction to Communication: Connecting with Others", 3, 4), Triple("Business of IT - Applications", 4, 4),
        Triple("American Politics and the US Constitution", 3, 5), Triple("Network and Security - Applications", 4, 5),
        Triple("Legal Issues in Information Security", 4, 5), Triple("Applied Discrete Mathematics", 3, 5),
        Triple("Introduction to Cryptography", 4, 6), Triple("Digital Forensics in Cybersecurity", 4, 6),
        Triple("Information Systems Security", 4, 6), Triple("Foundations of Programming (Python)", 3, 7),
        Triple("Version Control", 1, 7), Triple("Python for IT Automation", 3, 7),
        Triple("Introduction to AI and Security", 2, 7), Triple("Software Security and Testing", 3, 7),
        Triple("Linux Foundations", 3, 8), Triple("Data Management - Foundations", 3, 8),
        Triple("Data Analytics - Applications", 4, 8), Triple("Managing Information Security", 3, 8),
        Triple("Business of IT - Project Management", 4, 9), Triple("Cyber Defense and Countermeasures", 4, 9),
        Triple("Managing Cloud Security", 4, 9), Triple("Penetration Testing and Vulnerability Analysis", 4, 10),
        Triple("Cybersecurity and Information Assurance Capstone", 4, 10),
    ).map { (n, c, t) -> WguCourse(n, c, t) }.toMutableList()

    fun listToJson(l: List<WguCourse>) = JSONArray().apply { l.forEach { put(it.toJson()) } }
    fun listFromJson(a: JSONArray) = MutableList(a.length()) { WguCourse.fromJson(a.getJSONObject(it)) }

    private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    /**
     * Compare the feed's current course list with what the app has. Marks new courses "new", keeps removed ones
     * (flagged "removed") so Joseph sees what disappeared. Returns human-readable change lines.
     */
    fun merge(current: MutableList<WguCourse>, incoming: List<WguCourse>): List<String> {
        if (incoming.isEmpty()) return emptyList()
        val have = current.filter { it.flag != "removed" }.associateBy { norm(it.name) }
        val inc = incoming.associateBy { norm(it.name) }
        val lines = mutableListOf<String>()
        incoming.forEach { c -> if (norm(c.name) !in have) { lines += "Added: ${c.name}"; c.flag = "new" } else c.flag = have[norm(c.name)]!!.flag.takeIf { it == "new" } ?: "" }
        val removed = current.filter { it.flag != "removed" && norm(it.name) !in inc }.onEach { it.flag = "removed"; lines += "Removed: ${it.name}" }
        val keepOldRemoved = current.filter { it.flag == "removed" && norm(it.name) !in inc }
        current.clear()
        current.addAll(incoming.sortedWith(compareBy({ it.term }, { incoming.indexOf(it) })))
        current.addAll(removed + keepOldRemoved)
        return lines
    }
}

/** Study history stats for the Progress section. */
object Progress {
    fun studyDays(r: Roadmap): Set<LocalDate> = r.studyDone.mapNotNull {
        val d = it.substringBefore("|")
        if (d.length == 10 && d[4] == '-') runCatching { LocalDate.parse(d) }.getOrNull() else null
    }.toSet()

    fun bestStreak(r: Roadmap): Int {
        val days = studyDays(r).sorted()
        if (days.isEmpty()) return 0
        var best = 1; var run = 1
        for (i in 1 until days.size) {
            var gap = Roadmap.days(days[i - 1], days[i])
            // A skipped Monday (off day) doesn't break a streak.
            if (gap == 2 && days[i - 1].plusDays(1).dayOfWeek == java.time.DayOfWeek.MONDAY) gap = 1
            run = if (gap == 1) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }

    /** Study days per Tue–Mon week, oldest first, ending with the current week. */
    fun weekly(r: Roadmap, today: LocalDate, weeks: Int = 12): List<Pair<LocalDate, Int>> {
        val days = studyDays(r)
        val back = (today.dayOfWeek.value - java.time.DayOfWeek.TUESDAY.value + 7) % 7
        val thisWeek = today.minusDays(back.toLong())
        return (weeks - 1 downTo 0).map { w ->
            val start = thisWeek.minusWeeks(w.toLong())
            start to (0..6L).count { start.plusDays(it) in days }
        }
    }

    fun totalAlerts(r: Roadmap) = r.alertLog.values.sum()
}
