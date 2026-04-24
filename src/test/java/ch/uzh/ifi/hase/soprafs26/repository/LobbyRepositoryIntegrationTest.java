package ch.uzh.ifi.hase.soprafs26.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;

@DataJpaTest
public class LobbyRepositoryIntegrationTest {
    @Autowired
	private TestEntityManager entityManager;
    
    @Autowired
	private LobbyRepository lobbyRepository;


    @Test
    public void findById_returnsLobby() {
        // given
        Lobby lobby = generateLobbyForTests();

        entityManager.persist(lobby);
        entityManager.flush();
        entityManager.clear();

        // when
        Lobby foundLobby = lobbyRepository.findById(lobby.getId()).orElse(null);

        // then
        assertNotNull(foundLobby);
        assertEquals(lobby.getId(), foundLobby.getId());
        assertEquals(lobby.getJoinCode(), foundLobby.getJoinCode());
        assertEquals(lobby.getGameId(), foundLobby.getGameId());
        assertEquals(lobby.getCreatedAt().withNano(0), foundLobby.getCreatedAt().withNano(0));
        assertEquals(lobby.getGameDuration(), foundLobby.getGameDuration());
    }

    @Test
    public void findById_notFound() {
        // given
        UUID lobbyIdWithoutLobby = UUID.randomUUID();

        // when
        Lobby foundLobby = lobbyRepository.findById(lobbyIdWithoutLobby).orElse(null);

        // then
        assertNull(foundLobby);
    }
    
    @Test
    public void findByJoinCode_returnsLobby() {
        // given
        Lobby lobby = generateLobbyForTests();

        entityManager.persist(lobby);
        entityManager.flush();
        entityManager.clear();

        // when
        Lobby foundLobby = lobbyRepository.findByJoinCode(lobby.getJoinCode());

        // then
        assertNotNull(foundLobby);
        assertEquals(lobby.getId(), foundLobby.getId());
        assertEquals(lobby.getJoinCode(), foundLobby.getJoinCode());
        assertEquals(lobby.getGameId(), foundLobby.getGameId());
        assertEquals(lobby.getCreatedAt().withNano(0), foundLobby.getCreatedAt().withNano(0));
        assertEquals(lobby.getGameDuration(), foundLobby.getGameDuration());
    }

    @Test
    public void findByJoinCode_notFound() {
        // given
        String JoinCodeWithoutLobby = "JoinCode";

        // when
        Lobby foundLobby = lobbyRepository.findByJoinCode(JoinCodeWithoutLobby);

        // then
        assertNull(foundLobby);
    }
    
    // helpers to generate lobbyPlayer 
    private Lobby generateLobbyForTests() {
        Lobby lobby = new Lobby();
        lobby.setJoinCode("TestCode");
        lobby.setGameId(null);
        lobby.setCreatedAt(LocalDateTime.now());
        lobby.setGameDuration(10);
        return lobby;
    }
}
