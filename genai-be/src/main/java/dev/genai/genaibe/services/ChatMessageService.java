package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.completions.CompletionResponseDTO;
import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.ChatThread;
import dev.genai.genaibe.repositories.ChatMessageRepository;
import dev.genai.genaibe.repositories.ChatThreadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatThreadRepository chatThreadRepository;
    private final CompletionsApiService completionsApiService;
    private final AgentService agentService;

    // add chatThreadRepository into the Constructor
    public ChatMessageService(ChatMessageRepository chatMessageRepository,
                              ChatThreadRepository chatThreadRepository,
                              CompletionsApiService completionsApiService,
                              AgentService agentService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatThreadRepository = chatThreadRepository;
        this.completionsApiService = completionsApiService;
        this.agentService = agentService;
    }

    @Transactional
    public CompletionResponseDTO createMessage(ChatMessage message) throws Exception {

        // Check if the Fe send a null or empty Thread
        if (message.getThread() == null || message.getThread().getId() == null) {

            //Create a new Thread
            ChatThread newThread = new ChatThread();
            newThread.setAccount(message.getAccount()); // coonect to the specific account

            // The tittle of the chat
            String userContent = message.getContent();
            String title = userContent.length() > 30 ? userContent.substring(0, 30) + "..." : userContent;
            newThread.setTitle(title);

            newThread.setStatus("active");
            Instant now = Instant.now();
            newThread.setCreatedAt(now);
            newThread.setUpdatedAt(now);

            // Save the Thread in the DB
            newThread = chatThreadRepository.save(newThread);

            message.setThread(newThread);
        }

        // Ρυθμίζουμε τα υπόλοιπα πεδία του μηνύματος
        message.setSenderType("user");
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }

        message = chatMessageRepository.save(message);
        MessageDTO response = agentService.processMessage(message);

        // Save the Agent message
        ChatMessage responseMessage = new ChatMessage();
        responseMessage.setThread(message.getThread());
        responseMessage.setAccount(message.getAccount());
        responseMessage.setSenderType("agent");
        responseMessage.setContent(response.getContent());
        responseMessage.setCreatedAt(Instant.now());

        chatMessageRepository.save(responseMessage);

        return CompletionResponseDTO.builder()
                .message(responseMessage)
                .supportingDocuments(response.getSupportingDocuments())
                .build();
    }
}