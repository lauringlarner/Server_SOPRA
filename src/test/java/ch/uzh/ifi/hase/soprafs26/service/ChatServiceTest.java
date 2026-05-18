package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ChatMessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageGetDTO;

public class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private LobbyPlayerRepository lobbyPlayerRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PusherService pusherService;

    @InjectMocks
    private ChatService chatService;

    private User testUser;
    private Lobby testLobby;
    private LobbyPlayer testLobbyPlayer;
    private Game testGame;
    private UUID testGameId;
    private UUID testLobbyId;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testGameId = UUID.randomUUID();
        testLobbyId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        testLobby = new Lobby();
        testLobby.setId(testLobbyId);

        testLobbyPlayer = new LobbyPlayer();
        testLobbyPlayer.setUser(testUser);
        testLobbyPlayer.setLobby(testLobby);
        testLobbyPlayer.setTeamType(TeamType.Team1);

        testGame = new Game();
        testGame.setStatus(GameStatus.IN_PROGRESS);
        testGame.setLobbyId(testLobbyId);

        when(gameRepository.findById(testGameId)).thenReturn(Optional.of(testGame));
    }

    @Test
    public void sendMessage_validInput_success() {
        ChatMessage saved = new ChatMessage();
        saved.setGameId(testGameId);
        saved.setSender("testuser");
        saved.setTeamType(TeamType.Team1);
        saved.setMessage("Hello!");
        saved.setSentAt(Instant.now());

        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);
        when(chatMessageRepository.save(Mockito.any())).thenReturn(saved);

        ChatMessage result = chatService.sendMessage(testUser, testGameId, "Hello!");

        verify(chatMessageRepository, Mockito.times(1)).save(Mockito.any());
        verify(pusherService, Mockito.times(1))
                .trigger(Mockito.eq("game-" + testGameId), Mockito.eq("ChatMessage"),
                        Mockito.any(ChatMessageGetDTO.class));
        assertEquals("testuser", result.getSender());
        assertEquals(TeamType.Team1, result.getTeamType());
        assertEquals("Hello!", result.getMessage());
        assertEquals(testGameId, result.getGameId());
    }

    @Test
    public void sendMessage_emptyMessage_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, ""));
    }

    @Test
    public void sendMessage_blankMessage_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, "   "));
    }

    @Test
    public void sendMessage_nullMessage_throwsBadRequest() {
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, null));
    }

    @Test
    public void sendMessage_gameNotInProgress_throwsForbidden() {
        testGame.setStatus(GameStatus.ENDED);
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);

        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, "Hello!"));
    }

    @Test
    public void sendMessage_userNotInGameLobby_throwsForbidden() {
        Lobby otherLobby = new Lobby();
        otherLobby.setId(UUID.randomUUID());
        testLobbyPlayer.setLobby(otherLobby);
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);

        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, "Hello!"));
    }

    @Test
    public void sendMessage_setsCorrectFields() {
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);
        when(chatMessageRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        ChatMessage result = chatService.sendMessage(testUser, testGameId, "Test message");

        assertEquals(testGameId, result.getGameId());
        assertEquals("testuser", result.getSender());
        assertEquals(TeamType.Team1, result.getTeamType());
        assertEquals("Test message", result.getMessage());
        assertNotNull(result.getSentAt());
        verify(pusherService, Mockito.times(1))
                .trigger(Mockito.eq("game-" + testGameId), Mockito.eq("ChatMessage"),
                        Mockito.any(ChatMessageGetDTO.class));
    }

    @Test
    public void sendMessage_repeatedMessage_savesAndBroadcastsAgain() {
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);
        when(chatMessageRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        ChatMessage first = chatService.sendMessage(testUser, testGameId, "Hello!");
        ChatMessage second = chatService.sendMessage(testUser, testGameId, "Hello!");

        assertEquals("Hello!", first.getMessage());
        assertEquals("Hello!", second.getMessage());
        verify(chatMessageRepository, Mockito.times(2)).save(Mockito.any());
        verify(pusherService, Mockito.times(2))
                .trigger(Mockito.eq("game-" + testGameId), Mockito.eq("ChatMessage"),
                        Mockito.any(ChatMessageGetDTO.class));
    }

    @Test
    public void getMessages_returnsOrderedMessages() {
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);

        ChatMessage msg1 = new ChatMessage();
        msg1.setMessage("First");
        msg1.setSentAt(Instant.parse("2026-01-01T10:00:00Z"));

        ChatMessage msg2 = new ChatMessage();
        msg2.setMessage("Second");
        msg2.setSentAt(Instant.parse("2026-01-01T10:00:01Z"));

        when(chatMessageRepository.findByGameIdOrderBySentAtAsc(testGameId))
                .thenReturn(List.of(msg1, msg2));

        List<ChatMessage> result = chatService.getMessages(testUser, testGameId);

        assertEquals(2, result.size());
        assertEquals("First", result.get(0).getMessage());
        assertEquals("Second", result.get(1).getMessage());
    }

    @Test
    public void getMessages_userNotInGameLobby_throwsForbidden() {
        Lobby otherLobby = new Lobby();
        otherLobby.setId(UUID.randomUUID());
        testLobbyPlayer.setLobby(otherLobby);
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);

        assertThrows(ResponseStatusException.class,
                () -> chatService.getMessages(testUser, testGameId));
    }

    @Test
    public void getMessages_noMessages_returnsEmptyList() {
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);
        when(chatMessageRepository.findByGameIdOrderBySentAtAsc(testGameId))
                .thenReturn(List.of());

        List<ChatMessage> result = chatService.getMessages(testUser, testGameId);

        assertTrue(result.isEmpty());
    }

    @Test
    public void getMessages_moreThan7_returnsLast7() {
        when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);

        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ChatMessage msg = new ChatMessage();
            msg.setMessage("Message " + i);
            messages.add(msg);
        }

        when(chatMessageRepository.findByGameIdOrderBySentAtAsc(testGameId))
                .thenReturn(messages);

        List<ChatMessage> result = chatService.getMessages(testUser, testGameId);

        assertEquals(7, result.size());
        assertEquals("Message 4", result.get(0).getMessage());
        assertEquals("Message 10", result.get(6).getMessage());
    }
}
