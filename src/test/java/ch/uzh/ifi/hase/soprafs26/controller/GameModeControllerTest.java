package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameModeDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameModeService;

@WebMvcTest(GameModeController.class)
public class GameModeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private GameModeService gameModeService;

    // ─────────────────────────────────────────────
    // GET /gameModes
    // ─────────────────────────────────────────────

    @Test
    public void getAllGameModes_validToken_200() throws Exception {
        GameModeDTO dto = new GameModeDTO(
                "Lockout_bonus_points",
                "Lockout Bingo with bonus points",
                List.of("rule1", "rule2")
        );

        given(authService.authenticateToken(any())).willReturn(null);
        given(gameModeService.getAllGameModes()).willReturn(List.of(dto));

        mockMvc.perform(get("/gameModes")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllGameModes_noAuthHeader_stillWorks_200() throws Exception {
        given(authService.authenticateToken(any())).willReturn(null);
        given(gameModeService.getAllGameModes()).willReturn(List.of());

        mockMvc.perform(get("/gameModes"))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllGameModes_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/gameModes")
                        .header("Authorization", "bad-token"))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────
    // GET /gameModes/{id}
    // ─────────────────────────────────────────────

    @Test
    public void getGameMode_validId_200() throws Exception {
        GameModeDTO dto = new GameModeDTO(
                "Lockout_bonus_points",
                "Lockout Bingo with bonus points",
                List.of("rule1", "rule2")
        );

        given(authService.authenticateToken(any())).willReturn(null);
        given(gameModeService.getGameModeById("Lockout_bonus_points")).willReturn(dto);

        mockMvc.perform(get("/gameModes/Lockout_bonus_points")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isOk());
    }

    @Test
    public void getGameMode_invalidId_404() throws Exception {
        given(authService.authenticateToken(any())).willReturn(null);

        given(gameModeService.getGameModeById("invalid"))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/gameModes/invalid")
                        .header("Authorization", "Bearer token123"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getGameMode_unauthorized_403() throws Exception {
        given(authService.authenticateToken(any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/gameModes/Lockout_bonus_points")
                        .header("Authorization", "bad-token"))
                .andExpect(status().isForbidden());
    }
}