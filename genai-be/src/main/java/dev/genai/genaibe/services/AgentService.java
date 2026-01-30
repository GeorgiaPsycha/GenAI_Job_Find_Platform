package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.completions.ChatCompletionResponse;
import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.DocumentSection;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private final CompletionsApiService completionsApiService;
    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final Map<String, Tool> toolExecutionMap;
    private final List<JsonNode> toolsDefinitions; // Η λίστα που στέλνουμε στο LLM

    @Autowired
    public AgentService(CompletionsApiService completionsApiService,
                        ObjectMapper objectMapper,
                        List<Tool> toolsList) { // Spring automatically injects all beans implementing Tool
        this.completionsApiService = completionsApiService;
        this.objectMapper = objectMapper;
        this.toolsDefinitions = new ArrayList<>();

        // 1. Δημιουργία του Execution Map (Όνομα -> Tool Instance)
        this.toolExecutionMap = toolsList.stream()
                .collect(Collectors.toMap(Tool::getName, Function.identity()));

        // 2. ΔΥΝΑΜΙΚΗ ΚΑΤΑΣΚΕΥΗ ΤΩΝ DEFINITIONS (Χωρίς hardcoded strings!)
        // Διαβάζουμε το όνομα, description και parameters από κάθε Tool class
        for (Tool tool : toolsList) {
            try {
                ObjectNode functionNode = objectMapper.createObjectNode();
                functionNode.put("name", tool.getName());
                functionNode.put("description", tool.getDescription());
                // Το getParameters επιστρέφει String JSON, το κάνουμε parse σε JsonNode
                JsonNode paramsNode = objectMapper.readTree(tool.getParameters());
                functionNode.set("parameters", paramsNode);

                ObjectNode toolNode = objectMapper.createObjectNode();
                toolNode.put("type", "function");
                toolNode.set("function", functionNode);

                this.toolsDefinitions.add(toolNode);
                logger.info("Registered Tool: {}", tool.getName());
            } catch (Exception e) {
                logger.error("Failed to register tool definition for: " + tool.getName(), e);
            }
        }
    }

    public MessageDTO processMessage(ChatMessage message) {
        logger.info("=== Start Processing Message: '{}' ===", message.getContent());
        // Setup Agent (Hardcoded for demo, normally from DB)
        Agent agent = new Agent();
        agent.setBehavior("""
                You are a smart AI Career Recruiter.
                RULES:
                1. GENERAL CHAT: If the user says "hi", "hello", or asks general questions, REPLY DIRECTLY. DO NOT USE TOOLS.
                2. SEARCH: Use 'search' ONLY if the user asks to find jobs.
                3. APPLY: Use 'apply_to_job' ONLY if the user explicitly asks to apply for a job.
                4. MY APPS: Use 'get_my_applications' if user asks about their applications.
                5. RESPONSE: After using a tool, always summarize the result to the user in a friendly, human-readable text.
                """);
        agent.setLlmModel("llama3.2");
        agent.setRerankingModel("rerank-2.5-lite");
        agent.setTemperature(0.7);
        agent.setMaxTokens(5000);

        // --- CV CONTEXT INJECTION (Το ζήτησες πριν) ---
        // Ελέγχουμε αν ο χρήστης έχει ανεβάσει βιογραφικό και το προσθέτουμε στο context
        User user = message.getUser();
        String systemPrompt = agent.getBehavior();
        if (user != null && user.getCv_text() != null && !user.getCv_text().isEmpty()) {
            systemPrompt += "\n\nUSER CONTEXT:\nThe user has uploaded a CV with the following content. If they ask to find jobs 'based on my cv', use skills/keywords from here for the search tool:\n" + user.getCv_text();
            // Ενημερώνουμε προσωρινά το prompt για αυτό το request
            agent.setBehavior(systemPrompt);
        }
        // ----------------------------------------------

        // Ετοιμασία Ιστορικού
        List<MessageDTO> conversationHistory = new ArrayList<>();
        conversationHistory.add(MessageDTO.builder()
                .role("user")
                .content(message.getContent())
                .build());

        ChatCompletionResponse response;
        int counter = 0;
        List<DocumentSection> supportingDocuments = new ArrayList<>();

        do {
            logger.info("--- Loop Iteration: {} ---", counter + 1);
            // Κλήση στο LLM (στέλνουμε τα δυναμικά definitions)
            response = completionsApiService.getCompletion(agent, conversationHistory, this.toolsDefinitions);
            MessageDTO responseMessage = response.getChoices().getLast().getMessage();

            // Προσθήκη απάντησης στο ιστορικό
            conversationHistory.add(responseMessage);

            // Έλεγχος για Tool Calls
            if ("tool_calls".equals(response.getChoices().getFirst().getFinishReason())) {
                // Για κάθε tool που ζήτησε το LLM
                for (MessageDTO.ToolCall toolCall : responseMessage.getToolCalls()) {
                    String functionName = toolCall.getFunction().getName();
                    logger.info("Executing Tool: {}", functionName);

                    try {
                        // Εκτέλεση Tool μέσω του Map
                        if (toolExecutionMap.containsKey(functionName)) {
                            MessageDTO toolOutput = toolExecutionMap.get(functionName).execute(toolCall, agent, message);
                            // Αποθήκευση αποτελεσμάτων
                            if (toolOutput.getSupportingDocuments() != null) {
                                supportingDocuments.addAll(toolOutput.getSupportingDocuments());
                            }

                            // Προσθήκη στο ιστορικό ως 'tool' message
                            toolOutput.setToolCallId(toolCall.getId());
                            toolOutput.setName(functionName);
                            conversationHistory.add(toolOutput);
                            logger.info("<<< Tool '{}' executed successfully.", functionName);
                        } else {
                            throw new RuntimeException("Unknown tool: " + functionName);
                        }

                    } catch (Exception e) {
                        logger.error("!!! Tool Execution Failed: {}", e.getMessage());
                        conversationHistory.add(MessageDTO.builder()
                                .role("tool")
                                .toolCallId(toolCall.getId())
                                .name(functionName)
                                .content("Error: " + e.getMessage())
                                .build());
                    }
                }
            } else {
                logger.info(">>> Agent replied directly (No Tool).");
                break;
            }

        } while (counter++ < 5);

        logger.info("=== Message Processing Complete ===");
        MessageDTO lastResponse = conversationHistory.getLast();
        lastResponse.setSupportingDocuments(supportingDocuments);

        return lastResponse;
    }
}