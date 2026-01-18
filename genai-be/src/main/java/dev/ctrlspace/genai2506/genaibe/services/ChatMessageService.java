package dev.ctrlspace.genai2506.genaibe.services;


import dev.ctrlspace.genai2506.genaibe.models.dtos.completions.CompletionResponseDTO;
import dev.ctrlspace.genai2506.genaibe.models.dtos.completions.ContentPart;
import dev.ctrlspace.genai2506.genaibe.models.dtos.completions.MessageDTO;
import dev.ctrlspace.genai2506.genaibe.models.entities.ChatMessage;
import dev.ctrlspace.genai2506.genaibe.repositories.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService {


    private final ChatMessageRepository chatMessageRepository;
    private final CompletionsApiService completionsApiService;
    private final AgentService agentService;

    public ChatMessageService(ChatMessageRepository chatMessageRepository, CompletionsApiService completionsApiService, AgentService agentService) {
        this.chatMessageRepository = chatMessageRepository;
        this.completionsApiService = completionsApiService;
        this.agentService = agentService;
    }

    public CompletionResponseDTO createMessage(ChatMessage message) throws Exception {

        //TODO Validations
        // ......

        message.setSenderType("user");

        // same in DB
        message = chatMessageRepository.save(message);

        MessageDTO response = agentService.processMessage(message);
        // send to LLM

//        MessageDTO dto = new MessageDTO();
//        dto.setRole("user");
//        dto.setContent(message.getContent());
//        String response = completionsApiService.getCompletion("https://api.openai.com/v1/chat/completions",
//                        "gpt-5-nano",
//                        List.of(dto),
//                        0.7,
//                        1000,
//                        "You are a helpful customer support agent.")
//                .getChoices()
//                .getFirst().
//                getMessage()
//                .getContent();


        // save response to DB

        ChatMessage responseMessage = new ChatMessage();
        responseMessage.setThread(message.getThread());
        responseMessage.setAccount(message.getAccount());
        responseMessage.setSenderType("agent");
        responseMessage.setContent(response.getContent());

        chatMessageRepository.save(responseMessage);



        // return response


        CompletionResponseDTO completionResponse = CompletionResponseDTO.builder()
                .message(responseMessage)
                .supportingDocuments(response.getSupportingDocuments())
                .build();

        return completionResponse;
    }
}
