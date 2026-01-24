package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.completions.*;
import dev.genai.genaibe.models.entities.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompletionsApiService {

    private String apiKey;

    private Logger logger = LoggerFactory.getLogger(CompletionsApiService.class);


    public CompletionsApiService(@Value("${llms.openai.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public ChatCompletionResponse getCompletion(Agent agent, List<MessageDTO> messages, List<JsonNode> tools) {
        String url = "http://localhost:11434/v1/chat/completions";
        return getCompletion(url, agent.getLlmModel(), messages, agent.getTemperature(), agent.getMaxTokens(), agent.getBehavior(), tools);
    }
    public EmbeddingResponse getEmbedding(Agent agent, MessageDTO messages) {
        String url = "http://localhost:11434/v1/embeddings";
        return this.getEmbedding(url, agent.getEmbeddingsModel(), messages);
    }

    public ChatCompletionResponse getCompletion(String url, String model, List<MessageDTO> messages, Double temperature, Integer maxTokens, String systemPrompt, List<JsonNode> tools) {
        RestTemplate restTemplate = new RestTemplate();

        // 2. Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        RequestDTO requestBody = RequestDTO.builder()
                .model(model)
                .messages(new ArrayList<>())
//                .reasoningEffort("low")
                .temperature(temperature)
                .maxCompletionTokens(maxTokens)
                .toolChoice("auto")
                .tools(tools)
                .build();
        requestBody.getMessages().add(MessageDTO.builder()
                .role("system")
                .content(systemPrompt)
                .build());

        for (MessageDTO message : messages) {
            requestBody.getMessages().addLast(message);
        }


        logger.debug("Request body: {}", requestBody);
        HttpEntity request = new HttpEntity<>(requestBody, headers);
        ResponseEntity response = restTemplate.postForEntity(
                url,
                request,
                ChatCompletionResponse.class);

        logger.debug("Response body: {}", response);

        ChatCompletionResponse responseBody = (ChatCompletionResponse) response.getBody();

        return responseBody;
    }

    public EmbeddingResponse getEmbedding(String url, String model, MessageDTO messages) {


        RestTemplate restTemplate = new RestTemplate();

        // 2. Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        EmbeddingRequest requestBody = EmbeddingRequest.builder()
                .model(model)
                .input(messages.getContent())
                .build();

        HttpEntity request = new HttpEntity<>(requestBody, headers);
        ResponseEntity response = restTemplate.postForEntity(
                url,
                request,
                EmbeddingResponse.class);

        EmbeddingResponse responseBody = (EmbeddingResponse) response.getBody();

        return responseBody;
    }
}
