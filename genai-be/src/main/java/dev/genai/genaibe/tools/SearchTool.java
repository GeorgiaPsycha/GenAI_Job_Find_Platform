package dev.genai.genaibe.tools;

import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.DocumentSection;
import dev.genai.genaibe.services.DocumentService;
import dev.genai.genaibe.services.ReRankingApiService;
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
    public String getDescription() {
        // when to use this tool
        return "Performs a semantic search on job postings and documents. Use this tool when the user asks to find jobs, search for specific roles, or retrieve information about the company.";
    }

    @Override
    public String getParameters() {
        return """
            {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The natural language search query (e.g. 'Java developer jobs in Athens')."
                    }
                },
                "required": ["query"]
            }
            """;
    }

    @Override
    public MessageDTO execute(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage originalMessage) throws Exception {
        JsonNode arguments = objectMapper.readTree(toolCall.getFunction().getArguments());
        String query = arguments.get("query").asText();

        logger.info("Executing query: " + query);

        //Vector Search + Re-ranking
        // just bring the top 5 most relevant result
        List<DocumentSection> relevantSections = documentService.semanticSearch(agent,
                originalMessage.getAccount().getId(),
                originalMessage.getContent() ,
                query,
                5);

        // make the response AI readable
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
