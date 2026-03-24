package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class UserLoginResponseDTO {

	private Long id;
	private String username;
	private String token;
	private UserStatus status;
	private String bio;
	@JsonProperty("creation_date")
	private LocalDateTime createdAt;

	public UserLoginResponseDTO() {}

	public UserLoginResponseDTO(Long id, String username, String token, UserStatus status,
			String bio, LocalDateTime createdAt) {
		this.id = id;
		this.username = username;
		this.token = token;
		this.status = status;
		this.bio = bio;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
