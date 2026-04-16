package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

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
	@GeneratedValue
	private Long id;

	@Column(nullable = false)
	private UserStatus status; //?

    @Column(nullable = false)
    private String[] wordlist;

	@Column(nullable = false)
    private String[] wordListScore; //?

    @Column(nullable = false, unique = true)
	private String token;

	@Column(nullable = false)
	private int score_1;

	@Column(nullable = false)
	private int score_2;

	@Column(nullable = false)
	private int boardSize;

	@Column(columnDefinition = "TEXT")
	private String tileGridJson;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	} 

    public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}//? -> just maybe change to ID


    public String[] getWordList() {
		return wordlist;
	}

	public void setWordList(String[] WordList) {
		this.wordlist = WordList;
	}

	public String[] getWordListScore(){
		return wordListScore;
	}

	public void setWordListScore(String[] wordListScore){
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