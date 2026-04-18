package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.UUID;

public class LeaderboardPostDTO {

    private UUID gameId;

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }
}
