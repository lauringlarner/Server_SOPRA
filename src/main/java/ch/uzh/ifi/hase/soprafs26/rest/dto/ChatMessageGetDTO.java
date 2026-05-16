package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;

public class ChatMessageGetDTO {

    private String sender;
    private TeamType teamType;
    private String message;
    private Instant sentAt;

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public TeamType getTeamType() { return teamType; }
    public void setTeamType(TeamType teamType) { this.teamType = teamType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}