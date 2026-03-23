package ch.uzh.ifi.hase.soprafs26.service;

//needs to be importet to work with files
// Import this class to handle errors
import java.io.*;
// Import the Scanner class to read text files
import java.util.*;





public class Words {
    public static String[] WordList() {
    File wordList = new File("/Users/laurinprivate/Desktop/Server_SOPRA/src/main/java/ch/uzh/ifi/hase/soprafs26/service/urban_objects.csv");

    List<String> words = new ArrayList();

        try (BufferedReader br = new BufferedReader(new FileReader(wordList))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        words.add(line.trim()); // Add each line as a word
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Convert List to Array
                String[] wordsArray = words.toArray(new String[0]);



                return wordsArray; 

            }


    public static String Word() {
        String[] wordsList = WordList();//list of words

        int arrayLength = wordsList.length; //length of array

        int randomNum = (int)(Math.random() * arrayLength ); //not sure if the whole array is matched

        String word = wordsList[randomNum]; // get random word
        System.out.println(word);
        return word;

    }
        
public static void main(String[] args){
    Word();

}

}