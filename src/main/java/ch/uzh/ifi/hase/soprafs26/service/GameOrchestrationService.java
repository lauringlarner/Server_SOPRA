package ch.uzh.ifi.hase.soprafs26.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;




@Service
@Transactional
public class GameOrchestrationService {

    private final GameService gameService;
    private final LobbyService lobbyService;
    private final LeaderboardService leaderboardService;
    private final UserService userService;

    public GameOrchestrationService(
        GameService gameService,
        LobbyService lobbyService,
        LeaderboardService leaderboardService,
        UserService userService
    ) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
        this.leaderboardService = leaderboardService;
        this.userService = userService;
    }


    public GameDTO getGame(User user, UUID gameId) {
        // fetch Game from gameId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

        return DTOMapper.INSTANCE.convertEntityToGameDTO(game);
    }


    public Game startGame(User user, UUID lobbyId, int isSingleplayer) {

        // fetch lobbyPlayer and lobby or Not Found
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        // validate lobbyPlayer is in lobby and is a host or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        lobbyService.validateLobbyPlayerIsHost(lobbyPlayer);

        // validate all lobbyPlayers currently in the lobby are "ready" and the lobby is OPEN
        lobbyService.validateAllPlayersReady(lobby);
        lobbyService.validateLobbyIsOpen(lobby);

        if (isSingleplayer == 1) {
            // auto-assign all players to Team1 so team validation and image submission work correctly
            for (LobbyPlayer lp : lobby.getLobbyPlayers()) {
                lobbyService.updateTeamType(lp, TeamType.Team1);
            }
            lobby.setIsSinglePlayer(true);
        } else {
            lobbyService.validateAllPlayersAreInValidTeams(lobby);
            lobbyService.validateLobbyHasPlayersInBothTeams(lobby);
        }

        // create game
        Game game = gameService.createGame(lobby, isSingleplayer);
        leaderboardService.initOrUpdate(game);

        // set gameId in lobby and publish the started-lobby update to SSE subscribers
        lobbyService.setLobbyGameId(lobby, game.getId());

        return game;
    }

    public Leaderboard getLeaderboard(User user, UUID gameId) {
        Game game = gameService.getGameById(gameId);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);
        return leaderboardService.getLeaderboard(gameId);
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

    public void updateAllPlayersUserStats(Lobby lobby, TeamType winningTeam) {
        List<LobbyPlayer> lobbyPlayers = lobby.getLobbyPlayers();
        
        for (LobbyPlayer lobbyPlayer : lobbyPlayers) {
            if (lobbyPlayer.getTeamType() == winningTeam) {
                userService.updateUserStats(lobbyPlayer.getUser(), true);
            }
            else {
                userService.updateUserStats(lobbyPlayer.getUser(), false);
            }
        }
    }


    public void deleteGame(User user, UUID gameId) {
        // fetch Game from gameId, lobby from lobbyId and player from user or Not Found
        Game game = gameService.getGameById(gameId);
        Lobby lobby = lobbyService.getLobbyByLobbyId(game.getLobbyId());
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        
        // validate player is in game or FORBIDDEN
        lobbyService.validateLobbyPlayerIsInGame(lobbyPlayer, game);

        // update all Users stats if multiplayer
        if ((!game.getIsSinglePlayer()) && (!game.isStatsFinalized())) {
            game.setStatsFinalized(true);
            TeamType winningTeam = gameService.getWinningTeam(game);
            updateAllPlayersUserStats(lobby, winningTeam);
        }

        // reset all players readyStatus to false and sets Lobbys gameId to null
        lobbyService.resetLobbyAfterGame(game.getLobbyId());

        // delete game
        gameService.deleteGameDelayed(game.getId());
    }
}
