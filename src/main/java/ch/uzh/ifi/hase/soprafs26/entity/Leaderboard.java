package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "leaderboards")
public class Leaderboard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID gameId;

    @Column(nullable = false)
    private int team1Score;

    @Column(nullable = false)
    private int team2Score;

    // playerId tracking will be added in a future update to support individual player scores and rankings
    // currently, the leaderboard only tracks team scores for simplicity and to align with the current game design
    @Column(columnDefinition = "TEXT")
    private String playerScoresJson;

    // Snapshot of tileGrid at the time of last update
    @Column(columnDefinition = "TEXT")
    private String tileGridJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
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

    public String getPlayerScoresJson() {
        return playerScoresJson;
    }

    public void setPlayerScoresJson(String playerScoresJson) {
        this.playerScoresJson = playerScoresJson;
    }

    public String getTileGridJson() {
        return tileGridJson;
    }

    public void setTileGridJson(String tileGridJson) {
        this.tileGridJson = tileGridJson;
    }

    public Tile[][] getTileGrid() {
        if (tileGridJson == null) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(tileGridJson,
                mapper.getTypeFactory().constructArrayType(
                    mapper.getTypeFactory().constructArrayType(Tile.class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize tileGrid", e);
        }
    }

    public void setTileGrid(Tile[][] tileGrid) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.tileGridJson = mapper.writeValueAsString(tileGrid);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize tileGrid", e);
        }
    }
}
