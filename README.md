# StealthDefense 🛡️

> Minimalist Honeypot + Anti-Bot + Tarpit defense system for Termux, Proot, and PC.

![Java](https://img.shields.io/badge/Java-100%25-orange)
![Platform](https://img.shields.io/badge/Platform-Termux%20%7C%20Proot%20%7C%20PC-blue)
![License](https://img.shields.io/badge/License-GPL--3.0-green)

---

## 🎯 What is this?

**StealthDefense** is a lightweight, Java-based honeypot that:
- 🕵️ Detects port scanners & bots
- 🍯 Traps attackers in a tarpit
- 🚫 Auto-blacklists malicious IPs
- 📊 Logs to CSV & JSON

**Zero external dependencies.** Pure Java Standard Library.

---

## 🚀 Quick Start

```bash
git clone git@github.com:aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network

Flag Description
--i-own-this-network REQUIRED — Confirm ownership
--ports <list> Ports to listen
--threshold <n> Ports before blacklist
--window <ms> Time window
--delay <ms> Tarpit delay per byte
--max-duration <ms> Max tarpit duration
--max-tarpit <n> Max tarpit threads
--block-cmd <cmd> Firewall command (%IP% = target)

🧪 Examples

Basic:

```bash
java StealthDefense --i-own-this-network
```

Custom ports & threshold:

```bash
java StealthDefense --i-own-this-network --ports 80,443,8080 --threshold 3
```

With auto-block (root only):

```bash
java StealthDefense --i-own-this-network --block-cmd "iptables -A INPUT -s %IP% -j DROP"
```

Check logs:

```bash
cat stealth_log.csv
cat stealth_log.json
```

---

⚠️ Disclaimer

USE ONLY ON NETWORKS YOU OWN.
Unauthorized use may be illegal. This tool is for defensive purposes only.

---

🤖 Built With AI Collaboration

Role Contributor
💡 Idea & Concept Qwen 3.7 Plus
🏗️ Architecture & Core Logic Me (Human)
🐞 Debugging & Code Fixes DeepSeek V4.1 Pro
📝 README & Docs DeepSeek + Me

I didn't write every line myself. I used AI as a partner, not a replacement.
The idea, architecture, and final quality control are mine.
If you think that's "cheating", that's your problem. I'm proud of what I built. 🗿

---

📜 License

GPL-3.0 — see LICENSE

---

👤 Author

Aleksander (@aleksandergrambelik-png)

Built with ❤️ and too much coffee, from a phone. 📱☕

```
