package com.josephmiller.cloudroadmap

/** A hands-on portfolio project. Steps are checked off with keys "lab:<id>:<n>" (shared with WTD's Sunday lab task). */
class Project(
    val id: String,
    val level: String, // Beginner | Medium | Expert
    val title: String,
    val why: String,
    val target: String,
    val steps: List<String>,
)

/** Ordered by importance: do #1 first, work down to Expert. */
object Projects {
    val all = listOf(
        // ---------------- BEGINNER: help desk, done before you apply (mid-Jan 2027) ----------------
        Project("lab_ad", "Beginner", "Active Directory home lab",
            "The #1 thing help desk interviews ask about: users, groups, password resets.", "May 29",
            listOf(
                "Install Windows Server (evaluation) in a new VirtualBox VM",
                "Promote it to a domain controller and create your domain",
                "Join a Windows 10/11 VM to the domain",
                "Create users and groups; practice password resets and account unlocks",
                "Set up Group Policy: password rules and a mapped drive",
                "Write it up on GitHub with screenshots",
            )),
        Project("lab_net", "Beginner", "DHCP, DNS + Wireshark troubleshooting",
            "Shows you can fix \"the internet is down\" tickets, the most common help desk call.", "Jun 19",
            listOf(
                "Set up DHCP and DNS on your server",
                "Break DNS/DHCP on purpose, then fix it with ping, ipconfig and nslookup; write down each fix",
                "Capture traffic in Wireshark and find DHCP, DNS and HTTP packets",
                "Write it up on GitHub with screenshots",
            )),
        Project("lab_ticket", "Beginner", "osTicket help desk",
            "Proves you know the ticket workflow every IT job runs on.", "Jul 3",
            listOf(
                "Install osTicket (free) on a VM",
                "Log 5 fake tickets (password reset, printer, slow PC, etc.) and solve them",
                "Log 5 more tickets and write a resolution note for each",
            )),
        Project("lab_ps", "Beginner", "PowerShell user-creation script",
            "Automation stands out on an entry-level resume.", "Jul 17",
            listOf(
                "Write a PowerShell script that creates AD users from a CSV spreadsheet",
                "Test it on 10 users, then post the script on GitHub with a README",
            )),
        Project("gh_portfolio", "Beginner", "GitHub portfolio page",
            "One link on your resume that shows all your work.", "Jul 24",
            listOf(
                "Create your GitHub profile README (who you are, what you're learning)",
                "Link your 4 lab write-ups from it",
                "Pin the 4 lab repos and put the link on your resume and LinkedIn",
            )),
        Project("lab_fw", "Beginner", "Home firewall (OPNsense)",
            "Fixes your real lag (2 ms ping jumps to 177 ms when the house is busy) and proves firewall, NAT and VLAN skills.", "Aug 7",
            listOf(
                "Save your before tests: Wi-Fi 52/47 (8 → 39 ms) and wired 370/370 (2 → 177 ms loaded)",
                "Get the gear: mini PC with 2+ Intel ports, TL-SG105E switch, U7 Pro + PoE+ injector",
                "Install OPNsense behind the AT&T gateway, just for your room (WAN to AT&T, LAN to your switch)",
                "Fix PS5 NAT with UPnP or port forwards; confirm NAT Type 2 in PS5 network test",
                "Turn on traffic shaping (SQM at ~330/330); rerun fast.com wired, goal: loaded ping under 20 ms",
                "Split networks with VLANs (gaming, lab, guest) and write the firewall rules between them",
                "Write it up on GitHub with before/after screenshots and a network diagram",
            )),

        // ---------------- MEDIUM: SOC analyst skills (2027) ----------------
        Project("p_phish", "Medium", "Phishing investigation write-ups",
            "Phishing is the #1 alert a SOC analyst works. Show your process.", "Jul 2027",
            listOf(
                "Pick 3 phishing samples (TCM challenges, Blue Team Labs Online, or your own spam)",
                "For each: headers, sender auth (SPF/DKIM/DMARC), URLs, attachments, verdict",
                "Post a write-up for all 3 on GitHub",
            )),
        Project("p_siem", "Medium", "Home SIEM with detections",
            "Lets you say \"I built and tuned detections\" in a SOC interview.", "Aug 2027",
            listOf(
                "Install Splunk Free (or Wazuh) on a VM",
                "Send Windows event logs + Sysmon from your AD lab into it",
                "Build 3 detections: brute-force logons, new admin account, suspicious PowerShell",
                "Trigger each one on purpose and screenshot the alert",
                "Write it up on GitHub",
            )),
        Project("p_traffic", "Medium", "Network traffic investigations",
            "Packet analysis shows up in almost every SOC skills test.", "Sep 2027",
            listOf(
                "Do 3 exercises from malware-traffic-analysis.net in Wireshark",
                "For each: infected host, IOCs (IPs, domains, hashes), what happened",
                "Post the 3 write-ups on GitHub",
            )),
        Project("p_malware", "Medium", "Malware analysis lab",
            "Safe triage of a suspicious file is a tier 2 skill. Do this only in an isolated lab.", "Oct 2027",
            listOf(
                "Build FLARE-VM in VirtualBox with networking set to host-only, then snapshot it",
                "Do basic static analysis on a practice sample (hashes, strings, PE info)",
                "Look up the hashes on VirusTotal and write a 1-page triage report",
                "Revert the snapshot when you're done",
            )),
        Project("p_ir", "Medium", "Attack, detect, and report",
            "The full SOC loop in one project: the best interview story you can have.", "Nov 2027",
            listOf(
                "Run a safe simulated attack in your own lab (e.g. password spraying against your AD)",
                "Detect it in your home SIEM",
                "Write an incident report: timeline, impact, containment, lessons learned",
                "Post it on GitHub",
            )),

        // ---------------- EXPERT: cloud security (2028+) ----------------
        Project("p_azure", "Expert", "Secure Azure environment",
            "The foundation every cloud security job expects.", "2028",
            listOf(
                "Create an Azure free account and set a budget alert first",
                "Build a VNet with subnets and network security groups",
                "Lock down access: least-privilege roles (RBAC) and MFA",
                "Document the design with a diagram on GitHub",
                "Delete everything you don't need so you aren't billed",
            )),
        Project("p_sentinel", "Expert", "Microsoft Sentinel detection",
            "Sentinel is the SIEM SC-200 tests and many SOCs use.", "2028",
            listOf(
                "Turn on Sentinel (use the free trial) and connect a VM's logs",
                "Write a KQL analytics rule for a simulated attack",
                "Trigger it, investigate the incident, and screenshot it",
                "Write it up, then turn Sentinel off to avoid charges",
            )),
        Project("p_misconfig", "Expert", "Find and fix cloud misconfigurations",
            "Most cloud breaches come from bad settings. Show you can catch them.", "2028",
            listOf(
                "Make a test storage account public and give a test user too many permissions",
                "Find both with Microsoft Defender for Cloud or Azure Policy",
                "Fix them and document before/after",
            )),
        Project("p_python", "Expert", "Python security automation",
            "Automation is what separates senior analysts from tier 1.", "2029",
            listOf(
                "Write a Python script that checks your Azure subscription for public storage or risky roles",
                "Or: a script that enriches alerts with VirusTotal API lookups",
                "Post it on GitHub with a README and sample output",
            )),
        Project("p_terraform", "Expert", "Infrastructure as code with Terraform",
            "Cloud security engineers build secure environments as code.", "2029",
            listOf(
                "Rebuild your secure Azure environment with Terraform",
                "Add a security check (e.g. tfsec or Checkov) and fix what it finds",
                "Post the code on GitHub, then destroy the resources",
            )),
        Project("p_detect", "Expert", "Detection engineering with Sigma",
            "Writing detections mapped to MITRE ATT&CK is a top-tier blue team skill.", "2029",
            listOf(
                "Write 5 Sigma rules mapped to MITRE ATT&CK techniques",
                "Convert them to Splunk SPL and Sentinel KQL",
                "Test each against lab data and publish the repo",
            )),
    )

    val byId = all.associateBy { it.id }
    val steps: Map<String, List<String>> = all.associate { it.id to it.steps }

    fun doneSteps(r: Roadmap, p: Project) = p.steps.indices.count { "lab:${p.id}:$it" in r.studyDone }

    fun isDone(r: Roadmap, p: Project) =
        r.item(p.id)?.status == Status.DONE || doneSteps(r, p) == p.steps.size

    fun current(r: Roadmap): Project? = all.firstOrNull { !isDone(r, it) }

    fun levelColorKey(level: String) = when (level) { "Beginner" -> "green"; "Medium" -> "amber"; else -> "red" }
}
