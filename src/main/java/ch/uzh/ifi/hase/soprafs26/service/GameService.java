package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization;
import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
	private final LobbyPlayerRepository lobbyPlayerRepository;

	public GameService(@Qualifier("gameRepository") GameRepository gameRepository,
			@Qualifier("lobbyPlayerRepository") LobbyPlayerRepository lobbyPlayerRepository) {
		this.gameRepository = gameRepository;
		this.lobbyPlayerRepository = lobbyPlayerRepository;
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

	public Game createGame(Game newGame) {
		newGame.setToken(UUID.randomUUID().toString());
		newGame.setStatus(UserStatus.OFFLINE);
		checkIfGameExists(newGame);
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


		newGame = gameRepository.save(newGame);
		gameRepository.flush();

		log.debug("Created Information for Game: {}", newGame);
		return newGame;
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * name
	 * defined in the Game entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param gameToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see Game
	 */
	private void checkIfGameExists(Game gameToBeCreated) {
		Game gameByName = gameRepository.findByToken(gameToBeCreated.getToken());

		String baseErrorMessage = "The %s provided %s not unique. Therefore, the game could not be created!";
		if (gameByName != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
		}
	}

	public Game getGameById(Long id) {
    return gameRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,String.format( "Game with id %d was not found", id))); //user/id implementation
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

	private String getSubmissionTeam(User user) {
		LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);
		if (lobbyPlayer == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found!");
		}

		TeamType teamType = lobbyPlayer.getTeamType();
		if (teamType == TeamType.Team1) {
			return "1";
		}
		if (teamType == TeamType.Team2) {
			return "2";
		}

		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player is not in a valid team!");
	}

	public String validateSubmissionRequest(Game game, MultipartFile file, String object, User user) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is missing!");
		}
		if (object == null || object.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object is missing!");
		}

		String team = getSubmissionTeam(user);

		String[] wordlist = game.getWordList();
		int indexOfWord = checkWordList(wordlist, object);
		String[] wordListScore = game.getWordListScore();
		checkWordTaken(wordListScore, indexOfWord);

		return team;
	}

	@Async
	public void processSubmissionAsync(Long gameId, byte[] fileBytes, String object, String team) {
		try {
			processSubmission(gameId, fileBytes, object, team);
		}
		catch (ResponseStatusException exception) {
			log.debug("Submission for game {} was not applied: {}", gameId, exception.getReason());
		}
		catch (Exception exception) {
			log.error("Submission for game {} failed", gameId, exception);
		}
	}

	@Transactional
	public void processSubmission(Long gameId, byte[] fileBytes, String object, String team) {
		Game game = getGameById(gameId);
		String[] wordlist = game.getWordList();
		int indexOfWord = checkWordList(wordlist, object);
		String[] wordListScore = game.getWordListScore();
		checkWordTaken(wordListScore, indexOfWord);

		try {
			if (VisionQuickstartObjectLocalization.analyzeimage(fileBytes, object) != 1) {
				return;
			}

			wordListScore[indexOfWord] = "1";
			if ("1".equals(team)) {
				int score = game.getScore_1();
				game.setScore_1(score + 1);
			}
			else {
				int score = game.getScore_2();
				game.setScore_2(score + 1);
			}

			gameRepository.flush();
		}
		catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error with image type!");
		}
	}
}
