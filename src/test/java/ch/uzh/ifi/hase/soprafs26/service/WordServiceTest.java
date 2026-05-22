package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class WordServiceTest {

    // CSV loaded successfully → list is not empty
    @Test
    public void wordList_returnsNonEmpty() {
        String[] words = WordService.WordList("urban_objects.csv");

        assertNotNull(words);
        assertTrue(words.length > 0);
    }

    @Test
    public void wordList_containsNoBlankEntries() {
        String[] words = WordService.WordList("urban_objects.csv");

        assertTrue(Arrays.stream(words).noneMatch(word -> word == null || word.isBlank()));
    }

    // random word → not null and exists in the list
    @Test
    public void word_returnsValidWord() {
        String word = WordService.Word("all");
        String[] wordList = WordService.WordList("urban_objects.csv");

        assertNotNull(word);
        assertFalse(word.isBlank());
        assertTrue(Arrays.asList(wordList).contains(word));
    }
}
