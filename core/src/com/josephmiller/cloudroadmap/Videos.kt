package com.josephmiller.cloudroadmap

/** A video creator Joseph learns from, with what to watch and where. */
/** One video for a CS1015 lesson. Opens a YouTube search scoped to the channel, so it always finds the current upload. */
class LessonVideo(val channel: String, val title: String, val query: String) {
    val url get() = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode("$channel $query", "UTF-8")
}

/** Practical Networking (learn it) + NetworkChuck (make it stick), matched to each bundle. */
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

    /** Watch the Practical Networking one first (learn it), then NetworkChuck (make it stick). Commute is the best time. */
    val lessons = listOf(
        LessonPair(1, LessonVideo("Practical Networking", "OSI Model", "OSI model"), LessonVideo("NetworkChuck", "The OSI model", "OSI model explained")),
        LessonPair(2, LessonVideo("Practical Networking", "Subnetting Mastery (start here)", "subnetting mastery"), LessonVideo("NetworkChuck", "You SUCK at subnetting (ep 1)", "you suck at subnetting")),
        LessonPair(3, LessonVideo("Practical Networking", "Everything hosts need to know", "everything hosts need to know"), LessonVideo("NetworkChuck", "DNS explained", "what is DNS")),
        LessonPair(4, LessonVideo("Practical Networking", "Hosts, IPs and networks (review)", "networking fundamentals hosts ip address network"), LessonVideo("NetworkChuck", "DHCP explained", "DHCP explained")),
        LessonPair(5, LessonVideo("Practical Networking", "Packet traveling + ARP", "packet traveling how packets move through a network"), LessonVideo("NetworkChuck", "Switch vs router vs hub", "router vs switch")),
        LessonPair(6, LessonVideo("Practical Networking", "VLANs", "VLAN"), LessonVideo("NetworkChuck", "VLANs explained", "VLANs")),
        LessonPair(7, LessonVideo("Practical Networking", "Everything switches do", "everything switches do"), LessonVideo("NetworkChuck", "Port security / attacking switches", "switch port security")),
        LessonPair(8, LessonVideo("Practical Networking", "Physical layer basics", "layer 1 cables ethernet"), LessonVideo("NetworkChuck", "Ethernet cables explained", "ethernet cables")),
        LessonPair(9, LessonVideo("Practical Networking", "Router on a stick", "router on a stick"), LessonVideo("NetworkChuck", "Trunk ports explained", "trunk ports")),
        LessonPair(10, LessonVideo("Practical Networking", "Everything routers do (review)", "everything routers do"), LessonVideo("NetworkChuck", "Networking review: how the internet works", "how the internet works")),
        LessonPair(11, LessonVideo("Practical Networking", "Wireless basics", "wireless wifi"), LessonVideo("NetworkChuck", "Wi-Fi explained", "wifi explained")),
        LessonPair(12, LessonVideo("Practical Networking", "Troubleshooting with ping + traceroute", "troubleshooting ping traceroute"), LessonVideo("NetworkChuck", "Network troubleshooting commands", "network troubleshooting commands")),
        LessonPair(13, LessonVideo("Practical Networking", "Syslog + NTP", "syslog ntp"), LessonVideo("NetworkChuck", "Logs and Wireshark basics", "wireshark tutorial")),
        LessonPair(14, LessonVideo("Practical Networking", "Packet traveling (review)", "packet traveling"), LessonVideo("NetworkChuck", "Wireshark for beginners", "wireshark for beginners")),
        LessonPair(15, LessonVideo("Practical Networking", "Networking fundamentals (review)", "networking fundamentals"), LessonVideo("NetworkChuck", "NAT explained", "NAT explained")),
        LessonPair(16, LessonVideo("Practical Networking", "SSH + secure remote access", "ssh"), LessonVideo("NetworkChuck", "SSH explained", "what is ssh")),
        LessonPair(17, LessonVideo("Practical Networking", "Access lists + IPsec VPN", "ipsec vpn"), LessonVideo("NetworkChuck", "Firewalls + VPNs explained", "what is a vpn")),
        LessonPair(18, LessonVideo("Practical Networking", "TLS / SSL and encryption", "tls ssl"), LessonVideo("NetworkChuck", "Cybersecurity basics: the CIA triad", "cia triad cybersecurity")),
        LessonPair(19, LessonVideo("Practical Networking", "NAT (review before cloud)", "nat network address translation"), LessonVideo("NetworkChuck", "Virtual machines explained", "virtual machines explained")),
        LessonPair(20, LessonVideo("Practical Networking", "Access control lists", "access control list acl"), LessonVideo("NetworkChuck", "Firewalls explained", "what is a firewall")),
        LessonPair(21, LessonVideo("Practical Networking", "Subnetting practice (review)", "subnetting practice"), LessonVideo("NetworkChuck", "You SUCK at subnetting (practice ep)", "subnetting practice")),
        LessonPair(22, LessonVideo("Practical Networking", "VLSM subnetting (network design)", "vlsm subnetting"), LessonVideo("NetworkChuck", "Build a home network / homelab", "homelab network setup")),
        LessonPair(23, LessonVideo("Practical Networking", "Packet traveling (validate your design)", "packet traveling routers"), LessonVideo("NetworkChuck", "Ping, traceroute, nslookup in action", "ping traceroute nslookup")),
        LessonPair(24, LessonVideo("Practical Networking", "Networking fundamentals (final review)", "networking fundamentals"), LessonVideo("NetworkChuck", "Networking in one video (review)", "networking basics")),
        LessonPair(25, LessonVideo("Practical Networking", "Everything routers do (Touchstone refresh)", "everything routers do"), LessonVideo("NetworkChuck", "Network diagrams / documentation", "network diagram")),
        LessonPair(26, LessonVideo("Practical Networking", "What to learn next", "course"), LessonVideo("NetworkChuck", "Free CCNA / what to learn next", "free CCNA")),
    )
    val lessonByBundle = lessons.associateBy { it.bundle }
    fun toggle(r: Roadmap, key: String) { if (key in r.studyDone) r.studyDone.remove(key) else r.studyDone.add(key) }

    fun creatorOf(f: Finding): Creator? {
        val t = (f.title + " " + f.detail).lowercase()
        return creators.firstOrNull { c -> c.keywords.any { it in t } }
    }
}
