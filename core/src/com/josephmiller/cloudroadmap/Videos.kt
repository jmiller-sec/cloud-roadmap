package com.josephmiller.cloudroadmap

/** A video creator Joseph learns from, with what to watch and where. */
/** One exact YouTube video for a CS1015 lesson. [url] opens that video; [searchUrl] is the backup if it ever gets taken down. */
class LessonVideo(val channel: String, val title: String, val id: String, val mins: Int) {
    val url get() = "https://youtu.be/$id"
    val searchUrl get() = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode("$channel $title", "UTF-8")
}

/** One "learn it" video (Practical Networking, or Jeremy's IT Lab / Professor Messer where PN has no free video) + NetworkChuck (make it stick). */
class LessonPair(val bundle: Int, val pn: LessonVideo, val chuck: LessonVideo) {
    fun key(which: String) = "vid:$bundle:$which"
}

class Creator(
    val id: String,
    val name: String,
    val focus: String,
    val itemIds: List<String>, // roadmap items this creator covers
    val links: List<Pair<String, String>>, // label to URL
    val keywords: List<String>, // to match Sunday-check video findings
)

object Videos {
    val creators = listOf(
        Creator(
            "tcm", "TCM Security", "Free SOC 101 (YouTube, ~12 hrs): parts are paired with TryHackMe weeks",
            listOf("thm_soc1"),
            listOf(
                "SOC 101 free" to "https://youtu.be/56NDgBOSpUg",
                "Lab files" to "https://github.com/MalwareCube/SOC101_Free",
            ),
            listOf("tcm"),
        ),
        Creator(
            "messer", "Professor Messer", "Free CompTIA A+ Core 1 (220-1201) + Core 2 (220-1202)",
            listOf("messer_core1", "messer_core2"),
            listOf("A+ training (free)" to "https://www.professormesser.com/", "YouTube channel" to "https://www.youtube.com/@professormesser"),
            listOf("messer", "professor messer"),
        ),
        Creator(
            "hackersploit", "HackerSploit", "MITRE ATT&CK + Linux logging (paired with TryHackMe)",
            emptyList(),
            listOf("YouTube channel" to "https://www.youtube.com/@HackerSploit"),
            listOf("hackersploit"),
        ),
    )

