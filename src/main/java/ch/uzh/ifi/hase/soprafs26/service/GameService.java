package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
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

	private final Map<UUID, List<SseEmitter>> gameEmitters = new ConcurrentHashMap<>();

	public GameService(@Qualifier("gameRepository") GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	//////////////
	// Creation //
	//////////////

	public Game createGame(Lobby lobby) {
		Game newGame = new Game();

		newGame.setStatus(GameStatus.IN_PROGRESS);
		newGame.setLobbyId(lobby.getId());
		//setwordlist
		List<String> wordList = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			wordList.add(Words.Word());
		}
		newGame.setWordList(wordList);
		//set WordListScore
		List<String> wordListScore = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			wordListScore.add("0");
		}
		newGame.setWordListScore(wordListScore);
		//Set score
		int score = 0;
		newGame.setScore_1(score);
		newGame.setScore_2(score);
		// set game duration setting from lobby
		newGame.setGameDuration(lobby.getGameDuration());

		newGame = gameRepository.save(newGame);
		gameRepository.flush();

		log.debug("Created Information for Game: {}", newGame);
		return newGame;
	}

	///////////////
	// Retrieval //	
	///////////////

	public List<Game> getGames() {
		return this.gameRepository.findAll();
	}

	public Game getGameById(UUID gameId) {
		return gameRepository.findById(gameId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
	}

	/////////////
	// Updates //
	/////////////
	

	/////////////
	// Actions //
	/////////////


	//////////////
    // Deletion //
    //////////////

	public void deleteGame(Game game) {
		gameRepository.delete(game);

        log.debug("Game successfully deleted");
	}
	
	////////////////
	// Validation //
	////////////////

    ///////////////
    // Utilities //    
    ///////////////
	
	
    ///////////////////
    // SSE functions //
    ///////////////////

	public SseEmitter createAndRegisterGameStream(Game game) {
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

		// send initial game state
		try {
			emitter.send(SseEmitter.event()
				.name("gameUpdate")
				.data(DTOMapper.INSTANCE.convertEntityToGameDTO(game)));
		} catch (Exception e) {
			emitter.completeWithError(e);
		}

		// register emitter
		registerGameEmitter(game.getId(), emitter);

		// lifecycle cleanup
		emitter.onCompletion(() -> removeGameEmitter(game.getId(), emitter));
		emitter.onTimeout(() -> removeGameEmitter(game.getId(), emitter));

		return emitter;
	}

	public void registerGameEmitter(UUID gameId, SseEmitter emitter) {
		gameEmitters.computeIfAbsent(gameId, k -> new ArrayList<>()).add(emitter);
	}

	public void removeGameEmitter(UUID gameId, SseEmitter emitter) {
		List<SseEmitter> emitters = gameEmitters.get(gameId);
		if (emitters != null) {
			emitters.remove(emitter);
			if (emitters.isEmpty()) {
				gameEmitters.remove(gameId);
			}
		}
	}

	public void pushGameUpdate(Game game) {
		List<SseEmitter> emitters = gameEmitters.get(game.getId());
		if (emitters != null) {
			GameDTO gameDTO = DTOMapper.INSTANCE.convertEntityToGameDTO(game);
			for (SseEmitter emitter : emitters) {
				try {
					emitter.send(SseEmitter.event().name("gameUpdate").data(gameDTO));
				} catch (Exception e) {
					emitter.completeWithError(e);
				}
			}
		}
	}




/* Not yet sorted, and in need of refactoring */

	//returns a list of 16 randomly choosen words from the library
	public static List<String> WordList() {
		List<String> wordList = new ArrayList<>();

		for (int i = 0; i < 16; i++) {
			wordList.add(Words.Word());
		}

		return wordList;
	}

	public int checkWordList(List<String> wordList, String object) {
		for (int i = 0; i < wordList.size(); i++) {
			if (wordList.get(i).equals(object)) {
				return i;
			}
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object is not in the Game!");
	}

	public int checkWordTaken(List<String> wordListScore, int index) {
		if (wordListScore.get(index).equals("0")) {
			return 1;
		}

		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word is already taken by a team!");
	}

	public int imageSubmission(MultipartFile file, String object,List<String> wordListScore, int indexOfWord, String team, Game game){
	try{
    //check if the object is in the image 
    if(VisionQuickstartObjectLocalization.analyzeimage(file.getBytes(), object) == 1){//the object is in the list
	
        //set word as taken
        wordListScore.set(indexOfWord, "1");
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
	    // SSE pushes update
        pushGameUpdate(game);

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

//set word as taken
 //teamscore +=1
//return 1 if found, 0 if not

}