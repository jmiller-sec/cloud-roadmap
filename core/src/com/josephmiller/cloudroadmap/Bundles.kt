package com.josephmiller.cloudroadmap

/**
 * CS1015 done as "bundles": one topic three ways in one sitting.
 * Sophia piece -> matching TryHackMe room -> matching Packet Tracer lab -> screenshots -> 2-line portfolio note.
 * Sections are checkboxes (keys "b:N:<section>") so a bundle can stop mid-way and resume at the same section.
 */
class Section(val id: String, val title: String, val minutes: Int, val steps: List<String>, val shots: List<String> = emptyList(), val url: String? = null)

class StudyBundle(val n: Int, val unit: Int, val title: String, val light: Boolean, val sections: List<Section>, val noteHint: String) {
    fun key(section: String) = "b:$n:$section"
    val minutes get() = sections.sumOf { it.minutes }
    val folder get() = "CloudRoadmap/b%02d".format(n)
}

object Bundles {
    const val SOPHIA = "https://app.sophia.org"
    const val PRESEC = "https://tryhackme.com/path/outline/presecurity"
    private fun room(code: String) = "https://tryhackme.com/room/$code"
    private fun yt(q: String) = "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(q, "UTF-8")

    private fun warm() = Section("warm", "Warm-up: 3 quick questions on last time", 5, listOf("Tap the check to start. Any score counts; it's just to wake the memory up."))
    private fun sophia(name: String, mins: Int, extra: List<String> = emptyList(), shot: String = "Your score on this piece") =
        Section("sophia", "Sophia · $name", mins,
            listOf("Open CS1015 in Sophia and go to: $name.") + extra + listOf("Finish it and submit."), listOf(shot), SOPHIA)
    private fun thm(name: String, url: String, mins: Int, extra: List<String> = emptyList()) =
        Section("thm", "TryHackMe · $name", mins,
            listOf("Open the room, click Join/Start Room.") + extra + listOf("Answer every task question until the room shows Completed."),
            listOf("The room's Completed banner"), url)
    private fun video(name: String, q: String, mins: Int) =
        Section("thm", "Video · $name (no matching free room)", mins, listOf("Watch it start to finish. Pause and say the main idea out loud once."), emptyList(), yt(q))
    private fun pt(name: String, mins: Int, steps: List<String>, shots: List<String>) =
        Section("pt", "Packet Tracer · $name", mins, steps, shots)
    private fun note() = Section("note", "Portfolio note (2 lines)", 2, listOf("Type what you built or proved today, in 2 lines. It saves to your Portfolio list."))

    private fun b(n: Int, unit: Int, title: String, hint: String, vararg s: Section, light: Boolean = false) =
        StudyBundle(n, unit, title, light, (if (n > 1) listOf(warm()) else emptyList()) + s.toList() + note(), hint)

