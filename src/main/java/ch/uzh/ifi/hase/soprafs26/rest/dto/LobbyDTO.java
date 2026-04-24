package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.UUID;

public class LobbyDTO {

    private UUID id;

    private String joinCode;

    private Integer gameDuration;

    private UUID gameId;

    private String createdAt; // needs to be String for pusher

    private List<LobbyPlayerDTO> lobbyPlayers;
    
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }


    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public List<LobbyPlayerDTO> getLobbyPlayers() {
        return lobbyPlayers;
    }

    public void setLobbyPlayers(List<LobbyPlayerDTO> lobbyPlayers) {
        this.lobbyPlayers = lobbyPlayers;
    }

    public Integer getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(Integer gameDuration) {
        this.gameDuration = gameDuration;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    
}
