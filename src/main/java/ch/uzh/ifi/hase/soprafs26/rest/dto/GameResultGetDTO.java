package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.UUID; // Wichtig: UUID importieren

public class GameResultGetDTO {
    private String winnerTeam;
    private int winnerScore;
    private String loserTeam;
    private int loserScore;
    private boolean isDraw;
    private List<PlayerInfo> playerList;

    public static class PlayerInfo {
        private UUID id; // Geändert von Long zu UUID
        private String username;
        private String teamType;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getTeamType() { return teamType; }
        public void setTeamType(String teamType) { this.teamType = teamType; }
    }

    // Getters and Setters
    public String getWinnerTeam() { return winnerTeam; }
    public void setWinnerTeam(String winnerTeam) { this.winnerTeam = winnerTeam; }
    public int getWinnerScore() { return winnerScore; }
    public void setWinnerScore(int winnerScore) { this.winnerScore = winnerScore; }
    public String getLoserTeam() { return loserTeam; }
    public void setLoserTeam(String loserTeam) { this.loserTeam = loserTeam; }
    public int getLoserScore() { return loserScore; }
    public void setLoserScore(int loserScore) { this.loserScore = loserScore; }
    public boolean getIsDraw() { return isDraw; }
    public void setIsDraw(boolean draw) { isDraw = draw; }
    public List<PlayerInfo> getPlayerList() { return playerList; }
    public void setPlayerList(List<PlayerInfo> playerList) { this.playerList = playerList; }
}