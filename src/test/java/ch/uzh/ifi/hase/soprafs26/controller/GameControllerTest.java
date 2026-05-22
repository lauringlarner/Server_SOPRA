package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameOrchestrationService;
import ch.uzh.ifi.hase.soprafs26.service.LeaderboardService;

/**
 * GameControllerTest
 * WebMvcTest for the GameController, covering GET/POST/DELETE
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

        given(authService.authenticateToken(any())).willReturn(testUser);
    }

    // ─────────────────────────────────────────────
    // GET /games/{gameId}
    // ─────────────────────────────────────────────

    @Test
    public void getGame_validId_200() throws Exception {
        given(gameOrchestrationService.getGame(testUser, gameId)).willReturn(new GameDTO());

        MockHttpServletRequestBuilder getRequest = get("/games/" + gameId)
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk());
    }

    @Test
    public void getGame_invalidId_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        given(gameOrchestrationService.getGame(eq(testUser), eq(unknownId)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        MockHttpServletRequestBuilder getRequest = get("/games/" + unknownId)
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getGame_noAuthHeader_stillDelegatesToService() throws Exception {
        given(authService.authenticateToken(isNull())).willReturn(testUser);
        given(gameOrchestrationService.getGame(testUser, gameId)).willReturn(new GameDTO());

        mockMvc.perform(get("/games/" + gameId))
                .andExpect(status().isOk());
    }

    @Test
    public void getGame_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/games/" + gameId)
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // DELETE /games/{gameId}
    // ─────────────────────────────────────────────

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

    @Test
    public void deleteGame_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized"));

        mockMvc.perform(delete("/games/" + gameId)
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // POST /games/{gameId}/submission
    // ─────────────────────────────────────────────

    @Test
    public void submitImage_validRequest_202() throws Exception {
        doNothing().when(gameOrchestrationService)
                .submitImageAsync(eq(testUser), eq(gameId), any(), eq("word0"));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isAccepted());
    }

    @Test
    public void submitImage_gameNotFound_404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"))
                .when(gameOrchestrationService)
                .submitImageAsync(eq(testUser), eq(unknownId), any(), anyString());

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + unknownId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void submitImage_wordAlreadyTaken_400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word is already taken by a team!"))
                .when(gameOrchestrationService)
                .submitImageAsync(eq(testUser), eq(gameId), any(), eq("word0"));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submitImage_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized"));

        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .file(file)
                        .param("object", "word0")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void submitImage_missingFile_400() throws Exception {
        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .param("object", "word0")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void submitImage_missingObject_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-bytes".getBytes());

        mockMvc.perform(multipart("/games/" + gameId + "/submission")
                        .file(file)
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // POST /games/{gameId}/leaderboard
    // ─────────────────────────────────────────────

    @Test
    public void postLeaderboard_validRequest_201() throws Exception {
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(gameId);
        leaderboard.setTeam1Score(5);
        leaderboard.setTeam2Score(3);
        leaderboard.setIsSinglePlayer(false);
        leaderboard.setTileGrid(testGame.getTileGrid());

        given(gameOrchestrationService.getGameById(gameId)).willReturn(testGame);
        given(leaderboardService.initOrUpdate(testGame)).willReturn(leaderboard);

        LeaderboardPostDTO postDTO = new LeaderboardPostDTO();

        mockMvc.perform(post("/games/" + gameId + "/leaderboard")
                        .contentType("application/json")
                        .content(asJsonString(postDTO))
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isCreated());
    }

     @Test
     public void postLeaderboard_gameNotFound_404() throws Exception {
        given(gameOrchestrationService.getGameById(gameId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/games/" + gameId + "/leaderboard")
                        .contentType("application/json")
                        .content(asJsonString(new LeaderboardPostDTO()))
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void postLeaderboard_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/games/" + gameId + "/leaderboard")
                        .contentType("application/json")
                        .content(asJsonString(new LeaderboardPostDTO()))
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void postLeaderboard_missingBody_400() throws Exception {
        mockMvc.perform(post("/games/" + gameId + "/leaderboard")
                        .contentType("application/json")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────
    // GET /games/{gameId}/leaderboard
    // ─────────────────────────────────────────────

    @Test
    public void getLeaderboard_validRequest_200() throws Exception {
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(gameId);
        leaderboard.setTeam1Score(10);
        leaderboard.setTeam2Score(8);
        leaderboard.setIsSinglePlayer(false);
        leaderboard.setTileGrid(testGame.getTileGrid());

        given(gameOrchestrationService.getLeaderboard(gameId))
                .willReturn(leaderboard);

        mockMvc.perform(get("/games/" + gameId + "/leaderboard")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk());
    }

    @Test
    public void getLeaderboard_notFound_404() throws Exception {
        given(gameOrchestrationService.getLeaderboard(gameId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/games/" + gameId + "/leaderboard")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getLeaderboard_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/games/" + gameId + "/leaderboard")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created. %s", e.toString()));
        }
    }
}
