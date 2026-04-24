package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Leaderboard;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.repository.LeaderboardRepository;

import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LeaderboardPostDTO;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LeaderboardServiceTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private Game game;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        game = new Game();
        game.setId(UUID.randomUUID());
        game.setBoardSize(2);
        game.setScore_1(3);
        game.setScore_2(1);

        Tile[][] grid = new Tile[2][2];
        grid[0][0] = new Tile("cat", 1, TileStatus.CLAIMED_TEAM1);
        grid[0][1] = new Tile("dog", 1, TileStatus.UNCLAIMED);
        grid[1][0] = new Tile("rat", 1, TileStatus.UNCLAIMED);
        grid[1][1] = new Tile("ant", 1, TileStatus.UNCLAIMED);
        game.setTileGrid(grid);

        Mockito.when(leaderboardRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    // no existing leaderboard → creates new
    @Test
    public void Init() {
        Mockito.when(leaderboardRepository.findByGameId(game.getId())).thenReturn(null);

        Leaderboard result = leaderboardService.initOrUpdate(game);

        assertEquals(game.getId(), result.getGameId());
        assertEquals(0, result.getTeam1Score());
        assertEquals(0, result.getTeam2Score());
    }

    // existing leaderboard → updates scores
    @Test
    public void UpdateScores() {
        Leaderboard existing = new Leaderboard();
        existing.setGameId(game.getId());
        existing.setTeam1Score(0);
        existing.setTeam2Score(0);

        Mockito.when(leaderboardRepository.findByGameId(game.getId())).thenReturn(existing);

        Leaderboard result = leaderboardService.initOrUpdate(game);

        assertEquals(3, result.getTeam1Score());
        assertEquals(1, result.getTeam2Score());
    }

    // existing leaderboard → updates scores
    @Test
    public void updateLeaderboard_success() {
        Leaderboard existing = new Leaderboard();
        existing.setGameId(game.getId());
        existing.setTeam1Score(0);
        existing.setTeam2Score(0);

        Mockito.when(leaderboardRepository.findByGameId(game.getId())).thenReturn(existing);

        Leaderboard result = leaderboardService.updateLeaderboard(game);

        assertEquals(3, result.getTeam1Score());
        assertEquals(1, result.getTeam2Score());
    }

    // leaderboard not found → 404
    @Test
    public void updateLeaderboard_notFound_throwsException() {
        Mockito.when(leaderboardRepository.findByGameId(game.getId())).thenReturn(null);

        assertThrows(Exception.class, () -> leaderboardService.updateLeaderboard(game));
    }

    // existing leaderboard → returns it
    @Test
    public void getLeaderboard_success() {
        Leaderboard existing = new Leaderboard();
        existing.setGameId(game.getId());

        Mockito.when(leaderboardRepository.findByGameId(game.getId())).thenReturn(existing);

        Leaderboard result = leaderboardService.getLeaderboard(game.getId());

        assertEquals(game.getId(), result.getGameId());
    }

    // leaderboard not found → 404
    @Test
    public void getLeaderboard_notFound_throwsException() {
        Mockito.when(leaderboardRepository.findByGameId(game.getId())).thenReturn(null);

        assertThrows(Exception.class, () -> leaderboardService.getLeaderboard(game.getId()));
    }

    // LeaderboardGetDTO: scores and gameId correct
    @Test
    public void leaderboardGetDTO_settersAndGetters_work() {
        UUID gameId = UUID.randomUUID();
        LeaderboardGetDTO dto = new LeaderboardGetDTO(gameId);
        dto.setTeam1Score(10);
        dto.setTeam2Score(7);

        assertEquals(gameId, dto.getGameId());
        assertEquals(10, dto.getTeam1Score());
        assertEquals(7, dto.getTeam2Score());
    }

    // LeaderboardGetDTO: tileGrid sets correctly
    @Test
    public void leaderboardGetDTO_tileGrid_setsCorrectly() {
        UUID gameId = UUID.randomUUID();
        LeaderboardGetDTO dto = new LeaderboardGetDTO(gameId);
        Tile[][] grid = new Tile[][] { { new Tile("cat", 3, TileStatus.CLAIMED_TEAM1) } };
        dto.setTileGrid(grid);

        assertEquals(grid, dto.getTileGrid());
    }

    // LeaderboardPostDTO: gameId getter/setter
    @Test
    public void leaderboardPostDTO_setterAndGetter_work() {
        UUID gameId = UUID.randomUUID();
        LeaderboardPostDTO dto = new LeaderboardPostDTO();
        dto.setGameId(gameId);

        assertEquals(gameId, dto.getGameId());
    }
}
