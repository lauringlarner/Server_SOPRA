package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessInfoDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinCodeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ReadyStatusDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TeamTypeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.GameOrchestrationService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;






@RestController
@Transactional
public class LobbyController {

    private final AuthService authService;
    private final LobbyService lobbyService;
    private final GameOrchestrationService gameOrchestrationService;

    public LobbyController(LobbyService lobbyService, AuthService authService, GameOrchestrationService gameOrchestrationService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
        this.gameOrchestrationService = gameOrchestrationService;
    }



    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyAccessInfoDTO createLobby(@RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // create host player and create lobby or CONFLICT if user already has a player
        LobbyPlayer player = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(player);
        
        return DTOMapper.INSTANCE.convertEntityToLobbyAccessInfoDTO(lobby);
    }


    @GetMapping(value = "/lobbies/{lobbyId}/stream", produces = "text/event-stream")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<SseEmitter> getLobbyByIdEmitter(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);

        // fetch lobbyPlayer and lobby or NOT_FOUND
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);   

        // validate lobbyPlayer is in lobby or FORBIDDEN
        lobbyService.validateLobbyPlayerInLobby(lobbyPlayer, lobby);

        SseEmitter emitter = lobbyService.createAndRegisterLobbyStream(lobby);

        return ResponseEntity.ok()
            .header("Cache-Control", "no-cache, no-transform")
            .header("Connection", "keep-alive")
            .header("X-Accel-Buffering", "no")
            .body(emitter);
    }
    

    @PostMapping("/lobbies/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyAccessInfoDTO postJoinLobby(@RequestBody LobbyJoinCodeDTO lobbyJoinCodeDTO, 
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        
        // get Lobby from Join Code and fetch lobby 
        // or BAD_REQUEST on deformed joinCode
        // or NOT_FOUND on not finding lobby by join code
        String joinCode = lobbyJoinCodeDTO.getJoinCode();
        Lobby lobby = lobbyService.getLobbyByJoinCode(joinCode);

        // create non-host player and join lobby or CONFLICT if user already has a player
        LobbyPlayer lobbyPlayer = lobbyService.createLobbyPlayer(user, false);
        lobbyService.joinLobby(lobbyPlayer, lobby);

        return DTOMapper.INSTANCE.convertEntityToLobbyAccessInfoDTO(lobby);
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


    @PostMapping("/lobbies/{lobbyId}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public void createGame(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// authenticate and return user or UNAUTHORIZED
        User user = authService.authenticateToken(token);
        
        // starts game
        gameOrchestrationService.startGame(user, lobbyId);
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
        lobbyService.deleteLobby(lobbyPlayer, lobby);
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
