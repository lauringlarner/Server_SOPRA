package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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

    @Column(nullable = true)
	private TeamType teamType;

    @Column(nullable = false)
	private boolean isHost;

    @Column(nullable = false)
	private boolean isReady;

    @OneToOne(optional = false) // every player must have a user
    @JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

    @ManyToOne
    @JoinColumn(name = "lobby_id")
	private Lobby lobby;


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Lobby getLobby() {
        return lobby;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }

    
}
