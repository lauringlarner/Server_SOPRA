package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameSettingsDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinCodeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ReadyStatusDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TeamTypeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;






@RestController
public class LobbyController {

    private final AuthService authService;
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
    }



    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyJoinCodeDTO createLobby(@RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // create host player and create lobby
        // or BAD_REQUEST if user or userId is null
        // or CONFLICT if user already has a player or player already is in a lobby
        LobbyPlayer player = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(player);
        
        return DTOMapper.INSTANCE.convertEntityToLobbyJoinCodeDTO(lobby);
    }


    @GetMapping("/lobbies/{lobbyId}/stream")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public SseEmitter getLobbyById(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch lobbyPlayer and lobby or NOT_FOUND
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   

        // validate lobbyPlayer is in lobby or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);


        // Create SSE emitter
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Send initial lobby state
        try {
            emitter.send(SseEmitter.event()
                .name("lobbyUpdate")
                .data(DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby)));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        // Register emitter for future updates
        lobbyService.registerLobbyEmitter(lobbyId, emitter);

        emitter.onCompletion(() -> lobbyService.removeLobbyEmitter(lobbyId, emitter));
        emitter.onTimeout(() -> lobbyService.removeLobbyEmitter(lobbyId, emitter));

        return emitter;
            
        }
    

    @PostMapping("/lobbies/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyJoinCodeDTO postJoinLobby(@RequestBody LobbyJoinCodeDTO lobbyJoinCodeDTO, 
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        
        // get Lobby from Join Code and fetch lobby 
        // or BAD_REQUEST on deformed joinCode
        // or NOT_FOUND on not finding lobby by join code
        String joinCode = lobbyJoinCodeDTO.getJoinCode();
        Lobby lobby = lobbyService.getLobbyByJoinCode(joinCode);

        // create non-host player and join lobby
        // or BAD_REQUEST if user or userId is null
        // or CONFLICT if user already has a player
        LobbyPlayer lobbyPlayer = lobbyService.createLobbyPlayer(user, false);
        lobbyService.joinLobby(lobbyPlayer, lobby);

        return lobbyJoinCodeDTO;
    }


    @PutMapping("/lobbies/{lobbyId}/players/{playerId}/team")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void  updateTeamSelection(@PathVariable UUID lobbyId,
        @PathVariable UUID playerId, @RequestBody TeamTypeDTO teamTypeDTO,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // authenticate and return user or UNAUTHORIZED
		User user = authService.authenticateToken(token);

        // validate playerId and user correspond to same lobbyPlayer or FORBIDDEN
        lobbyService.validateUserMatchesLobbyPlayerId(playerId, user);
        
        // fetch lobbyPlayer and lobby or NOT_FOUND
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerById(playerId);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   
        
        // validate lobbyPlayer is in lobby or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        
        // get team type and update lobbyPlayer or BAD_REQUEST
        TeamType teamType = teamTypeDTO.getTeamType();
        lobbyService.updateTeamType(lobbyPlayer, teamType);

    }

    @PutMapping("/lobbies/{lobbyId}/players/{playerId}/ready")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void  updateReadyStatus(@PathVariable UUID lobbyId,
        @PathVariable UUID playerId, @RequestBody ReadyStatusDTO readyStatusDTO,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // authenticate and return user or UNAUTHORIZED
		User user = authService.authenticateToken(token);

        // validate playerId and user correspond to same lobbyPlayer or FORBIDDEN
        lobbyService.validateUserMatchesLobbyPlayerId(playerId, user);

        // fetch lobbyPlayer and lobby or NOT_FOUND
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerById(playerId);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   
        
        // validate lobbyPlayer is in lobby or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        
        // get ready status and update lobbyPlayer or BAD_REQUEST
        Boolean readyStatus = readyStatusDTO.getIsReady();
        lobbyService.updateReadyStatus(lobbyPlayer, readyStatus);
    }


    @PutMapping("/lobbies/{lobbyId}/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void  updateGameSettings(@PathVariable UUID lobbyId,
        @RequestBody GameSettingsDTO gameSettingsDTO,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch lobbyPlayer and lobby from user or Not Found
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   
        
        // validate lobbyPlayer is in lobby and is a host or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        lobbyService.validateLobbyPlayerIsHost(lobbyPlayer);
        
        // get gameDuration and update lobbys gameDuration or BAD_REQUEST
        Integer gameDuration = gameSettingsDTO.getGameDuration();
        lobbyService.updateLobbySettings(lobby, gameDuration);

    }

    ///////////////////////////////////////
    // currenly doesn't return anything. later it needs to return at least the Game id per DTO
    ///////////////////////////////////////
    @PostMapping("/lobbies/{lobbyId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createGame(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch lobbyPlayer and lobby or Not Found
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   

        // validate lobbyPlayer is in lobby and is a host or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        lobbyService.validateLobbyPlayerIsHost(lobbyPlayer);
        
        // validate all lobbyPlayers currently in the lobby are "ready" and the lobby is OPEN or CONFLICT
        lobbyService.validateAllPlayersReady(lobby);
        lobbyService.validateLobbyIsOpen(lobby);
        
        /////////////////////////////////////////////////////
        /* Replace with GameService function to create a game
        gameService.createGame()
        */
        /////////////////////////////////////////////////////
        
        // set all lobbyPlayers, currently in lobby, ready status to false and set lobby status to running
        lobbyService.updateAllLobbyPlayersReadyStatusToFalse(lobby);
        lobbyService.setLobbyStatusRunning(lobby);
    }
    

    @DeleteMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteLobby(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch lobbyPlayer and lobby or NOT_FOUND
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   

        // validate lobbyPlayer is in lobby and is a host or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);
        lobbyService.validateLobbyPlayerIsHost(lobbyPlayer);

        // delete lobby
        lobbyService.deleteLobby(lobby);
    }


    @DeleteMapping("/lobbies/{lobbyId}/players/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteLobbyPlayer(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch lobbyPlayer and lobby or NOT_FOUND
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);    

        // validate lobbyPlayer is in lobby or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        // validate lobbyPlayer is not a host or CONFLICT
        lobbyService.validateLobbyPlayerIsNotHost(lobbyPlayer);

        // delete lobbyPlayer
        lobbyService.deleteLobbyPlayer(lobbyPlayer);
        }
    
}
