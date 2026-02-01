package dev.genai.genaibe.controllers;

import dev.genai.genaibe.models.dtos.completions.voyageai.RerankResponse;
import dev.genai.genaibe.models.dtos.completions.voyageai.RerankResponse.RerankResult;
import dev.genai.genaibe.models.entities.Application;
import dev.genai.genaibe.models.entities.Document;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.ApplicationRepository;
import dev.genai.genaibe.repositories.DocumentRepository;
import dev.genai.genaibe.repositories.UserRepository;
import dev.genai.genaibe.services.JwtService;
import dev.genai.genaibe.services.ReRankingApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin-ai")
@RequiredArgsConstructor
@Slf4j
public class AdminAiController {
    // Admin dashboard

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ReRankingApiService reRankingApiService;
    private final JwtService jwtService;

    // find all the jobs the admin has created
    @GetMapping("/my-jobs")
    public ResponseEntity<?> getMyJobs(@RequestHeader("Authorization") String authHeader) {
        User admin = getUserFromToken(authHeader);
        List<Document> myJobs = documentRepository.findByCreatedBy(admin);
        return ResponseEntity.ok(myJobs);
    }

    // reranking model to find the best candidate fit for the job
    @PostMapping("/job/{jobId}/rank-candidates")
    public ResponseEntity<?> rankCandidates(
            @PathVariable UUID jobId,
            @RequestHeader("Authorization") String authHeader
    ) {
        log.info("Ranking candidates for Job ID: {}", jobId);

        Document job = documentRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        List<Application> applications = applicationRepository.findByJob(job);

        if (applications.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<String> cvTexts = new ArrayList<>();
        List<Application> validApplications = new ArrayList<>();

        for (Application app : applications) {
            String text = app.getCvContentText();
            //if the description in teh cv is null we take the motivation text he sent with the application
            if (text == null || text.isBlank()) {
                text = app.getMotivationText();
            }
            if (text != null && !text.isBlank()) {
                cvTexts.add(text);
                validApplications.add(app);
            }
        }
        if (cvTexts.isEmpty()) {
            return ResponseEntity.ok(applications);
        }
        RerankResponse response = reRankingApiService.rerank(job.getBody(), cvTexts, cvTexts.size());
        List<Map<String, Object>> rankedResults = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (RerankResult result : response.getData()) {
                int originalIndex = result.getIndex();
                double score = result.getRelevanceScore();
                Application app = validApplications.get(originalIndex);
                Map<String, Object> dto = new HashMap<>();
                dto.put("applicationId", app.getId());
                dto.put("candidateName", app.getUser().getDisplayName());
                dto.put("candidateEmail", app.getUser().getEmail());
                dto.put("score", score);
                dto.put("motivation", app.getMotivationText());
                dto.put("status", app.getStatus());
                dto.put("appliedAt", app.getCreatedAt());
                rankedResults.add(dto);
            }
        }
        // rerank the result based on the score
        rankedResults.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));
        return ResponseEntity.ok(rankedResults);
    }

    private User getUserFromToken(String authHeader) {
        String token = authHeader.substring(7);
        String userId = jwtService.extractUserId(token);
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}