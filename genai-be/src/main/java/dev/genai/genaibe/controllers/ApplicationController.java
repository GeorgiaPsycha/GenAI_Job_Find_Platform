package dev.genai.genaibe.controllers;

import dev.genai.genaibe.models.entities.Application;
import dev.genai.genaibe.services.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // Endpoint για να βλέπει ο Admin τις αιτήσεις μιας αγγελίας
    // GET http://localhost:8080/documents/{jobId}/applications
    @GetMapping("/documents/{jobId}/applications")
    public List<Application> getJobApplications(@PathVariable UUID jobId) {
        return applicationService.getApplicationsForJob(jobId);
    }

    // Προαιρετικό: Endpoint για Manual Application (αν θες να το τεστάρεις με Postman)
    @PostMapping("/applications")
    public Application applyManual(@RequestParam UUID userId, @RequestParam UUID jobId, @RequestBody String motivation) {
        return applicationService.applyForJob(userId, jobId, motivation);
    }
}
