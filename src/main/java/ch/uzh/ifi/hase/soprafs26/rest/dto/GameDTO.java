package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;
import java.util.UUID;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;

public class GameDTO {

	private UUID id;
	private GameStatus status;
	private List<String> wordList;
	private List<String> wordListScore;
	private int score_1;
	private int score_2;
	private Integer gameDuration;
	private UUID lobbyId;

	public UUID getLobbyId() {
		return lobbyId;
	}

	public void setLobbyId(UUID lobbyId) {
		this.lobbyId = lobbyId;
	}

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

	public void setWordList(List<String> wordList){
		this.wordList = wordList;
	}

	public List<String> getWordList() {
		return wordList;
	}

	public void setWordListScore(List<String> wordListScore){
		this.wordListScore = wordListScore;
	}

	public List<String> getWordListScore() {
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



