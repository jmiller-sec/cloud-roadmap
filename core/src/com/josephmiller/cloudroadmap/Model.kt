package com.josephmiller.cloudroadmap

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

enum class Status { TODO, WORKING, DONE }

class Stage(val id: String, val title: String)

class Item(
    val id: String,
    var stage: String,
    var title: String,
    var start: LocalDate,
    var end: LocalDate,
    var originalEnd: LocalDate,
    var status: Status = Status.TODO,
    var startedOn: LocalDate? = null,
    var doneOn: LocalDate? = null,
    var locked: Boolean = false,
    var badgeKey: String? = null,
    var autoExtended: Int = 0,
    var doneByBadge: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("stage", stage); put("title", title)
        put("start", start.toString()); put("end", end.toString())
        put("originalEnd", originalEnd.toString()); put("status", status.name)
        put("startedOn", startedOn?.toString() ?: ""); put("doneOn", doneOn?.toString() ?: "")
        put("locked", locked); put("badgeKey", badgeKey ?: "")
        put("autoExtended", autoExtended); put("doneByBadge", doneByBadge)
    }

    companion object {
        fun fromJson(o: JSONObject) = Item(
            id = o.getString("id"),
            stage = o.getString("stage"),
            title = o.getString("title"),
            start = LocalDate.parse(o.getString("start")),
            end = LocalDate.parse(o.getString("end")),
            originalEnd = LocalDate.parse(o.getString("originalEnd")),
            status = Status.valueOf(o.getString("status")),
            startedOn = o.optString("startedOn").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            doneOn = o.optString("doneOn").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
            locked = o.optBoolean("locked"),
            badgeKey = o.optString("badgeKey").takeIf { it.isNotBlank() },
            autoExtended = o.optInt("autoExtended"),
            doneByBadge = o.optBoolean("doneByBadge"),
        )
    }
}

/** A change waiting for Joseph's OK. kind = shift | late | feed */
class Proposal(
    val id: String,
    val kind: String,
    val text: String,
    val itemId: String,
    val days: Int,
    val anchorEnd: LocalDate,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("kind", kind); put("text", text); put("itemId", itemId)
        put("days", days); put("anchorEnd", anchorEnd.toString())
    }

    companion object {
        fun fromJson(o: JSONObject) = Proposal(
            o.getString("id"), o.getString("kind"), o.getString("text"), o.getString("itemId"),
            o.getInt("days"), LocalDate.parse(o.getString("anchorEnd")),
        )
    }
}

/** One thing the Sunday check found. type = info | change. state = new | approved | dismissed | seen */
class Finding(
    val id: String,
    val week: String,
    val type: String,
    val title: String,
    val detail: String,
    val link: String,
    val action: JSONObject?,
    var state: String,
    val category: String = "",
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("week", week); put("type", type); put("title", title)
        put("detail", detail); put("link", link); put("state", state); put("category", category)
        if (action != null) put("action", action)
    }

    companion object {
        fun fromJson(o: JSONObject) = Finding(
            o.getString("id"), o.optString("week"), o.optString("type", "info"),
            o.optString("title"), o.optString("detail"), o.optString("link"),
            o.optJSONObject("action"), o.optString("state", "new"), o.optString("category"),
        )
    }
}

class Note(val date: LocalDate, val text: String)

class Badge(val name: String, val issuer: String, val date: LocalDate) {
    fun toJson(): JSONObject = JSONObject().put("name", name).put("issuer", issuer).put("date", date.toString())

    companion object {
        fun fromJson(o: JSONObject) = Badge(o.getString("name"), o.optString("issuer"), LocalDate.parse(o.getString("date")))
    }
}

object Plan {
    val stages = listOf(
        Stage("transfer", "1. Finish transfer credits"),
        Stage("soctrain", "2. SOC training (TryHackMe)"),
        Stage("projects", "3. Home lab projects + learning"),
        Stage("money", "4. Car + money checkpoint"),
        Stage("helpdesk", "5. Help desk job"),
        Stage("wgu", "6. WGU"),
        Stage("cloudbasics", "7. Cloud basics (during WGU)"),
        Stage("soc", "8. SOC analyst job"),
        Stage("cloudsec", "9. Cloud security"),
    )

    private fun d(s: String) = LocalDate.parse(s)

    private fun i(
        id: String, stage: String, title: String, start: String, end: String,
        locked: Boolean = false, badge: String? = null,
    ) = Item(id, stage, title, d(start), d(end), d(end), locked = locked, badgeKey = badge)

