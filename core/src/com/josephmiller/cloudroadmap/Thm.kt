package com.josephmiller.cloudroadmap

import java.net.URLEncoder
import java.time.LocalDate

/** One paired video for a commute day. */
class PairedVideo(val title: String, val url: String)

/**
 * One TryHackMe Saturday: a module done on the work laptop, plus the video that pairs with it
 * (first half on the Tuesday commute, second half on the Thursday commute).
 */
class ThmWeek(
    val n: Int,
    val planned: LocalDate,
    val module: String,
    val rooms: String,
    val tue: PairedVideo?,
    val thu: PairedVideo?,
    val free: Boolean = false,
    val pt: String? = null,
    /** For the networking weeks: the exact TryHackMe rooms, in order (title to URL). */
    val roomLinks: List<Pair<String, String>> = emptyList(),
) {
    fun roomKey(i: Int) = "thm:$n:$i"
    val ptKey get() = "pt:$n"
    val key get() = "thm:$n"
    val tueKey get() = "vid:$n:tue"
    val thuKey get() = "vid:$n:thu"
}

object ThmPlan {
    const val PATH_PRE = "https://tryhackme.com/path/outline/presecurity"
    const val PATH_SOC1 = "https://tryhackme.com/path/outline/soclevel1"
    const val PRICING = "https://tryhackme.com/pricing"
    const val PACKET_TRACER = "https://www.netacad.com/courses/getting-started-cisco-packet-tracer"
    private const val TCM = "https://youtu.be/56NDgBOSpUg"


    /** Weeks with no studying at all (the Saturday of that week). */
    val OFF = setOf(LocalDate.parse("2026-11-28"), LocalDate.parse("2026-12-26"))

    /** Projects stay locked until SOC Level 1 is finished. */
    val PROJECTS_UNLOCK: LocalDate = LocalDate.parse("2027-05-01")

    private fun d(s: String) = LocalDate.parse(s)
    private fun sec(t: String) = t.split(":").map { it.toInt() }.let { (h, m, s) -> h * 3600 + m * 60 + s }
    private fun tcm(from: String, to: String, what: String) = PairedVideo("TCM SOC 101 · $from → $to · $what", "$TCM?t=${sec(from)}")
    /** An exact YouTube video (checked 2026-11-14). */
    private fun v(title: String, id: String) = PairedVideo(title, "https://youtu.be/$id")

