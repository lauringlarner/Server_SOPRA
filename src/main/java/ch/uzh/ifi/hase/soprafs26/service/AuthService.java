package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public void checkAuthToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization token is required!");
        }
        String actualToken = extractTokenFromBearer(token);
        if (!userService.isValidToken(actualToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
        }
    }

    public String extractTokenFromBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
