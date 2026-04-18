package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.VisionQuickstartObjectLocalization;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisResult;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LeaderboardService;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

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
    private final LeaderboardService leaderboardService;

    GameController(GameService gameService, AuthService authService, LeaderboardService leaderboardService) {
        this.gameService = gameService;
        this.authService = authService;
        this.leaderboardService = leaderboardService;
    }
 
    @GetMapping("/lobbies/{lobbyId}/games")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<GameGetDTO> getAllGames(@PathVariable UUID lobbyId,
                                       @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        List<Game> games = gameService.getGames();
        List<GameGetDTO> gameGetDTOs = new ArrayList<>();

        for (Game game : games) {
            gameGetDTOs.add(DTOMapper.INSTANCE.convertEntityToGameGetDTO(game));
        }
        return gameGetDTOs;
    }

    @PostMapping("/lobbies/{lobbyId}/games")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createGame(@PathVariable UUID lobbyId,
                                 @RequestBody GamePostDTO gamePostDTO,
                                 @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        Game gameInput = DTOMapper.INSTANCE.convertGamePostDTOtoEntity(gamePostDTO);
        Game createdGame = gameService.createGame(gameInput);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(createdGame);
    }

    @PostMapping("/lobbies/{lobbyId}/games/{gameId}/leaderboard")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LeaderboardGetDTO postLeaderboard(@PathVariable UUID lobbyId,
                                             @PathVariable Long gameId,
                                             @RequestBody LeaderboardPostDTO leaderboardPostDTO,
                                             @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        Game game = gameService.getGameById(gameId);

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
                                            @PathVariable Long gameId,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        Leaderboard leaderboard = leaderboardService.getLeaderboard(gameId);

        LeaderboardGetDTO dto = new LeaderboardGetDTO(leaderboard.getGameId());
        dto.setTeam1Score(leaderboard.getTeam1Score());
        dto.setTeam2Score(leaderboard.getTeam2Score());
        dto.setTileGrid(leaderboard.getTileGrid());
        return dto;
    }

    @PostMapping("/lobbies/{lobbyId}/games/{gameId}/submission")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageAnalysisGetDTO analyze(@RequestParam("image") MultipartFile file,
                                       @RequestParam("object") String object,
                                       @RequestParam("team") String team,
                                       @PathVariable UUID lobbyId,
                                       @PathVariable Long gameId,
                                       @RequestHeader(value = "Authorization", required = false) String token
                                       ) throws Exception {

    authService.authenticateToken(token);
    Game game=gameService.getGameById(gameId);//get game and check if exists

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