    val weeks = listOf(
        ThmWeek(1, d("2027-01-16"), "SOC L1 · Blue Team Introduction", "Junior Security Analyst Intro, SOC Role in Blue Team (free intro rooms)",
            tcm("1:30:00", "2:15:00", "security operations fundamentals"),
            tcm("2:15:00", "3:00:51", "security operations fundamentals, part 2")),
        ThmWeek(2, d("2027-01-23"), "SOC L1 · SOC Team Internals", "Alert Triage, Alert Reporting, Workbooks + Lookups, Metrics",
            v("MyDFIR · SOC alert triage explained (5 min)", "k9c1PBynVpA"),
            v("Cybersecurity with G · Every step taken when an alert comes in (9 min)", "Cr8km4SrmVU")),
        ThmWeek(3, d("2027-01-30"), "SOC L1 · Core SOC Solutions", "EDR, SIEM, Splunk basics, Elastic basics, SOAR",
            v("InfoSec Guardians · SIEM, EDR, XDR, MDR and SOAR explained (10 min)", "lhoa0_z-7Xk"),
            v("Jon Good · Splunk tutorial for beginners (12 min)", "3CiRs6WaWaU")),
        ThmWeek(4, d("2027-02-06"), "SOC L1 · Cyber Defence Frameworks", "Pyramid of Pain, Kill Chain, Unified Kill Chain, MITRE, Summit, Eviction",
            v("HackerSploit · Introduction to MITRE ATT&CK (36 min)", "LCec9K0aAkM"),
            v("StudioSec · The Pyramid of Pain explained (12 min)", "O7PSKrgdHAI")),
        ThmWeek(5, d("2027-02-13"), "SOC L1 · Phishing Analysis", "Headers, senders, links, attachments",
            tcm("3:00:51", "3:50:47", "email fundamentals + phishing types"),
            tcm("3:50:47", "4:35:18", "headers, sender + email authentication")),
        ThmWeek(6, d("2027-02-20"), "SOC L1 · Network Traffic Analysis", "Wireshark rooms + NetworkMiner",
            tcm("7:52:04", "8:50:00", "packet capture + tcpdump"),
            tcm("8:50:00", "10:09:15", "Wireshark")),
        ThmWeek(7, d("2027-02-27"), "SOC L1 · Network Security Monitoring", "Intrusion detection with Snort",
            tcm("10:09:15", "10:51:36", "Snort intro + rules"),
            tcm("10:51:36", "11:15:43", "Snort IDS/IPS practice")),
        ThmWeek(8, d("2027-03-06"), "SOC L1 · Web Security Monitoring", "Web attacks in server + proxy logs",
            v("Professor Messer · Application attacks (12 min)", "yRSqIGjeb7s"),
            v("Tech with Jono · How to correctly read logs as a SOC analyst (9 min)", "TlB-vGW-xLQ")),
        ThmWeek(9, d("2027-03-13"), "SOC L1 · Windows Security Monitoring", "Which Windows events matter",
            tcm("11:15:43", "11:51:00", "endpoint security"),
            v("Infosec Wizard · Windows logon events 4624, 4625, 4634 (15 min)", "M3fAyZGZQKc")),
        ThmWeek(10, d("2027-03-20"), "SOC L1 · Linux Security Monitoring", "Linux logs and what to watch",
            v("Cyber Baby · Reading Linux auth logs in 10 min", "mfWqquL_Mik"),
            v("HackerSploit · Linux monitoring and logging (25 min)", "kZ5LhS6fThM")),
        ThmWeek(11, d("2027-03-27"), "SOC L1 · Malware Concepts for SOC", "Malware behavior for triage",
            v("Professor Messer · An overview of malware (6 min)", "-eZs8wjjGGE"),
            v("MyDFIR · SOC analyst: malware detected (4 min)", "jOp5V45Wnoc")),
        ThmWeek(12, d("2027-04-03"), "SOC L1 · Threat Analysis Tools", "Threat intel lookups to enrich alerts",
            v("Professor Messer · Threat intelligence (5 min)", "86fruE9jkKk"),
            v("MyDFIR · VirusTotal for beginner analysts (5 min)", "1oXpWJC_VUc")),
        ThmWeek(13, d("2027-04-10"), "SOC L1 · SIEM Triage for SOC", "End-to-end triage in Splunk + Elastic",
            v("MyDFIR · How to learn Splunk for SOC analysts (8 min)", "awz9hugFdYg"),
            v("Steve Shangguan · Getting started with Elastic SIEM (14 min)", "EvMOQoTKvSw")),
        ThmWeek(14, d("2027-04-17"), "SOC L1 · Capstone: Tempest", "Full investigation. No video first: watch a walkthrough only if stuck.", null, null),
        ThmWeek(15, d("2027-04-24"), "SOC L1 · Capstone: Boogeyman", "Full investigation. Finishing this finishes SOC Level 1.", null, null),
    )

    fun next(r: Roadmap): ThmWeek? = weeks.firstOrNull { it.key !in r.studyDone }
    fun done(r: Roadmap) = weeks.count { it.key in r.studyDone }

    /** Saturday that ends the Tue–Sat study week containing [date]. */
    fun saturdayOf(date: LocalDate): LocalDate = date.plusDays(((6 - date.dayOfWeek.value + 7) % 7).toLong())

    fun isOffWeek(date: LocalDate) = saturdayOf(date) in OFF
}
