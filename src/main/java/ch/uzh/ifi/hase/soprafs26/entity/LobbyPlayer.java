package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


@Entity
@Table(name = "player")
public class LobbyPlayer {
    
    private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

    @Column(nullable = false)
	private LocalDateTime joinedAt;

    @Column(nullable = false)
	private TeamColor team;

    @Column(nullable = false)
	private boolean isHost;

    @Column(nullable = false)
	private boolean isReady;

    // reference to its corresponding User
    @Column(nullable = false)
	private UUID UserId;

    @ManyToOne
    @JoinColumn(name = "lobby_id")
	private Lobby lobby;


    public UUID getUserId() {
        return UserId;
    }

    public void setUserId(UUID userId) {
        UserId = userId;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

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

    public boolean isHost() {
        return isHost;
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setIsReady(boolean isReady) {
        this.isReady = isReady;
    }

    
}
