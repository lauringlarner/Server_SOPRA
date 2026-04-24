package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

import static org.junit.jupiter.api.Assertions.*;

public class ScoreServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private ScoreService scoreService;

    private Game game;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        game = new Game();
        game.setBoardSize(2);

        Tile[][] grid = new Tile[2][2];
        grid[0][0] = new Tile("cat", 1, TileStatus.UNCLAIMED);
        grid[0][1] = new Tile("dog", 1, TileStatus.UNCLAIMED);
        grid[1][0] = new Tile("rat", 1, TileStatus.UNCLAIMED);
        grid[1][1] = new Tile("ant", 1, TileStatus.UNCLAIMED);
        game.setTileGrid(grid);
    }

    @Test
    public void claimTile_team1_success() {
        scoreService.claimTile(game, 0, "1");

        assertEquals(TileStatus.CLAIMED_TEAM1, game.getTileGrid()[0][0].getStatus());
        assertEquals(1, game.getScore_1());
        assertEquals(0, game.getScore_2());
    }

    @Test
    public void claimTile_team2_success() {
        scoreService.claimTile(game, 0, "2");

        assertEquals(TileStatus.CLAIMED_TEAM2, game.getTileGrid()[0][0].getStatus());
        assertEquals(0, game.getScore_1());
        assertEquals(1, game.getScore_2());
    }

    @Test
    public void claimTile_alreadyClaimed_throwsException() {
        Tile[][] grid = game.getTileGrid();
        grid[0][0].setStatus(TileStatus.CLAIMED_TEAM1);
        game.setTileGrid(grid);

        assertThrows(Exception.class, () -> scoreService.claimTile(game, 0, "2"));
    }

    @Test
    public void claimTile_nullTeam_throwsException() {
        assertThrows(Exception.class, () -> scoreService.claimTile(game, 0, null));
    }

    @Test
    public void claimTile_invalidTeam_throwsException() {
        assertThrows(Exception.class, () -> scoreService.claimTile(game, 0, "3"));
    }

    @Test
    public void calculatePointsPerTile_mixedTiles_returnsCorrectCount() {
        Tile[][] grid = game.getTileGrid();
        grid[0][0].setStatus(TileStatus.CLAIMED_TEAM1);
        grid[0][1].setStatus(TileStatus.CLAIMED_TEAM2);
        grid[1][0].setStatus(TileStatus.CLAIMED_TEAM2);
        game.setTileGrid(grid);

        int[] points = scoreService.calculatePointsPerTile(game);

        assertEquals(1, points[0]);
        assertEquals(2, points[1]);
    }

    @Test
    public void ColBonusPoints() {
        Tile[][] grid = game.getTileGrid();
        grid[0][0].setStatus(TileStatus.CLAIMED_TEAM1);
        grid[0][1].setStatus(TileStatus.CLAIMED_TEAM1);
        game.setTileGrid(grid);

        int[] bonus = scoreService.calculateColBonusPoints(game, 3);

        assertEquals(3, bonus[0]);
        assertEquals(0, bonus[1]);
    }

    @Test
    public void RowBonusPoints() {
        Tile[][] grid = game.getTileGrid();
        grid[0][0].setStatus(TileStatus.CLAIMED_TEAM1);
        grid[1][0].setStatus(TileStatus.CLAIMED_TEAM1);
        game.setTileGrid(grid);

        int[] bonus = scoreService.calculateRowBonusPoints(game, 3);

        assertEquals(3, bonus[0]);
        assertEquals(0, bonus[1]);
    }

    @Test
    public void DiagBonusPoints_team1MainDiag_returnsBonus() {
        Tile[][] grid = game.getTileGrid();
        grid[0][0].setStatus(TileStatus.CLAIMED_TEAM1);
        grid[1][1].setStatus(TileStatus.CLAIMED_TEAM1);
        game.setTileGrid(grid);

        int[] bonus = scoreService.calculateDiagBonusPoints(game, 4);

        assertEquals(4, bonus[0]);
        assertEquals(0, bonus[1]);
    }

    @Test
    public void totalPoints_team1_diag() {
        Tile[][] grid = game.getTileGrid();
        grid[0][0].setStatus(TileStatus.CLAIMED_TEAM1);
        grid[1][1].setStatus(TileStatus.CLAIMED_TEAM1);
        game.setTileGrid(grid);

        int[] points = scoreService.calculateTotalPointsDefault(game);

        // 2 tile points + 4 diag bonus = 6
        assertEquals(6, points[0]);
        assertEquals(0, points[1]);
    }
}
