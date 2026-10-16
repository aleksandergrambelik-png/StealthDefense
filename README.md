# StealthDefense 🛡️

> Minimalist Honeypot + Anti-Bot + Tarpit defense system for Termux, Proot, and PC.

![Java](https://img.shields.io/badge/Java-100%25-orange)
![Platform](https://img.shields.io/badge/Platform-Termux%20%7C%20Proot%20%7C%20PC-blue)
![License](https://img.shields.io/badge/License-GPL--3.0-green)

---

## 🎯 What is this?

StealthDefense is a lightweight Java honeypot designed to:
- Detect port scanners and automated bots
- Trap attackers in a tarpit
- Auto-blacklist malicious IPs
- Log activity to CSV and JSON

Zero external dependencies — pure Java Standard Library.

---

## 🚀 Quick Start

Clone, compile, and run:

```bash
git clone git@github.com:aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

Required flag:
- --i-own-this-network — Confirm you own the target network (REQUIRED)

Available options:
- --ports <list>         Comma-separated ports to listen on (e.g., 22,80,8080)
- --threshold <n>        Number of distinct ports accessed before blacklisting
- --window <ms>          Time window in milliseconds for threshold counting
- --delay <ms>           Tarpit delay per byte sent back to the client
- --max-duration <ms>    Maximum duration for a single tarpit session
- --max-tarpit <n>       Maximum number of concurrent tarpit threads
- --block-cmd <cmd>      Firewall command to run when blocking an IP (use %IP% placeholder)

Examples:

Basic:
```bash
java StealthDefense --i-own-this-network
```

Custom ports & threshold:
```bash
java StealthDefense --i-own-this-network --ports 80,443,8080 --threshold 3
```

With auto-block (requires root):
```bash
java StealthDefense --i-own-this-network --block-cmd "iptables -A INPUT -s %IP% -j DROP"
```

Check logs:
```bash
cat stealth_log.csv
cat stealth_log.json
```

---

## ⚙️ How it works (high level)

- Listens on configured ports.
- Tracks incoming connection patterns per source IP.
- If an IP scans multiple ports within the configured window and exceeds the threshold, the IP is tar-pitted and optionally blocked.
- Events are recorded to CSV and JSON for analysis.

---

## ⚠️ Disclaimer

Use only on networks you own or are authorized to test. Unauthorized use may be illegal. This tool is for defensive and research purposes only.

---

## 🤖 Built With AI Collaboration

Role | Contributor
---|---
Idea & Concept | Qwen 3.7 Plus
Architecture & Core Logic | Aleksander (human)
Debugging & Code Fixes | DeepSeek V4.1 Pro
README & Docs | DeepSeek, Aleksander, GitHub Copilot

Note: AI tools were used as assistants. Final design and quality control were performed by the human author.

---

## 📜 License

GPL-3.0 — see LICENSE

---

## 👤 Author

Aleksander (@aleksandergrambelik-png)

Built with ❤️ and too much coffee, from a phone. 📱☕
