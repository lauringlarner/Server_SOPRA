package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;

public class LobbyPlayerDTO {
    
    private UUID id;

    private LocalDateTime joinedAt;

    private TeamColor team;

    private boolean isHost;

    private boolean isReady;

    private UUID userId;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public TeamColor getTeam() {
        return team;
    }

    public void setTeam(TeamColor team) {
        this.team = team;
    }

    public boolean getIsHost() {
        return isHost;
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }

    public boolean getIsReady() {
        return isReady;
    }

    public void setIsReady(boolean isReady) {
        this.isReady = isReady;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        userId = userId;
    }

    
}
