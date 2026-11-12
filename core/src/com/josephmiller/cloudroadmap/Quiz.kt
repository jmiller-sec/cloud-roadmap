package com.josephmiller.cloudroadmap

import java.time.LocalDate

class Q(val unit: Int, val q: String, val options: List<String>, val answer: Int)

/** Question bank for warm-ups (3 questions) and the override test (5 questions, must be perfect). */
object Quiz {
    val bank = listOf(
        // Unit 1: models, IP addressing, DHCP, NAT
        Q(1, "Which OSI layer do routers work at?", listOf("Layer 2 Data Link", "Layer 3 Network", "Layer 4 Transport", "Layer 7 Application"), 1),
        Q(1, "Which OSI layer uses MAC addresses?", listOf("Layer 1 Physical", "Layer 2 Data Link", "Layer 3 Network", "Layer 5 Session"), 1),
        Q(1, "How many layers does the TCP/IP model have?", listOf("3", "4", "5", "7"), 1),
        Q(1, "What does DHCP do?", listOf("Translates names to IPs", "Hands out IP addresses automatically", "Encrypts traffic", "Blocks ports"), 1),
        Q(1, "How many usable hosts are in a /24 network?", listOf("256", "255", "254", "128"), 2),
        Q(1, "Which address is private?", listOf("8.8.8.8", "192.168.1.10", "172.217.1.1", "1.1.1.1"), 1),
        Q(1, "What does NAT do?", listOf("Lets many private IPs share one public IP", "Assigns MAC addresses", "Speeds up Wi-Fi", "Resolves domain names"), 0),
        Q(1, "What is the subnet mask for /26?", listOf("255.255.255.0", "255.255.255.128", "255.255.255.192", "255.255.255.224"), 2),
        Q(1, "Data at the Transport layer is called a…", listOf("Frame", "Packet", "Segment", "Bit"), 2),
        Q(1, "IPv6 addresses are how many bits long?", listOf("32", "64", "128", "256"), 2),
        Q(1, "Which command shows a Windows PC's IP settings?", listOf("ipconfig", "ifup", "netstat -r", "show ip"), 0),
        // Unit 2: switching, VLANs, layer 2 security, cabling
        Q(2, "What does a switch use to forward frames?", listOf("IP addresses", "MAC address table", "DNS records", "Port numbers"), 1),
        Q(2, "What does ARP do?", listOf("Finds the MAC for a known IP", "Finds the IP for a domain name", "Assigns IPs", "Routes between VLANs"), 0),
        Q(2, "A VLAN creates a separate…", listOf("Broadcast domain", "Collision-free cable", "Public IP", "Wi-Fi channel"), 0),
        Q(2, "A port that carries many VLANs is called a…", listOf("Access port", "Trunk port", "Console port", "Mirror port"), 1),
        Q(2, "What tag standard do trunks use?", listOf("802.11", "802.1Q", "802.3af", "802.1X"), 1),
        Q(2, "What does Spanning Tree Protocol prevent?", listOf("Switching loops", "Weak passwords", "DHCP spoofing only", "Slow Wi-Fi"), 0),
        Q(2, "Port security with 'violation shutdown' does what on a violation?", listOf("Logs only", "Puts the port in err-disabled", "Reboots the switch", "Moves the port to VLAN 1"), 1),
        Q(2, "Router-on-a-stick is used for…", listOf("Inter-VLAN routing", "Wireless roaming", "NAT", "Fiber links"), 0),
        Q(2, "Max length of a Cat6 Ethernet run (standard)?", listOf("10 m", "50 m", "100 m", "500 m"), 2),
        Q(2, "Which cable type is immune to electrical interference?", listOf("UTP", "STP", "Coax", "Fiber"), 3),
        // Unit 3: wireless, performance, monitoring
        Q(3, "Which Wi-Fi security should you use today (of these)?", listOf("WEP", "WPA", "WPA2/WPA3", "Open"), 2),
        Q(3, "SSID is the…", listOf("Wi-Fi network name", "Router's MAC", "Encryption key", "Channel number"), 0),
        Q(3, "Which 2.4 GHz channels don't overlap?", listOf("1, 6, 11", "1, 2, 3", "2, 7, 12", "Any three"), 0),
        Q(3, "Which band reaches farther through walls?", listOf("2.4 GHz", "5 GHz", "6 GHz", "All the same"), 0),
        Q(3, "Latency is…", listOf("Delay for data to travel", "Total bandwidth", "Lost packets", "Number of hops"), 0),
        Q(3, "Jitter is…", listOf("Variation in delay", "A cable fault", "Packet encryption", "Signal strength"), 0),
        Q(3, "QoS is used to…", listOf("Prioritize important traffic like voice", "Encrypt traffic", "Hand out IPs", "Block websites"), 0),
        Q(3, "A network baseline is…", listOf("Normal performance to compare against", "The first router installed", "The default VLAN", "The ISP's speed test"), 0),
        Q(3, "Syslog is used to…", listOf("Collect device logs in one place", "Sync clocks", "Assign IPs", "Route between VLANs"), 0),
        Q(3, "Why set NTP on network devices?", listOf("Accurate log timestamps", "Faster Wi-Fi", "Stronger passwords", "More bandwidth"), 0),
        Q(3, "First step of the troubleshooting method?", listOf("Identify the problem", "Implement the fix", "Document findings", "Escalate"), 0),
        // Unit 4: security
        Q(4, "Which is secure remote management?", listOf("Telnet", "SSH", "HTTP", "FTP"), 1),
        Q(4, "An ACL is used to…", listOf("Permit or deny traffic", "Assign VLANs", "Speed up routing", "Encrypt Wi-Fi"), 0),
        Q(4, "Every ACL ends with an invisible…", listOf("permit any", "deny any", "log all", "redirect"), 1),
        Q(4, "The 'C' in the CIA triad is…", listOf("Control", "Confidentiality", "Compliance", "Connectivity"), 1),
        Q(4, "A VPN mainly provides…", listOf("An encrypted tunnel over an untrusted network", "Faster internet", "Free IPs", "Wi-Fi coverage"), 0),
        Q(4, "Phishing is an attack that targets…", listOf("People", "Cables", "Switch ports", "DNS servers only"), 0),
        Q(4, "Least privilege means…", listOf("Users get only the access they need", "Everyone is admin", "No passwords", "Guest Wi-Fi only"), 0),
        Q(4, "MFA means using…", listOf("Two or more types of proof", "Two passwords", "A longer password", "A VPN"), 0),
        Q(4, "Device hardening includes…", listOf("Disabling unused ports and services", "Turning on Telnet", "Default passwords", "Open Wi-Fi"), 0),
        Q(4, "In cloud 'shared responsibility', the customer always handles…", listOf("Their data and access", "Physical data centers", "Hypervisor patches", "Power and cooling"), 0),
        Q(4, "A container differs from a VM because it…", listOf("Shares the host OS kernel", "Needs its own full OS", "Can't run apps", "Is always physical"), 0),
        // Unit 5: design + validation
        Q(5, "High availability is about…", listOf("Avoiding single points of failure", "The cheapest design", "One big switch", "Open Wi-Fi"), 0),
        Q(5, "Splitting users into VLANs mainly improves…", listOf("Security and broadcast control", "Cable length", "Wi-Fi range", "Power use"), 0),
        Q(5, "Designing for growth means…", listOf("Leaving room in IP plans and ports", "Using every IP now", "Avoiding documentation", "One flat network"), 0),
        Q(5, "A test plan should list…", listOf("Test, expected result, actual result", "Only passwords", "Only device names", "Vendor prices"), 0),
        Q(5, "Why document a network?", listOf("So others can support and fix it", "It's optional decoration", "To slow changes", "Only for audits"), 0),
        Q(5, "Putting guests on a separate VLAN + ACL is an example of…", listOf("Segmentation", "Redundancy", "QoS", "NAT"), 0),
        Q(5, "Two links between core switches give…", listOf("Redundancy", "Lower security", "Fewer VLANs", "No need for STP"), 0),
        Q(5, "A post-cutover review checks…", listOf("Whether the change worked as planned", "Vendor discounts", "New hire forms", "Cable colors"), 0),
    )

    /** [count] questions for [unit], stable for a given day + purpose so reopening shows the same set. */
    fun pick(unit: Int, count: Int, day: LocalDate, purpose: String): List<Q> {
        val pool = bank.filter { it.unit == unit }
        val rnd = java.util.Random(day.toEpochDay() * 31 + purpose.hashCode())
        // Shuffle the answer order too, so the right answer isn't usually the first one.
        return pool.shuffled(rnd).take(count).map { q ->
            val order = q.options.indices.shuffled(rnd)
            Q(q.unit, q.q, order.map { q.options[it] }, order.indexOf(q.answer))
        }
    }

    /** Quick check after a lesson video: 3 questions on that lesson's unit. */
    fun afterVideo(b: StudyBundle, which: String, day: LocalDate) = pick(b.unit, 3, day, "vid:${b.n}:$which")
}
