package com.josephmiller.cloudroadmap

/** A video creator Joseph learns from, with what to watch and where. */
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
            "chuck", "NetworkChuck", "Networking basics: IP, subnets, DNS, DHCP",
            listOf("net_videos"),
            listOf("YouTube channel" to "https://www.youtube.com/@NetworkChuck"),
            listOf("networkchuck", "network chuck"),
        ),
        Creator(
            "hackersploit", "HackerSploit", "MITRE ATT&CK + Linux logging (paired with TryHackMe)",
            emptyList(),
            listOf("YouTube channel" to "https://www.youtube.com/@HackerSploit"),
            listOf("hackersploit"),
        ),
    )

    fun creatorOf(f: Finding): Creator? {
        val t = (f.title + " " + f.detail).lowercase()
        return creators.firstOrNull { c -> c.keywords.any { it in t } }
    }
}
