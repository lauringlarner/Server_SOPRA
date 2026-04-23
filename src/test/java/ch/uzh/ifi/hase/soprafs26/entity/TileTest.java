package ch.uzh.ifi.hase.soprafs26.entity;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;

import static org.junit.jupiter.api.Assertions.*;

public class TileTest {

    // default constructor → all fields null/default
    @Test
    public void defaultConstructor_fieldsAreDefault() {
        Tile tile = new Tile();

        assertNull(tile.getWord());
        assertEquals(0, tile.getValue());
        assertNull(tile.getStatus());
    }

    // parameterized constructor → all fields set correctly
    @Test
    public void parameterizedConstructor_setsAllFields() {
        Tile tile = new Tile("cat", 3, TileStatus.UNCLAIMED);

        assertEquals("cat", tile.getWord());
        assertEquals(3, tile.getValue());
        assertEquals(TileStatus.UNCLAIMED, tile.getStatus());
    }

    // setters → update fields correctly
    @Test
    public void setters_updateFields() {
        Tile tile = new Tile();
        tile.setWord("dog");
        tile.setValue(5);
        tile.setStatus(TileStatus.CLAIMED_TEAM1);

        assertEquals("dog", tile.getWord());
        assertEquals(5, tile.getValue());
        assertEquals(TileStatus.CLAIMED_TEAM1, tile.getStatus());
    }
}
