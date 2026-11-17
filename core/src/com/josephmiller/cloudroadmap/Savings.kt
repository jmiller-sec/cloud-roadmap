package com.josephmiller.cloudroadmap

import java.time.LocalDate

/**
 * Per-paycheck money goal (Nov 17 2026). He's paid on the 7th and 22nd.
 * Goal: WGU's first term by Aug 1 2027 = $4,425 tuition + $200 resource fee (wgu.edu, checked Nov 17 2026).
 * 17 paychecks from Nov 22 2026 to Jul 22 2027 -> $272 each. A WGU scholarship would lower what's needed.
 */
object Savings {
    const val GOAL = 4625
    const val PER_CHECK = 272
    val FIRST: LocalDate = LocalDate.parse("2026-11-22")
    val LAST: LocalDate = LocalDate.parse("2027-07-22")

    fun isPayday(d: LocalDate) = (d.dayOfMonth == 7 || d.dayOfMonth == 22) && !d.isBefore(FIRST) && !d.isAfter(LAST)
    fun key(d: LocalDate) = "save:$d"
    val paydays: List<LocalDate> get() = generateSequence(FIRST) { it.plusDays(1) }.takeWhile { !it.isAfter(LAST) }.filter { isPayday(it) }.toList()
    fun checksDone(r: Roadmap) = paydays.count { key(it) in r.studyDone }
    fun saved(r: Roadmap) = minOf(GOAL, checksDone(r) * PER_CHECK)

    fun paydayTask(d: LocalDate, r: Roadmap): DayTask? {
        if (!isPayday(d)) return null
        val left = paydays.count { !it.isBefore(d) }
        return DayTask(key(d), "money", "Payday · WGU savings goal",
            "Move \$$PER_CHECK into savings for your first WGU term. Saved so far: \$${saved(r)} of \$$GOAL. " +
                "Paychecks left after this one: ${left - 1}. Tap the circle once it's moved.", "college_save")
    }
}
