package ch.uzh.ifi.hase.soprafs26.service;

import java.io.*;
import java.util.*;

public class Words {

    public static String[] WordList() {
        List<String> words = new ArrayList<>();

        try (InputStream is = Words.class.getClassLoader().getResourceAsStream("urban_objects.csv");
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) {
                    words.add(word);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return words.toArray(new String[0]);
    }

    public static String Word() {
        String[] wordsList = WordList();
        int randomNum = (int) (Math.random() * wordsList.length);
        String word = wordsList[randomNum];
        System.out.println(word);
        return word;
    }
    // select n words and send to frontend
    // maybe a feature, handle harder words for higher levels, easier words for lower levels (easy, medium, hard)
    //
    

    public static void main(String[] args) {
        Word();
    }
}
