package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;

public class LobbyPlayerDTO {
    
    private UUID id;

    private LocalDateTime joinedAt;

    private TeamType teamType;

    private boolean isHost;

    private boolean isReady;

    private User user;


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

    public TeamType getTeam() {
        return teamType;
    }

    public void setTeam(TeamType teamType) {
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
    public UserGetDTO getUser() {
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    }

    public void setUser(User user) {
        this.user = user;
    }

    
}
