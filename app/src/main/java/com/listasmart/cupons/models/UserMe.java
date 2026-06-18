package com.listasmart.cupons.models;

/**
 * Resposta de GET /users/me: perfil + gamificação + posição no ranking.
 * Espelha o UserMeResponse do backend.
 */
public class UserMe {

    private long id;
    private String name;
    private int points;
    private int contributions;
    private int rankingPosition;
    private Badge badge;

    public long getId() { return id; }
    public String getName() { return name; }
    public int getPoints() { return points; }
    public int getContributions() { return contributions; }
    public int getRankingPosition() { return rankingPosition; }
    public Badge getBadge() { return badge; }

    /**
     * Selo de confiabilidade (rank atual + progresso até o próximo).
     * nextRank/nextThreshold vêm nulos quando no rank máximo (Desafiante).
     */
    public static class Badge {
        private String currentRank;
        private String nextRank;
        private int points;
        private int currentThreshold;
        private Integer nextThreshold;
        private int progressPercent;

        public String getCurrentRank() { return currentRank; }
        public String getNextRank() { return nextRank; }
        public int getPoints() { return points; }
        public int getCurrentThreshold() { return currentThreshold; }
        public Integer getNextThreshold() { return nextThreshold; }
        public int getProgressPercent() { return progressPercent; }
    }
}
