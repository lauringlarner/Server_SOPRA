package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class ErrorDTO {

	private String reason;

	public ErrorDTO(String reason) {
		this.reason = reason;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
