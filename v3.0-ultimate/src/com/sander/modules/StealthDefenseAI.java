package com.sander.modules;

import com.sander.core.SanderCore;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class StealthDefenseAI {
    private static final int[] PORTS = {2121, 2222, 3306, 8080, 33890};
    private static final int DELAY_MS = 1500;
    private static final int MAX_DURATION_MS = 120000;
    private static final int MAX_TARPIT_THREADS = 50;
    
    private static final String JSON_FILE = "stealth_ai.jsonl";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final long BOT_TIME_THRESHOLD_MS = 100;
    private static final String[] BAD_USER_AGENTS = {
        "python-requests", "curl", "wget", "masscan", "zgrab", 
        "nmap", "sqlmap", "nikto", "go-http-client", "libwww-perl",
        "java/", "okhttp", "axios", "postman"
    };

    private static SanderCore aiBrain;

    private static final Pattern SQLI_PATTERN = Pattern.compile("(?i)(union\\s+select|select\\s+.*\\s+from|'.*or.*'=')");
    private static final Pattern XSS_PATTERN = Pattern.compile("(?i)(<script|javascript:|onerror\\s*=|onload\\s*=)");
    private static final Pattern CMD_INJ_PATTERN = Pattern.compile("(?i)(;|\\||&&|\\$\\(|`)");

    private static final AtomicLong cConn = new AtomicLong(0);
    private static final AtomicLong cTarpit = new AtomicLong(0);
    private static final AtomicLong cThreat = new AtomicLong(0);
    private static final AtomicLong cBot = new AtomicLong(0);
    
    private static final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    private static final java.util.Random rand = new java.util.Random();

    private static volatile boolean running = true;

    public static void start() {
        running = true;
        aiBrain = new SanderCore();
        aiBrain.loadState();
        
        System.out.println("=== STEALTHDEFENSE v5.0 (AI INTEGRATED) ===");
        System.out.println("[CORE] AI Brain Online. Battery: " + aiBrain.getHwBattery() + "%");
        
        new Thread(() -> {
            while (running) {
                try { aiBrain.syncWithHardware(); Thread.sleep(1000); } catch (Exception e) {}
            }
        }, "HardwareSync").start();

        for (int p : PORTS) {
            new Thread(() -> listen(p), "Listener-" + p).start();
        }
        
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(3000);
                    System.out.println("\n[STAT] Conn:" + cConn.get() + 
                                     " | Tarpit:" + cTarpit.get() + 
                                     " | Threat:" + cThreat.get() + 
                                     " | Bot:" + cBot.get() + 
                                     " | Stress:" + aiBrain.getStressLevel() + "/" + SanderCore.MAX_STRESS +
                                     " | Batt:" + aiBrain.getHwBattery() + "%");
                } catch (Exception e) {}
            }
        }, "Stats").start();
    }

    public static void stop() {
        running = false;
    }

    private static void listen(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[LISTEN] Port " + port + " active.");
            while (running) {
                Socket s = ss.accept();
                cConn.incrementAndGet();
                String ip = s.getInetAddress().getHostAddress();
                
                if (blacklist.contains(ip)) {
                    logEvent("REHIT_BLACKLIST", ip, port, "Blocked immediately");
                    try { s.close(); } catch(Exception e){}
                    continue;
                }
                new Thread(() -> process(s, port), "Handler-" + port).start();
            }
        } catch (Exception e) {
            if (running) System.err.println("[FATAL] Port " + port + " failed: " + e.getMessage());
        }
    }

    private static void process(Socket s, int port) {
        String ip = s.getInetAddress().getHostAddress();
        try {
            s.setSoTimeout(2000);
            InputStream in = s.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead = in.read(buffer);
            
            if (bytesRead == -1) return;
            
            String requestData = new String(buffer, 0, bytesRead);
            
            int threatLevel = 0;
            String attackType = "UNKNOWN";
            boolean isSuspicious = false;

            if (CMD_INJ_PATTERN.matcher(requestData).find()) {
                threatLevel += 90; attackType = "CMD_INJECTION"; isSuspicious = true;
            } else if (SQLI_PATTERN.matcher(requestData).find()) {
                threatLevel += 80; attackType = "SQL_INJECTION"; isSuspicious = true;
            } else if (XSS_PATTERN.matcher(requestData).find()) {
                threatLevel += 70; attackType = "XSS"; isSuspicious = true;
            }

            for (String bad : BAD_USER_AGENTS) {
                if (requestData.toLowerCase().contains(bad)) {
                    threatLevel += 50; attackType = "BAD_USER_AGENT"; isSuspicious = true; break;
                }
            }

            String aiDecision = aiBrain.getDecision(threatLevel);
            
            if (aiDecision.contains("TOTAL_LOCKDOWN")) {
                cThreat.incrementAndGet();
                System.out.println("\n[AI OVERRIDE] Total Lockdown! IP " + ip + " blocked permanently.");
                logEvent("AI_LOCKDOWN", ip, port, attackType + " -> " + aiDecision);
                blacklist.add(ip);
                tarpit(s, ip);
            } 
            else if (isSuspicious || aiDecision.contains("TARPIT")) {
                cBot.incrementAndGet();
                System.out.println("\n[AI DECISION] " + aiDecision + " -> Trapping IP " + ip);
                logEvent("AI_TARPIT", ip, port, attackType + " -> " + aiDecision);
                blacklist.add(ip);
                tarpit(s, ip);
            } 
            else {
                sendBanner(s, port);
            }

        } catch (SocketTimeoutException e) {
        } catch (Exception e) {
        } finally {
            try { s.close(); } catch (Exception e) {}
        }
    }

    private static void tarpit(Socket s, String ip) {
        if (cTarpit.get() >= MAX_TARPIT_THREADS) {
            try { s.close(); } catch (Exception e) {}
            return;
        }
        cTarpit.incrementAndGet();
        logEvent("TARPIT", ip, s.getLocalPort(), "Engaged");
        try {
            OutputStream out = s.getOutputStream();
            out.write("220 (SanderTrap Tarpit v5.0 - AI Protected)\r\n".getBytes());
            out.flush();
            long start = System.currentTimeMillis();
            byte[] buf = new byte[1];
            while (s.getInputStream().read(buf) != -1) {
                if (System.currentTimeMillis() - start > MAX_DURATION_MS) break;
                out.write('.'); out.flush();
                Thread.sleep(DELAY_MS);
            }
        } catch (Exception e) {
        } finally {
            try { s.close(); } catch (Exception e) {}
            cTarpit.decrementAndGet();
        }
    }

    private static void sendBanner(Socket s, int port) {
        try {
            String banner = "";
            if (port == 2222) banner = "SSH-2.0-OpenSSH_" + rand.nextInt(9) + ".0\r\n";
            else if (port == 8080) banner = "HTTP/1.1 200 OK\r\nServer: Fake\r\n\r\n<html>Login</html>";
            else banner = "220 Service Ready\r\n";
            s.getOutputStream().write(banner.getBytes());
            s.getOutputStream().flush();
        } catch (Exception e) {}
    }

    private static synchronized void logEvent(String event, String ip, int port, String details) {
        String time = DATE_FORMAT.format(new Date());
        try (FileWriter fw = new FileWriter(JSON_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            String json = String.format("{\"timestamp\":\"%s\",\"event\":\"%s\",\"ip\":\"%s\",\"port\":%d,\"details\":\"%s\"}\n",
                    time, event, ip, port, details.replace("\"", "'"));
            bw.write(json);
        } catch (Exception e) {}
    }
}
