package dev.ctrlspace.genai2506.genaibe.tools;

import dev.ctrlspace.genai2506.genaibe.models.dtos.completions.MessageDTO;
import dev.ctrlspace.genai2506.genaibe.models.entities.Agent;
import dev.ctrlspace.genai2506.genaibe.models.entities.ChatMessage;
import dev.ctrlspace.genai2506.genaibe.models.entities.DocumentSection;
import dev.ctrlspace.genai2506.genaibe.services.DocumentService;
import dev.ctrlspace.genai2506.genaibe.services.ReRankingApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class SearchTool implements Tool {
    private final DocumentService documentService;
    private final ReRankingApiService reRankingApiService;
    Logger logger = LoggerFactory.getLogger(SearchTool.class);
    private final ObjectMapper objectMapper;

    public SearchTool(ObjectMapper objectMapper, DocumentService documentService, ReRankingApiService reRankingApiService) {
        this.objectMapper = objectMapper;
        this.documentService = documentService;
        this.reRankingApiService = reRankingApiService;
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public MessageDTO execute(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage originalMessage) throws Exception {
        JsonNode arguments = objectMapper.readTree(toolCall.getFunction().getArguments());
        String query = arguments.get("query").asText();

        logger.info("Executing query: " + query);

        List<DocumentSection> relevantSections = documentService.semanticSearch(agent,
                originalMessage.getAccount().getId(),
                originalMessage.getContent() ,
                query,
                5);

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("""
                Results of search:
               
                """);

        for (int i = 0; i < relevantSections.size(); i++) {
            contextBuilder.append("Section ").append(i + 1).append(":\n");
            contextBuilder.append(relevantSections.get(i).getContent()).append("\n\n");
        }


        return MessageDTO.builder()
                .role("tool")
                .content("Search Result:\n" + contextBuilder.toString())
                .supportingDocuments(relevantSections)
                .build();

    }


}
