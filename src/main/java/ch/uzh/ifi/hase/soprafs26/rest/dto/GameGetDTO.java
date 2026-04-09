package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

public class GameGetDTO {

	private Long id;
	private UserStatus status;
	private String[] wordList;
	private String[] wordListScore;
	private String token;
	private int score_1;
	private int score_2;


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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
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



