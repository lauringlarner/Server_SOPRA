package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameModeDTO;

class GameModeServiceTest {

    private final GameModeService gameModeService = new GameModeService();

    @Test
    void getAllGameModes_returnsList() {
        List<GameModeDTO> result = gameModeService.getAllGameModes();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Lockout_bonus_points", result.get(0).getId());
        assertEquals("Lockout Bingo with bonus points", result.get(0).getName());
        assertEquals(4, result.get(0).getRules().size());
    }

    @Test
    void getGameModeById_validId_returnsGameMode() {
        GameModeDTO result = gameModeService.getGameModeById("Lockout_bonus_points");

        assertNotNull(result);
        assertEquals("Lockout_bonus_points", result.getId());
        assertEquals("Lockout Bingo with bonus points", result.getName());
    }

    @Test
    void getGameModeById_validIdDifferentCase_returnsGameMode() {
        GameModeDTO result = gameModeService.getGameModeById("lockout_bonus_points");

        assertNotNull(result);
        assertEquals("Lockout_bonus_points", result.getId());
    }

    @Test
    void getGameModeById_invalidId_throwsNotFound() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameModeService.getGameModeById("INVALID_ID")
        );

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("GameMode rules not found"));
    }
}