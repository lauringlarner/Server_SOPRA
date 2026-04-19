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




@Service
@Transactional
public class GameOrchestrationService {

    private final GameService gameService;
    private final LobbyService lobbyService;

    public GameOrchestrationService(GameService gameService, LobbyService lobbyService) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
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


    public SseEmitter startGameStream(User user, UUID gameId) {

        // fetch Game from gameId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

        // create and register to SSE
        return gameService.createAndRegisterGameStream(game);
    }


   public int submitImage(User user, UUID gameId, MultipartFile file, String object, String team, String playerName) {
    // fetch Game from gameId and player from user or Not Found
    Game game = gameService.getGameById(gameId);
    LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

    // validate player is in game or FORBIDDEN
    lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

    //check if the word is in the list and if yes return the index
    int index = gameService.checkWordList(game.getWordList(), object);

    //check if the word is not taken
    gameService.checkWordTaken(game.getWordListScore(), index);

    return gameService.imageSubmission(file, object, game.getWordListScore(), index, team, game, playerName);
}


    public Game getGameById(UUID gameId) {
        return gameService.getGameById(gameId);
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
}