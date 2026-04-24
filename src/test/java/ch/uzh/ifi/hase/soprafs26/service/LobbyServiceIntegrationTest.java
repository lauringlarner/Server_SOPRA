package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import jakarta.transaction.Transactional;

@Transactional
@SpringBootTest
public class LobbyServiceIntegrationTest {
    
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private LobbyRepository lobbyRepository;

	@Autowired
	private LobbyPlayerRepository lobbyPlayerRepository;

	@Autowired
	private LobbyService lobbyService;

    @MockitoBean
    private PusherService pusherService;


    @Test
    void createLobbyPlayer_persistsInDatabase() {
        User user = createUserForTests();
        userRepository.flush();

        LobbyPlayer lp = lobbyService.createLobbyPlayer(user, true);

        LobbyPlayer found = lobbyPlayerRepository.findById(lp.getId()).orElse(null);

        assertNotNull(found);
        assertEquals(user.getId(), found.getUser().getId());
        assertTrue(found.getIsHost());
    }

    @Test
    void createLobby_persistsLobbyAndPlayerRelationship() {
        User user = createUserForTests();

        LobbyPlayer lp = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(lp);

        Lobby saved = lobbyRepository.findById(lobby.getId()).orElse(null);

        assertNotNull(saved);
        assertEquals(1, saved.getLobbyPlayers().size());
        assertEquals(lp.getId(), saved.getLobbyPlayers().get(0).getId());
    }

    @Test
    void joinLobby_persistsPlayerInLobby() {
       User userPlayer = createUserForTests("_player");
       User userHost = createUserForTests("_host");

        LobbyPlayer lp = lobbyService.createLobbyPlayer(userPlayer, false);

        Lobby hostLobby = lobbyService.createLobby(
            lobbyService.createLobbyPlayer(userHost, true)
        );

        lobbyService.joinLobby(lp, hostLobby);

        Lobby reloaded = lobbyRepository.findById(hostLobby.getId()).orElse(null);

        assertEquals(2, reloaded.getLobbyPlayers().size());
    }

    @Test
    void deleteLobbyPlayer_removesFromDatabaseAndLobby() {
       User user = createUserForTests();
        LobbyPlayer lp = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(lp);

        lobbyService.deleteLobbyPlayer(lp);

        assertFalse(lobbyPlayerRepository.findById(lp.getId()).isPresent());

        Lobby updated = lobbyRepository.findById(lobby.getId()).orElse(null);
        assertEquals(0, updated.getLobbyPlayers().size());
    }

    @Test
    void resetLobbyAfterGame_resetsGameIdAndReadyStatus() {
        User user = createUserForTests();

        LobbyPlayer lp = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(lp);

        lobbyService.updateReadyStatus(lp, true);

        lobbyService.resetLobbyAfterGame(lobby.getId());

        Lobby reloaded = lobbyRepository.findById(lobby.getId()).orElse(null);

        assertNull(reloaded.getGameId());
        assertFalse(reloaded.getLobbyPlayers().get(0).getIsReady());
    }

    @Test
    void updateLobbySettings_persistsGameDuration() {
        User user = createUserForTests();

        LobbyPlayer lp = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(lp);

        lobbyService.updateLobbySettings(lobby, 15);

        Lobby reloaded = lobbyRepository.findById(lobby.getId()).orElse(null);

        assertEquals(15, reloaded.getGameDuration());
    }

    @Test
    void setLobbyGameId_persistsGameId() {
        User user = createUserForTests();

        LobbyPlayer lp = lobbyService.createLobbyPlayer(user, true);
        Lobby lobby = lobbyService.createLobby(lp);

        UUID gameId = UUID.randomUUID();
        lobbyService.setLobbyGameId(lobby, gameId);

        Lobby reloaded = lobbyRepository.findById(lobby.getId()).orElse(null);

        assertEquals(gameId, reloaded.getGameId());
    }

    @Test
    void fullLobbyLifecycle_flowWorks() {
        User userHost = createUserForTests("_host");
        User userPlayer = createUserForTests("player");

        LobbyPlayer hostLp = lobbyService.createLobbyPlayer(userHost, true);
        Lobby lobby = lobbyService.createLobby(hostLp);

        LobbyPlayer playerLp = lobbyService.createLobbyPlayer(userPlayer, false);
        lobbyService.joinLobby(playerLp, lobby);

        lobbyService.updateTeamType(playerLp, TeamType.Team1);
        lobbyService.updateReadyStatus(playerLp, true);

        lobbyService.setLobbyGameId(lobby, UUID.randomUUID());

        Lobby reloaded = lobbyRepository.findById(lobby.getId()).orElse(null);

        assertEquals(2, reloaded.getLobbyPlayers().size());
    }


    // helpers to create users
    private User createUserForTests() {
        return createUserForTests("");
    }

    private User createUserForTests(String suffix) {
        User user = new User();
        user.setEmail("test" + suffix + "@email.com");
        user.setUsername("testusername" + suffix);
        user.setToken(UUID.randomUUID().toString());
        user.setStatus(UserStatus.OFFLINE);
        user.setPassword("password");
        user.setGamesPlayed(0);
        user.setGamesWon(0);
        user.setCorrectItemsFound(0);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
