package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


@Entity
@Table(name = "lobby")
public class Lobby {
    
    private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

    @Column(unique = true)
	private String joinCode;

    @Column(nullable = false)
	private LobbyStatus status;

    @Column(nullable = false)
	private LocalDateTime createdAt;

    @OneToMany(mappedBy="lobby", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<LobbyPlayer> lobbyPlayers = new ArrayList<>();

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

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

    public LobbyStatus getStatus() {
        return status;
    }

    public void setStatus(LobbyStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void startGame() {
        // implementation
    }

    public void closeLobby() {
        // implementation
    }

    public void addPlayer(LobbyPlayer lobbyPlayer) {
        lobbyPlayers.add(lobbyPlayer);
    }

    public void removePlayer(LobbyPlayer lobbyPlayer) {
        lobbyPlayers.removeIf(p -> Objects.equals(p.getId(), lobbyPlayer.getId()));
    }


}
