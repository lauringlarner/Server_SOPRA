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

        if (null == team) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not in Game!");
        } else switch (team) {
            case "1" -> {
                tile.setStatus(TileStatus.CLAIMED_TEAM1);
                
            }
            case "2" -> {
                tile.setStatus(TileStatus.CLAIMED_TEAM2);
                
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team not in Game!");
        }

        game.setTileGrid(tileGrid);

        int[] pointsPerTeam = calculateTotalPointsDefault(game);
        game.setScore_1(pointsPerTeam[0]);
        game.setScore_2(pointsPerTeam[1]);
    }
    
    public int[] calculatePointsPerTile(Game game) {
        int PointsTeam1 = 0;
        int PointsTeam2 = 0;
        int boardSize = game.getBoardSize();
        Tile[][] tileGrid = game.getTileGrid();

        for (int col = 0; col < boardSize; col++) {
            for (int row = 0; row < boardSize; row++) {
                Tile tile = tileGrid[row][col];
                if (tile.getStatus() == TileStatus.CLAIMED_TEAM1) {
                    PointsTeam1++;
                }
                if (tile.getStatus() == TileStatus.CLAIMED_TEAM2) {
                    PointsTeam2++;
                }
            }
        }

        return new int[]{PointsTeam1,PointsTeam2};
    }

    public int[] calculateRowBonusPoints(Game game, int rowBonus) {
        int bonusPointsTeam1 = 0;
        int bonusPointsTeam2 = 0;
        int boardSize = game.getBoardSize();
        Tile[][] tileGrid = game.getTileGrid();

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
                bonusPointsTeam1 = bonusPointsTeam1 + rowBonus;
            }
            if (claimedTeam2 == boardSize) {
                bonusPointsTeam2 = bonusPointsTeam2 + rowBonus;
            }
        }

        return new int[]{bonusPointsTeam1,bonusPointsTeam2};
    }

    public int[] calculateColBonusPoints(Game game, int colBonus) {
        int bonusPointsTeam1 = 0;
        int bonusPointsTeam2 = 0;
        int boardSize = game.getBoardSize();
        Tile[][] tileGrid = game.getTileGrid();

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
                bonusPointsTeam1 = bonusPointsTeam1 + colBonus;
            }
            if (claimedTeam2 == boardSize) {
                bonusPointsTeam2 = bonusPointsTeam2 + colBonus;
            }
        }

        return new int[]{bonusPointsTeam1,bonusPointsTeam2};
    }

    public int[] calculateDiagBonusPoints(Game game, int diagBonus) {
        int bonusPointsTeam1 = 0;
        int bonusPointsTeam2 = 0;
        int boardSize = game.getBoardSize();
        Tile[][] tileGrid = game.getTileGrid();
        
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


    public int[] calculateTotalPointsDefault(Game game) {
        final int rowBonus = 3;
        final int colBonus = 3;
        final int diagBonus = 4;

        int[] tilePointsPerTeam = calculatePointsPerTile(game);
        int[] rowPointsPerTeam = calculateRowBonusPoints(game, rowBonus);
        int[] colPointsPerTeam = calculateColBonusPoints(game, colBonus);
        int[] diagPointsPerTeam = calculateDiagBonusPoints(game, diagBonus);

        int pointsTeam1 = tilePointsPerTeam[0] + rowPointsPerTeam[0] + colPointsPerTeam[0] + diagPointsPerTeam[0];
        int pointsTeam2 = tilePointsPerTeam[1] + rowPointsPerTeam[1] + colPointsPerTeam[1] + diagPointsPerTeam[1];

        return new int[]{pointsTeam1,pointsTeam2};
    }
    
    



}