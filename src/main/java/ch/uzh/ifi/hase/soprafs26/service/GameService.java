package ch.uzh.ifi.hase.soprafs26.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class GameService {

	private final Logger log = LoggerFactory.getLogger(GameService.class);

	private final GameRepository gameRepository;

	public GameService(@Qualifier("gameRepository") GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}



	public List<Game> getGames() {
		return this.gameRepository.findAll();
	}

	public static String[] WordList() {				//returns a list of 16 randomly choosen words from the library
		String[] WordList = new String[16];

		for (int i = 0; i<16; i++){
			WordList[i] = Words.Word();
		}
		return WordList;
	}

	public Game createGame(Lobby lobby) {
		Game newGame = new Game();

		newGame.setStatus(GameStatus.IN_PROGRESS);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		//setwordlist
		String[] wordList = new String[16];
		for(int i=0; i < 16; i++ ){
			wordList[i] = Words.Word();
		}
		newGame.setWordList(wordList);
		//set WordListScore
		String[] wordListScore = new String[16];
		Arrays.fill(wordListScore, "0");
		newGame.setWordListScore(wordListScore);
		//Set score
		int score = 0;
		newGame.setScore_1(score);
		newGame.setScore_1(score);
		// set game duration setting from lobby
		newGame.setGameDuration(lobby.getGameDuration());

		newGame = gameRepository.save(newGame);
		gameRepository.flush();

		log.debug("Created Information for Game: {}", newGame);
		return newGame;
	}

	public Game getGameById(UUID gameId) {
		return gameRepository.findById(gameId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
	}

	public int checkWordList(String[] wordlist, String object){
		for (int i=0; i<wordlist.length; i++){
			if ( wordlist[i].equals(object)){
				return i;
			}	
	}
	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object is not in the Game!");
		}
		

	public int checkWordTaken(String[] wordListScore, int index){
		if (wordListScore[index].equals("0")){
			return 1;
		}

		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word is already taken by a team!");
	}


	public int imageSubmission(MultipartFile file, String object,String[] wordlistscore, int indexofword, String team, Game game){
	try{
    //check if the object is in the image 
    if(VisionQuickstartObjectLocalization.analyzeimage(file.getBytes(), object) == 1){//the object is in the list
	
        //set word as taken
        wordlistscore[indexofword]="1";
        //teamscore +=1
       if("1".equals(team)){
        int score=game.getScore_1();
        game.setScore_1(score+1);
       }                                        //add in service so it can be saved
       else if("2".equals(team)){
        int score=game.getScore_2();
        game.setScore_2(score+1);
       }
       else{throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not in Game!");}

        //here check if all words are taken and end the game(all objects found == all items in wordlistscore != 0)

       gameRepository.flush();
        //return
        int result = 1;
            return result;
        
    }else{
        int result=0;
        return result;
        
	}}catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error with image type!");


}
	}
}
//set word as taken
 //teamscore +=1
//return 1 if found, 0 if not