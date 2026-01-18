package dev.ctrlspace.genai2506.genaibe.controllers;


import dev.ctrlspace.genai2506.genaibe.models.dtos.completions.CompletionResponseDTO;
import dev.ctrlspace.genai2506.genaibe.models.entities.ChatMessage;
import dev.ctrlspace.genai2506.genaibe.services.ChatMessageService;
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
