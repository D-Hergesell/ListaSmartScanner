package com.listasmart.cupons.helpers;

import android.content.Context;

import com.listasmart.cupons.R;

/**
 * Regras de gamificação: selo de confiabilidade e progresso de nível.
 * Os limites seguem o protótipo (5 / 15 / 30 contribuições).
 */
public class GamificationHelper {

    public static final int POINTS_QR = 10;
    public static final int POINTS_MANUAL = 5;

    public static String getBadgeLevel(Context context, int contributions) {
        if (contributions >= 30) return context.getString(R.string.badge_gold);
        if (contributions >= 15) return context.getString(R.string.badge_silver);
        if (contributions >= 5) return context.getString(R.string.badge_bronze);
        return context.getString(R.string.badge_beginner);
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
