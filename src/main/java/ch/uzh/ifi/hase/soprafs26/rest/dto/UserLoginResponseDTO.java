package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;

public class UserLoginResponseDTO {

	private UUID id;
	private String username;
	private String token;
	private UserStatus status;
	@JsonProperty("creation_date")
	private LocalDateTime createdAt;

	public UserLoginResponseDTO() {}

	public UserLoginResponseDTO(UUID id, String username, String token, UserStatus status, LocalDateTime createdAt) {
		this.id = id;
		this.username = username;
		this.token = token;
		this.status = status;
		this.createdAt = createdAt;
	}

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
}
