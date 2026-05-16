package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GameSettingsDTO {
    private Integer gameDuration;
    private String listType;

    public Integer getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(Integer gameDuration) {
        this.gameDuration = gameDuration;
    }

    public String getListType() {
        return listType;
    }

    public void setListType(String listType) {
        this.listType = listType;
    }

}
