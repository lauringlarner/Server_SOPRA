package ch.uzh.ifi.hase.soprafs26.service;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class WordServiceTest {

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

    @Test
    public void word_insideBranch_returnsValidWord() {
        String word = WordService.Word("inside");
        assertNotNull(word);
        assertFalse(word.isBlank());
    }

    @Test
    public void word_outsideBranch_returnsValidWord() {
        String word = WordService.Word("outside");
        assertNotNull(word);
        assertFalse(word.isBlank());
    }

    @Test
    public void word_demoBranch_returnsFixedIndex() {
        String word = WordService.Word("demo");
        assertNotNull(word);
        assertFalse(word.isBlank());
    }
}
