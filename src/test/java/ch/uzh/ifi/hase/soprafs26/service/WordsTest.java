package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class WordsTest {

    // CSV loaded successfully → list is not empty
    @Test
    public void wordList_returnsNonEmpty() {
        String[] words = Words.WordList();

        assertNotNull(words);
        assertTrue(words.length > 0);
    }

    // random word → not null and exists in the list
    @Test
    public void word_returnsValidWord() {
        String word = Words.Word();
        String[] wordList = Words.WordList();

        assertNotNull(word);
        assertTrue(Arrays.asList(wordList).contains(word));
    }
}
