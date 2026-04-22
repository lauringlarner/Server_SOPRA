package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;

public class LobbyPlayerDTO {
    
    private UUID id;

    private String joinedAt;

    private TeamType teamType;

    private boolean isHost;

    private boolean isReady;

    private UserGetDTO userGetDTO;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }

    public TeamType getTeamType() {
        return teamType;
    }

    public void setTeamType(TeamType teamType) {
        this.teamType = teamType;
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
    // User getter returns UserGetDTO instead of the actual User to hide token
    public UserGetDTO getUserGetDTO() {
        return userGetDTO; 
    }

    public void setUserGetDTO(UserGetDTO userGetDTO) {
        this.userGetDTO = userGetDTO;
    }

    
}
