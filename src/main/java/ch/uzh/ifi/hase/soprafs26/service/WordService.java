package ch.uzh.ifi.hase.soprafs26.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WordService {

    private final Logger log = LoggerFactory.getLogger(WordService.class);

    private final SecureRandom secureRandom = new SecureRandom();

    public String[] WordList(String nameOfWordList) {
        List<String> words = new ArrayList<>();

        try (InputStream is = WordService.class.getClassLoader().getResourceAsStream(nameOfWordList);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {        
                    words.add(trimmed);
                }
            }
    } catch (Exception e) {
        log.debug("Unexpected error while reading word list: {}", nameOfWordList, e);
    }

    return words.toArray(String[]::new);
}
    public String Word(String typeOfWordList) {
        switch (typeOfWordList) {
            case "inside" -> {
                String[] wordsList = WordList("urban_objects_inside.csv");
                int randomNum = secureRandom.nextInt(wordsList.length);
                String word = wordsList[randomNum];
                log.debug("Generated word: {}", word);
                return word;
            }
            case "outside" -> {
                String[] wordsList = WordList("urban_objects_outside.csv");
                int randomNum = secureRandom.nextInt(wordsList.length);
                String word = wordsList[randomNum];
                log.debug("Generated word: {}", word);
                return word;
            }
            case "demo" -> {
                String[] wordsList = WordList("urban_objects_demo.csv");
                String word =wordsList[15];
                log.debug("Generated word: {}", word);
                return word;
            }
            default -> {
                String[] wordsList = WordList("urban_objects.csv");
                int randomNum = secureRandom.nextInt(wordsList.length);
                String word = wordsList[randomNum];
                log.debug("Generated word: {}", word);
                return word;
            }
        }
       
    }

}
