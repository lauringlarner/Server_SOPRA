package ch.uzh.ifi.hase.soprafs26.controller;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TimerStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameTimerService;

@WebMvcTest(GameTimerController.class)
public class GameTimerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameTimerService gameTimerService;

    @MockitoBean
    private AuthService authService;

    private UUID gameId;
    private GameTimer timer;

    @BeforeEach
    void setup() {
        gameId = UUID.randomUUID();

        timer = new GameTimer();
        timer.setGameId(gameId);
        timer.setDurationMinutes(30);
        timer.setStatus(TimerStatus.NOT_STARTED);

        given(authService.authenticateToken(any())).willReturn(null);
    }

    // ─────────────────────────────────────────────
    // POST /games/{gameId}/timer/start
    // ─────────────────────────────────────────────

    @Test
    public void startTimer_valid_200() throws Exception {
        GameTimer running = new GameTimer();
        running.setGameId(gameId);
        running.setDurationMinutes(30);
        running.setStatus(TimerStatus.RUNNING);
        running.setStartedAt(Instant.now());

        given(gameTimerService.startTimer(gameId)).willReturn(running);
        given(gameTimerService.getRemainingMinutes(running)).willReturn(29);

        mockMvc.perform(post("/games/" + gameId + "/timer/start")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk());
    }

    @Test
    public void startTimer_alreadyStarted_409() throws Exception {
        given(gameTimerService.startTimer(gameId))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mockMvc.perform(post("/games/" + gameId + "/timer/start")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isConflict());
    }

    @Test
    public void startTimer_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/games/" + gameId + "/timer/start")
                        .header("Authorization", "bad-token"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // GET /games/{gameId}/timer
    // ─────────────────────────────────────────────

    @Test
    public void getTimer_valid_200() throws Exception {
        GameTimer running = new GameTimer();
        running.setGameId(gameId);
        running.setDurationMinutes(30);
        running.setStatus(TimerStatus.RUNNING);
        running.setStartedAt(Instant.now());

        given(gameTimerService.getTimerByGameId(gameId)).willReturn(running);
        given(gameTimerService.getRemainingMinutes(running)).willReturn(25);

        mockMvc.perform(get("/games/" + gameId + "/timer"))
                .andExpect(status().isOk());
    }

    @Test
    public void getTimer_notFound_404() throws Exception {
        given(gameTimerService.getTimerByGameId(gameId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/games/" + gameId + "/timer"))
                .andExpect(status().isNotFound());
    }
}