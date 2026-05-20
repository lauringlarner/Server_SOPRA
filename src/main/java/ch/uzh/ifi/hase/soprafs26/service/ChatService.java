package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ChatMessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final LobbyPlayerRepository lobbyPlayerRepository;
    private final GameRepository gameRepository;
    private final PusherService pusherService;

    public ChatService(ChatMessageRepository chatMessageRepository,
            LobbyPlayerRepository lobbyPlayerRepository,
            GameRepository gameRepository,
            PusherService pusherService) {
        this.chatMessageRepository = chatMessageRepository;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
        this.gameRepository = gameRepository;
        this.pusherService = pusherService;
    }

    public ChatMessage sendMessage(User user, UUID gameId, String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        Game game = getGame(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is not in progress");
        }

        LobbyPlayer lobbyPlayer = getLobbyPlayerInGameLobby(user, game);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGameId(gameId);
        chatMessage.setSender(user.getUsername());
        chatMessage.setTeamType(lobbyPlayer.getTeamType());
        chatMessage.setMessage(message);
        chatMessage.setSentAt(Instant.now());

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        pushChatMessage(savedMessage);
        return savedMessage;
    }

    public List<ChatMessage> getMessages(User user, UUID gameId) {
        Game game = getGame(gameId);
        getLobbyPlayerInGameLobby(user, game);

        List<ChatMessage> all = chatMessageRepository.findByGameIdOrderBySentAtAsc(gameId);
        int size = all.size();
        return size <= 7 ? all : all.subList(size - 7, size);
    }

    public void pushChatMessage(ChatMessage chatMessage) {
        ChatMessageGetDTO chatMessageDTO = DTOMapper.INSTANCE.convertEntityToChatMessageGetDTO(chatMessage);
        pusherService.trigger("game-" + chatMessage.getGameId(), "ChatMessage", chatMessageDTO);
    }

    private Game getGame(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
    }

    private LobbyPlayer getLobbyPlayerInGameLobby(User user, Game game) {
        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);
        if (lobbyPlayer == null || lobbyPlayer.getLobby() == null
                || !game.getLobbyId().equals(lobbyPlayer.getLobby().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not in this game's lobby");
        }
        return lobbyPlayer;
    }
}
