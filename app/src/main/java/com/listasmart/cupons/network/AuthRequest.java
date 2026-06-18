package com.listasmart.cupons.network;

/**
 * Corpo de POST /auth/register e POST /auth/login.
 */
public class AuthRequest {

    private final String username;
    private final String password;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
