package dev.genai.genaibe.tools;
import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.Application;
import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.ApplicationRepository;
import dev.genai.genaibe.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GetMyApplicationsTool implements Tool {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public GetMyApplicationsTool(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getName() {
        return "get_my_applications";
    }

    @Override
    public String getDescription() {
        return "Retrieves a list of job applications made by the current user. Use this when the user asks 'What jobs have I applied to?' or 'Show my applications'.";
    }

    @Override
    public String getParameters() {
        // Επιστρέφουμε JSON Schema για τις παραμέτρους (εδώ είναι κενό γιατί δεν θέλει input)
        return """
            {
                "type": "object",
                "properties": {},
                "required": []
            }
            """;
    }

    @Override
    public MessageDTO execute(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage message) throws Exception {
        log.info("--- Executing GetMyApplicationsTool ---");

        // 1. Βρες τον χρήστη
        User user = message.getUser();
        // Fallback για testing (αν είσαι σε dev mode και δεν βρέθηκε user)
        if (user == null) {
            user = userRepository.findByEmail("chris@mailinator.com").orElse(null);
        }

        if (user == null) {
            return MessageDTO.builder().role("tool").content("Error: Could not identify the user.").build();
        }

        // 2. Βρες τις αιτήσεις
        List<Application> apps = applicationRepository.findByUser(user);

        if (apps.isEmpty()) {
            return MessageDTO.builder().role("tool").content("You have not applied to any jobs yet.").build();
        }

        // 3. Φτιάξε μια λίστα κειμένου για το LLM
        String report = apps.stream()
                .map(app -> String.format("- Job: %s (Company: %s) | Status: %s | Date: %s",
                        app.getJob().getTitle(),
                        app.getJob().getCompany(),
                        app.getStatus(),
                        app.getCreatedAt().toString().substring(0, 10))) // Μόνο η ημερομηνία
                .collect(Collectors.joining("\n"));

        return MessageDTO.builder()
                .role("tool")
                .content("Here are your applications:\n" + report)
                .build();
    }
}