package com.listasmart.cupons.models;

/**
 * Item do ranking de colaboradores. Os concorrentes vêm da MockAPI;
 * o usuário atual é montado a partir dos dados locais.
 */
public class LeaderboardUser {

    private String name;
    private int points;
    private int contributions;
    private String avatar;
    private boolean currentUser;

    public LeaderboardUser() {
    }

    public LeaderboardUser(String name, int points, int contributions, String avatar, boolean currentUser) {
        this.name = name;
        this.points = points;
        this.contributions = contributions;
        this.avatar = avatar;
        this.currentUser = currentUser;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getContributions() { return contributions; }
    public void setContributions(int contributions) { this.contributions = contributions; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isCurrentUser() { return currentUser; }
    public void setCurrentUser(boolean currentUser) { this.currentUser = currentUser; }
}
