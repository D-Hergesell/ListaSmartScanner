package com.listasmart.cupons.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.listasmart.cupons.network.ApiClient;
import com.listasmart.cupons.network.AuthResponse;

/**
 * Controle de sessão local (SharedPreferences). Além do nome, guarda o token
 * JWT e o id do usuário retornados pelo backend, e propaga o token ao
 * {@link ApiClient} para autenticar as requisições.
 */
public class SessionManager {

    private static final String PREF_NAME = "lista_smart_session";
    private static final String KEY_LOGGED = "is_logged";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Garante que o token persistido esteja disponível ao ApiClient
        // logo na criação (ex.: app reaberto com sessão ativa).
        ApiClient.setAuthToken(getToken());
    }

    /** Login real (backend): persiste token + id + username e ativa no ApiClient. */
    public void login(AuthResponse auth) {
        prefs.edit()
                .putBoolean(KEY_LOGGED, true)
                .putString(KEY_NAME, auth.getUsername())
                .putString(KEY_TOKEN, auth.getToken())
                .putLong(KEY_USER_ID, auth.getUserId())
                .apply();
        ApiClient.setAuthToken(auth.getToken());
    }

    /**
     * Login legado apenas com nome (sem backend). Mantido para compatibilidade
     * com o fluxo atual; não define token.
     */
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

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public void logout() {
        prefs.edit().clear().apply();
        ApiClient.setAuthToken(null);
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
