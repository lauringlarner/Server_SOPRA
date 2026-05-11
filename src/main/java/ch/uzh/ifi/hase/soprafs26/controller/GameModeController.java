package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameModeDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameModeService;

@RestController
public class GameModeController {
    
	private final AuthService authService;
    private final GameModeService gameModeService;

	GameModeController(AuthService authService, GameModeService gameModeService) {
		this.authService = authService;
        this.gameModeService = gameModeService;
	}

    @GetMapping("gameModes")
    @ResponseStatus(HttpStatus.OK)
    public List<GameModeDTO> getAllGameModes(@RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		authService.authenticateToken(token);

        return gameModeService.getAllGameModes();
    }


    @GetMapping("gameModes/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GameModeDTO getGameMode(@RequestHeader(value = "Authorization", required = false) String token, @PathVariable String id) {
        // Check if user is authenticated
		authService.authenticateToken(token);

        // return gameMode rule by id or NOT FOUND
        return gameModeService.getGameModeById(id);
    }
}
