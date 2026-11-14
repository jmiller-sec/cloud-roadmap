package com.josephmiller.cloudroadmap

import java.time.DayOfWeek
import java.time.LocalDate

/** One thing to do today. [key] is what gets stored when it's checked off. */
class DayTask(val key: String, val stream: String, val where: String, val task: String, val itemId: String? = null, val url: String? = null)

/**
 * v4.0 plan (Plan C, with burnout guard in Burnout.kt):
 * - CS1015 as 26 bundles: Tue + Thu evening 1 bundle, Saturday 3 (Plan B: 2). Each bundle = Sophia piece +
 *   matching TryHackMe room + matching Packet Tracer lab + screenshots + portfolio note.
 * - From Jan 19: Tuesday is job-hunt night. Thursdays: Study.com once CS1015 is done (from Feb 4), else the SOC video.
 * - Saturdays after CS1015: TryHackMe SOC Level 1, one module a week. Projects from May 1.
 * - Everything starts Tue Dec 1 (Sophia reactivated that day). Nothing before then.
 * - Sun, Mon, Wed, Fri: off. Off weeks Nov 28 + Dec 26. Missed days never stack.
 */
object StudyPlan {
    val labSteps: Map<String, List<String>> get() = Projects.steps

    fun projectsOpen(date: LocalDate) = !date.isBefore(ThmPlan.PROJECTS_UNLOCK)

    /** Bundles to show on [date]: ones touched today, then the next unfinished ones up to today's amount. */
    fun bundlesToday(date: LocalDate, r: Roadmap): List<StudyBundle> {
        if (!Burnout.isStudyDay(date)) return emptyList()
        val target = Burnout.bundlesFor(r, date)
        val started = Bundles.all.filter { "$date|b:${it.n}" in r.studyDone }
        val more = Bundles.next(r).filter { it !in started }.take((target - started.size).coerceAtLeast(0))
        return (started + more).sortedBy { it.n }
    }

    fun sectionTask(b: StudyBundle, s: Section) = DayTask(
        b.key(s.id), "bundle", "Bundle ${b.n}/${Bundles.all.size} · ${b.title} · ${s.title}",
        (s.steps.mapIndexed { i, st -> "${i + 1}. $st" } + s.shots.map { "📸 $it" }).joinToString("\n"), "cs1015", s.url)

    /** Everything to do on [date], in order. */
    fun tasksFor(date: LocalDate, r: Roadmap): List<DayTask> {
        val out = mutableListOf<DayTask>()
        val off = ThmPlan.isOffWeek(date)
        val mode = Burnout.mode(r, date)
        val csDone = Bundles.next(r).isEmpty()

        if (date in Gate.WARMUP) return Gate.warmupTasks(r, date)
        if (Gate.checkin(r, date) != null && Gate.score(r, date) == 0)
            return listOf(DayTask("$date|rest", "rest", "Rest day", "Your check-in says you're running on empty. Nothing today, and nothing piles up.", null))
        setupTasks[date]?.forEach { (k, t, u) -> out += DayTask(k, "todo", "Setup · 10 min, then Bundle 1", t, null, u) }
        bundlesToday(date, r).forEach { b -> b.sections.forEach { s -> out += sectionTask(b, s) } }

        if (!off && mode != Mode.REST) when (date.dayOfWeek) {
            DayOfWeek.TUESDAY -> {
                Burnout.jobTask(date)?.let { out += it }
                if (csDone && Burnout.jobTask(date) == null) videoTask(date, r)?.let { out += it }
            }
            DayOfWeek.THURSDAY -> if (csDone) {
                val c = studyComFor(date, r)
                if (c != null) out += DayTask("$date|course", "course", "Evening · Study.com",
                    "${c.title}: next lesson + its quiz. Tap Done on the Roadmap tab when the whole course is finished.",
                    c.id, "https://study.com/academy/dashboard.html")
                else videoTask(date, r)?.let { out += it }
            }
            DayOfWeek.SATURDAY -> if (csDone) {
                val wk = ThmPlan.weeks.firstOrNull { "$date|thmwk:${it.n}" in r.studyDone } ?: ThmPlan.next(r)
                if (wk != null) out += DayTask(wk.key, "thm", "Work laptop · TryHackMe ${wk.n}/${ThmPlan.weeks.size}",
                    "${wk.module}: ${wk.rooms}", "thm_soc1", ThmPlan.PATH_SOC1)
                else if (projectsOpen(date)) nextLabStep(r)?.let { out += it }
            }
            else -> {}
        }

        if (out.isEmpty()) out += DayTask("$date|rest", "rest", "Day off", when {
            off -> "Week off. No studying, no app. Rest up."
            mode == Mode.REST -> "Rest week: your energy has been low for 3 weeks. No studying. Nothing piles up."
            else -> "Nothing due today. Rest up."
        })
        return out
    }

    /** One-time setup on day 1 (Tue Dec 1), before Bundle 1. Nothing at all before Dec 1. */
    val setupTasks: Map<LocalDate, List<Triple<String, String, String>>> = mapOf(
        LocalDate.parse("2026-12-01") to listOf(
            Triple("todo:setup_sophia", "Reactivate Sophia: 1-month plan (\$99). Open CS1015 once to make sure it loads.", "https://app.sophia.org"),
            Triple("todo:setup_thm", "Make a free TryHackMe account and join the Pre Security path.", "https://tryhackme.com/path/outline/presecurity"),
        ),
    )

    private fun videoTask(date: LocalDate, r: Roadmap): DayTask? {
        val wk = ThmPlan.next(r) ?: return null
        val tue = date.dayOfWeek == DayOfWeek.TUESDAY
        val v = (if (tue) wk.tue else wk.thu) ?: return null
        return DayTask(if (tue) wk.tueKey else wk.thuKey, "video", "Commute · video for Sat", "${v.title}. Pairs with: ${wk.module}.", "thm_soc1", v.url)
    }

    val TRANSFERS = listOf("sdc_ethics", "sdc_infosec", "sdc_forensics")

    /** The Study.com course to work on: the first unfinished one, once its start date arrives. */
    fun studyComFor(date: LocalDate, r: Roadmap): Item? =
        TRANSFERS.mapNotNull { r.item(it) }.firstOrNull { it.status != Status.DONE && !r.paused(it.id) }?.takeIf { !date.isBefore(it.start) }

    fun nextLabStep(r: Roadmap): DayTask? {
        val p = Projects.current(r) ?: return null
        val n = p.steps.indices.firstOrNull { "lab:${p.id}:$it" !in r.studyDone } ?: return null
        return DayTask("lab:${p.id}:$n", "lab", "Work laptop · ${p.level} project (step ${n + 1}/${p.steps.size})", p.steps[n], p.id)
    }
}
