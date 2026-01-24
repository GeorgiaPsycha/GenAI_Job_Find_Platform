package dev.genai.genaibe.tools;

import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.services.ApplicationService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
public class ApplyTool implements Tool {

    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;

    public ApplyTool(ApplicationService applicationService, ObjectMapper objectMapper) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "apply_to_job";
    }

    @Override
    public String getDescription() {
        return "Applies to a job posting on behalf of the user. Requires the job_id and a short motivation letter.";
    }

    @Override
    public String getParameters() {
        // JSON Schema για το LLM
        return """
            {
                "type": "object",
                "properties": {
                    "job_id": {
                        "type": "string",
                        "description": "The UUID of the job posting to apply to."
                    },
                    "motivation": {
                        "type": "string",
                        "description": "A short sentence explaining why the user is a good fit."
                    }
                },
                "required": ["job_id", "motivation"]
            }
            """;
    }

    @Override
    public MessageDTO execute(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage message) throws Exception {
        // 1. Παίρνουμε τα ορίσματα (Arguments) που έστειλε το LLM ως JSON String
        String argumentsJson = toolCall.getFunction().getArguments();

        // 2. Μετατροπή JSON σε Java αντικείμενα (Parsing)
        JsonNode args = objectMapper.readTree(argumentsJson);
        String jobIdStr = args.get("job_id").asText();
        String motivation = args.get("motivation").asText();

        // 3. Βρίσκουμε το User ID (Εδώ βάζουμε hardcoded του Chris για το demo)
        // Στο production θα το έπαιρνες από το message.getUserId() ή το SecurityContext
        UUID demoUserId = UUID.fromString("ΒΑΛΕ_ΤΟ_UUID_ΤΟΥ_CHRIS_ΕΔΩ");
        UUID jobId = UUID.fromString(jobIdStr);

        // 4. Εκτέλεση της λογικής (Service)
        applicationService.applyForJob(demoUserId, jobId, motivation);

        // 5. Επιστροφή αποτελέσματος στο LLM (ως MessageDTO με role 'tool')
        return MessageDTO.builder()
                .role("tool")
                .toolCallId(toolCall.getId()) // Σημαντικό: Πρέπει να ταιριάζει με το ID της κλήσης
                .content("Successfully applied to job " + jobIdStr)
                .build();
    }
}
