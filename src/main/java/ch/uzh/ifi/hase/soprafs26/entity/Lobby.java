package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    @Column(nullable = true)
    private UUID gameId;

    @Column(nullable = false)
	private LocalDateTime createdAt;

    @OneToMany(mappedBy="lobby", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<LobbyPlayer> lobbyPlayers = new ArrayList<>();

    // Game settings
    @Column(nullable = false)
	private Integer gameDuration;

    /*
    @Column(nullable = false)
	private Integer bingoBoardSize; // bingoBoardSize = 4 -> 4 x 4 Board, i.e. 16 items

    @Column(nullable = false)
	private Integer numberOfRounds;

    @Column(nullable = false)
	private GameMode gameMode;   
    */
  
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Integer getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(Integer gameDuration) {
        this.gameDuration = gameDuration;
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

    public List<LobbyPlayer> getLobbyPlayers() {
        return lobbyPlayers;
    }

    public void setLobbyPlayers(List<LobbyPlayer> lobbyPlayers) {
        this.lobbyPlayers = lobbyPlayers;
    }

    public UUID getGameId() {
        return gameId;
    }

    public void setGameId(UUID gameId) {
        this.gameId = gameId;
    }

}
