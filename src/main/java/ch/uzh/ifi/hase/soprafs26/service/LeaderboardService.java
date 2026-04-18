package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.repository.LeaderboardRepository;

import java.util.UUID;

@Service
@Transactional
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(@Qualifier("leaderboardRepository") LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    /**
     * Initializes a new leaderboard or updates the existing one in a single query.
     * Called by the client on game start and after every score event.
     */
    public Leaderboard initOrUpdate(Game game) {
        Leaderboard leaderboard = leaderboardRepository.findByGameId(game.getId());

        if (leaderboard == null) {
            leaderboard = new Leaderboard();
            leaderboard.setGameId(game.getId());
            leaderboard.setTeam1Score(0);
            leaderboard.setTeam2Score(0);
        } else {
            leaderboard.setTeam1Score(game.getScore_1());
            leaderboard.setTeam2Score(game.getScore_2());
        }

        leaderboard.setTileGrid(game.getTileGrid());
        return leaderboardRepository.save(leaderboard);
    }

    /**
     * Updates the leaderboard after a score event.
     * Called by the server internally after a successful imageSubmission.
     */
    public Leaderboard updateLeaderboard(Game game) {
        Leaderboard leaderboard = leaderboardRepository.findByGameId(game.getId());
        if (leaderboard == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard not found for this game!");
        }

        leaderboard.setTeam1Score(game.getScore_1());
        leaderboard.setTeam2Score(game.getScore_2());
        leaderboard.setTileGrid(game.getTileGrid());

        return leaderboardRepository.save(leaderboard);
    }

    public Leaderboard getLeaderboard(UUID gameId) {
        Leaderboard leaderboard = leaderboardRepository.findByGameId(gameId);
        if (leaderboard == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Leaderboard not found for this game!");
        }
        return leaderboard;
    }
}
