package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinCodeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;





@RestController
public class LobbyController {

    private final LobbyPlayerRepository lobbyPlayerRepository;
    private final LobbyService lobbyService;
    private final UserService userService;

    public LobbyController(LobbyService lobbyService, UserService userService, LobbyPlayerRepository lobbyPlayerRepository) {
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
    }



    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyJoinCodeDTO createLobby(@RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		checkAuthToken(token);
        String actualToken = extractTokenFromBearer(token);
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


    @GetMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyDTO getLobbyById(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		checkAuthToken(token);
        String actualToken = extractTokenFromBearer(token);

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

        if (lobbyPlayer.getLobby() != lobby) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User doesn't belong to Lobby!");
        }

        // convert to DTO
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
        
    }
    

    @PostMapping("/lobbies/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody

    public LobbyJoinCodeDTO postJoinLobby(@RequestBody LobbyDTO lobbyDTO, 
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		checkAuthToken(token);
        String actualToken = extractTokenFromBearer(token);

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
    

    




	private void checkAuthToken(String token) {
		if (token == null || token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required!");
		}
		// Extract token from Bearer format if present
		String actualToken = extractTokenFromBearer(token);
		// Validate token by retrieving user - throws 401 if token is invalid
		try {
            userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found");
        }
	}

	private String extractTokenFromBearer(String authHeader) {
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7); // Remove "Bearer " prefix (7 characters)
		}
		return authHeader;
	}


}
