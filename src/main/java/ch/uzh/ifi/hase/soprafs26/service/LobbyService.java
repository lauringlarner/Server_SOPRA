package ch.uzh.ifi.hase.soprafs26.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;


@Service
@Transactional
public class LobbyService {
	private final LobbyPlayerRepository lobbyPlayerRepository;

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);

	private final LobbyRepository lobbyRepository;

	public LobbyService(@Qualifier("userRepository") LobbyRepository lobbyRepository, LobbyPlayerRepository lobbyPlayerRepository) {
		this.lobbyRepository = lobbyRepository;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
	}


    public LobbyPlayer createLobbyPlayer(User newLobbyUser, Boolean isHost) {
        
        if (newLobbyUser == null || newLobbyUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User doen't exist");
        }


        LobbyPlayer newLobbyPlayer = new LobbyPlayer();

        newLobbyPlayer.setIsHost(isHost);
        newLobbyPlayer.setIsReady(false);
        newLobbyPlayer.setJoinedAt(LocalDateTime.now());
        newLobbyPlayer.setUserId(newLobbyUser.getId());

        newLobbyPlayer = lobbyPlayerRepository.save(newLobbyPlayer);
        lobbyPlayerRepository.flush();

        log.debug("Created LobbyPlayer: {}", newLobbyPlayer);
		return newLobbyPlayer;
    }


    public Lobby createLobby(LobbyPlayer lobbyPlayer) {

        if (lobbyPlayer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host doen't exist");
        }
        if(lobbyPlayer.getLobby() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Host is already in another Lobby");
        }

        Lobby newLobby = new Lobby();

        newLobby.setCreatedAt(LocalDateTime.now());
        newLobby.setJoinCode(generateJoinCode());
        newLobby.setStatus(LobbyStatus.OPEN);
        newLobby.addPlayer(lobbyPlayer);

        // Default Game Settings
        newLobby.setBingoBoardSize(5);
        newLobby.setGameDuration(10);
    

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        lobbyPlayer.setLobby(newLobby);

        lobbyPlayerRepository.save(lobbyPlayer);
        lobbyPlayerRepository.flush();

        log.debug("Created Lobby: {}", newLobby);
		return newLobby;
    }


    public String generateJoinCode() {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final int CODE_LENGTH = 6;

        final SecureRandom random = new SecureRandom();

        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }
        return code.toString();
    }


    public Lobby getLobbyByLobbyId(UUID lobbyId) {
        return lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }


}
