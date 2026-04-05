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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameSettingsDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinCodeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ReadyStatusDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.TeamTypeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;






@RestController
public class LobbyController {

    private final AuthService authService;
    private final LobbyService lobbyService;
    private final UserService userService;

    public LobbyController(LobbyService lobbyService, UserService userService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.authService = authService;
    }



    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyJoinCodeDTO createLobby(@RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated and extract token from Bearer format
		String actualToken = authService.checkAuthToken(token);
        // get user from token
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found");
        }
        // create host player and Lobby
        LobbyPlayer player = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(player);
        
        return DTOMapper.INSTANCE.convertEntityToLobbyJoinCodeDTO(lobby);
    }


    @GetMapping("/lobbies/{lobbyId}/stream")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public SseEmitter getLobbyById(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated and extract token from Bearer format
		String actualToken = authService.checkAuthToken(token);

        // fetch user
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        if (lobbyPlayer== null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a player!");
        }
        
        // fetch Lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }


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
    public LobbyJoinCodeDTO postJoinLobby(@RequestBody LobbyDTO lobbyDTO, 
        @RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated and extract token from Bearer format
		String actualToken = authService.checkAuthToken(token);

        // get user from token
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found");
        }
        // get Lobby from Join Code
        Lobby lobbyFromJoinCode = DTOMapper.INSTANCE.convertLobbyDTOToEntity(lobbyDTO);
        Lobby lobbyToJoin = lobbyService.getLobbyByJoinCode(lobbyFromJoinCode.getJoinCode());

        // create non-host player and join lobby
        LobbyPlayer lobbyPlayer = lobbyService.createLobbyPlayer(user, false);
        lobbyService.joinLobby(lobbyPlayer, lobbyToJoin);


        return DTOMapper.INSTANCE.convertEntityToLobbyJoinCodeDTO(lobbyToJoin);
    }


    @PutMapping("/lobbies/{lobbyId}/players/{playerId}/team")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void  updateTeamSelection(@PathVariable UUID lobbyId,
        @PathVariable UUID playerId, @RequestBody TeamTypeDTO teamTypeDTO,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		String actualToken = authService.checkAuthToken(token);

        // verify id and token refer to the same lobbyPlayer
        verifyUserAuthorization(playerId, actualToken);
        
        // get TeamType 
        TeamType teamType = teamTypeDTO.getTeamType();
        if (teamType != TeamType.Team1 && teamType != TeamType.Team2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Team submitted!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerById(playerId);

        // fetch lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }

        lobbyService.updateTeamType(lobbyPlayer, teamType);

    }

    @PutMapping("/lobbies/{lobbyId}/players/{playerId}/ready")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void  updateReadyStatus(@PathVariable UUID lobbyId,
        @PathVariable UUID playerId, @RequestBody ReadyStatusDTO readyStatusDTO,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated 
		String actualToken = authService.checkAuthToken(token);
        
        // verify id and token refer to the same lobbyPlayer
        verifyUserAuthorization(playerId, actualToken);

        // get ready status 
        Boolean readyStatus = readyStatusDTO.getIsReady();
        if (readyStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ready status submitted!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerById(playerId);

        // fetch lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }

        lobbyService.updateReadyStatus(lobbyPlayer, readyStatus);

    }


    @PutMapping("/lobbies/{lobbyId}/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void  updateGameSettings(@PathVariable UUID lobbyId,
        @RequestBody GameSettingsDTO gameSettingsDTO,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated 
		String actualToken = authService.checkAuthToken(token);
        
        // fetch user
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        if (lobbyPlayer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a player!");
        }

        // get gameDuration
        Integer gameDuration = gameSettingsDTO.getGameDuration();
        // current bounds 5 -120 min
        if (gameDuration < 5 || gameDuration > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "game duration must be between 5 - 120 minutes");
        }

        // fetch lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }

        // verify that the lobbyPlayer is a host
        if (!lobbyService.isPlayerHost(lobbyPlayer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not Host!");
        }

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
        // Check if user is authenticated and extract token from Bearer format
		String actualToken = authService.checkAuthToken(token);
        
        // fetch user
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        if (lobbyPlayer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a player!");
        }

        // fetch lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);


        // verify that the lobbyPlayer is a host
        if (!lobbyService.isPlayerHost(lobbyPlayer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not Host!");
        }

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }
        
        // verify all lobbyPlayers currently in the lobby are "ready"
        if (!lobbyService.areAllLobbyPlayersReady(lobby)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not all Players are ready!");
        }
        
        /////////////////////////////////////////////////////
        /* Replace with GameService function to create a game
        gameService.createGame()
        */
        /////////////////////////////////////////////////////

        lobbyService.setAllLobbyPlayersReadyStatusToFalse(lobby);

    }
    

    @DeleteMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteLobby(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated 
		String actualToken = authService.checkAuthToken(token);  
        
        // fetch user
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        if (lobbyPlayer== null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a player!");
        }

        // fetch lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);

        // verify that the lobbyPlayer is a host
        if (!lobbyService.isPlayerHost(lobbyPlayer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not Host!");
        }

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }

        lobbyService.deleteLobby(lobby);
        
    }


    @DeleteMapping("/lobbies/{lobbyId}/players/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deleteLobbyPlayer(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated and extract token from Bearer format
		String actualToken = authService.checkAuthToken(token);
        
        // fetch user
        User user;
        try {
            user = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found!");
        }

        // fetch lobbyPlayer
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);
        if (lobbyPlayer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not a player!");
        }

        // fetch lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);    

        // verify lobbyPlayer is in lobby
        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }

        // verify that the lobbyPlayer is not a host
        if (lobbyService.isPlayerHost(lobbyPlayer)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                 "Wrong endpoint was used. Hosts need to use  DELETE /lobbies/{lobbyId}  !");
        }

        lobbyService.deleteLobbyPlayer(lobbyPlayer);

        }
    



    private void verifyUserAuthorization(UUID lobbyPlayerId, String token) {
		String actualToken = authService.extractTokenFromBearer(token);
		User user = userService.getUserByToken(actualToken);
        LobbyPlayer lobbyPlayer = lobbyService.getLobbyPlayerByUser(user);

		if (!lobbyPlayer.getId().equals(lobbyPlayerId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own profile!");
		}
	}
}
