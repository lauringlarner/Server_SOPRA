package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserGetDTO {

	private UUID id;
	private String username;
	private String email;
	private int gamesPlayed;
	private int gamesWon;
	private int correctItemsFound;
	@JsonIgnore
	private String token;
	private UserStatus status;
	@JsonProperty("creation_date")
	private LocalDateTime createdAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getEmail() { 
		return email; 
		}


	public void setEmail(String email) { 
		this.email = email; 
		}

	public int getGamesPlayed() { 
		return gamesPlayed; 
		}

	public void setGamesPlayed(int gamesPlayed) { 
		this.gamesPlayed = gamesPlayed;
		 }

	public int getGamesWon() { 
		return gamesWon; 
		}

	public void setGamesWon(int gamesWon) { 
		this.gamesWon = gamesWon; 
		}

	public int getCorrectItemsFound() { 
		return correctItemsFound; 
		}

	public void setCorrectItemsFound(int correctItemsFound) { 
		this.correctItemsFound = correctItemsFound;
		}


}
