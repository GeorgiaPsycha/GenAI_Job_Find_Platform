package dev.genai.genaibe.controllers;

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
public class FileUploadController {

    // Ορίζουμε φάκελο αποθήκευσης
    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Δημιουργία φακέλου αν δεν υπάρχει
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Μοναδικό όνομα αρχείου για να μην έχουμε συγκρούσεις
            String filename = UUID.randomUUID().toString() + "" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            // Αποθήκευση
            Files.copy(file.getInputStream(), filePath);

            // Επιστροφή του URL (Relative path)
            String fileUrl = "/uploads/" + filename;

            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload file");
        }
    }
}