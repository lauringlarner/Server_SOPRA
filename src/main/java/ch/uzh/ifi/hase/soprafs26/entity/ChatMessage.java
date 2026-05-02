package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.Instant;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID gameId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = true)
    private TeamType teamType;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Instant sentAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public TeamType getTeamType() { return teamType; }
    public void setTeamType(TeamType teamType) { this.teamType = teamType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}