package com.sander.core;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;

public class SanderCore {
    private final AtomicLong state;
    private static final long MULTIPLIER = 6364136223846793005L;
    private static final long INCREMENT  = 1442695040888963407L;
    private static final int ROTATE_DIST = 13;
    private volatile int hwBattery = 100;
    private long lastProbeTime = 0;
    private static final long PROBE_INTERVAL_MS = 3000;
    private final String storagePath = System.getProperty("user.home") + "/sander_core.dat";
    private volatile int stressLevel = 0;
    public static final int MAX_STRESS = 5;

    public SanderCore() { this.state = new AtomicLong(0); }
    public int getStressLevel() { return stressLevel; }
    public int getHwBattery() { return hwBattery; }

    public void loadState() {
        try {
            if (Files.exists(Paths.get(storagePath))) {
                long loaded = Long.parseLong(Files.readString(Paths.get(storagePath)).trim());
                state.set(loaded);
                System.out.println("[MEMORY] Consciousness restored from disk.");
            } else {
                state.set(System.nanoTime());
                saveState();
                System.out.println("[MEMORY] New brain initialized (First Birth).");
            }
        } catch (Exception e) {
            state.set(System.nanoTime());
            System.err.println("[WARN] Memory corrupted. Reborn.");
        }
    }

    public void saveState() {
        try { Files.writeString(Paths.get(storagePath), String.valueOf(state.get())); } catch (Exception e) {}
    }

    public void syncWithHardware() {
        long now = System.currentTimeMillis();
        if (now - lastProbeTime < PROBE_INTERVAL_MS) return; 
        try {
            ProcessBuilder pbApi = new ProcessBuilder("termux-battery-status");
            pbApi.redirectErrorStream(true);
            Process pApi = pbApi.start();
            ProcessBuilder pbPerl = new ProcessBuilder("perl", "-e", 
                "my $j = do { local $/; <STDIN> }; " +
                "print ($j =~ /\"percentage\"\\s*:\\s*(\\d+)/) ? $1 : 'FAIL';");
            pbPerl.redirectErrorStream(true);
            Process pPerl = pbPerl.start();
            Thread pipeThread = new Thread(() -> {
                try (InputStream in = pApi.getInputStream(); OutputStream out = pPerl.getOutputStream()) { in.transferTo(out); } catch (IOException e) {}
            });
            pipeThread.start();
            String result;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pPerl.getInputStream()))) { result = reader.readLine(); }
            pipeThread.join(); pApi.waitFor(); pPerl.waitFor();   
            if (result != null && !result.equals("FAIL")) { hwBattery = Integer.parseInt(result.trim()); lastProbeTime = now; }
        } catch (Exception e) {}
    }

    private long nextValue() {
        return state.updateAndGet(s -> {
            long v = s * MULTIPLIER + INCREMENT;
            if (hwBattery >= 20) { v = (v << ROTATE_DIST) | (v >>> (64 - ROTATE_DIST)); v ^= hwBattery; }
            return v;
        });
    }

    public String getDecision(int threatLevel) {
        long raw = nextValue();
        int entropy = (int) ((raw & Long.MAX_VALUE) % 100); 
        if (threatLevel >= 70 || hwBattery < 15) { stressLevel = Math.min(stressLevel + 1, MAX_STRESS); } else { stressLevel = Math.max(stressLevel - 1, 0); }
        if (stressLevel >= MAX_STRESS) return "TOTAL_LOCKDOWN [STRESS_OVERRIDE]"; 
        if (threatLevel >= 80 || hwBattery < 10) { return ((entropy & 1) == 0) ? "TOTAL_LOCKDOWN" : "AGGRESSIVE_SCAN"; } 
        else {
            switch(entropy % 4) {
                case 0: return "DEFENSIVE_TARPIT"; case 1: return "DECOY_RESPONSE";
                case 2: return "ADAPTIVE_EVASION"; case 3: return "STEALTH_PROBE"; default: return "STANDBY";
            }
        }
    }

    public static class TerminalFX {
        public static final String RED = "\033[1;31m"; public static final String GREEN = "\033[1;32m";
        public static final String YELLOW = "\033[1;33m"; public static final String CYAN = "\033[1;36m"; public static final String RESET = "\033[0m";
        public static String renderStressBar(int current, int max) {
            StringBuilder bar = new StringBuilder("[");
            int filled = (current * 10) / max; 
            for (int i = 0; i < 10; i++) { if (i < filled) bar.append("█"); else bar.append("░"); }
            bar.append("] ");
            if (current >= max) return RED + bar.toString() + RESET;
            if (current >= max * 0.7) return YELLOW + bar.toString() + RESET;
            return GREEN + bar.toString() + RESET;
        }
    }
}
