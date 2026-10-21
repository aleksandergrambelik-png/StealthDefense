package com.sander;

import com.sander.core.SanderCore;
import com.sander.core.TerminalFX;
import com.sander.modules.StealthDefenseAI;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        SanderCore ai = new SanderCore();
        ai.loadState();
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print(TerminalFX.CLEAR);
            TerminalFX.printHeader();
            System.out.println("  SANDER ULTIMATE - HYBRID ARSENAL");
            System.out.println("==========================================");
            System.out.println(" 1. StealthDefense v5 (AI Honeypot)");
            System.out.println(" 2. System Status (Battery & Stress)");
            System.out.println(" 0. Exit");
            System.out.println("------------------------------------------");
            System.out.print(" Enter choice (0-2): ");
            
            String choice = scanner.nextLine().trim();
            
            if (choice.equals("0")) {
                System.out.println(TerminalFX.RED + "[*] Exiting Arsenal." + TerminalFX.RESET);
                break;
            }
            
            switch (choice) {
                case "1":
                    System.out.println(TerminalFX.CYAN + "[*] Launching StealthDefense v5..." + TerminalFX.RESET);
                    try {
                        StealthDefenseAI.start();
                    } catch (Exception e) {
                        System.out.println(TerminalFX.RED + "[!] Failed: " + e.getMessage() + TerminalFX.RESET);
                    }
                    break;
                case "2":
                    System.out.println("\n" + TerminalFX.YELLOW + "[SYSTEM STATUS]" + TerminalFX.RESET);
                    System.out.println(" Battery : " + ai.getHwBattery() + "%");
                    System.out.println(" Stress  : " + TerminalFX.renderStressBar(ai.getStressLevel(), SanderCore.MAX_STRESS));
                    System.out.println(" Decision: " + ai.getDecision(ai.getStressLevel() * 10));
                    System.out.print("\nPress Enter to return...");
                    scanner.nextLine();
                    break;
                default:
                    System.out.println(TerminalFX.RED + "[!] Invalid choice." + TerminalFX.RESET);
            }
        }
        scanner.close();
    }
}
