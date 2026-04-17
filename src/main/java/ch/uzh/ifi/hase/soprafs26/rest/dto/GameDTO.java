package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;

public class GameDTO {

	private UUID id;
	private GameStatus status;
	private String[] wordList;
	private String[] wordListScore;
	private int score_1;
	private int score_2;
	private Integer gameDuration;


	public UUID getId() {
		return id;
	}

	public Integer getGameDuration() {
		return gameDuration;
	}

	public void setGameDuration(Integer gameDuration) {
		this.gameDuration = gameDuration;
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

	public void setWordList(String[] wordList){
		this.wordList = wordList;
	}

	public String[] getWordList() {
		return wordList;
	}

	public void setWordListScore(String[] wordList){
		this.wordList = wordListScore;
	}

	public String[] getWordListScore() {
		return wordListScore;
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



