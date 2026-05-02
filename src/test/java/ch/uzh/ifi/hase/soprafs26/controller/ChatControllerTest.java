package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.TeamType;
import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessagePostDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.ChatService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private AuthService authService;

    // POST /games/{gameId}/chat

    @Test
    public void sendMessage_validInput_201() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setToken("token123");

        ChatMessage message = new ChatMessage();
        message.setGameId(gameId);
        message.setSender("testuser");
        message.setTeamType(TeamType.Team1);
        message.setMessage("Hello team!");
        message.setSentAt(Instant.parse("2026-01-01T10:00:00Z"));

        ChatMessagePostDTO dto = new ChatMessagePostDTO();
        dto.setMessage("Hello team!");

        given(authService.authenticateToken("Bearer token123")).willReturn(user);
        given(chatService.sendMessage(Mockito.any(), Mockito.eq(gameId), Mockito.eq("Hello team!")))
                .willReturn(message);

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token123")
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sender", is("testuser")))
                .andExpect(jsonPath("$.message", is("Hello team!")))
                .andExpect(jsonPath("$.teamType", is("Team1")));
    }

    @Test
    public void sendMessage_emptyMessage_400() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();
        User user = new User();
        user.setToken("token123");

        ChatMessagePostDTO dto = new ChatMessagePostDTO();
        dto.setMessage("");

        given(authService.authenticateToken("Bearer token123")).willReturn(user);
        given(chatService.sendMessage(Mockito.any(), Mockito.eq(gameId), Mockito.eq("")))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token123")
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void sendMessage_userNotInGame_403() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();
        User user = new User();
        user.setToken("token123");

        ChatMessagePostDTO dto = new ChatMessagePostDTO();
        dto.setMessage("Hello!");

        given(authService.authenticateToken("Bearer token123")).willReturn(user);
        given(chatService.sendMessage(Mockito.any(), Mockito.any(), Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not in an active game"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token123")
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    public void sendMessage_invalidToken_403() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();

        ChatMessagePostDTO dto = new ChatMessagePostDTO();
        dto.setMessage("Hello!");

        given(authService.authenticateToken(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer bad-token")
                .content(asJsonString(dto));

        mockMvc.perform(postRequest)
                .andExpect(status().isForbidden());
    }

    // GET /games/{gameId}/chat

    @Test
    public void getMessages_validRequest_200() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();
        User user = new User();
        user.setToken("token123");

        ChatMessage msg1 = new ChatMessage();
        msg1.setSender("alice");
        msg1.setTeamType(TeamType.Team1);
        msg1.setMessage("Hello!");
        msg1.setSentAt(Instant.parse("2026-01-01T10:00:00Z"));

        ChatMessage msg2 = new ChatMessage();
        msg2.setSender("bob");
        msg2.setTeamType(TeamType.Team2);
        msg2.setMessage("Hi!");
        msg2.setSentAt(Instant.parse("2026-01-01T10:00:01Z"));

        given(authService.authenticateToken("Bearer token123")).willReturn(user);
        given(chatService.getMessages(gameId)).willReturn(List.of(msg1, msg2));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sender", is("alice")))
                .andExpect(jsonPath("$[1].sender", is("bob")));
    }

    @Test
    public void getMessages_emptyChat_200() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();
        User user = new User();
        user.setToken("token123");

        given(authService.authenticateToken("Bearer token123")).willReturn(user);
        given(chatService.getMessages(gameId)).willReturn(List.of());

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer token123");

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getMessages_invalidToken_403() throws Exception {
        // given
        UUID gameId = UUID.randomUUID();

        given(authService.authenticateToken(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized"));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/games/" + gameId + "/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer bad-token");

        mockMvc.perform(getRequest)
                .andExpect(status().isForbidden());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
