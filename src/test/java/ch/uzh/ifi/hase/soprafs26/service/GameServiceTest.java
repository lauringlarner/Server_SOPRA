package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ScoreService scoreService;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private SseService sseService;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private Lobby testLobby;
    private UUID gameId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();

        testLobby = new Lobby();
        testLobby.setId(UUID.randomUUID());
        testLobby.setGameDuration(120);

        testGame = new Game();
        testGame.setId(gameId);
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setLobbyId(testLobby.getId());
        testGame.setScore_1(0);
        testGame.setScore_2(0);
        testGame.setGameDuration(120);
        testGame.setBoardSize(4);

        List<String> wordList = new ArrayList<>();
        List<String> wordListScore = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            wordList.add("word" + i);
            wordListScore.add("0");
        }
        testGame.setWordList(wordList);
        testGame.setWordListScore(wordListScore);

        Tile[][] tileGrid = new Tile[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                tileGrid[i][j] = new Tile(wordList.get(i * 4 + j), 1, TileStatus.UNCLAIMED);
            }
        }
        testGame.setTileGrid(tileGrid);
    }

    // ─────────────────────────────────────────────
    // createGame
    // ─────────────────────────────────────────────

    @Test
    public void createGame_validLobby_returnsGame() {
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> inv.getArgument(0));

        Game result = gameService.createGame(testLobby);

        assertNotNull(result);
        assertEquals(GameStatus.IN_PROGRESS, result.getStatus());
        assertEquals(testLobby.getId(), result.getLobbyId());
        assertEquals(16, result.getWordList().size());
        assertEquals(16, result.getWordListScore().size());
        assertEquals(0, result.getScore_1());
        assertEquals(0, result.getScore_2());
        assertEquals(4, result.getBoardSize());
        assertEquals(120, result.getGameDuration());
        verify(gameRepository, times(1)).save(any(Game.class));
        verify(gameRepository, times(1)).flush();
    }

    @Test
    public void createGame_tileGridInitialized_allUnclaimed() {
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> inv.getArgument(0));

        Game result = gameService.createGame(testLobby);

        Tile[][] grid = result.getTileGrid();
        assertNotNull(grid);
        assertEquals(4, grid.length);
        for (Tile[] row : grid) {
            assertEquals(4, row.length);
            for (Tile tile : row) {
                assertEquals(TileStatus.UNCLAIMED, tile.getStatus());
            }
        }
    }

    @Test
    public void createGame_wordListScoreAllZero_onCreation() {
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> inv.getArgument(0));

        Game result = gameService.createGame(testLobby);

        for (String score : result.getWordListScore()) {
            assertEquals("0", score);
        }
    }

    @Test
    public void createGame_gameDurationFromLobby() {
        testLobby.setGameDuration(300);
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> inv.getArgument(0));

        Game result = gameService.createGame(testLobby);

        assertEquals(300, result.getGameDuration());
    }

    // ─────────────────────────────────────────────
    // getGames
    // ─────────────────────────────────────────────

    @Test
    public void getGames_returnsAllGames() {
        List<Game> games = List.of(testGame, new Game());
        given(gameRepository.findAll()).willReturn(games);

        List<Game> result = gameService.getGames();

        assertEquals(2, result.size());
        verify(gameRepository, times(1)).findAll();
    }

    @Test
    public void getGames_emptyRepository_returnsEmptyList() {
        given(gameRepository.findAll()).willReturn(new ArrayList<>());

        List<Game> result = gameService.getGames();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    // getGameById
    // ─────────────────────────────────────────────

    @Test
    public void getGameById_validId_returnsGame() {
        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));

        Game result = gameService.getGameById(gameId);

        assertNotNull(result);
        assertEquals(gameId, result.getId());
    }

    @Test
    public void getGameById_invalidId_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        given(gameRepository.findById(unknownId)).willReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getGameById(unknownId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Game not found"));
    }

    // ─────────────────────────────────────────────
    // deleteGame
    // ─────────────────────────────────────────────

    @Test
    public void deleteGame_validGame_deletesSuccessfully() {
        doNothing().when(gameRepository).delete(testGame);

        assertDoesNotThrow(() -> gameService.deleteGame(testGame));

        verify(gameRepository, times(1)).delete(testGame);
    }

    // ─────────────────────────────────────────────
    // checkWordList
    // ─────────────────────────────────────────────

    @Test
    public void checkWordList_wordPresent_returnsIndex() {
        List<String> wordList = List.of("apple", "banana", "cherry");

        int index = gameService.checkWordList(wordList, "banana");

        assertEquals(1, index);
    }

    @Test
    public void checkWordList_wordNotPresent_throwsBadRequest() {
        List<String> wordList = List.of("apple", "banana");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.checkWordList(wordList, "mango"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Object is not in the Game!"));
    }

    @Test
    public void checkWordList_firstWord_returnsZero() {
        List<String> wordList = List.of("first", "second");

        int index = gameService.checkWordList(wordList, "first");

        assertEquals(0, index);
    }

    // ─────────────────────────────────────────────
    // checkWordTaken
    // ─────────────────────────────────────────────

    @Test
    public void checkWordTaken_wordNotTaken_returnsOne() {
        List<String> wordListScore = List.of("0", "1", "0");

        int result = gameService.checkWordTaken(wordListScore, 0);

        assertEquals(1, result);
    }

    @Test
    public void checkWordTaken_wordAlreadyTaken_throwsBadRequest() {
        List<String> wordListScore = List.of("0", "1", "0");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.checkWordTaken(wordListScore, 1));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Word is already taken by a team!"));
    }

    // ─────────────────────────────────────────────
    // WordList (static helper)
    // ─────────────────────────────────────────────

    @Test
    public void wordList_returns16Words() {
        List<String> result = GameService.WordList();

        assertNotNull(result);
        assertEquals(16, result.size());
        result.forEach(w -> assertNotNull(w));
    }

    // ─────────────────────────────────────────────
    // imageSubmission
    // ─────────────────────────────────────────────

    @Test
    public void imageSubmission_objectFoundInImage_returnsOne() throws Exception {
        List<String> wordList = new ArrayList<>(List.of("apple", "banana"));
        List<String> wordListScore = new ArrayList<>(List.of("0", "0"));
        testGame.setWordList(wordList);
        testGame.setWordListScore(wordListScore);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        try (MockedStatic<ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization> vision =
                     Mockito.mockStatic(ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization.class)) {

            vision.when(() -> ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization
                    .analyzeimage(any(byte[].class), eq("apple"))).thenReturn(1);

            int result = gameService.imageSubmission(file, "apple", wordListScore, 0, "team1", testGame);

            assertEquals(1, result);
            assertEquals("1", wordListScore.get(0));
            verify(scoreService).claimTile(testGame, 0, "team1");
            verify(leaderboardService).updateLeaderboard(testGame);
        }
    }

    @Test
    public void imageSubmission_objectNotFoundInImage_returnsZero() throws Exception {
        List<String> wordListScore = new ArrayList<>(List.of("0", "0"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        try (MockedStatic<ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization> vision =
                     Mockito.mockStatic(ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization.class)) {

            vision.when(() -> ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization
                    .analyzeimage(any(byte[].class), eq("apple"))).thenReturn(0);

            int result = gameService.imageSubmission(file, "apple", wordListScore, 0, "team1", testGame);

            assertEquals(0, result);
            assertEquals("0", wordListScore.get(0));
            verifyNoInteractions(scoreService, leaderboardService);
        }
    }

    @Test
    public void imageSubmission_exceptionThrown_throwsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.jpg", "image/jpeg", new byte[0]);

        try (MockedStatic<ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization> vision =
                     Mockito.mockStatic(ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization.class)) {

            vision.when(() -> ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization
                    .analyzeimage(any(byte[].class), anyString()))
                    .thenThrow(new RuntimeException("Vision API error"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> gameService.imageSubmission(file, "apple",
                            new ArrayList<>(List.of("0")), 0, "team1", testGame));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Error with image type!"));
        }
    }

    // ─────────────────────────────────────────────
    // SSE functions
    // ─────────────────────────────────────────────

    @Test
    public void createAndRegisterGameStream_validGame_returnsEmitter() throws Exception {
        doNothing().when(sseService).register(any(UUID.class), any(SseEmitter.class));

        SseEmitter emitter = gameService.createAndRegisterGameStream(testGame);

        assertNotNull(emitter);
        verify(sseService, times(1)).register(eq(gameId), any(SseEmitter.class));
    }

    @Test
    public void pushGameUpdate_validGame_callsSseService() {
        doNothing().when(sseService).push(any(UUID.class), anyString(), any());

        assertDoesNotThrow(() -> gameService.pushGameUpdate(testGame));

        verify(sseService, times(1)).push(eq(gameId), eq("gameUpdate"), any(GameDTO.class));
    }
}
