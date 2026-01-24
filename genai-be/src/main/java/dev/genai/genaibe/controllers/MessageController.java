package dev.genai.genaibe.controllers;


import dev.genai.genaibe.models.dtos.completions.CompletionResponseDTO;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.services.ChatMessageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {


    private final ChatMessageService chatMessageService;

    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @PostMapping("/messages")
    public CompletionResponseDTO postMessage(@RequestBody ChatMessage message) throws Exception {

        return chatMessageService.createMessage(message);
    }
}
