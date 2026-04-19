package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameOrchestrationService;
import ch.uzh.ifi.hase.soprafs26.service.LeaderboardService;


/**
 * Game Controller
 * This class is responsible for handling all REST requests related to
 * the game.
 * The controller will receive the request and delegate the execution to the
 * GameService and finally return the result.
 */
@RestController
public class GameController {

    private final AuthService authService;
    private final GameOrchestrationService gameOrchestrationService;
    private final LeaderboardService leaderboardService;

    GameController(AuthService authService, GameOrchestrationService gameOrchestrationService, LeaderboardService leaderboardService) {
        this.authService = authService;
        this.gameOrchestrationService = gameOrchestrationService;
        this.leaderboardService = leaderboardService;
    }


    @GetMapping("/games/{gameId}/stream")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public SseEmitter getGameByIdEmitter(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        User user = authService.authenticateToken(token);
        return gameOrchestrationService.startGameStream(user, gameId);
    }


    @DeleteMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteGame(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        User user = authService.authenticateToken(token);
        gameOrchestrationService.deleteGame(user, gameId);
    }


    @PostMapping("/games/{gameId}/submission")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void analyze(@RequestParam("image") MultipartFile file,
        @RequestParam("object") String object,
        @PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) throws Exception {
        User user = authService.authenticateToken(token);
        gameOrchestrationService.submitImageAsync(user, gameId, file, object);
    }


    @PostMapping("/lobbies/{lobbyId}/games/{gameId}/leaderboard")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LeaderboardGetDTO postLeaderboard(@PathVariable UUID lobbyId,
                                             @PathVariable UUID gameId,
                                             @RequestBody LeaderboardPostDTO leaderboardPostDTO,
                                             @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        Game game = gameOrchestrationService.getGameById(gameId);
        Leaderboard leaderboard = leaderboardService.initOrUpdate(game);

        LeaderboardGetDTO dto = new LeaderboardGetDTO(leaderboard.getGameId());
        dto.setTeam1Score(leaderboard.getTeam1Score());
        dto.setTeam2Score(leaderboard.getTeam2Score());
        dto.setTileGrid(leaderboard.getTileGrid());
        return dto;
    }


    @GetMapping("/lobbies/{lobbyId}/games/{gameId}/leaderboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LeaderboardGetDTO getLeaderboard(@PathVariable UUID lobbyId,
                                            @PathVariable UUID gameId,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        Leaderboard leaderboard = leaderboardService.getLeaderboard(gameId);

        LeaderboardGetDTO dto = new LeaderboardGetDTO(leaderboard.getGameId());
        dto.setTeam1Score(leaderboard.getTeam1Score());
        dto.setTeam2Score(leaderboard.getTeam2Score());
        dto.setTileGrid(leaderboard.getTileGrid());
        return dto;
    }
}
