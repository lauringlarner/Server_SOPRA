package ch.uzh.ifi.hase.soprafs26.controller;

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
import ch.uzh.ifi.hase.soprafs26.rest.dto.CreateLobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;


@RestController
public class LobbyController {

    private final LobbyService lobbyService;
    private final UserService userService;

    public LobbyController(LobbyService lobbyService, UserService userService) {
        this.lobbyService = lobbyService;
        this.userService = userService;
    }



    @PostMapping("lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public CreateLobbyResponseDTO createLobby(@RequestHeader(value = "Authorization", required = false) String token) {
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
        // return Lobby code
        return DTOMapper.INSTANCE.convertEntityToCreateLobbyResponseDTO(lobby);
    }





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
