package dev.genai.genaibe.services;

import dev.genai.genaibe.exceptions.GenAiException;
import dev.genai.genaibe.models.entities.Application;
import dev.genai.genaibe.models.entities.Document;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.ApplicationRepository;
import dev.genai.genaibe.repositories.DocumentRepository;
import dev.genai.genaibe.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {
    // the official apply of a user in a Job
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              DocumentRepository documentRepository,
                              UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Application applyForJob(UUID userId, UUID jobId, String motivationText) {
        // Check if teh User exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenAiException("User not found"));

        // Chck if this JobId exists
        Document job = documentRepository.findById(jobId)
                .orElseThrow(() -> new GenAiException("Job not found"));

        // Check if the user has already aplly for this job
        if (applicationRepository.findByUserIdAndJobId(userId, jobId).isPresent()) {
            throw new GenAiException("User has already applied to this job");
        }

        // Create the new application
        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setMotivationText(motivationText);
        application.setStatus("APPLIED");

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsForJob(UUID jobId) {
        return applicationRepository.findByJobId(jobId);
    }
}
