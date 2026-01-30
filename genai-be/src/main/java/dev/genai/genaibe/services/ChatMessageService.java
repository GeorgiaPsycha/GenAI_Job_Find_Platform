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
    private final ChatThreadRepository chatThreadRepository; // <--- Νέο πεδίο
    private final CompletionsApiService completionsApiService;
    private final AgentService agentService;

    // Προσθέτουμε το chatThreadRepository στον Constructor
    public ChatMessageService(ChatMessageRepository chatMessageRepository,
                              ChatThreadRepository chatThreadRepository,
                              CompletionsApiService completionsApiService,
                              AgentService agentService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatThreadRepository = chatThreadRepository;
        this.completionsApiService = completionsApiService;
        this.agentService = agentService;
    }

    @Transactional // Σημαντικό: Όλα γίνονται σε μία συναλλαγή
    public CompletionResponseDTO createMessage(ChatMessage message) throws Exception {

        // --- ΛΥΣΗ ΓΙΑ ΤΟ NULL THREAD ---
        // Ελέγχουμε αν το Frontend έστειλε null ή άδειο thread
        if (message.getThread() == null || message.getThread().getId() == null) {

            // 1. Δημιουργούμε νέο αντικείμενο Thread
            ChatThread newThread = new ChatThread();
            newThread.setAccount(message.getAccount()); // Συνδέουμε με το Account

            // 2. Βάζουμε έναν τίτλο (π.χ. τις πρώτες 30 λέξεις του μηνύματος)
            String userContent = message.getContent();
            String title = userContent.length() > 30 ? userContent.substring(0, 30) + "..." : userContent;
            newThread.setTitle(title);

            newThread.setStatus("active");
            Instant now = Instant.now();
            newThread.setCreatedAt(now);
            newThread.setUpdatedAt(now);

            // 3. Αποθηκεύουμε το Thread στη βάση ΠΡΙΝ το μήνυμα
            newThread = chatThreadRepository.save(newThread);

            // 4. Συνδέουμε το μήνυμα με το νέο Thread
            message.setThread(newThread);
        }

        // Ρυθμίζουμε τα υπόλοιπα πεδία του μηνύματος
        message.setSenderType("user");
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }

        // Τώρα η αποθήκευση θα πετύχει γιατί το thread_id υπάρχει!
        message = chatMessageRepository.save(message);

        // Στέλνουμε το μήνυμα στον Agent
        MessageDTO response = agentService.processMessage(message);

        // Αποθηκεύουμε την απάντηση του Agent
        ChatMessage responseMessage = new ChatMessage();
        responseMessage.setThread(message.getThread()); // Χρησιμοποιούμε το ίδιο thread
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