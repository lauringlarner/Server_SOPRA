package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    // ─────────────────────────────────────────────
    // POST /lobbies/{lobbyId}/games/{gameId}/leaderboard
    // ─────────────────────────────────────────────

    @Test
    public void postLeaderboard_validRequest_201() throws Exception {
        // given
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(gameId);
        leaderboard.setTeam1Score(10);
        leaderboard.setTeam2Score(20);
        leaderboard.setTileGrid(new Tile[4][4]);

        LeaderboardPostDTO leaderboardPostDTO = new LeaderboardPostDTO();

        given(gameOrchestrationService.getGameById(gameId)).willReturn(testGame);
        given(leaderboardService.initOrUpdate(org.mockito.ArgumentMatchers.any(Game.class))).willReturn(leaderboard);

        // when
        MockHttpServletRequestBuilder postRequest = post("/lobbies/" + lobbyId + "/games/" + gameId + "/leaderboard")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(leaderboardPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId", is(gameId.toString())))
                .andExpect(jsonPath("$.team1Score", is(10)))
                .andExpect(jsonPath("$.team2Score", is(20)));
    }

    @Test
    public void postLeaderboard_gameNotFound_404() throws Exception {
        // given
        LeaderboardPostDTO leaderboardPostDTO = new LeaderboardPostDTO();
        
        given(gameOrchestrationService.getGameById(gameId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/lobbies/" + lobbyId + "/games/" + gameId + "/leaderboard")
                .header("Authorization", "Bearer token123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(leaderboardPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void postLeaderboard_unauthorized_403() throws Exception {
        // given
        LeaderboardPostDTO leaderboardPostDTO = new LeaderboardPostDTO();
        
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/lobbies/" + lobbyId + "/games/" + gameId + "/leaderboard")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(leaderboardPostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // GET /lobbies/{lobbyId}/games/{gameId}/leaderboard
    // ─────────────────────────────────────────────

    @Test
    public void getLeaderboard_validRequest_200() throws Exception {
        // given
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(gameId);
        leaderboard.setTeam1Score(50);
        leaderboard.setTeam2Score(30);
        leaderboard.setTileGrid(new Tile[4][4]);

        given(gameOrchestrationService.getLeaderboard(eq(testUser), eq(lobbyId), eq(gameId)))
                .willReturn(leaderboard);

        // when
        MockHttpServletRequestBuilder getRequest = get("/lobbies/" + lobbyId + "/games/" + gameId + "/leaderboard")
                .header("Authorization", "Bearer token123");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(gameId.toString())))
                .andExpect(jsonPath("$.team1Score", is(50)))
                .andExpect(jsonPath("$.team2Score", is(30)));
    }

    @Test
    public void getLeaderboard_notFound_404() throws Exception {
        // given
        given(gameOrchestrationService.getLeaderboard(eq(testUser), eq(lobbyId), eq(gameId)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard not found"));

        // when
        MockHttpServletRequestBuilder getRequest = get("/lobbies/" + lobbyId + "/games/" + gameId + "/leaderboard")
                .header("Authorization", "Bearer token123");

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getLeaderboard_unauthorized_403() throws Exception {
        // given
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized"));

        // when
        MockHttpServletRequestBuilder getRequest = get("/lobbies/" + lobbyId + "/games/" + gameId + "/leaderboard")
                .header("Authorization", "Bearer bad-token");

        // then
        mockMvc.perform(getRequest)
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
