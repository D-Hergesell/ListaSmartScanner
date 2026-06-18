package com.listasmart.cupons.network;

/**
 * Resposta de /auth/register e /auth/login: token JWT + dados básicos.
 */
public class AuthResponse {

    private String token;
    private long userId;
    private String username;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
