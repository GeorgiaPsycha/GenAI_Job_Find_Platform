package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.completions.ChatCompletionResponse;
import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.DocumentSection;
import dev.genai.genaibe.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private final DocumentService documentService;
    private final ReRankingApiService reRankingApiService;
    Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final CompletionsApiService completionsApiService;

    private Map<String, Tool> toolExecutionMap;


    private List<JsonNode> tools;

    private String runSqlTool = """
             {
                        "type": "function",
                        "function": {
                            "name": "run_select_query",
                            "description": "Run SQL queries in DB",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "query": {
                                        "type": "string",
                                        "description": "the SQL query to run in DB"
                                    }
                                },
                                "required": [
                                    "query"
                                ]
                            }
                        }
                    }
            """;


    private String searchTool = """
             {
                        "type": "function",
                        "function": {
                            "name": "search",
                            "description": "This tool is used Search and answer questions in a RAG system. Create a short question from a long user input, to be used in vector search for better results. if the question is short (less that 20 words), dont do anything (keep the same question). Use this tool only if the question is long, by summarizing it.",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "query": {
                                        "type": "string",
                                        "description": "the search question to then perform vector search on"
                                    }
                                },
                                "required": [
                                    "query"
                                ]
                            }
                        }
                    }
            """;

    @Autowired
    public AgentService(CompletionsApiService completionsApiService, ObjectMapper objectMapper, List<Tool> toolsList, DocumentService documentService, ReRankingApiService reRankingApiService) throws Exception {
        this.completionsApiService = completionsApiService;


        this.tools = new ArrayList<>();
        tools.add(objectMapper.readTree(runSqlTool));
        tools.add(objectMapper.readTree(searchTool));

        toolExecutionMap = toolsList.stream().collect(
                java.util.stream.Collectors.toMap(
                        Tool::getName,
                        tool -> tool
                )
        );
        this.documentService = documentService;
        this.reRankingApiService = reRankingApiService;
    }

    public MessageDTO processMessage(ChatMessage message) {


        // TODO: Get Agent from DB
        Agent agent = new Agent();
        agent.setBehavior("""
                You are a helpful recruiter, that help people to find their ideal job. When user asks a question, 
                a vector search will be applied, the relevant job will be posted along with user's question in the context
                of the message. Use the provided context to answer the user's question as best as you can.
                Mention the job title and company name when relevant.
                """);
        agent.setLlmModel("gpt-5-mini");
        agent.setRerankingModel("rerank-2.5");
        agent.setTemperature(1.0);
        agent.setMaxTokens(5000);


        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Answer the question: \n").append(message.getContent()).append("\n");

        List<MessageDTO> messageDTOs = new ArrayList<>();
        messageDTOs.add(MessageDTO.builder()
                .role("user")
                .content(contextBuilder.toString())
                .build());





        ChatCompletionResponse response;
        int counter = 0;
        List<DocumentSection> supportingDocuments = new ArrayList<>();
        do {

            response = completionsApiService.getCompletion(agent, messageDTOs, tools);

            messageDTOs.addLast(response.getChoices().getLast().getMessage());


            if ("tool_calls".equals(response.getChoices().getFirst().getFinishReason())) {

                logger.info("Having tools to execute! name: {} ", response.getChoices().getFirst().getMessage().getToolCalls().getFirst().getFunction().getName());
                for (MessageDTO.ToolCall toolCall : response.getChoices().getFirst().getMessage().getToolCalls()) {

                    try {
                        MessageDTO toolResponse = handleToolCall(toolCall, agent, message);
                        supportingDocuments.addAll(toolResponse.getSupportingDocuments());
                        toolResponse.setToolCallId(toolCall.getId());
                        toolResponse.setName(toolCall.getFunction().getName());
                        messageDTOs.addLast(toolResponse);
                    } catch (Exception e) {
                        MessageDTO toolResponseError = MessageDTO.builder()
                                .role("tool")
                                .toolCallId(toolCall.getId())
                                .name(toolCall.getFunction().getName())
                                .content("Error executing tool " + toolCall.getFunction().getName() + ": " + e.getMessage())
                                .build();
                    }

                }
            }


        } while ("tool_calls".equals(response.getChoices().getFirst().getFinishReason()) && counter++ < 10);

        MessageDTO lastResponse = messageDTOs.getLast();
        lastResponse.setSupportingDocuments(supportingDocuments);

        // Implement agent processing logic here
        return lastResponse;
    }

    private MessageDTO handleToolCall(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage message) throws Exception {

        return toolExecutionMap.get(toolCall.getFunction().getName()).execute(toolCall, agent, message);

    }


}
