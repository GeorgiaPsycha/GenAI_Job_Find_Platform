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
            @RequestHeader("Authorization") String authHeader, // 1. Διαβάζουμε το Header
            @RequestBody ChatMessage message
    ) throws Exception {

        // 2. Εξαγωγή του Token (βγάζουμε το "Bearer ")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);

        // 3. Βρίσκουμε το User ID από το Token
        String userIdStr = jwtService.extractUserId(token);

        // 4. Φέρνουμε τον User από τη βάση
        User user = userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new RuntimeException("User from token not found"));

        // 5. Καρφώνουμε τον ΣΩΣΤΟ χρήστη στο μήνυμα
        message.setUser(user);

        return chatMessageService.createMessage(message);
    }
}
