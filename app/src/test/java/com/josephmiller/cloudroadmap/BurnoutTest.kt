package com.josephmiller.cloudroadmap

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BurnoutTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun finish(r: Roadmap, b: StudyBundle, day: LocalDate) =
        b.sections.forEach { s -> if (b.key(s.id) !in r.studyDone) r.toggleTask(StudyPlan.sectionTask(b, s), day) }

    @Test fun planCIsOneOneThreeAndOffDaysStayOff() {
        val r = Roadmap()
        val week = (0..6L).map { d("2026-11-29").plusDays(it) } // Sun Nov 29 .. Sat Dec 5
        val counts = week.associate { it.dayOfWeek.name.take(3) to StudyPlan.bundlesToday(it, r).size }
        assertEquals(mapOf("SUN" to 0, "MON" to 0, "TUE" to 1, "WED" to 0, "THU" to 1, "FRI" to 0, "SAT" to 3), counts)
        listOf("SUN", "MON", "WED", "FRI").forEach { k -> assertEquals(listOf("rest"), StudyPlan.tasksFor(week.first { it.dayOfWeek.name.take(3) == k }, r).map { it.stream }) }
        assertEquals(26, Bundles.all.size)
        assertEquals(listOf(1), StudyPlan.bundlesToday(d("2026-12-01"), r).map { it.n })
        assertEquals(listOf(1, 2, 3), StudyPlan.bundlesToday(d("2026-12-05"), r).map { it.n })
        // Each bundle is Sophia + a practice room/video + (usually) Packet Tracer + note.
        assertTrue(Bundles.all.all { b -> b.sections.any { it.id == "sophia" } && b.sections.last().id == "note" })
        assertEquals(17, Bundles.all.count { b -> b.sections.any { it.id == "pt" } })
        assertTrue(Bundles.all.drop(1).all { it.sections.first().id == "warm" })
    }

    @Test fun sleepOnlyLightensTheDay() {
        val r = Roadmap()
        val tue = d("2026-12-08"); val sat = d("2026-12-12")
        assertEquals(1, Burnout.bundlesFor(r, tue)); assertEquals(120, Burnout.capMin(tue, r))
        r.sleep[tue.toString()] = 400   // 6h40: short
        assertEquals(1, Burnout.bundlesFor(r, tue)); assertEquals(90, Burnout.capMin(tue, r))
        r.sleep[tue.toString()] = 300   // 5h: low -> videos + quiz only
        assertEquals(0, Burnout.bundlesFor(r, tue))
        assertTrue(Burnout.sleepNote(r, tue)!!.contains("light night"))
        r.sleep[sat.toString()] = 400; assertEquals(2, Burnout.bundlesFor(r, sat)); assertEquals(360, Burnout.capMin(sat, r))
        r.sleep[sat.toString()] = 300; assertEquals(1, Burnout.bundlesFor(r, sat))
        r.sleep[sat.toString()] = 480; assertEquals(3, Burnout.bundlesFor(r, sat))
        // Synced between devices.
        assertEquals(480, Roadmap.fromJson(r.toJson()).sleep[sat.toString()])
    }

    @Test fun threeShortNightsInThreeWeeksInARowMeansRest() {
        val r = Roadmap()
        // Weeks ending Dec 5, Dec 12, Dec 19: three nights under 5h30 each.
        listOf("2026-12-01","2026-12-02","2026-12-03","2026-12-08","2026-12-09","2026-12-10","2026-12-15","2026-12-16","2026-12-17").forEach { r.sleep[it] = 300 }
        assertTrue(Burnout.isRestWeek(r, d("2027-01-05")).not()) // week of Dec 26 skipped, but week ending Jan 2 had no low nights
        r.sleep["2026-12-29"] = 300; r.sleep["2026-12-30"] = 300; r.sleep["2026-12-31"] = 300
        assertTrue(Burnout.isRestWeek(r, d("2027-01-05")))
    }

    @Test fun missedDaysNeverStack() {
        val r = Roadmap()
        // Skip Tue + Thu entirely: Saturday still shows exactly 3, starting from bundle 1.
        assertEquals(listOf(1, 2, 3), StudyPlan.bundlesToday(d("2026-12-05"), r).map { it.n })
        // Half-finish bundle 1 on Tuesday: Thursday shows bundle 1 again (resume), not 1 + 2.
        val b1 = Bundles.byN(1)
        r.toggleTask(StudyPlan.sectionTask(b1, b1.sections[0]), d("2026-12-01"))
        assertEquals(listOf(1), StudyPlan.bundlesToday(d("2026-12-03"), r).map { it.n })
        assertTrue(r.dayDone(d("2026-12-01")))
        // A bundle finished today stays on screen today.
        finish(r, b1, d("2026-12-03"))
        assertEquals(listOf(1), StudyPlan.bundlesToday(d("2026-12-03"), r).map { it.n })
        assertEquals(listOf(2), StudyPlan.bundlesToday(d("2026-12-08"), r).map { it.n })
        assertEquals(Status.WORKING, r.item("cs1015")!!.status)
    }

    @Test fun nothingBeforeDec1ThenSetupPlusBundle1() {
        val r = Roadmap()
        (0..19L).map { d("2026-11-11").plusDays(it) }.forEach {
            assertEquals(it.toString(), listOf("rest"), StudyPlan.tasksFor(it, r).map { t -> t.stream })
        }
        val dec1 = StudyPlan.tasksFor(d("2026-12-01"), r)
        assertEquals(listOf("todo:setup_sophia", "todo:setup_thm"), dec1.filter { it.stream == "todo" }.map { it.key })
        assertTrue(dec1.any { it.key == "b:1:sophia" })
        assertTrue(StudyPlan.tasksFor(d("2026-12-03"), r).none { it.stream == "todo" })
    }

    @Test fun offWeeksAreEmpty() {
        val r = Roadmap()
        listOf("2026-11-24", "2026-11-26", "2026-11-28", "2026-12-22", "2026-12-24", "2026-12-26").forEach {
            assertEquals(it, listOf("rest"), StudyPlan.tasksFor(d(it), r).map { t -> t.stream })
        }
    }

    @Test fun twoLowRatingsDropSaturdayToTwoAndRecover() {
        val r = Roadmap()
        r.energy["2026-12-01"] = 2; r.energy["2026-12-03"] = 1
        assertEquals(Mode.B, Burnout.mode(r, d("2026-12-05")))
        assertEquals(2, StudyPlan.bundlesToday(d("2026-12-05"), r).size)
        assertEquals(1, StudyPlan.bundlesToday(d("2026-12-08"), r).size) // weekdays stay 1
        r.energy["2026-12-05"] = 4
        assertEquals(Mode.C, Burnout.mode(r, d("2026-12-08")))
        assertEquals(3, StudyPlan.bundlesToday(d("2026-12-12"), r).size)
    }

    @Test fun threeLowWeeksGiveARestWeekThenResume() {
        val r = Roadmap()
        listOf("2026-11-10", "2026-11-12", "2026-11-14", "2026-11-17", "2026-11-19", "2026-11-21", "2026-12-01", "2026-12-03", "2026-12-05").forEach { r.energy[it] = 1 }
        assertEquals(Mode.REST, Burnout.mode(r, d("2026-12-08")))
        assertEquals(listOf("rest"), StudyPlan.tasksFor(d("2026-12-12"), r).map { it.stream })
        assertTrue(StudyPlan.tasksFor(d("2026-12-12"), r)[0].task.contains("Rest week"))
        assertNotEquals(Mode.REST, Burnout.mode(r, d("2026-12-15"))) // the rest week itself has no ratings
    }

    @Test fun timerCapsAndOverride() {
        val r = Roadmap()
        val tue = d("2026-11-10")
        val t0 = 1_000_000_000_000L
        Burnout.startTimer(r, tue, t0)
        assertEquals(30, Burnout.minutesOn(r, tue, t0 + 30 * 60_000L))
        assertEquals(50, Burnout.minutesOn(r, tue, t0 + 90 * 60_000L)) // a block never counts past 50
        Burnout.stopTimer(r, t0 + 90 * 60_000L)
        assertEquals(50, r.studyMin[tue.toString()])
        r.studyMin[tue.toString()] = 120
        assertTrue(Burnout.capReached(r, tue, t0))
        Burnout.recordOverride(r, tue, 4, 5) // not perfect: no extra time, no retry
        assertTrue(Burnout.capReached(r, tue, t0)); assertTrue(Burnout.overrideUsed(r, tue))
        val sat = d("2026-11-14")
        r.studyMin[sat.toString()] = 360
        Burnout.recordOverride(r, sat, 5, 5)
        assertFalse(Burnout.capReached(r, sat, t0))
        assertEquals(390, Burnout.capMin(sat, r))
        r.energy["2026-11-17"] = 1; r.energy["2026-11-19"] = 1
        Burnout.recordOverride(r, d("2026-11-20"), 5, 5) // passed: this week stays on Plan C
        assertEquals(Mode.C, Burnout.mode(r, d("2026-11-21")))
    }

    @Test fun quizBankCoversEveryUnit() {
        (1..5).forEach { u ->
            assertTrue(Quiz.bank.count { it.unit == u } >= 8)
            assertEquals(5, Quiz.pick(u, 5, d("2026-11-14"), "override").distinct().size)
        }
        assertEquals(Quiz.pick(2, 3, d("2026-11-14"), "warm5").map { it.q }, Quiz.pick(2, 3, d("2026-11-14"), "warm5").map { it.q })
        assertTrue(Quiz.bank.all { it.answer in it.options.indices })
    }

    @Test fun afterCs1015JobNightsStudyComAndSoc() {
        val r = Roadmap()
        Bundles.all.forEach { finish(r, it, d("2026-12-31")) }
        assertEquals(Status.DONE, r.item("cs1015")!!.status)
        assertEquals("thm", StudyPlan.tasksFor(d("2027-01-02"), r)[0].stream) // SOC L1 Saturdays
        assertEquals("video", StudyPlan.tasksFor(d("2027-01-05"), r)[0].stream)
        assertEquals("job", StudyPlan.tasksFor(d("2027-01-19"), r)[0].stream) // Tuesday job night
        assertEquals("video", StudyPlan.tasksFor(d("2027-01-21"), r)[0].stream)
        assertEquals("course", StudyPlan.tasksFor(d("2027-02-04"), r)[0].stream) // Study.com Thursdays
        assertTrue(StudyPlan.tasksFor(d("2027-02-04"), r)[0].task.contains("Philosophy 104"))
        val job = StudyPlan.tasksFor(d("2027-01-19"), r)[0]
        r.toggleTask(job, d("2027-01-19"))
        assertTrue(r.dayDone(d("2027-01-19")))
        assertEquals(Status.WORKING, r.item("helpdesk_job")!!.status)
    }

    @Test fun stateSurvivesSaveAndLoad() {
        val r = Roadmap()
        r.energy["2026-11-10"] = 3; r.studyMin["2026-11-10"] = 75; r.portfolio["1"] = "Built a LAN"
        r.timerStart = 123L; r.timerDay = "2026-11-10"
        val b1 = Bundles.byN(1)
        finish(r, b1, d("2026-11-10"))
        val r2 = Roadmap.fromJson(r.toJson())
        assertEquals(3, r2.energy["2026-11-10"]); assertEquals(75, r2.studyMin["2026-11-10"])
        assertEquals("Built a LAN", r2.portfolio["1"]); assertEquals(123L, r2.timerStart)
        assertTrue(Bundles.isDone(r2, b1))
    }
}

class RenameTest {
    @org.junit.Test fun oldFeedLinkMovesToNewAccount() {
        val r = Roadmap(feedUrl = "https://raw.githubusercontent.com/jos492/cloud-roadmap-feed/main/updates.json")
        org.junit.Assert.assertEquals(Roadmap.DEFAULT_FEED, Roadmap.fromJson(r.toJson()).feedUrl)
        org.junit.Assert.assertTrue(Roadmap.DEFAULT_FEED.contains("/jmiller-sec/"))
    }
}
