package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;

public class ChatMessageGetDTO {

    private String sender;
    private TeamType teamType;
    private String message;
    private String sentAt;

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public TeamType getTeamType() { return teamType; }
    public void setTeamType(TeamType teamType) { this.teamType = teamType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }
}