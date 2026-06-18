package com.listasmart.cupons.helpers;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Controle de sessão local usando SharedPreferences.
 * Guarda nome e estado de login do usuário (cadastro simulado, sem backend real).
 */
public class SessionManager {

    private static final String PREF_NAME = "lista_smart_session";
    private static final String KEY_LOGGED = "is_logged";
    private static final String KEY_NAME = "user_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void login(String name) {
        prefs.edit()
                .putBoolean(KEY_LOGGED, true)
                .putString(KEY_NAME, name)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED, false);
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "Você");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    /** Iniciais do nome para exibir no avatar (ex: "Carlos Silva" -> "CS"). */
    public String getInitials() {
        String name = getUserName().trim();
        if (name.isEmpty()) return "VC";
        String[] parts = name.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
