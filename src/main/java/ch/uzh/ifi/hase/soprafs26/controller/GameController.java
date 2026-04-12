package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GameService;

import org.springframework.web.bind.annotation.RequestHeader;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;

import java.util.ArrayList;
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

    private final GameService gameService;
    private final AuthService authService;

    GameController(GameService gameService, AuthService authService) {
        this.gameService = gameService;
        this.authService = authService;
    }
 
    @GetMapping("/games")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<GameGetDTO> getAllGames(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        // fetch all games in the internal representation
        List<Game> games = gameService.getGames();
        List<GameGetDTO> gameGetDTOs = new ArrayList<>();

        // convert each game to the API representation
        for (Game game : games) {
            gameGetDTOs.add(DTOMapper.INSTANCE.convertEntityToGameGetDTO(game));
        }
        return gameGetDTOs;
    }

    @GetMapping("/games/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO getGame(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        Game game = gameService.getGameById(id);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createGame(@RequestBody GamePostDTO gamePostDTO,
            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        // convert API game to internal representation
        Game gameInput = DTOMapper.INSTANCE.convertGamePostDTOtoEntity(gamePostDTO);

        // create game
        Game createdGame = gameService.createGame(gameInput);
        // convert internal representation of game back to API
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(createdGame);
    }

    @PostMapping("/games/{id}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void analyze(@RequestParam("image") MultipartFile file,
            @RequestParam("object") String object,
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) throws Exception {

        User user = authService.authenticateToken(token);
        Game game = gameService.getGameById(id);
        String team = gameService.validateSubmissionRequest(game, file, object, user);
        gameService.processSubmissionAsync(id, file.getBytes(), object, team);
    }
}
