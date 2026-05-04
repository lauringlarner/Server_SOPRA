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

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final LobbyPlayerRepository lobbyPlayerRepository;
    private final GameRepository gameRepository;

    public ChatService(ChatMessageRepository chatMessageRepository, LobbyPlayerRepository lobbyPlayerRepository, GameRepository gameRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
        this.gameRepository = gameRepository;
    }

    public ChatMessage sendMessage(User user, UUID gameId, String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is not in progress");
        }

        LobbyPlayer lobbyPlayer = lobbyPlayerRepository.findByUser(user);
        if (lobbyPlayer == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not in an active game");
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setGameId(gameId);
        chatMessage.setSender(user.getUsername());
        chatMessage.setTeamType(lobbyPlayer.getTeamType());
        chatMessage.setMessage(message);
        chatMessage.setSentAt(Instant.now());

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getMessages(UUID gameId) {
        List<ChatMessage> all = chatMessageRepository.findByGameIdOrderBySentAtAsc(gameId);
        int size = all.size();
        return size <= 7 ? all : all.subList(size - 7, size);
    }
}