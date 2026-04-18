package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameResultGetDTO;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisResult;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameOrchestrationService;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import java.util.List;

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

    GameController(AuthService authService, GameOrchestrationService gameOrchestrationService) {
        this.authService = authService;
        this.gameOrchestrationService = gameOrchestrationService;

    }
 



    @GetMapping("/games/{gameId}/stream")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public SseEmitter getGameByIdEmitter(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // start game stream
        return gameOrchestrationService.startGameStream(user, gameId);
    }


    @DeleteMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteGame(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        
        // delete game
        gameOrchestrationService.deleteGame(user, gameId);
    }


    @PostMapping("/games/{gameId}/submission")
    @ResponseStatus(HttpStatus.CREATED)
    
    public ImageAnalysisGetDTO analyze(@RequestParam("image") MultipartFile file,
        @RequestParam("object") String object, @RequestParam("team") String team,
        @PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) throws Exception {
        // authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        


        //set word as taken
        //teamscore +=1
        //return 1 if found, 0 if not

        if(gameOrchestrationService.submitImage(user, gameId, file, object, team) == 1)
        {int result = 1;
            return DTOMapper.INSTANCE.convertImageAnalysisResultToGetDTO(new ImageAnalysisResult(result));
        }
        else{
            int result=0;
            return DTOMapper.INSTANCE.convertImageAnalysisResultToGetDTO(new ImageAnalysisResult(result));
            
        }
       
        
    }

    @GetMapping("/games/{gameId}/results")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameResultGetDTO getGameResults(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        
        // Authenticate
        authService.authenticateToken(token);

        // Fetch session data
        Game game = gameOrchestrationService.getGameById(gameId);
        List<LobbyPlayer> lobbyPlayers = gameOrchestrationService.getLobbyPlayersByGameId(gameId);

        // Execute logic and return team leaderboard
        return gameOrchestrationService.processGameResults(game, lobbyPlayers);
    }

}

