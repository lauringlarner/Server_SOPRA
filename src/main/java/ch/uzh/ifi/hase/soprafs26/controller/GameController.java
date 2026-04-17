package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;
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

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisResult;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

/**
 * Game Controller
 * This class is responsible for handling all REST requests related to
 * the game.
 * The controller will receive the request and delegate the execution to the
 * GameService and finally return the result.
 */
@RestController
public class GameController {

    private final GameService gameService;
    private final AuthService authService;

    GameController(GameService gameService, AuthService authService) {
        this.gameService = gameService;
        this.authService = authService;
    }
 



    @GetMapping("/games/{gameId}/stream")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public SseEmitter getGameByIdEmitter(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        
        // fetch Game from gameId or Not Found
        Game game = gameService.getGameById(gameId); 

        // validate User is in game or FORBIDDEN
        gameService.validateUserInGame(user, game);

        return gameService.createAndRegisterGameStream(game);
    }


    @DeleteMapping("/game/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteGame(@PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch Game from gameId or Not Found
        Game game = gameService.getGameById(gameId);

        // validate User is in game and is host or FORBIDDEN
        gameService.validateUserInGame(user, game);
        gameService.validateUserPlayerIsHost(user);

        // delete game
        gameService.deleteGame(game);
    }


    @PostMapping("/games/{gameId}/submission")
    @ResponseStatus(HttpStatus.CREATED)
    
    public ImageAnalysisGetDTO analyze(@RequestParam("image") MultipartFile file,
        @RequestParam("object") String object, @RequestParam("team") String team,
        @PathVariable UUID gameId,
        @RequestHeader(value = "Authorization", required = false) String token) throws Exception {
        // authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        
        // fetch Game from gameId or Not Found
        Game game = gameService.getGameById(gameId);

        // validate User is in game or FORBIDDEN
        gameService.validateUserInGame(user, game);

        //check if the word is in the list and if yes return the index
        List<String> wordList = game.getWordList();
        int indexOfWord = gameService.checkWordList(wordList,object);


        //check if the word is not taken;
        List<String> wordListScore = game.getWordListScore();
        gameService.checkWordTaken(wordListScore,indexOfWord );
        

        //set word as taken
        //teamscore +=1
        //return 1 if found, 0 if not

        if( gameService.imageSubmission(file, object,wordListScore,indexOfWord,team, game) == 1)
        {int result = 1;
            return DTOMapper.INSTANCE.convertImageAnalysisResultToGetDTO(new ImageAnalysisResult(result));
        }
        else{
            int result=0;
            return DTOMapper.INSTANCE.convertImageAnalysisResultToGetDTO(new ImageAnalysisResult(result));
            
        }


    }
}

