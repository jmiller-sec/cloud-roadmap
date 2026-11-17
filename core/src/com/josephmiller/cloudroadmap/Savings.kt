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
    fun carSaved(r: Roadmap) = minOf(CAR_GOAL, done(r).count { !it.isAfter(CAR_LAST) } * CAR_PER)
    fun wguSaved(r: Roadmap) = done(r).size * WGU_PER

    fun paydayTask(d: LocalDate, r: Roadmap): DayTask? {
        if (!isPayday(d)) return null
        val wgu = "WGU: \$${wguSaved(r)} of \$$WGU_GOAL."
        return if (!d.isAfter(CAR_LAST)) DayTask(key(d), "money", "Payday · save \$500",
            "Move \$$CAR_PER to the car fund (Texans CU) and \$$WGU_PER to WGU savings. Car: \$${carSaved(r)} of \$$CAR_GOAL by Mar 1. $wgu Tap the circle once it's moved.", "car")
        else DayTask(key(d), "money", "Payday · WGU savings",
            "Move \$$WGU_PER to WGU savings (the car note and insurance come out of the rest; keep them under \$$CAR_MAX_MONTHLY a month). $wgu Tap the circle once it's moved.", "college_save")
    }
}
