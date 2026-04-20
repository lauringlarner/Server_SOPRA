package ch.uzh.ifi.hase.soprafs26.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
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

    private final SseService sseService;

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);

	public LobbyService(@Qualifier("lobbyRepository") LobbyRepository lobbyRepository,
                        @Qualifier("lobbyPlayerRepository") LobbyPlayerRepository lobbyPlayerRepository,
                        SseService sseService) {
		this.lobbyRepository = lobbyRepository;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
        this.sseService = sseService;
	}

    //////////////
    // Creation //
    //////////////

    public LobbyPlayer createLobbyPlayer(User user, Boolean isHost) {
        
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User doen't exist");
        }

        validateUserHasNoPlayer(user); // CONFLICT on failure

        LobbyPlayer newLobbyPlayer = new LobbyPlayer();

        newLobbyPlayer.setIsHost(isHost);
        newLobbyPlayer.setIsReady(false);
        newLobbyPlayer.setJoinedAt(LocalDateTime.now());
        newLobbyPlayer.setUser(user);
        newLobbyPlayer.setTeamType(TeamType.Undecided);

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
        newLobby.addPlayer(lobbyPlayer);

        // Default Game Settings
        newLobby.setGameDuration(10);
    

        newLobby = lobbyRepository.save(newLobby);
        lobbyRepository.flush();

        lobbyPlayer.setLobby(newLobby);

        lobbyPlayerRepository.save(lobbyPlayer);
        lobbyPlayerRepository.flush();

        log.debug("Created Lobby: {}", newLobby);
		return newLobby;
    }

    ///////////////
    // Retrieval //
    ///////////////
    
    public Lobby getLobbyByJoinCode(String joinCode) {
        validateJoinCode(joinCode); // BAD_REQUEST on failure
        
		Lobby lobby = lobbyRepository.findByJoinCode(joinCode);
		if (lobby == null) {
            log.debug("Lobby not found by JoinCode");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found!");
		}
        
		return lobby;
    }
    
    public Lobby getLobbyByLobbyId(UUID lobbyId) {
        return lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }
    
    
    public LobbyPlayer getLobbyPlayerByUser(User user) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);
		if (lobbyPlayer == null) {
            log.debug("Player not found by user!");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found!");
		}
		return lobbyPlayer;
	}
    
    public LobbyPlayer getLobbyPlayerById(UUID playerId) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findById(playerId).orElseThrow(() -> {
            log.debug("Player not found by id!");
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found!");
        });
        return lobbyPlayer;
    }
    
    /////////////
    // Actions //
    /////////////
    
    public Lobby joinLobby(LobbyPlayer lobbyPlayer, Lobby lobbyToJoin) {

        if (lobbyPlayer == null || lobbyToJoin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player or Lobby doesn't exist!");
        }

        if (lobbyToJoin.getGameId() != null) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is not OPEN!");
        }


        lobbyToJoin.addPlayer(lobbyPlayer);

        lobbyToJoin = lobbyRepository.save(lobbyToJoin);
        lobbyRepository.flush();

        lobbyPlayer.setLobby(lobbyToJoin);

        lobbyPlayerRepository.save(lobbyPlayer);
        lobbyPlayerRepository.flush();

        // SSE pushes update
        pushLobbyUpdate(lobbyToJoin);

        log.debug("Lobby {} added new Player: {}", lobbyToJoin,lobbyPlayer);

        return lobbyToJoin;
    }

    public void resetLobbyAfterGame(UUID lobbyId) {
        Lobby lobby = getLobbyByLobbyId(lobbyId);
        lobby.setGameId(null);
        updateAllLobbyPlayersReadyStatusToFalse(lobby);
    }

    /////////////
    // Updates //
    /////////////
    
    public void updateTeamType(LobbyPlayer lobbyPlayer, TeamType teamType) {
        validateTeamType(teamType); // BAD_REQUEST on failure
        lobbyPlayer.setTeamType(teamType);
        
        // SSE push update
        pushLobbyUpdate(lobbyPlayer.getLobby());
        
        log.debug("Player {} successfully updated their team type",lobbyPlayer);
    }
    
    public void updateReadyStatus(LobbyPlayer lobbyPlayer, Boolean readyStatus) {
        validateReadyStatus(readyStatus); // BAD_REQUEST on failure 
        lobbyPlayer.setIsReady(readyStatus);
        
        // SSE push update
        pushLobbyUpdate(lobbyPlayer.getLobby());
        
        log.debug("Player {} successfully updated their ready status",lobbyPlayer);
    }
    
    public void updateAllLobbyPlayersReadyStatusToFalse(Lobby lobby) {
        List<LobbyPlayer> LobbyPlayers = lobby.getLobbyPlayers();
        
        for (LobbyPlayer lobbyPlayer : LobbyPlayers) {
            lobbyPlayer.setIsReady(false);
        }
    }
    
    
    public void updateLobbySettings(Lobby lobby, Integer gameDuration) {
        validateGameDuration(gameDuration); // BAD_REQUEST on failure
        lobby.setGameDuration(gameDuration);
        
        // SSE push update
        pushLobbyUpdate(lobby);
        
        log.debug("Lobby {} successfully changed their settings",lobby);
    }
   
    public void setLobbyGameId(Lobby lobby, UUID gameId) {
        lobby.setGameId(gameId);

        // Notify connected lobby clients immediately so they can transition into the game.
        pushLobbyUpdate(lobby);
        
        log.debug("Lobby {} is now running the game {}", lobby, gameId);
    }
       
    //////////////
    // Deletion //
    //////////////
    
    public void deleteLobby(Lobby lobby) {
        lobbyRepository.delete(lobby);

        log.debug("Lobby successfully deleted");
    }
    
    public void deleteLobbyPlayer(LobbyPlayer lobbyPlayer) {
        Lobby lobby = lobbyPlayer.getLobby();
        
        lobby.removePlayer(lobbyPlayer);
        
        lobbyPlayerRepository.delete(lobbyPlayer);
        
        // SSE push update
        pushLobbyUpdate(lobbyPlayer.getLobby());
        
        log.debug("Player successfully deleted");
    }

    ////////////////
    // Validation //
    ////////////////
    
    public void validateLobbyPlayerInLobby(LobbyPlayer lobbyPlayer, Lobby lobby) {
        if (!lobbyPlayer.getLobby().equals(lobby)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player doesn't belong to Lobby!");
        }
    }
    
    public void validateLobbyPlayerIsNotHost(LobbyPlayer lobbyPlayer) {
        if (isPlayerHost(lobbyPlayer)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Wrong endpoint was used. Hosts need to use  DELETE /lobbies/{lobbyId}  !");
            }
        }
        
    public void validateLobbyPlayerIsHost(LobbyPlayer lobbyPlayer) {
        if (!isPlayerHost(lobbyPlayer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not Host!");
        }
    }
        
    public void validateAllPlayersReady(Lobby lobby) {
        List<LobbyPlayer> lobbyPlayers = lobby.getLobbyPlayers();

        for (LobbyPlayer lobbyPlayer : lobbyPlayers) {
            if (!isPlayerReady(lobbyPlayer)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not all Players are ready!");
            }
        }
    }

    public void validateAllPlayersAreInValidTeams(Lobby lobby) {
        List<LobbyPlayer> lobbyPlayers = lobby.getLobbyPlayers();

        for (LobbyPlayer lobbyPlayer : lobbyPlayers) {
            if (!isPlayerInValidTeam(lobbyPlayer)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not all Players have joined a team yet!");
            }
        }
    }
    

    public void validateLobbyIsOpen(Lobby lobby) {
        if (!isLobbyOpen(lobby)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A game is already RUNNING!");
        }
    }

    public void validateLobbyHasPlayersInBothTeams(Lobby lobby) {
        List<LobbyPlayer> lobbyPlayers = lobby.getLobbyPlayers();

        boolean hasTeam1 = false;
        boolean hasTeam2 = false;

        for (LobbyPlayer lobbyPlayer : lobbyPlayers) {
            TeamType team = lobbyPlayer.getTeamType();

            if (team == TeamType.Team1) {
                hasTeam1 = true;
            } else if (team == TeamType.Team2) {
                hasTeam2 = true;
            }

            if (hasTeam1 && hasTeam2) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, "A team has zero players assigned!");

    }
    
    
    public void validateUserMatchesLobbyPlayerId(UUID lobbyPlayerId, User user) {
        LobbyPlayer lobbyPlayer = getLobbyPlayerByUser(user);
        
        if (!lobbyPlayer.getId().equals(lobbyPlayerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own Player!");
        }
    }

    private void validateUserHasNoPlayer(User user) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);

        if (lobbyPlayer != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a player!");
        }
    }

    
    private void validateGameDuration(Integer gameDuration) {
        Integer minDuration = 5;
        Integer maxDuration = 20;
        
        if (gameDuration < minDuration || gameDuration > maxDuration) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                String.format("Game duration must be between %d - %d minutes", minDuration, maxDuration));
        }
    }

    private void validateReadyStatus(Boolean readyStatus) {
        if (readyStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ready status submitted!");
        }
    }

    private void validateTeamType(TeamType teamType) {
        if (teamType != TeamType.Team1 && teamType != TeamType.Team2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Team submitted!");
        }
    }

    private void validateJoinCode(String joinCode) {
        if (joinCode == null || joinCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Join code cannot be empty!");
        }
        if (!joinCode.matches("^[A-Z0-9]{6}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Join code must be 6 characters long and contain only uppercase letters and numbers!");
        }
    }


    public void validateLobbyPlayerIsInGame(LobbyPlayer lobbyPlayer, Game game) {
        if (!lobbyPlayer.getLobby().getId().equals(game.getLobbyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Player is not part of this game!");
        }
    }
    
    ///////////////
    // Utilities //    
    ///////////////
    
    public boolean isPlayerHost(LobbyPlayer lobbyPlayer) {
        return lobbyPlayer.getIsHost();
    }

    public boolean isPlayerReady(LobbyPlayer lobbyPlayer) {
        return lobbyPlayer.getIsReady();
    }

    public boolean isPlayerInValidTeam(LobbyPlayer lobbyPlayer) {
        return lobbyPlayer.getTeamType() != TeamType.Undecided;
    }

    public boolean isLobbyOpen(Lobby lobby) {
        return lobby.getGameId() == null;
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
                  
    ///////////////////
    // SSE functions //
    ///////////////////
    
    public SseEmitter createAndRegisterLobbyStream(Lobby lobby) {
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

		// Send initial lobby state
        try {
            emitter.send(SseEmitter.event()
                .name("lobbyUpdate")
                .data(DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby)));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

		// register emitter
		sseService.register(lobby.getId(), emitter);

		// lifecycle cleanup
		emitter.onCompletion(() -> sseService.remove(lobby.getId(), emitter));
        emitter.onTimeout(() -> sseService.remove(lobby.getId(), emitter));

		return emitter;
	}

    public void pushLobbyUpdate(Lobby lobby) {
        LobbyDTO lobbyDTO = DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
        sseService.push(lobby.getId(), "lobbyUpdate", lobbyDTO);
    }

}