    fun defaults(): MutableList<Item> = mutableListOf(
        // 1. Transfer credits. Klaira (WGU) confirmed by phone Nov 10: all four transfer. Tue/Thu commutes.
        i("wgu_confirm", "transfer", "Klaira (WGU) confirmed transfers", "2026-11-09", "2026-11-10"),
        i("cs1015", "transfer", "CS1015 Intro to Networking: 26 bundles (Sophia + TryHackMe + Packet Tracer)", "2026-11-07", "2027-01-12", badge = "CS1015"),
        i("sdc_ethics", "transfer", "Study.com: Philosophy 104 (Ethics in Technology)", "2027-02-04", "2027-03-25", badge = "Ethics in Technology"),
        i("sdc_infosec", "transfer", "Study.com: CS 202 or CS 110 (Fundamentals of Information Security)", "2027-04-01", "2027-05-27", badge = "Information Security"),
        i("sdc_forensics", "transfer", "Study.com: CS 336 (Digital Forensics)", "2027-06-03", "2027-07-22", badge = "Digital Forensics"),

        // 2. SOC training: TryHackMe on Saturdays, paired videos on Tue/Thu commutes.
        i("thm_soc1", "soctrain", "TryHackMe SOC Level 1 (Premium, monthly)", "2027-01-16", "2027-04-24"),
        i("ld_socfund", "soctrain", "LetsDefend: SOC Fundamentals", "2026-11-10", "2026-11-16"),
        i("ld_phish", "soctrain", "LetsDefend: Phishing Email Analysis", "2026-11-24", "2026-12-07"),
        i("ld_linux", "soctrain", "LetsDefend: Linux for Blue Team", "2026-12-22", "2026-12-28"),
        i("ld_web", "soctrain", "LetsDefend: Detecting Web Attacks", "2026-12-29", "2027-01-11"),
        i("ld_malab", "soctrain", "LetsDefend: Malware Analysis Lab", "2027-01-19", "2027-01-25"),

        // 3. Projects + learning. Projects unlock Sat May 1, 2027 (after SOC Level 1).
        i("net_videos", "projects", "Networking videos", "2026-11-07", "2027-01-10"),
        i("messer_core1", "projects", "Messer A+ Core 1 videos", "2027-08-01", "2027-08-31"),
        i("messer_core2", "projects", "Messer A+ Core 2 videos", "2027-09-01", "2027-09-30"),
        i("lab_ad", "projects", "Lab: Active Directory", "2027-05-01", "2027-05-29"),
        i("lab_net", "projects", "Lab: DHCP, DNS + Wireshark", "2027-05-29", "2027-06-19"),
        i("lab_ticket", "projects", "Lab: osTicket help desk", "2027-06-19", "2027-07-03"),
        i("lab_ps", "projects", "Lab: PowerShell user script", "2027-07-03", "2027-07-17"),
        i("resume", "projects", "Resume (badges + TryHackMe on top)", "2027-01-08", "2027-01-15"),

        // 3. Money checkpoint
        i("k401_check", "money", "Check 401k loan + vesting", "2026-11-09", "2027-01-15"),
        i("car", "money", "Buy the car", "2027-01-31", "2027-01-31", locked = true),

        // 4. Help desk
        i("apply", "helpdesk", "Start applying", "2027-01-15", "2027-01-15", locked = true),
        i("helpdesk_job", "helpdesk", "Land a help desk job", "2027-01-15", "2027-04-30"),

        // 5. WGU
        i("wgu_start", "wgu", "Start WGU", "2027-08-01", "2027-08-01", locked = true),
        i("wgu_itf", "wgu", "IT Foundations + IT Applications (A+)", "2027-08-01", "2027-11-30", badge = "CompTIA A+"),
        i("wgu_linux", "wgu", "Linux Foundations", "2027-12-01", "2028-01-31"),
        i("wgu_python", "wgu", "Python courses", "2028-02-01", "2028-04-30"),
        i("wgu_cloudsec", "wgu", "Managing Cloud Security", "2028-05-01", "2028-06-30"),

        // 6. Cloud basics
        i("az900", "cloudbasics", "AZ-900 Azure Fundamentals", "2027-09-01", "2027-10-31", badge = "Azure Fundamentals"),
        i("sc200", "cloudbasics", "SC-200 Security Operations Analyst", "2027-11-01", "2028-02-29", badge = "Security Operations Analyst"),

        // 7. SOC
        i("soc_apply", "soc", "Apply for SOC jobs", "2028-03-01", "2028-06-30"),
        i("soc_job", "soc", "Land a SOC analyst job", "2028-03-01", "2028-09-30"),

        // 8. Cloud security
        i("aws_cq", "cloudsec", "AWS Cloud Quest (bonus)", "2024-08-01", "2024-08-14", badge = "AWS Cloud Quest"),
        i("az500", "cloudsec", "AZ-500 Azure Security Engineer", "2029-01-01", "2029-06-30", badge = "Azure Security Engineer"),
        i("terraform", "cloudsec", "Terraform", "2029-07-01", "2029-12-31"),
        i("portfolio", "cloudsec", "Cloud security portfolio", "2029-01-01", "2030-06-30"),
        i("cloudsec_job", "cloudsec", "Cloud security role", "2030-01-01", "2031-03-31"),
    )

    fun startingBadges(): MutableList<Badge> = mutableListOf(
        Badge("Human Biology Lab (SCIE1021)", "Sophia Learning", d("2026-11-05")),
        Badge("U.S. Government (HIST1020)", "Sophia Learning", d("2026-10-29")),
        Badge("Workplace Communication (COMM1010)", "Sophia Learning", d("2026-10-28")),
        Badge("Critical Thinking (PHIL1005)", "Sophia Learning", d("2026-10-25")),
        Badge("English Composition I (ENG1001)", "Sophia Learning", d("2026-10-23")),
        Badge("Environmental Science (SCIE1005)", "Sophia Learning", d("2026-10-14")),
        Badge("Introduction to Statistics (STAT1001)", "Sophia Learning", d("2026-09-14")),
        Badge("College Algebra (MATH1001)", "Sophia Learning", d("2026-09-13")),
        Badge("Macroeconomics (ECON1001)", "Sophia Learning", d("2026-09-12")),
        Badge("Visual Communications (COMM1005)", "Sophia Learning", d("2026-09-12")),
        Badge("Introduction to Nutrition (HLTH1010)", "Sophia Learning", d("2026-09-11")),
        Badge("Introduction to Relational Databases (CS1011)", "Sophia Learning", d("2026-09-06")),
        Badge("Project Management (BUSI1023)", "Sophia Learning", d("2026-09-02")),
        Badge("Introduction to Information Technology (CS1001)", "Sophia Learning", d("2026-08-26")),
        Badge("AWS Cloud Quest: Cloud Practitioner - Training Badge", "Amazon Web Services Training and Certification", d("2024-08-14")),
    )

    fun itemsToJson(items: List<Item>) = JSONArray().apply { items.forEach { put(it.toJson()) } }
}
