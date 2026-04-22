package ch.uzh.ifi.hase.soprafs26.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;




@Service
@Transactional
public class GameOrchestrationService {

    private final GameService gameService;
    private final LobbyService lobbyService;
    private final LeaderboardService leaderboardService;

    public GameOrchestrationService(
        GameService gameService,
        LobbyService lobbyService,
        LeaderboardService leaderboardService
    ) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        this.leaderboardService = leaderboardService;
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
        leaderboardService.initOrUpdate(game);

        // set gameId in lobby and publish the started-lobby update to SSE subscribers
        lobbyService.setLobbyGameId(lobby, game.getId());

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


    public void submitImageAsync(User user, UUID gameId, MultipartFile file, String object) {
        // fetch Game from gameId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

        TeamType teamType = lobbyPlayer.getTeamType();
        if (teamType != TeamType.Team1 && teamType != TeamType.Team2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player is not in a valid team!");
        }

        gameService.validateSubmissionRequest(game, file, object);

        try {
            byte[] fileBytes = file.getBytes();
            String team = teamType == TeamType.Team1 ? "1" : "2";

            gameService.markSubmissionProcessing(game, object, team);
            gameService.processSubmissionAsync(
                gameId,
                fileBytes,
                object,
                team
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error with image type!");
        }
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