    val all = listOf(
        // ---------------- Unit 1: Network Foundations ----------------
        b(1, 1, "Network Models", "Built a 2-PC LAN in Packet Tracer, verified with ping, traced the packet through the OSI layers.",
            sophia("Challenge 1.1 Network Models and Data Transmission", 45, listOf("While reading, write the 7 OSI layers and the 4 TCP/IP layers on paper.")),
            thm("What is Networking?", room("whatisnetworking"), 30),
            pt("Lab 1: two PCs and a switch", 30, listOf(
                "Open Packet Tracer (already installed) and log in with your NetAcad account.",
                "Drag in 1 switch (2960) and 2 PCs. Connect each PC to the switch with Copper Straight-Through.",
                "PC0 > Desktop > IP Configuration: 192.168.1.10, mask 255.255.255.0. PC1: 192.168.1.11.",
                "PC0 > Desktop > Command Prompt: ping 192.168.1.11 (expect 4 replies).",
                "Switch to Simulation mode, ping again, click the envelope, open the OSI Model tab.",
                "File > Save As: lab01-osi.pkt"),
                listOf("The diagram with green link lights", "The successful ping", "The OSI Model tab in Simulation mode"))),
        b(2, 1, "IP Addressing, DHCP, NAT", "Built two subnets joined by a router and handed out addresses with DHCP; verified cross-subnet ping.",
            sophia("Challenge 1.2 IP Addressing and Configuration", 60, listOf("Practice 2 subnetting problems on paper before the quiz.")),
            thm("Intro to LAN", room("introtolan"), 40, listOf("Pay extra attention to the subnetting and DHCP tasks.")),
            pt("Lab 2: two subnets + DHCP", 45, listOf(
                "New file. 1 router (2911), 2 switches, 2 PCs per switch.",
                "Router > CLI: interface g0/0 > ip address 192.168.1.1 255.255.255.0 > no shutdown. Same on g0/1 with 192.168.2.1.",
                "Add DHCP on the router: ip dhcp pool LAN1 > network 192.168.1.0 255.255.255.0 > default-router 192.168.1.1. Repeat for LAN2.",
                "Set every PC to DHCP and confirm each gets an address.",
                "Ping a PC on the other subnet.",
                "Save: lab02-subnets.pkt"),
                listOf("The diagram", "One PC showing its DHCP address", "Cross-subnet ping working"))),
        b(3, 1, "Touchstone Task 1", "Documented a small network: topology, IP plan and core services (DHCP, DNS, web).",
            sophia("Touchstone Task 1 (GreenLeaf topology, IP addressing, core services)", 60, listOf("Use your lab 2 network as your model: draw it, list every IP, list the services.")),
            thm("OSI Model", room("osimodelzi"), 30),
            pt("Lab 3: add core services", 30, listOf(
                "Open lab02-subnets.pkt. Add a Server on LAN1 with static IP 192.168.1.100.",
                "Server > Services: turn on DNS (add record www.lab.local -> 192.168.1.100) and HTTP.",
                "On the router DHCP pools add: dns-server 192.168.1.100. Renew DHCP on the PCs.",
                "From a LAN2 PC: Desktop > Web Browser > www.lab.local",
                "Save: lab03-services.pkt"),
                listOf("The web page loading by name", "The DNS record on the server"))),
        b(4, 1, "Milestone 1 (review day)", "Passed CS1015 Unit 1 exam (network models, IP addressing, DHCP, NAT).",
            Section("thm", "TryHackMe · Section 5 Topic Rewind Recap", 20, listOf("On the Pre Security page, open Section 5's Topic Rewind Recap and do it as a warm review."), listOf("Recap completed"), PRESEC),
            sophia("Milestone 1", 60, listOf("Retake anything you missed in Challenges 1.1-1.2 first if you're unsure."), "Your Milestone 1 score"),
            light = true),

        // ---------------- Unit 2: Switching and Wired Networks ----------------
        b(5, 2, "Devices + Packet Flows", "Traced ARP and ICMP across a router step by step and confirmed the path with tracert.",
            sophia("Challenge 2.1 Network Devices and Packet Flows", 45),
            thm("Packets & Frames", room("packetsframes"), 35),
            pt("Lab 5: watch a packet cross a router", 30, listOf(
                "Open lab02-subnets.pkt.",
                "Simulation mode > Edit Filters: show only ARP and ICMP.",
                "Ping from LAN1 to LAN2 and click Capture/Forward one step at a time. Note where ARP happens.",
                "Realtime mode: tracert 192.168.2.x from a LAN1 PC.",
                "Save: lab05-flows.pkt"),
                listOf("The ARP step in Simulation", "The tracert output"))),
        b(6, 2, "VLANs", "Split one switch into Staff and Guest VLANs and routed between them (router-on-a-stick).",
            sophia("Challenge 2.2 VLAN Configuration and Management", 50),
            thm("Extending Your Network", room("extendingyournetwork"), 35),
            pt("Lab 6: VLANs + inter-VLAN routing", 45, listOf(
                "New file: 1 router, 1 switch, 4 PCs.",
                "Switch: vlan 10 (name STAFF), vlan 20 (name GUEST). Put 2 ports in each: switchport mode access, switchport access vlan 10/20.",
                "Switch port to router: switchport mode trunk.",
                "Router: interface g0/0.10 > encapsulation dot1Q 10 > ip address 192.168.10.1 255.255.255.0. Same for .20.",
                "PCs: static IPs in their VLAN, gateway .1. Ping Staff to Guest.",
                "Switch: show vlan brief",
                "Save: lab06-vlans.pkt"),
                listOf("show vlan brief", "Staff-to-Guest ping working"))),
        b(7, 2, "Switch Security", "Locked down switch ports with port security and showed a violation shutting the port.",
            sophia("Challenge 2.3 Switch Security and Layer 2 Protection", 45),
            video("Professor Messer port security + STP", "Professor Messer port security spanning tree", 20),
            pt("Lab 7: port security", 30, listOf(
                "Open lab06-vlans.pkt.",
                "On one access port: switchport port-security > maximum 1 > violation shutdown > mac-address sticky.",
                "Ping once so the switch learns the MAC.",
                "Unplug that PC, plug in a new PC on the same port, ping: the port goes err-disabled (red).",
                "show port-security interface <port>",
                "Save: lab07-portsec.pkt"),
                listOf("The red, shut-down port", "show port-security output"))),
        b(8, 2, "Cabling", "Identified cable types (straight, crossover, fiber, console) and fixed a wrong-cable link.",
            sophia("Challenge 2.4 Physical Layer Cabling and Connectors", 40),
            video("Professor Messer copper + fiber cabling", "Professor Messer cabling copper fiber", 20),
            pt("Lab 8: cables", 25, listOf(
                "New file. Connect 2 switches with a straight-through cable: note the link color, then swap to crossover.",
                "Connect a router and a switch with fiber (add fiber modules if needed).",
                "Connect a PC to the router's console port, then Desktop > Terminal.",
                "Save: lab08-cables.pkt"),
                listOf("Each cable type connected", "The console Terminal session"))),
        b(9, 2, "Touchstone Task 2", "Documented switch configs, VLAN layout and physical connections for a small business.",
            sophia("Touchstone Task 2 (switches, VLANs, physical connections)", 60, listOf("Use labs 6-8 as your model: paste show vlan brief, list trunk/access ports, list cable types.")),
            pt("Lab 9: capture your configs", 15, listOf(
                "Open lab07-portsec.pkt.",
                "On the switch: show running-config, show vlan brief, show interfaces trunk.",
                "Copy each output into a text file for the task."),
                listOf("show running-config", "show interfaces trunk"))),
        b(10, 2, "Milestone 2 (review day)", "Passed CS1015 Unit 2 exam (switching, VLANs, Layer 2 security, cabling).",
            video("Professor Messer VLANs", "Professor Messer VLAN trunking", 20),
            sophia("Milestone 2", 60, emptyList(), "Your Milestone 2 score"),
            light = true),

        // ---------------- Unit 3: Wireless and Performance ----------------
        b(11, 3, "Wireless", "Built a WPA2 Wi-Fi network for a small office and joined a laptop with DHCP.",
            sophia("Challenge 3.1 Wireless Networking Fundamentals", 50),
            video("Professor Messer wireless standards + security", "Professor Messer 802.11 wireless standards WPA2 WPA3", 25),
            pt("Lab 11: Wi-Fi office", 40, listOf(
                "New file: 1 wireless router (WRT300N), 1 switch, 2 PCs, 1 laptop.",
                "Wireless router GUI > Wireless: set your own SSID. Wireless Security: WPA2 Personal + a passphrase.",
                "Laptop: power off, swap the wired NIC for the wireless module, power on.",
                "Laptop > Desktop > PC Wireless: join your SSID with the passphrase.",
                "Ping a wired PC from the laptop.",
                "Save: lab11-wifi.pkt"),
                listOf("The WPA2 settings page", "The laptop connected", "Ping from laptop"))),
        b(12, 3, "Performance + Troubleshooting", "Broke a working network 3 ways and fixed each like a help desk ticket.",
            sophia("Challenge 3.2 Performance Measurement and Troubleshooting", 45),
            thm("DNS in Detail", room("dnsindetail"), 30),
            pt("Lab 12: break it, fix it", 40, listOf(
                "Open lab11-wifi.pkt.",
                "Break 1: give a PC the wrong default gateway. Find it with ipconfig + ping, fix it.",
                "Break 2: set a wrong DNS server. Show the website fails by name but works by IP. Fix it.",
                "Break 3: change the Wi-Fi passphrase on the router. Show the laptop drops. Reconnect.",
                "Write each as a 3-line ticket: problem, cause, fix.",
                "Save: lab12-troubleshoot.pkt"),
                listOf("Each broken state", "Each fix working"))),
        b(13, 3, "Monitoring + Baselines", "Sent router logs to a syslog server and set NTP so logs have the right time.",
            sophia("Challenge 3.3 Monitoring and Network Baselines", 45),
            thm("HTTP in Detail", room("httpindetail"), 30, listOf("Notice the status codes: they're what you'll read in web logs.")),
            pt("Lab 13: syslog + NTP", 35, listOf(
                "Open lab12-troubleshoot.pkt. Add a Server, static IP.",
                "Server > Services: turn on Syslog and NTP.",
                "Router CLI: logging host <server IP> > ntp server <server IP> > service timestamps log datetime msec.",
                "Shut and re-enable an interface to create log entries.",
                "Server > Services > Syslog: see the entries.",
                "Save: lab13-syslog.pkt"),
                listOf("Syslog entries on the server", "The router logging config"))),
        b(14, 3, "Touchstone Task 3", "Documented a wireless network, its performance checks and its logging setup.",
            sophia("Touchstone Task 3 (wireless, performance metrics, logging)", 60, listOf("Use labs 11-13: SSID + security settings, your 3 tickets, your syslog setup.")),
            thm("How Websites Work", room("howwebsiteswork"), 30)),
        b(15, 3, "Milestone 3 (review day)", "Passed CS1015 Unit 3 exam (wireless, performance, monitoring).",
            thm("Putting it all together", room("puttingitalltogether"), 20),
            sophia("Milestone 3", 60, emptyList(), "Your Milestone 3 score"),
            light = true),

        // ---------------- Unit 4: Network Security ----------------
        b(16, 4, "Threats + Device Hardening", "Hardened a router and switch: SSH only, encrypted passwords, banner, unused ports shut.",
            sophia("Challenge 4.1 Network Threats and Device Hardening", 45),
            Section("thm", "TryHackMe · Operating System Security", 30, listOf("On the Pre Security page, Section 3: open Operating System Security.", "Answer every task question until Completed."), listOf("The room's Completed banner"), PRESEC),
            pt("Lab 16: SSH + hardening", 40, listOf(
                "Open lab13-syslog.pkt.",
                "Router: hostname R1 > ip domain-name lab.local > crypto key generate rsa (1024) > username admin secret <pass>.",
                "line vty 0 4 > transport input ssh > login local. enable secret <pass>. service password-encryption.",
                "banner motd #Authorized access only#",
                "Switch: shut every unused port (interface range ... > shutdown).",
                "From a PC: ssh -l admin <router IP>",
                "Save: lab16-hardening.pkt"),
                listOf("SSH login working", "show running-config with encrypted passwords"))),
        b(17, 4, "Firewalls, ACLs, VPN", "Wrote an ACL that blocks Guest from Staff but allows the internet, and proved it.",
            sophia("Challenge 4.2 Secure Connections and Access Control", 50),
            Section("thm", "TryHackMe · Cryptography Concepts", 30, listOf("On the Pre Security page, Section 7: open Cryptography Concepts.", "Answer every task question until Completed."), listOf("The room's Completed banner"), PRESEC),
            pt("Lab 17: ACL", 40, listOf(
                "Open lab06-vlans.pkt (Staff 10 + Guest 20).",
                "Router: access-list 110 deny ip 192.168.20.0 0.0.0.255 192.168.10.0 0.0.0.255 > access-list 110 permit ip any any.",
                "interface g0/0.20 > ip access-group 110 in",
                "Guest PC: ping a Staff PC (fails). Staff to Guest still allowed.",
                "show access-lists (see the match counts).",
                "Save: lab17-acl.pkt"),
                listOf("Guest ping blocked", "show access-lists with matches"))),
        b(18, 4, "Monitoring, Risk, Privacy", "Mapped network risks to the CIA triad and wrote a one-page risk list.",
            sophia("Challenge 4.3 Monitoring, Risk, and Privacy", 45),
            Section("thm", "TryHackMe · The CIA Triad", 25, listOf("On the Pre Security page, Section 7: open The CIA Triad.", "Answer every task question until Completed."), listOf("The room's Completed banner"), PRESEC),
            Section("pt", "Mini-task: risk list (no new lab)", 20, listOf(
                "Look at your lab 17 network.",
                "Write 5 risks (e.g. weak Wi-Fi password). For each: which CIA part it hurts, and the fix you already built."),
                listOf("Your risk list"))),
        b(19, 4, "Virtualization + Cloud", "Explained VMs, containers and cloud shared responsibility; linked them to Azure.",
            sophia("Challenge 4.4 Virtualization and Cloud Security", 45),
            Section("thm", "TryHackMe · Virtualisation Basics + Cloud Computing Fundamentals", 45, listOf("On the Pre Security page, Section 2: do Virtualisation Basics, then Cloud Computing Fundamentals.", "Answer every task question until Completed."), listOf("Both Completed banners"), PRESEC)),
        b(20, 4, "Touchstone Task 4", "Documented firewall/ACL, VPN and authentication setup with strengths and risks.",
            sophia("Touchstone Task 4 (firewall, VPN, authentication)", 60, listOf("Use labs 16-17 and your risk list: paste the ACL, the SSH setup, and 3 strengths + 3 risks.")),
            pt("Lab 20: capture configs", 15, listOf("Open lab17-acl.pkt and lab16-hardening.pkt.", "show access-lists, show running-config | section line vty, show ip ssh.", "Copy the outputs into your task."), listOf("The three outputs"))),
        b(21, 4, "Milestone 4 (review day)", "Passed CS1015 Unit 4 exam (threats, hardening, ACLs, VPN, cloud security).",
            Section("thm", "TryHackMe · Section 7 Topic Rewind Recap", 20, listOf("On the Pre Security page, open Section 7's Topic Rewind Recap."), listOf("Recap completed"), PRESEC),
            sophia("Milestone 4", 60, emptyList(), "Your Milestone 4 score"),
            light = true),

        // ---------------- Unit 5: Network Design ----------------
        b(22, 5, "Designing Resilient Networks", "Designed one complete office network: VLANs, Wi-Fi, servers, ACL, hardening, logging.",
            sophia("Challenge 5.1 Designing Resilient Networks", 50),
            Section("thm", "TryHackMe · Client-Server Basics", 25, listOf("On the Pre Security page, Section 2: open Client-Server Basics.", "Answer every task question until Completed."), listOf("The room's Completed banner"), PRESEC),
            pt("Lab 22: the full office", 60, listOf(
                "New file: combine what you built: router, switch with Staff/Guest VLANs, a wireless router, a server (DHCP/DNS/HTTP/Syslog/NTP).",
                "Add the Guest-blocking ACL and SSH-only access.",
                "Label every device and subnet with Place Note.",
                "Save: lab22-office.pkt"),
                listOf("The full labeled diagram"))),
        b(23, 5, "Validating Designs", "Wrote and ran a test plan proving every subnet, service and security rule works.",
            sophia("Challenge 5.2 Troubleshooting and Validating Network Designs", 45),
            pt("Lab 23: test plan", 40, listOf(
                "Open lab22-office.pkt.",
                "Make a table: test, expected, actual. At least 8 tests (each VLAN pings gateway, web by name, Guest blocked from Staff, SSH works, Telnet refused, Wi-Fi joins, syslog receives).",
                "Run every test and fill in the actual result. Fix anything that fails."),
                listOf("Your filled test table", "1-2 of the tests running"))),
        b(24, 5, "Milestone 5 (review day)", "Passed CS1015 Unit 5 exam (network design and validation).",
            video("Professor Messer network design basics", "Professor Messer network topologies design", 20),
            sophia("Milestone 5", 60, emptyList(), "Your Milestone 5 score"),
            light = true),
        b(25, 5, "Touchstone 5 (graded, 155 pts)", "Completed a full network documentation portfolio for a growing business (CS1015 Touchstone 5).",
            sophia("Touchstone 5 GreenLeaf Network Documentation Portfolio", 120, listOf(
                "Open the rubric first and list every required section.",
                "Reuse Touchstone Tasks 1-4 for the current-network parts.",
                "Use lab22-office.pkt + your lab 23 test plan for the design and validation parts.",
                "Check each rubric line before you submit.")),
            light = true),
        b(26, 5, "Final Milestone", "Finished Sophia CS1015 Introduction to Networking.",
            Section("thm", "Review your weak spots", 30, listOf("Re-read the questions you missed on Milestones 1-5 (Sophia shows them)."), emptyList(), SOPHIA),
            sophia("Final Milestone", 75, emptyList(), "Your Final Milestone score"),
            light = true),
    )

    fun byN(n: Int): StudyBundle = all.first { it.n == n }
    fun isDone(r: Roadmap, b: StudyBundle) = b.sections.all { b.key(it.id) in r.studyDone }
    fun next(r: Roadmap) = all.filter { !isDone(r, it) }
    fun done(r: Roadmap) = all.count { isDone(r, it) }
}
