package ch.uzh.ifi.hase.soprafs26.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameSettingsDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinCodeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ReadyStatusDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TeamTypeDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameOrchestrationService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.service.PusherService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LobbyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private LobbyPlayerRepository lobbyPlayerRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private LobbyService lobbyService;

    @MockitoBean
    private GameOrchestrationService gameOrchestrationService;

    @MockitoBean
    private PusherService pusherService;


    @Test
    void createLobby_invalidToken_Unauthorized() throws Exception {
        // given 
        String badToken = "badToken";

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createLobby_userAlreadyHasPlayer_Conflict() throws Exception {
        // given
        String token = "token";
        User userHasPlayer = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(userHasPlayer);
        when(lobbyService.createLobbyPlayer(userHasPlayer, true))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User already has player"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void createLobby_success_Created() throws Exception {
        // given
        String token = "token";
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        Lobby lobby = new Lobby();
        lobby.setJoinCode("testJoinCode");

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.createLobbyPlayer(user, true)).thenReturn(lobbyPlayer);
        when(lobbyService.createLobby(lobbyPlayer)).thenReturn(lobby);

        MockHttpServletRequestBuilder postRequest = post("/lobbies")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",is(lobby.getId())))
                .andExpect(jsonPath("$.joinCode",is(lobby.getJoinCode())));
    }


    @Test
    void getLobbyById_invalidUser_NotFound() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User userWithoutPlayer = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(userWithoutPlayer);
        when(lobbyService.getLobbyPlayerByUser(userWithoutPlayer))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        MockHttpServletRequestBuilder getRequest = get("/lobbies/{lobbyId}",lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void getLobbyById_invalidLobbyId_NotFound() throws Exception {
        // given
        String token = "token";
        UUID badLobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(badLobbyId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder getRequest = get("/lobbies/{lobbyId}",badLobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void getLobbyById_lobbyPlayerNotInLobby_Forbidden() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "LobbyPlayer not in Lobby"))
                .when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder getRequest = get("/lobbies/{lobbyId}",lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isForbidden());
    }



    @Test
    void joinLobby_invalidToken_Unauthorized() throws Exception {
        // given
        String badToken = "badToken";
        LobbyJoinCodeDTO lobbyJoinCodeDTO = new LobbyJoinCodeDTO();
        lobbyJoinCodeDTO.setJoinCode("testJoinCode");

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        
        MockHttpServletRequestBuilder postRequest = post("/lobbies/join")
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinCodeDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }  

    @Test
    void joinLobby_invalidJoinCodeDeformed_BadRequest() throws Exception {
        // given
        String token = "token";
        User user = new User();
        LobbyJoinCodeDTO lobbyJoinCodeDTO = new LobbyJoinCodeDTO();
        lobbyJoinCodeDTO.setJoinCode("badDeformedJoinCode");

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyByJoinCode(lobbyJoinCodeDTO.getJoinCode()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deformed Code"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/join")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinCodeDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }   

    @Test
    void joinLobby_invalidJoinCodeCorrectlyFormed_NotFound() throws Exception {
        // given
        String token = "token";
        User user = new User();
        LobbyJoinCodeDTO lobbyJoinCodeDTO = new LobbyJoinCodeDTO();
        lobbyJoinCodeDTO.setJoinCode("badJoinCode");

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyByJoinCode(lobbyJoinCodeDTO.getJoinCode()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/join")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinCodeDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }   

    @Test
    void joinLobby_userAlreadyHasPlayer_Conflict() throws Exception {
        // given
        String token = "token";
        User user = new User();
        LobbyJoinCodeDTO lobbyJoinCodeDTO = new LobbyJoinCodeDTO();
        lobbyJoinCodeDTO.setJoinCode("JoinCode");
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyByJoinCode(lobbyJoinCodeDTO.getJoinCode())).thenReturn(lobby);
        when(lobbyService.createLobbyPlayer(user, false))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User already has a player"));

        MockHttpServletRequestBuilder postRequest = post("/lobbies/join")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinCodeDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void joinLobby_lobbyNotOpen_Conflict() throws Exception {
        // given
        String token = "token";
        User user = new User();
        LobbyJoinCodeDTO lobbyJoinCodeDTO = new LobbyJoinCodeDTO();
        lobbyJoinCodeDTO.setJoinCode("JoinCode");
        Lobby lobby = new Lobby();
        lobby.setGameId(UUID.randomUUID());
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(false);

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyByJoinCode(lobbyJoinCodeDTO.getJoinCode())).thenReturn(lobby);
        when(lobbyService.createLobbyPlayer(user, false)).thenReturn(lobbyPlayer);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Lobby not open "))
                .when(lobbyService).joinLobby(lobbyPlayer, lobby);
                
        MockHttpServletRequestBuilder postRequest = post("/lobbies/join")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinCodeDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void joinLobby_success_Conflict() throws Exception {
        // given
        String token = "token";
        User user = new User();
        LobbyJoinCodeDTO lobbyJoinCodeDTO = new LobbyJoinCodeDTO();
        lobbyJoinCodeDTO.setJoinCode("JoinCode");
        Lobby lobby = new Lobby();
        lobby.setGameId(UUID.randomUUID());
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(false);

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyByJoinCode(lobbyJoinCodeDTO.getJoinCode())).thenReturn(lobby);
        when(lobbyService.createLobbyPlayer(user, false)).thenReturn(lobbyPlayer);
        when(lobbyService.joinLobby(lobbyPlayer, lobby)).thenReturn(lobby);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/join")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(lobbyJoinCodeDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",is(lobby.getId())))
                .andExpect(jsonPath("$.joinCode",is(lobby.getJoinCode())));
    }


    @Test
    void updateTeamSelection_invalidToken_Unauthorized() throws Exception { 
        // given
        String badToken = "badToken";
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        TeamTypeDTO teamTypeDTO = new TeamTypeDTO();
        teamTypeDTO.setTeamType(TeamType.Team1);

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        
        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/team", lobbyId, playerId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamTypeDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTeamSelection_UserAndPlayerIdMismatch_Forbidden() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        TeamTypeDTO teamTypeDTO = new TeamTypeDTO();
        teamTypeDTO.setTeamType(TeamType.Team1);

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User and playerId do not belong to same player"))       
                .when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/team", lobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamTypeDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTeamSelection_invalidlobbyId_NotFound() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID badLobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        TeamTypeDTO teamTypeDTO = new TeamTypeDTO();
        teamTypeDTO.setTeamType(TeamType.Team1);

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doNothing().when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);
        when(lobbyService.getLobbyByLobbyId(badLobbyId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/team", badLobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamTypeDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTeamSelection_lobbyPlayerNotInLobby_Forbidden() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        TeamTypeDTO teamTypeDTO = new TeamTypeDTO();
        teamTypeDTO.setTeamType(TeamType.Team1);
        Lobby lobby = new Lobby();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doNothing().when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);
        when(lobbyService.getLobbyPlayerById(playerId)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "LobbyPlayer not in Lobby"))
                .when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/team", lobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamTypeDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTeamSelection_success_NoContent() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        TeamTypeDTO teamTypeDTO = new TeamTypeDTO();
        teamTypeDTO.setTeamType(TeamType.Team1);
        Lobby lobby = new Lobby();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doNothing().when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doNothing().when(lobbyService).updateTeamType(lobbyPlayer, teamTypeDTO.getTeamType());

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/team", lobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(teamTypeDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNoContent());
    }


    @Test
    void updateReadyStatus_invalidToken_Unauthorized() throws Exception { 
        // given
        String badToken = "badToken";
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        ReadyStatusDTO readyStatusDTO = new ReadyStatusDTO();
        readyStatusDTO.setIsReady(true);

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        
        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/ready", lobbyId, playerId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(readyStatusDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReadyStatus_UserAndPlayerIdMismatch_Forbidden() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        ReadyStatusDTO readyStatusDTO = new ReadyStatusDTO();
        readyStatusDTO.setIsReady(true);

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User and playerId do not belong to same player"))       
                .when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/ready", lobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(readyStatusDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReadyStatus_invalidlobbyId_NotFound() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID badLobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        ReadyStatusDTO readyStatusDTO = new ReadyStatusDTO();
        readyStatusDTO.setIsReady(true);

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doNothing().when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);
        when(lobbyService.getLobbyByLobbyId(badLobbyId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/ready", badLobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(readyStatusDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReadyStatus_lobbyPlayerNotInLobby_Forbidden() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        ReadyStatusDTO readyStatusDTO = new ReadyStatusDTO();
        readyStatusDTO.setIsReady(true);
        Lobby lobby = new Lobby();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doNothing().when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);
        when(lobbyService.getLobbyPlayerById(playerId)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "LobbyPlayer not in Lobby"))
                .when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/ready", lobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(readyStatusDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReadyStatus_success_NoContent() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        ReadyStatusDTO readyStatusDTO = new ReadyStatusDTO();
        readyStatusDTO.setIsReady(true);
        Lobby lobby = new Lobby();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doNothing().when(lobbyService).validateUserMatchesLobbyPlayerId(playerId, user);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doNothing().when(lobbyService).updateReadyStatus(lobbyPlayer, readyStatusDTO.getIsReady());

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/players/{playerId}/ready", lobbyId, playerId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(readyStatusDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNoContent());
    }


    @Test
    void updateGameSettings_invalidToken_Unauthorized() throws Exception { 
        // given
        String badToken = "badToken";
        UUID lobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(10);

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        
        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", lobbyId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateGameSettings_InvalidUser_NotFound() throws Exception { 
        // given
        String token = "token";
        User userWithoutPlayer = new User();
        UUID lobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(10);

        // when
        when(authService.authenticateToken(token)).thenReturn(userWithoutPlayer);
        when(lobbyService.getLobbyPlayerByUser(userWithoutPlayer))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateGameSettings_invalidlobbyId_NotFound() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID badLobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(10);
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(badLobbyId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", badLobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateGameSettings_lobbyPlayerNotInLobby_Forbidden() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(10);
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "LobbyPlayer not in Lobby"))
                .when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateGameSettings_lobbyPlayerIsHost_Forbidden() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(10);
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(false);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "LobbyPlayer is not host"))
                .when(lobbyService).validateLobbyPlayerIsHost(lobbyPlayer);

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateGameSettings_invalidGameDuration_BadRequest() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(-5);
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(true);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doNothing().when(lobbyService).validateLobbyPlayerIsHost(lobbyPlayer);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid gameDuration"))
                .when(lobbyService).updateLobbySettings(lobby, gameSettingsDTO.getGameDuration());

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGameSettings_success_NoContent() throws Exception { 
        // given
        String token = "token";
        User user = new User();
        UUID lobbyId = UUID.randomUUID();
        GameSettingsDTO gameSettingsDTO = new GameSettingsDTO();
        gameSettingsDTO.setGameDuration(-5);
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(true);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doNothing().when(lobbyService).validateLobbyPlayerIsHost(lobbyPlayer);
        doNothing().when(lobbyService).updateLobbySettings(lobby, gameSettingsDTO.getGameDuration());

        MockHttpServletRequestBuilder putRequest = put("/lobbies/{lobbyId}/settings", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gameSettingsDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNoContent());
    }


    @Test
    void createGame_invalidToken_Unauthorized() throws Exception { 
        // given
        String badToken = "badToken";
        UUID lobbyId = UUID.randomUUID();

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        
        MockHttpServletRequestBuilder putRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGame_invalidLobbyId_NotFound() throws Exception {
        // given
        String token = "token";
        UUID badLobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"))
                .when(gameOrchestrationService).startGame(user, badLobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", badLobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void createGame_invalidUser_NotFound() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void createGame_lobbyPlayerNotInLobby_Forbidden() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not in lobby"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void createGame_notHost_Forbidden() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not host"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void createGame_notAllPlayersReady_Conflict() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Not all ready"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void createGame_lobbyNotOpen_Conflict() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Game already running"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void createGame_playersNotInValidTeams_Conflict() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Invalid teams"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void createGame_missingPlayersInTeams_Conflict() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Team empty"))
                .when(gameOrchestrationService).startGame(user, lobbyId);

        MockHttpServletRequestBuilder postRequest = post("/lobbies/{lobbyId}/start", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void createGame_success_Created() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        Game game = new Game();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(gameOrchestrationService.startGame(user, lobbyId)).thenReturn(game);

        MockHttpServletRequestBuilder postRequest =
                post("/lobbies/{lobbyId}/start", lobbyId)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated());
    }


    @Test
    void deleteLobby_invalidToken_Unauthorized() throws Exception {
        // given
        String badToken = "badToken";
        UUID lobbyId = UUID.randomUUID();

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}", lobbyId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteLobby_invalidUser_NotFound() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User userWithoutPlayer = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(userWithoutPlayer);
        when(lobbyService.getLobbyPlayerByUser(userWithoutPlayer))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLobby_invalidLobbyId_NotFound() throws Exception {
        // given
        String token = "token";
        UUID badLobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(badLobbyId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}", badLobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLobby_lobbyPlayerNotInLobby_Forbidden() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Player not in lobby"))
                .when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteLobby_lobbyPlayerNotHost_Forbidden() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(false);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not host"))
                .when(lobbyService).validateLobbyPlayerIsHost(lobbyPlayer);

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteLobby_success_NoContent() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(true);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doNothing().when(lobbyService).validateLobbyPlayerIsHost(lobbyPlayer);
        doNothing().when(lobbyService).deleteLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }


    @Test
    void deleteLobbyPlayer_invalidToken_Unauthorized() throws Exception {
        // given
        String badToken = "badToken";
        UUID lobbyId = UUID.randomUUID();

        // when
        when(authService.authenticateToken(badToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}/players/me", lobbyId)
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteLobbyPlayer_invalidUser_NotFound() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User userWithoutPlayer = new User();

        // when
        when(authService.authenticateToken(token)).thenReturn(userWithoutPlayer);
        when(lobbyService.getLobbyPlayerByUser(userWithoutPlayer))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}/players/me", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLobbyPlayer_invalidLobbyId_NotFound() throws Exception {
        // given
        String token = "token";
        UUID badLobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(badLobbyId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}/players/me", badLobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLobbyPlayer_lobbyPlayerNotInLobby_Forbidden() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "LobbyPlayer not in lobby"))
                .when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}/players/me", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteLobbyPlayer_isHost_Conflict() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(true);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Host cannot leave with this endpoint"))
                .when(lobbyService).validateLobbyPlayerIsNotHost(lobbyPlayer);

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}/players/me", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isConflict());
    }

    @Test
    void deleteLobbyPlayer_success_NoContent() throws Exception {
        // given
        String token = "token";
        UUID lobbyId = UUID.randomUUID();
        User user = new User();
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setIsHost(false);
        Lobby lobby = new Lobby();

        // when
        when(authService.authenticateToken(token)).thenReturn(user);
        when(lobbyService.getLobbyPlayerByUser(user)).thenReturn(lobbyPlayer);
        when(lobbyService.getLobbyByLobbyId(lobbyId)).thenReturn(lobby);
        doNothing().when(lobbyService).validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        doNothing().when(lobbyService).validateLobbyPlayerIsNotHost(lobbyPlayer);
        doNothing().when(lobbyService).deleteLobbyPlayer(lobbyPlayer);

        MockHttpServletRequestBuilder deleteRequest = delete("/lobbies/{lobbyId}/players/me", lobbyId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }















	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}
