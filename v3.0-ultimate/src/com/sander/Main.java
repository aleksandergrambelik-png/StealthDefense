package com.sander;

import com.sander.core.SanderCore;
import com.sander.modules.StealthDefenseAI;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        SanderCore ai = new SanderCore();
        ai.loadState();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\033[2J\033[H");
            System.out.println("\033[1;36m=== SANDER CORE v1.3: CYBERPUNK HUD ===\033[0m");
            System.out.println("  SANDER ULTIMATE - HYBRID ARSENAL");
            System.out.println("==========================================");
            System.out.println(" 1. StealthDefense v3.0 (AI Honeypot)");
            System.out.println(" 2. System Status (Animated HUD)");
            System.out.println(" 0. Exit");
            System.out.println("------------------------------------------");
            System.out.print(" Enter choice (0-2): ");
            
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("0")) {
                System.out.println("\033[1;31m[*] Exiting Arsenal...\033[0m");
                System.exit(0); 
            }
            
            if (choice.equals("2")) {
                showAnimatedHUD(ai);
                System.out.print("\nPress Enter to return...");
                scanner.nextLine();
            } else if (choice.equals("1")) {
                System.out.println("\033[1;33m[!] StealthDefense v3.0 engine is loading...\033[0m");
                try {
                    Thread engineThread = new Thread(() -> {
                        try {
                            StealthDefenseAI.start();
                        } catch (Exception e) {
                            System.out.println("\033[1;31m[!] StealthDefenseAI failed: " + e.getMessage() + "\033[0m");
                        }
                    });
                    engineThread.start();
                    
                    System.out.println("\033[1;32m[+] StealthDefense is running in background.\033[0m");
                    System.out.println("\033[1;33m[!] Press Enter to STOP the engine and return to menu...\033[0m");
                    scanner.nextLine();
                    
                    StealthDefenseAI.stop();
                    Thread.sleep(1000); 
                    
                } catch (Exception e) {
                    System.out.println("\033[1;31m[!] Failed to load StealthDefenseAI.\033[0m");
                }
            } else {
                System.out.println("\033[1;31m[!] Invalid choice.\033[0m");
            }
        }
    }
    
    private static void showAnimatedHUD(SanderCore ai) {
        try {
            System.out.println("\n\033[1;33m[*] Menunggu data Hardware...\033[0m");
            Thread.sleep(1500);

            for (int i = 1; i <= 15; i++) { 
                ai.syncWithHardware();
                
                int threat;
                if (i >= 5 && i <= 9) threat = 85; 
                else threat = i * 5;

                String action = ai.getDecision(threat);
                int stress = ai.getStressLevel();
                
                System.out.print("\r"); 
                System.out.printf("\033[1;36m[CYCLE %02d]\033[0m Threat:%-3d | Stress:%s%d/%d | HW:%d%% | Decision: ", 
                    i, 
                    threat, 
                    renderStressBar(stress, SanderCore.MAX_STRESS),
                    stress, SanderCore.MAX_STRESS,
                    ai.getHwBattery());
                
                if (action.contains("LOCKDOWN")) {
                    System.out.print("\033[5;1;31m" + action + "\033[0m");
                } else {
                    System.out.print("\033[1;32m" + action + "\033[0m");
                }
                
                System.out.flush(); 
                Thread.sleep(1000); 
            }
            System.out.println("\n\n\033[1;32m[SYSTEM] Simulation ended. State saved to disk.\033[0m");
        } catch (Exception e) {}
    }
    
    private static String renderStressBar(int current, int max) {
        StringBuilder bar = new StringBuilder("[");
        int filled = (current * 10) / max; 
        for (int i = 0; i < 10; i++) {
            if (i < filled) bar.append("█");
            else bar.append("░");
        }
        bar.append("] ");
        if (current >= max) return "\033[1;31m" + bar.toString() + "\033[0m";
        if (current >= max * 0.7) return "\033[1;33m" + bar.toString() + "\033[0m";
        return "\033[1;32m" + bar.toString() + "\033[0m";
    }
}
