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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization;
import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
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
	private final ScoreService scoreService;
	private final LeaderboardService leaderboardService;

	private final Map<UUID, List<SseEmitter>> gameEmitters = new ConcurrentHashMap<>();

	public GameService(@Qualifier("gameRepository") GameRepository gameRepository,
					   ScoreService scoreService,
					   LeaderboardService leaderboardService) {
		this.gameRepository = gameRepository;
		this.scoreService = scoreService;
		this.leaderboardService = leaderboardService;
	}

	//////////////
	// Creation //
	//////////////

	public Game createGame(Lobby lobby) {
		Game newGame = new Game();
		int boardSize = 4;
		List<String> wordList = new ArrayList<>();
		Tile[][] tileGrid = new Tile[boardSize][boardSize];

		newGame.setStatus(GameStatus.IN_PROGRESS);
		newGame.setLobbyId(lobby.getId());

		// Build one canonical 4x4 board and derive the flat word list from it.
		for (int row = 0; row < boardSize; row++) {
			for (int col = 0; col < boardSize; col++) {
				String word = Words.Word();
				wordList.add(word);
				tileGrid[row][col] = new Tile(word, 1, TileStatus.UNCLAIMED);
			}
		}

		newGame.setWordList(wordList);
		//set WordListScore
		List<String> wordListScore = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			wordListScore.add("0");
		}
		newGame.setWordListScore(wordListScore);
		//Set score
		newGame.setScore_1(0);
		newGame.setScore_2(0);
		// set game duration setting from lobby
		newGame.setGameDuration(lobby.getGameDuration());

		newGame.setBoardSize(boardSize);
		newGame.setTileGrid(tileGrid);

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

	public void validateTileAvailable(Game game, int indexOfWord) {
		Tile tile = getTileAtIndex(game, indexOfWord);
		if (tile.getStatus() != TileStatus.UNCLAIMED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word is already being processed or claimed!");
		}
	}

	public void validateSubmissionRequest(Game game, MultipartFile file, String object) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is missing!");
		}
		if (object == null || object.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object is missing!");
		}

		int indexOfWord = checkWordList(game.getWordList(), object);
		checkWordTaken(game.getWordListScore(), indexOfWord);
		validateTileAvailable(game, indexOfWord);
	}

	public void markSubmissionProcessing(Game game, String object, String team) {
		int indexOfWord = checkWordList(game.getWordList(), object);
		Tile[][] tileGrid = game.getTileGrid();
		Tile tile = getTileAtIndex(tileGrid, game.getBoardSize(), indexOfWord);
		tile.setStatus(getProcessingStatus(team));
		game.setTileGrid(tileGrid);
		gameRepository.flush();
		pushGameUpdate(game);
	}

	@Async
	public void processSubmissionAsync(UUID gameId, byte[] fileBytes, String object, String team) {
		try {
			processSubmission(gameId, fileBytes, object, team);
		} catch (ResponseStatusException exception) {
			log.debug("Submission for game {} was not applied: {}", gameId, exception.getReason());
		} catch (Throwable exception) {
			log.error("Submission for game {} failed", gameId, exception);
		}
	}

	@Transactional
	public void processSubmission(UUID gameId, byte[] fileBytes, String object, String team) {
		Game game = getGameById(gameId);
		int indexOfWord = checkWordList(game.getWordList(), object);
		Tile[][] tileGrid = game.getTileGrid();
		Tile tile = getTileAtIndex(tileGrid, game.getBoardSize(), indexOfWord);
		TileStatus processingStatus = getProcessingStatus(team);

		if (tile.getStatus() != processingStatus) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Tile is no longer reserved for this submission!");
		}

		try {
			if (VisionQuickstartObjectLocalization.analyzeimage(fileBytes, object) != 1) {
				resetTileStatus(game, indexOfWord);
				return;
			}

			// Hand control back to the existing claim logic from the synchronous flow.
			tile.setStatus(TileStatus.UNCLAIMED);
			game.setTileGrid(tileGrid);
			game.getWordListScore().set(indexOfWord, "1");

			// Claim tile and recompute score state through the existing game services.
			scoreService.claimTile(game, indexOfWord, team);
			leaderboardService.updateLeaderboard(game);

			gameRepository.flush();
			pushGameUpdate(game);
		} catch (Throwable exception) {
			resetTileStatus(game, indexOfWord);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error with image type!");
		}
	}

	private Tile getTileAtIndex(Tile[][] tileGrid, int boardSize, int tileIndex) {
		int row = tileIndex / boardSize;
		int col = tileIndex % boardSize;
		return tileGrid[row][col];
	}

	private Tile getTileAtIndex(Game game, int tileIndex) {
		return getTileAtIndex(game.getTileGrid(), game.getBoardSize(), tileIndex);
	}

	private TileStatus getProcessingStatus(String team) {
		if ("1".equals(team)) {
			return TileStatus.PROCESSING_TEAM1;
		}
		if ("2".equals(team)) {
			return TileStatus.PROCESSING_TEAM2;
		}

		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not in Game!");
	}

	private void resetTileStatus(Game game, int tileIndex) {
		Tile[][] tileGrid = game.getTileGrid();
		Tile tile = getTileAtIndex(tileGrid, game.getBoardSize(), tileIndex);
		tile.setStatus(TileStatus.UNCLAIMED);
		game.setTileGrid(tileGrid);
		gameRepository.flush();
		pushGameUpdate(game);
	}

}
