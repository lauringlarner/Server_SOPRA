package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

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

	public Game createGame(Game newGame) {
		newGame.setToken(UUID.randomUUID().toString());
		newGame.setStatus(UserStatus.OFFLINE);
		checkIfGameExists(newGame);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
        newGame.setWordList(Words.WordList());
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
}

