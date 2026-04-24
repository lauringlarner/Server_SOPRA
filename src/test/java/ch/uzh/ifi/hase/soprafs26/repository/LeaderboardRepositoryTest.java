package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class LeaderboardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    // gameId matches → returns leaderboard
    @Test
    public void findByGameId_success() {
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setGameId(UUID.randomUUID());
        leaderboard.setTeam1Score(5);
        leaderboard.setTeam2Score(3);
        entityManager.persistAndFlush(leaderboard);

        Leaderboard result = leaderboardRepository.findByGameId(leaderboard.getGameId());

        assertNotNull(result);
        assertEquals(leaderboard.getGameId(), result.getGameId());
        assertEquals(5, result.getTeam1Score());
        assertEquals(3, result.getTeam2Score());
    }

    // gameId not found → returns null
    @Test
    public void findByGameId_notFound_returnsNull() {
        Leaderboard result = leaderboardRepository.findByGameId(UUID.randomUUID());

        assertNull(result);
    }
}
