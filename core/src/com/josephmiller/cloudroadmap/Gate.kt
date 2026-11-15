package com.josephmiller.cloudroadmap

import java.time.LocalDate

/**
 * Two locks that make the plan foolproof:
 *  1. Burnout check-in: on any study day, nothing starts (timer, bundle, warm-up) until you rate how you feel right now.
 *     1 = rest day, 2 = light day, 3-5 = normal (sleep rules still apply on top).
 *  2. Recall gate: before a new lesson you must pass 2 of 3 questions on the last one. Fail = locked.
 *     Each retry gets different questions. After 2 misses in a row you must rewatch the last lesson's video first.
 * Plus the two warm-up Saturdays before Dec 1 (60 min, Packet Tracer basics + lesson videos 1-3).
 */
object Gate {
    const val PASS = 2
    const val OF = 3

    // ---------- check-in + sleep: both have to agree, the lower one wins ----------
    enum class Level { REST, LIGHT, NORMAL }

    fun checkin(r: Roadmap, d: LocalDate): Int? = r.checkin[d.toString()]
    /** Check-in alone: 1 = rest (0), 2 = light (1), 3 = reduced (2), 4-5 = full (3). */
    fun checkScore(v: Int) = when { v <= 1 -> 0; v == 2 -> 1; v == 3 -> 2; else -> 3 }
    /** Sleep alone: under 5h30 = light (1), 5h30-7h = reduced (2), 7h+ = full (3). */
    fun sleepScore(min: Int) = when { min < Burnout.SLEEP_LOW_MIN -> 1; min < Burnout.SLEEP_GOOD_MIN -> 2; else -> 3 }
    /**
     * The day's score, 0-3. Sleep and check-in must match: the lower one wins, so neither can talk the other up.
     * Once you've checked in, missing watch data counts as "reduced" (wear the watch). Before check-in it's a preview from sleep only.
     */
    fun score(r: Roadmap, d: LocalDate): Int {
        val s = Burnout.sleepMin(r, d)?.let(::sleepScore)
        val c = checkin(r, d)?.let(::checkScore) ?: return s ?: 3
        return minOf(c, s ?: 2)
    }
    fun scoreName(x: Int) = when (x) { 0 -> "rest"; 1 -> "light"; 2 -> "reduced"; else -> "full" }
    /** One line explaining how today's load was decided. */
    fun why(r: Roadmap, d: LocalDate): String? {
        val c = checkin(r, d) ?: return null
        val m = Burnout.sleepMin(r, d)
        val sl = if (m == null) "no watch data (counts as reduced)" else "slept ${Burnout.fmtSleep(m)} (${scoreName(sleepScore(m))})"
        return "Check-in $c/5 (${scoreName(checkScore(c))}) + $sl → ${scoreName(score(r, d))} day. The lower one wins."
    }
    fun level(r: Roadmap, d: LocalDate): Level? = checkin(r, d)?.let { when (score(r, d)) { 0 -> Level.REST; 1 -> Level.LIGHT; else -> Level.NORMAL } }
    fun isPlanned(d: LocalDate) = d in WARMUP || (Burnout.isStudyDay(d) && !d.isBefore(Burnout.BUNDLES_FROM) && !ThmPlan.isOffWeek(d))
    fun needsCheckin(r: Roadmap, d: LocalDate) = isPlanned(d) && checkin(r, d) == null
    fun setCheckin(r: Roadmap, d: LocalDate, v: Int) { r.checkin[d.toString()] = v.coerceIn(1, 5) }

    // ---------- recall gate (bundles) ----------
    fun key(n: Int) = "gate:b:$n"
    fun passed(r: Roadmap, n: Int) = n <= 1 || key(n) in r.studyDone
    fun fails(r: Roadmap, n: Int) = r.studyDone.count { it.startsWith("gatefail:b:$n:") }
    fun reviews(r: Roadmap, n: Int) = r.studyDone.count { it.startsWith("gatereview:b:$n:") }
    /** After every 2 misses you have to review before the next try. */
    fun needsReview(r: Roadmap, n: Int) = !passed(r, n) && fails(r, n) >= 2 * (reviews(r, n) + 1)
    fun markReviewed(r: Roadmap, n: Int) { r.studyDone.add("gatereview:b:$n:${reviews(r, n) + 1}") }
    /** Questions on the bundle before [n]; a new set each attempt. */
    fun questions(n: Int, day: LocalDate, r: Roadmap): List<Q> {
        val prev = Bundles.all.getOrNull(n - 2) ?: Bundles.all.first()
        return Quiz.pick(prev.unit, OF, day, "gate$n:${fails(r, n)}")
    }
    /** Records a try. Returns true when it passed (and ticks the bundle's warm-up section). */
    fun record(r: Roadmap, b: StudyBundle, score: Int, day: LocalDate): Boolean {
        return if (score >= PASS) {
            r.studyDone.add(key(b.n)); r.studyDone.add(b.key("warm")); r.studyDone.add("$day|b:${b.n}"); true
        } else { r.studyDone.add("gatefail:b:${b.n}:${fails(r, b.n) + 1}"); false }
    }
    /** What to rewatch before retrying: the "learn it" video for the previous lesson. */
    fun reviewVideo(n: Int): LessonVideo? = Videos.lessonByBundle[n - 1]?.pn

