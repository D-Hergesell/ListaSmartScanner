package com.listasmart.cupons.helpers;

/**
 * Regras de gamificação: selo de confiabilidade e progresso de nível.
 * Os limites seguem o protótipo (5 / 15 / 30 contribuições).
 */
public class GamificationHelper {

    public static final int POINTS_QR = 10;
    public static final int POINTS_MANUAL = 5;

    public static String getBadgeLevel(int contributions) {
        if (contributions >= 30) return "Ouro";
        if (contributions >= 15) return "Prata";
        if (contributions >= 5) return "Bronze";
        return "Iniciante";
    }

    public static int getNextLevelTarget(int contributions) {
        if (contributions < 5) return 5;
        if (contributions < 15) return 15;
        if (contributions < 30) return 30;
        return 50;
    }

    public static int getProgressPercent(int contributions) {
        int target = getNextLevelTarget(contributions);
        int percent = (int) ((contributions / (float) target) * 100);
        return Math.min(percent, 100);
    }
}
