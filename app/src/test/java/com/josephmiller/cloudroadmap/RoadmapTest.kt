package com.josephmiller.cloudroadmap

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class RoadmapTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun smallLateFinishAutoShiftsLaterItemsOnly() {
        val r = Roadmap()
        r.markWorking("sdc_ethics", d("2027-02-04"))
        r.markDone("sdc_ethics", d("2027-03-29"), d("2027-03-29")) // 4 days late
        assertEquals(d("2027-05-31"), r.item("sdc_infosec")!!.end)
        assertEquals(d("2027-07-26"), r.item("sdc_forensics")!!.end)
        assertEquals(d("2027-04-24"), r.item("thm_soc1")!!.end) // parallel item untouched
        assertTrue(r.proposals.isEmpty())
    }

    @Test fun bigLateFinishAsksFirstThenShiftsOnApproval() {
        val r = Roadmap()
        r.markDone("sdc_ethics", d("2027-04-09"), d("2027-04-09")) // 15 days late
        assertEquals(1, r.proposals.size)
        assertEquals(d("2027-05-27"), r.item("sdc_infosec")!!.end)
        r.approve(r.proposals[0].id, d("2027-04-09"))
        assertEquals(d("2027-06-11"), r.item("sdc_infosec")!!.end)
        assertTrue(r.proposals.isEmpty())
    }

    @Test fun earlyFinishPullsDatesIn() {
        val r = Roadmap()
        r.markDone("sdc_ethics", d("2027-03-20"), d("2027-03-20")) // 5 days early
        assertEquals(d("2027-03-27"), r.item("sdc_infosec")!!.start)
        r.markDone("cs1015", d("2026-11-30"), d("2026-11-30")) // CS1015 early: nothing moves
        assertEquals(d("2027-03-27"), r.item("sdc_infosec")!!.start)
    }

    @Test fun lockedDatesNeverMove() {
        val r = Roadmap()
        r.markDone("k401_check", d("2027-01-20"), d("2027-01-20"))
        assertEquals(d("2027-01-31"), r.item("car")!!.end)
    }

    @Test fun lateCheckExtendsOnceThenAsks() {
        val r = Roadmap()
        r.markWorking("lab_ad", d("2027-05-01"))
        r.lateCheck(d("2027-06-01")) // 3 days over
        val ad = r.item("lab_ad")!!
        assertEquals(d("2027-06-05"), ad.end)
        assertEquals(d("2027-06-05"), r.item("lab_net")!!.start)
        r.lateCheck(d("2027-06-11")) // over again
        assertEquals(1, r.proposals.count { it.kind == "late" })
        r.lateCheck(d("2027-06-18"))
        assertEquals(1, r.proposals.count { it.kind == "late" }) // no duplicates
    }

    @Test fun credlyBadgesMarkDone() {
        val json = File(System.getenv("BADGES") ?: "/mnt/user-data/uploads/85eec095-attachment.txt").readText()
        val arr = org.json.JSONObject(json).getJSONArray("data")
        val badges = (0 until arr.length()).map {
            val b = arr.getJSONObject(it)
            b.getJSONObject("badge_template").getString("name") to LocalDate.parse(b.getString("issued_at_date"))
        }
        val r = Roadmap()
        val done = r.applyBadges(badges, d("2026-11-08"))
        assertEquals(listOf("AWS Cloud Quest (bonus)"), done)
        assertEquals(Status.TODO, r.item("cs1015")!!.status)
        // pretend CS1015 badge arrives
        r.applyBadges(badges + ("Introduction to Networking (CS1015)" to d("2026-12-03")), d("2026-12-06"))
        assertEquals(Status.DONE, r.item("cs1015")!!.status)
        assertTrue(r.item("cs1015")!!.doneByBadge)
        assertEquals(d("2027-02-04"), r.item("sdc_ethics")!!.start) // CS1015 never moves Study.com
    }

    @Test fun feedAppliesOncePerWeekAndActionsWork() {
        val r = Roadmap()
        val feed = """{"weekId":"2026-11-08","summary":"2 updates","findings":[
          {"type":"info","title":"AZ-900 price unchanged","detail":"","link":""},
          {"type":"change","title":"SC-200 renamed","detail":"x","link":"","action":{"op":"rename","itemId":"sc200","title":"SC-200 (new name)"}}]}"""
        assertEquals("2 updates", r.applyFeed(feed, d("2026-11-08")))
        assertNull(r.applyFeed(feed, d("2026-11-08")))
        assertEquals(2, r.findings.size)
        r.approveFinding(r.findings[1].id, d("2026-11-08"))
        assertEquals("SC-200 (new name)", r.item("sc200")!!.title)
    }

    @Test fun jsonRoundTrip() {
        val r = Roadmap()
        r.markWorking("az900", d("2027-09-01"))
        r.markDone("az900", d("2027-11-20"), d("2027-11-20")) // 20 days late: asks
        val r2 = Roadmap.fromJson(r.toJson())
        assertEquals(r.items.size, r2.items.size)
        assertEquals(Status.DONE, r2.item("az900")!!.status)
        assertEquals(1, r2.proposals.size)
    }

    @Test fun studyComBadgeMarksDoneAndOldSavesGetKeys() {
        val r = Roadmap()
        r.item("sdc_ethics")!!.badgeKey = null // simulate a v1.0 save
        val r2 = Roadmap.fromJson(r.toJson())
        assertEquals("Ethics in Technology", r2.item("sdc_ethics")!!.badgeKey)
        r2.applyBadges(listOf("Computer Science 202: Ethics in Technology" to d("2026-12-20")), d("2026-12-21"))
        assertEquals(Status.DONE, r2.item("sdc_ethics")!!.status)
        assertEquals(Status.TODO, r2.item("sdc_infosec")!!.status)
    }

    @Test fun feedDoneActionAppliesWithoutApproval() {
        val r = Roadmap()
        r.applyFeed("""{"weekId":"2026-12-27","summary":"x","findings":[
          {"type":"info","category":"wgu","title":"Study.com: Ethics in Technology finished","detail":"","link":"",
           "action":{"op":"done","itemId":"sdc_ethics","date":"2026-12-24"}}]}""", d("2026-12-27"))
        assertEquals(Status.DONE, r.item("sdc_ethics")!!.status)
        assertEquals(d("2026-12-24"), r.item("sdc_ethics")!!.doneOn)
        assertEquals("seen", r.findings[0].state)
    }

    @Test fun finishingAllLabStepsMarksLabDone() {
        val r = Roadmap()
        val today = d("2027-05-01")
        repeat(StudyPlan.labSteps["lab_ad"]!!.size) {
            val step = StudyPlan.nextLabStep(r)!!
            assertEquals("lab_ad", step.itemId)
            r.toggleTask(step, today)
        }
        assertEquals(Status.DONE, r.item("lab_ad")!!.status)
        assertEquals("lab_net", StudyPlan.nextLabStep(r)!!.itemId)
        assertTrue(r.dayDone(today))
        assertEquals(1, r.streak(today))
    }

    @Test fun ldCoursesAreManualAndMigrateIntoOldSaves() {
        val old = Roadmap()
        old.items.removeAll { it.id.startsWith("ld_") }
        val r = Roadmap.fromJson(old.toJson())
        Roadmap.LD_FREE.forEach { assertNotNull(r.item(it)); assertNull(r.item(it)!!.badgeKey) }
        assertNull(r.item("ld_alerts"))
        assertNull(r.item("tcm_ld"))
        assertEquals(Status.DONE, r.item("ld_socfund")!!.status) // one-time fix: already finished
        assertTrue(r.proposals.isEmpty())
        r.toggleLdCourse("ld_socfund", d("2026-11-15"))
        assertNotEquals(Status.DONE, r.item("ld_socfund")!!.status)
        assertNotEquals(Status.DONE, Roadmap.fromJson(r.toJson()).item("ld_socfund")!!.status) // undo sticks
        val r2 = Roadmap.fromJson(r.toJson())
        assertEquals(r.items.size, r2.items.size) // no duplicates on reload
    }

    @Test fun checkingOffAlreadyFinishedLdCoursesDoesNotMoveThePlan() {
        val r = Roadmap()
        val before = r.items.associate { it.id to it.end }
        Roadmap.LD_FREE.forEach { r.toggleLdCourse(it, d("2026-11-07")) }
        assertTrue(Roadmap.LD_FREE.all { r.item(it)!!.status == Status.DONE })
        assertTrue(r.proposals.isEmpty())
        r.items.filter { it.id !in Roadmap.LD_FREE }.forEach { assertEquals(it.id, before[it.id], it.end) }
    }

    @Test fun projectsRunBeginnerToExpertAndSundayFollowsThem() {
        assertEquals(listOf("Beginner", "Medium", "Expert"), Projects.all.map { it.level }.distinct())
        val r = Roadmap()
        assertEquals("lab_ad", Projects.current(r)!!.id)
        listOf("lab_ad", "lab_net", "lab_ticket", "lab_ps", "gh_portfolio", "lab_fw").forEach { id ->
            Projects.byId[id]!!.steps.indices.forEach { n -> r.toggleTask(DayTask("lab:$id:$n", "lab", "", "", id), d("2027-05-01")) }
        }
        assertEquals(Status.DONE, r.item("lab_ps")!!.status)
        assertEquals("p_phish", Projects.current(r)!!.id) // first Medium project is next
        assertEquals("p_phish", StudyPlan.nextLabStep(r)!!.itemId)
    }

    @Test fun firewallGearListTracksWhatsLeft() {
        val r = Roadmap(); val fw = Projects.byId["lab_fw"]!!
        val total = fw.parts.sumOf { it.price }
        assertEquals(total, Projects.partsLeftCost(r, fw))
        Projects.togglePart(r, fw, 1)
        assertEquals(total - fw.parts[1].price, Projects.partsLeftCost(r, fw))
        Projects.togglePart(r, fw, 1)
        assertEquals(total, Projects.partsLeftCost(r, fw))
        assertEquals(Roadmap.fromJson(r.toJson()).studyDone.contains("part:lab_fw:1"), false)
    }

    @Test fun wguFeedFlagsAddedAndRemovedCourses() {
        val r = Roadmap()
        val list = WguPlan.baseline().filter { it.name != "Version Control" }.map { """{"name":"${it.name}","cu":${it.cu},"term":${it.term}}""" } +
            """{"name":"Cloud Threat Hunting","cu":3,"term":9}"""
        r.applyFeed("""{"weekId":"2026-11-15","summary":"x","wguCatalog":"202612","wguCourses":[${list.joinToString(",")}],"findings":[]}""", d("2026-11-15"))
        assertEquals("202612", r.wguCatalog)
        assertEquals("new", r.wguCourses.first { it.name == "Cloud Threat Hunting" }.flag)
        assertEquals("removed", r.wguCourses.first { it.name == "Version Control" }.flag)
        assertEquals(37, r.wguCourses.count { it.flag != "removed" })
        val r2 = Roadmap.fromJson(r.toJson())
        assertEquals("removed", r2.wguCourses.first { it.name == "Version Control" }.flag)
    }

    @Test fun progressStats() {
        val r = Roadmap()
        listOf("2026-11-08", "2026-11-10", "2026-11-11").forEach { r.studyDone.add("$it|soc") } // Mon 9th is off
        assertEquals(3, Progress.bestStreak(r))
        assertEquals(3, Progress.studyDays(r).size)
        assertEquals(2, Progress.weekly(r, d("2026-11-11")).last().second)
    }

    @Test fun projectsAreLockedOneAtATimeAndStepsInOrder() {
        val r = Roadmap()
        r.toggleTask(DayTask("lab:lab_ad:0", "lab", "", "", "lab_ad"), d("2026-11-15")) // before Mar 20: everything locked
        assertFalse("lab:lab_ad:0" in r.studyDone)
        assertTrue(r.projectLock(DayTask("lab:lab_ad:0", "lab", "", "", "lab_ad"), d("2027-04-30"))!!.contains("May 1"))
        val day = d("2027-05-01")
        r.toggleTask(DayTask("lab:p_siem:0", "lab", "", "", "p_siem"), day) // later project: locked
        assertFalse("lab:p_siem:0" in r.studyDone)
        assertNotNull(r.projectLock(DayTask("lab:p_siem:0", "lab", "", "", "p_siem"), day))
        r.toggleTask(DayTask("lab:lab_ad:2", "lab", "", "", "lab_ad"), day) // skipping ahead in current project
        assertFalse("lab:lab_ad:2" in r.studyDone)
        r.toggleTask(DayTask("lab:lab_ad:0", "lab", "", "", "lab_ad"), day)
        r.toggleTask(DayTask("lab:lab_ad:1", "lab", "", "", "lab_ad"), day)
        assertTrue("lab:lab_ad:1" in r.studyDone)
        r.toggleTask(DayTask("lab:lab_ad:0", "lab", "", "", "lab_ad"), day) // can't uncheck an earlier step
        assertTrue("lab:lab_ad:0" in r.studyDone)
        r.toggleTask(DayTask("lab:lab_ad:1", "lab", "", "", "lab_ad"), day) // can undo the last one
        assertFalse("lab:lab_ad:1" in r.studyDone)
    }


    @Test fun migrationToThmPausesCoursesAndDropsOldItems() {
        val old = Roadmap()
        old.items.removeAll { it.id in listOf("thm_free", "thm_soc1") }
        old.items.add(Item("tcm_ld", "soctrain", "TCM SOC 101", d("2026-11-08"), d("2027-01-07"), d("2027-01-07")))
        old.items.add(Item("tcm_helpdesk", "soctrain", "TCM Help Desk", d("2027-01-12"), d("2027-02-05"), d("2027-02-05")))
        old.items.add(Item("ld_alerts", "soctrain", "Alerts", d("2026-11-10"), d("2027-02-28"), d("2027-02-28")))
        old.item("lab_ad")!!.start = d("2026-11-09"); old.item("lab_ad")!!.end = d("2026-12-07")
        old.markWorking("cs1015", d("2026-11-07"))
        old.studyDone.add("tcm:1")
        val r = Roadmap.fromJson(old.toJson())
        assertNull(r.item("tcm_ld")); assertNull(r.item("tcm_helpdesk")); assertNull(r.item("ld_alerts"))
        assertNotNull(r.item("thm_soc1"))
        assertEquals(d("2027-05-01"), r.item("lab_ad")!!.start)
        assertFalse(r.paused("cs1015")) // Klaira confirmed: un-paused right after
        assertEquals(Status.WORKING, r.item("cs1015")!!.status)
        assertEquals(Status.DONE, r.item("wgu_confirm")!!.status)
        assertEquals(d("2027-02-04"), r.item("sdc_ethics")!!.start)
        assertEquals(d("2027-04-24"), r.item("thm_soc1")!!.end)
        assertNull(r.item("thm_free"))
        assertFalse("tcm:1" in r.studyDone)
        val r2 = Roadmap.fromJson(r.toJson())
        assertEquals(r.items.size, r2.items.size)
    }
}

