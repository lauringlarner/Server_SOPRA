package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
public class LobbyController {

    private final LobbyService lobbyService;
    private final UserService userService;

    public LobbyController(LobbyService lobbyService, UserService userService) {
        this.lobbyService = lobbyService;
        this.userService = userService;
    }



    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyDTO createLobby(@RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		checkAuthToken(token);
        String actualToken = extractTokenFromBearer(token);
        // get user from token
        User host;
        try {
            host = userService.getUserByToken(actualToken);
        } catch (ResponseStatusException ex) {
             throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The tokens corresponding User was not found");
        }
        // create host player and Lobby
        LobbyPlayer player = lobbyService.createLobbyPlayer(host, true);
        Lobby lobby = lobbyService.createLobby(player);
        
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
    }


    @GetMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyDTO getLobbyById(@PathVariable UUID lobbyId,
        @RequestHeader(value = "Authorization", required = false) String token) {
        // Check if user is authenticated
		checkAuthToken(token);

        // fetch Lobby
        Lobby lobby = lobbyService.getLobbyByLobbyId(lobbyId);

        // convert to DTO
        return DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
        
    }
    

    //@PostMapping("/lobbies/join")
    //@ResponseStatus(HttpStatus.OK)
    //@ResponseBody
    //public LobbyDTO postJoinLobbyByJoinCode(@RequestBody String entity) {
        //TODO: process POST request
        
    //    return entity;
    //}
    




	private void checkAuthToken(String token) {
		if (token == null || token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required!");
		}
		// Extract token from Bearer format if present
		String actualToken = extractTokenFromBearer(token);
		// Validate token by retrieving user - throws 401 if token is invalid
		userService.getUserByToken(actualToken);
	}

	private String extractTokenFromBearer(String authHeader) {
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7); // Remove "Bearer " prefix (7 characters)
		}
		return authHeader;
	}


}
