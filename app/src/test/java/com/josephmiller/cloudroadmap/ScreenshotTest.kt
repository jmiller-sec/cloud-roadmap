package com.josephmiller.cloudroadmap

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h1500dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun sample(): Roadmap {
        val t = LocalDate.now()
        val r = Roadmap()
        r.applyOneTimeFixes(t)
        r.applyBadges(listOf("AWS Cloud Quest: Cloud Practitioner - Training Badge" to LocalDate.parse("2024-08-14")), t)
        r.markWorking("cs1015", t)
        r.markWorking("thm_soc1", t)
        r.markWorking("net_videos", t)
        r.studyShift = 0
        StudyPlan.tasksFor(t, r).firstOrNull()?.let { r.toggleTask(it, t) }
        r.studyDone.add("${t.minusDays(1)}|soc")
        r.applyFeed("""{"weekId":"2026-11-08","summary":"Free AZ-900 voucher event in December",
          "findings":[{"type":"info","title":"Free AZ-900 voucher event Dec 9","detail":"Attend Microsoft's free Azure Fundamentals training day to get a free exam voucher.","link":"https://learn.microsoft.com"},
          {"type":"info","category":"jobs","title":"Southwest: IT Support Technician I (Dallas)","detail":"Entry-level desktop support at HQ by Love Field. Posted this week.","link":"https://careers.southwestair.com"},
          {"type":"info","category":"wgu","title":"WGU tuition unchanged for 2027","detail":"Flat-rate tuition per 6-month term stays the same.","link":"https://wgu.edu"},
          {"type":"info","category":"videos","title":"Professor Messer: November A+ Core 1 study group","detail":"Live Q&A on Core 1 topics.","link":"https://www.professormesser.com"},
          {"type":"info","category":"videos","title":"NetworkChuck: new subnetting video","detail":"Short subnetting walkthrough.","link":"https://www.youtube.com/@NetworkChuck"},
          {"type":"info","category":"deals","title":"TCM Black Friday sale starts Nov 27","detail":"TCM Academy all-access is discounted. SOC 101 is included.","link":"https://tcm-sec.com"},
          {"type":"change","title":"SC-200 exam updated","detail":"Microsoft renamed the exam. Rename it in your plan?","link":"https://learn.microsoft.com","action":{"op":"rename","itemId":"sc200","title":"SC-200 (updated)"}}]}""", t)
        return r
    }

    private fun shot(tab: Int, name: String) {
        val r = sample()
        rule.setContent { RoadmapTheme { AppContent(r, tab, {}, {}) } }
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun wtd() = shot(0, "0-wtd")
    @Test fun roadmap() = shot(1, "1-roadmap")
    @Test fun projects() = shot(2, "2-projects")
    @Test fun videos() = shot(3, "3-videos")
    @Test fun jobs() = shot(4, "4-jobs")
    @Test fun wgu() = shot(5, "5-wgu")
    @Test fun updates() = shot(99, "6-updates")
}
