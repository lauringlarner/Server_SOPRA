package ch.uzh.ifi.hase.soprafs26.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameResultGetDTO;
import java.util.List;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;



@Service
@Transactional
public class GameOrchestrationService {

    private final GameService gameService;
    private final LobbyService lobbyService;
    private final GameRepository gameRepository;
    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;

    public GameOrchestrationService(GameService gameService, LobbyService lobbyService,GameRepository gameRepository,LobbyRepository lobbyRepository,UserRepository userRepository) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        this.gameRepository = gameRepository;
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
    }




    public Game startGame(User user, UUID lobbyId) {

        // fetch lobbyPlayer and lobby or Not Found
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate lobbyPlayer is in lobby and is a host or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        lobbyService.validateLobbyPlayerIsHost(lobbyPlayer);

        // validate all lobbyPlayers currently in the lobby are "ready" and the lobby is OPEN 
        // and every team has at least 1 player each and every player is assigned to a valid team or CONFLICT
        lobbyService.validateAllPlayersReady(lobby);
        lobbyService.validateLobbyIsOpen(lobby);
        lobbyService.validateAllPlayersAreInValidTeams(lobby);
        lobbyService.validateLobbyHasPlayersInBothTeams(lobby);

        // create game
        Game game = gameService.createGame(lobby);

        // set gameId in lobby
        lobby.setGameId(game.getId());

        return game;
    }

    public Game getGameById(UUID gameId) {
        return gameService.getGameById(gameId);
    }

    public SseEmitter startGameStream(User user, UUID gameId) {

        // fetch Game from gameId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

        // create and register to SSE
        return gameService.createAndRegisterGameStream(game);
    }


    public int submitImage(User user, UUID gameId, MultipartFile file, String object, String team) {
        // fetch Game from gameId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

        //check if the word is in the list and if yes return the index
        int index = gameService.checkWordList(game.getWordList(), object);

        //check if the word is not taken;
        gameService.checkWordTaken(game.getWordListScore(), index);

        return gameService.imageSubmission(file, object, game.getWordListScore(), index, team, game);
    }


    public void deleteGame(User user, UUID gameId) {
        // fetch Game from gameId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);
        lobbyService.validateLobbyPlayerIsHost(lobbyPlayer);

        ///////////////////////////////////////////////////
        /// Update user stats would come here probably ? //
        ///////////////////////////////////////////////////

        // reset all players readyStatus to false and sets Lobbys gameId to null
        lobbyService.resetLobbyAfterGame(game.getLobbyId());

        // delete game
        gameService.deleteGame(game);

    }

    public GameResultGetDTO processGameResults(Game game, List<LobbyPlayer> lobbyPlayers) {
    GameResultGetDTO result = new GameResultGetDTO();
    
    int s1 = game.getScore_1();
    int s2 = game.getScore_2();

    // 1. Bestimmung von Gewinner/Verlierer/Unentschieden für das DTO
    if (s1 > s2) {
        result.setWinnerTeam("Team 1");
        result.setWinnerScore(s1);
        result.setLoserTeam("Team 2");
        result.setLoserScore(s2);
        result.setIsDraw(false);
    } else if (s2 > s1) {
        result.setWinnerTeam("Team 2");
        result.setWinnerScore(s2);
        result.setLoserTeam("Team 1");
        result.setLoserScore(s1);
        result.setIsDraw(false);
    } else {
        result.setIsDraw(true);
        result.setWinnerScore(s1);
        result.setLoserScore(s2);
        // Bei Draw setzen wir optionale Teamnamen oder lassen sie leer
        result.setWinnerTeam("Team 1");
        result.setLoserTeam("Team 2");
    }

    // 2. Manuelles Mapping der LobbyPlayer-Liste in das flache PlayerInfo-DTO
    // Das verhindert die MapStruct-Komplexität und schickt nur nötige Daten ans Frontend
    List<GameResultGetDTO.PlayerInfo> playerInfos = lobbyPlayers.stream().map(lp -> {
        GameResultGetDTO.PlayerInfo info = new GameResultGetDTO.PlayerInfo();
        
        // ID ist jetzt UUID (passend zum Fehler-Fix)
        info.setId(lp.getUser().getId()); 
        info.setUsername(lp.getUser().getUsername());
        info.setTeamType(lp.getTeamType().toString());
        
        return info;
    }).collect(java.util.stream.Collectors.toList());

    result.setPlayerList(playerInfos);

    // 3. Update der permanenten User-Statistiken in der Datenbank
    for (LobbyPlayer lp : lobbyPlayers) {
        User user = lp.getUser(); 
        
        // Sieg-Statistik (nur wenn es kein Unentschieden war)
        if (!result.getIsDraw()) {
            TeamType playerTeam = lp.getTeamType();
            boolean won = (s1 > s2 && playerTeam == TeamType.Team1) || 
                          (s2 > s1 && playerTeam == TeamType.Team2);
            
            if (won) {
                user.setGamesWon(user.getGamesWon() + 1);
            }
        }

        // Addiere die gefundenen Items dieses Spiels zum Lifetime-Konto
        int itemsFoundByTeam = (lp.getTeamType() == TeamType.Team1) ? s1 : s2;
        user.setCorrectItemsFound(user.getCorrectItemsFound() + itemsFoundByTeam);

       
        userRepository.save(user);
    }
    
   
    userRepository.flush();

    return result;
}



public List<LobbyPlayer> getLobbyPlayersByGameId(UUID gameId) {
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    
    Lobby lobby = lobbyRepository.findById(game.getLobbyId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    
    return lobby.getLobbyPlayers();
}


}

    
    
