package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameOrchestrationService;
import ch.uzh.ifi.hase.soprafs26.service.LeaderboardService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GameControllerTest
 * WebMvcTest for the GameController, covering GET/POST/PUT/DELETE
 * requests without sending them over the network.
 */
@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameOrchestrationService gameOrchestrationService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LeaderboardService leaderboardService;

    private Game testGame;
    private UUID gameId;
    private UUID lobbyId;
    private User testUser;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        lobbyId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testUser");

        testGame = new Game();
        testGame.setId(gameId);
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setLobbyId(lobbyId);
        testGame.setScore_1(0);
        testGame.setScore_2(0);
        testGame.setGameDuration(120);
        testGame.setBoardSize(8);

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

        // Configure authService to return testUser for any token
        given(authService.authenticateToken(any())).willReturn(testUser);
    }

    // GET /games/{gameId}/stream  (SSE)

    @Test
    public void getGameStream_validId_200() throws Exception {
        SseEmitter emitter = new SseEmitter();
        given(gameOrchestrationService.startGameStream(testUser, gameId)).willReturn(emitter);

        MockHttpServletRequestBuilder getRequest = get("/games/" + gameId + "/stream")
                .header("Authorization", "Bearer token123")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void getGameStream_invalidId_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(gameOrchestrationService.startGameStream(eq(testUser), eq(unknownId)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        MockHttpServletRequestBuilder getRequest = get("/games/" + unknownId + "/stream")
                .header("Authorization", "Bearer token123")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    // DELETE /games/{gameId}

    @Test
    public void deleteGame_validId_204() throws Exception {
        doNothing().when(gameOrchestrationService).deleteGame(testUser, gameId);

        MockHttpServletRequestBuilder deleteRequest = delete("/games/" + gameId)
                .header("Authorization", "Bearer token123");

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void deleteGame_invalidId_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"))
                .when(gameOrchestrationService).deleteGame(eq(testUser), eq(unknownId));

        MockHttpServletRequestBuilder deleteRequest = delete("/games/" + unknownId)
                .header("Authorization", "Bearer token123");

        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }

    // POST /games/{gameId}/submission  (image submission)

    @Test
    public void submitImage_objectFound_201() throws Exception {
        given(gameOrchestrationService.submitImage(eq(testUser), eq(gameId), any(), eq("word0"), eq("team1")))
                .willReturn(1);

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .param("team", "team1")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.found", is(1)));
    }

    @Test
    public void submitImage_objectNotFound_201WithZero() throws Exception {
        given(gameOrchestrationService.submitImage(eq(testUser), eq(gameId), any(), eq("word0"), eq("team1")))
                .willReturn(0);

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .param("team", "team1")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.found", is(0)));
    }

    @Test
    public void submitImage_gameNotFound_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(gameOrchestrationService.submitImage(eq(testUser), eq(unknownId), any(), anyString(), anyString()))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + unknownId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .param("team", "team1")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    // POST /games/{gameId}/leaderboard

    @Test
    public void postLeaderboard_validRequest_201() throws Exception {
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(gameId);
        leaderboard.setTeam1Score(5);
        leaderboard.setTeam2Score(3);
        leaderboard.setTileGrid(testGame.getTileGrid());

        given(gameOrchestrationService.getGameById(gameId)).willReturn(testGame);
        given(leaderboardService.initOrUpdate(testGame)).willReturn(leaderboard);

        LeaderboardPostDTO postDTO = new LeaderboardPostDTO();

        mockMvc.perform(post("/games/" + lobbyId + "/" + gameId + "/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(postDTO))
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId", is(gameId.toString())))
                .andExpect(jsonPath("$.team1Score", is(5)))
                .andExpect(jsonPath("$.team2Score", is(3)));
    }

    @Test
    public void postLeaderboard_gameNotFound_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(gameOrchestrationService.getGameById(unknownId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        LeaderboardPostDTO postDTO = new LeaderboardPostDTO();

        mockMvc.perform(post("/games/" + lobbyId + "/" + unknownId + "/leaderboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(postDTO))
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    // GET /games/{gameId}/leaderboard

    @Test
    public void getLeaderboard_validRequest_200() throws Exception {
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(gameId);
        leaderboard.setTeam1Score(5);
        leaderboard.setTeam2Score(3);
        leaderboard.setTileGrid(testGame.getTileGrid());

        given(leaderboardService.getLeaderboard(gameId)).willReturn(leaderboard);

        mockMvc.perform(get("/games/" + lobbyId + "/" + gameId + "/leaderboard")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(gameId.toString())))
                .andExpect(jsonPath("$.team1Score", is(5)))
                .andExpect(jsonPath("$.team2Score", is(3)));
    }

    @Test
    public void getLeaderboard_notFound_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(leaderboardService.getLeaderboard(unknownId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard not found"));

        mockMvc.perform(get("/games/" + lobbyId + "/" + unknownId + "/leaderboard")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    // Helpers

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
