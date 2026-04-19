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
    

    // not used anywhere yet!
    public int[] calculateBonusPoints(Game game) {
        final int rowBonus = 5;
        final int colBonus = 5;
        final int diagBonus = 5;

        int bonusPointsTeam1 = 0;
        int bonusPointsTeam2 = 0;
        int boardSize = game.getBoardSize();
        Tile[][] tileGrid = game.getTileGrid();

        // row bonus
        for (int col = 0; col < boardSize; col++) {
            int claimedTeam1 = 0;
            int claimedTeam2 = 0;

            for (int row = 0; row < boardSize; row++) {
                Tile tile = tileGrid[row][col];
                if (tile.getStatus() == TileStatus.CLAIMED_TEAM1) {
                    claimedTeam1++;
                }
                if (tile.getStatus() == TileStatus.CLAIMED_TEAM2) {
                    claimedTeam2++;
                }
            }

            if (claimedTeam1 == boardSize) {
                bonusPointsTeam1 = bonusPointsTeam1 + colBonus;
            }
            if (claimedTeam2 == boardSize) {
                bonusPointsTeam2 = bonusPointsTeam2 + colBonus;
            }
        }

        // col bonus
        for (int row = 0; row < boardSize; row++) {
            int claimedTeam1 = 0;
            int claimedTeam2 = 0;

            for (int col = 0; col < boardSize; col++) {
                Tile tile = tileGrid[row][col];
                if (tile.getStatus() == TileStatus.CLAIMED_TEAM1) {
                    claimedTeam1++;
                }
                if (tile.getStatus() == TileStatus.CLAIMED_TEAM2) {
                    claimedTeam2++;
                }
            }

            if (claimedTeam1 == boardSize) {
                bonusPointsTeam1 = bonusPointsTeam1 + rowBonus;
            }
            if (claimedTeam2 == boardSize) {
                bonusPointsTeam2 = bonusPointsTeam2 + rowBonus;
            }
        }

        // diag bonus
        // TLBR->top left to bottom right, TRBL->top right to bottom left
        int claimedTeam1TLBR = 0;
        int claimedTeam2TLBR = 0;
        int claimedTeam1TRBL = 0;
        int claimedTeam2TRBL = 0;
        for (int idx = 0; idx < boardSize; idx++) {
            // checks \
            Tile tileTLBR = tileGrid[idx][idx];
            if (tileTLBR.getStatus() == TileStatus.CLAIMED_TEAM1) {
                claimedTeam1TLBR++;
            }
            if (tileTLBR.getStatus() == TileStatus.CLAIMED_TEAM2) {
                claimedTeam2TLBR++;
            }
            // checks /
            Tile tileTRBL = tileGrid[idx][boardSize - idx - 1];
            if (tileTRBL.getStatus() == TileStatus.CLAIMED_TEAM1) {
                claimedTeam1TRBL++;
            }
            if (tileTRBL.getStatus() == TileStatus.CLAIMED_TEAM2) {
                claimedTeam2TRBL++;
            }
        }


        if (claimedTeam1TLBR == boardSize) {
            bonusPointsTeam1 = bonusPointsTeam1 + diagBonus;
        }
        if (claimedTeam2TLBR == boardSize) {
            bonusPointsTeam2 = bonusPointsTeam2 + diagBonus;
        }
        if (claimedTeam1TRBL == boardSize) {
            bonusPointsTeam1 = bonusPointsTeam1 + diagBonus;
        }
        if (claimedTeam2TRBL == boardSize) {
            bonusPointsTeam2 = bonusPointsTeam2 + diagBonus;
        }

        return new int[]{bonusPointsTeam1,bonusPointsTeam2};
    }
    
    



}