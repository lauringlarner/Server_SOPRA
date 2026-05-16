package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.entity.Tile;

public class LeaderboardGetDTO {

    private final UUID gameId;
    private int team1Score;
    private int team2Score;
    private Boolean isSinglePlayer;
    private Tile[][] tileGrid;

    public LeaderboardGetDTO(UUID gameId) {
        this.gameId = gameId;
    }

    public UUID getGameId() {
        return gameId;
    }

    public int getTeam1Score() {
        return team1Score;
    }

    public void setTeam1Score(int team1Score) {
        this.team1Score = team1Score;
    }

    public int getTeam2Score() {
        return team2Score;
    }

    public void setTeam2Score(int team2Score) {
        this.team2Score = team2Score;
    }

    public Boolean getIsSinglePlayer() {
        return isSinglePlayer;
    }

    public void setIsSinglePlayer(Boolean isSinglePlayer) {
        this.isSinglePlayer = isSinglePlayer;
    }

    public Tile[][] getTileGrid() {
        return tileGrid;
    }

    public void setTileGrid(Tile[][] tileGrid) {
        this.tileGrid = tileGrid;
    }
}
