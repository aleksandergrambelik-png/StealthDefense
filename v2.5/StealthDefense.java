import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.text.SimpleDateFormat;

public class StealthDefense {

    private static int[] PORTS = {2121, 2222, 3306, 8080, 33890};
    private static int THRESHOLD = 2;
    private static long WINDOW_MS = 5000;
    private static int DELAY_MS = 1500;
    private static int MAX_DURATION_MS = 120000;
    private static int MAX_TARPIT_THREADS = 50;
    private static String BLOCK_CMD = "";
    
    private static final String CSV_FILE = "stealth_log.csv";
    private static final String JSON_FILE = "stealth_log.json";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final long BOT_TIME_THRESHOLD_MS = 100;
    private static final String[] BAD_USER_AGENTS = {
        "python-requests", "curl", "wget", "masscan", "zgrab", 
        "nmap", "sqlmap", "nikto", "go-http-client", "libwww-perl",
        "java/", "okhttp", "axios", "postman"
    };

    private static final AtomicLong cConn = new AtomicLong(0);
    private static final AtomicLong cTarpit = new AtomicLong(0);
    private static final AtomicLong cThreat = new AtomicLong(0);
    private static final AtomicLong cBot = new AtomicLong(0);
    
    private static final Map<String, Long> ipFirstHit = new ConcurrentHashMap<>();
    private static final Map<String, Set<Integer>> ipPorts = new ConcurrentHashMap<>();
    private static final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    
    private static String ENV_MODE = "UNKNOWN";
    
    private static final String[] MYSQL_DISTROS = {
        "ubuntu0.16.04.1", "ubuntu0.20.04.1", "ubuntu0.22.04.1", 
        "debian0.10.1", "debian0.11.1", "debian0.12.1", 
        "parrot0.9.1", "parrot0.10.1", 
        "kali0.6.1", "alpine0.7.1", "arch0.1.1"
    };
    private static final String[] EBPF_VERSIONS = {
        "6.1.0", "6.2.0", "6.3.0", "6.4.0", "6.5.0"
    };
    private static final java.util.Random rand = new java.util.Random();

