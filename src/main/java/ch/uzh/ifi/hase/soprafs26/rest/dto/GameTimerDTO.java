package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.TimerStatus;

public class GameTimerDTO {

    private TimerStatus status;

    private Integer remainingMinutes;

    public TimerStatus getStatus() {
        return status;
    }

    public void setStatus(TimerStatus status) {
        this.status = status;
    }

    public Integer getRemainingMinutes() {
        return remainingMinutes;
    }

    public void setRemainingMinutes(Integer remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }
}
