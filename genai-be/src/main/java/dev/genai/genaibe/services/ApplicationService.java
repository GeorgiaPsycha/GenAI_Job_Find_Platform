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
        // 1. Έλεγχος αν υπάρχει ο χρήστης
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GenAiException("User not found"));

        // 2. Έλεγχος αν υπάρχει η αγγελία
        Document job = documentRepository.findById(jobId)
                .orElseThrow(() -> new GenAiException("Job not found"));

        // 3. Έλεγχος αν έχει ήδη κάνει αίτηση
        if (applicationRepository.findByUserIdAndJobId(userId, jobId).isPresent()) {
            throw new GenAiException("User has already applied to this job");
        }

        // 4. Δημιουργία της αίτησης
        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setMotivationText(motivationText);
        application.setStatus("APPLIED");

        // (Προαιρετικά: Εδώ θα μπορούσαμε να τραβήξουμε το CV του χρήστη και να το αποθηκεύσουμε)
        // application.setCvContentText(...)

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsForJob(UUID jobId) {
        return applicationRepository.findByJobId(jobId);
    }
}
