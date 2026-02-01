package dev.genai.genaibe.controllers;

import dev.genai.genaibe.models.dtos.completions.CompletionResponseDTO;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.UserRepository;
import dev.genai.genaibe.services.ChatMessageService;
import dev.genai.genaibe.services.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class MessageController {

    private final ChatMessageService chatMessageService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public MessageController(ChatMessageService chatMessageService, JwtService jwtService, UserRepository userRepository) {
        this.chatMessageService = chatMessageService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/messages")
    public CompletionResponseDTO postMessage(
            @RequestHeader("Authorization") String authHeader, // Read the Header
            @RequestBody ChatMessage message
    ) throws Exception {

        // Extract the Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);

        // Take the USERid from the token
        String userIdStr = jwtService.extractUserId(token);

        // Bring the user from the DB
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new RuntimeException("User from token not found"));

        // Combine the User with the message
        message.setUser(user);

        return chatMessageService.createMessage(message);
    }
}
