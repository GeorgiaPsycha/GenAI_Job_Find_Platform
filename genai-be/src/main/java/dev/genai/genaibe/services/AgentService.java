package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.completions.ChatCompletionResponse;
import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.DocumentSection;
import dev.genai.genaibe.tools.ApplyTool;
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
    private final ApplyTool applyTool;
    private final CompletionsApiService completionsApiService;

    Logger logger = LoggerFactory.getLogger(AgentService.class);

    private Map<String, Tool> toolExecutionMap;
    private List<JsonNode> tools;

//    private String runSqlTool = """
//             {
//                        "type": "function",
//                        "function": {
//                            "name": "run_select_query",
//                            "description": "Run SQL queries in DB",
//                            "parameters": {
//                                "type": "object",
//                                "properties": {
//                                    "query": {
//                                        "type": "string",
//                                        "description": "the SQL query to run in DB"
//                                    }
//                                },
//                                "required": [
//                                    "query"
//                                ]
//                            }
//                        }
//                    }
//            """;
//                            "description": "This tool is used Search and answer questions in a RAG system. Create a short question from a long user input, to be used in vector search for better results. if the question is short (less that 20 words), dont do anything (keep the same question). Use this tool only if the question is long, by summarizing it.",

//                                        "description": "the search question to then perform vector search on"

    private String searchTool = """
             {
                        "type": "function",
                        "function": {
                            "name": "search",
                            "description": "Search for job postings. Use this when the user asks to find jobs, lists, or vacancies.",
                            "parameters": {
                                "type": "object",
                                "properties": {
                                    "query": {
                                        "type": "string",
                                        "description": "The search keywords (e.g. 'Java Developer')"
                                    }
                                },
                                "required": [
                                    "query"
                                ]
                            }
                        }
                    }
            """;

    private String applyToolDefinition = """
            {
                "type": "function",
                "function": {
                    "name": "apply_to_job",
                    "description": "Apply to a specific job posting. Use ONLY when the user explicitly asks to apply.",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "job_id": { "type": "string", "description": "The UUID of the job/document." },
                            "motivation": { "type": "string", "description": "A short motivation sentence." }
                        },
                        "required": ["job_id", "motivation"]
                    }
                }
            }
            """;

    @Autowired
    public AgentService(CompletionsApiService completionsApiService, ObjectMapper objectMapper, List<Tool> toolsList, DocumentService documentService, ReRankingApiService reRankingApiService, ApplyTool applyTool) throws Exception {
        this.completionsApiService = completionsApiService;
        this.tools = new ArrayList<>();
        this.applyTool = applyTool;
        this.documentService = documentService;
        this.reRankingApiService = reRankingApiService;

//        tools.add(objectMapper.readTree(runSqlTool));
        tools.add(objectMapper.readTree(searchTool));
        tools.add(objectMapper.readTree(applyToolDefinition));

        toolExecutionMap = toolsList.stream().collect(
                java.util.stream.Collectors.toMap(
                        Tool::getName,
                        tool -> tool
                )
        );
    }

    public MessageDTO processMessage(ChatMessage message) {
        logger.info("=== Start Processing Message: '{}' ===", message.getContent());
        // TODO: Get Agent from DB
        Agent agent = new Agent();
//        agent.setBehavior("""
//                You are a helpful recruiter, that help people to find their ideal job. When user asks a question,
//                a vector search will be applied, the relevant job will be posted along with user's question in the context
//                of the message. Use the provided context to answer the user's question as best as you can.
//                Mention the job title and company name when relevant.
//                """);
        agent.setBehavior("""
                You are a smart AI Career Recruiter.
                
                RULES:
                1. GENERAL CHAT: If the user says "hi", "hello", or asks general questions, REPLY DIRECTLY. DO NOT USE TOOLS.
                2. SEARCH: Use 'search' ONLY if the user asks to find jobs.
                3. APPLY: Use 'apply_to_job' ONLY if the user explicitly asks to apply for a job.
                4. RESPONSE: After using a tool, always summarize the result to the user in a friendly, human-readable text.
                """);
        agent.setLlmModel("llama3.2");
        agent.setRerankingModel("rerank-2.5-lite");
        agent.setTemperature(0.7);
        agent.setMaxTokens(5000);


        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Answer the question: \n").append(message.getContent()).append("\n");

        List<MessageDTO> conversationHistory = new ArrayList<>();
        conversationHistory.add(MessageDTO.builder()
                .role("user")
                .content(contextBuilder.toString())
                .build());


        ChatCompletionResponse response;
        int counter = 0;
        List<DocumentSection> supportingDocuments = new ArrayList<>();

        do {
            logger.info("--- Loop Iteration: {} ---", counter + 1);
            // Κλήση στο LLM
            response = completionsApiService.getCompletion(agent, conversationHistory, tools);
            MessageDTO responseMessage = response.getChoices().getLast().getMessage();

            // Προσθήκη της απάντησης του LLM στο ιστορικό
            conversationHistory.add(responseMessage);

            // Ελέγχουμε αν το LLM θέλει να τρέξει Tool
            if ("tool_calls".equals(response.getChoices().getFirst().getFinishReason())) {

                logger.info("Executing Tool: {}", responseMessage.getToolCalls().getFirst().getFunction().getName());

                for (MessageDTO.ToolCall toolCall : responseMessage.getToolCalls()) {
                    try {
                        // Εκτέλεση του Tool (π.χ. DB Search ή Insert Application)
                        MessageDTO toolOutput = handleToolCall(toolCall, agent, message);

                        // Κρατάμε τα έγγραφα αν υπάρχουν
                        if (toolOutput.getSupportingDocuments() != null) {
                            supportingDocuments.addAll(toolOutput.getSupportingDocuments());
                        }

                        // Προσθήκη του αποτελέσματος στο ιστορικό
                        toolOutput.setToolCallId(toolCall.getId());
                        toolOutput.setName(toolCall.getFunction().getName());
                        conversationHistory.add(toolOutput);

                        logger.info("<<< Tool Execution Successful. Output added to context.");
                    } catch (Exception e) {
                        logger.error("!!! Tool Execution Failed: {}", e.getMessage());
                        // Error Handling μέσα στο Loop
                        conversationHistory.add(MessageDTO.builder()
                                .role("tool")
                                .toolCallId(toolCall.getId())
                                .name(toolCall.getFunction().getName())
                                .content("Error: " + e.getMessage())
                                .build());
                    }
                }
                // Το Loop συνεχίζεται για να διαβάσει το LLM το αποτέλεσμα και να απαντήσει
            } else {
                // Αν δεν έχει Tool Calls, σταματάμε
                logger.info(">>> Agent replied directly (No Tool).");
                break;
            }

        } while (counter++ < 5);

        logger.info("=== Message Processing Complete ===");
        MessageDTO lastResponse = conversationHistory.getLast();
        lastResponse.setSupportingDocuments(supportingDocuments);

        // Implement agent processing logic here
        return lastResponse;
    }

    private MessageDTO handleToolCall(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage message) throws Exception {

        if (toolExecutionMap.containsKey(toolCall.getFunction().getName())) {
            return toolExecutionMap.get(toolCall.getFunction().getName()).execute(toolCall, agent, message);
        } else {
            throw new RuntimeException("Unknown tool: " + toolCall.getFunction().getName());
        }
    }




}
