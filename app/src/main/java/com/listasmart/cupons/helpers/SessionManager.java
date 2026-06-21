package com.listasmart.cupons.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.listasmart.cupons.network.ApiClient;
import com.listasmart.cupons.network.AuthResponse;

/**
 * Controle de sessão local. Dados sensíveis (token JWT, id do usuário) são
 * gravados em {@link EncryptedSharedPreferences}, com a chave mestra protegida
 * pelo Android Keystore (componente de chaves seguro do SO). Em caso de falha
 * ao inicializar a criptografia (ex.: Keystore indisponível em alguns
 * emuladores), há degradação graciosa para SharedPreferences comum, mantendo o
 * app funcional. Propaga o token ao {@link ApiClient} para autenticar as
 * requisições.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "lista_smart_session";
    private static final String PREF_NAME_SECURE = "lista_smart_session_secure";
    private static final String KEY_LOGGED = "is_logged";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = buildSecurePrefs(context.getApplicationContext());
        // Garante que o token persistido esteja disponível ao ApiClient
        // logo na criação (ex.: app reaberto com sessão ativa).
        ApiClient.setAuthToken(getToken());
    }

    /**
     * Cria o armazenamento criptografado (AES-256) com a chave mestra no
     * Android Keystore. Se a inicialização falhar, cai para SharedPreferences
     * comum para não travar o login.
     */
    private static SharedPreferences buildSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME_SECURE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.w(TAG, "Falha ao inicializar armazenamento criptografado; "
                    + "usando SharedPreferences comum.", e);
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
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
