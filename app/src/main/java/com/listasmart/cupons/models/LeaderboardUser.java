package com.listasmart.cupons.models;

/**
 * Item do ranking de colaboradores, vindo da API (/ranking).
 */
public class LeaderboardUser {

    private String name;
    private int points;
    private int contributions;
    private String avatar;
    private boolean currentUser;
    private String badge;   // Selo de Confiabilidade (derivado dos pontos no backend)
    // Posição real (1-based) no ranking. 0 = usar a posição na lista exibida.
    // Útil ao mostrar o usuário fora do top 10 mantendo seu ranking verdadeiro.
    private int rank;

    public LeaderboardUser() {
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

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}
