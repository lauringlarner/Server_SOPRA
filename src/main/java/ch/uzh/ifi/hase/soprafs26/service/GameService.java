package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

	private final PusherService pusherService;

    private final Logger log = LoggerFactory.getLogger(GameService.class);

	private final GameRepository gameRepository;
	private final ScoreService scoreService;
	private final LeaderboardService leaderboardService;
	private final LobbyService lobbyService;


	public GameService(@Qualifier("gameRepository") GameRepository gameRepository,
					   	ScoreService scoreService,
					   	LeaderboardService leaderboardService, 
						PusherService pusherService,
						LobbyService lobbyService) {
		this.gameRepository = gameRepository;
		this.scoreService = scoreService;
		this.leaderboardService = leaderboardService;
        this.pusherService = pusherService;
        this.lobbyService = lobbyService;
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
				String word;

				do {
            		word = Words.Word();
        		} while (wordList.contains(word));

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
		newGame.setStartedAt(Instant.now());

		newGame.setBoardSize(boardSize);
		newGame.setTileGrid(tileGrid);

		newGame = gameRepository.save(newGame);
		gameRepository.flush();

		pushGameUpdate(newGame);

		log.debug("Created Information for Game: {}", newGame);
		return newGame;
	}

	///////////////
	// Retrieval //	
	///////////////

	public List<Game> getGames() {
		return this.gameRepository.findAll();
	}

	public List<Game> getGamesByStatus(GameStatus status) {
		return gameRepository.findAllByStatus(status);
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

	public synchronized boolean finishGameIfExpired(UUID gameId) {
		Game game = getGameById(gameId);
		if (game.getStatus() != GameStatus.IN_PROGRESS) {
			return false;
		}
		if (!isExpired(game, Instant.now())) {
			return false;
		}

		clearProcessingTiles(game);
		game.setStatus(GameStatus.ENDED);
		leaderboardService.initOrUpdate(game);
		gameRepository.flush();
		pushGameUpdate(game);
		lobbyService.resetLobbyAfterGame(game.getLobbyId());

		log.debug("Game {} ended because the timer expired", gameId);
		return true;
	}

	@Scheduled(fixedRate = 1000)
	public void finishExpiredGames() {
		Instant now = Instant.now();
		for (Game game : getGamesByStatus(GameStatus.IN_PROGRESS)) {
			if (isExpired(game, now)) {
				finishGameIfExpired(game.getId());
			}
		}
	}
	
	////////////////
	// Validation //
	////////////////

    ///////////////
    // Utilities //    
    ///////////////
	
	
    ////////////
    // Pusher //
    ////////////

	public void pushGameUpdate(Game game) {
		GameDTO gameDTO = DTOMapper.INSTANCE.convertEntityToGameDTO(game);
		pusherService.trigger("game-" + game.getId(), "GameUpdate", gameDTO);
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
		validateGameIsActive(game);

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
		validateGameIsActive(game);
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
			boolean objectDetected = VisionQuickstartObjectLocalization.analyzeimage(fileBytes, object) == 1;
			processSubmissionResult(gameId, object, team, objectDetected);
			log.debug("Submission for game {} succeeded", gameId);
		} catch (ResponseStatusException exception) {
			log.debug("Submission for game {} was not applied: {}", gameId, exception.getReason());
		} catch (Throwable exception) {
			resetSubmissionIfActive(gameId, object, team);
			log.error("Submission for game {} failed", gameId, exception);
		}
	}

	public void processSubmissionResult(UUID gameId, String object, String team, boolean objectDetected) {
		Game game = getGameById(gameId);
		validateGameIsActive(game);

		int indexOfWord = checkWordList(game.getWordList(), object);
		Tile[][] tileGrid = game.getTileGrid();
		Tile tile = getTileAtIndex(tileGrid, game.getBoardSize(), indexOfWord);
		TileStatus processingStatus = getProcessingStatus(team);

		if (tile.getStatus() != processingStatus) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Tile is no longer reserved for this submission!");
		}

		if (!objectDetected) {
			resetTileStatus(game, indexOfWord);
			return;
		}

		tile.setStatus(TileStatus.UNCLAIMED);
		game.setTileGrid(tileGrid);
		game.getWordListScore().set(indexOfWord, "1");

		scoreService.claimTile(game, indexOfWord, team);
		leaderboardService.updateLeaderboard(game);

		gameRepository.flush();
		pushGameUpdate(game);
	}

	private void resetSubmissionIfActive(UUID gameId, String object, String team) {
		Game game = getGameById(gameId);
		if (game.getStatus() != GameStatus.IN_PROGRESS) {
			return;
		}
		if (isExpired(game, Instant.now())) {
			finishGameIfExpired(gameId);
			return;
		}

		int indexOfWord = checkWordList(game.getWordList(), object);
		Tile tile = getTileAtIndex(game, indexOfWord);
		if (tile.getStatus() != getProcessingStatus(team)) {
			return;
		}

		resetTileStatus(game, indexOfWord);
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

	private void validateGameIsActive(Game game) {
		if (game.getStatus() != GameStatus.IN_PROGRESS) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has already ended.");
		}

		if (isExpired(game, Instant.now())) {
			finishGameIfExpired(game.getId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has already ended.");
		}
	}

	private boolean isExpired(Game game, Instant now) {
		if (game.getStartedAt() == null || game.getGameDuration() == null) {
			return false;
		}

		long totalSeconds = Duration.ofMinutes(game.getGameDuration()).getSeconds();
		long elapsedSeconds = Duration.between(game.getStartedAt(), now).getSeconds();
		return elapsedSeconds >= totalSeconds;
	}

	private void clearProcessingTiles(Game game) {
		Tile[][] tileGrid = game.getTileGrid();
		boolean changed = false;

		for (Tile[] row : tileGrid) {
			for (Tile tile : row) {
				if (tile.getStatus() == TileStatus.PROCESSING_TEAM1 || tile.getStatus() == TileStatus.PROCESSING_TEAM2) {
					tile.setStatus(TileStatus.UNCLAIMED);
					changed = true;
				}
			}
		}

		if (changed) {
			game.setTileGrid(tileGrid);
		}
	}

}
