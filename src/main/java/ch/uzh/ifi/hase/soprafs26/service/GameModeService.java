package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GameModeDTO;

@Service
public class GameModeService {
    
    private final List<GameModeDTO> gameModes = List.of(
        new GameModeDTO(
            "Lockout_bonus_points",
            "Lockout Bingo with bonus points",
            List.of(
                "Locate an item listed on the bingo board in the real world.",
                "Tap the tile to open the camera and snap a photo of that item.",
                "Once submitted, our AI will validate the image to ensure it matches the item on the tile.",
                "Earn points for every captured tile, plus bonus points for completing rows, columns, or diagonals."
            )
        ),

        new GameModeDTO(
            "Lockout_no_bonus_points",
            "Lockout Bingo with bonus points",
            List.of(
                "Locate an item listed on the bingo board in the real world.",
                "Tap the tile to open the camera and snap a photo of that item.",
                "Once submitted, our AI will validate the image to ensure it matches the item on the tile.",
                "Earn points for every captured tile."
            )
        )
    );

    public List<GameModeDTO> getAllGameModes() {
        return gameModes;
    }

    public GameModeDTO getGameModeById(String id) {
        return gameModes.stream()
                .filter(mode -> mode.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GameMode rules not found!" + id));
    }
}
