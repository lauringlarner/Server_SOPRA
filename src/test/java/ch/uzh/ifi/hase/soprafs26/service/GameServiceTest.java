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
        testLobby.setListType("all");

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
    public void createGame_wordListHasNoBlankEntries() {
        given(gameRepository.save(any(Game.class))).willAnswer(inv -> {
            Game g = inv.getArgument(0);
            g.setId(gameId);
            return g;
        });
        doNothing().when(pusherService).trigger(anyString(), anyString(), any());

        Game result = gameService.createGame(testLobby);

        assertTrue(result.getWordList().stream().noneMatch(word -> word == null || word.isBlank()));
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
}
