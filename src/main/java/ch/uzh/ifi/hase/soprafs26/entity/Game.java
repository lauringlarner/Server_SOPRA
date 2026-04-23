package ch.uzh.ifi.hase.soprafs26.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "games")
public class Game implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private GameStatus status;

    @Column(nullable = false)
    private List<String> wordList = new ArrayList<>();

	@Column(nullable = false)
    private List<String> wordListScore = new ArrayList<>();

	@Column(nullable = false)
	private int score_1;

	@Column(nullable = false)
	private int score_2;

	@Column(nullable = false)
	private Integer gameDuration;

	@Column(nullable = false)
	private Instant startedAt;

	@Column(nullable = false)
	private UUID lobbyId;

	@Column(nullable = false)
	private int boardSize;

	@Column(columnDefinition = "TEXT")
	private String tileGridJson;

	public void setLobbyId(UUID lobbyId) {
		this.lobbyId = lobbyId;
	}

	public UUID getLobbyId() {
		return lobbyId;
	}

	public Integer getGameDuration() {
		return gameDuration;
	}

	public void setGameDuration(Integer gameDuration) {
		this.gameDuration = gameDuration;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public GameStatus getStatus() {
		return status;
	}

	public void setStatus(GameStatus status) {
		this.status = status;
	}

    public List<String> getWordList() {
		return wordList;
	}

	public void setWordList(List<String> WordList) {
		this.wordList = WordList;
	}

	public List<String> getWordListScore(){
		return wordListScore;
	}

	public void setWordListScore(List<String> wordListScore){
		this.wordListScore = wordListScore;
	}

	public int getScore_1() {
		return score_1;
	}

	public void setScore_1(int score_1) {
		this.score_1 = score_1;
	}

	public int getScore_2() {
		return score_2;
	}

	public void setScore_2(int score_2) {
		this.score_2 = score_2;
	}

	public int getBoardSize() {
		return boardSize;
	}

	public void setBoardSize(int boardSize) {
		this.boardSize = boardSize;
	}

	public String getTileGridJson() {
		return tileGridJson;
	}

	public void setTileGridJson(String tileGridJson) {
		this.tileGridJson = tileGridJson;
	}

	public Tile[][] getTileGrid() {
		if (tileGridJson == null) return null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(tileGridJson,
				mapper.getTypeFactory().constructArrayType(
					mapper.getTypeFactory().constructArrayType(Tile.class)));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to deserialize tileGrid", e);
		}
	}

	public void setTileGrid(Tile[][] tileGrid) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			this.tileGridJson = mapper.writeValueAsString(tileGrid);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize tileGrid", e);
		}
	}
}
