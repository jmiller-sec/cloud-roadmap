package com.josephmiller.cloudroadmap

/** A video creator Joseph learns from, with what to watch and where. */
/** One video for a CS1015 lesson. Opens a YouTube search scoped to the channel, so it always finds the current upload. */
class LessonVideo(val channel: String, val title: String, val query: String) {
    val url get() = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode("$channel $query", "UTF-8")
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

    /** Watch the Practical Networking one first (learn it), then NetworkChuck (make it stick). Commute is the best time. */
    val lessons = listOf(
        LessonPair(1, LessonVideo("Practical Networking", "OSI Model: A Practical Perspective", "OSI Model A Practical Perspective Networking Fundamentals Lesson 2"), LessonVideo("NetworkChuck", "Let's follow a packet through the OSI model", "what is TCP/IP and OSI CCNA EP 3")),
        LessonPair(2, LessonVideo("Practical Networking", "Subnetting Mastery (start here)", "Subnetting Mastery"), LessonVideo("NetworkChuck", "What is an IP address? (You SUCK at Subnetting EP 1)", "what is an IP Address You SUCK at Subnetting EP 1")),
        LessonPair(3, LessonVideo("Practical Networking", "Hosts, IP addresses, networks", "Network Devices Hosts IP Addresses Networks Lesson 1"), LessonVideo("NetworkChuck", "DNS explained", "DNS explained")),
        LessonPair(4, LessonVideo("Practical Networking", "Hub, bridge, switch, router", "Hub Bridge Switch Router Network Devices"), LessonVideo("NetworkChuck", "We ran OUT of IP addresses (NAT, EP 2)", "we ran OUT of IP Addresses You SUCK at Subnetting EP 2")),
        LessonPair(5, LessonVideo("Practical Networking", "Packet Traveling: how packets move", "Packet Traveling How Packets Move Through a Network"), LessonVideo("NetworkChuck", "What is a router? (CCNA EP 2)", "What is a router CCNA EP 2")),
        LessonPair(6, LessonVideo("Jeremy's IT Lab", "VLANs part 1 (Day 16)", "Free CCNA VLANs Part 1 Day 16"), LessonVideo("NetworkChuck", "VLANs explained", "VLAN explained")),
        LessonPair(7, LessonVideo("Practical Networking", "Everything switches do", "Everything Switches do Networking Fundamentals Lesson 4"), LessonVideo("NetworkChuck", "I can HACK your switches (Port Security, EP 14)", "i can HACK your switches Port Security CCNA EP 14")),
        LessonPair(8, LessonVideo("Jeremy's IT Lab", "Interfaces and cables (Day 3)", "Free CCNA Interfaces and Cables Day 3"), LessonVideo("NetworkChuck", "Fiber optics is AMAZING (CCNA EP 13)", "fiber optics is AMAZING CCNA EP13")),
        LessonPair(9, LessonVideo("Jeremy's IT Lab", "VLANs part 2: trunks + router-on-a-stick (Day 17)", "Free CCNA VLANs Part 2 Day 17"), LessonVideo("NetworkChuck", "What is a network switch? (CCNA EP 1)", "what is a NETWORK SWITCH CCNA EP 1")),
        LessonPair(10, LessonVideo("Practical Networking", "Everything routers do", "Everything Routers do Networking Fundamentals"), LessonVideo("NetworkChuck", "Real-life example: TCP/IP + OSI (EP 4)", "REAL LIFE example TCP/IP and OSI Layers CCNA EP 4")),
        LessonPair(11, LessonVideo("Jeremy's IT Lab", "Wireless fundamentals (Day 55)", "Free CCNA Wireless Fundamentals Day 55"), LessonVideo("NetworkChuck", "Wi-Fi explained", "WiFi explained")),
        LessonPair(12, LessonVideo("Professor Messer", "Network troubleshooting methodology", "Network Troubleshooting Methodology N10-009"), LessonVideo("NetworkChuck", "Ping, traceroute and network commands", "network troubleshooting commands ping traceroute")),
        LessonPair(13, LessonVideo("Jeremy's IT Lab", "Syslog (Day 41), then NTP (Day 37)", "Free CCNA Syslog Day 41"), LessonVideo("NetworkChuck", "Wireshark for beginners", "wireshark tutorial")),
        LessonPair(14, LessonVideo("Practical Networking", "OSI Model part 2", "OSI Model A Practical Perspective Part 2"), LessonVideo("NetworkChuck", "How the OSI model works on YouTube (EP 5)", "how the OSI model works on YouTube CCNA EP 5")),
        LessonPair(15, LessonVideo("Practical Networking", "ARP explained", "ARP Address Resolution Protocol"), LessonVideo("NetworkChuck", "We ran out of IP addresses... what now? (EP 3)", "we ran OUT of IP Addresses What now You SUCK at Subnetting EP 3")),
        LessonPair(16, LessonVideo("Jeremy's IT Lab", "SSH (Day 42)", "Free CCNA SSH Day 42"), LessonVideo("NetworkChuck", "SSH explained", "SSH explained")),
        LessonPair(17, LessonVideo("Jeremy's IT Lab", "Standard ACLs (Day 34)", "Free CCNA Standard ACLs Day 34"), LessonVideo("NetworkChuck", "What is a VPN?", "what is a VPN")),
        LessonPair(18, LessonVideo("Professor Messer", "The CIA triad", "CIA Triad Security+ SY0-701"), LessonVideo("NetworkChuck", "Cybersecurity basics", "cybersecurity basics")),
        LessonPair(19, LessonVideo("Jeremy's IT Lab", "Virtualization & cloud (Day 54)", "Free CCNA Virtualization Cloud Day 54"), LessonVideo("NetworkChuck", "You need to learn virtual machines", "you need to learn Virtual Machines RIGHT NOW")),
        LessonPair(20, LessonVideo("Jeremy's IT Lab", "Extended ACLs (Day 35)", "Free CCNA Extended ACLs Day 35"), LessonVideo("NetworkChuck", "What is a firewall?", "what is a firewall")),
        LessonPair(21, LessonVideo("Practical Networking", "Subnetting practice", "Subnetting Mastery practice"), LessonVideo("NetworkChuck", "Subnetting my coffee shop (EP 7)", "Help me build my coffee shop network you suck at subnetting EP 7")),
        LessonPair(22, LessonVideo("Jeremy's IT Lab", "VLSM subnetting (Day 15)", "Free CCNA Subnetting Part 3 VLSM Day 15"), LessonVideo("NetworkChuck", "VLSM: subnet a subnet", "VLSM you suck at subnetting")),
        LessonPair(23, LessonVideo("Practical Networking", "Packet Traveling (validate your design)", "Packet Traveling How Packets Move Through a Network"), LessonVideo("NetworkChuck", "DO NOT design your network like this (EP 6)", "DO NOT design your network like this FREE CCNA EP 6")),
        LessonPair(24, LessonVideo("Practical Networking", "Hosts, IPs, networks (final review)", "Network Devices Hosts IP Addresses Networks Lesson 1"), LessonVideo("NetworkChuck", "Reverse subnetting (EP 8)", "Reverse subnetting you suck at subnetting EP 8")),
        LessonPair(25, LessonVideo("Jeremy's IT Lab", "LAN architectures (Day 52)", "Free CCNA LAN Architectures Day 52"), LessonVideo("NetworkChuck", "Let's subnet your home network (EP 6)", "let's subnet your home network You SUCK at subnetting EP 6")),
        LessonPair(26, LessonVideo("Jeremy's IT Lab", "What the full free CCNA covers (Day 1)", "Free CCNA Network Devices Day 1"), LessonVideo("NetworkChuck", "Free CCNA: what's next", "Free CCNA 200-301 preview")),
    )
    val lessonByBundle = lessons.associateBy { it.bundle }
    fun toggle(r: Roadmap, key: String) { if (key in r.studyDone) r.studyDone.remove(key) else r.studyDone.add(key) }

    fun creatorOf(f: Finding): Creator? {
        val t = (f.title + " " + f.detail).lowercase()
        return creators.firstOrNull { c -> c.keywords.any { it in t } }
    }
}
