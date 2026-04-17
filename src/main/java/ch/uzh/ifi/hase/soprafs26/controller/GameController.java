package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
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
        // authenticate user or UNAUTHORIZED
        authService.authenticateToken(token);

        Game game = gameService.getGameById(gameId); 

        // Create SSE emitter
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Send initial state
        try {
            emitter.send(SseEmitter.event()
                .name("gameUpdate")
                .data(DTOMapper.INSTANCE.convertEntityToGameDTO(game)));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        // Register emitter
        gameService.registerGameEmitter(gameId, emitter);

        emitter.onCompletion(() -> gameService.removeGameEmitter(gameId, emitter));
        emitter.onTimeout(() -> gameService.removeGameEmitter(gameId, emitter));

        return emitter;
    }


    @PostMapping("/games/{id}/submission")
    @ResponseStatus(HttpStatus.CREATED)
    
    public ImageAnalysisGetDTO analyze(@RequestParam("image") MultipartFile file,
                                   @RequestParam("object") String object,
                                   @RequestParam("team") String team,
                                    @PathVariable UUID id,
                                    @RequestHeader(value = "Authorization", required = false) String token
                                    ) throws Exception {

    authService.authenticateToken(token);
    Game game=gameService.getGameById(id);//get game and check if exists

    //check if the word is in the list and if yes return the index
    String[] wordlist = game.getWordList();
    int indexofword= gameService.checkWordList(wordlist,object);


    //check if the word is not taken;
    String[] wordlistscore = game.getWordListScore();
    gameService.checkWordTaken(wordlistscore,indexofword );
    

    //set word as taken
    //teamscore +=1
    //return 1 if found, 0 if not

    if( gameService.imageSubmission(file, object,wordlistscore,indexofword,team, game) == 1)
    {int result = 1;
        return DTOMapper.INSTANCE.convertImageAnalysisResultToGetDTO(new ImageAnalysisResult(result));
    }
    else{
        int result=0;
        return DTOMapper.INSTANCE.convertImageAnalysisResultToGetDTO(new ImageAnalysisResult(result));
        
    }


}
}

