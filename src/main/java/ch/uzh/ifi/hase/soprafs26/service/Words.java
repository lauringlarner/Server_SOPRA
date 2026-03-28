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
                words.add(line.trim());
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

    public static void main(String[] args) {
        Word();
    }
}
