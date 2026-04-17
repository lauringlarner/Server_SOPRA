package ch.uzh.ifi.hase.soprafs26.entity;

import java.io.Serializable;
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
    private String[] wordlist;

	@Column(nullable = false)
    private String[] wordListScore;

	@Column(nullable = false)
	private int score_1;

	@Column(nullable = false)
	private int score_2;

	@Column(nullable = false)
	private Integer gameDuration;	

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

	public GameStatus getStatus() {
		return status;
	}

	public void setStatus(GameStatus status) {
		this.status = status;
	}

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
}