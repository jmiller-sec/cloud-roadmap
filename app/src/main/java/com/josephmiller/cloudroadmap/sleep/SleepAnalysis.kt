package com.josephmiller.cloudroadmap.sleep

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** watch sleep stages, simplified */
enum class Stage { AWAKE, LIGHT, DEEP, REM, ASLEEP }

data class StageSpan(val start: Instant, val end: Instant, val stage: Stage) {
    val minutes get() = Duration.between(start, end).toMinutes().toInt()
}

data class SleepNight(
    val start: Instant,          // in bed / tracking started
    val end: Instant,            // woke up / tracking ended
    val stages: List<StageSpan>, // may be empty if the watch only logged the session
    val source: String = "",
)

data class NightStats(
    val date: LocalDate,         // the morning you woke up
    val bedtime: Instant,
    val wake: Instant,
    val inBedMin: Int,
    val asleepMin: Int,
    val deepMin: Int,
    val remMin: Int,
    val lightMin: Int,
    val awakeMin: Int,
    val wakeUps: Int,
    val fellAsleepMin: Int?,     // from going to bed to the first sleep stage
    val efficiency: Int,         // asleep / in bed, %
    val score: Int,              // 0..100 Fox Score
    val hasStages: Boolean,
    val stages: List<StageSpan> = emptyList(),
)

data class SleepWeek(val nights: List<NightStats>, val avgAsleepMin: Int, val bedtimeSpreadMin: Int, val goalMin: Int)

object SleepAnalysis {
    const val GOAL_MIN = 8 * 60

    fun night(n: SleepNight, zone: ZoneId): NightStats {
        val inBed = Duration.between(n.start, n.end).toMinutes().toInt().coerceAtLeast(0)
        val st = n.stages.sortedBy { it.start }
        val hasStages = st.any { it.stage != Stage.ASLEEP }
        fun sum(vararg s: Stage) = st.filter { it.stage in s }.sumOf { it.minutes }
        val deep = sum(Stage.DEEP); val rem = sum(Stage.REM); val light = sum(Stage.LIGHT, Stage.ASLEEP)
        val awake = if (st.isEmpty()) 0 else sum(Stage.AWAKE)
        val asleep = if (st.isEmpty()) inBed else deep + rem + light
        // a wake-up = an awake stretch of 2+ minutes between two sleep stretches
        var wakeUps = 0
        val firstSleep = st.indexOfFirst { it.stage != Stage.AWAKE }
        val lastSleep = st.indexOfLast { it.stage != Stage.AWAKE }
        if (firstSleep >= 0) for (i in firstSleep..lastSleep) if (st[i].stage == Stage.AWAKE && st[i].minutes >= 2) wakeUps++
        val fellAsleep = if (firstSleep >= 0) Duration.between(n.start, st[firstSleep].start).toMinutes().toInt().coerceAtLeast(0) else null
        val eff = if (inBed > 0) (asleep * 100 / inBed).coerceIn(0, 100) else 0
        return NightStats(
            date = n.end.atZone(zone).toLocalDate(), bedtime = n.start, wake = n.end, inBedMin = inBed, asleepMin = asleep,
            deepMin = deep, remMin = rem, lightMin = light, awakeMin = awake, wakeUps = wakeUps, fellAsleepMin = fellAsleep,
            efficiency = eff, score = score(asleep, deep, rem, eff, wakeUps, hasStages), hasStages = hasStages, stages = st,
        )
    }

    /**
     * Fox Score, 0..100:
     * 60 pts how long you slept (vs 8 h; short nights cost a lot), 15 deep+REM share, 15 efficiency, 10 few wake-ups.
     * Without stage data, length and efficiency carry the whole score.
     */
    fun score(asleep: Int, deep: Int, rem: Int, eff: Int, wakeUps: Int, hasStages: Boolean): Int {
        val length = (asleep.toFloat() / GOAL_MIN).coerceIn(0f, 1f)
        val lengthPts = 60f * Math.pow(length.toDouble(), 2.2).toFloat()                                 // short nights hurt more
        val restore = if (asleep > 0) ((deep + rem).toFloat() / asleep / 0.40f).coerceIn(0f, 1f) else 0f   // ~40% deep+REM is great
        val effPts = 15f * ((eff - 70) / 25f).coerceIn(0f, 1f)                                              // 95%+ = full marks
        val wakePts = 10f * (1f - wakeUps / 6f).coerceIn(0f, 1f)
        val s = if (hasStages) lengthPts + 15f * restore + effPts + wakePts
                else (lengthPts + effPts) * (100f / 75f)
        return s.roundToInt().coerceIn(0, 100)
    }

    fun week(nights: List<NightStats>, zone: ZoneId): SleepWeek {
        val last = nights.sortedBy { it.date }.takeLast(7)
        val avg = if (last.isEmpty()) 0 else last.sumOf { it.asleepMin } / last.size
        // how much bedtime moves around: spread of minutes-after-6pm
        val bt = last.map { val t = it.bedtime.atZone(zone).toLocalTime(); ((t.hour * 60 + t.minute) - 18 * 60 + 1440) % 1440 }
        val spread = if (bt.size < 2) 0 else (bt.max() - bt.min())
        return SleepWeek(last, avg, spread, GOAL_MIN)
    }

    fun hm(min: Int): String = if (min >= 60) "${min / 60}h ${min % 60}m" else "${min}m"

    /** short, plain-English takeaways for the night (most important first) */
    fun insights(n: NightStats, w: SleepWeek, flowStartedAt: Instant? = null): List<String> {
        val out = mutableListOf<String>()
        val short = GOAL_MIN - n.asleepMin
        out += when {
            short <= 0 -> "You hit your 8 hours. Fox is proud."
            short <= 60 -> "Just ${hm(short)} short of 8 hours."
            else -> "${hm(short)} short of 8 hours. Going to bed earlier is the fix."
        }
        if (n.hasStages && n.asleepMin > 0) {
            val dp = n.deepMin * 100 / n.asleepMin; val rp = n.remMin * 100 / n.asleepMin
            out += when {
                dp < 10 -> "Deep sleep was low (${dp}%). Late caffeine, screens or a warm room can do that."
                dp >= 20 -> "Great deep sleep (${dp}%). That's the body-repair kind."
                else -> "Deep sleep ${dp}%, REM ${rp}%. Normal range."
            }
        }
        if (n.wakeUps >= 3) out += "You woke up ${n.wakeUps} times. Try the after-story sounds for 1 hr."
        n.fellAsleepMin?.let { if (it > 30) out += "Took ${it} min to fall asleep." else if (it in 1..15) out += "Out in ${it} min. Nice." }
        flowStartedAt?.let { f ->
            val first = n.fellAsleepMin?.let { n.bedtime.plusSeconds(it * 60L) }
            if (first != null && f.isBefore(first) && Duration.between(f, first).toMinutes() in 1..120)
                out += "Fell asleep ${Duration.between(f, first).toMinutes()} min after starting Goodnight Flow."
        }
        if (w.nights.size >= 3 && w.bedtimeSpreadMin > 90) out += "Your bedtime moved around ${hm(w.bedtimeSpreadMin)} this week. Same time nightly = easier 4:30s."
        return out
    }

    fun foxLine(score: Int): String = when {
        score >= 85 -> "Fox slept like a rock. So did you."
        score >= 76 -> "Solid night. Fox approves."
        score >= 60 -> "Decent. Fox wants a little more."
        score >= 40 -> "Rough one. Easy day today."
        else -> "Fox is worried. Bed early tonight."
    }
}
