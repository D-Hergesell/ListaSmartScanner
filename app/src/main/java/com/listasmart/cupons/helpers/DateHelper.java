package com.listasmart.cupons.helpers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utilitários de data usados na validação e exibição das contribuições.
 */
public class DateHelper {

    private static final SimpleDateFormat ISO =
            new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
    private static final SimpleDateFormat DISPLAY =
            new SimpleDateFormat("dd MMM yyyy 'às' HH:mm", new Locale("pt", "BR"));
    private static final SimpleDateFormat DISPLAY_DATE =
            new SimpleDateFormat("dd MMM yyyy", new Locale("pt", "BR"));

    public static String todayIso() {
        return ISO.format(new Date());
    }

    public static String formatIso(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        return ISO.format(c.getTime());
    }

    public static Date parseIso(String iso) {
        try {
            return ISO.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatTimestamp(long millis) {
        return DISPLAY.format(new Date(millis));
    }

    /** Formata a data da compra ("yyyy-MM-dd") para exibição (ex.: "15 jun 2025"). */
    public static String formatDate(String iso) {
        Date d = parseIso(iso);
        return d != null ? DISPLAY_DATE.format(d) : iso;
    }
}
