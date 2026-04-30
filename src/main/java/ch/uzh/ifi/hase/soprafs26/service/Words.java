package ch.uzh.ifi.hase.soprafs26.service;

import java.io.*;
import java.util.*;

public class Words {

    public static String[] WordList(String nameOfWordList) {
        List<String> words = new ArrayList<>();

        try (InputStream is = Words.class.getClassLoader().getResourceAsStream(nameOfWordList);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {        
                    words.add(trimmed);
                }
            }
    } catch (IOException e) {
        e.printStackTrace();
    }

    return words.toArray(new String[0]);
}
    public static String Word(String typeOfWordList) {
        if (typeOfWordList.equals("inside")){
            String[] wordsList = WordList("urban_objects_inside.csv");
            int randomNum = (int) (Math.random() * wordsList.length);
            String word = wordsList[randomNum];
            System.out.println(word);
            return word;
        }
        else if(typeOfWordList.equals("outside")){
            String[] wordsList = WordList("urban_objects_outside.csv");
            int randomNum = (int) (Math.random() * wordsList.length);
            String word = wordsList[randomNum];
            System.out.println(word);
            return word;
        }
        else {
            String[] wordsList = WordList("urban_objects.csv");
            int randomNum = (int) (Math.random() * wordsList.length);
            String word = wordsList[randomNum];
            System.out.println(word);
            return word;
        }
       
    }
    // select n words and send to frontend
    // maybe a feature, handle harder words for higher levels, easier words for lower levels (easy, medium, hard)
    //
    

    public static void main(String[] args) {
        Word("inside");
    }
}
