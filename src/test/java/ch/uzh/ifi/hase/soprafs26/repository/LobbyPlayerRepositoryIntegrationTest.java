package ch.uzh.ifi.hase.soprafs26.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;

@DataJpaTest
public class LobbyPlayerRepositoryIntegrationTest {

    @Autowired
	private TestEntityManager entityManager;
    
    @Autowired
	private LobbyPlayerRepository lobbyPlayerRepository;


    @Test
    public void findById_returnsLobbyPlayer() {
        // given
        User user = generateUserForTests();
        Lobby lobby = generateLobbyForTests();
        LobbyPlayer lobbyPlayer = generateLobbyPlayerForTests(user, lobby);

        lobby.addPlayer(lobbyPlayer);

        entityManager.persist(user);
        entityManager.persist(lobby);
        entityManager.persist(lobbyPlayer);
        entityManager.flush();
        entityManager.clear();

        // when
        LobbyPlayer foundLobbyPlayer = lobbyPlayerRepository.findById(lobbyPlayer.getId()).orElse(null);

        // then
        assertNotNull(foundLobbyPlayer);
        assertEquals(lobbyPlayer.getId(), foundLobbyPlayer.getId());
        assertEquals(lobbyPlayer.getJoinedAt().withNano(0), foundLobbyPlayer.getJoinedAt().withNano(0));
        assertEquals(lobbyPlayer.getTeamType(), foundLobbyPlayer.getTeamType());
        assertEquals(lobbyPlayer.getIsHost(), foundLobbyPlayer.getIsHost());
        assertEquals(lobbyPlayer.getIsReady(), foundLobbyPlayer.getIsReady());
        assertEquals(lobbyPlayer.getUser().getId(), foundLobbyPlayer.getUser().getId());
        assertEquals(lobbyPlayer.getLobby().getId(), foundLobbyPlayer.getLobby().getId());
    }
    
    @Test
    public void findById_notFound() {
        // given
        UUID lobbyPlayerIdWithoutLobbyPlayer = UUID.randomUUID();

        // when
        LobbyPlayer foundLobbyPlayer = lobbyPlayerRepository.findById(lobbyPlayerIdWithoutLobbyPlayer).orElse(null);

        // then
        assertNull(foundLobbyPlayer);
    }

    @Test
    public void findByUser_returnsLobbyPlayer() {
        // given
        User user = generateUserForTests();
        Lobby lobby = generateLobbyForTests();
        LobbyPlayer lobbyPlayer = generateLobbyPlayerForTests(user, lobby);

        lobby.addPlayer(lobbyPlayer);

        entityManager.persist(user);
        entityManager.persist(lobby);
        entityManager.persist(lobbyPlayer);
        entityManager.flush();
        entityManager.clear();

        // when
        LobbyPlayer foundLobbyPlayer = lobbyPlayerRepository.findByUser(user);

        // then
        assertNotNull(foundLobbyPlayer);
        assertEquals(lobbyPlayer.getId(), foundLobbyPlayer.getId());
        assertEquals(lobbyPlayer.getJoinedAt().withNano(0), foundLobbyPlayer.getJoinedAt().withNano(0));
        assertEquals(lobbyPlayer.getTeamType(), foundLobbyPlayer.getTeamType());
        assertEquals(lobbyPlayer.getIsHost(), foundLobbyPlayer.getIsHost());
        assertEquals(lobbyPlayer.getIsReady(), foundLobbyPlayer.getIsReady());
        assertEquals(lobbyPlayer.getUser().getId(), foundLobbyPlayer.getUser().getId());
        assertEquals(lobbyPlayer.getLobby().getId(), foundLobbyPlayer.getLobby().getId());
    }

    @Test
    public void findByUser_notFound() {
        // given
        User userWithNoPlayer = generateUserForTests();

        entityManager.persist(userWithNoPlayer);
        entityManager.flush();
        entityManager.clear();

        // when
        LobbyPlayer foundLobbyPlayer = lobbyPlayerRepository.findByUser(userWithNoPlayer);

        // then
        assertNull(foundLobbyPlayer);
    }
    

    // helpers to generate lobbyPlayer 
    private LobbyPlayer generateLobbyPlayerForTests(User user, Lobby lobby) {
        LobbyPlayer lobbyPlayer = new LobbyPlayer();
        lobbyPlayer.setJoinedAt(LocalDateTime.now());
        lobbyPlayer.setTeamType(TeamType.Team1);
        lobbyPlayer.setIsHost(true);
        lobbyPlayer.setIsReady(false);
        lobbyPlayer.setUser(user);
        lobbyPlayer.setLobby(lobby);
        return lobbyPlayer;
    }

    private User generateUserForTests() {
        User user = new User();
        user.setEmail("test@email");
		user.setUsername("TestUserName");
		user.setToken("1");
		user.setStatus(UserStatus.OFFLINE);
		user.setPassword("password");
		user.setGamesPlayed(2);
        user.setGamesWon(3);
        user.setCorrectItemsFound(4);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private Lobby generateLobbyForTests() {
        Lobby lobby = new Lobby();
        lobby.setJoinCode("TestCode");
        lobby.setGameId(null);
        lobby.setCreatedAt(LocalDateTime.now());
        lobby.setGameDuration(10);
        lobby.setListType("all");
        return lobby;
    }

}