    public static void main(String[] args) {
        if (!parseArgs(args)) {
            printUsage();
            System.exit(1);
        }

        initLogFiles();
        detectEnvironment();
        printBanner();

        for (int p : PORTS) {
            new Thread(() -> listen(p), "Listener-" + p).start();
        }
        
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                    System.out.print("\r[STAT] Conn:" + cConn.get() + 
                                     " | Tarpit:" + cTarpit.get() + 
                                     " | Threat:" + cThreat.get() + 
                                     " | Bot:" + cBot.get() + "   ");
                } catch (Exception e) {}
            }
        }, "Stats").start();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3600000);
                    int sizeBefore = blacklist.size();
                    blacklist.clear();
                    System.out.println("\n[CLEANUP] Blacklist cleared. Before: " + sizeBefore);
                    logEvent("CLEANUP", "system", 0, "Blacklist cleared: " + sizeBefore + " IPs");
                } catch (Exception e) {}
            }
        }, "Cleanup").start();
    }

    private static void initLogFiles() {
        try {
            File csv = new File(CSV_FILE);
            if (!csv.exists()) {
                csv.createNewFile();
                writeToFile(CSV_FILE, "timestamp,event,ip,port,details\n");
            }
            
            File json = new File(JSON_FILE);
            if (!json.exists()) {
                json.createNewFile();
                writeToFile(JSON_FILE, "[\n");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to init log files: " + e.getMessage());
        }
    }

    private static synchronized void logEvent(String event, String ip, int port, String details) {
        String timestamp = DATE_FORMAT.format(new Date());
        
        String csvLine = String.format("%s,%s,%s,%d,\"%s\"\n", timestamp, event, ip, port, details);
        writeToFile(CSV_FILE, csvLine);
        
        String jsonLine = String.format(
            "  {\"timestamp\":\"%s\",\"event\":\"%s\",\"ip\":\"%s\",\"port\":%d,\"details\":\"%s\"},\n",
            timestamp, event, ip, port, details.replace("\"", "'")
        );
        writeToFile(JSON_FILE, jsonLine);
    }

    private static void writeToFile(String filename, String content) {
        try (FileWriter fw = new FileWriter(filename, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(content);
        } catch (Exception e) {}
    }

    private static boolean parseArgs(String[] args) {
        if (args.length == 0) return false;
        
        boolean hasPermission = false;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            try {
                switch (arg) {
                    case "--i-own-this-network":
                        hasPermission = true;
                        break;
                    case "--ports":
                        String[] p = args[++i].split(",");
                        PORTS = new int[p.length];
                        for (int j = 0; j < p.length; j++) PORTS[j] = Integer.parseInt(p[j].trim());
                        break;
                    case "--threshold":
                        THRESHOLD = Integer.parseInt(args[++i]);
                        break;
                    case "--window":
                        WINDOW_MS = Long.parseLong(args[++i]);
                        break;
                    case "--delay":
                        DELAY_MS = Integer.parseInt(args[++i]);
                        break;
                    case "--max-duration":
                        MAX_DURATION_MS = Integer.parseInt(args[++i]);
                        break;
                    case "--max-tarpit":
                        MAX_TARPIT_THREADS = Integer.parseInt(args[++i]);
                        break;
                    case "--block-cmd":
                        BLOCK_CMD = args[++i];
                        break;
                    case "--help":
                    case "-h":
                        return false;
                    default:
                        System.err.println("[ERROR] Unknown arg: " + arg);
                        return false;
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Invalid value for " + arg);
                return false;
            }
        }
        
        if (!hasPermission) {
            System.err.println("[SECURITY] You must confirm ownership with --i-own-this-network");
            return false;
        }
        
        return true;
    }

    private static void printUsage() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         StealthDefense v2.5 (Universal Edition)      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java StealthDefense --i-own-this-network [OPTIONS]");
        System.out.println();
        System.out.println("REQUIRED:");
        System.out.println("  --i-own-this-network    Confirm you own the network");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  --ports <list>          Ports to listen");
        System.out.println("  --threshold <n>         Ports before blacklist");
        System.out.println("  --window <ms>           Time window");
        System.out.println("  --delay <ms>            Tarpit delay per byte");
        System.out.println("  --max-duration <ms>     Max tarpit duration");
        System.out.println("  --max-tarpit <n>        Max tarpit threads");
        System.out.println("  --block-cmd <cmd>       Firewall command (%IP% = target)");
        System.out.println();
        System.out.println("OUTPUT:");
        System.out.println("  - stealth_log.csv       Log in CSV format");
        System.out.println("  - stealth_log.json      Log in JSON format");
        System.out.println();
        System.out.println("PLUGINS:");
        System.out.println("  - clickjack_filter      Active (HTTP header + CSS injection)");
        System.out.println();
    }

    private static void detectEnvironment() {
        String os = System.getProperty("os.name").toLowerCase();
        String termux = System.getenv("TERMUX_VERSION");
        String prefix = System.getenv("PREFIX");
        
        if (termux != null || (prefix != null && prefix.contains("com.termux"))) {
            ENV_MODE = "TERMUX (non-root)";
        } else if (os.contains("linux") && new File("/.dockerenv").exists()) {
            ENV_MODE = "DOCKER/CONTAINER";
        } else if (os.contains("linux") && isProot()) {
            ENV_MODE = "PROOT (emulated)";
        } else if (os.contains("linux")) {
            ENV_MODE = "LINUX (native)";
        } else if (os.contains("win")) {
            ENV_MODE = "WINDOWS";
        } else if (os.contains("mac")) {
            ENV_MODE = "MACOS";
        } else {
            ENV_MODE = "UNKNOWN";
        }
    }

    private static boolean isProot() {
        try {
            String status = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("/proc/self/status")));
            return status.contains("proot") || System.getenv("PROOT_TMP_DIR") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         StealthDefense v2.5 (Universal Edition)      ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Environment : " + padRight(ENV_MODE, 36) + "║");
        System.out.println("║  Ports       : " + padRight(Arrays.toString(PORTS), 36) + "║");
        System.out.println("║  Telemetry   : " + padRight("DISABLED (local only)", 36) + "║");
        System.out.println("║  Log Output  : " + padRight("CSV + JSON", 36) + "║");
        System.out.println("║  Plugins     : " + padRight("Clickjack Filter", 36) + "║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static void listen(int port) {
        try {
            ServerSocket ss = new ServerSocket(port);
            ss.setSoTimeout(0);
            System.out.println("[LISTEN] Port " + port + " active.");
            while (true) {
                try {
                    Socket s = ss.accept();
                    cConn.incrementAndGet();
                    logEvent("CONN", s.getInetAddress().getHostAddress(), port, "New connection");
                    new Thread(() -> process(s, port), "Handler-" + port).start();
                } catch (SocketTimeoutException e) {
                } catch (Exception e) {
                    System.err.println("[ERROR] Accept failed on port " + port + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[FATAL] Cannot bind port " + port + ": " + e.getMessage());
        }
    }

    private static void process(Socket s, int port) {
        String ip = s.getInetAddress().getHostAddress();
        
        if (blacklist.contains(ip)) {
            tarpit(s, ip);
            return;
        }

        boolean isThreat = false;
        long now = System.currentTimeMillis();
        
        if (detectBot(s, port)) {
            isThreat = true;
            cBot.incrementAndGet();
            System.out.println("\n[BOT] " + ip + " flagged as bot!");
            logEvent("BOT", ip, port, "Bot signature detected");
            blacklist.add(ip);
            cThreat.incrementAndGet();
            tarpit(s, ip);
            return;
        }

        ipPorts.putIfAbsent(ip, ConcurrentHashMap.newKeySet());
        ipFirstHit.putIfAbsent(ip, now);
        
        if (now - ipFirstHit.get(ip) > WINDOW_MS) {
            ipPorts.get(ip).clear();
            ipFirstHit.put(ip, now);
        }
        
        ipPorts.get(ip).add(port);
        
        if (ipPorts.get(ip).size() >= THRESHOLD) {
            isThreat = true;
            blacklist.add(ip);
            cThreat.incrementAndGet();
            System.out.println("\n[THREAT] " + ip + " scanned " + ipPorts.get(ip).size() + " ports!");
            logEvent("THREAT", ip, port, "Port scan detected: " + ipPorts.get(ip).size() + " ports");
            
            if (!BLOCK_CMD.isEmpty()) {
                try { 
                    Runtime.getRuntime().exec(BLOCK_CMD.replace("%IP%", ip)); 
                } catch (Exception e) {}
            }
        }

        if (isThreat) {
            tarpit(s, ip);
        } else {
            try {
                s.getOutputStream().write(getBanner(port).getBytes());
                s.getOutputStream().flush();
                s.close();
            } catch (Exception e) {}
        }
    }

    private static boolean detectBot(Socket s, int port) {
        try {
            long startTime = System.currentTimeMillis();
            
            if (port == 8080) {
                s.setSoTimeout(2000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line;
                boolean isBadUA = false;
                
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("user-agent:")) {
                        String ua = line.substring(11).trim().toLowerCase();
                        for (String bad : BAD_USER_AGENTS) {
                            if (ua.contains(bad)) {
                                isBadUA = true;
                                break;
                            }
                        }
                    }
                }
                
                long duration = System.currentTimeMillis() - startTime;
                if (isBadUA || duration < BOT_TIME_THRESHOLD_MS) {
                    return true;
                }
            } else {
                s.setSoTimeout(500);
                InputStream in = s.getInputStream();
                if (in.available() > 0) {
                    return true;
                }
            }
        } catch (SocketTimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static void tarpit(Socket s, String ip) {
        if (cTarpit.get() >= MAX_TARPIT_THREADS) {
            try { s.close(); } catch (Exception e) {}
            return;
        }

        cTarpit.incrementAndGet();
        logEvent("TARPIT", ip, s.getLocalPort(), "Tarpit engaged");
        
        try {
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            out.write(getBanner(s.getLocalPort()).getBytes());
            out.flush();
            
            long start = System.currentTimeMillis();
            byte[] buf = new byte[1];
            
            while (true) {
                if (System.currentTimeMillis() - start > MAX_DURATION_MS) break;
                if (in.read(buf) == -1) break;
                out.write('.');
                out.flush();
                Thread.sleep(DELAY_MS);
            }
        } catch (Exception e) {
        } finally {
            try { s.close(); } catch (Exception e) {}
            cTarpit.decrementAndGet();
        }
    }

    private static String injectClickjackingProtection(String htmlContent) {
        String headers = "X-Frame-Options: DENY\r\n" +
                         "Content-Security-Policy: frame-ancestors 'none'\r\n";
        
        String cssInjection = "<style>\n" +
                              "  html { display: none !important; }\n" +
                              "  body { display: none !important; }\n" +
                              "</style>\n";
        
        return headers + cssInjection + htmlContent;
    }

    private static String getBanner(int p) {
        if (p == 2121) return "220 (vsFTPd 3.0.3)\r\n";
        
        if (p == 2222) {
            String distro = MYSQL_DISTROS[rand.nextInt(MYSQL_DISTROS.length)].split("0")[0];
            return "SSH-2.0-OpenSSH_7.2p2 " + distro + "\r\n";
        }
        
        if (p == 3306) {
            String distro = MYSQL_DISTROS[rand.nextInt(MYSQL_DISTROS.length)];
            String ebpf = EBPF_VERSIONS[rand.nextInt(EBPF_VERSIONS.length)];
            return "5.7.32-" + distro + "-ebpf-" + ebpf + "\u0000";
        }
        
        if (p == 8080) {
            String ebpf = EBPF_VERSIONS[rand.nextInt(EBPF_VERSIONS.length)];
            
            String fakeHtml = "<html><head><title>Router Login</title></head>" +
                              "<body style='font-family: sans-serif; text-align: center; padding-top: 50px;'>" +
                              "<h1>401 Unauthorized</h1>" +
                              "<p>Please login to access the router configuration.</p>" +
                              "<form>" +
                              "<input type='text' placeholder='Username' style='margin: 5px; padding: 8px;'><br>" +
                              "<input type='password' placeholder='Password' style='margin: 5px; padding: 8px;'><br>" +
                              "<button type='submit' style='margin: 5px; padding: 8px 20px;'>Login</button>" +
                              "</form></body></html>";
            
            String protectedHtml = injectClickjackingProtection(fakeHtml);
            
            return "HTTP/1.1 200 OK\r\n" +
                   "Server: Microsoft-IIS/10.0\r\n" +
                   "X-Powered-By: eBPF/" + ebpf + "\r\n" +
                   "X-Protection: Active\r\n" +
                   protectedHtml; 
        }
        
        return "220 Service Ready\r\n";
    }
}
