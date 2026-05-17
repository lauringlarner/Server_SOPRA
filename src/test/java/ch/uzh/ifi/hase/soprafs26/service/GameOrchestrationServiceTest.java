package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;

@ExtendWith(MockitoExtension.class)
class GameOrchestrationServiceTest {

    @Mock
    private GameService gameService;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private LeaderboardService leaderboardService;

    @Mock
    private UserService userService;

    @InjectMocks
    private GameOrchestrationService service;

    // ---------------- GET GAME ----------------

    @Test
    void getGame_success() {
        User user = new User();
        UUID gameId = UUID.randomUUID();

        Game game = new Game();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        when(gameService.getGameById(gameId)).thenReturn(game);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);

        GameDTO result = service.getGame(user, gameId);

        assertNotNull(result);
        verify(lobbyService).validateLobbyPlayerIsInGame(lobbyPlayer, game);
    }

    // ---------------- START GAME ----------------

    @Test
    void startGame_singleplayer_assignsPlayersToTeam1AndSkipsTeamValidation() {
        User user = new User();
        UUID lobbyId = UUID.randomUUID();

        Lobby lobby = new Lobby();
        LobbyPlayer player = new LobbyPlayer();
        player.setTeamType(TeamType.Team1);

        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        Game game = new Game();
        game.setId(UUID.randomUUID());
        when(gameService.createGame(lobby, 1)).thenReturn(game);

        service.startGame(user, lobbyId, 1);

        verify(lobbyService, never()).validateAllPlayersAreInValidTeams(any());
        verify(lobbyService, never()).validateLobbyHasPlayersInBothTeams(any());
    }

    @Test
    void startGame_singleplayer_undecidedPlayer_doesNotThrow() {
        User user = new User();
        UUID lobbyId = UUID.randomUUID();

        Lobby lobby = new Lobby();
        LobbyPlayer player = new LobbyPlayer();
        player.setTeamType(TeamType.Undecided);

        lobby.setLobbyPlayers(List.of(player));

        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        Game game = new Game();
        game.setId(UUID.randomUUID());
        when(gameService.createGame(lobby, 1)).thenReturn(game);

        assertDoesNotThrow(() -> service.startGame(user, lobbyId, 1));

        verify(lobbyService).updateTeamType(player, TeamType.Team1);
    }

    @Test
    void startGame_singleplayer_success() {
        User user = new User();
        UUID lobbyId = UUID.randomUUID();

        Lobby lobby = new Lobby();
        LobbyPlayer player = new LobbyPlayer();

        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        Game game = new Game();
        game.setId(UUID.randomUUID());

        when(gameService.createGame(lobby, 1)).thenReturn(game);

        Game result = service.startGame(user, lobbyId, 1);

        assertNotNull(result);
        verify(gameService).createGame(lobby, 1);
        verify(leaderboardService).initOrUpdate(game);
        verify(lobbyService).setLobbyGameId(lobby, game.getId());
    }

    @Test
    void startGame_notHost_throwsForbidden() {
        User user = new User();
        UUID lobbyId = UUID.randomUUID();

        Lobby lobby = new Lobby();
        LobbyPlayer player = new LobbyPlayer();

        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
                .when(lobbyService).validateLobbyPlayerIsHost(player);

        assertThrows(ResponseStatusException.class,
                () -> service.startGame(user, lobbyId, 0));
    }

    // ---------------- LEADERBOARD ----------------

    @Test
    void getLeaderboard_success() {
        User user = new User();
        UUID gameId = UUID.randomUUID();

        Game game = new Game();
        LobbyPlayer player = new LobbyPlayer();
        Leaderboard leaderboard = new Leaderboard();

        when(gameService.getGameById(gameId)).thenReturn(game);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);
        when(leaderboardService.getLeaderboard(gameId)).thenReturn(leaderboard);

        Leaderboard result = service.getLeaderboard(user, gameId);

        assertNotNull(result);
        verify(lobbyService).validateLobbyPlayerIsInGame(player, game);
    }

    // ---------------- IMAGE SUBMISSION ----------------

    @Test
    void submitImage_invalidTeam_throwsBadRequest() throws Exception {
        User user = new User();
        UUID gameId = UUID.randomUUID();

        Game game = new Game();
        LobbyPlayer player = new LobbyPlayer();
        player.setTeamType(null);

        MultipartFile file = mock(MultipartFile.class);

        when(gameService.getGameById(gameId)).thenReturn(game);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        assertThrows(ResponseStatusException.class,
                () -> service.submitImageAsync(user, gameId, file, "obj"));
    }

    @Test
    void submitImage_valid_callsProcess() throws Exception {
        User user = new User();
        UUID gameId = UUID.randomUUID();

        Game game = new Game();
        LobbyPlayer player = new LobbyPlayer();
        player.setTeamType(TeamType.Team1);

        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});

        when(gameService.getGameById(gameId)).thenReturn(game);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        doNothing().when(gameService).validateSubmissionRequest(game, file, "obj");

        service.submitImageAsync(user, gameId, file, "obj");

        verify(gameService).markSubmissionProcessing(game, "obj", "1");
        verify(gameService).processSubmissionAsync(eq(gameId), any(), eq("obj"), eq("1"));
    }

    // ---------------- DELETE GAME ----------------

    @Test
    void deleteGame_singleplayer_doesNotUpdateStats() {
        User user = new User();
        UUID gameId = UUID.randomUUID();

        Game game = new Game();
        game.setId(gameId); // 
        game.setLobbyId(UUID.randomUUID());
        game.setIsSinglePlayer(true);
        game.setStatsFinalized(false);

        Lobby lobby = new Lobby();
        LobbyPlayer player = new LobbyPlayer();

        when(gameService.getGameById(gameId)).thenReturn(game);
        when(lobbyService.getLobbyByLobbyId(game.getLobbyId())).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player);

        service.deleteGame(user, gameId);

        verify(userService, never()).updateUserStats(any(), anyBoolean());
        verify(lobbyService).resetLobbyAfterGame(game.getLobbyId());
        verify(gameService).deleteGameDelayed(gameId); 
    }

    @Test
    void deleteGame_multiplayer_updatesStats() {
        User user = new User();
        UUID gameId = UUID.randomUUID();

        Game game = new Game();
        game.setLobbyId(UUID.randomUUID());
        game.setIsSinglePlayer(false);
        game.setStatsFinalized(false);

        Lobby lobby = new Lobby();

        LobbyPlayer player1 = new LobbyPlayer();
        player1.setTeamType(TeamType.Team1);
        player1.setUser(new User());

        LobbyPlayer player2 = new LobbyPlayer();
        player2.setTeamType(TeamType.Team2);
        player2.setUser(new User());

        lobby.setLobbyPlayers(List.of(player1, player2));

        when(gameService.getGameById(gameId)).thenReturn(game);
        when(lobbyService.getLobbyByLobbyId(game.getLobbyId())).thenReturn(lobby);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(player1);

        when(gameService.getWinningTeam(game)).thenReturn(TeamType.Team1);

        service.deleteGame(user, gameId);

        verify(userService).updateUserStats(player1.getUser(), true);
        verify(userService).updateUserStats(player2.getUser(), false);
    }
}