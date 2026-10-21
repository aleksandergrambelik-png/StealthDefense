# StealthDefense

![Java](https://img.shields.io/badge/Java-100%25-orange)
![Platform](https://img.shields.io/badge/Platform-Termux%20%7C%20Proot%20%7C%20PC-blue)
![License](https://img.shields.io/badge/License-GPL--3.0-green)

> Minimalist Honeypot + Anti-Bot + Tarpit defense for Termux/PC. Built-in Anti-Clickjacking & Barcode Guard (SQLi/XSS/LFI filters). Zero dependencies.

---

## Termux Installation

For Android and Termux users, install the required tools and clone the project first:

```bash
pkg update && pkg install git openjdk-17 -y
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

If SSH cloning fails in Termux, use HTTPS instead:

```bash
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

This section is intended for Termux and mobile testing environments.

---

## SanderUltimate v3.0 (Next Generation)

The next-generation honeypot is now available in the `v3.0-ultimate/` folder. It combines AI-driven decision making (`SanderCore`), real-time hardware probing, and a cyberpunk-style HUD.

### How to Run v3.0

#### 1) Clone the repository
```bash
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
```

#### 2) Open the v3.0 folder
```bash
cd v3.0-ultimate
```

#### 3) Install dependencies (Termux only)
```bash
pkg install openjdk-17 perl termux-api -y
```

#### 4) Build the project
```bash
chmod +x build.sh
./build.sh
```

#### 5) Run the honeypot
```bash
java -jar SanderUltimate.jar
```

### Requirements
- Java 17+
- Termux / Linux / WSL / PC environment
- Basic shell access

---

## Overview

StealthDefense is a lightweight Java honeypot that:
- Detects port scanners and automated bots
- Traps attackers in a tarpit to slow them down
- Auto-blacklists malicious IPs
- Logs activity to CSV and JSON for analysis

Zero external dependencies — only the Java Standard Library.

---

## Quick Start

Clone, compile, and run:

```bash
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

Required flag:
- `--i-own-this-network` — Confirm you own or are authorized to test the target network (REQUIRED).

Suggested defaults (if applicable):
- ports=22,80,443
- threshold=3
- window=60000
- delay=50
- max-duration=300000
- max-tarpit=10

---

## Options

Flag | Description
---|---
`--i-own-this-network` | REQUIRED — Confirm ownership/authorization
`--ports <list>` | Comma-separated ports to listen on (e.g., `22,80,8080`)
`--threshold <n>` | Number of distinct ports accessed before blacklisting
`--window <ms>` | Time window (ms) for threshold counting
`--delay <ms>` | Tarpit delay per byte sent back to the client (ms)
`--max-duration <ms>` | Max duration for a single tarpit session (ms)
`--max-tarpit <n>` | Max concurrent tarpit threads
`--block-cmd <cmd>` | Firewall command to run when blocking an IP (use `%IP%` placeholder)

---

## Examples

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
# iptables (Linux)
java StealthDefense --i-own-this-network --block-cmd "iptables -A INPUT -s %IP% -j DROP"
```

Example nftables command:
```bash
java StealthDefense --i-own-this-network --block-cmd "nft add rule inet filter input ip saddr %IP% drop"
```

Windows placeholder (requires admin & appropriate firewall command):
```powershell
java StealthDefense --i-own-this-network --block-cmd "powershell -Command \"New-NetFirewallRule -RemoteAddress %IP% -Action Block -DisplayName 'StealthDefense'\""
```

---

## Plugins

StealthDefense v2.6 includes built-in plugins that activate automatically.

| Plugin | Status | Description |
|--------|--------:|-------------|
| `clickjack_filter` | Active | Protects against clickjacking via HTTP headers + CSS injection. |
| `barcode_guard`   | Active | Filters malicious payloads (SQLi, XSS, LFI) using regex pattern matching. |

### How to Test Clickjacking Protection

1. Run StealthDefense:
```bash
java StealthDefense --i-own-this-network
```

2. Check the HTTP headers:
```bash
curl -I http://127.0.0.1:8080
```
Expected output includes:
```
X-Frame-Options: DENY
Content-Security-Policy: frame-ancestors 'none'
```

3. Visual test (open in browser):

Create an HTML file:
```html
<!doctype html>
<html>
  <body>
    <h1>Clickjacking Test</h1>
    <iframe src="http://127.0.0.1:8080" width="600" height="400"></iframe>
  </body>
</html>
```
If the iframe is blank/invisible, the protection is working.

### How to Test Barcode Guard (SQLi / XSS / LFI)

1. Run StealthDefense:
```bash
java StealthDefense --i-own-this-network
```

2. Test SQL Injection:
```bash
curl "http://127.0.0.1:8080/?id=1'%20OR%20'1'='1"
```
Expected log:
```
[PAYLOAD] Malicious payload detected from 127.0.0.1
```

3. Test XSS:
```bash
curl "http://127.0.0.1:8080/?q=<script>alert('xss')</script>"
```
Expected log:
```
[PAYLOAD] Malicious payload detected from 127.0.0.1
```

4. Test LFI:
```bash
curl "http://127.0.0.1:8080/?file=../../etc/passwd"
```
Expected log:
```
[PAYLOAD] Malicious payload detected from 127.0.0.1
```

Note: Barcode Guard uses regex patterns to match known attack signatures. If a payload bypasses the filter, it may still be caught by Bot Detection (User-Agent + timing) or the Tarpit mechanism.

---

## Platforms & Installation

Requirements:
- Java JDK 11+ (Windows/macOS) or OpenJDK (Linux)
- Git

Linux / macOS:
```bash
sudo apt update && sudo apt install default-jdk git -y
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

Windows (CMD / PowerShell):
1. Install Java from Adoptium or Oracle and ensure `javac` is on PATH.
2. In CMD/PowerShell:
```cmd
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

Windows (WSL — recommended for devs):
```powershell
# In PowerShell (install WSL)
wsl --install
# Then in WSL:
sudo apt update && sudo apt install default-jdk git -y
git clone https://github.com/aleksandergrambelik-png/StealthDefense.git
cd StealthDefense
javac StealthDefense.java
java StealthDefense --i-own-this-network
```

---

## Logs

Output files:
- `stealth_log.csv` — CSV log of events
- `stealth_log.json` — JSON log for structured analysis

Quick check:
```bash
head -n 20 stealth_log.csv
jq '.' stealth_log.json | less
```

---

## Troubleshooting

- Permission errors when using `--block-cmd`: run with root/admin or use a non-root test command.
- Port already in use: ensure no service binds to the configured ports (`ss -ltnp` / `netstat -ano`).
- Java version: ensure `java -version` reports 11+.

---

## How it works (high level)

- Listens concurrently on configured ports.
- Tracks connection patterns per source IP over the specified time window.
- If an IP connects to multiple distinct ports within the window and exceeds the threshold, it is tar-pitted and optionally blocked using `--block-cmd`.
- Events are recorded to CSV/JSON for later analysis.

---

## Responsible Use & Disclaimer

Use only on networks you own or are authorized to test. Unauthorized use may be illegal. This tool is for defensive research and testing only.

If you find a security issue, follow responsible disclosure: contact the repository owner or open an issue (do not post exploit details publicly).

---

## Contributing

Contributions, bug reports, and PRs are welcome. Please:
1. Fork the repo
2. Create a feature branch
3. Open a PR with a clear description and test instructions

---

## Built With AI Collaboration

Role | Contributor
---|---
Idea & Concept | Qwen 3.7 Plus
Architecture & Core Logic | Aleksander (human)
Debugging & Code Fixes | DeepSeek V4.1 Pro
README & Docs | DeepSeek, Aleksander, GitHub Copilot

AI tools assisted; final design and QA by the human author.

---

## License

GPL-3.0 — see LICENSE

---

## Author

Aleksander (@aleksandergrambelik-png)

Built with love and too much coffee, from a phone.

## Version History

| Version | Path | Features |
|---------|------|----------|
| **v2.6** (latest) | `v2.6/StealthDefense.java` | Barcode Guard (SQLi/XSS/LFI Filter) |
| **v2.5** | `v2.5/StealthDefense.java` | Clickjacking Protection |
| **v2.4** | `v2.4/StealthDefense.java` | Stable, no plugins |

> The root `StealthDefense.java` is a copy of the latest version for easy compilation.
