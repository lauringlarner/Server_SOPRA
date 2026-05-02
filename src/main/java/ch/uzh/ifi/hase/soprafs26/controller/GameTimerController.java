package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameTimerDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameTimerService;

@RestController
public class GameTimerController {

    private final GameTimerService gameTimerService;
    private final AuthService authService;

    public GameTimerController(GameTimerService gameTimerService, AuthService authService) {
        this.gameTimerService = gameTimerService;
        this.authService = authService;
    }

    @PostMapping("/games/{gameId}/timer/start")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameTimerDTO startTimer(
            @PathVariable UUID gameId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);

        GameTimer timer = gameTimerService.startTimer(gameId);

        GameTimerDTO dto = new GameTimerDTO();
        dto.setStatus(timer.getStatus());
        dto.setRemainingMinutes(gameTimerService.getRemainingMinutes(timer));
        return dto;
    }

    // no need to auth
    // Mapping change from /lobbies/{lobbyId}/games/{gameId}/leaderboard to /games/{gameId}/leaderboard

    @GetMapping("/games/{gameId}/timer")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameTimerDTO getTimer(
            @PathVariable UUID gameId) {
        GameTimer timer = gameTimerService.getTimerByGameId(gameId);

        GameTimerDTO dto = new GameTimerDTO();
        dto.setStatus(timer.getStatus());
        dto.setRemainingMinutes(gameTimerService.getRemainingMinutes(timer));
        return dto;
    }
}
