package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessageGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ChatMessagePostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.ChatService;

@RestController
public class ChatController {

    private final AuthService authService;
    private final ChatService chatService;

    ChatController(AuthService authService, ChatService chatService) {
        this.authService = authService;
        this.chatService = chatService;
    }

    @PostMapping("/games/{gameId}/chat")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ChatMessageGetDTO sendMessage(@PathVariable UUID gameId,
            @RequestBody ChatMessagePostDTO dto,
            @RequestHeader(value = "Authorization", required = false) String token) {
        User user = authService.authenticateToken(token);
        ChatMessage message = chatService.sendMessage(user, gameId, dto.getMessage());
        return DTOMapper.INSTANCE.convertEntityToChatMessageGetDTO(message);
    }

    @GetMapping("/games/{gameId}/chat")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ChatMessageGetDTO> getMessages(@PathVariable UUID gameId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.authenticateToken(token);
        List<ChatMessage> messages = chatService.getMessages(gameId);
        return messages.stream()
                .map(DTOMapper.INSTANCE::convertEntityToChatMessageGetDTO)
                .collect(Collectors.toList());
    }
}