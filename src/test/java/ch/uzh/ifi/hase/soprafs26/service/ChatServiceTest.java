package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.ChatMessageRepository;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.repository.LobbyPlayerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

public class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private LobbyPlayerRepository lobbyPlayerRepository;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private ChatService chatService;

    private User testUser;
    private LobbyPlayer testLobbyPlayer;
    private Game testGame;
    private UUID testGameId;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        testGameId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        testLobbyPlayer = new LobbyPlayer();
        testLobbyPlayer.setUser(testUser);
        testLobbyPlayer.setTeamType(TeamType.Team1);

        testGame = new Game();
        testGame.setStatus(GameStatus.IN_PROGRESS);

        Mockito.when(gameRepository.findById(testGameId)).thenReturn(Optional.of(testGame));
    }

    // sendMessage

    @Test
    public void sendMessage_validInput_success() {
        // given
        ChatMessage saved = new ChatMessage();
        saved.setGameId(testGameId);
        saved.setSender("testuser");
        saved.setTeamType(TeamType.Team1);
        saved.setMessage("Hello!");
        saved.setSentAt(Instant.now());

        Mockito.when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);
        Mockito.when(chatMessageRepository.save(Mockito.any())).thenReturn(saved);

        // when
        ChatMessage result = chatService.sendMessage(testUser, testGameId, "Hello!");

        // then
        verify(chatMessageRepository, Mockito.times(1)).save(Mockito.any());
        assertEquals("testuser", result.getSender());
        assertEquals(TeamType.Team1, result.getTeamType());
        assertEquals("Hello!", result.getMessage());
        assertEquals(testGameId, result.getGameId());
    }

    @Test
    public void sendMessage_emptyMessage_throwsBadRequest() {
        // when/then
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, ""));
    }

    @Test
    public void sendMessage_blankMessage_throwsBadRequest() {
        // when/then
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, "   "));
    }

    @Test
    public void sendMessage_nullMessage_throwsBadRequest() {
        // when/then
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, null));
    }

    @Test
    public void sendMessage_gameNotInProgress_throwsForbidden() {
        // given
        testGame.setStatus(GameStatus.ENDED);
        Mockito.when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);

        // when/then
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, "Hello!"));
    }

    @Test
    public void sendMessage_userNotInGame_throwsForbidden() {
        // given
        Mockito.when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(null);

        // when/then
        assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(testUser, testGameId, "Hello!"));
    }

    @Test
    public void sendMessage_setsCorrectFields() {
        // given
        Mockito.when(lobbyPlayerRepository.findByUser(testUser)).thenReturn(testLobbyPlayer);
        Mockito.when(chatMessageRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        // when
        ChatMessage result = chatService.sendMessage(testUser, testGameId, "Test message");

        // then
        assertEquals(testGameId, result.getGameId());
        assertEquals("testuser", result.getSender());
        assertEquals(TeamType.Team1, result.getTeamType());
        assertEquals("Test message", result.getMessage());
        assertNotNull(result.getSentAt());
    }

    // getMessages

    @Test
    public void getMessages_returnsOrderedMessages() {
        // given
        ChatMessage msg1 = new ChatMessage();
        msg1.setMessage("First");
        msg1.setSentAt(Instant.parse("2026-01-01T10:00:00Z"));

        ChatMessage msg2 = new ChatMessage();
        msg2.setMessage("Second");
        msg2.setSentAt(Instant.parse("2026-01-01T10:00:01Z"));

        Mockito.when(chatMessageRepository.findByGameIdOrderBySentAtAsc(testGameId))
                .thenReturn(List.of(msg1, msg2));

        // when
        List<ChatMessage> result = chatService.getMessages(testGameId);

        // then
        assertEquals(2, result.size());
        assertEquals("First", result.get(0).getMessage());
        assertEquals("Second", result.get(1).getMessage());
    }

    @Test
    public void getMessages_noMessages_returnsEmptyList() {
        // given
        Mockito.when(chatMessageRepository.findByGameIdOrderBySentAtAsc(testGameId))
                .thenReturn(List.of());

        // when
        List<ChatMessage> result = chatService.getMessages(testGameId);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    public void getMessages_moreThan7_returnsLast7() {
        // given
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ChatMessage msg = new ChatMessage();
            msg.setMessage("Message " + i);
            messages.add(msg);
        }

        Mockito.when(chatMessageRepository.findByGameIdOrderBySentAtAsc(testGameId))
                .thenReturn(messages);

        // when
        List<ChatMessage> result = chatService.getMessages(testGameId);

        // then
        assertEquals(7, result.size());
        assertEquals("Message 4", result.get(0).getMessage());
        assertEquals("Message 10", result.get(6).getMessage());
    }
}
