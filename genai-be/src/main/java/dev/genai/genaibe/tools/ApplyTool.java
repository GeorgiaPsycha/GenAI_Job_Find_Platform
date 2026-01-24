package dev.genai.genaibe.tools;

import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.Application;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.Document;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.ApplicationRepository;
import dev.genai.genaibe.repositories.DocumentRepository;
import dev.genai.genaibe.repositories.UserRepository;
import dev.genai.genaibe.services.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class ApplyTool implements Tool {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(ApplyTool.class);

    public ApplyTool(ApplicationRepository applicationRepository,
                     DocumentRepository documentRepository,
                     UserRepository userRepository,
                     ObjectMapper objectMapper) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
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
        JsonNode arguments = objectMapper.readTree(toolCall.getFunction().getArguments());
        String jobIdStr = arguments.get("job_id").asText();
        String motivation = arguments.has("motivation") ? arguments.get("motivation").asText() : "No motivation provided";

        logger.info("--- Executing ApplyTool ---");
        logger.info("Target Job ID: {}", jobIdStr);
        logger.info("Motivation: {}", motivation);

        Document job = documentRepository.findById(UUID.fromString(jobIdStr))
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobIdStr));

        User applicant;
        if (message.getUser() != null) {
            applicant = message.getUser();
            logger.info("Applicant identified from Message: {}", applicant.getEmail());
        } else {
            applicant = userRepository.findByEmail("zeta@gmail.com")
                    .orElseThrow(() -> new RuntimeException("Default user not found"));
            logger.warn("No user in message. Using fallback user: {}", applicant.getEmail());
        }

        Application app = new Application();
        app.setJob(job);
        app.setUser(applicant);
        app.setMotivationText(motivation);
        app.setStatus("applied");
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());

        applicationRepository.save(app);

        logger.info("✅ Application SAVED successfully! [App ID: {}]", app.getId());

        return MessageDTO.builder()
                .role("tool")
                .content(String.format("Successfully applied to '%s' at '%s'. Application ID saved.", job.getTitle(), job.getCompany()))
                .build();
    }
}
