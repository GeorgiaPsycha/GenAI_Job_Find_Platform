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
        String jobIdStr = arguments.path("job_id").asText(null);
        String motivation = arguments.path("motivation").asText("I am very interested in this position.");
        String cvFileUrl = arguments.path("cv_file_url").asText(null);

        if (jobIdStr == null || "null".equals(jobIdStr)) {
            throw new RuntimeException("Job ID is missing from the request.");
        }
        logger.info("--- Executing ApplyTool ---");
        logger.info("Target Job ID: {}", jobIdStr);
        logger.info("CV URL: {}", cvFileUrl);

        Document job = documentRepository.findById(UUID.fromString(jobIdStr))
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobIdStr));

        User applicant = message.getUser();
        if (applicant == null) {
            applicant = userRepository.findByEmail("zeta@gmail.com").orElse(null);        }

        Application app = new Application();
        app.setJob(job);
        app.setUser(applicant);
        app.setMotivationText(motivation);
        app.setCvFileUrl(cvFileUrl);
        app.setStatus("APPLIED");
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());

        applicationRepository.save(app);

        return MessageDTO.builder()
                .role("tool")
                .content(String.format("Successfully applied to '%s'. Application ID: %s", job.getTitle(), app.getId()))
                .build();
    }
}
