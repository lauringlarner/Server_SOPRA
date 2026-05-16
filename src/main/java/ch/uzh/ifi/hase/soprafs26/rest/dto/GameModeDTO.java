package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.List;

public class GameModeDTO {
    
    private String id;

    private String name;

    private List<String> rules;

    public GameModeDTO(String id, String name, List<String> rules) {
        this.id  = id;
        this.name = name;
        this.rules = rules;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    
}
