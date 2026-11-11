package com.josephmiller.cloudroadmap

import org.junit.Assert.*
import org.junit.Test

class SyncTest {
    private fun copy(r: Roadmap) = Roadmap.fromJson(r.toJson())

    @Test fun checksFromBothDevicesSurvive() {
        val base = Roadmap(); base.updatedAt = 1
        val phone = copy(base); val pc = copy(base)
        phone.studyDone.add("b:1:sophia"); phone.updatedAt = 10
        pc.studyDone.add("b:1:pt"); pc.portfolio["1"] = "Built a LAN"; pc.updatedAt = 20
        val m = Merge.merge(phone, pc)
        assertTrue("b:1:sophia" in m.studyDone); assertTrue("b:1:pt" in m.studyDone)
        assertEquals("Built a LAN", m.portfolio["1"])
        assertEquals(20, m.updatedAt)
        assertEquals(m.toJson(), Merge.merge(m, copy(m)).toJson()) // merging again changes nothing
    }

    @Test fun uncheckOnOneDeviceWinsIfLater() {
        val a = Roadmap(); a.studyDone.add("b:2:thm")
        Thread.sleep(5)
        val b = copy(a); b.studyDone.remove("b:2:thm"); b.updatedAt = 5; a.updatedAt = 9 // a saved later, but b's uncheck is newer
        assertFalse("b:2:thm" in Merge.merge(a, b).studyDone)
    }

    @Test fun minutesAndEnergyMerge() {
        val a = Roadmap(); a.studyMin["2026-12-05"] = 70; a.energy["2026-12-05"] = 4; a.updatedAt = 1
        val b = Roadmap(); b.studyMin["2026-12-05"] = 40; b.energy["2026-12-03"] = 2; b.updatedAt = 2
        val m = Merge.merge(a, b)
        assertEquals(70, m.studyMin["2026-12-05"]); assertEquals(4, m.energy["2026-12-05"]); assertEquals(2, m.energy["2026-12-03"])
    }

    @Test fun logSurvivesSaveLoadAndLoadingDoesNotLog() {
        val r = Roadmap(); r.studyDone.add("x")
        val r2 = copy(r)
        assertTrue(r2.doneLog["x"]!! > 0)
        assertEquals(r.doneLog["x"], r2.doneLog["x"])
        val r3 = copy(r2); assertEquals(r2.doneLog, r3.doneLog) // a settled save reloads identically
    }
}
