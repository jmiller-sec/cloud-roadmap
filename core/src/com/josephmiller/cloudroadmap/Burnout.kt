package com.josephmiller.cloudroadmap

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Burnout guard.
 * - Plan C: 1 bundle Tue + Thu evening, 3 on Saturday. Plan B: Saturday drops to 2.
 * - Two low energy ratings (1-2) in a row -> Plan B. Ratings back up -> Plan C.
 * - Three weeks in a row averaging 2 or less -> rest week (no study).
 * - Daily caps: 2 h Tue/Thu, 6 h Sat. 50 min on, 10 min break. Override = 5/5 quiz, once a day, +30 min.
 * - No new bundle starts after 8 pm on Tue/Thu.
 * - Missed days never stack: each day only ever shows its own amount.
 */
enum class Mode { C, B, REST }

object Burnout {
    const val WORK_MIN = 50
    const val BREAK_MIN = 10
    const val OVERRIDE_MIN = 30
    const val LATE_HOUR = 20
    val JOB_NIGHTS_FROM: LocalDate = LocalDate.parse("2027-01-19")
    /** Everything starts Tue Dec 1 (Sophia reactivated that day). */
    val BUNDLES_FROM: LocalDate = LocalDate.parse("2026-12-01")

    fun isStudyDay(d: LocalDate) = d.dayOfWeek in setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)

    fun baseCapMin(d: LocalDate) = when (d.dayOfWeek) {
        DayOfWeek.SATURDAY -> 360
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY -> 120
        else -> 0
    }

    fun capMin(d: LocalDate, r: Roadmap) = baseCapMin(d) + if ("override:$d" in r.studyDone) OVERRIDE_MIN else 0

    // ---------- energy ----------
    fun rating(r: Roadmap, d: LocalDate): Int? = r.energy[d.toString()]

    /** Ratings strictly before [d], newest first. */
    private fun ratingsBefore(r: Roadmap, d: LocalDate) =
        r.energy.entries.map { LocalDate.parse(it.key) to it.value }.filter { it.first.isBefore(d) }.sortedByDescending { it.first }

    /** Rest week when the 3 most recent study weeks before this one (off weeks skipped) each averaged 2 or less. */
    fun isRestWeek(r: Roadmap, d: LocalDate): Boolean {
        val sat = ThmPlan.saturdayOf(d)
        val all = r.energy.entries.map { LocalDate.parse(it.key) to it.value }
        val weeks = generateSequence(sat.minusWeeks(1)) { it.minusWeeks(1) }.take(12).filter { it !in ThmPlan.OFF }.take(3).toList()
        return weeks.size == 3 && weeks.all { wkSat ->
            val inWeek = all.filter { !it.first.isAfter(wkSat) && it.first.isAfter(wkSat.minusDays(7)) }.map { it.second }
            inWeek.isNotEmpty() && inWeek.average() <= 2.0
        }
    }

    fun mode(r: Roadmap, d: LocalDate): Mode {
        if (isRestWeek(r, d)) return Mode.REST
        if ("planC:${ThmPlan.saturdayOf(d)}" in r.studyDone) return Mode.C
        val last2 = ratingsBefore(r, d).take(2)
        return if (last2.size == 2 && last2.all { it.second <= 2 }) Mode.B else Mode.C
    }

    /** How many bundles this day is meant to hold. */
    fun bundlesFor(r: Roadmap, d: LocalDate): Int {
        if (ThmPlan.isOffWeek(d) || d.isBefore(BUNDLES_FROM)) return 0
        val m = mode(r, d)
        if (m == Mode.REST) return 0
        return when (d.dayOfWeek) {
            DayOfWeek.TUESDAY -> if (d.isBefore(JOB_NIGHTS_FROM)) 1 else 0
            DayOfWeek.THURSDAY -> 1
            DayOfWeek.SATURDAY -> if (m == Mode.B) 2 else 3
            else -> 0
        }
    }

    // ---------- timer ----------
    /** Minutes studied on [d], including a running session (counted up to the 50-minute block). */
    fun minutesOn(r: Roadmap, d: LocalDate, nowMs: Long): Int {
        val base = r.studyMin[d.toString()] ?: 0
        return base + runningSessionMin(r, d, nowMs)
    }

    fun runningSessionMin(r: Roadmap, d: LocalDate, nowMs: Long): Int {
        if (r.timerStart <= 0 || r.timerDay != d.toString()) return 0
        return ((nowMs - r.timerStart) / 60000L).toInt().coerceIn(0, WORK_MIN)
    }

    fun capReached(r: Roadmap, d: LocalDate, nowMs: Long) = baseCapMin(d) > 0 && minutesOn(r, d, nowMs) >= capMin(d, r)

    fun startTimer(r: Roadmap, d: LocalDate, nowMs: Long) {
        if (r.timerStart > 0) return
        r.timerStart = nowMs; r.timerDay = d.toString()
    }

    /** Stop the running session and bank its minutes (a session never counts past its 50-minute block). */
    fun stopTimer(r: Roadmap, nowMs: Long) {
        if (r.timerStart <= 0) return
        val day = LocalDate.parse(r.timerDay)
        val mins = runningSessionMin(r, day, nowMs)
        r.studyMin[r.timerDay] = (r.studyMin[r.timerDay] ?: 0) + mins
        r.timerStart = 0; r.timerDay = ""
    }

    // ---------- override ----------
    fun overrideUsed(r: Roadmap, d: LocalDate) = "quiz:$d" in r.studyDone

    /** Record an override quiz. Perfect score: +30 min today, and this week back on Plan C. */
    fun recordOverride(r: Roadmap, d: LocalDate, score: Int, of: Int) {
        r.studyDone.add("quiz:$d")
        if (score == of) {
            r.studyDone.add("override:$d")
            r.studyDone.add("planC:${ThmPlan.saturdayOf(d)}")
        }
    }

    fun currentUnit(r: Roadmap) = Bundles.next(r).firstOrNull()?.unit ?: 5

    // ---------- job nights ----------
    val interviewQs = listOf(
        "Walk me through how you'd troubleshoot a user who can't get online.",
        "What's the difference between a switch and a router?",
        "A user forgot their password and is locked out. What do you do?",
        "Explain DNS to someone non-technical.",
        "Tell me about a time you handled an upset customer (use an airport example).",
        "What does DHCP do, and what happens if it fails?",
        "How do you prioritize 5 tickets that all came in at once?",
        "What's a VLAN and why would a company use one?",
        "Someone gets a suspicious email. What steps do you take?",
        "Why do you want to move from maintenance into IT?",
    )

    fun jobTask(d: LocalDate): DayTask? {
        if (d.dayOfWeek != DayOfWeek.TUESDAY || d.isBefore(JOB_NIGHTS_FROM) || ThmPlan.isOffWeek(d)) return null
        val q = interviewQs[((d.toEpochDay() / 7) % interviewQs.size).toInt()]
        return DayTask("job:$d", "job", "Evening · job hunt",
            "Send 5 help desk / IT support applications (tailor the top of your resume to each). Then practice out loud: \"$q\"",
            "helpdesk_job", "https://www.indeed.com/jobs?q=help+desk&l=Dallas%2C+TX")
    }
}
