package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;

@ExtendWith(MockitoExtension.class)
public class LobbyServiceTest {
    @InjectMocks
    private LobbyService lobbyService;

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private LobbyPlayerRepository lobbyPlayerRepository;

    @Mock
    private PusherService pusherService;
    

    ///////////////////////
    // createLobbyPlayer //
    ///////////////////////
    
    @Test
    void createLobbyPlayer_success() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(null);
        when(lobbyPlayerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LobbyPlayer result = lobbyService.createLobbyPlayer(user, true);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertTrue(result.getIsHost());
        assertFalse(result.getIsReady());
        assertEquals(TeamType.Undecided, result.getTeamType());

        verify(lobbyPlayerRepository).save(any());
        verify(lobbyPlayerRepository).flush();
    }

    @Test
    void createLobbyPlayer_userNull_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.createLobbyPlayer(null, true));
    }

    @Test
    void createLobbyPlayer_userWithoutId_throwsBadRequest() {
        User user = new User();
        user.setId(null);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.createLobbyPlayer(user, true));
    }

    @Test
    void createLobbyPlayer_userAlreadyHasPlayer_throwsConflict() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(new LobbyPlayer());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.createLobbyPlayer(user, false));
    }

    /////////////////
    // createLobby //
    /////////////////

    @Test
    void createLobby_success() {
        LobbyPlayer lp = new LobbyPlayer();

        when(lobbyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Lobby result = lobbyService.createLobby(lp);

        assertNotNull(result);
        assertEquals(10, result.getGameDuration());
        assertEquals(lp, result.getLobbyPlayers().get(0));

        verify(lobbyRepository).save(any());
        verify(lobbyPlayerRepository).save(lp);
        verify(lobbyPlayerRepository).flush();
    }

    @Test
    void createLobby_nullLobbyPlayer_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.createLobby(null));
    }

    @Test
    void createLobby_playerAlreadyInLobby_throwsConflict() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(new Lobby());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.createLobby(lp));
    }

    ////////////////////////
    // getLobbyByJoinCode //
    ////////////////////////

    @Test
    void getLobbyByJoinCode_success() {
        Lobby lobby = new Lobby();

        when(lobbyRepository.findByJoinCode("ABC123")).thenReturn(lobby);

        Lobby result = lobbyService.getLobbyByJoinCode("ABC123");

        assertEquals(lobby, result);
    }

    @Test
    void getLobbyByJoinCode_notFound_throwsNotFound() {
        when(lobbyRepository.findByJoinCode("ABC123")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByJoinCode("ABC123"));
    }

    @Test
    void getLobbyByJoinCode_invalidCode_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByJoinCode("abc"));
    }

    //////////////////
    // getLobbyById //
    //////////////////

    @Test
    void getLobbyByLobbyId_success() {
        UUID id = UUID.randomUUID();
        Lobby lobby = new Lobby();

        when(lobbyRepository.findById(id)).thenReturn(Optional.of(lobby));

        Lobby result = lobbyService.getLobbyByLobbyId(id);

        assertEquals(lobby, result);
    }

    @Test
    void getLobbyByLobbyId_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();

        when(lobbyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByLobbyId(id));
    }

    ////////////////////
    // getLobbyByUser //
    ////////////////////

    @Test
    void getLobbyPlayerByUser_success() {
        User user = new User();
        LobbyPlayer lp = new LobbyPlayer();

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(lp);

        LobbyPlayer result = lobbyService.getLobbyPlayerByUser(user);

        assertEquals(lp, result);
    }

    @Test
    void getLobbyPlayerByUser_notFound_throwsNotFound() {
        User user = new User();

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyPlayerByUser(user));
    }

    ////////////////////////
    // getLobbyPlayerById //
    ////////////////////////

    @Test
    void getLobbyPlayerById_success() {
        UUID id = UUID.randomUUID();
        LobbyPlayer lp = new LobbyPlayer();

        when(lobbyPlayerRepository.findById(id)).thenReturn(Optional.of(lp));

        LobbyPlayer result = lobbyService.getLobbyPlayerById(id);

        assertEquals(lp, result);
    }

    @Test
    void getLobbyPlayerById_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();

        when(lobbyPlayerRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyPlayerById(id));
    }

    ///////////////
    // joinLobby //
    ///////////////

    @Test
    void joinLobby_success() {
        Lobby lobby = new Lobby();
        LobbyPlayer lp = new LobbyPlayer();

        when(lobbyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Lobby result = lobbyService.joinLobby(lp, lobby);

        assertNotNull(result);
        assertEquals(result, lp.getLobby());

        verify(lobbyRepository).save(lobby);
        verify(lobbyPlayerRepository).save(lp);
        verify(lobbyPlayerRepository).flush();
    }

    @Test
    void joinLobby_nullInput_throwsBadRequest() {
        LobbyPlayer lp = null;
        Lobby lobby = null;

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.joinLobby(lp, lobby));
    }

    @Test
    void joinLobby_gameAlreadyRunning_throwsConflict() {
        Lobby lobby = new Lobby();
        lobby.setGameId(UUID.randomUUID());

        LobbyPlayer lp = new LobbyPlayer();

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.joinLobby(lp, lobby));
    }

    /////////////////////////
    // resetLobbyAfterGame //
    /////////////////////////

    @Test
    void resetLobbyAfterGame_success() {
        UUID id = UUID.randomUUID();

        Lobby lobby = new Lobby();
        lobby.setGameId(UUID.randomUUID());

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setIsReady(true);

        lobby.setLobbyPlayers(List.of(p1));

        when(lobbyRepository.findById(id)).thenReturn(Optional.of(lobby));

        lobbyService.resetLobbyAfterGame(id);

        assertNull(lobby.getGameId());
        assertFalse(p1.getIsReady());
    }

    @Test
    void resetLobbyAfterGame_lobbyNotFound_throwsException() {
        UUID id = UUID.randomUUID();

        when(lobbyRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.resetLobbyAfterGame(id));
    }

    ////////////////////
    // updateTeamType //
    ////////////////////
    
    @Test
    void updateTeamType_success() {
        Lobby lobby = new Lobby();
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);

        lobbyService.updateTeamType(lp, TeamType.Team1);

        assertEquals(TeamType.Team1, lp.getTeamType());
        verify(lobbyPlayerRepository).flush();
    }

    @Test
    void updateTeamType_invalid_throwsException() {
        LobbyPlayer lp = new LobbyPlayer();

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateTeamType(lp, TeamType.Undecided));
    }

    ///////////////////////
    // updateReadyStatus //
    ///////////////////////

    @Test
    void updateReadyStatus_success() {
        Lobby lobby = new Lobby();
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);

        lobbyService.updateReadyStatus(lp, true);

        assertTrue(lp.getIsReady());
        verify(lobbyPlayerRepository).flush();
    }

    @Test
    void updateReadyStatus_null_throwsException() {
        LobbyPlayer lp = new LobbyPlayer();

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateReadyStatus(lp, null));
    }

    /////////////////////////////////////////////
    // updateAllLobbyPlayersReadyStatusToFalse //
    /////////////////////////////////////////////
    
    @Test
    void updateAllLobbyPlayersReadyStatusToFalse_success() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setIsReady(true);

        LobbyPlayer p2 = new LobbyPlayer();
        p2.setIsReady(true);

        lobby.setLobbyPlayers(List.of(p1, p2));

        lobbyService.updateAllLobbyPlayersReadyStatusToFalse(lobby);

        assertFalse(p1.getIsReady());
        assertFalse(p2.getIsReady());
    }

    /////////////////////////
    // updateLobbySettings //
    /////////////////////////

    @Test
    void updateLobbySettings_success() {
        Lobby lobby = new Lobby();

        lobbyService.updateLobbySettings(lobby, 10);

        assertEquals(10, lobby.getGameDuration());
        verify(lobbyRepository).flush();
    }

    @Test
    void updateLobbySettings_invalid_throwsException() {
        Lobby lobby = new Lobby();

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateLobbySettings(lobby, 999));
    }

    ////////////////////
    // setLobbyGameId //
    ////////////////////

    @Test
    void setLobbyGameId_success() {
        Lobby lobby = new Lobby();
        UUID gameId = UUID.randomUUID();

        lobbyService.setLobbyGameId(lobby, gameId);

        assertEquals(gameId, lobby.getGameId());
        verify(lobbyRepository).flush();
    }
    /////////////////
    // deleteLobby //
    /////////////////

    @Test
    void deleteLobby_success() {
        Lobby lobby = new Lobby();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        lobbyPlayer.setLobby(lobby);
        lobby.addPlayer(lobbyPlayer);

        lobbyService.deleteLobby(lobbyPlayer, lobby);

        verify(lobbyRepository).delete(lobby);
        verify(lobbyPlayerRepository).delete(lobbyPlayer);
    }

    ///////////////////////
    // deleteLobbyPlayer //
    ///////////////////////

    @Test
    void deleteLobbyPlayer_success() {
        Lobby lobby = new Lobby();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        lobbyPlayer.setLobby(lobby);

        lobby.setLobbyPlayers(new ArrayList<>());
        lobby.getLobbyPlayers().add(lobbyPlayer);

        lobbyService.deleteLobbyPlayer(lobbyPlayer);

        verify(lobbyPlayerRepository).delete(lobbyPlayer);
        verify(lobbyPlayerRepository).flush();
    }

    ////////////////////////////////
    // validateLobbyPlayerInLobby //
    ////////////////////////////////

    @Test
    void validateLobbyPlayerInLobby_correctLobby_noException() {
        Lobby lobby = new Lobby();

        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);

        assertDoesNotThrow(() ->
            lobbyService.validateLobbyPlayerInLobby(lp, lobby));
    }

    @Test
    void validateLobbyPlayerInLobby_wrongLobby_throwsForbidden() {
        Lobby lobby = new Lobby();
        Lobby otherLobby = new Lobby();

        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(otherLobby);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyPlayerInLobby(lp, lobby));
    }

    //////////////////////////////////
    // validateLobbyPlayerIsNotHost //
    //////////////////////////////////

    @Test
    void validateLobbyPlayerIsNotHost_notHost_noException() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsHost(false);

        assertDoesNotThrow(() ->
            lobbyService.validateLobbyPlayerIsNotHost(lp));
    }

    @Test
    void validateLobbyPlayerIsNotHost_isHost_throwsConflict() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsHost(true);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyPlayerIsNotHost(lp));
    }

    ///////////////////////////////
    // validateLobbyPlayerIsHost //
    ///////////////////////////////

    @Test
    void validateLobbyPlayerIsHost_isHost_noException() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsHost(true);

        assertDoesNotThrow(() ->
            lobbyService.validateLobbyPlayerIsHost(lp));
    }

    @Test
    void validateLobbyPlayerIsHost_notHost_throwsForbidden() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsHost(false);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyPlayerIsHost(lp));
    }

    ///////////////////////////////
    // validateLobbyPlayerIsHost //
    ///////////////////////////////

    @Test
    void validateAllPlayersReady_allReady_noException() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setIsReady(true);

        LobbyPlayer p2 = new LobbyPlayer();
        p2.setIsReady(true);

        lobby.setLobbyPlayers(List.of(p1, p2));

        assertDoesNotThrow(() ->
            lobbyService.validateAllPlayersReady(lobby));
    }

    @Test
    void validateAllPlayersReady_notAllReady_throwsConflict() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setIsReady(true);

        LobbyPlayer p2 = new LobbyPlayer();
        p2.setIsReady(false);

        lobby.setLobbyPlayers(List.of(p1, p2));

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateAllPlayersReady(lobby));
    }

    ///////////////////////////////////////
    // validateAllPlayersAreInValidTeams //
    ///////////////////////////////////////

    @Test
    void validateAllPlayersAreInValidTeams_allValid_noException() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setTeamType(TeamType.Team1);

        LobbyPlayer p2 = new LobbyPlayer();
        p2.setTeamType(TeamType.Team2);

        lobby.setLobbyPlayers(List.of(p1, p2));

        assertDoesNotThrow(() ->
            lobbyService.validateAllPlayersAreInValidTeams(lobby));
    }

    @Test
    void validateAllPlayersAreInValidTeams_invalidTeam_throwsConflict() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setTeamType(TeamType.Team1);

        LobbyPlayer p2 = new LobbyPlayer();
        p2.setTeamType(TeamType.Undecided);

        lobby.setLobbyPlayers(List.of(p1, p2));

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateAllPlayersAreInValidTeams(lobby));
    }

    /////////////////////////
    // validateLobbyIsOpen //
    /////////////////////////

    @Test
    void validateLobbyIsOpen_openLobby_noException() {
        Lobby lobby = new Lobby();
        lobby.setGameId(null);

        assertDoesNotThrow(() ->
            lobbyService.validateLobbyIsOpen(lobby));
    }

    @Test
    void validateLobbyIsOpen_gameRunning_throwsConflict() {
        Lobby lobby = new Lobby();
        lobby.setGameId(UUID.randomUUID());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyIsOpen(lobby));
    }

    ////////////////////////////////////////
    // validateLobbyHasPlayersInBothTeams //
    ////////////////////////////////////////

    @Test
    void validateLobbyHasPlayersInBothTeams_valid_doesNotThrow() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setTeamType(TeamType.Team1);

        LobbyPlayer p2 = new LobbyPlayer();
        p2.setTeamType(TeamType.Team2);

        lobby.setLobbyPlayers(List.of(p1, p2));

        assertDoesNotThrow(() ->
            lobbyService.validateLobbyHasPlayersInBothTeams(lobby));
    }
    
    @Test
    void validateLobbyHasPlayersInBothTeams_undecidedOnly_throwsConflict() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setTeamType(TeamType.Undecided);

        lobby.setLobbyPlayers(List.of(p1));

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyHasPlayersInBothTeams(lobby));
    }

    @Test
    void validateLobbyHasPlayersInBothTeams_onlyTeam2_throwsConflict() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setTeamType(TeamType.Team2);

        lobby.setLobbyPlayers(List.of(p1));

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyHasPlayersInBothTeams(lobby));
    }

    @Test
    void validateLobbyHasPlayersInBothTeams_onlyTeam1_throwsConflict() {
        Lobby lobby = new Lobby();

        LobbyPlayer p1 = new LobbyPlayer();
        p1.setTeamType(TeamType.Team1);

        lobby.setLobbyPlayers(List.of(p1));

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyHasPlayersInBothTeams(lobby));
    }

    //////////////////////////////////////
    // validateUserMatchesLobbyPlayerId //
    //////////////////////////////////////
    
    @Test
    void validateUserMatchesLobbyPlayerId_correctId_noException() {
        User user = new User();
        UUID id = UUID.randomUUID();

        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setId(id);

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(lobbyPlayer);

        assertDoesNotThrow(() ->
            lobbyService.validateUserMatchesLobbyPlayerId(id, user));
    }

    @Test
    void validateUserMatchesLobbyPlayerId_wrongId_throwsForbidden() {
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setId(UUID.randomUUID());

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(lobbyPlayer);

        UUID differentId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateUserMatchesLobbyPlayerId(differentId, user));
    }

    /////////////////////////////
    // validateUserHasNoPlayer //
    /////////////////////////////

    @Test
    void validateUserHasNoPlayer_noExistingPlayer_allowsCreation() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(lobbyPlayerRepository.findByUser(user)).thenReturn(null);
        when(lobbyPlayerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() ->
            lobbyService.createLobbyPlayer(user, false));
    }

    @Test
    void validateUserHasNoPlayer_userAlreadyHasPlayer_throwsConflict() {
        User user = new User();
        user.setId(UUID.randomUUID());

        lenient().when(lobbyPlayerRepository.findByUser(user))
                .thenReturn(new LobbyPlayer());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.createLobbyPlayer(user, true));
    }

    //////////////////////////
    // validateGameDuration //
    //////////////////////////

    @Test
    void validateGameDuration_valid_accepts() {
        Lobby lobby = new Lobby();

        assertDoesNotThrow(() ->
            lobbyService.updateLobbySettings(lobby, 10));
    }

    @Test
    void validateGameDuration_tooSmall_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateLobbySettings(new Lobby(), 1));
    }

    @Test
    void validateGameDuration_tooLarge_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateLobbySettings(new Lobby(), 999));
    }

    /////////////////////////
    // validateReadyStatus //
    /////////////////////////

    @Test
    void validateReadyStatus_true_accepts() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(new Lobby());

        assertDoesNotThrow(() ->
            lobbyService.updateReadyStatus(lp, true));
    }

    @Test
    void validateReadyStatus_null_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateReadyStatus(new LobbyPlayer(), null));
    }

    //////////////////////
    // validateTeamType //
    //////////////////////

    @Test
    void validateTeamType_valid_accepts() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(new Lobby());

        assertDoesNotThrow(() ->
            lobbyService.updateTeamType(lp, TeamType.Team1));
    }

    @Test
    void validateTeamType_invalid_throwsBadRequest() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(new Lobby());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.updateTeamType(lp, TeamType.Undecided));
    }

    //////////////////////
    // validateJoinCode //
    //////////////////////

    @Test
    void validateJoinCode_validFormat_butNotFound() {
        when(lobbyRepository.findByJoinCode("ABC123")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByJoinCode("ABC123"));
    }

    @Test
    void validateJoinCode_null_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByJoinCode(null));
    }

    @Test
    void validateJoinCode_blank_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByJoinCode(" "));
    }

    @Test
    void validateJoinCode_invalidFormat_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
            () -> lobbyService.getLobbyByJoinCode("abc123"));
    }

    /////////////////////////////////
    // validateLobbyPlayerIsInGame //
    /////////////////////////////////

    @Test
    void validateLobbyPlayerIsInGame_correctGame_noException() {
        UUID id = UUID.randomUUID();

        Lobby lobby = new Lobby();
        lobby.setId(id);

        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);

        Game game = new Game();
        game.setLobbyId(id);

        assertDoesNotThrow(() ->
            lobbyService.validateLobbyPlayerIsInGame(lp, game));
    }

    @Test
    void validateLobbyPlayerIsInGame_wrongGame_throwsForbidden() {
        Lobby lobby = new Lobby();
        lobby.setId(UUID.randomUUID());

        LobbyPlayer lp = new LobbyPlayer();
        lp.setLobby(lobby);

        Game game = new Game();
        game.setLobbyId(UUID.randomUUID());

        assertThrows(ResponseStatusException.class,
            () -> lobbyService.validateLobbyPlayerIsInGame(lp, game));
    }

    //////////////////
    // isPlayerHost //    
    //////////////////

    @Test
    void isPlayerHost_true() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsHost(true);

        assertTrue(lobbyService.isPlayerHost(lp));
    }

    @Test
    void isPlayerHost_false() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsHost(false);

        assertFalse(lobbyService.isPlayerHost(lp));
    }

    ///////////////////
    // isPlayerReady //    
    ///////////////////

    @Test
    void isPlayerReady_true() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsReady(true);

        assertTrue(lobbyService.isPlayerReady(lp));
    }

    @Test
    void isPlayerReady_false() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setIsReady(false);

        assertFalse(lobbyService.isPlayerReady(lp));
    }

    /////////////////////////
    // isPlayerInValidTeam //    
    /////////////////////////

    @Test
    void isPlayerInValidTeam_team1_true() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setTeamType(TeamType.Team1);

        assertTrue(lobbyService.isPlayerInValidTeam(lp));
    }

    @Test
    void isPlayerInValidTeam_team2_true() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setTeamType(TeamType.Team2);

        assertTrue(lobbyService.isPlayerInValidTeam(lp));
    }

    @Test
    void isPlayerInValidTeam_undecided_false() {
        LobbyPlayer lp = new LobbyPlayer();
        lp.setTeamType(TeamType.Undecided);

        assertFalse(lobbyService.isPlayerInValidTeam(lp));
    }

    /////////////////
    // isLobbyOpen //    
    /////////////////

    @Test
    void isLobbyOpen_open_true() {
        Lobby lobby = new Lobby();
        lobby.setGameId(null);

        assertTrue(lobbyService.isLobbyOpen(lobby));
    }

    @Test
    void isLobbyOpen_gameRunning_false() {
        Lobby lobby = new Lobby();
        lobby.setGameId(UUID.randomUUID());

        assertFalse(lobbyService.isLobbyOpen(lobby));
    }

    //////////////////////
    // generateJoinCode //    
    //////////////////////

    @Test
    void generateJoinCode_validFormat() {
        String code = lobbyService.generateJoinCode();

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("^[A-Z0-9]{6}$"));
    }


    /////////////////////
    // pushLobbyUpdate //
    /////////////////////

    @Test
    void pushLobbyUpdate_success_callsPusher() {
        Lobby lobby = new Lobby();
        UUID lobbyId = UUID.randomUUID();
        lobby.setId(lobbyId);

        lobbyService.pushLobbyUpdate(lobby);

        verify(pusherService).trigger(
            eq("lobby-" + lobbyId),
            eq("LobbyUpdate"),
            any(LobbyDTO.class)
        );
    }
    
}
