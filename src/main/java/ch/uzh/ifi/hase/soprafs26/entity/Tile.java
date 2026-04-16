package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;

public class Tile {

    private String word;
    private int value;
    private TileStatus status;

    public Tile() {
    }

    public Tile(String word, int value, TileStatus status) {
        this.word = word;
        this.value = value;
        this.status = status;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public TileStatus getStatus() {
        return status;
    }

    public void setStatus(TileStatus status) {
        this.status = status;
    }
}
