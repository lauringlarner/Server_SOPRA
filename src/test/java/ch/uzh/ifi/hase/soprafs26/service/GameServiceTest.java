package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private LobbyService lobbyService;
    
    @Mock
    private GameRepository gameRepository;

    @Mock
    private ScoreService scoreService;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private PusherService pusherService;


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
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

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
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

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
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        Game result = gameService.createGame(testLobby);

        for (String score : result.getWordListScore()) {
            assertEquals("0", score);
        }
    }

    @Test
    public void createGame_gameDurationFromLobby() {
        testLobby.setGameDuration(300);
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        Game result = gameService.createGame(testLobby);

        assertEquals(300, result.getGameDuration());
    }

    @Test
    public void createGame_wordListHasNoDuplicates() {
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        Game result = gameService.createGame(testLobby);

        long distinctCount = result.getWordList().stream().distinct().count();
        assertEquals(16, distinctCount);
    }

    @Test
    public void createGame_pusherTriggered() {
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });

        gameService.createGame(testLobby);

        verify(pusherService, times(1)).trigger(eq("game-" + gameId), eq("GameUpdate"), any(GameDTO.class));
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

    @Test
    public void validateSubmissionRequest_nullObject_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "bytes".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.validateSubmissionRequest(testGame, file, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Object is missing!"));
    }

    @Test
    public void validateSubmissionRequest_blankObject_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "bytes".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.validateSubmissionRequest(testGame, file, "   "));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Object is missing!"));
    }

    @Test
    public void validateSubmissionRequest_wordNotInGame_throwsBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "bytes".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.validateSubmissionRequest(testGame, file, "unknownWord"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Object is not in the Game!"));
    }

    @Test
    public void validateSubmissionRequest_wordAlreadyTaken_throwsBadRequest() {
        testGame.getWordListScore().set(0, "1"); // word0 already claimed
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "bytes".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.validateSubmissionRequest(testGame, file, "word0"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Word is already taken by a team!"));
    }

    // ─────────────────────────────────────────────
    // markSubmissionProcessing
    // ─────────────────────────────────────────────

    @Test
    public void markSubmissionProcessing_team1_setsProcessingTeam1() {
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        gameService.markSubmissionProcessing(testGame, "word0", "1");

        assertEquals(TileStatus.PROCESSING_TEAM1, testGame.getTileGrid()[0][0].getStatus());
        verify(gameRepository, times(1)).flush();
        verify(pusherService, times(1)).trigger(anyString(), eq("GameUpdate"), any(GameDTO.class));
    }

    @Test
    public void markSubmissionProcessing_team2_setsProcessingTeam2() {
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        gameService.markSubmissionProcessing(testGame, "word0", "2");

        assertEquals(TileStatus.PROCESSING_TEAM2, testGame.getTileGrid()[0][0].getStatus());
        verify(gameRepository, times(1)).flush();
    }

    @Test
    public void markSubmissionProcessing_invalidTeam_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.markSubmissionProcessing(testGame, "word0", "3"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Team not in Game!"));
    }

    @Test
    public void markSubmissionProcessing_wordNotInGame_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.markSubmissionProcessing(testGame, "unknownWord", "1"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Object is not in the Game!"));
    }

    @Test
    public void pushGameUpdate_validGame_triggersPusher() {
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        assertDoesNotThrow(() -> gameService.pushGameUpdate(testGame));

        verify(pusherService, times(1))
                .trigger(eq("game-" + gameId), eq("GameUpdate"), any(GameDTO.class));
    }

    

    // ───────────────────
    // finishGameIfExpired
    // ───────────────────

    @Test
    public void finishGameIfExpired_gameNotInProgress_returnsFalse() {
        // Arrange
        testGame.setStatus(GameStatus.ENDED);
        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));

        // Act
        boolean result = gameService.finishGameIfExpired(gameId);

        // Assert
        assertFalse(result);
        verify(gameRepository, never()).flush();
        verify(leaderboardService, never()).initOrUpdate(any());
    }

    @Test
    public void finishGameIfExpired_notYetExpired_returnsFalse() {
        // Arrange
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setStartedAt(java.time.Instant.now());
        testGame.setGameDuration(10); // 10 minutes
        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));

        // Act
        boolean result = gameService.finishGameIfExpired(gameId);

        // Assert
        assertFalse(result);
        assertEquals(GameStatus.IN_PROGRESS, testGame.getStatus());
    }

    @Test
    public void finishGameIfExpired_isExpired_endsGameAndCleansUp() {
        // Arrange
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setGameDuration(5);
        // Set start time to 6 minutes ago
        java.time.Instant startTime = java.time.Instant.now().minus(java.time.Duration.ofMinutes(6));
        testGame.setStartedAt(startTime);
        
        // Put a tile in processing state to verify clearProcessingTiles works
        testGame.getTileGrid()[0][0].setStatus(TileStatus.PROCESSING_TEAM1);

        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        // Act
        boolean result = gameService.finishGameIfExpired(gameId);

        // Assert
        assertTrue(result);
        assertEquals(GameStatus.ENDED, testGame.getStatus());
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[0][0].getStatus());
        
        verify(leaderboardService, times(1)).initOrUpdate(testGame);
        verify(lobbyService, times(1)).resetLobbyAfterGame(testGame.getLobbyId());
        verify(gameRepository, times(1)).flush();
        verify(pusherService, times(1)).trigger(eq("game-" + gameId), eq("GameUpdate"), any());
    }

    // ─────────────────────────────────────────────
    // validateTileAvailable
    // ─────────────────────────────────────────────

    @Test
    public void validateTileAvailable_tileUnclaimed_noExceptionThrown() {
        // Arrange: testGame is initialized with all tiles as UNCLAIMED in setUp()
        int indexOfWord = 5;

        // Act & Assert
        assertDoesNotThrow(() -> gameService.validateTileAvailable(testGame, indexOfWord));
    }

    @Test
    public void validateTileAvailable_tileAlreadyClaimed_throwsBadRequest() {
        // Arrange
        int indexOfWord = 0;
        
        // Use reflection or the public getter to get the exact tile the service will see
        Tile[][] grid = testGame.getTileGrid();
        grid[0][0].setStatus(TileStatus.PROCESSING_TEAM1); 
        
        // Ensure the game object actually holds this modified grid
        testGame.setTileGrid(grid);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.validateTileAvailable(testGame, indexOfWord),
                "Expected validateTileAvailable to throw, but it passed.");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Word is already being processed or claimed!"));
    }

   

    // ─────────────────────────────────────────────
    // processSubmissionResult
    // ─────────────────────────────────────────────

    @Test
    public void processSubmissionResult_success_claimsTileAndUpdatesScore() {
        // Arrange
        String object = "word0";
        String team = "1";
        
        // 1. Get the grid and the specific tile instance
        Tile[][] grid = testGame.getTileGrid();
        int indexOfWord = 0; // "word0" is at index 0
        int row = indexOfWord / 4;
        int col = indexOfWord % 4;
        
        // 2. Set the status on the EXACT tile the service will retrieve
        grid[row][col].setStatus(TileStatus.PROCESSING_TEAM1);
        
        // 3. Re-set the grid to ensure the Game entity reference is updated
        testGame.setTileGrid(grid);

        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));
        
        // Standard mocking for the other services
        // (Removed doNothing() because mocks do nothing by default)
        
        // Act
        gameService.processSubmissionResult(gameId, object, team, true);

        // Assert
        assertEquals("1", testGame.getWordListScore().get(0));
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[row][col].getStatus());
        verify(scoreService).claimTile(testGame, 0, team);
    }

    @Test
    public void processSubmissionResult_objectNotDetected_resetsTileStatus() {
        // Arrange
        String object = "word0";
        String team = "1";
        
        // 1. Explicitly grab the grid
        Tile[][] grid = testGame.getTileGrid();
        
        // 2. Set the status on the specific tile instance
        grid[0][0].setStatus(TileStatus.PROCESSING_TEAM1);
        
        // 3. CRITICAL: Re-set the grid back into the game object 
        // This ensures any internal state or reference tracking in the Entity is refreshed
        testGame.setTileGrid(grid);

        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));
        
        // We can omit doNothing() as it's the default for mocks
        // pusherService.trigger is void, so it will do nothing by default

        // Act
        gameService.processSubmissionResult(gameId, object, team, false);

        // Assert
        // The word list score should remain "0" because detection failed
        assertEquals("0", testGame.getWordListScore().get(0));
        
        // The tile should be reset to UNCLAIMED so others can try
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[0][0].getStatus());
        
        verify(scoreService, never()).claimTile(any(), anyInt(), anyString());
        verify(gameRepository, times(1)).flush();
    }

    @Test
    public void processSubmissionResult_wrongStatus_throwsConflict() {
        // Arrange
        String object = "word0";
        String team = "1";
        // Tile is UNCLAIMED instead of PROCESSING_TEAM1
        testGame.getTileGrid()[0][0].setStatus(TileStatus.UNCLAIMED);

        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.processSubmissionResult(gameId, object, team, true));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tile is no longer reserved for this submission!"));
    }

    @Test
    public void processSubmissionResult_gameEnded_throwsConflict() {
        // Arrange
        testGame.setStatus(GameStatus.ENDED);
        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.processSubmissionResult(gameId, "word0", "1", true));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Game has already ended."));
    }

    // ───────────────────────
    // resetSubmissionIfActive 
    // ───────────────────────

    @Test
    public void resetSubmissionIfActive_wrongTileStatus_doesNothing() {
        // Arrange
        given(gameRepository.findById(gameId)).willReturn(Optional.of(testGame));
        // Tile is already unclaimed, so the method should return early
        testGame.getTileGrid()[0][0].setStatus(TileStatus.UNCLAIMED);

        // Act
        ReflectionTestUtils.invokeMethod(gameService, "resetSubmissionIfActive", 
            gameId, "word0", "1");

        // Assert
        verify(gameRepository, never()).flush();
    }

    // ──────────────
    // getTileAtIndex 
    // ──────────────

    @Test
    public void getTileAtIndex_firstTile_returnsCorrectTile() {
        // Arrange
        // Index 0 should map to [0][0]
        Tile expectedTile = testGame.getTileGrid()[0][0];

        // Act
        Tile result = ReflectionTestUtils.invokeMethod(gameService, "getTileAtIndex", 
            testGame, 0);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTile.getWord(), result.getWord());
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(gameService, "checkWordList", 
            testGame.getWordList(), result.getWord()));
    }

    @Test
    public void getTileAtIndex_middleTile_returnsCorrectTile() {
        // Arrange
        // For a 4x4 board, index 6 (row 1, col 2) 
        // Math: 6 / 4 = 1 (row), 6 % 4 = 2 (col)
        Tile expectedTile = testGame.getTileGrid()[1][2];

        // Act
        Tile result = ReflectionTestUtils.invokeMethod(gameService, "getTileAtIndex", 
            testGame, 6);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTile.getWord(), result.getWord());
    }

    @Test
    public void getTileAtIndex_lastTile_returnsCorrectTile() {
        // Arrange
        // Index 15 should map to [3][3]
        Tile expectedTile = testGame.getTileGrid()[3][3];

        // Act
        Tile result = ReflectionTestUtils.invokeMethod(gameService, "getTileAtIndex", 
            testGame, 15);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTile.getWord(), result.getWord());
    }

    // ───────────────
    // resetTileStatus
    // ───────────────

    @Test
    public void resetTileStatus_updatesStatusAndTriggersUpdate() {
        // Arrange
        int indexToReset = 0;
        // Start with the tile in a processing state
        testGame.getTileGrid()[0][0].setStatus(TileStatus.PROCESSING_TEAM1);
        
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        // Act
        ReflectionTestUtils.invokeMethod(gameService, "resetTileStatus", testGame, indexToReset);

        // Assert
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[0][0].getStatus());
        verify(gameRepository, times(1)).flush();
        verify(pusherService, times(1)).trigger(eq("game-" + gameId), eq("GameUpdate"), any(GameDTO.class));
    }

    // ─────────────────────────────────────────────
    // clearProcessingTiles (Private Method)
    // ─────────────────────────────────────────────

    @Test
    public void clearProcessingTiles_resetsAllProcessingStatuses() {
        // Arrange
        // Set up a mix of statuses in the grid
        testGame.getTileGrid()[0][0].setStatus(TileStatus.PROCESSING_TEAM1);
        testGame.getTileGrid()[0][1].setStatus(TileStatus.PROCESSING_TEAM2);
        testGame.getTileGrid()[1][0].setStatus(TileStatus.UNCLAIMED);
        // Note: WordListScore would usually be "1" if claimed, 
        // so we leave a normally claimed tile alone if your logic allows
        
        // Act
        ReflectionTestUtils.invokeMethod(gameService, "clearProcessingTiles", testGame);

        // Assert
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[0][0].getStatus());
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[0][1].getStatus());
        assertEquals(TileStatus.UNCLAIMED, testGame.getTileGrid()[1][0].getStatus());
    }

    @Test
    public void clearProcessingTiles_noChanges_doesNotModifyGrid() {
        // Arrange
        // All tiles are already UNCLAIMED (set in setUp)
        
        // Act
        ReflectionTestUtils.invokeMethod(gameService, "clearProcessingTiles", testGame);

        // Assert
        for (Tile[] row : testGame.getTileGrid()) {
            for (Tile tile : row) {
                assertEquals(TileStatus.UNCLAIMED, tile.getStatus());
            }
        }
        // Verification that the setter wasn't called is harder with Reflection, 
        // but the state remains correct.
    }
}
