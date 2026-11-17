package com.josephmiller.cloudroadmap

import java.time.LocalDate

/**
 * Per-paycheck money goals (Nov 17 2026). Paid on the 7th and 22nd; he can save $500 a check.
 * - Car: $2,000 down by Mar 1 2027 -> $300 a check, Nov 22 - Feb 22 (7 checks). Fund started from $0.
 * - WGU first term (Aug 2027): $4,425 tuition + $200 fee = $4,625, minus his estimated Pell
 *   (2027-28 FAFSA: up to $2,290 a year, about $1,145 a term) = about $3,480. $200 every check, 17 checks = $3,400;
 *   the WGU Cybersecurity Scholarship ($1,500 a term) covers the rest and then some.
 * - After the car (Mar 7 on): the car note + insurance comes out of the same $500, so keep it under $600 a month.
 */
object Savings {
    const val CAR_GOAL = 2000
    const val CAR_PER = 300
    const val WGU_GOAL = 3480
    const val WGU_PER = 200
    const val CAR_MAX_MONTHLY = 600
    val FIRST: LocalDate = LocalDate.parse("2026-11-22")
    val CAR_LAST: LocalDate = LocalDate.parse("2027-02-22")
    val LAST: LocalDate = LocalDate.parse("2027-07-22")

    fun isPayday(d: LocalDate) = (d.dayOfMonth == 7 || d.dayOfMonth == 22) && !d.isBefore(FIRST) && !d.isAfter(LAST)
    fun key(d: LocalDate) = "save:$d"
    val paydays: List<LocalDate> get() = generateSequence(FIRST) { it.plusDays(1) }.takeWhile { !it.isAfter(LAST) }.filter { isPayday(it) }.toList()
    private fun done(r: Roadmap) = paydays.filter { key(it) in r.studyDone }

    /** What actually went into [goal] ("car" or "wgu") on [d]: the logged amount, or the plan amount for an old tick with no amount. */
    fun amountOn(r: Roadmap, goal: String, d: LocalDate): Int {
        val extra = r.money["$goal+|$d"] ?: 0
        val logged = r.money["$goal|$d"]
        val payday = when {
            logged != null -> logged
            isPayday(d) && key(d) in r.studyDone -> if (goal == "car") (if (d.isAfter(CAR_LAST)) 0 else CAR_PER) else WGU_PER
            else -> 0
        }
        return payday + extra
    }

    private fun allDates(r: Roadmap): List<LocalDate> =
        (paydays + r.money.keys.mapNotNull { k -> runCatching { LocalDate.parse(k.substringAfter("|")) }.getOrNull() }).distinct().sorted()

    fun saved(r: Roadmap, goal: String, upTo: LocalDate? = null) =
        allDates(r).filter { upTo == null || !it.isAfter(upTo) }.sumOf { amountOn(r, goal, it) }
    fun carSaved(r: Roadmap) = saved(r, "car")
    fun wguSaved(r: Roadmap) = saved(r, "wgu")

    /** Payday step done: record what was actually moved. */
    fun recordPayday(r: Roadmap, d: LocalDate, car: Int, wgu: Int) {
        r.money["car|$d"] = car; r.money["wgu|$d"] = wgu; r.studyDone.add(key(d))
    }

    /** Money moved outside a payday (or taken out: negative). Adds to anything logged that day. */
    fun recordExtra(r: Roadmap, d: LocalDate, car: Int, wgu: Int) {
        if (car != 0) r.money["car+|$d"] = (r.money["car+|$d"] ?: 0) + car
        if (wgu != 0) r.money["wgu+|$d"] = (r.money["wgu+|$d"] ?: 0) + wgu
    }

    /** One point per payday: what the plan says you should have by then, and what you've actually saved (null after today). */
    class Point(val day: LocalDate, val planned: Int, val actual: Int?)

    fun carSeries(r: Roadmap, today: LocalDate): List<Point> {
        var plan = 0
        return paydays.filter { !it.isAfter(CAR_LAST) }.map { d ->
            plan = minOf(CAR_GOAL, plan + CAR_PER)
            Point(d, plan, if (d.isAfter(today)) null else saved(r, "car", d))
        }
    }

    fun wguSeries(r: Roadmap, today: LocalDate): List<Point> {
        var plan = 0
        return paydays.map { d ->
            plan += WGU_PER
            Point(d, plan, if (d.isAfter(today)) null else saved(r, "wgu", d))
        }
    }

    /** How far ahead (+) or behind (-) of the plan you are today. */
    fun gap(r: Roadmap, goal: String, series: List<Point>, today: LocalDate): Int {
        val past = series.filter { !it.day.isAfter(today) }
        return if (past.isEmpty()) 0 else saved(r, goal, today) - past.last().planned
    }

    fun paydayTask(d: LocalDate, r: Roadmap): DayTask? {
        if (!isPayday(d)) return null
        val wgu = "WGU: \$${wguSaved(r)} of \$$WGU_GOAL."
        return if (!d.isAfter(CAR_LAST)) DayTask(key(d), "money", "Payday · save \$500",
            "Move \$$CAR_PER to the car fund (Texans CU) and \$$WGU_PER to WGU savings. Car: \$${carSaved(r)} of \$$CAR_GOAL by Mar 1. $wgu Tap the circle and enter what you actually moved.", "car")
        else DayTask(key(d), "money", "Payday · WGU savings",
            "Move \$$WGU_PER to WGU savings (the car note and insurance come out of the rest; keep them under \$$CAR_MAX_MONTHLY a month). $wgu Tap the circle and enter what you actually moved.", "college_save")
    }
}
