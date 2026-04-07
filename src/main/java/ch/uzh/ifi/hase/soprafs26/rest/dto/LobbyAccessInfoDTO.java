package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.UUID;

public class LobbyAccessInfoDTO {

    private UUID id;

    private String joinCode;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

}
