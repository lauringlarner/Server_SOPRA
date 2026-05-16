package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class WordsTest {

    // CSV loaded successfully → list is not empty
    @Test
    public void wordList_returnsNonEmpty() {
        String[] words = Words.WordList("urban_objects.csv");

        assertNotNull(words);
        assertTrue(words.length > 0);
    }

    @Test
    public void wordList_containsNoBlankEntries() {
        String[] words = Words.WordList("urban_objects.csv");

        assertTrue(Arrays.stream(words).noneMatch(word -> word == null || word.isBlank()));
    }

    // random word → not null and exists in the list
    @Test
    public void word_returnsValidWord() {
        String word = Words.Word("all");
        String[] wordList = Words.WordList("urban_objects.csv");

        assertNotNull(word);
        assertFalse(word.isBlank());
        assertTrue(Arrays.asList(wordList).contains(word));
    }
}
