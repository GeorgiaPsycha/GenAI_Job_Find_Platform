package dev.genai.genaibe.controllers;

import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.UserRepository;
import dev.genai.genaibe.services.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@Slf4j
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    private final UserRepository userRepository;
    private final JwtService jwtService;

    //Constructor Injection
    public FileUploadController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            // Upload the file with a unique UUID
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath);
            String fileUrl = "/uploads/" + filename;

            // Check if the user is connected
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String userId = jwtService.extractUserId(token);

                    User user = userRepository.findById(UUID.fromString(userId))
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    //Check if the role is USER to save the PDF
                    if ("USER".equalsIgnoreCase(user.getRole())) {

                        // Convert the PDF to StringText
                        String extractedText = extractTextFromPdf(filePath);

                        // Update User in DB
                        user.setCv_text(extractedText);
                        userRepository.save(user);

                        log.info("CV Text saved for user: {}", user.getEmail());
                    } else {
                        log.info("ℹUser is ADMIN/RECRUITER. CV text not saved.");
                    }

                } catch (Exception e) {
                    log.error(" Warning: Failed to extract/save CV text: {}", e.getMessage());
                }
            }
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file");
        }
    }

    // Helper method to extract text using Apache PDFBox
    private String extractTextFromPdf(Path path) {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                // Limit text length to avoid DB crash
                return text.length() > 10000 ? text.substring(0, 10000) : text;
            }
        } catch (IOException e) {
            System.err.println("Error parsing PDF: " + e.getMessage());
        }
        return null;
    }
}