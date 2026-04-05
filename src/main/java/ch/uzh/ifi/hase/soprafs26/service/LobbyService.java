package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.controller.LobbyController;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;


@Service
@Transactional
public class LobbyService {

    private final LobbyPlayerRepository lobbyPlayerRepository;

	private final LobbyRepository lobbyRepository;
    
    private final Map<UUID, List<SseEmitter>> lobbyEmitters = new ConcurrentHashMap<>();

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);

	public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository, @Qualifier("lobbyPlayerRepository") LobbyPlayerRepository lobbyPlayerRepository) {
		this.lobbyRepository = lobbyRepository;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
	}


    public LobbyPlayer createLobbyPlayer(User newLobbyUser, Boolean isHost) {
        
        if (newLobbyUser == null || newLobbyUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User doen't exist");
        }

        if (checkIfLobbyPlayerExistsByUser(newLobbyUser)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"The User is already a Player");
        }

        LobbyPlayer newLobbyPlayer = new LobbyPlayer();

        newLobbyPlayer.setIsHost(isHost);
        newLobbyPlayer.setIsReady(false);
        newLobbyPlayer.setJoinedAt(LocalDateTime.now());
        newLobbyPlayer.setUser(newLobbyUser);
        newLobbyPlayer.setTeamType(TeamType.Undecided);

        newLobbyPlayer = lobbyPlayerRepository.save(newLobbyPlayer);
        lobbyPlayerRepository.flush();

        log.debug("Created LobbyPlayer: {}", newLobbyPlayer);
		return newLobbyPlayer;
    }


    public Boolean checkIfLobbyPlayerExistsByUser(User user) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);
        return lobbyPlayer != null;
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
        newLobby.setBingoBoardSize(4);
        newLobby.setGameDuration(10);
    

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        lobbyPlayer.setLobby(newLobby);

        lobbyPlayerRepository.save(lobbyPlayer);
        lobbyPlayerRepository.flush();

        log.debug("Created Lobby: {}", newLobby);
		return newLobby;
    }


    public Lobby joinLobby(LobbyPlayer lobbyPlayer, Lobby lobbyToJoin) {

        if (lobbyPlayer == null || lobbyToJoin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player or Lobby doesn't exist!");
        }

        if (lobbyToJoin.getStatus() == LobbyStatus.CLOSED) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is already closed!");
        }


        lobbyToJoin.addPlayer(lobbyPlayer);

        lobbyToJoin = lobbyRepository.save(lobbyToJoin);
        lobbyRepository.flush();

        lobbyPlayer.setLobby(lobbyToJoin);

        lobbyPlayerRepository.save(lobbyPlayer);
        lobbyPlayerRepository.flush();

        // SSE pushes update
        pushLobbyUpdate(lobbyToJoin);

        log.debug("Lobby added new Player: {}", lobbyPlayer);

        return lobbyToJoin;
    }

    public Lobby getLobbyByJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Join Code!");
		}
	
		Lobby lobby = lobbyRepository.findByJoinCode(joinCode);
		if (lobby == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Join Code!");
		}

		return lobby;
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

    public LobbyPlayer getLobbyPlayerByUser(User user) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);
        return lobbyPlayer;
    }

    public LobbyPlayer getLobbyPlayerById(UUID playerId) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findById(playerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        return lobbyPlayer;
    }

    public void updateTeamType(LobbyPlayer lobbyPlayer, TeamType teamType) {
        lobbyPlayer.setTeamType(teamType);

        // SSE push update
        pushLobbyUpdate(lobbyPlayer.getLobby());
    }


    public void updateReadyStatus(LobbyPlayer lobbyPlayer, Boolean readyStatus) {
        lobbyPlayer.setIsReady(readyStatus);

        // SSE push update
        pushLobbyUpdate(lobbyPlayer.getLobby());
    }

    public void updateLobbySettings(Lobby lobby, Integer gameDuration) {
        lobby.setGameDuration(gameDuration);

        // SSE push update
        pushLobbyUpdate(lobby);
    }

    public boolean isPlayerHost(LobbyPlayer lobbyPlayer) {
        Boolean isHost = lobbyPlayer.getIsHost();
        if (isHost != null && isHost) {
            return true;
        }
        return false;
    }

    public boolean isPlayerReady(LobbyPlayer lobbyPlayer) {
        Boolean isReady = lobbyPlayer.getIsReady();
        if (isReady != null && isReady) {
            return true;
        }
        return false;
    }

    public Boolean areAllLobbyPlayersReady(Lobby lobby) {
        List<LobbyPlayer> LobbyPlayers = lobby.getLobbyPlayers();

        for (LobbyPlayer lobbyPlayer : LobbyPlayers) {
            if (!isPlayerReady(lobbyPlayer)) {
                return false;
            }
        }
        return true;
    }

    public void setAllLobbyPlayersReadyStatusToFalse(Lobby lobby) {
        List<LobbyPlayer> LobbyPlayers = lobby.getLobbyPlayers();

            for (LobbyPlayer lobbyPlayer : LobbyPlayers) {
                lobbyPlayer.setIsReady(false);
        }
    }

    public void deleteLobby(Lobby lobby) {
        lobbyRepository.delete(lobby);
    }

    public void deleteLobbyPlayer(LobbyPlayer lobbyPlayer) {
        Lobby lobby = lobbyPlayer.getLobby();

        lobby.removePlayer(lobbyPlayer);

        lobbyPlayerRepository.delete(lobbyPlayer);

        // SSE push update
        pushLobbyUpdate(lobbyPlayer.getLobby());
    }

    

    public void registerLobbyEmitter(UUID lobbyId, SseEmitter emitter) {
        lobbyEmitters.computeIfAbsent(lobbyId, k -> new ArrayList<>()).add(emitter);
    }

    public void removeLobbyEmitter(UUID lobbyId, SseEmitter emitter) {
        List<SseEmitter> emitters = lobbyEmitters.get(lobbyId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                lobbyEmitters.remove(lobbyId);
            }
        }
    }

    public void pushLobbyUpdate(Lobby lobby) {
        List<SseEmitter> emitters = lobbyEmitters.get(lobby.getId());
        if (emitters != null) {
            LobbyDTO lobbyDTO = DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("lobbyUpdate").data(lobbyDTO));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }
        }
    }


}