    // ---------- lesson videos ----------
    /** A lesson's video pair only opens once the previous lesson's "learn it" video was passed (2/3 quick check). */
    /** Result of a video quick check: a pass ticks the video everywhere, a miss counts toward new questions. */
    fun recordVideo(r: Roadmap, key: String, score: Int): Boolean {
        if (score >= PASS) { r.studyDone.add(key); r.studyDone.add("quiz:$key:$score") }
        else r.studyDone.add("vidfail:$key:${videoFails(r, key) + 1}")
        return score >= PASS
    }
    fun videoUnlocked(r: Roadmap, n: Int) = n <= 1 || "vid:${n - 1}:pn" in r.studyDone
    fun videoFails(r: Roadmap, pairKey: String) = r.studyDone.count { it.startsWith("vidfail:$pairKey:") }

    // ---------- warm-up Saturdays (before Dec 1) ----------
    private const val PT_URL = "https://www.netacad.com/courses/getting-started-cisco-packet-tracer"
    val WARMUP: Map<LocalDate, Int> = mapOf(LocalDate.parse("2026-11-14") to 1, LocalDate.parse("2026-11-15") to 1, LocalDate.parse("2026-11-21") to 2)
    const val WARMUP_CAP = 60
    /** One-off: the Sunday he asked for a hands-on taste. Lesson 1 videos first, then two intro rooms, same 60-minute cap. */
    val THM_INTRO_DAY: LocalDate = LocalDate.parse("2026-11-15")
    /** Hands-on part only on a full day: check-in 4-5 AND 7h+ of sleep. Anything less = videos only. */
    fun warmupFull(r: Roadmap, d: LocalDate) = checkin(r, d) != null && score(r, d) == 3

    fun warmupTasks(r: Roadmap, d: LocalDate): List<DayTask> {
        val n = WARMUP[d] ?: return emptyList()
        if (checkin(r, d) != null && score(r, d) == 0) return listOf(DayTask("$d|rest", "rest", "Rest day", "Your check-in says you're empty. Warm-up skipped; Dec 1 is what matters.", null))
        val out = mutableListOf<DayTask>()
        Videos.lessonByBundle[n]?.let { pair ->
            listOf("pn" to pair.pn, "nc" to pair.chuck).forEach { (k, v) ->
                out += DayTask(pair.key(k), "lvideo", "Step ${if (k == "pn") 1 else 2}: watch (${v.mins} min)",
                    "${v.channel}: ${v.title}. Then tap the circle and pass the quick check (2 of 3). Miss it: rewatch and retry.", null, v.url)
            }
        }
        if (warmupFull(r, d)) {
            if (d == THM_INTRO_DAY) {
                // Sun Nov 15: two short TryHackMe intro rooms that are NOT part of any lesson (the lesson rooms stay untouched for Dec).
                out += DayTask("todo:warm_thm_off", "todo", "Step 3: hands-on (about 10 min)",
                    "TryHackMe: Offensive Security Intro. Make your free account first, then hack the fake bank. Only this room and the next one: the networking rooms belong to your lessons.", null, "https://tryhackme.com/room/offensivesecurityintro")
                out += DayTask("todo:warm_thm_def", "todo", "Step 4: hands-on (about 15 min)",
                    "TryHackMe: Defensive Security Intro. Stop a live attack on the fake bank, the way a SOC analyst would. Then you're done for the day.", null, "https://tryhackme.com/room/defensivesecurityintro")
            } else {
                val firstHalf = n == 1 || "todo:warm1_pt" !in r.studyDone
                out += DayTask("todo:warm${n}_pt", "todo", "Step 3: hands-on (about 30 min)",
                    "Cisco's free 2-hour course \"Getting Started with Cisco Packet Tracer\" (log in with your NetAcad account). " +
                        (if (firstHalf) "Start at the beginning and do the first half: the interface, adding devices, cabling." else "Do the second half: simulation mode and the final activity.") +
                        " If the link moved, search NetAcad for the course name.", null, PT_URL)
            }
        }
        else out += DayTask("$d|warmnote", "rest", "Hands-on part skipped",
            why(r, d)?.let { "$it Hands-on needs a full day (check-in 4-5 and 7h+ sleep), so it's videos only." } ?: "Check in first.", null)
        return out
    }
}
