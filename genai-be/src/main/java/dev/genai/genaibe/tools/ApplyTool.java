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

        // 1. Διάβασμα παραμέτρων (με ασφάλεια για null)
        String jobIdStr = arguments.path("job_id").asText(null);
        String motivation = arguments.path("motivation").asText("I am very interested in this position.");
        String cvFileUrl = arguments.path("cv_file_url").asText(null);

        if (jobIdStr == null || "null".equals(jobIdStr)) {
            return MessageDTO.builder().role("tool").content("Error: Job ID was not provided.").build();
        }

        // 2. Εύρεση Job
        Document job = documentRepository.findById(UUID.fromString(jobIdStr))
                .orElseThrow(() -> new RuntimeException("Job not found with ID: " + jobIdStr));

        // 3. Εύρεση User
        User applicant = message.getUser();
        if (applicant == null) {
            // Fallback για testing (αν χρειάζεται)
            applicant = userRepository.findByEmail("chris@mailinator.com").orElse(null);
        }

        if (applicant == null) {
            return MessageDTO.builder().role("tool").content("Error: Could not identify the user applying.").build();
        }

        // --- ΑΛΛΑΓΗ ΓΙΑ DUPLICATE ---
        // 4. Έλεγχος αν έχει ήδη κάνει αίτηση
        boolean alreadyApplied = applicationRepository.existsByJobAndUser(job, applicant);
        if (alreadyApplied) {
            // Επιστρέφουμε μήνυμα επιτυχίας/ενημέρωσης ΧΩΡΙΣ να κάνουμε save
            return MessageDTO.builder()
                    .role("tool")
                    .content(String.format("You have already applied for the position '%s'. No new application was created.", job.getTitle()))
                    .build();
        }

        // 5. Δημιουργία Application
        Application app = new Application();
        app.setJob(job);
        app.setUser(applicant);
        app.setMotivationText(motivation);
        app.setCvFileUrl(cvFileUrl); // Αν υπάρχει URL αρχείου
        app.setStatus("APPLIED");

        // --- ΑΛΛΑΓΗ ΓΙΑ CV TEXT ---
        // Αντλούμε το κείμενο από το προφίλ του χρήστη (αν υπάρχει) και το βάζουμε στην αίτηση
        if (applicant.getCv_text() != null && !applicant.getCv_text().isEmpty()) {
            app.setCvContentText(applicant.getCv_text());
        }

        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());

        // 6. Αποθήκευση
        applicationRepository.save(app);

        return MessageDTO.builder()
                .role("tool")
                .content(String.format("Successfully applied to '%s'. Application ID: %s", job.getTitle(), app.getId()))
                .build();
    }
}