    /** Each lesson: one "learn it" video before the Sophia piece, one "make it stick" video after. Exact videos, checked 2026-11-14. */
    val lessons = listOf(
        LessonPair(1, LessonVideo("Practical Networking", "OSI Model: A Practical Perspective", "LkolbURrtTs", 13), LessonVideo("NetworkChuck", "What is TCP/IP and OSI? (CCNA EP 3)", "CRdL1PcherM", 12)),
        LessonPair(2, LessonVideo("Practical Networking", "What is Subnetting? (Subnetting Mastery 1 of 7)", "BWZ-MHIhqjM", 9), LessonVideo("NetworkChuck", "What is an IP address? (Subnetting EP 1)", "5WfiTHiU4x8", 19)),
        LessonPair(3, LessonVideo("Practical Networking", "Hosts, IP addresses, networks", "bj-Yfakjllc", 12), LessonVideo("NetworkChuck", "What is DNS?", "NiQTs9DbtW4", 24)),
        LessonPair(4, LessonVideo("Practical Networking", "Hub, bridge, switch, router", "H7-NR3Q3BeI", 15), LessonVideo("NetworkChuck", "We ran OUT of IP addresses", "tcae4TSSMo8", 17)),
        LessonPair(5, LessonVideo("Practical Networking", "Packet Traveling: how packets move", "rYodcvhh7b8", 15), LessonVideo("NetworkChuck", "What is a router? (CCNA EP 2)", "p9ScLm9S3B4", 22)),
        LessonPair(6, LessonVideo("Jeremy's IT Lab", "VLANs part 1 (Day 16)", "cjFzOnm6u1g", 24), LessonVideo("PowerCert", "VLAN explained", "jC6MJTh9fRE", 5)),
        LessonPair(7, LessonVideo("Practical Networking", "Everything switches do (part 1)", "AhOU2eOpmX0", 12), LessonVideo("NetworkChuck", "Port Security (CCNA EP 14)", "0W4JZIWtjLQ", 23)),
        LessonPair(8, LessonVideo("Jeremy's IT Lab", "Interfaces and cables (Day 2)", "ieTH5lVhNaY", 36), LessonVideo("NetworkChuck", "Fiber optic cables (CCNA EP 13)", "E3DEJ7odWq0", 19)),
        LessonPair(9, LessonVideo("Jeremy's IT Lab", "VLANs part 2: trunks + router-on-a-stick (Day 17)", "Jl9OOzNaBDU", 40), LessonVideo("NetworkChuck", "What is a switch? (CCNA Day 1)", "9eH16Fxeb9o", 23)),
        LessonPair(10, LessonVideo("Practical Networking", "Everything routers do (part 1)", "AzXys5kxpAM", 17), LessonVideo("NetworkChuck", "Real-life example: TCP/IP + OSI (EP 4)", "3kfO61Mensg", 17)),
        LessonPair(11, LessonVideo("Jeremy's IT Lab", "Wireless fundamentals (Day 55)", "zuYiktLqNYQ", 36), LessonVideo("PowerCert", "Wi-Fi standards and generations", "hhks5xSpM-0", 9)),
        LessonPair(12, LessonVideo("Professor Messer", "Network troubleshooting methodology", "dovuPm3dGhc", 8), LessonVideo("PowerCert", "Ping and tracert", "vJV-GBZ6PeM", 5)),
        LessonPair(13, LessonVideo("Jeremy's IT Lab", "Syslog (Day 41)", "RaQPSKQ4J5A", 28), LessonVideo("Anson Alexander", "Wireshark tutorial for beginners", "qTaOZrDnMzQ", 20)),
        LessonPair(14, LessonVideo("Practical Networking", "OSI Model part 2", "0aGqGKrRE0g", 14), LessonVideo("NetworkChuck", "How the OSI model works on YouTube (EP 5)", "oIRkXulqJA4", 22)),
        LessonPair(15, LessonVideo("Practical Networking", "ARP in less than 5 minutes", "QPi5Nvxaosw", 4), LessonVideo("NetworkChuck", "Private IP addresses (the fix for running out)", "8bhvn9tQk8o", 15)),
        LessonPair(16, LessonVideo("Jeremy's IT Lab", "SSH (Day 42)", "AvgYqI2qSD4", 31), LessonVideo("PowerCert", "Telnet vs SSH", "tZop-zjYkrU", 5)),
        LessonPair(17, LessonVideo("Jeremy's IT Lab", "Standard ACLs (Day 34)", "z023_eRUtSo", 47), LessonVideo("PowerCert", "VPN explained", "R-JUOpCgTZc", 7)),
        LessonPair(18, LessonVideo("Professor Messer", "The CIA triad", "SBcDGb9l6yo", 5), LessonVideo("NetworkChuck", "Let's hack your home network (CCNA EP 9)", "80vIin4xGp8", 30)),
        LessonPair(19, LessonVideo("Jeremy's IT Lab", "Virtualization & cloud (Day 54, part 1)", "_S3greGajJA", 39), LessonVideo("NetworkChuck", "You need to learn virtual machines", "wX75Z-4MEoM", 28)),
        LessonPair(20, LessonVideo("Jeremy's IT Lab", "Extended ACLs (Day 35)", "dUttKY_CNXE", 41), LessonVideo("PowerCert", "What is a firewall?", "kDEX1HXybrU", 6)),
        LessonPair(21, LessonVideo("Practical Networking", "Solve any subnetting problem (Subnetting Mastery 3 of 7)", "5-wlfAdcmFQ", 6), LessonVideo("NetworkChuck", "Subnetting my coffee shop", "B1vqKQIPxr0", 15)),
        LessonPair(22, LessonVideo("Jeremy's IT Lab", "VLSM subnetting (Day 15)", "z-JqCedc9EI", 24), LessonVideo("Practical Networking", "VLSM (Subnetting Mastery appendix)", "amKyfbg5G2Q", 12)),
        LessonPair(23, LessonVideo("Practical Networking", "How data moves through the Internet", "YJGGYKAV4pA", 27), LessonVideo("NetworkChuck", "DO NOT design your network like this (EP 6)", "wwwAXlE4OtU", 20)),
        LessonPair(24, LessonVideo("Practical Networking", "Everything hosts do (part 1)", "gYN2qN11-wE", 12), LessonVideo("NetworkChuck", "Subnetting, but in reverse", "6zopTcQFhqM", 8)),
        LessonPair(25, LessonVideo("Jeremy's IT Lab", "LAN architectures (Day 52)", "PvyEcLhmNBk", 28), LessonVideo("NetworkChuck", "Let's subnet your home network (EP 6)", "mJ_5qeqGOaI", 10)),
        LessonPair(26, LessonVideo("Jeremy's IT Lab", "Network devices (Day 1): what the full free CCNA covers", "H8W9oMNSuwo", 30), LessonVideo("NetworkChuck", "You need to get your CCNA", "f9IolErKgsw", 9)),
    )
    val lessonByBundle = lessons.associateBy { it.bundle }
    fun toggle(r: Roadmap, key: String) { if (key in r.studyDone) r.studyDone.remove(key) else r.studyDone.add(key) }

    fun creatorOf(f: Finding): Creator? {
        val t = (f.title + " " + f.detail).lowercase()
        return creators.firstOrNull { c -> c.keywords.any { it in t } }
    }
}
