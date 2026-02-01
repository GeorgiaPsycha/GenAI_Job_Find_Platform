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
        // is null beacuse it is not need imput
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

        // Find me the user
        User user = message.getUser();
        if (user == null) {
            user = userRepository.findByEmail("penelope@gmail.com").orElse(null);
        }

        if (user == null) {
            return MessageDTO.builder().role("tool").content("Error: Could not identify the user.").build();
        }

        // Fid all the applications
        List<Application> apps = applicationRepository.findByUser(user);

        if (apps.isEmpty()) {
            return MessageDTO.builder().role("tool").content("You have not applied to any jobs yet.").build();
        }

        // the list for the LLM
        String report = apps.stream()
                .map(app -> String.format("- Job: %s (Company: %s) | Status: %s | Date: %s",
                        app.getJob().getTitle(),
                        app.getJob().getCompany(),
                        app.getStatus(),
                        app.getCreatedAt().toString().substring(0, 10)))
                .collect(Collectors.joining("\n"));

        // return the list to Agent loop
        return MessageDTO.builder()
                .role("tool")
                .content("Here are your applications:\n" + report)
                .build();
    }
}