package ch.uzh.ifi.hase.soprafs26.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TileStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Tile;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;

@Service
@Transactional
public class ScoreService {

    private final GameRepository gameRepository;

    public ScoreService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Works only for NxN boards 
     * When checkWordList is refactored (I think it is planned to) to return row/col directly, this method should be updated accordingly.
     *
     * @param game      the game being played
     * @param tileIndex flat index from checkWordList (0 to boardSize*boardSize-1)
     * @param team      "1" for team 1, "2" for team 2
     */
    public void claimTile(Game game, int tileIndex, String team) {
        int boardSize = game.getBoardSize();
        int row = tileIndex / boardSize;
        int col = tileIndex % boardSize;

        Tile[][] tileGrid = game.getTileGrid();
        Tile tile = tileGrid[row][col];

        if (tile.getStatus() != TileStatus.UNCLAIMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tile is already claimed!");
        }

        if ("1".equals(team)) {
            tile.setStatus(TileStatus.CLAIMED_TEAM1);
            game.setScore_1(game.getScore_1() + tile.getValue());
        } else if ("2".equals(team)) {
            tile.setStatus(TileStatus.CLAIMED_TEAM2);
            game.setScore_2(game.getScore_2() + tile.getValue());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not in Game!");
        }

        game.setTileGrid(tileGrid);
    }
}
