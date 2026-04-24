package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the GameService.
 *
 * @see GameService
 */
@WebAppConfiguration
@SpringBootTest
public class GameServiceIntegrationTest {

	@Qualifier("gameRepository")
	@Autowired
	private GameRepository gameRepository;

	@Qualifier("lobbyRepository")
	@Autowired
	private LobbyRepository lobbyRepository;

	@Autowired
	private GameService gameService;

	private Lobby testLobby;

	@BeforeEach
	public void setup() {
		gameRepository.deleteAll();
		lobbyRepository.deleteAll();

		testLobby = new Lobby();
		testLobby.setJoinCode("TEST123");
		testLobby.setGameDuration(120);
		testLobby = lobbyRepository.save(testLobby);
	}

	@Test
	public void createGame_validLobby_success() {
		// given
		assertNull(gameRepository.findById(testLobby.getId()).orElse(null));

		// when
		Game createdGame = gameService.createGame(testLobby);

		// then
		assertNotNull(createdGame);
		assertNotNull(createdGame.getId());
		assertEquals(GameStatus.IN_PROGRESS, createdGame.getStatus());
		assertEquals(testLobby.getId(), createdGame.getLobbyId());
		assertEquals(16, createdGame.getWordList().size());
		assertEquals(16, createdGame.getWordListScore().size());
		assertEquals(0, createdGame.getScore_1());
		assertEquals(0, createdGame.getScore_2());
		assertEquals(4, createdGame.getBoardSize());
	}

	@Test
	public void getGames_multipleGames_returnsAll() {
		// given
		Game game1 = gameService.createGame(testLobby);
		
		// when
		List<Game> games = gameService.getGames();

		// then
		assertTrue(games.size() >= 1);
	}

	@Test
	public void getGameById_validId_returnsGame() {
		// given
		Game createdGame = gameService.createGame(testLobby);

		// when
		Game retrievedGame = gameService.getGameById(createdGame.getId());

		// then
		assertNotNull(retrievedGame);
		assertEquals(createdGame.getId(), retrievedGame.getId());
		assertEquals(GameStatus.IN_PROGRESS, retrievedGame.getStatus());
	}

	@Test
	public void getGameById_invalidId_throwsNotFoundException() {
		// when & then
		assertThrows(ResponseStatusException.class, () -> gameService.getGameById(UUID.randomUUID()));
	}

	@Test
	public void deleteGame_validGame_deletesSuccessfully() {
		// given
		Game createdGame = gameService.createGame(testLobby);
		assertNotNull(gameRepository.findById(createdGame.getId()).orElse(null));

		// when
		gameService.deleteGame(createdGame);

		// then
		assertTrue(gameRepository.findById(createdGame.getId()).isEmpty());
	}
}
