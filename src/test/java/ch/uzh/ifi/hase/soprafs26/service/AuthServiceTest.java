package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs26.entity.User;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // null token → 401
    @Test
    public void authenticateToken_nullToken_throwsException() {
        assertThrows(Exception.class, () -> authService.authenticateToken(null));
    }

    // empty token → 401
    @Test
    public void authenticateToken_emptyToken_throwsException() {
        assertThrows(Exception.class, () -> authService.authenticateToken(""));
    }

    // valid Bearer token → returns user
    @Test
    public void authenticateToken_validToken_returnsUser() {
        User user = new User();
        user.setUsername("testuser");

        Mockito.when(userService.getUserByToken("token123")).thenReturn(user);

        User result = authService.authenticateToken("Bearer token123");

        assertEquals("testuser", result.getUsername());
    }

    // no "Bearer " prefix → 401
    @Test
    public void extractTokenFromBearer_invalidFormat_throwsException() {
        assertThrows(Exception.class, () -> authService.extractTokenFromBearer("token123"));
    }

    // valid "Bearer " prefix → returns raw token
    @Test
    public void extractTokenFromBearer_validFormat_returnsToken() {
        String result = authService.extractTokenFromBearer("Bearer token123");

        assertEquals("token123", result);
    }
}
